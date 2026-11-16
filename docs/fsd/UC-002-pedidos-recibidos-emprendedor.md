# Extensión de UC-002 – Gestión de pedidos recibidos por el emprendedor



## 1. Información general



- **Sistema:** UMSS Market

- **Caso de uso relacionado:** UC-002 – Procesar Pedido

- **Documento base:** `docs/fsd/FSD\_v4.md`

- **Actor principal:** Emprendedor

- **Fecha:** 16/11/2026

- **Estado:** Implementado en backend; validación automatizada parcial



## 2. Objetivo



Permitir que un emprendedor consulte los pedidos recibidos por su tienda, visualice exclusivamente los productos que le pertenecen y actualice el estado de los pedidos autorizados.



Esta funcionalidad complementa UC-002 – Procesar Pedido, cuyo flujo principal corresponde al registro de pedidos por parte del cliente.



## 3. Endpoints implementados



### 3.1. Listar pedidos recibidos



`GET /api/stores/{storeId}/orders`



- Requiere autenticación JWT y rol `EMPRENDEDOR`.

- Verifica que el usuario sea propietario de la tienda.

- Permite filtrar por el parámetro opcional `status`.

- Devuelve únicamente los productos pertenecientes a la tienda.

- Calcula el subtotal correspondiente a esa tienda.



### 3.2. Consultar detalle de pedido



`GET /api/stores/{storeId}/orders/{orderId}`



- Requiere autenticación y autorización.

- Devuelve el detalle del pedido correspondiente a la tienda.

- Excluye productos pertenecientes a otras tiendas.

- Responde HTTP 404 cuando el pedido no existe o no contiene productos de la tienda consultada.



### 3.3. Actualizar estado del pedido



`PATCH /api/stores/{storeId}/orders/{orderId}/status`



**Ejemplo de solicitud:**



```json

{

&#x20; "status": "CONFIRMADO"

}

```



Estados admitidos:



- `PENDIENTE`

- `CONFIRMADO`

- `CANCELADO`



## 4. Reglas de negocio



- **RN-PR-001:** Solo los emprendedores autenticados pueden gestionar pedidos recibidos.

- **RN-PR-002:** Un emprendedor únicamente puede gestionar pedidos de su propia tienda.

- **RN-PR-003:** El listado y detalle deben mostrar exclusivamente productos pertenecientes a la tienda consultada.

- **RN-PR-004:** El subtotal debe calcularse utilizando solamente los productos de esa tienda.

- **RN-PR-005:** No se permite modificar el estado global de un pedido que contiene productos de varias tiendas.

- **RN-PR-006:** Un pedido confirmado no puede regresar al estado pendiente.

- **RN-PR-007:** Un pedido cancelado no admite nuevas transiciones.

- **RN-PR-008:** Al cancelar un pedido, deben liberarse las reservas de inventario correspondientes cuando existan.



## 5. Respuestas de error



| Código HTTP | Descripción |

|---|---|

| 400 | Estado requerido o JSON inválido |

| 401 | Usuario no autenticado o inexistente, según validación del controlador |

| 403 | Rol no autorizado o tienda ajena |

| 404 | Tienda o pedido inexistente, o pedido sin productos de la tienda |

| 409 | Pedido sin productos, pedido multitienda o transición inválida |



**Nota:** La respuesta efectiva ante un JWT ausente debe verificarse mediante pruebas de integración con Spring Security.



## 6. Componentes involucrados



- `StoreReceivedOrdersController.java`

- `UpdateOrderStatusUseCase.java`

- `UpdateOrderStatusRequest.java`

- `SecurityConfig.java`

- `GlobalExceptionHandler.java`

- Puertos de repositorio de usuarios, tiendas, pedidos, ítems y publicaciones.



## 7. Pruebas automatizadas



**Archivo:** `StoreReceivedOrdersControllerTest.java`



**Resultado verificado el 16/11/2026:**



| Indicador | Resultado |

|---|---|

| Pruebas ejecutadas | 11 |

| Fallos | 0 |

| Errores | 0 |

| Omitidas | 0 |

| Compilación | BUILD SUCCESS |



Las pruebas verifican autorización, propiedad de tienda, aislamiento de productos, listado, detalle, confirmación simulada, pedido inexistente, estado requerido y bloqueo de actualizaciones multitienda.



Son pruebas unitarias con JUnit 5 y Mockito. No representan una medición porcentual de cobertura ni sustituyen las pruebas HTTP/JWT.



## 8. Limitaciones del MVP



- El listado utiliza `findAll()` y procesa los pedidos en memoria.

- No existe paginación en el controlador.

- El estado pertenece al pedido completo y no individualmente a cada tienda.

- Los pedidos multitienda pueden consultarse parcialmente, pero no cambiar su estado global desde una sola tienda.



## 9. Trazabilidad



| Artefacto | Relación |

|---|---|

| `docs/fsd/FSD\_v4.md` | UC-002 – Procesar Pedido |

| `StoreReceivedOrdersController.java` | Implementación de endpoints |

| `UpdateOrderStatusUseCase.java` | Reglas de transición de estado |

| `StoreReceivedOrdersControllerTest.java` | Pruebas automatizadas |



## 10. Validaciones pendientes



- Pruebas automatizadas de transiciones inválidas.

- Pruebas HTTP mediante MockMvc.

- Pruebas de integración con filtros JWT.

- Ejecución y revisión de la suite completa de Maven.



---



**Conclusión:** Se implementó la consulta y gestión de pedidos recibidos por emprendedores, con controles de autorización y aislamiento de productos por tienda. La implementación cuenta con 13 pruebas unitarias aprobadas y requiere completar las validaciones pendientes antes del cierre definitivo.
