# Monitoreo online - UMSS Market

Este módulo analiza los eventos JSONL generados por el backend de UMSS Market.

## Entrada

El backend genera:

backend/umss-market-api/logs/ai-monitoring.jsonl

Cada línea representa una interacción.

## Señales

El monitor calcula:

- número de interacciones
- p95 de latencia
- frases prohibidas
- feedback negativo cuando está disponible
- fuera de alcance
- bloqueos
- disponibilidad de tokens
- disponibilidad de feedback

## Alertas

### CRÍTICA

- frases prohibidas
- ráfagas de bloqueos por usuario

Acción:

revertir versión o limitar usuario.

### ALTA

- fidelidad por debajo del umbral cuando exista evidencia suficiente.

Acción:

investigar timeline.

### MEDIA

- feedback negativo
- fuera de alcance
- latencia p95

Acción:

revisar muestra y decidir.

## Quality Gate

El monitor devuelve:

0 = PASS

1 = FAIL

## Ejecución

Desde:

ai-testing-agent/monitoring

Ejecutar:

python src/monitor.py "../../backend/umss-market-api/logs/ai-monitoring.jsonl"

## Tests

pytest tests/test_monitor.py