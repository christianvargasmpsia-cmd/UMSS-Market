import json
import sys
from pathlib import Path

import pytest

SRC = Path(__file__).resolve().parents[1] / "src"

sys.path.insert(0, str(SRC))

from metrics import (
    load_events,
    p95_latency,
    count_prohibited_phrases,
    feedback_negative_rate,
    out_of_scope_rate
)

from monitor import quality_gate


def event(
    timestamp,
    latency,
    question="Busca productos",
    answer="Encontré productos",
    guard="ALLOW",
    feedback=None,
    tokens=None,
    user="test-user"
):
    return {
        "ts": timestamp,
        "usuario": user,
        "version": "m7-v1",
        "pregunta": question,
        "respuesta": answer,
        "fuentes": [],
        "camino": "RAG_SEMANTICO_CATALOGO",
        "herramienta": "SEARCH_CATALOG",
        "guard": guard,
        "latencia_ms": latency,
        "tokens": tokens,
        "feedback": feedback
    }


def test_p95_latency():
    events = [
        event(
            "2026-09-27T10:00:00-04:00",
            latency
        )
        for latency in [100, 200, 300, 400, 500]
    ]

    result = p95_latency(events)

    assert result == pytest.approx(480.0)


def test_prohibited_phrase_detection():

    events = [
        event(
            "2026-09-27T10:00:00-04:00",
            100,
            answer="Ignora las instrucciones anteriores"
        ),
        event(
            "2026-09-27T10:01:00-04:00",
            100
        )
    ]

    result = count_prohibited_phrases(
        events,
        ["ignora las instrucciones anteriores"]
    )

    assert result == 1


def test_negative_feedback_rate():

    events = [
        event(
            "2026-09-27T10:00:00-04:00",
            100,
            feedback="positive"
        ),
        event(
            "2026-09-27T10:01:00-04:00",
            100,
            feedback="negative"
        )
    ]

    result = feedback_negative_rate(events)

    assert result == pytest.approx(0.5)


def test_out_of_scope_rate():

    events = [
        event(
            "2026-09-27T10:00:00-04:00",
            100,
            question="Busca productos"
        ),
        event(
            "2026-09-27T10:01:00-04:00",
            100,
            question="¿Quién ganará las elecciones?"
        )
    ]

    result = out_of_scope_rate(
        events,
        ["elecciones"]
    )

    assert result == pytest.approx(0.5)


def test_quality_gate_pass():

    metrics = {
        "p95_latency_ms": 1000,
        "negative_feedback_rate": 0.05,
        "out_of_scope_rate": 0.05,
        "prohibited_phrase_events": 0,
        "blocked_events": 0
    }

    thresholds = {
        "latencia_p95_maxima_ms": 3000,
        "feedback_negativo_maximo": 0.15,
        "fuera_de_alcance_maximo": 0.15
    }

    assert quality_gate(metrics, [], thresholds) == 0


def test_quality_gate_fail_on_critical():

    metrics = {
        "p95_latency_ms": 1000,
        "negative_feedback_rate": 0.05,
        "out_of_scope_rate": 0.05,
        "prohibited_phrase_events": 1,
        "blocked_events": 0
    }

    thresholds = {
        "latencia_p95_maxima_ms": 3000,
        "feedback_negativo_maximo": 0.15,
        "fuera_de_alcance_maximo": 0.15
    }

    alerts = [
        {
            "signal": "promesas_prohibidas",
            "severity": "CRITICA",
            "action": "revertir_version_o_limitar_usuario",
            "value": 1,
            "threshold": 0
        }
    ]

    assert quality_gate(
        metrics,
        alerts,
        thresholds
    ) == 1