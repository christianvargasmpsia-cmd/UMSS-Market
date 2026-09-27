from __future__ import annotations

from typing import Any


def create_alert(
    signal: str,
    severity: str,
    action: str,
    value: Any,
    threshold: Any
) -> dict[str, Any]:

    return {
        "signal": signal,
        "severity": severity,
        "action": action,
        "value": value,
        "threshold": threshold
    }


def evaluate_alerts(
    metrics: dict[str, Any],
    thresholds: dict[str, Any],
    attack_bursts: list[dict[str, Any]],
    fidelity_available: bool = False,
    fidelity_value: float | None = None
) -> list[dict[str, Any]]:

    alerts = []

    if metrics["prohibited_phrase_events"] > thresholds[
        "promesas_prohibidas_maximo"
    ]:

        alerts.append(
            create_alert(
                "promesas_prohibidas",
                "CRITICA",
                "revertir_version_o_limitar_usuario",
                metrics["prohibited_phrase_events"],
                thresholds["promesas_prohibidas_maximo"]
            )
        )

    if attack_bursts:
        alerts.append(
            create_alert(
                "bloqueos_por_usuario",
                "CRITICA",
                "revertir_version_o_limitar_usuario",
                attack_bursts,
                thresholds["bloqueos_por_usuario"]
            )
        )

    if fidelity_available and fidelity_value is not None:
        if fidelity_value < thresholds["fidelidad_promedio_minima"]:
            alerts.append(
                create_alert(
                    "fidelidad",
                    "ALTA",
                    "investigar_timeline",
                    fidelity_value,
                    thresholds["fidelidad_promedio_minima"]
                )
            )

    negative_rate = metrics["negative_feedback_rate"]

    if negative_rate is not None:
        if negative_rate > thresholds["feedback_negativo_maximo"]:
            alerts.append(
                create_alert(
                    "feedback_negativo",
                    "MEDIA",
                    "revisar_muestra_y_decidir",
                    negative_rate,
                    thresholds["feedback_negativo_maximo"]
                )
            )

    oos_rate = metrics["out_of_scope_rate"]

    if oos_rate is not None:
        if oos_rate > thresholds["fuera_de_alcance_maximo"]:
            alerts.append(
                create_alert(
                    "fuera_de_alcance",
                    "MEDIA",
                    "revisar_muestra_y_decidir",
                    oos_rate,
                    thresholds["fuera_de_alcance_maximo"]
                )
            )

    p95 = metrics["p95_latency_ms"]

    if p95 is not None:
        if p95 > thresholds["latencia_p95_maxima_ms"]:
            alerts.append(
                create_alert(
                    "latencia_p95",
                    "MEDIA",
                    "revisar_muestra_y_decidir",
                    p95,
                    thresholds["latencia_p95_maxima_ms"]
                )
            )

    return alerts