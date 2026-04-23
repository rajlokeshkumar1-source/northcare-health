"""
GitHubClient — thin wrapper around PyGithub for NorthCare AI Task Agent.
"""
from github import Github, GithubException


class GitHubClient:
    def __init__(self, token: str):
        self._gh = Github(token)

    # ------------------------------------------------------------------
    # Issue operations
    # ------------------------------------------------------------------

    def create_issue(
        self,
        org: str,
        repo: str,
        title: str,
        body: str,
        labels: list[str] | None = None,
    ) -> dict:
        """Create a GitHub issue and return {number, url}."""
        repository = self._gh.get_repo(f"{org}/{repo}")
        label_objects = []
        for label_name in (labels or []):
            label_objects.append(self._get_or_create_label(repository, label_name))

        issue = repository.create_issue(
            title=title,
            body=body,
            labels=label_objects,
        )
        return {"number": issue.number, "url": issue.html_url}

    def get_recent_closed_issues(
        self,
        org: str,
        repo: str,
        limit: int = 5,
    ) -> list[dict]:
        """Return the most recently closed issues as a list of dicts."""
        repository = self._gh.get_repo(f"{org}/{repo}")
        closed = repository.get_issues(state="closed", sort="updated", direction="desc")
        results = []
        for issue in closed:
            if len(results) >= limit:
                break
            results.append(
                {
                    "number": issue.number,
                    "title": issue.title,
                    "closed_at": issue.closed_at.isoformat() if issue.closed_at else None,
                }
            )
        return results

    def get_open_issues_count(self, org: str, repo: str) -> int:
        """Return the number of currently open issues in a repo."""
        repository = self._gh.get_repo(f"{org}/{repo}")
        return repository.open_issues_count

    # ------------------------------------------------------------------
    # Label helpers
    # ------------------------------------------------------------------

    def add_label_to_repo(
        self,
        org: str,
        repo: str,
        label_name: str,
        color: str = "ededed",
    ) -> None:
        """Create a label in the repo if it doesn't already exist."""
        repository = self._gh.get_repo(f"{org}/{repo}")
        self._get_or_create_label(repository, label_name, color)

    def _get_or_create_label(self, repository, label_name: str, color: str = "ededed"):
        """Return an existing label object or create it."""
        # Map common label names to recognisable colours
        color_map = {
            "bug": "d73a4a",
            "enhancement": "a2eeef",
            "docs": "0075ca",
            "learning": "e4e669",
            "sprint-plan": "0e8a16",
            "sprint-review": "0e8a16",
            "weekly": "bfd4f2",
            "code-review-exercise": "f9d0c4",
            "monthly-challenge": "e99695",
            "chaos-engineering": "b60205",
            "sre": "5319e7",
        }
        resolved_color = color_map.get(label_name, color)
        try:
            return repository.get_label(label_name)
        except GithubException:
            return repository.create_label(name=label_name, color=resolved_color)
