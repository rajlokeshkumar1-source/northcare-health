# jenkins-config

Jenkins CI configuration for the **NorthCare Enterprise Health Platform**.
Includes Configuration as Code (JCasC) and a shared pipeline library.

---

## Prerequisites

- Ubuntu 22.04 LTS (or compatible Linux)
- Docker installed and Jenkins user in `docker` group
- AWS CLI v2 configured with appropriate IAM role
- Git, Python 3.11+

---

## Installing Jenkins on a Local Linux Machine

```bash
# 1. Install Java 17
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# 2. Add Jenkins repo and install
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | \
  sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/" | \
  sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update && sudo apt-get install -y jenkins

# 3. Add Jenkins to docker group
sudo usermod -aG docker jenkins

# 4. Start Jenkins
sudo systemctl enable --now jenkins
```

---

## Required Jenkins Plugins

Install these via the Plugin Manager (or add to `plugins.txt` for Docker-based setups):

| Plugin | Purpose |
|--------|---------|
| `configuration-as-code` | Load `jenkins.yaml` at startup |
| `job-dsl` | Seed job DSL for pipeline creation |
| `workflow-aggregator` | Full pipeline support |
| `pipeline-github-lib` | Shared library from GitHub |
| `github-branch-source` | Multibranch pipelines from GitHub |
| `blueocean` | Modern pipeline UI |
| `role-strategy` | RBAC |
| `ansicolor` | Coloured build logs |
| `timestamper` | Timestamps in console |
| `ws-cleanup` | Workspace cleanup post-build |
| `docker-workflow` | Docker steps in pipelines |
| `credentials-binding` | `withCredentials` block |
| `html-publisher` | Publish coverage reports |
| `junit` | Test result publishing |

---

## Environment Variables (Secrets)

Set these in `/etc/default/jenkins` or via `systemctl edit jenkins`:

```ini
JENKINS_ADMIN_PASSWORD=<strong-password>
ECR_REGISTRY_URL=<AWS_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com
GITHUB_TOKEN=<github-pat-with-repo-and-workflow-scopes>
AWS_ACCOUNT_ID=<12-digit-account-id>
CASC_JENKINS_CONFIG=/var/lib/jenkins/casc/jenkins.yaml
```

**Never commit these values to Git.**

---

## Loading JCasC Configuration

```bash
# Copy config to Jenkins home
sudo mkdir -p /var/lib/jenkins/casc
sudo cp jenkins-config/casc/jenkins.yaml /var/lib/jenkins/casc/
sudo chown -R jenkins:jenkins /var/lib/jenkins/casc

# Point Jenkins to it
echo 'CASC_JENKINS_CONFIG=/var/lib/jenkins/casc/jenkins.yaml' | \
  sudo tee -a /etc/default/jenkins

# Restart Jenkins to apply
sudo systemctl restart jenkins
```

To reload without restarting: **Manage Jenkins → Configuration as Code → Reload existing configuration**.

---

## How the Shared Library Works

The `northcare-pipeline` shared library lives in this repo under `shared-library/`.

```
shared-library/
└── vars/
    ├── northcarePipeline.groovy      # Main pipeline definition
    ├── northcareTest.groovy          # Lint & pytest
    ├── northcareBuild.groovy         # Docker build (BuildKit)
    ├── northcareScan.groovy          # Trivy security scan
    ├── northcarePush.groovy          # ECR login + push
    └── northcareGitOpsUpdate.groovy  # Patch values.yaml + git push
```

The library is **implicitly loaded** (no `@Library` annotation needed) because JCasC
configures it with `implicit: true`.

Each service's `Jenkinsfile` is minimal:

```groovy
northcarePipeline(serviceName: 'hospital-core')
```

---

## Adding a New Service Pipeline

1. Create `service-<name>` GitHub repo with a root `Jenkinsfile` containing:
   ```groovy
   northcarePipeline(serviceName: '<name>')
   ```
2. Add the service to the `services` list in `casc/jenkins.yaml` under the DSL seed script.
3. Reload JCasC — Jenkins auto-creates the multibranch pipeline job.
4. Trigger the first scan manually or wait for the GitHub webhook.

---

## CI/CD Flow

```
Developer pushes to service-hospital-core (main branch)
    │
    ▼
Jenkins Multibranch Pipeline detects push (webhook or polling)
    │
    ├─ Checkout
    ├─ northcareTest     → pytest + coverage (fails if < 80%)
    ├─ northcareBuild    → docker build (BuildKit, OCI labels)
    ├─ northcareScan     → Trivy scan (fails on CRITICAL CVEs)
    ├─ northcarePush     → ECR login + push versioned + :latest tags
    └─ northcareGitOpsUpdate → patches hospital-core/dev/values.yaml in gitops-argocd
                                git commit + push [skip ci]
                                    │
                                    ▼
                              ArgoCD detects commit (~3 min)
                                    │
                                    ▼
                              Helm upgrade → EKS rolling update
```

---

## Webhook vs Polling

**Preferred: GitHub Webhook**

In each service repo → Settings → Webhooks → Add webhook:
- Payload URL: `http://jenkins.northcare-health.internal:8080/github-webhook/`
- Content type: `application/json`
- Events: Push, Pull request

**Fallback: Polling (configured in JCasC)**

The DSL sets `periodic(5)` — polls every 5 minutes if the webhook fails.

---

## Image Tagging Strategy

Images are tagged as `<BUILD_NUMBER>-<GIT_SHA_7>`, e.g. `42-abc1234`.

- **Immutable** — a tag is never reused
- **Traceable** — Jenkins build number and commit SHA are embedded
- **:latest** — also pushed as a convenience tag (not used in Helm values)

The GitOps repo always references the exact versioned tag.
