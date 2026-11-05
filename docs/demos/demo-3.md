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

---

## Integrante: Melani (GitHub: `melrodriguez1133`)

**Compromiso individual:** dos funcionalidades del producto, con caso de uso y criterios de aceptación documentados en el FSD. Implementación y PR verificados en el release.

### Funcionalidad 1 — UC-009: Consultar mi historial de pedidos por estado

**Caso de uso FSD:** [UC-009 — Consultar mi historial de pedidos por estado](../fsd/UC-009-historial-pedidos-filtrado.md); extiende UC-002 Procesar Pedido y BR-006.

**Actor:** comprador autenticado. **Endpoint:** `GET /api/orders/me?status=PENDIENTE` (estado opcional).

**Criterios de aceptación resumidos:**
- El historial utiliza la identidad del JWT, no un identificador recibido como parámetro.
- Filtra por `PENDIENTE`, `CONFIRMADO` o `CANCELADO` y conserva la consulta sin filtro.
- Un comprador sin pedidos coincidentes recibe `HTTP 200 []`; un estado inválido produce `HTTP 400`.
- Las solicitudes sin autenticación son bloqueadas. En la prueba HTTP observada respondieron `403` (no `401`).

**Evidencia:** registro y login del comprador de prueba, creación de pedido y transiciones entre los tres estados; se verificó inclusión y exclusión por estado. Pruebas unitarias de caso de uso y controlador.

### Funcionalidad 2 — UC-010: Consultar catálogo REST paginado

**Caso de uso FSD:** [UC-010 — Consultar el catálogo con paginación REST](../fsd/UC-010-catalogo-rest-paginado.md); extiende UC-001 Gestionar Publicaciones.

**Actor:** visitante/comprador. **Endpoint:** `GET /api/catalog/publications?page=0&size=2`.

**Criterios de aceptación resumidos:**
- Paginación de base cero con tamaño entre 1 y 50, metadatos `items`, `page`, `size`, `totalElements` y `totalPages`.
- Filtros combinables de texto, tipo, precio, tienda, categoría y disponibilidad; orden determinista.
- Parámetros inválidos (por ejemplo `page=-1`) devuelven `HTTP 400`.
- Preserva el contrato anterior de `GET /api/publications` y no expone campos internos.

**Evidencia:** consulta HTTP 200 con 2 publicaciones, 105 registros totales y 53 páginas; `page=-1` dio HTTP 400. Incluye tests unitarios y regresión.

### Evidencias y trazabilidad

- [Resultados de pruebas Maven y HTTP](evidencias/demo-3-melani-http-2026-11-04.md).
- [Documento individual de Demo 3](demo-3-melani.md).
- [PR #13 — Integración a release/8.3.0](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/13), fusionado.
- Suite completa reportada: **265 contabilizadas, 256 ejecutadas/aprobadas, 0 fallidas, 0 errores y 9 omitidas**. Los escenarios HTTP no ejecutados se distinguen en el registro de evidencias.

**Alcance de la demostración:** el stack Docker/Compose y el análisis k6 corresponden a los aportes declarados por el otro integrante. Para la presentación se debe levantar la demo sobre ese stack, tal como indica la consigna del docente.
