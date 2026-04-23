#!/usr/bin/env python3
"""
morning_checkin.py — Sends Mithra her daily technical check-in on Slack.

Flow:
  1. Fetch her most recent in-progress Jira task
  2. Generate 3 technical questions via GPT
  3. Send a friendly DM on Slack
  4. Store the message ts + questions in a Jira comment (for evening retrieval)

Run via GitHub Actions at 9am weekdays.
"""
import os
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "ai-task-agent" / "agent"))

from slack_client import SlackClient
from checkin_generator import CheckinGenerator
from jira_client import JiraClient


MITHRA_SLACK_USER_ID = os.environ["MITHRA_SLACK_USER_ID"]


def get_todays_task(jira: JiraClient) -> dict | None:
    """Return Mithra's most recent sprint-1 task (in-progress first, then to-do)."""
    import requests
    from requests.auth import HTTPBasicAuth

    BASE = "https://rajlokeshkumar1.atlassian.net"
    MITHRA_ACCOUNT = "557058:7138211b-6e83-47fa-9800-c99925a6a030"
    auth = HTTPBasicAuth(os.environ["JIRA_EMAIL"], os.environ["JIRA_API_TOKEN"])
    headers = {"Accept": "application/json", "Content-Type": "application/json"}

    for status in ['"In Progress"', '"To Do"']:
        jql = (
            f'project = SCRUM AND assignee = "{MITHRA_ACCOUNT}" '
            f'AND status = {status} ORDER BY created ASC'
        )
        resp = requests.post(
            f"{BASE}/rest/api/3/search/jql",
            auth=auth, headers=headers,
            json={"jql": jql, "maxResults": 1, "fields": ["summary", "description", "status"]},
        )
        resp.raise_for_status()
        issues = resp.json().get("issues", [])
        if issues:
            issue = issues[0]
            # Extract plain text from ADF description
            desc = ""
            try:
                content = issue["fields"]["description"]["content"]
                for block in content:
                    for node in block.get("content", []):
                        if node.get("type") == "text":
                            desc += node.get("text", "") + " "
            except Exception:
                desc = ""
            return {
                "key": issue["key"],
                "summary": issue["fields"]["summary"],
                "description": desc.strip(),
                "status": issue["fields"]["status"]["name"],
            }
    return None


def build_slack_message(checkin_data: dict) -> tuple[str, list]:
    """Build plain text + blocks for the morning Slack message."""
    qs = checkin_data["questions"]
    task = checkin_data["task_title"]

    plain = (
        f"👋 Good morning Mithra!\n\n"
        f"Today's task: *{task}*\n\n"
        f"{checkin_data['intro']}\n\n"
        f"*Q1.* {qs[0]}\n"
        f"*Q2.* {qs[1]}\n"
        f"*Q3.* {qs[2]}\n\n"
        f"Reply with Q1:, Q2:, Q3: answers whenever you're ready 🙌"
    )

    blocks = [
        {"type": "header", "text": {"type": "plain_text", "text": "☀️ Daily Check-in — NorthCare Learning"}},
        {"type": "section", "text": {"type": "mrkdwn", "text": f"*Today's task:* {task}"}},
        {"type": "divider"},
        {"type": "section", "text": {"type": "mrkdwn", "text": checkin_data["intro"]}},
        {"type": "section", "text": {"type": "mrkdwn", "text": f"*Q1.* {qs[0]}"}},
        {"type": "section", "text": {"type": "mrkdwn", "text": f"*Q2.* {qs[1]}"}},
        {"type": "section", "text": {"type": "mrkdwn", "text": f"*Q3.* {qs[2]}"}},
        {"type": "divider"},
        {"type": "section", "text": {"type": "mrkdwn", "text": "Reply with *Q1:*, *Q2:*, *Q3:* answers whenever you're ready 🙌"}},
    ]

    return plain, blocks


def main():
    print("🌅 Morning check-in agent starting...")

    slack = SlackClient(os.environ["SLACK_BOT_TOKEN"])
    jira  = JiraClient(os.environ["JIRA_EMAIL"], os.environ["JIRA_API_TOKEN"])
    gen   = CheckinGenerator(os.environ["GH_MODELS_API_KEY"])

    # 1. Get today's task
    task = get_todays_task(jira)
    if not task:
        print("⚠️  No active task found for Mithra — skipping check-in.")
        return

    print(f"  📋 Task: {task['key']} — {task['summary']}")

    # 2. Generate questions
    print("  🤖 Generating questions...")
    checkin = gen.generate_questions(task["summary"], task["description"])

    # 3. Send Slack DM
    channel_id = slack.open_dm(MITHRA_SLACK_USER_ID)
    plain, blocks = build_slack_message(checkin)
    thread_ts = slack.send_message(channel_id, plain, blocks)
    print(f"  💬 Slack DM sent (ts={thread_ts})")

    # 4. Store thread_ts + questions in Jira comment (for evening retrieval)
    meta = json.dumps({
        "slack_channel": channel_id,
        "slack_ts": thread_ts,
        "questions": checkin["questions"],
        "task_key": task["key"],
    })
    jira.add_comment(
        task["key"],
        f"🤖 **AGENT_META** (do not edit)\n```\n{meta}\n```",
    )
    print(f"  ✅ Metadata stored on {task['key']}")
    print("Morning check-in complete!")


if __name__ == "__main__":
    main()
