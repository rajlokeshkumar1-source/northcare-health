# 🤖 NorthCare AI Task Agent

A zero-cost daily task agent that keeps Mithra (Cloud/DevOps/SRE engineer) sharp
by generating realistic GitHub Issues every weekday — calibrated to her current skill level.

---

## What it does

| Schedule | Action |
|----------|--------|
| **Mon–Fri 9am EST** | Generates 1–2 weighted tasks (50% enhancement / 30% bug / 10% docs / 10% learning) |
| **Every Monday** | Creates a `🗓️ Sprint Plan` issue for the week |
| **Every Friday** | Creates a `📊 Sprint Review` issue + LinkedIn post draft |
| **1st of month** | Creates a `🔍 Code Review Challenge` with hidden bugs for Mithra to catch |
| **15th of month** | Creates a `🌪️ Chaos Day` AWS Fault Injection Simulator exercise |

Task difficulty auto-scales across 4 months:
`beginner → intermediate → advanced → senior`

---

## GitHub Secrets required

Add these in **Settings → Secrets → Actions** on the `ai-task-agent` repo:

| Secret | Description |
|--------|-------------|
| `AGENT_GITHUB_TOKEN` | A GitHub PAT with `repo` scope for the `northcare-health` org |
| `GITHUB_MODELS_API_KEY` | Your GitHub Models API key (see below) |

---

## How to get a GitHub Models API key (free)

1. Go to [github.com/marketplace/models](https://github.com/marketplace/models)
2. Select any model (e.g. GPT-4o mini)
3. Click **"Get API key"** → **"Generate new token"**
4. Copy the token — it's a standard GitHub PAT that also authenticates GitHub Models
5. Paste it as `GITHUB_MODELS_API_KEY` in your repo secrets

> **Cost:** $0. GitHub Models free tier covers ~150 requests/day for GPT-4o mini.
> GitHub Actions free tier gives 2000 min/month — each agent run takes ~30 seconds.

---

## How to test locally

```bash
# 1. Clone and enter the repo
git clone https://github.com/northcare-health/ai-task-agent
cd ai-task-agent

# 2. Create a .env file (never commit this!)
cat > .env <<EOF
GITHUB_TOKEN=ghp_your_pat_here
GITHUB_MODELS_API_KEY=ghp_your_models_key_here
EOF

# 3. Install dependencies
pip install -r requirements.txt

# 4. Run the agent
cd agent
python -c "
from dotenv import load_dotenv; load_dotenv('../.env')
import main; main.main()
"
```

---

## How to customise tasks

Edit **`mithra-skills.yaml`** at the root of this repo:

### Update current month manually
```yaml
meta:
  project_start_date: "2025-06-01"  # Set to your actual start date
  current_month: 2                   # Override if auto-calculation is off
```
The agent calculates month automatically from `project_start_date`.
Setting `current_month` acts as a manual override fallback.

### Adjust skill levels after completing tasks
```yaml
skills:
  aws:
    eks: intermediate   # bump to "advanced" once you're comfortable
    iam_irsa: beginner  # leave as-is until you've done the IRSA tasks
```

### Change task distribution
Edit `TASK_TYPE_POOL` in `agent/task_generator.py`:
```python
TASK_TYPE_POOL = ["enhancement"] * 5 + ["bug"] * 3 + ["docs"] * 1 + ["learning"] * 1
```

---

## Sample tasks by difficulty

### Beginner (Month 1)
- **[enhancement]** Deploy `hospital-core` FastAPI service to EKS with a Helm chart
- **[bug]** ArgoCD app stuck in `OutOfSync` after RDS secret rotation
- **[docs]** Write runbook: "How to roll back a broken ArgoCD deployment"

### Intermediate (Month 2)
- **[enhancement]** Set up IRSA for `hospital-core` to access Secrets Manager without static creds
- **[bug]** Prometheus scrape failing for pods in `hospital` namespace — 403 from RBAC
- **[learning]** Configure SLO alerting: 99.5% availability target with error budget burn alerts

### Advanced (Month 3)
- **[enhancement]** Implement HPA + Karpenter for patient-api based on custom Prometheus metrics
- **[bug]** Node group running OOM during peak hours — triage and fix resource requests/limits
- **[chaos]** Run AWS FIS experiment: terminate 50% of EKS nodes, verify auto-recovery < 5 min

### Senior (Month 4)
- **[enhancement]** Refactor Terraform into reusable modules with remote state and workspace strategy
- **[bug]** Cross-service latency spike between hospital-core and billing-service — distributed trace
- **[docs]** Write ADR: "Multi-region DR strategy for HIPAA-compliant RDS clusters"

---

## Repository structure

```
ai-task-agent/
├── agent/
│   ├── main.py              # Entry point — orchestrates daily logic
│   ├── task_generator.py    # LLM prompts → GitHub Issues
│   ├── github_client.py     # PyGithub wrapper
│   └── skills_tracker.py    # mithra-skills.yaml reader/writer
├── .github/
│   └── workflows/
│       └── daily-agent.yml  # Cron schedule: 9am EST weekdays
├── mithra-skills.yaml        # Skill tracker + monthly curriculum
├── requirements.txt
├── .gitignore
└── README.md
```

---

## Cost breakdown

| Component | Cost |
|-----------|------|
| GitHub Models API (GPT-4o mini) | **$0** — 150 req/day free |
| GitHub Actions | **$0** — ~30s/run, 2000 min/month free |
| GitHub Issues API | **$0** |
| **Total** | **$0/month** |
