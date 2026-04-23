"""
SkillsTracker — reads and writes mithra-skills.yaml to calibrate task difficulty.
"""
from datetime import date, datetime
from pathlib import Path

import yaml


class SkillsTracker:
    DIFFICULTY_MAP = {
        1: "beginner",
        2: "intermediate",
        3: "advanced",
        4: "senior",
    }

    def __init__(self, skills_file: str):
        self._path = Path(skills_file)
        self._data = self._load()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _load(self) -> dict:
        with self._path.open("r", encoding="utf-8") as fh:
            return yaml.safe_load(fh)

    def _save(self) -> None:
        with self._path.open("w", encoding="utf-8") as fh:
            yaml.dump(self._data, fh, default_flow_style=False, allow_unicode=True)

    # ------------------------------------------------------------------
    # Month / difficulty
    # ------------------------------------------------------------------

    def get_current_month(self) -> int:
        """Return 1–4 based on elapsed months since project_start_date.

        Falls back to the ``current_month`` value stored in the YAML if the
        start date hasn't been set yet or is in the future.
        """
        meta = self._data.get("meta", {})
        start_str = meta.get("project_start_date", "")
        try:
            start = datetime.strptime(start_str, "%Y-%m-%d").date()
            elapsed_months = (
                (date.today().year - start.year) * 12
                + (date.today().month - start.month)
            )
            month = max(1, min(4, elapsed_months + 1))
        except (ValueError, TypeError):
            month = int(meta.get("current_month", 1))
        return month

    def get_difficulty_for_month(self, month: int) -> str:
        """Return difficulty string for the given month (1-4)."""
        curriculum = self._data.get("monthly_curriculum", {})
        key = f"month_{month}"
        if key in curriculum:
            return curriculum[key].get("difficulty", self.DIFFICULTY_MAP.get(month, "beginner"))
        return self.DIFFICULTY_MAP.get(month, "beginner")

    def get_focus_areas(self, month: int) -> list[str]:
        """Return list of focus area strings for the given month."""
        curriculum = self._data.get("monthly_curriculum", {})
        key = f"month_{month}"
        if key in curriculum:
            return curriculum[key].get("focus", [])
        return []

    # ------------------------------------------------------------------
    # Skill mutations
    # ------------------------------------------------------------------

    def update_skill_level(self, skill: str, new_level: str) -> None:
        """Update a skill level in the skills section and persist to disk.

        ``skill`` can be a dotted path, e.g. ``"aws.eks"`` or ``"kubernetes.hpa"``.
        """
        valid_levels = {"beginner", "intermediate", "advanced", "senior"}
        if new_level not in valid_levels:
            raise ValueError(f"new_level must be one of {valid_levels}, got {new_level!r}")

        skills_section = self._data.setdefault("skills", {})
        parts = skill.split(".", 1)
        if len(parts) == 2:
            category, name = parts
            skills_section.setdefault(category, {})[name] = new_level
        else:
            skills_section[skill] = new_level

        self._save()

    # ------------------------------------------------------------------
    # Convenience accessors
    # ------------------------------------------------------------------

    def get_profile(self) -> dict:
        return self._data.get("profile", {})

    def get_all_skills(self) -> dict:
        return self._data.get("skills", {})
