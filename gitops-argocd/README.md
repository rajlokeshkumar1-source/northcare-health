# gitops-argocd

GitOps repository for the **NorthCare Enterprise Health Platform**, managed by ArgoCD on EKS.

---

## Repository Structure

```
gitops-argocd/
├── apps/                          # ArgoCD Application manifests (App-of-Apps root)
│   ├── argocd-app-of-apps.yaml   # Root app — bootstraps everything
│   ├── hospital-core.yaml
│   ├── telehealth.yaml
│   ├── billing.yaml
│   ├── insurance.yaml
│   ├── notifications.yaml
│   └── monitoring.yaml
│
├── hospital-core/dev/             # Helm values + templates for hospital-core (dev)
│   ├── Chart.yaml                 # Declares dependency on shared northcare-service chart
│   ├── values.yaml                # Service-specific overrides (image tag updated by Jenkins)
│   └── templates/
│       └── external-secret.yaml  # ExternalSecret pulling creds from AWS Secrets Manager
│
├── telehealth/dev/                # Same pattern for each service
├── billing/dev/
├── insurance/dev/
├── notifications/dev/
│
├── monitoring/                    # kube-prometheus-stack umbrella chart
│   ├── Chart.yaml
│   └── values.yaml
│
└── cluster-setup/                 # One-time cluster bootstrap (applied before apps)
    ├── argocd-project.yaml        # AppProject: northcare
    └── cluster-secret-store.yaml  # ClusterSecretStore for External Secrets Operator
```

---

## App-of-Apps Pattern

ArgoCD uses a single root `Application` (`argocd-app-of-apps`) that watches the `apps/` directory.
Any YAML file added to `apps/` automatically becomes a managed application.

```
                     ┌─────────────────────────────┐
                     │  argocd-app-of-apps           │
                     │  watches: apps/               │
                     └────────────┬────────────────┘
          ┌────────────┬──────────┼──────────┬────────────────┐
          ▼            ▼          ▼          ▼                ▼
    hospital-core  telehealth  billing  insurance  notifications  monitoring
    (ns: hospital) (telehealth)(billing)(insurance)(notifs)      (monitoring)
```

### Bootstrap Order

1. Apply `cluster-setup/argocd-project.yaml` manually (one-time)
2. Apply `cluster-setup/cluster-secret-store.yaml` manually (one-time)
3. Apply `apps/argocd-app-of-apps.yaml` — ArgoCD self-manages everything from here

---

## How Jenkins Updates Image Tags

Jenkins CI pipeline runs on every push to `main` of a service repo.
The final stage (`northcareGitOpsUpdate`) does:

```bash
# Clone this repo
git clone https://<token>@github.com/northcare-health/gitops-argocd.git

# Patch the image tag
sed -i 's/tag: .*/tag: "<BUILD_NUMBER>-<GIT_SHA>"/' hospital-core/dev/values.yaml

# Commit and push
git commit -m "deploy(hospital-core): 42-abc1234 [skip ci]"
git push
```

ArgoCD detects the commit within ~3 minutes (or immediately via webhook) and rolls out
the new image automatically (automated sync is enabled for all service apps).

---

## How to Manually Sync

```bash
# Sync a single app
argocd app sync hospital-core

# Sync all apps
argocd app sync --selector app.kubernetes.io/part-of=northcare-health-platform

# Hard refresh (ignore cache)
argocd app get hospital-core --hard-refresh

# Sync monitoring (no auto-sync — must be done manually)
argocd app sync monitoring
```

---

## How to Add a New Service

1. **Create service app manifest** in `apps/<service-name>.yaml` (copy from an existing one, update name/path/namespace).
2. **Create service values directory**: `<service-name>/dev/` with `Chart.yaml`, `values.yaml`, `templates/external-secret.yaml`.
3. **Add namespace** to `cluster-setup/argocd-project.yaml` destinations list.
4. **Add Prometheus rules** in the `observability/` repo (separate PR).
5. **Commit** — ArgoCD's App-of-Apps picks up the new `apps/<service-name>.yaml` automatically.
6. **Add Jenkins pipeline** — create a `multibranchPipelineJob` entry in `jenkins-config/casc/jenkins.yaml`.

---

## External Secrets Operator

Secrets are **never stored in Git**. Instead, each service has an `ExternalSecret` resource
that instructs External Secrets Operator (ESO) to pull from AWS Secrets Manager.

```
ExternalSecret  →  ClusterSecretStore (IRSA)  →  AWS Secrets Manager
     │                                                  │
     └──────── creates Kubernetes Secret ───────────────┘
```

**Secret paths follow the pattern:** `northcare/<env>/<service>/<store>`

Example: `northcare/dev/hospital-core/db` → properties: `database_url`, `password`

Secrets are refreshed every **1 hour** automatically by ESO.

---

## Sync Policies

| App           | Auto-sync | Self-heal | Prune |
|---------------|-----------|-----------|-------|
| hospital-core | ✅        | ✅        | ✅    |
| telehealth    | ✅        | ✅        | ✅    |
| billing       | ✅        | ✅        | ✅    |
| insurance     | ✅        | ✅        | ✅    |
| notifications | ✅        | ✅        | ✅    |
| monitoring    | ❌ Manual | ❌        | ❌    |

Monitoring uses manual sync to avoid unintentional Prometheus/Grafana restarts.
