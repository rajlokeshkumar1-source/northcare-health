# NorthCare Enterprise Health Platform — Infrastructure (Terraform)

> AWS infrastructure for the **NorthCare** North-American hospital-network simulator.  
> EKS + ArgoCD GitOps · PostgreSQL RDS · Cost-optimised scheduling · HIPAA-aligned storage encryption.

---

## Architecture Overview

```
                          ┌─────────────────────────────────────────────────────┐
                          │                  us-east-1  VPC  10.0.0.0/16        │
                          │                                                     │
                          │  Public Subnets          Private Subnets            │
                          │  ┌───────────────┐       ┌────────────────────┐    │
Internet ──► IGW ─────────►  │ 10.0.1.0/24   │       │ 10.0.10.0/24 (1a)  │    │
                          │  │ 10.0.2.0/24   │       │ 10.0.20.0/24 (1b)  │    │
                          │  └──────┬────────┘       │                    │    │
                          │         │ NAT GW          │  EKS Node Group    │    │
                          │         └────────────────►│  (SPOT t3.medium)  │    │
                          │                          │                    │    │
                          │                          │  RDS PostgreSQL 15  │    │
                          │                          └────────────────────┘    │
                          └─────────────────────────────────────────────────────┘
                                          ▲
                          Jenkins (local Linux) ──► ArgoCD ──► EKS
                                                              ├── hospital-core
                                                              ├── telehealth
                                                              ├── billing
                                                              ├── insurance
                                                              └── notifications

                          EventBridge ──► Lambda Scheduler
                          (10am EST start / 4pm EST stop, Mon–Fri)
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Single NAT Gateway | Saves ~$32/mo vs multi-AZ; acceptable for dev simulator |
| SPOT instances (t3.medium + t3a.medium) | ~70% cost saving vs on-demand |
| Lambda scale-to-zero scheduler | Eliminates node/RDS costs outside business hours |
| IRSA (IAM Roles for Service Accounts) | Per-pod least-privilege; no static AWS keys in pods |
| Storage encryption on RDS | HIPAA-aligned: encrypts PHI at rest |
| Local Terraform backend | Zero cost for dev; instructions to migrate to TF Cloud included |

---

## Prerequisites

| Tool | Minimum Version | Install |
|------|----------------|---------|
| Terraform | 1.6.0 | https://developer.hashicorp.com/terraform/install |
| AWS CLI | 2.x | https://aws.amazon.com/cli/ |
| kubectl | 1.29+ | https://kubernetes.io/docs/tasks/tools/ |
| helm | 3.x | https://helm.sh/docs/intro/install/ |

**AWS credentials** must be configured before running Terraform:

```bash
aws configure           # interactive
# or
export AWS_PROFILE=northcare-dev
```

The caller identity needs the following high-level IAM permissions:
`AmazonEKSFullAccess`, `AmazonRDSFullAccess`, `IAMFullAccess`, `AmazonVPCFullAccess`, `AWSLambda_FullAccess`, `SecretsManagerReadWrite`, `CloudWatchEventsFullAccess`.

---

## Quick Start

```bash
# 1 — Clone and enter the dev environment
cd infra-terraform/environments/dev

# 2 — Initialise providers and modules
terraform init

# 3 — Review the execution plan
terraform plan

# 4 — Apply (EKS creation takes ~15 minutes)
terraform apply

# 5 — Configure kubectl (run after apply completes)
aws eks update-kubeconfig \
  --region us-east-1 \
  --name northcare-dev

