@'
# Monitoreo de IA — Evals en línea

## Objetivo

Implementar monitoreo online para UMSS Market mediante señales observables sobre el tráfico de interacción con el asistente.

## Señales monitoreadas

- Fidelidad promedio.
- Frases prohibidas.
- Feedback negativo.
- Consultas fuera de alcance.
- P95 de latencia.
- Bloqueos por usuario en una ventana temporal.

## Telemetría

Cada interacción utiliza un evento JSONL con:

- `ts`
- `usuario`
- `version`
- `pregunta`
- `respuesta`
- `fuentes`
- `camino`
- `herramienta`
- `guard`
- `latencia_ms`
- `tokens`
- `feedback`

No se registran contraseñas, tokens JWT, API keys ni credenciales.

## Umbrales

Los umbrales están versionados en:

`src/config.json`

Principales valores:

| Señal | Umbral |
|---|---:|
| Fidelidad mínima | 0.90 |
| Frases prohibidas máximas | 0 |
| Feedback negativo máximo | 0.15 |
| Fuera de alcance máximo | 0.15 |
| P95 máximo | 3000 ms |
| Bloqueos por usuario | 3 en 10 minutos |

## Severidad

| Severidad | Acción |
|---|---|
| CRITICA | revertir versión o limitar usuario |
| ALTA | investigar timeline |
| MEDIA | revisar muestra y decidir |

## Quality Gate

El monitor devuelve:

- `0`: tráfico aceptable.
- `1`: se detectó una condición que requiere revisión.

## Evidencia controlada

### Tráfico normal

Archivo:

`data/normal.jsonl`

Resultado:

```text
QUALITY GATE: 0