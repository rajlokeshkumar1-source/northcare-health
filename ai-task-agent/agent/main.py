#!/usr/bin/env python3
"""
NorthCare AI Task Agent
Runs daily to assign realistic Cloud/DevOps/SRE tasks to Mithra.
"""
import sys
import os
from datetime import datetime, date
from task_generator import TaskGenerator
from github_client import GitHubClient
from skills_tracker import SkillsTracker


def main():
    print(f"🤖 NorthCare Task Agent starting — {datetime.now().strftime('%A %Y-%m-%d %H:%M')}")

    gh = GitHubClient(token=os.environ["GITHUB_TOKEN"])
    skills = SkillsTracker("mithra-skills.yaml")
    generator = TaskGenerator(
        github_models_api_key=os.environ["GITHUB_MODELS_API_KEY"],
        skills=skills
    )

    today = date.today()
    weekday = today.weekday()  # 0=Monday, 4=Friday

    # Read last 5 closed issues to avoid repetition
    recent_issues = gh.get_recent_closed_issues("northcare-health", "service-hospital-core", limit=5)

    # Monday: sprint planning
    if weekday == 0:
        generator.create_sprint_plan(gh, recent_issues)
    # Friday: sprint review
    elif weekday == 4:
        generator.create_sprint_review(gh, recent_issues)
        generator.update_devlog(gh)
    # Regular day: assign 1-2 tasks
    else:
        tasks = generator.generate_daily_tasks(recent_issues)
        for task in tasks:
            issue = gh.create_issue(
                org="northcare-health",
                repo=task["repo"],
                title=task["title"],
                body=task["body"],
                labels=task["labels"]
            )
            print(f"✅ Created issue #{issue['number']}: {task['title']}")

    # Monthly bad PR (1st of month)
    if today.day == 1:
        generator.create_intentional_bad_pr(gh)

    # Monthly chaos day (15th of month)
    if today.day == 15:
        generator.create_chaos_day(gh)

    print("✅ Agent completed successfully")


if __name__ == "__main__":
    main()