# 6 — Verify nodes are ready
kubectl get nodes
```

### Destroy (full teardown)

```bash
terraform destroy
```

> **Note:** RDS has `deletion_protection = false` and `skip_final_snapshot = true` for easy dev teardown.

---

## Module Reference

### `modules/vpc-standard`

Creates a production-pattern VPC with public/private subnet pairs across two AZs, a single NAT Gateway, and all Kubernetes subnet tags.

| Input | Default | Description |
|-------|---------|-------------|
| `project_name` | — | Used in resource names |
| `environment` | — | Used in resource names and tags |
| `cluster_name` | — | Sets `kubernetes.io/cluster/*` subnet tags |
| `vpc_cidr` | `10.0.0.0/16` | VPC CIDR block |

**Subnets created:**

| Subnet | CIDR | AZ | Purpose |
|--------|------|----|---------|
| public-1 | 10.0.1.0/24 | us-east-1a | NAT GW, future ALB |
| public-2 | 10.0.2.0/24 | us-east-1b | Future ALB |
| private-1 | 10.0.10.0/24 | us-east-1a | EKS nodes, RDS |
| private-2 | 10.0.20.0/24 | us-east-1b | EKS nodes, RDS |

---

### `modules/eks-cluster`

Provisions an EKS 1.29 cluster with:
- Public + private API endpoint access
- SPOT managed node group (t3.medium / t3a.medium)
- Scaling: desired=1 / min=0 / max=2 (dev), max=3 (configurable)
- OIDC provider for IRSA
- Add-ons: `coredns`, `kube-proxy`, `vpc-cni`, `aws-ebs-csi-driver`

The node group has `lifecycle { ignore_changes = [scaling_config[0].desired_size] }` so Terraform drift detection does not fight the Lambda scheduler.

---

### `modules/rds-postgres`

PostgreSQL 15.4 on `db.t3.micro` with:
- Private subnet placement (no public access)
- Storage encryption enabled (HIPAA at-rest requirement)
- 7-day automated backups
- Master password generated by `random_password` and stored as a JSON bundle in **AWS Secrets Manager**

```json
// Secret value stored at: northcare-dev-db-db-credentials
{
  "username": "northcare_admin",
  "password": "<generated>",
  "host": "<rds-endpoint>",
  "port": 5432,
  "dbname": "northcare",
  "engine": "postgres"
}
```

---

### `modules/iam-irsa`

Generic reusable module that creates an IAM role with an OIDC-scoped trust policy restricted to a **single Kubernetes namespace + service account**. Use once per microservice:

```hcl
module "billing_irsa" {
  source = "../../modules/iam-irsa"

  role_name            = "northcare-dev-billing-irsa"
  oidc_provider_arn    = module.eks.oidc_provider_arn
  oidc_provider_url    = module.eks.oidc_provider_url
  namespace            = "billing"
  service_account_name = "billing-sa"
  policy_arns          = [aws_iam_policy.billing_secrets.arn]
}
```

Annotate the Kubernetes service account with `eks.amazonaws.com/role-arn: <role_arn>`.

---

### `modules/scheduler-lambda`

Weekday cost-control automation:

```
Mon–Fri  10:00 EST (14:00 UTC) → scale_up   → EKS desiredSize=1, RDS start
Mon–Fri  16:00 EST (20:00 UTC) → scale_down → EKS desiredSize=0, RDS stop
```

The Lambda function is packaged inline (no S3 upload needed). It handles the case where RDS is already in the desired state gracefully (no error).

**Manual override** — to start the environment immediately:

```bash
aws lambda invoke \
  --function-name dev-northcare-scheduler \
  --payload '{"action":"scale_up"}' \
  --cli-binary-format raw-in-base64-out \
  response.json
```

---

## Cost Breakdown (~$83/month)

Costs assume the **scheduler runs Mon–Fri 10am–4pm EST** = ~126 active hours/month (21 weekdays × 6 hrs).

| Component | Config | Active Hours | Est. Cost/Month |
|-----------|--------|-------------|-----------------|
| EKS Control Plane | Always on (can't stop) | 720 hrs | **$72.00** |
| EKS SPOT Nodes | t3.medium SPOT ~$0.009/hr | 126 hrs | **$1.10** |
| RDS db.t3.micro | Stopped outside hours; 20 GB storage | 126 hrs + storage | **$2.60** |
| NAT Gateway | Single AZ; minimal data in dev | 720 hrs + data | **$4.50** |
| Lambda + EventBridge | 2 invocations/day | ~42 calls/month | **$0.00** |
| CloudWatch Logs | Basic cluster/Lambda logs | — | **$1.00** |
| Secrets Manager | 1 secret | — | **$0.40** |
| **Total** | | | **~$81–83/month** |

> 💡 **Largest saving lever:** The EKS control plane at $72/month is fixed. For experiments under $50/month, consider replacing EKS with a single EC2 instance running k3s.

---

## Connecting kubectl After Apply

```bash
# Update local kubeconfig
aws eks update-kubeconfig \
  --region us-east-1 \
  --name northcare-dev \
  --alias northcare-dev

# Verify connectivity
kubectl cluster-info
kubectl get nodes -o wide

# View all NorthCare workloads
kubectl get pods -A
```

### Retrieve DB credentials from Secrets Manager

```bash
aws secretsmanager get-secret-value \
  --secret-id northcare-dev-db-db-credentials \
  --query SecretString \
  --output text | python3 -m json.tool
```

---

## How the Scheduler Works

```
EventBridge cron(0 14 ? * MON-FRI *)     ← 10:00 AM EST
        │
        └─► Lambda dev-northcare-scheduler
                │   payload: {"action": "scale_up"}
                ├── eks.update_nodegroup_config(desiredSize=1, minSize=1)
                └── rds.start_db_instance(northcare-dev-db)

EventBridge cron(0 20 ? * MON-FRI *)     ← 4:00 PM EST
        │
        └─► Lambda dev-northcare-scheduler
                │   payload: {"action": "scale_down"}
                ├── eks.update_nodegroup_config(desiredSize=0, minSize=0)
                └── rds.stop_db_instance(northcare-dev-db)
```

> **Timezone note:** All EventBridge crons are in UTC. EST = UTC−5. If you observe DST (EDT = UTC−4), update the crons to `cron(0 13 ? * MON-FRI *)` (start) and `cron(0 20 ? * MON-FRI *)` (stop) during summer months.

---

## Repository Structure

```
infra-terraform/
├── modules/
│   ├── vpc-standard/          # VPC, subnets, IGW, NAT GW, route tables
│   ├── eks-cluster/           # EKS cluster, node group, OIDC, add-ons
│   ├── rds-postgres/          # RDS PG15, subnet group, SG, Secrets Manager
│   ├── iam-irsa/              # Generic IRSA role (one instance per service)
│   └── scheduler-lambda/      # Lambda + EventBridge scale-to-zero scheduler
└── environments/
    └── dev/
        ├── providers.tf        # Provider versions + local backend
        ├── variables.tf        # Variable declarations with dev defaults
        ├── terraform.tfvars    # Concrete dev values
        └── main.tf             # Module composition
```

---

## Migrating to Terraform Cloud

1. Create a workspace named `northcare-dev` in your HCP Terraform organisation.
2. In `environments/dev/providers.tf`, replace the `backend "local" {}` block with:

```hcl
cloud {
  organization = "northcare"
  workspaces {
    name = "northcare-dev"
  }
}
```

3. Run `terraform login && terraform init -migrate-state`.
