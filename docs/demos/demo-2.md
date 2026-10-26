# Demo 2 — UMSS Market — lunes 26/10/2026

**Release:** [release/8.2.0](https://github.com/christianvargasmpsia-cmd/UMSS-Market/tree/release/8.2.0)
**Estado:** recomendaciones preparadas como backend/API; ver [validación reproducible](demo-2-recommendations-validation.md). El CI remoto se comprobará al abrir el PR; no se atribuye validación al aporte de otra integrante.

| Integrante | Funcionalidad para presentar | Caso de uso FSD | Criterios de aceptación | Repositorio y evidencia individual |
| --- | --- | --- | --- | --- |
| Abad Melani Rodriguez Gonzales | Inventario concurrente (Issue #2; funcionalidad grande) | UC-002 — Procesar Pedido | No sobreventa; transacciones atómicas; solicitudes simultáneas consistentes; manejo de stock insuficiente; pruebas | [Repositorio del grupo](https://github.com/christianvargasmpsia-cmd/UMSS-Market) · [PR #8](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/8) |
| Christian Bernardo Vargas Sandoval | Recomendaciones personalizadas de productos para compradores (funcionalidad grande) | M8-REC-01: Obtener recomendaciones propias | JWT; historial propio; ponderación de interacciones; publicaciones disponibles; exclusión del historial reciente; límite 1..50; fallback sin historial; fallback de Ollama; aislamiento por usuario |     [Issue #4](https://github.com/christianvargasmpsia-cmd/UMSS-Market/issues/4) · [PR #10](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/10) · [Evidencia](demo-2-recommendations-evidence.md) |

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

**Paginación GraphQL:** el método `JpaCatalogQueryAdapter.buscar` utiliza `PageRequest.of(pagina - 1, limite, Sort.by(createdAt DESC, id DESC))`, correspondiente a paginación OFFSET. Para la defensa se debe comparar una página inicial y una página profunda en PostgreSQL mediante `EXPLAIN (ANALYZE, BUFFERS)` y explicar los índices involucrados.

**OpenAPI:** el contrato REST se genera desde Spring Boot y se versiona como `openapi.json`. El workflow `.github/workflows/openapi.yml` ejecuta las pruebas, genera el contrato, rechaza diferencias entre el contrato generado y el versionado y ejecuta Spectral con errores bloqueantes.

### Christian Vargas

**Funcionalidad:** Recomendaciones personalizadas de productos para compradores.

**Caso de uso:** M8-REC-01 — Obtener recomendaciones propias, extensión funcional del Módulo 8.

El backend calcula afinidad por contenido y tienda utilizando las últimas 30 interacciones del comprador autenticado. Ollama puede generar una explicación textual opcional, pero no interviene en el ranking de los productos.

**Endpoint principal:**

`GET /api/recommendations?limit=10`

**Endpoints auxiliares:**

- `POST /api/interactions`
- `GET /api/interactions`
- `GET /api/interactions/{id}`
- `DELETE /api/interactions/{id}`
- Login JWT existente.

### Criterios de aceptación

1. El usuario debe estar autenticado mediante JWT.
2. Las recomendaciones utilizan únicamente el historial del usuario autenticado.
3. Se consideran las últimas 30 interacciones.
4. Los tipos de interacción tienen los siguientes pesos:
   - `VIEW = 1`
   - `LIKE = 2`
   - `COMMENT = 2`
   - `FAVORITE = 3`
   - `PURCHASE = 4`
5. Solo se recomiendan publicaciones activas.
6. Se excluyen publicaciones con stock nulo, cero o negativo.
7. No se repiten publicaciones referenciadas en el historial reciente utilizado.
8. El parámetro `limit` acepta valores entre 1 y 50 y utiliza 10 por defecto.
9. Sin historial útil se devuelve catálogo general con `personalized=false`.
10. Si Ollama no está disponible, se conserva el ranking y se devuelve `aiStatus=FALLBACK`.
11. Las interacciones pertenecientes a otros usuarios no pueden consultarse ni eliminarse.

### Estrategia de recomendación

La afinidad se calcula de forma determinista utilizando:

- palabras normalizadas del nombre y descripción de las publicaciones;
- coincidencias con publicaciones previamente interactuadas;
- peso según tipo de interacción;
- afinidad por tienda;
- desempate determinista por UUID.

El ranking **no es generado por Ollama**.

Ollama únicamente puede generar una explicación textual sobre por qué las recomendaciones podrían ser relevantes.

### Pruebas y validación

Se validó mediante:

- `RecommendationsUnitTest`
- `RecommendationsIntegrationTest`
- `RecommendationsPostgresTest`
- `OllamaRecommendationAdapterTest`
- `RedTeamSecurityTest`
- `AIServiceImplTest`
- `OpenApiContractTest`

Resultado del conjunto relevante:

**39 pruebas ejecutadas — 39 aprobadas — 0 fallos.**

La validación incluye PostgreSQL real, autenticación JWT, aislamiento por usuario, límites, fallbacks y contrato OpenAPI.

Los resultados reproducibles se encuentran en:

[demo-2-recommendations-validation.md](demo-2-recommendations-validation.md)

### Evidencia para la demostración

Durante la defensa se puede demostrar:

1. Login de un comprador.
2. Recomendaciones iniciales.
3. Registro de una interacción mediante `POST /api/interactions`.
4. Cambio en las recomendaciones después de registrar la interacción.
5. Compradores con historiales distintos reciben resultados distintos.
6. Usuario sin historial devuelve `personalized=false`.
7. Publicaciones agotadas o inactivas no son recomendadas.
8. Validación de `limit`.
9. Intento de consultar una interacción ajena devuelve `404`.
10. Fallback cuando Ollama no está disponible.
11. Endpoints documentados en OpenAPI/Swagger.
12. Pipeline de GitHub Actions con pruebas, control de drift y Spectral.

### Alcance actual

La Demo 2 corresponde al backend y API REST.

Las interacciones que alimentan el recomendador actualmente se registran mediante la API de interacciones. La captura automática de vistas, favoritos y compras desde el frontend queda como evolución posterior.

No se presenta un frontend nuevo como parte de este aporte.