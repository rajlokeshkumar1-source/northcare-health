"""
CheckinGenerator — uses GPT-4.1-mini to:
  1. Generate 3 technical questions for Mithra based on her current Jira task
  2. Evaluate her answers and produce friendly feedback
"""
import json
from openai import OpenAI


class CheckinGenerator:
    SYSTEM_PROMPT = (
        "You are a friendly but rigorous Cloud/DevOps mentor coaching a complete beginner "
        "named Mithra who is learning through the NorthCare Health Platform project. "
        "Your goal is to verify real understanding, not just task completion. "
        "Be encouraging, specific, and practical."
    )

    def __init__(self, api_key: str):
        self.client = OpenAI(
            base_url="https://models.github.ai/inference",
            api_key=api_key,
        )
        self.model = "openai/gpt-4.1-mini"

    def _call_llm(self, prompt: str) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": self.SYSTEM_PROMPT},
                {"role": "user",   "content": prompt},
            ],
            temperature=0.7,
            max_tokens=1500,
        )
        return response.choices[0].message.content.strip()

    # ── Question generation ───────────────────────────────────────────────────

    def generate_questions(self, task_summary: str, task_description: str) -> dict:
        """Generate 3 progressive technical questions for today's task.

        Returns:
            {
              "intro": "...",
              "questions": ["Q1 text", "Q2 text", "Q3 text"],
              "task_title": "..."
            }
        """
        prompt = f"""
Mithra is working on this Jira task today:

TASK: {task_summary}

DESCRIPTION:
{task_description[:800]}

Generate exactly 3 technical questions to check her understanding BEFORE she completes the task.
Rules:
- Q1: Conceptual — test understanding of WHY (e.g. "What is the difference between X and Y?")
- Q2: Practical — test HOW (e.g. "Write the command to do X")
- Q3: NorthCare-specific — tie it back to the actual project (e.g. "In the hospital-core service, what would happen if...")

Respond ONLY with valid JSON in this format:
{{
  "intro": "A warm 1-sentence intro referencing today's task",
  "questions": ["Q1 text", "Q2 text", "Q3 text"]
}}
"""
        raw = self._call_llm(prompt)
        try:
            # strip markdown fences if present
            clean = raw.strip().strip("```json").strip("```").strip()
            data = json.loads(clean)
            data["task_title"] = task_summary
            return data
        except Exception:
            return {
                "intro": f"Quick check-in on today's task: {task_summary}",
                "questions": [
                    "Explain the key concept behind today's task in your own words.",
                    "What command or step are you most unsure about?",
                    "How does this task connect to the NorthCare project?",
                ],
                "task_title": task_summary,
            }

    # ── Answer evaluation ─────────────────────────────────────────────────────

    def evaluate_answers(
        self,
        task_summary: str,
        questions: list[str],
        answers_text: str,
    ) -> dict:
        """Evaluate Mithra's answers and return structured feedback.

        Returns:
            {
              "score": "3/3",
              "overall": "Great job! ...",
              "feedback": ["Feedback on Q1", "Feedback on Q2", "Feedback on Q3"],
              "tip": "One actionable tip for tomorrow"
            }
        """
        qs_formatted = "\n".join(f"Q{i+1}: {q}" for i, q in enumerate(questions))
        prompt = f"""
Mithra was working on: {task_summary}

The questions asked were:
{qs_formatted}

Her answers (raw Slack message):
\"\"\"{answers_text}\"\"\"

Evaluate her answers. Be honest but encouraging.
- If she didn't answer clearly, note it gently.
- Give a score out of 3 (one point per question).
- For each question, give 1-2 sentences of specific feedback.
- End with one practical tip for tomorrow.

Respond ONLY with valid JSON:
{{
  "score": "X/3",
  "overall": "1-2 sentence overall assessment",
  "feedback": ["feedback on Q1", "feedback on Q2", "feedback on Q3"],
  "tip": "One actionable tip"
}}
"""
        raw = self._call_llm(prompt)
        try:
            clean = raw.strip().strip("```json").strip("```").strip()
            return json.loads(clean)
        except Exception:
            return {
                "score": "?/3",
                "overall": "Thanks for your answers, Mithra! Keep it up.",
                "feedback": ["Good effort!", "Keep practicing.", "You're making progress!"],
                "tip": "Review the task acceptance criteria and try again tomorrow.",
            }

    # ── No-answer handling ────────────────────────────────────────────────────

    def generate_reminder(self, task_summary: str) -> str:
        """Generate a gentle reminder if Mithra hasn't replied."""
        prompt = (
            f"Write a short, friendly 2-sentence Slack reminder to Mithra. "
            f"She hasn't replied to this morning's check-in questions about: {task_summary}. "
            f"Be warm, not pressuring. End with an emoji."
        )
        return self._call_llm(prompt)
