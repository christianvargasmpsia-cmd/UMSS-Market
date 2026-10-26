# Demo 2 — UMSS Market — lunes 26/10/2026

**Release:** [release/8.2.0](https://github.com/christianvargasmpsia-cmd/UMSS-Market/tree/release/8.2.0)
**Estado:** recomendaciones preparadas como backend/API; ver [validación reproducible](demo-2-recommendations-validation.md). El CI remoto se comprobará al abrir el PR; no se atribuye validación al aporte de otra integrante.

| Integrante | Funcionalidad para presentar | Caso de uso FSD | Criterios de aceptación | Repositorio y evidencia individual |
| --- | --- | --- | --- | --- |
| Abad Melani Rodriguez Gonzales | Inventario concurrente (Issue #2; funcionalidad grande) | UC-002 — Procesar Pedido | No sobreventa; transacciones atómicas; solicitudes simultáneas consistentes; manejo de stock insuficiente; pruebas | [Repositorio del grupo](https://github.com/christianvargasmpsia-cmd/UMSS-Market) · [PR #8](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/8) |
| Christian Bernardo Vargas Sandoval | Recomendaciones personalizadas de productos para compradores (funcionalidad grande, confirmada) | M8-REC-01 — Obtener recomendaciones propias; extensión M8 documentada en la guía | JWT; historial propio; pesos; disponibilidad; exclusión de vistos; límites; fallback; aislamiento | [Repositorio según git remote](https://github.com/christianvargasmpsia-cmd/Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales/tree/release/8.2.0) · [Guía y criterios](demo-2-recommendations-evidence.md) · PR individual pendiente de abrir |

### Control de entrega

- [ ] Confirmación explícita de las funcionalidades por cada integrante.
- [ ] Demostración de funcionalidades reales desde release/8.2.0.
- [x] Contrato generado desde código y versionado (`openapi.json`); recomendaciones e interacciones incluidas.
- [x] Workflow de CI en `.github/workflows/openapi.yml` con comparación de drift y Spectral (ejecución remota pendiente del PR).
- [ ] Endpoints REST demostrados declarados en OpenAPI; esquema GraphQL independiente.
- [ ] Plan de paginación con `EXPLAIN (ANALYZE, BUFFERS)`; explicar índices.
- [ ] Enlace de repositorio individual/PR por integrante y captura de evidencias.
- [ ] Tabla actualizada en Classroom.

### Evidencia técnica recomendada para la defensa

**Paginación GraphQL:** el método `JpaCatalogQueryAdapter.buscar` ya utiliza `PageRequest.of(pagina - 1, limite, Sort.by(createdAt DESC, id DESC))`, es paginación OFFSET. Comparar primera y última página en PostgreSQL con EXPLAIN sin atribuir resultados antes de ejecutar.

**OpenAPI:** el enunciado de Classroom lo requiere para el grupo. Las diapositivas del curso describen la actividad OpenAPI en CI como laboratorio demostrativo no evaluado de forma separada. Se deben satisfacer los requisitos del Classroom y prepararse para la pregunta sobre paginación/índices.

### Christian Vargas

**Funcionalidad:** Recomendaciones personalizadas de productos para compradores.

**Caso de uso:** M8-REC-01 — Obtener recomendaciones propias (extensión funcional M8;
no confundir con UC-006 de análisis de tests del FSD antiguo).
El backend calcula afinidad por contenido y tienda a partir de las últimas 30
interacciones propias. Ollama redacta una explicación opcional; no decide el ranking.

**Endpoint principal:** `GET /api/recommendations?limit=10`.
**Auxiliares:** `POST /api/interactions`, `GET /api/interactions`,
`GET /api/interactions/{id}`, `DELETE /api/interactions/{id}` y login JWT existente.

**Criterios:** los diez criterios verificables se detallan en
[la guía de evidencia](demo-2-recommendations-evidence.md#criterios-de-aceptación):
autenticación, historial propio, pesos VIEW=1/LIKE=2/COMMENT=2/FAVORITE=3/PURCHASE=4,
publicaciones activas, stock positivo, exclusión del historial reciente,
limit 1..50, fallback sin historial útil, fallback de Ollama y privacidad de interacciones.

**Pruebas:** unitarias, integración H2, repetición de integración en PostgreSQL,
adaptador HTTP, seguridad/red team, servicio de chat y contrato OpenAPI.
Resultados exactos en [validación](demo-2-recommendations-validation.md).

**Evidencia esperada:** login real, interacción → cambio de ranking; A tecnología y B
deporte; C sin historial; exclusión de agotado/inactivo; límites; intento de lectura
ajena 404; fallback de IA; Swagger; reportes de pruebas, Spectral y control de drift.
No se presenta frontend ni captura automática. La confirmación de Melani, su evidencia
y el registro en Classroom permanecen bajo su control; no se alteró su aporte.
