
# Demo 2 — UMSS Market — release/8.2.0

**Fecha:** 26 de octubre de 2026

**Release:** [release/8.2.0](https://github.com/christianvargasmpsia-cmd/UMSS-Market/tree/release/8.2.0)

**Estado:** funcionalidades desarrolladas y documentadas. La integración final de ambas implementaciones, la regeneración del contrato OpenAPI y el CI del Pull Request de inventario están pendientes de validación.

## 1. Funcionalidades nuevas para Demo 2

| Integrante | Funcionalidad | Caso de uso | Criterios de aceptación | Evidencia |
| --- | --- | --- | --- | --- |
| Abad Melani Rodriguez Gonzales | Alertas de inventario: stock bajo, productos agotados, resumen y paginación | [UC-008 — Alertas de inventario](../fsd/UC-008-alertas-inventario.md), extensión de UC-001 | CA-01 a CA-09; seguridad por tienda; filtros; paginación; pruebas automatizadas | `InventoryAlertsIntegrationTest`, OpenAPI, PostgreSQL y EXPLAIN. PR pendiente |
| Christian Bernardo Vargas Sandoval | Recomendaciones personalizadas de productos para compradores | M8-REC-01 — Obtener recomendaciones propias | Autenticación JWT, historial propio, ponderación, filtros, límite, aislamiento y fallbacks | [PR #10](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/10), [evidencia](demo-2-recommendations-evidence.md) |

La funcionalidad de Christian fue integrada en `release/8.2.0` mediante el PR #10.

La funcionalidad UC-008 está desarrollada en la rama `feature/8.2-inventory-alerts` y requiere completar su integración en el release.

## 2. Funcionalidad de Melani — UC-008: Alertas de inventario

### Objetivo

Permitir que un emprendedor consulte los productos de su tienda que se encuentran agotados o con existencias por debajo de un umbral configurable.

La funcionalidad permite identificar productos que requieren reposición y consultar información resumida del inventario.

### Arquitectura

La implementación utiliza:

- Java 21 y Spring Boot.
- API REST.
- Arquitectura por capas y puertos/adaptadores.
- Spring Data JPA.
- PostgreSQL.
- Spring Security y JWT.
- OpenAPI y Springdoc.
- JUnit y pruebas de integración.

### Endpoints

**Listado paginado de productos con stock bajo**

`GET /api/stores/{storeId}/inventory/low-stock`

Parámetros:

- `threshold`: umbral de stock permitido, entre 0 y 1000000.
- `page`: número de página, desde 0.
- `size`: cantidad de elementos por página, entre 1 y 100.

Ejemplo:

```http
GET /api/stores/{storeId}/inventory/low-stock?threshold=5&page=0&size=2
```

**Resumen de inventario**

`GET /api/stores/{storeId}/inventory/summary`

Ejemplo:

```http
GET /api/stores/{storeId}/inventory/summary?threshold=5
```

### Reglas de negocio

1. Solo se consideran publicaciones activas.
2. Solo se consideran publicaciones de tipo `PRODUCTO`.
3. Se incluyen productos con stock entre 0 y el umbral solicitado.
4. Se identifican productos agotados cuando su stock es 0.
5. Se identifican productos con stock bajo cuando su stock es positivo y no supera el umbral.
6. Cada emprendedor solamente puede consultar el inventario de las tiendas que le pertenecen.
7. Los parámetros inválidos deben ser rechazados.
8. La respuesta del listado incluye información de paginación.
9. El resumen permite conocer la cantidad de productos críticos y agotados.

### Criterios de aceptación

Los criterios detallados CA-01 a CA-09 se encuentran en:

[UC-008 — Alertas de inventario](../fsd/UC-008-alertas-inventario.md)

### Pruebas automatizadas

Se implementaron pruebas de integración en:

`InventoryAlertsIntegrationTest.java`

Se validan:

- Autenticación mediante JWT.
- Consulta de stock bajo.
- Resumen del inventario.
- Paginación.
- Restricciones de acceso por tienda.
- Validación de parámetros.
- Respuestas de error.

### Evidencia local

En la ejecución local anterior a la integración final se obtuvo:

```text
Tests run: 239, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La suite completa deberá ejecutarse nuevamente después de integrar los cambios de `release/8.2.0`.

### PostgreSQL e índices

Se creó el índice:

`idx_publications_inventory_alerts`

Su objetivo es apoyar las consultas de inventario filtradas por tienda, stock y publicaciones activas.

Archivos SQL:

- `docs/sql/demo2_inventory_indexes.sql`
- `docs/sql/demo2_inventory_seed.sql`

Se ejecutó `EXPLAIN ANALYZE` en PostgreSQL.

**Resultado observado:** PostgreSQL eligió un `Seq Scan` sobre una tabla pequeña de aproximadamente 120 publicaciones, con tiempo de ejecución cercano a 0.052 ms.

No se atribuye una mejora de rendimiento al índice sin evidencia comparativa.

### Demostración UC-008

1. Autenticar un emprendedor desde Postman.
2. Consultar el listado de productos con stock bajo.
3. Mostrar el stock y estado de cada producto.
4. Mostrar la página 0.
5. Mostrar la página 1, utilizando datos suficientes para demostrar paginación.
6. Consultar el resumen de inventario.
7. Enviar un umbral inválido y verificar el rechazo.
8. Intentar consultar una tienda ajena y verificar el control de acceso.
9. Mostrar las pruebas de integración.
10. Mostrar la consulta y el índice en PostgreSQL.

La demostración HTTP completa y la segunda página con productos distintos requieren evidencia final.

## 3. Funcionalidad de Christian — Recomendaciones personalizadas

### Caso de uso

**M8-REC-01 — Obtener recomendaciones propias**

Extensión funcional del Módulo 8, documentada en:

[Demo 2 — Evidencia de recomendaciones](demo-2-recommendations-evidence.md)

### Objetivo

Permitir que compradores autenticados descubran publicaciones disponibles que sean afines a sus interacciones anteriores.

### Endpoints

**Endpoint principal**

`GET /api/recommendations?limit=10`

**Endpoints de interacciones**

- `POST /api/interactions`
- `GET /api/interactions`
- `GET /api/interactions/{id}`
- `DELETE /api/interactions/{id}`

### Criterios de aceptación

1. El comprador debe estar autenticado mediante JWT.
2. Las recomendaciones utilizan únicamente el historial del usuario autenticado.
3. Se consideran las últimas 30 interacciones.
4. Los tipos de interacción influyen mediante ponderaciones:
   - `VIEW = 1`
   - `LIKE = 2`
   - `COMMENT = 2`
   - `FAVORITE = 3`
   - `PURCHASE = 4`
5. Solo se recomiendan publicaciones activas.
6. Se excluyen publicaciones sin stock disponible.
7. No se recomiendan nuevamente publicaciones utilizadas en el historial reciente considerado.
8. El parámetro `limit` acepta valores entre 1 y 50; su valor predeterminado es 10.
9. Sin historial útil se devuelve un catálogo general con `personalized=false`.
10. Cuando Ollama no está disponible se conserva el ranking y se devuelve `aiStatus=FALLBACK`.
11. Las interacciones ajenas no pueden consultarse ni eliminarse.

### Estrategia de recomendación

La afinidad se calcula de forma determinista mediante:

- Normalización del nombre y descripción de las publicaciones.
- Coincidencias con publicaciones previamente consultadas.
- Peso del tipo de interacción.
- Afinidad por tienda.
- Desempate determinista por UUID.

**Ollama no genera ni decide el ranking de productos.**

Su función es generar una explicación textual opcional sobre las recomendaciones.

### Pruebas y validación

Se documentaron las siguientes pruebas:

- `RecommendationsUnitTest`
- `RecommendationsIntegrationTest`
- `RecommendationsPostgresTest`
- `OllamaRecommendationAdapterTest`
- `RedTeamSecurityTest`
- `AIServiceImplTest`
- `OpenApiContractTest`

El aporte de Christian documenta:

**39 pruebas ejecutadas, 39 aprobadas y 0 fallos.**

Estos resultados corresponden a su validación individual, no a una ejecución posterior a la integración con UC-008.

Más información:

[Validación de recomendaciones](demo-2-recommendations-validation.md)

### Demostración de recomendaciones

1. Autenticar un comprador.
2. Consultar las recomendaciones iniciales.
3. Registrar una interacción.
4. Consultar nuevamente las recomendaciones.
5. Comparar resultados de compradores con historiales diferentes.
6. Demostrar el comportamiento sin historial.
7. Comprobar que no se recomiendan publicaciones agotadas o inactivas.
8. Validar los límites del parámetro `limit`.
9. Comprobar el aislamiento de interacciones por usuario.
10. Demostrar el fallback cuando Ollama no está disponible.
11. Mostrar los endpoints documentados en OpenAPI.

## 4. Mejoras técnicas de entregas anteriores

Las siguientes implementaciones forman parte de la arquitectura existente y se utilizan como evidencia complementaria.

### Issue #2 — Reserva concurrente de inventario

[PR #8](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/8)

La reserva atómica busca garantizar que:

- No se reserve más stock del disponible.
- Las operaciones concurrentes mantengan consistencia.
- Se rechacen reservas con stock insuficiente.
- El inventario y los pedidos permanezcan consistentes.

Prueba de referencia:

`JpaPublicationAtomicReservationTest`

### Issue #3 — Transactional Outbox

[PR #9](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/9)

Este patrón permite almacenar eventos de negocio de forma transaccional y procesarlos posteriormente.

Prueba:

`TransactionalOutboxTest`

Evidencia PostgreSQL previa:

- Evento: `ORDER_CREATED`.
- Estado: `PROCESSED`.
- Intentos registrados: `0`.

Los fallos simulados de `FlakyConsumer` en las pruebas no deben confundirse automáticamente con fallos del sistema en producción.

Estos aportes son complementarios y no sustituyen las nuevas funcionalidades de Demo 2.

## 5. Contrato OpenAPI y validación CI

El backend genera su contrato REST mediante Springdoc.

Archivo versionado:

`openapi.json`

El contrato integrado debe incluir, como mínimo:

**Inventario**

- `GET /api/stores/{storeId}/inventory/low-stock`
- `GET /api/stores/{storeId}/inventory/summary`

**Recomendaciones e interacciones**

- `GET /api/recommendations`
- `POST /api/interactions`
- `GET /api/interactions`
- `GET /api/interactions/{id}`
- `DELETE /api/interactions/{id}`

### Validación del contrato

El flujo CI debe:

1. Compilar el proyecto.
2. Ejecutar las pruebas automatizadas.
3. Generar el contrato OpenAPI desde el código.
4. Comparar el contrato generado con `openapi.json`.
5. Detectar diferencias o drift.
6. Ejecutar Spectral.
7. Rechazar errores bloqueantes.

Los workflows existentes deben revisarse conjuntamente:

- `.github/workflows/openapi.yml`
- `.github/workflows/openapi-contract.yml`

La configuración Spectral se encuentra en:

`.spectral.yaml`

**Pendiente:** regenerar y verificar el contrato único después de integrar las funcionalidades de ambos integrantes.

El esquema GraphQL se mantiene independiente del contrato REST OpenAPI.

## 6. Paginación, índices y EXPLAIN

### Inventario

UC-008 utiliza paginación para limitar la cantidad de productos devueltos en cada consulta.

Se recomienda demostrar la primera y segunda página con datos preparados en PostgreSQL.

### GraphQL

La implementación existente de `JpaCatalogQueryAdapter.buscar` utiliza:

`PageRequest.of(pagina - 1, limite, Sort.by(createdAt DESC, id DESC))`

Corresponde a paginación basada en OFFSET.

### Análisis SQL

Se debe mostrar `EXPLAIN (ANALYZE, BUFFERS)` y explicar:

- El plan de ejecución seleccionado.
- El número de filas procesadas.
- El uso de índices cuando corresponda.
- El tiempo de ejecución.
- La diferencia entre tablas pequeñas y grandes.

No se deben afirmar mejoras de rendimiento sin mediciones reproducibles.

## 7. Pruebas de carga opcionales

El escenario opcional de carga de inventario se encuentra en:

`scripts/k6-inventory-read.js`

Con k6 instalado y variables de entorno configuradas para una base de pruebas:

```powershell
$env:BASE_URL = 'http://localhost:8080'
$env:STORE_ID = 'UUID_TIENDA_DE_PRUEBAS'
$env:JWT = 'TOKEN_VALIDO_DE_PRUEBAS'

k6 run .\scripts\k6-inventory-read.js
```

Los resultados deben incluir, cuando corresponda:

- Latencia p50.
- Latencia p95.
- Latencia p99.
- Tasa de errores.
- Concurrencia utilizada.

Este escenario no sustituye las pruebas de concurrencia ni el análisis de PostgreSQL.

## 8. Checklist final de entrega

- [x] Funcionalidad UC-008 implementada.
- [x] Recomendaciones personalizadas implementadas.
- [x] PR #10 de recomendaciones integrado en `release/8.2.0`.
- [x] FSD y criterios de aceptación UC-008 documentados.
- [x] Suite local anterior a la integración: 239 pruebas aprobadas.
- [x] Índice de inventario creado en PostgreSQL.
- [x] EXPLAIN ANALYZE ejecutado y documentado.
- [x] Evidencia de Transactional Outbox procesado.
- [ ] Resolver los conflictos de integración.
- [ ] Regenerar el contrato OpenAPI incluyendo ambas funcionalidades.
- [ ] Ejecutar las pruebas sobre el código integrado.
- [ ] Validar drift y Spectral del contrato final.
- [ ] Confirmar las pruebas HTTP de inventario con JWT válido.
- [ ] Completar evidencia de paginación con dos páginas diferentes.
- [ ] Publicar la rama `feature/8.2-inventory-alerts`.
- [ ] Abrir el PR de inventario hacia `release/8.2.0`.
- [ ] Verificar GitHub Actions del PR.
- [ ] Integrar el PR después de validar el CI.
- [ ] Adjuntar evidencias y enlaces en Classroom.

## 9. Alcance de la entrega

La Demo 2 incluye las funcionalidades backend/API implementadas por ambos integrantes.

La integración final debe conservar:

- Alertas de inventario.
- Recomendaciones personalizadas.
- Seguridad JWT.
- Control de acceso por propietario.
- Persistencia de datos.
- Pruebas automatizadas.
- Contrato OpenAPI.
- Validaciones de CI.

La automatización de interacciones desde el frontend y nuevas interfaces visuales no forman parte del alcance demostrado por el aporte de recomendaciones.

La aprobación final depende de validar el código integrado y entregar las evidencias solicitadas por el docente.
