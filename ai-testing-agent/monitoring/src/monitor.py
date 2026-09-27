from __future__ import annotations

import argparse
import json
from pathlib import Path

from alerts import evaluate_alerts
from metrics import (
    calculate_metrics,
    blocked_attack_bursts,
    load_events
)


BASE_DIR = Path(__file__).resolve().parent
CONFIG_PATH = BASE_DIR / "config.json"


def load_config() -> dict:
    with open(CONFIG_PATH, "r", encoding="utf-8") as file:
        return json.load(file)


def print_summary(
    path: str,
    metrics: dict,
    alerts: list[dict]
) -> None:

    print("=" * 60)
    print("MONITOREO ONLINE - UMSS MARKET")
    print("=" * 60)

    print(f"Archivo: {path}")
    print(f"Interacciones: {metrics['interacciones']}")

    print()
    print("SEÑALES")
    print("-" * 60)

    print(
        f"P95 latencia: "
        f"{metrics['p95_latency_ms']} ms"
    )

    print(
        f"Frases prohibidas: "
        f"{metrics['prohibited_phrase_events']}"
    )

    print(
        f"Feedback negativo: "
        f"{metrics['negative_feedback_rate']}"
    )

    print(
        f"Fuera de alcance: "
        f"{metrics['out_of_scope_rate']}"
    )

    print(
        f"Bloqueos totales: "
        f"{metrics['blocked_events']}"
    )

    print(
        f"Bloqueos entrada vacía: "
        f"{metrics['empty_input_blocks']}"
    )

    print(
        f"Bloqueos entrada larga: "
        f"{metrics['long_input_blocks']}"
    )

    print(
        f"Eventos con tokens: "
        f"{metrics['tokens_available']}"
    )

    print(
        f"Eventos con feedback: "
        f"{metrics['feedback_available']}"
    )

    print()
    print("ALERTAS")
    print("-" * 60)

    if not alerts:
        print("Sin alertas.")
    else:
        for alert in alerts:
            print(
                f"[{alert['severity']}] "
                f"{alert['signal']} "
                f"| valor={alert['value']} "
                f"| umbral={alert['threshold']} "
                f"| accion={alert['action']}"
            )


def quality_gate(
    metrics: dict,
    alerts: list[dict],
    thresholds: dict
) -> int:

    critical = any(
        alert["severity"] == "CRITICA"
        for alert in alerts
    )

    if critical:
        return 1

    p95 = metrics["p95_latency_ms"]

    if p95 is not None:
        if p95 > thresholds["latencia_p95_maxima_ms"]:
            return 1

    negative = metrics["negative_feedback_rate"]

    if negative is not None:
        if negative > thresholds["feedback_negativo_maximo"]:
            return 1

    oos = metrics["out_of_scope_rate"]

    if oos is not None:
        if oos > thresholds["fuera_de_alcance_maximo"]:
            return 1

    return 0


def main() -> int:

    parser = argparse.ArgumentParser(
        description="Analizador de monitoreo online de UMSS Market"
    )

    parser.add_argument(
        "archivo",
        help="Archivo JSONL de tráfico"
    )

    args = parser.parse_args()

    config = load_config()

    events = load_events(args.archivo)

    metrics = calculate_metrics(
        events,
        config["prohibited_phrases"],
        config["out_of_scope_phrases"]
    )

    attack_bursts = blocked_attack_bursts(
        events,
        config["thresholds"]["bloqueos_por_usuario"]["maximo"],
        config["thresholds"]["bloqueos_por_usuario"]["ventana_minutos"]
    )

    alerts = evaluate_alerts(
        metrics,
        config["thresholds"],
        attack_bursts
    )

    gate = quality_gate(
        metrics,
        alerts,
        config["thresholds"]
    )

    print_summary(
        args.archivo,
        metrics,
        alerts
    )

    print()
    print("=" * 60)
    print(f"QUALITY GATE: {gate}")
    print("=" * 60)

    return gate


if __name__ == "__main__":
    raise SystemExit(main())