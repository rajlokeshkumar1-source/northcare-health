"""
JiraClient — Jira REST API v3 wrapper for NorthCare AI Task Agent.

Connects to the SCRUM project on rajlokeshkumar1.atlassian.net and creates
issues assigned to Mithra.
"""
import json
import os
import re
import requests
from requests.auth import HTTPBasicAuth


# ── Constants ─────────────────────────────────────────────────────────────────

JIRA_BASE      = "https://rajlokeshkumar1.atlassian.net"
PROJECT_KEY    = "SCRUM"
MITHRA_ACCOUNT = "557058:7138211b-6e83-47fa-9800-c99925a6a030"

ISSUE_TYPE_MAP = {
    "Story": "10004",
    "Task":  "10003",
    "Bug":   "10007",
    "Epic":  "10001",
}


# ── Markdown → ADF converter ──────────────────────────────────────────────────

def _md_to_adf(text: str) -> dict:
    """Convert basic Markdown to Atlassian Document Format (ADF).

    Handles: # headings, - bullet lists, ```code blocks```, plain paragraphs.
    """
    nodes = []
    lines = text.split("\n")
    i = 0
    pending_bullets: list[str] = []

    def _text(s: str) -> dict:
        return {"type": "text", "text": s}

    def flush_bullets() -> None:
        if not pending_bullets:
            return
        nodes.append({
            "type": "bulletList",
            "content": [
                {
                    "type": "listItem",
                    "content": [{"type": "paragraph", "content": [_text(b)]}],
                }
                for b in pending_bullets
            ],
        })
        pending_bullets.clear()

    while i < len(lines):
        line = lines[i]

        # Headings
        if line.startswith("### "):
            flush_bullets()
            nodes.append({"type": "heading", "attrs": {"level": 3},
                          "content": [_text(line[4:])]})
        elif line.startswith("## "):
            flush_bullets()
            nodes.append({"type": "heading", "attrs": {"level": 2},
                          "content": [_text(line[3:])]})
        elif line.startswith("# "):
            flush_bullets()
            nodes.append({"type": "heading", "attrs": {"level": 1},
                          "content": [_text(line[2:])]})

        # Bullet list items
        elif re.match(r"^[-*] ", line):
            pending_bullets.append(line[2:].strip())

        # Fenced code block
        elif line.startswith("```"):
            flush_bullets()
            lang = line[3:].strip() or "plain"
            code_lines = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                code_lines.append(lines[i])
                i += 1
            if code_lines:
                nodes.append({
                    "type": "codeBlock",
                    "attrs": {"language": lang},
                    "content": [_text("\n".join(code_lines))],
                })

        # Table rows (render as paragraph to avoid complexity)
        elif line.startswith("|"):
            flush_bullets()
            clean = line.replace("|", " ").strip()
            if clean and not re.match(r"^[-\s]+$", clean):
                nodes.append({"type": "paragraph", "content": [_text(clean)]})

        # Horizontal rule
        elif re.match(r"^---+$", line.strip()):
            flush_bullets()
            nodes.append({"type": "rule"})

        # Blank line
        elif line.strip() == "":
            flush_bullets()

        # Normal paragraph
        else:
            flush_bullets()
            nodes.append({"type": "paragraph", "content": [_text(line)]})

        i += 1

    flush_bullets()

    if not nodes:
        nodes = [{"type": "paragraph", "content": [_text(" ")]}]

    return {"version": 1, "type": "doc", "content": nodes}


# ── JiraClient ────────────────────────────────────────────────────────────────

class JiraClient:
    def __init__(self, email: str, api_token: str):
        self._auth    = HTTPBasicAuth(email, api_token)
        self._headers = {"Content-Type": "application/json", "Accept": "application/json"}
        self._base    = JIRA_BASE

    def _get(self, path: str, params: dict | None = None) -> dict:
        resp = requests.get(
            f"{self._base}{path}", auth=self._auth,
            headers=self._headers, params=params, timeout=15
        )
        resp.raise_for_status()
        return resp.json()

    def _post(self, path: str, payload: dict) -> dict:
        resp = requests.post(
            f"{self._base}{path}", auth=self._auth,
            headers=self._headers, json=payload, timeout=15
        )
        resp.raise_for_status()
        return resp.json()

    # ── Issue operations ───────────────────────────────────────────────────────

    def create_issue(
        self,
        summary: str,
        description_md: str,
        issue_type: str = "Task",
        labels: list[str] | None = None,
        assignee_account_id: str = MITHRA_ACCOUNT,
    ) -> dict:
        """Create a Jira issue in the SCRUM project assigned to Mithra.

        Returns {"key": "SCRUM-N", "url": "https://..."}.
        """
        type_id = ISSUE_TYPE_MAP.get(issue_type, ISSUE_TYPE_MAP["Task"])

        payload: dict = {
            "fields": {
                "project":     {"key": PROJECT_KEY},
                "summary":     summary,
                "description": _md_to_adf(description_md),
                "issuetype":   {"id": type_id},
                "assignee":    {"accountId": assignee_account_id},
            }
        }

        if labels:
            # Jira labels must be single words (no spaces)
            payload["fields"]["labels"] = [
                lbl.replace(" ", "-") for lbl in labels
            ]

        data = self._post("/rest/api/3/issue", payload)
        key = data["key"]
        url = f"{self._base}/browse/{key}"
        print(f"  → {key}: {url}")
        return {"key": key, "url": url}

    def get_recent_resolved_issues(self, limit: int = 5) -> list[dict]:
        """Return recently resolved SCRUM issues (to avoid repetition)."""
        jql = (
            f'project = {PROJECT_KEY} AND status in ("Done","Closed","Resolved") '
            f'ORDER BY updated DESC'
        )
        data = self._get(
            "/rest/api/3/search",
            params={"jql": jql, "maxResults": limit, "fields": "summary,resolutiondate"},
        )
        return [
            {
                "key":    issue["key"],
                "title":  issue["fields"]["summary"],
                "closed_at": issue["fields"].get("resolutiondate", ""),
            }
            for issue in data.get("issues", [])
        ]
