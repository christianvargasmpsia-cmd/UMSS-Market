# Demo 3 — Release 8.3.0

## Integrante

Christian Bernardo Vargas Sandoval

## Funcionalidad 1 — Imagen y stack de producción con Docker

### Descripción

Implementación de un entorno reproducible para UMSS Market mediante Docker, utilizando un Dockerfile multi-stage y Docker Compose para levantar la API y PostgreSQL.

### Caso de uso del FSD

**CU-D3-01 — Levantar el entorno de UMSS Market**

El desarrollador necesita iniciar el backend y la base de datos de UMSS Market en un entorno reproducible, sin tener que configurar manualmente cada componente.

### Criterios de aceptación

- El Dockerfile utiliza una construcción multi-stage.
- Docker Compose levanta la API y PostgreSQL.
- El comando `docker compose up -d --wait` permite iniciar el stack.
- Los servicios cuentan con healthchecks.
- La API responde correctamente al endpoint de salud.
- La construcción de la imagen se revisa mediante Hadolint y Trivy.
- Los hallazgos críticos y altos se corrigen o justifican técnicamente.
- El stack puede ejecutarse desde un clon limpio.

### Evidencias

- Dockerfile.
- compose.yaml.
- Resultado de `docker compose ps`.
- Resultado del endpoint de salud.
- Reportes de Hadolint y Trivy.
- Captura de la ejecución del stack.
