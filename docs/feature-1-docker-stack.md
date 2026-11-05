# Feature 1 — Imagen y stack reproducible de UMSS-Market

## Alcance

- Dockerfile multi-stage: Maven/Java 21 compila el backend; JRE 21 ejecuta el JAR.
- Usuario de runtime no root.
- `compose.yaml`: API y PostgreSQL; Ollama queda en perfil opcional `ai`.
- PostgreSQL persiste en el volumen nombrado `postgres_data`.
- Healthchecks de PostgreSQL y API permiten `docker compose up -d --wait`.
- Actuator expone únicamente el endpoint `health`.

## Inicio desde clon limpio

```sh
docker compose up -d --wait
docker compose ps
```

El stack básico no requiere descargar modelos de Ollama. Para habilitar el runtime de IA:

```sh
docker compose --profile ai up -d --wait
```

Descargar modelos y habilitar `IA_HABILITADA=true` según la configuración de Ollama antes de probar funcionalidades de IA.

## Persistencia

PostgreSQL utiliza el volumen nombrado `postgres_data`. Para recrear contenedores sin perder los datos:

```sh
docker compose down
docker compose up -d --wait
```

**No usar `docker compose down -v`** si se necesita conservar la base de datos.

## Configuración

Copiar `.env.example` a `.env` para personalizar puertos y credenciales locales. Los valores de fallback de Compose son únicamente para desarrollo local; cambiar contraseñas y `APP_JWT_SECRET` antes de cualquier despliegue real.

## Validación de Dockerfile

```sh
hadolint backend/umss-market-api/Dockerfile
docker compose config
docker compose build --no-cache api
docker compose up -d --wait
docker compose ps
```

## Escaneo Trivy

```sh
trivy image --severity HIGH,CRITICAL --exit-code 1 umss-market-api:feature1
trivy image --format json --output evidencia/trivy-api-feature1.json umss-market-api:feature1
trivy image --severity HIGH,CRITICAL --exit-code 1 postgres:16-alpine
trivy image --format json --output evidencia/trivy-postgres-feature1.json postgres:16-alpine
trivy config --severity HIGH,CRITICAL --exit-code 1 compose.yaml
```

Si el primer comando devuelve código distinto de cero, revisar cada CVE, confirmar si afecta al paquete/versión presente y aplicar actualización de imagen o dependencia cuando corresponda. Si un hallazgo se mantiene, documentar CVE, componente, alcance, mitigación, razón de aceptación y fecha de revisión. No declarar “sin hallazgos” sin guardar la salida del escaneo.

## Evidencias por guardar

- Salida de `docker compose up -d --wait` y `docker compose ps`.
- Salida de Hadolint.
- Reportes JSON de Trivy para API y PostgreSQL, y salida del escaneo de configuración.
- Resumen de hallazgos HIGH/CRITICAL: componente/CVE, alcance, acción aplicada o justificación, y estado final.
- Prueba de persistencia: con valores locales predeterminados, ejecutar `docker compose exec db psql -U umss_market -d umss_market -c "CREATE TABLE IF NOT EXISTS feature1_persistence_probe(id integer PRIMARY KEY); INSERT INTO feature1_persistence_probe VALUES (1) ON CONFLICT DO NOTHING;"`; recrear contenedores con `docker compose down` (sin `-v`) y `docker compose up -d --wait`; verificar con `docker compose exec db psql -U umss_market -d umss_market -c "SELECT * FROM feature1_persistence_probe;"`. Eliminar la tabla de prueba al terminar.
