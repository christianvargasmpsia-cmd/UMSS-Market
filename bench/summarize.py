
#!/usr/bin/env python3
"""Build a Markdown table with p95 and error rate from three k6 JSON exports."""

import argparse
import json
import statistics
from pathlib import Path


def metric_values(path: Path):
    with path.open(encoding="utf-8") as stream:
        data = json.load(stream)

    metrics = data.get("metrics", {})

    # k6 summary-export presenta las métricas directamente,
    # no dentro de una propiedad "values".
    duration = metrics.get("http_req_duration", {})
    failed = metrics.get("http_req_failed", {})
    requests_metric = metrics.get("http_reqs", {})

    p95 = duration.get("p(95)")
    error_rate = failed.get("value")
    requests = requests_metric.get("count", 0)

    if p95 is None or error_rate is None:
        raise ValueError(
            f"Missing p(95) or HTTP error rate in {path}"
        )

    return float(p95), float(error_rate) * 100, int(requests)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results_dir", type=Path)
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("bench/results/summary.md"),
    )
    args = parser.parse_args()

    paths = [
        args.results_dir / f"run{i}.json"
        for i in range(1, 4)
    ]

    missing = [str(path) for path in paths if not path.is_file()]
    if missing:
        raise SystemExit("Missing k6 summaries: " + ", ".join(missing))

    results = [metric_values(path) for path in paths]

    median_p95 = statistics.median(r[0] for r in results)
    median_errors = statistics.median(r[1] for r in results)
    median_requests = statistics.median(r[2] for r in results)

    lines = [
        "# k6 — resumen de tres corridas",
        "",
        "> Generado automáticamente desde los JSON de k6.",
        "",
        "| Corrida | Requests | p95 (ms) | Errores (%) |",
        "|---:|---:|---:|---:|",
    ]

    for index, (p95, errors, requests) in enumerate(results, start=1):
        lines.append(
            f"| {index} | {requests} | {p95:.2f} | {errors:.3f} |"
        )

    lines.extend([
        (
            f"| **Mediana** | **{median_requests:.0f}** "
            f"| **{median_p95:.2f}** | **{median_errors:.3f}** |"
        ),
        "",
        "## Interpretación técnica",
        "",
        (
            "Evaluar las mediciones considerando el entorno, ritmo, duración "
            "y límites configurados. Comparar p95, errores y volumen entre "
            "corridas. No atribuir causalidad a una única medición."
        ),
        "",
    ])

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(lines), encoding="utf-8")

    print(f"Mediana p95: {median_p95:.2f} ms")
    print(f"Mediana errores: {median_errors:.3f}%")
    print(f"Mediana requests: {median_requests:.0f}")


if __name__ == "__main__":
    main()
