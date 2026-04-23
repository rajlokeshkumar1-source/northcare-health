# observability

Observability stack for the **NorthCare Enterprise Health Platform**.
Prometheus + Grafana + Alertmanager — all GitOps-managed via ArgoCD.

---

## What's Included

| Component | Purpose |
|-----------|---------|
| **Prometheus** | Metrics collection (kube-prometheus-stack) |
| **Grafana** | Dashboards + SLO visibility |
| **Alertmanager** | Alert routing → GitHub Issues |
| **Recording rules** | Pre-computed SLO metrics (fast queries) |
| **Alerting rules** | Per-service + cross-service SLO burn alerts |

---

## Repository Structure

```
observability/
├── prometheus/
│   └── rules/
│       ├── hospital-core.yaml     # Per-service rules
│       ├── telehealth.yaml
│       ├── billing.yaml
│       ├── insurance.yaml
│       ├── notifications.yaml
│       └── slo-recording.yaml     # Cross-service SLO + burn-rate rules
├── alertmanager/
│   └── config.yaml                # Alert routing to GitHub Issues
├── grafana/
│   ├── dashboards/
│   │   ├── hospital-core.json     # Service dashboard (6 panels)
│   │   └── slo-dashboard.json     # Error budget + burn rate overview
│   └── provisioning/
│       └── dashboards.yaml        # Grafana dashboard provisioner config
└── README.md
```

---

## How Dashboards Are Loaded via GitOps

Grafana uses the **k8s-sidecar** container to auto-discover dashboards from ConfigMaps.

1. The `kube-prometheus-stack` Helm chart deploys Grafana with `sidecar.dashboards.enabled: true`
2. The sidecar watches **all namespaces** for ConfigMaps labelled `grafana_dashboard: "1"`
3. Dashboard JSON files are converted to ConfigMaps in the monitoring namespace
4. The sidecar injects them into `/var/lib/grafana/dashboards/northcare/`
5. Grafana picks them up within 30 seconds (no restart required)

To add a dashboard via GitOps, create a ConfigMap:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboard-my-service
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  my-service.json: |
    { ... Grafana dashboard JSON ... }
```

Alternatively, add the JSON file to `grafana/dashboards/` and let ArgoCD sync it.

---

## SLO Definitions

| Service       | SLO Target | Error Budget (30d) | Key Metric |
|---------------|------------|---------------------|------------|
| hospital-core | 99.5%      | ~3.6 hours          | HTTP 5xx rate |
| telehealth    | 99.5%      | ~3.6 hours          | HTTP 5xx rate |
| billing       | 99.9%      | ~43 minutes         | HTTP 5xx + claims failure |
| insurance     | 99.5%      | ~3.6 hours          | HTTP 5xx + eligibility timeout |
| notifications | 99.0%      | ~7.3 hours          | HTTP 5xx + delivery failure |

### Multi-Window Multi-Burn-Rate (Google SRE Pattern)

Alerts fire on **two conditions simultaneously** to reduce false positives:

| Alert Type   | Short Window | Long Window | Burn Rate | Action |
|-------------|-------------|-------------|-----------|--------|
| Fast Burn   | 5m          | 1h          | 14.4×     | Page immediately |
| Slow Burn   | 30m         | 6h          | 6×        | Create GitHub issue |

---

## How Alerts Create GitHub Issues

Alertmanager routes all alerts to a webhook receiver pointing at
[alertmanager-github-webhook](https://github.com/m-mizutani/alertmanager-github-issues).

**Deploy the webhook service:**
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: alertmanager-github-webhook
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: alertmanager-github-webhook
  template:
    metadata:
      labels:
        app: alertmanager-github-webhook
    spec:
      containers:
        - name: webhook
          image: ghcr.io/m-mizutani/alertmanager-github-issues:latest
          env:
            - name: GITHUB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: alertmanager-github-token
                  key: token
            - name: GITHUB_OWNER
              value: northcare-health
            - name: GITHUB_REPO
              value: ops-incidents
EOF
```

Alert labels are mapped to GitHub issue labels:
- `severity: page` → label `P1`
- `severity: critical` → label `P2`
- `service: hospital-core` → label `hospital-core`

---

## How to Add a New Dashboard

1. Design the dashboard in Grafana UI (use `dev` instance)
2. Export as JSON: Dashboard settings → JSON Model → Copy
3. Save to `grafana/dashboards/<service-name>.json`
4. Set `"editable": false` and assign a unique `uid`
5. Commit and push — ArgoCD syncs the ConfigMap, sidecar loads it within 30s

---

## Key Metrics for Hospital Core

| Metric | Description |
|--------|-------------|
| `http_requests_total{job="hospital-core"}` | Total request count by status code |
| `http_request_duration_seconds_bucket` | Request duration histogram |
| `hospital_core:availability:ratio5m` | 5m availability ratio (recording rule) |
| `hospital_core:latency_p95:rate5m` | p95 latency (recording rule) |
| `slo:error_budget_remaining:hospital_core` | Error budget fraction remaining |
| `kube_deployment_status_replicas_available` | Available pod count |
| `container_cpu_usage_seconds_total` | CPU usage per container |
| `container_memory_working_set_bytes` | Memory working set per container |

---

## Runbook Template

When a new alert is added, create a corresponding runbook at
`runbooks/<service-name>.md`:

```markdown
# <Service Name> Runbook

## Alert: <AlertName>

### Symptoms
- ...

### Immediate Actions
1. Check pod status: `kubectl get pods -n <namespace>`
2. Check logs: `kubectl logs -n <namespace> deploy/<service> --tail=100`
3. Check recent deployments: `argocd app history <service>`

### Root Cause Investigation
- Check Grafana dashboard: <link>
- Check error patterns in logs
- Check dependent services

### Escalation
- If not resolved in 30 minutes, page the on-call engineer
```
