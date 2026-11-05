# Feature 2 — Análisis de rendimiento con k6

El script y el runner están en `bench/`. La ruta medida es el catálogo público `GET /api/publications`.

## Procedimiento

1. Iniciar el stack: `docker compose up -d --wait`.
2. Confirmar que la API está saludable: `docker compose ps`.
3. Ejecutar las tres corridas: `bench/run-three.ps1` (Windows) o `bench/run-three.sh` (Linux/macOS).
4. Revisar `bench/results/run-1.json`, `run-2.json`, `run-3.json` y `summary.md`.
5. Registrar entorno y completar la interpretación con valores reales.

## Diseño de carga

- Executor: `constant-arrival-rate`.
- Configuración predeterminada: 10 solicitudes/segundo durante 60 segundos por corrida.
- VUs preasignados: 10; máximo: 30.
- Thresholds: p95 < 500 ms; errores HTTP < 1%; checks fallidos < 1%.
- Se realizan tres corridas separadas y se calcula la mediana del p95 y del porcentaje de errores.

## Tabla de resultados

El generador `bench/summarize.py` crea una tabla por corrida y una fila de mediana. Los JSON de k6 son la fuente de verdad. No introducir cifras manuales.

## Análisis a completar con resultados

Explicar:
- Si el p95 de cada corrida y la mediana cumplen el objetivo.
- Si el porcentaje de errores es cero o supera el límite.
- Si las corridas son consistentes o existe variación relevante.
- Condiciones del entorno (CPU/RAM, versión de Docker, base de datos, datos y perfil IA).
- Limitaciones: una sola ruta y un perfil de carga no representan todas las operaciones ni todos los usuarios reales.
