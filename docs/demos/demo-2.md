# Demo 2 — UMSS Market — lunes 26/10/2026

**Release:** [release/8.2.0](https://github.com/christianvargasmpsia-cmd/UMSS-Market/tree/release/8.2.0)
**Estado:** preparación de evidencia. **No implica aprobación de CI ni ejecución de pruebas.**

| Integrante | Funcionalidad para presentar | Caso de uso FSD | Criterios de aceptación | Repositorio y evidencia individual |
| --- | --- | --- | --- | --- |
| Abad Melani Rodriguez Gonzales | Inventario concurrente (Issue #2; funcionalidad grande) | UC-002 — Procesar Pedido | No sobreventa; transacciones atómicas; solicitudes simultáneas consistentes; manejo de stock insuficiente; pruebas | [Repositorio del grupo](https://github.com/christianvargasmpsia-cmd/UMSS-Market) · [PR #8](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/8) |
| Christian Bernardo Vargas Sandoval | Detección semántica de duplicados (Issue #6; **pendiente de confirmar**) | UC-001 — Gestionar Publicaciones (verificar extensión FSD) | Compara en misma tienda; devuelve coincidencias y puntuación; respeta límites; no bloquea; errores controlados; pruebas | [Repositorio del grupo](https://github.com/christianvargasmpsia-cmd/UMSS-Market) · [Issue #6](https://github.com/christianvargasmpsia-cmd/UMSS-Market/issues/6) (añadir PR individual) |

### Control de entrega

- [ ] Confirmación explícita de las funcionalidades por cada integrante.
- [ ] Demostración de funcionalidades reales desde release/8.2.0.
- [ ] Contrato generado desde código y versionado (`openapi.json`).
- [ ] Workflow de CI en `.github/workflows/` con comparación de drift y Spectral.
- [ ] Endpoints REST demostrados declarados en OpenAPI; esquema GraphQL independiente.
- [ ] Plan de paginación con `EXPLAIN (ANALYZE, BUFFERS)`; explicar índices.
- [ ] Enlace de repositorio individual/PR por integrante y captura de evidencias.
- [ ] Tabla actualizada en Classroom.

### Evidencia técnica recomendada para la defensa

**Paginación GraphQL:** el método `JpaCatalogQueryAdapter.buscar` ya utiliza `PageRequest.of(pagina - 1, limite, Sort.by(createdAt DESC, id DESC))`, es paginación OFFSET. Comparar primera y última página en PostgreSQL con EXPLAIN sin atribuir resultados antes de ejecutar.

**OpenAPI:** el enunciado de Classroom lo requiere para el grupo. Las diapositivas del curso describen la actividad OpenAPI en CI como laboratorio demostrativo no evaluado de forma separada. Se deben satisfacer los requisitos del Classroom y prepararse para la pregunta sobre paginación/índices.
