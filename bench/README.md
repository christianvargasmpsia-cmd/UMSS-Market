# Feature 2 — Prueba de rendimiento con k6

## Ruta bajo prueba

`GET /api/publications` (catálogo público).

La ruta está definida en `PublicationController` y no requiere autenticación para GET.

Puede agregarse un filtro reproducible mediante la variable `CATALOG_QUERY`.

## Requisitos

- UMSS-Market API ejecutándose y saludable.
- k6 instalado.
- Python 3.9+ para generar el resumen.

## Ejecución reproducible

### PowerShell (Windows)

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:RATE = "10"          # llegadas por segundo
$env:DURATION = "60s"    # duración de cada corrida
$env:CATALOG_QUERY = ""  # opcional: término de búsqueda

.\bench\run-three.ps1
```

### Linux/macOS

```sh
BASE_URL=http://localhost:8080 RATE=10 DURATION=60s ./bench/run-three.sh
```

El runner ejecuta tres corridas independientes y guarda los resultados JSON en `bench/results/`. También produce `summary.md` con p95, porcentaje de errores y mediana.

Las corridas que incumplan los thresholds conservan sus JSON para análisis.

## Métricas y límites

- **Ritmo:** `constant-arrival-rate`, 10 iteraciones/segundo por defecto.
- **Duración:** 60 segundos por corrida.
- **p95 de `http_req_duration`:** objetivo `< 500 ms`.
- **Error HTTP:** objetivo `< 1%`.
- **Checks de status 200 y `Content-Type` JSON:** objetivo `< 1%` de checks fallidos.

Los valores pueden ajustarse mediante variables de entorno. Registrar cualquier cambio al comparar resultados.

## Resultados del experimento

Se realizaron tres corridas sobre el catálogo público utilizando la configuración establecida.

| Corrida | Requests | p95 (ms) | Errores HTTP (%) |
|---:|---:|---:|---:|
| 1 | 601 | 6.05 | 0.000 |
| 2 | 601 | 4.79 | 0.000 |
| 3 | 601 | 4.62 | 0.000 |
| **Mediana** | **601** | **4.79** | **0.000** |

### Interpretación

Las tres corridas cumplieron los thresholds configurados para el experimento.

La mediana del percentil 95 (p95) fue de **4.79 ms**, con un porcentaje de errores HTTP de **0%** en las mediciones.

El p95 representa el tiempo bajo el cual finalizó el 95% de las solicitudes. El porcentaje de errores refleja solicitudes HTTP fallidas; los checks adicionales detectan respuestas que no sean HTTP 200 o JSON.

La mediana de las tres corridas reduce la influencia de una corrida atípica, pero no sustituye un análisis de variabilidad.

Los resultados corresponden al entorno local y a la configuración de carga utilizada. No representan una garantía de rendimiento en producción.

## Evidencias

Guardar los tres JSON y `summary.md`.

Los resultados individuales se encuentran en:

```text
bench/results/run1.json
bench/results/run2.json
bench/results/run3.json
```

El resumen consolidado se encuentra en:

```text
bench/results/summary.md
```

Agregar a la entrega:

- Los resultados JSON de las tres corridas.
- El resumen generado automáticamente.
- Una captura de consola de la ejecución.
- Una breve interpretación basada en los resultados reales.

No rellenar métricas manualmente.

## Reproducibilidad y análisis

Para comparar resultados, documentar:

- CPU y RAM del entorno.
- Versión de la imagen utilizada.
- Datos del catálogo.
- Concurrencia y ritmo configurados.
- Estado de Ollama durante las pruebas.

La metodología y configuración de la prueba deben mantenerse alineadas con los resultados generados.

