"""
TaskGenerator — uses GitHub Models API (GPT-4o-mini) to generate realistic
Cloud/DevOps/SRE tasks for Mithra on the NorthCare Health Platform.

Tasks are created as Jira issues in the SCRUM project.
"""
import json
import re
import random
from datetime import date

from openai import OpenAI


# Jira issue type per task category
TASK_TYPE_TO_JIRA = {
    "enhancement": "Story",
    "bug":         "Bug",
    "docs":        "Task",
    "learning":    "Task",
}


class TaskGenerator:
    # Service areas used in task descriptions (no longer tied to GitHub repos)
    SERVICE_AREAS = [
        "service-hospital-core",
        "infra-terraform",
        "platform-gitops",
        "service-billing",
        "service-telehealth",
    ]

    # Weighted task-type pool: 50% enhancement, 30% bug, 10% docs, 10% learning
    TASK_TYPE_POOL = (
        ["enhancement"] * 5
        + ["bug"] * 3
        + ["docs"] * 1
        + ["learning"] * 1
    )

    def __init__(self, github_models_api_key: str, skills: "SkillsTracker"):
        self.client = OpenAI(
            base_url="https://models.github.ai/inference",   # new endpoint (azure deprecated Oct 2025)
            api_key=github_models_api_key,
        )
        self.skills = skills
        self.model = "openai/gpt-4.1-mini"   # best available model on GitHub Models (Copilot Pro)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _call_llm(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.8,
            max_tokens=1500,
        )
        return response.choices[0].message.content

    def _extract_json(self, text: str) -> dict:
        """Pull the first JSON object out of an LLM response string."""
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if match:
            return json.loads(match.group())
        raise ValueError(f"No JSON object found in LLM response:\n{text}")

    # ------------------------------------------------------------------
    # Daily task generation
    # ------------------------------------------------------------------

    def generate_daily_tasks(self, recent_issues: list) -> list:
        """Generate 1–2 tasks based on weighted task-type distribution."""
        month = self.skills.get_current_month()
        difficulty = self.skills.get_difficulty_for_month(month)
        focus_areas = self.skills.get_focus_areas(month)
        task_type = random.choice(self.TASK_TYPE_POOL)
        service_area = random.choice(self.SERVICE_AREAS)

        system_prompt = (
            "You are a senior engineering manager at NorthCare Health Platform, "
            "a HIPAA-compliant hospital management system running on AWS EKS. "
            f"You assign realistic {difficulty}-level Cloud/DevOps/SRE tasks. "
            "The platform stack: Spring Boot 3, PostgreSQL, Kubernetes/EKS, "
            "Jenkins, ArgoCD, Terraform, Prometheus, Grafana, "
            "AWS (ECR, RDS, Secrets Manager, IAM/IRSA, EventBridge, Lambda). "
            "Generate tasks that build real, interview-worthy experience."
        )

        recent_titles = [i["title"] for i in recent_issues]
        user_prompt = f"""Generate a {task_type} task for today.
Month of training: {month} (difficulty: {difficulty})
Current focus areas: {focus_areas}
Service area: {service_area}
Recently resolved issues (avoid repeating): {recent_titles}

Return ONLY valid JSON with exactly these fields:
{{
  "title": "clear action-oriented title under 80 chars",
  "body": "detailed markdown body with these sections:\\n## Context\\n## Requirements\\n## Acceptance Criteria\\n## Learning Resources\\n## Expected Time",
  "labels": ["label1", "label2"],
  "issue_type": "{TASK_TYPE_TO_JIRA[task_type]}"
}}

Rules by task type:
- bug: embed a realistic error log / stack trace inside ## Context
- enhancement: include an ASCII sequence diagram or architecture description
- docs: reference a specific runbook or ADR template to be filled out
- learning: name a concrete AWS/K8s concept with a step-by-step hands-on task
"""
        raw = self._call_llm(system_prompt, user_prompt)
        task = self._extract_json(raw)
        task.setdefault("issue_type", TASK_TYPE_TO_JIRA[task_type])
        return [task]

    # ------------------------------------------------------------------
    # Sprint planning (Monday)
    # ------------------------------------------------------------------

    def create_sprint_plan(self, jira, recent_issues: list):
        month = self.skills.get_current_month()
        difficulty = self.skills.get_difficulty_for_month(month)
        focus_areas = self.skills.get_focus_areas(month)

        system_prompt = (
            "You are a Scrum Master for NorthCare Health Platform. "
            "Create concise, realistic 5-day sprint plans for a Cloud/DevOps/SRE engineer."
        )
        user_prompt = f"""It's Monday — create a sprint plan for Mithra.
Training month: {month} | Difficulty: {difficulty} | Focus: {focus_areas}
Recent context: {[i['title'] for i in recent_issues[:3]]}

Format as Markdown:
## Sprint Goal
One sentence.

## Daily Breakdown
| Day | Task | Est. Time | Service Area |
|-----|------|-----------|--------------|
(Mon through Fri rows, realistic SRE/DevOps tasks)

## Definition of Done
- bullet points

Keep it achievable for a solo engineer practising ~4h/day."""

        plan = self._call_llm(system_prompt, user_prompt)
        issue = jira.create_issue(
            summary=f"Sprint Plan — Week of {date.today().strftime('%B %d, %Y')}",
            description_md=plan,
            issue_type="Epic",
            labels=["sprint-plan", "weekly"],
        )
        print(f"📅 Sprint plan created: {issue['key']}")

    # ------------------------------------------------------------------
    # Sprint review (Friday)
    # ------------------------------------------------------------------

    def create_sprint_review(self, jira, closed_issues: list):
        system_prompt = (
            "You are a Scrum Master. Write sprint review summaries and LinkedIn post drafts "
            "for a Cloud/DevOps/SRE engineer building their portfolio."
        )
        user_prompt = f"""It's Friday — write a sprint review for Mithra.
Closed this week: {[i['title'] for i in closed_issues[:5]]}

Format as Markdown:
## What Was Completed
## What Carried Over
## Lessons Learned
## Skills Levelled Up
## LinkedIn Post Draft
(2–3 sentences, professional tone, highlights 1 key technical achievement, no cringe)"""

        review = self._call_llm(system_prompt, user_prompt)
        issue = jira.create_issue(
            summary=f"Sprint Review — {date.today().strftime('%B %d, %Y')}",
            description_md=review,
            issue_type="Task",
            labels=["sprint-review", "weekly"],
        )
        print(f"📊 Sprint review created: {issue['key']}")

    # ------------------------------------------------------------------
    # DEVLOG update (Friday) — placeholder
    # ------------------------------------------------------------------

    def update_devlog(self, jira):
        """Placeholder — future: append weekly summary to a Jira page via Confluence API."""
        pass

    # ------------------------------------------------------------------
    # Monthly: code review challenge (1st of month)
    # ------------------------------------------------------------------

    def create_intentional_bad_pr(self, jira):
        system_prompt = (
            "You are creating a code-review exercise for a Cloud/DevOps/SRE engineer. "
            "Generate a realistic-looking PR description that contains hidden bugs and security issues "
            "that the reviewer must catch. Do NOT list the issues in the PR body."
        )
        user_prompt = """Write a PR body for a code review exercise.
The PR pretends to fix a memory leak in the hospital-core patient-query cache service.
Hidden issues embedded in the diff description (don't name them explicitly):
1. Missing error handling on DB connection retry
2. Hardcoded AWS credentials in config block
3. Zero unit tests added
4. Unsanitised string used in raw SQL (injection vector)
5. The memory leak is not actually fixed — the cache object is still unbounded

Write as if this is a real PR from an eager junior team member.
Format:
## Summary
## Changes Made
## Testing Done
## Deployment Notes
## Screenshots / Logs"""

        pr_body = self._call_llm(system_prompt, user_prompt)
        issue = jira.create_issue(
            summary="CODE REVIEW CHALLENGE: Fix memory leak in patient query cache",
            description_md=(
                "> **[MONTHLY CODE REVIEW EXERCISE]** "
                "Find all issues before closing this ticket. "
                "There are at least 5. Good luck!\n\n"
                "---\n\n"
                + pr_body
                + "\n\n---\n"
                "_This is a monthly exercise generated by the NorthCare AI Task Agent._"
            ),
            issue_type="Bug",
            labels=["code-review-exercise", "monthly-challenge"],
        )
        print(f"🔍 Code review challenge created: {issue['key']}")

    # ------------------------------------------------------------------
    # Monthly: chaos day (15th of month)
    # ------------------------------------------------------------------

    def create_chaos_day(self, jira):
        system_prompt = (
            "You are a senior SRE at NorthCare Health Platform. "
            "Design a realistic AWS Fault Injection Simulator chaos day exercise "
            "for a Cloud/DevOps/SRE engineer at the intermediate-to-advanced level."
        )
        user_prompt = """Create a chaos engineering exercise for today.
Platform: AWS EKS, RDS PostgreSQL, ALB, Secrets Manager.
The engineer must design the experiment, run it in staging, observe metrics, and write a post-mortem.

Format as Markdown:
## Chaos Day Scenario
## Pre-conditions & Safety Checks
## Experiment Steps (using AWS FIS or manual fault injection)
## Observability Checkpoints (what to watch in Prometheus/Grafana/CloudWatch)
## Expected vs Actual Outcomes table
## Post-Mortem Template
## Learning Objectives"""

        chaos_body = self._call_llm(system_prompt, user_prompt)
        issue = jira.create_issue(
            summary=f"Chaos Day — {date.today().strftime('%B %Y')}",
            description_md=chaos_body,
            issue_type="Task",
            labels=["chaos-engineering", "monthly-challenge", "sre"],
        )
        print(f"🌪️ Chaos day created: {issue['key']}")
