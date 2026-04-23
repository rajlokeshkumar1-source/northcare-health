#!/usr/bin/env python3
"""
evening_review.py — Reads Mithra's Slack replies, evaluates them with GPT,
sends feedback, and posts a progress comment on the Jira ticket.

Flow:
  1. Find today's Jira task + morning metadata (channel_id, thread_ts, questions)
  2. Read Mithra's replies from the Slack thread
  3. If no replies — send a gentle reminder
  4. If replies exist — GPT evaluates answers, sends feedback DM, updates Jira

Run via GitHub Actions at 5pm weekdays.
"""
import os
import json
import sys
from pathlib import Path
from datetime import date

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "ai-task-agent" / "agent"))

from slack_client import SlackClient
from checkin_generator import CheckinGenerator
from jira_client import JiraClient


MITHRA_SLACK_USER_ID = os.environ["MITHRA_SLACK_USER_ID"]


def get_morning_meta(jira: JiraClient) -> dict | None:
    """Find today's morning metadata comment on the active Jira task."""
    import requests
    from requests.auth import HTTPBasicAuth

    BASE = "https://rajlokeshkumar1.atlassian.net"
    MITHRA_ACCOUNT = "557058:7138211b-6e83-47fa-9800-c99925a6a030"
    auth = HTTPBasicAuth(os.environ["JIRA_EMAIL"], os.environ["JIRA_API_TOKEN"])
    headers = {"Accept": "application/json", "Content-Type": "application/json"}

    # Find active task
    for status in ['"In Progress"', '"To Do"']:
        jql = (
            f'project = SCRUM AND assignee = "{MITHRA_ACCOUNT}" '
            f'AND status = {status} ORDER BY created ASC'
        )
        resp = requests.post(
            f"{BASE}/rest/api/3/search/jql",
            auth=auth, headers=headers,
            json={"jql": jql, "maxResults": 1, "fields": ["summary"]},
        )
        resp.raise_for_status()
        issues = resp.json().get("issues", [])
        if not issues:
            continue

        task_key = issues[0]["key"]
        task_summary = issues[0]["fields"]["summary"]

        # Fetch comments on this task
        resp2 = requests.get(
            f"{BASE}/rest/api/3/issue/{task_key}/comment",
            auth=auth, headers=headers,
        )
        resp2.raise_for_status()
        comments = resp2.json().get("comments", [])

        # Find today's AGENT_META comment
        today_str = date.today().isoformat()
        for comment in reversed(comments):
            created = comment.get("created", "")
            if not created.startswith(today_str):
                continue
            # Extract text from ADF body
            body_text = ""
            try:
                for block in comment["body"]["content"]:
                    for node in block.get("content", []):
                        if node.get("type") == "text":
                            body_text += node.get("text", "")
            except Exception:
                pass
            if "AGENT_META" in body_text:
                try:
                    json_start = body_text.index("{")
                    json_end   = body_text.rindex("}") + 1
                    meta = json.loads(body_text[json_start:json_end])
                    meta["task_summary"] = task_summary
                    return meta
                except Exception:
                    pass

    return None


def build_feedback_message(evaluation: dict) -> str:
    """Build a friendly evening feedback Slack message."""
    fb = evaluation["feedback"]
    lines = [
        f"🌆 *Evening feedback — {date.today().strftime('%A %d %b')}*\n",
        f"Score: *{evaluation['score']}* — {evaluation['overall']}\n",
        f"*Q1 feedback:* {fb[0]}",
        f"*Q2 feedback:* {fb[1]}",
        f"*Q3 feedback:* {fb[2]}",
        f"\n💡 *Tip for tomorrow:* {evaluation['tip']}",
        "\nKeep going Mithra, you're doing great! 🚀",
    ]
    return "\n".join(lines)


def main():
    print("🌆 Evening review agent starting...")

    slack = SlackClient(os.environ["SLACK_BOT_TOKEN"])
    jira  = JiraClient(os.environ["JIRA_EMAIL"], os.environ["JIRA_API_TOKEN"])
    gen   = CheckinGenerator(os.environ["GH_MODELS_API_KEY"])

    # 1. Get morning metadata
    meta = get_morning_meta(jira)
    if not meta:
        print("⚠️  No morning check-in metadata found for today — skipping.")
        return

    channel_id   = meta["slack_channel"]
    thread_ts    = meta["slack_ts"]
    questions    = meta["questions"]
    task_key     = meta["task_key"]
    task_summary = meta["task_summary"]
    print(f"  📋 Task: {task_key} — {task_summary}")

    # 2. Read replies from the Slack thread
    replies = slack.get_thread_replies(channel_id, thread_ts)
    user_replies = [r for r in replies if r.get("user") == MITHRA_SLACK_USER_ID]

    if not user_replies:
        # No reply — send a gentle reminder
        print("  ⚠️  No replies found — sending reminder...")
        reminder = gen.generate_reminder(task_summary)
        slack.send_reply(channel_id, thread_ts, reminder)
        print("  💬 Reminder sent.")
        jira.add_comment(
            task_key,
            f"🤖 Evening check: Mithra had not replied to today's check-in by 5pm. Reminder sent.",
        )
        return

    # 3. Concatenate all her replies
    answers_text = "\n".join(r.get("text", "") for r in user_replies)
    print(f"  💬 Found {len(user_replies)} reply message(s) from Mithra.")

    # 4. Evaluate answers
    print("  🤖 Evaluating answers...")
    evaluation = gen.evaluate_answers(task_summary, questions, answers_text)
    print(f"  📊 Score: {evaluation['score']}")

    # 5. Send feedback as a thread reply
    feedback_msg = build_feedback_message(evaluation)
    slack.send_reply(channel_id, thread_ts, feedback_msg)
    print("  💬 Feedback sent on Slack.")

    # 6. Post progress update on Jira
    jira_comment = (
        f"## 📊 Daily Check-in — {date.today().isoformat()}\n\n"
        f"**Score:** {evaluation['score']}\n\n"
        f"**Overall:** {evaluation['overall']}\n\n"
        f"**Q1 feedback:** {evaluation['feedback'][0]}\n\n"
        f"**Q2 feedback:** {evaluation['feedback'][1]}\n\n"
        f"**Q3 feedback:** {evaluation['feedback'][2]}\n\n"
        f"**Tip:** {evaluation['tip']}"
    )
    jira.add_comment(task_key, jira_comment)
    print(f"  ✅ Progress comment added to {task_key}")
    print("Evening review complete!")


if __name__ == "__main__":
    main()
