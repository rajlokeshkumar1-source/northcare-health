"""
SlackClient — wrapper around Slack Web API for NorthCare check-in agent.

Sends DMs to Mithra, reads her replies, posts feedback.
"""
import time
import requests


class SlackClient:
    BASE = "https://slack.com/api"

    def __init__(self, bot_token: str):
        self._token = bot_token
        self._headers = {
            "Authorization": f"Bearer {bot_token}",
            "Content-Type": "application/json",
        }

    # ── Internal helpers ──────────────────────────────────────────────────────

    def _post(self, endpoint: str, payload: dict) -> dict:
        resp = requests.post(
            f"{self.BASE}/{endpoint}",
            headers=self._headers,
            json=payload,
            timeout=15,
        )
        resp.raise_for_status()
        data = resp.json()
        if not data.get("ok"):
            raise RuntimeError(f"Slack API error [{endpoint}]: {data.get('error')}")
        return data

    def _get(self, endpoint: str, params: dict) -> dict:
        headers = {"Authorization": f"Bearer {self._token}"}
        resp = requests.get(
            f"{self.BASE}/{endpoint}",
            headers=headers,
            params=params,
            timeout=15,
        )
        resp.raise_for_status()
        data = resp.json()
        if not data.get("ok"):
            raise RuntimeError(f"Slack API error [{endpoint}]: {data.get('error')}")
        return data

    # ── DM channel ────────────────────────────────────────────────────────────

    def open_dm(self, user_id: str) -> str:
        """Open (or return existing) DM channel with a user. Returns channel_id."""
        data = self._post("conversations.open", {"users": user_id})
        return data["channel"]["id"]

    # ── Messaging ─────────────────────────────────────────────────────────────

    def send_message(self, channel_id: str, text: str, blocks: list | None = None) -> str:
        """Send a message. Returns the message timestamp (thread anchor)."""
        payload: dict = {"channel": channel_id, "text": text}
        if blocks:
            payload["blocks"] = blocks
        data = self._post("chat.postMessage", payload)
        return data["ts"]

    def send_reply(self, channel_id: str, thread_ts: str, text: str) -> str:
        """Reply inside an existing thread."""
        data = self._post("chat.postMessage", {
            "channel": channel_id,
            "thread_ts": thread_ts,
            "text": text,
        })
        return data["ts"]

    # ── History ───────────────────────────────────────────────────────────────

    def get_dm_history(self, channel_id: str, oldest_ts: str | None = None, limit: int = 50) -> list[dict]:
        """Return messages in a DM channel, newest first."""
        params: dict = {"channel": channel_id, "limit": limit}
        if oldest_ts:
            params["oldest"] = oldest_ts
        data = self._get("conversations.history", params)
        return data.get("messages", [])

    def get_thread_replies(self, channel_id: str, thread_ts: str) -> list[dict]:
        """Return all replies in a thread (excludes the parent message)."""
        data = self._get("conversations.replies", {
            "channel": channel_id,
            "ts": thread_ts,
        })
        messages = data.get("messages", [])
        return messages[1:]  # skip parent

    # ── Utility ───────────────────────────────────────────────────────────────

    def now_ts(self) -> str:
        """Current Unix timestamp as string (for oldest= param)."""
        return str(time.time())

    def hours_ago_ts(self, hours: float) -> str:
        return str(time.time() - hours * 3600)
