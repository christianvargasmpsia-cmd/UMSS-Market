# UC-009 — Consultar mi historial de pedidos por estado (Demo 3)

**Base:** FSD v4, UC-002 Procesar Pedido y BR-006 (historial de estados).  
**Actor:** comprador autenticado.  
**Endpoint nuevo:** `GET /api/orders/me?status=PENDIENTE`. El parametro `status` es opcional.

## Flujo
1. El comprador inicia sesion y envia su JWT.
2. El servidor obtiene el UUID del usuario autenticado, no del query string.
3. Filtra los pedidos de ese UUID; si se envio estado, aplica `findByUserIdAndStatus`.
4. Devuelve `List<OrderResponse>` con los productos asociados.

## Criterios de aceptacion
- CA-01: sin autenticacion no se puede consultar `/api/orders/me`.
- CA-02: no se acepta `userId` arbitrario en la nueva consulta; la identidad proviene del JWT.
- CA-03: sin estado se muestra el historial completo del comprador.
- CA-04: con `PENDIENTE`, `CONFIRMADO` o `CANCELADO` se filtra en repositorio por usuario y estado.
- CA-05: estado invalido devuelve error de validacion (HTTP 400).
- CA-06: un comprador sin pedidos coincidentes recibe `200 []`.
- CA-07: `GET /api/orders?userId=...` se mantiene para compatibilidad, fuera del nuevo contrato seguro; debe revisarse por separado su autorizacion.

## Evidencia y pruebas
- `GetOrdersUseCase`, `MyOrdersController`, `SecurityConfig`.
- `GetMyOrdersUseCaseTest` y `MyOrdersControllerTest`.
- Ejecutar pruebas y capturar resultados de `/api/orders/me` con JWT valido y sin token antes de marcar CA aprobados.

**Nota:** el filtrado actual trabaja con estado de pedido `PENDIENTE/CONFIRMADO/CANCELADO`, no con estado bancario.
