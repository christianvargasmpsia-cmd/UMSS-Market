# Implementación de las dos features del Módulo 4

Este paquete contiene cambios para Feature 1 (Docker/DevOps) y Feature 2 (k6).

## Aplicar

Extraer el contenido del paquete en la raíz del repositorio UMSS-Market y aceptar la sobrescritura de los archivos listados. No se incluye el código completo del repositorio.

## Feature 1

1. Revisar `.env.example`; opcionalmente copiarlo a `.env` y cambiar valores locales.
2. Ejecutar `docker compose config`.
3. Ejecutar `hadolint backend/umss-market-api/Dockerfile`.
4. Ejecutar `docker compose up -d --build --wait`.
5. Ejecutar `docker compose ps` y guardar la evidencia.
6. Ejecutar los comandos Trivy descritos en `docs/feature-1-docker-stack.md` y conservar los reportes.
7. Ejecutar la prueba del volumen descrita en esa documentación.

## Feature 2

Con el stack saludable y k6/Python instalados, en PowerShell:

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:RATE = "10"
$env:DURATION = "60s"
.\bench\run-three.ps1
```

El runner produce `bench/results/run-1.json`, `run-2.json`, `run-3.json` y `summary.md`. Interpretar las cifras reales y guardar captura de ejecución.

## Validación que falta ejecutar en la máquina del proyecto

El entorno de preparación no dispone de Docker, k6, Hadolint ni Trivy; por eso no se fabricaron logs, métricas ni resultados de escaneo. Los archivos de configuración, POM XML, YAML de Compose y generador de resumen sí se validaron estáticamente. La aceptación final requiere ejecutar los comandos anteriores en el entorno del usuario y conservar sus resultados.
