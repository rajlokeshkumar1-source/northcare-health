#!/usr/bin/env python3
"""
NorthCare AI Task Agent
Runs daily to assign realistic Cloud/DevOps/SRE tasks to Mithra via Jira.
"""
import sys
import os
from datetime import datetime, date
from task_generator import TaskGenerator
from jira_client import JiraClient
from skills_tracker import SkillsTracker


def main():
    print(f"🤖 NorthCare Task Agent starting — {datetime.now().strftime('%A %Y-%m-%d %H:%M')}")

    jira = JiraClient(
        email=os.environ["JIRA_EMAIL"],
        api_token=os.environ["JIRA_API_TOKEN"],
    )
    skills = SkillsTracker("mithra-skills.yaml")
    generator = TaskGenerator(
        github_models_api_key=os.environ["GITHUB_MODELS_API_KEY"],
        skills=skills,
    )

    today = date.today()
    weekday = today.weekday()  # 0=Monday, 4=Friday

    # Read last 5 resolved issues to avoid repetition
    recent_issues = jira.get_recent_resolved_issues(limit=5)

    # Monday: sprint planning (Epic in Jira)
    if weekday == 0:
        generator.create_sprint_plan(jira, recent_issues)
    # Friday: sprint review + devlog
    elif weekday == 4:
        generator.create_sprint_review(jira, recent_issues)
        generator.update_devlog(jira)
    # Regular day: assign 1–2 tasks
    else:
        tasks = generator.generate_daily_tasks(recent_issues)
        for task in tasks:
            issue = jira.create_issue(
                summary=task["title"],
                description_md=task["body"],
                issue_type=task.get("issue_type", "Task"),
                labels=task.get("labels", []),
            )
            print(f"✅ Created {issue['key']}: {task['title']}")

    # Monthly code review challenge (1st of month)
    if today.day == 1:
        generator.create_intentional_bad_pr(jira)

    # Monthly chaos day (15th of month)
    if today.day == 15:
        generator.create_chaos_day(jira)

    print("✅ Agent completed successfully")


if __name__ == "__main__":
    main()
