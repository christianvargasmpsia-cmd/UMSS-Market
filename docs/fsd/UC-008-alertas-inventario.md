# UC-008 — Consultar alertas de inventario de mi tienda (Demo 2)

**Actor:** emprendedor autenticado. **Extension de UC-001:** Gestionar Publicaciones.
**Objetivo:** saber cuales productos fisicos activos requieren reposicion sin consultar inventario ajeno.
**Nueva funcionalidad de release/8.2.0:** no reutiliza como entrega nueva la reserva concurrente del issue #2.

## Flujo principal
1. El emprendedor inicia sesion, obtiene su JWT y selecciona una tienda propia.
2. Solicita `GET /api/stores/{storeId}/inventory/low-stock?threshold=5&page=0&size=10`.
3. El backend verifica usuario, rol y propiedad de la tienda a partir del JWT; no confia en un userId enviado por el cliente.
4. Consulta productos fisicos activos con `stock BETWEEN 0 AND threshold`, excluyendo servicios y productos sin stock configurado.
5. Devuelve elementos ordenados por `createdAt DESC, id DESC`, `totalElements` y `totalPages`.
6. `GET /api/stores/{storeId}/inventory/summary?threshold=5` devuelve total criticos, agotados y con stock bajo positivo.

## Criterios de aceptacion
- **CA-01:** solo `ROLE_EMPRENDEDOR` autenticado puede acceder.
- **CA-02:** un emprendedor no puede consultar el inventario de otra tienda (403); tienda inexistente (404).
- **CA-03:** solo publicaciones activas de tipo PRODUCTO con stock no nulo y >=0.
- **CA-04:** threshold entre 0 y 1000000; page>=0; size entre 1 y 100 (400 en caso contrario).
- **CA-05:** paginacion determinista y metadatos de conteo; no se filtra ni se muestra informacion de otras tiendas.
- **CA-06:** resumen consistente: `productosCriticos=agotados+stockBajo`.
- **CA-07:** operaciones de lectura: nunca descuentan stock ni modifican el catalogo.
- **CA-08:** endpoint aparece en `openapi.json` generado del codigo, y el CI comprueba drift + Spectral.
- **CA-09:** demostrar planes reales PostgreSQL mediante `EXPLAIN (ANALYZE, BUFFERS)`; no reportar mejora sin datos.

## Pruebas
`InventoryAlertsIntegrationTest` cubre listado, paginacion, resumen, seguridad, validacion, `/api/auth/me` y lectura no destructiva (H2).
La exportacion OpenAPI comprueba el esquema `InventoryAlertItem` (publicationId, nombre, stock, estado), separado del tipo generico `Item` utilizado en checkout.
Pruebas de compatibilidad: `JpaPublicationAtomicReservationTest` (Issue #2) y `TransactionalOutboxTest` (Issue #3).

## Decisiones
- No se usa OpenAI ni Ollama: esta funcionalidad es determinista y gratuita.
- No se introduce interfaz web; la demostracion se realiza con Postman y PostgreSQL.
- REST para lectura simple; GraphQL sigue funcionando con su esquema propio, no se incluye en el contrato REST.
