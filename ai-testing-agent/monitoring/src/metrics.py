from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timedelta, timezone
from math import ceil
from typing import Any


def load_events(path: str) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []

    with open(path, "r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            line = line.strip()

            if not line:
                continue

            try:
                events.append(__import__("json").loads(line))
            except Exception as exc:
                raise ValueError(
                    f"Línea JSONL inválida {line_number}: {exc}"
                ) from exc

    return events


def parse_timestamp(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def percentile(values: list[float], percentage: float) -> float | None:
    if not values:
        return None

    ordered = sorted(values)

    position = (len(ordered) - 1) * percentage
    lower = int(position)
    upper = ceil(position)

    if lower == upper:
        return ordered[lower]

    weight = position - lower

    return (
        ordered[lower]
        + (ordered[upper] - ordered[lower]) * weight
    )


def p95_latency(events: list[dict[str, Any]]) -> float | None:
    values = []

    for event in events:
        value = event.get("latencia_ms")

        if isinstance(value, (int, float)):
            values.append(float(value))

    return percentile(values, 0.95)


def count_guard(events: list[dict[str, Any]], guard: str) -> int:
    return sum(
        1
        for event in events
        if event.get("guard") == guard
    )


def count_prohibited_phrases(
    events: list[dict[str, Any]],
    prohibited_phrases: list[str]
) -> int:

    total = 0

    phrases = [
        phrase.lower()
        for phrase in prohibited_phrases
    ]

    for event in events:
        question = str(event.get("pregunta") or "").lower()
        answer = str(event.get("respuesta") or "").lower()

        text = f"{question} {answer}"

        if any(phrase in text for phrase in phrases):
            total += 1

    return total


def feedback_negative_rate(
    events: list[dict[str, Any]]
) -> float | None:

    values = []

    for event in events:
        feedback = event.get("feedback")

        if feedback is None:
            continue

        normalized = str(feedback).strip().lower()

        if normalized in {"negative", "negativo", "bad", "down", "0"}:
            values.append(1)

        elif normalized in {"positive", "positivo", "good", "up", "1"}:
            values.append(0)

    if not values:
        return None

    return sum(values) / len(values)


def out_of_scope_rate(
    events: list[dict[str, Any]],
    out_of_scope_phrases: list[str]
) -> float | None:

    if not events:
        return None

    phrases = [
        phrase.lower()
        for phrase in out_of_scope_phrases
    ]

    count = 0

    for event in events:
        question = str(event.get("pregunta") or "").lower()

        if any(phrase in question for phrase in phrases):
            count += 1

    return count / len(events)


def blocked_attack_bursts(
    events: list[dict[str, Any]],
    max_blocks: int,
    window_minutes: int
) -> list[dict[str, Any]]:

    blocked = [
        event
        for event in events
        if str(event.get("guard", "")).startswith("BLOCK")
    ]

    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)

    for event in blocked:
        user = str(event.get("usuario") or "anonymous")
        grouped[user].append(event)

    alerts = []

    window = timedelta(minutes=window_minutes)

    for user, user_events in grouped.items():

        ordered = sorted(
            user_events,
            key=lambda event: parse_timestamp(event["ts"])
        )

        for index, first in enumerate(ordered):

            first_time = parse_timestamp(first["ts"])
            count = 0

            for current in ordered[index:]:
                current_time = parse_timestamp(current["ts"])

                if current_time - first_time <= window:
                    count += 1
                else:
                    break

            if count > max_blocks:
                alerts.append({
                    "usuario": user,
                    "bloqueos": count,
                    "ventana_minutos": window_minutes,
                    "desde": first["ts"]
                })
                break

    return alerts


def calculate_metrics(
    events: list[dict[str, Any]],
    prohibited_phrases: list[str],
    out_of_scope_phrases: list[str]
) -> dict[str, Any]:

    total = len(events)

    return {
        "interacciones": total,
        "p95_latency_ms": p95_latency(events),
        "prohibited_phrase_events": count_prohibited_phrases(
            events,
            prohibited_phrases
        ),
        "negative_feedback_rate": feedback_negative_rate(events),
        "out_of_scope_rate": out_of_scope_rate(
            events,
            out_of_scope_phrases
        ),
        "empty_input_blocks": count_guard(
            events,
            "BLOCK_EMPTY_INPUT"
        ),
        "long_input_blocks": count_guard(
            events,
            "BLOCK_INPUT_TOO_LONG"
        ),
        "blocked_events": sum(
            1
            for event in events
            if str(event.get("guard", "")).startswith("BLOCK")
        ),
        "tokens_available": sum(
            1
            for event in events
            if event.get("tokens") is not None
        ),
        "feedback_available": sum(
            1
            for event in events
            if event.get("feedback") is not None
        )
    }