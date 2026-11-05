#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-http://localhost:8080}"
RATE="${RATE:-10}"
DURATION="${DURATION:-60s}"
OUT_DIR="${OUT_DIR:-bench/results}"
mkdir -p "$OUT_DIR"

for run in 1 2 3; do
  echo "=== k6 corrida ${run}/3 | ${BASE_URL} | ${RATE} req/s | ${DURATION} ==="
  if BASE_URL="$BASE_URL" RATE="$RATE" DURATION="$DURATION" \
    k6 run --summary-export "$OUT_DIR/run${run}.json" bench/catalog.js; then
    echo "Corrida ${run}: k6 terminó sin thresholds incumplidos."
  else
    echo "Corrida ${run}: k6 reportó threshold incumplido; se conserva el JSON y se continúa."
  fi
done

python3 bench/summarize.py "$OUT_DIR" --output "$OUT_DIR/summary.md"
echo "Resumen generado: $OUT_DIR/summary.md"
