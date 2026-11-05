# Evidencia manual de validación — Demo 3 (UC-009 y UC-010)

**Fecha de ejecución reportada:** 2026-11-04 (hora local Bolivia, UTC-04:00).  
**Rama bajo prueba:** `feature/8.3-melani-pedidos-catalogo`.  
**Base de datos usada en la sesión final:** PostgreSQL local `umss_market` (según configuración final del entorno).  
**Origen de los resultados:** salidas de PowerShell/Maven aportadas por quien ejecutó las pruebas. Este documento transcribe y resume esas salidas; no equivale a una ejecución de CI.  
**Datos sensibles:** no se registran contraseñas, secretos JWT ni valores de tokens.

## 1. Pruebas automatizadas (Spring Boot / Maven)

**Comando, tres clases nuevas:**

```powershell
cd backend/umss-market-api
.\mvnw.cmd "-Dtest=GetMyOrdersUseCaseTest,MyOrdersControllerTest,PagedPublicationsControllerTest" test
```

**Resultado reportado:**

```text
GetMyOrdersUseCaseTest              Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
MyOrdersControllerTest              Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
PagedPublicationsControllerTest     Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Comando, regresión del catálogo previo y GraphQL:**

```powershell
.\mvnw.cmd "-Dtest=PublicationControllerTest,SearchCatalogUseCaseTest,JpaCatalogQueryAdapterTest,GraphqlCatalogControllerTest" test
```

**Resultado reportado:** 29 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`. Incluye pruebas con base embebida H2.

**Suite completa:**

```powershell
.\mvnw.cmd test
```

**Resultado reportado:**

```text
Tests run: 265, Failures: 0, Errors: 0, Skipped: 9
BUILD SUCCESS
```

Esto representa **256 ejecutadas y aprobadas** y **9 omitidas**; las 9 omitidas corresponden a `RecommendationsPostgresTest`. No se afirma que esas 9 hayan pasado ni que exista una ejecución de CI.

## 2. UC-010 — Catálogo REST paginado

### Caso C-01: primera página sin JWT

```http
GET http://localhost:8080/api/catalog/publications?page=0&size=2
```

**Respuesta real reportada:** HTTP 200; `items` contiene 2 publicaciones. Metadatos:

```json
{
  "page": 0,
  "size": 2,
  "totalElements": 105,
  "totalPages": 53
}
```

Los registros incluyeron `id`, `storeId`, `nombre`, `descripcion`, `precio`, `tipo` y `stock`. En este extracto se omiten los UUID y el contenido detallado de las publicaciones, ya visibles en la salida original de PowerShell.

### Caso C-02: parámetro page inválido

```http
GET http://localhost:8080/api/catalog/publications?page=-1&size=2
```

**Respuesta real reportada:** HTTP 400.

**Límites de esta evidencia:** no se adjuntó salida HTTP de `page=1`, filtro combinado o consulta de catálogo vacío, aunque hay cobertura automatizada de validaciones y filtros.

## 3. UC-009 — Historial propio de pedidos con JWT

### Caso P-01: acceso anónimo

```http
GET http://localhost:8080/api/orders/me?status=PENDIENTE
```

**Respuesta real reportada:** **HTTP 403**. Se bloqueó el acceso sin JWT. No se debe registrar como 401: la configuración de Spring Security observada devolvió 403 y queda como detalle para revisión si se requiere semántica 401.

### Caso P-02: registro y acceso autenticado

Se registró un comprador sintético con `POST /api/auth/register/customer`, respuesta **HTTP 201**, y se inició sesión mediante `POST /api/auth/login`, obteniéndose JWT válido. No se guardan sus credenciales.

Consultas:

```http
GET http://localhost:8080/api/orders/me?status=PENDIENTE
GET http://localhost:8080/api/orders/me?status=CONFIRMADO
GET http://localhost:8080/api/orders/me?status=CANCELADO
Authorization: Bearer <TOKEN_OMITIDO>
```

**Resultado real:** HTTP 200 en los tres casos, respuesta `[]` antes de crear pedidos para el comprador nuevo.

### Caso P-03: transición real de un pedido y filtrado positivo

Se creó un pedido mediante `POST /api/orders` usando una publicación de pruebas con stock, y se actualizaron sus estados con `PATCH /api/orders/{id}/status`. En cada paso se consultó el endpoint nuevo con JWT válido y se comprobó que el pedido apareciera en el estado requerido.

**Pedido de prueba:** `12686619-61cb-437d-bf80-fbaa9b2b73ae`.

```text
APROBADO: PENDIENTE - Pedido encontrado
APROBADO: CONFIRMADO - Pedido encontrado
APROBADO: CANCELADO - Pedido encontrado
PRUEBAS DE ESTADOS COMPLETADAS
```

Al finalizar, el pedido quedó **CANCELADO**. Se usó un producto de prueba; las operaciones afectaron los datos de la base local.

### Caso P-04: exclusión por estado (filtrado negativo)

Con el mismo pedido ya cancelado se consultaron los tres estados: la búsqueda verificó **0 apariciones** para `PENDIENTE` y `CONFIRMADO`, y **1 aparición** para `CANCELADO`.

```text
APROBADO: filtro PENDIENTE
APROBADO: filtro CONFIRMADO
APROBADO: filtro CANCELADO
FILTRADO POR ESTADO VALIDADO
```

## 4. Alcance, riesgos y pendientes

- **Verificado:** UC-009 autenticado, consulta por tres estados, inclusión y exclusión del pedido de prueba; UC-010 primera página y validación de página negativa; compilación y suite Maven.
- **Observación:** la petición anónima a `/api/orders/me` devolvió 403, no 401. Cumple el bloqueo de acceso, pero debe revisarse si se desea un contrato 401 explícito.
- **Sin evidencia HTTP en esta sesión:** token falsificado/expirado, aislamiento entre dos usuarios reales, `status` inválido (400), segunda página y filtros combinados del catálogo.
- **Compatibilidad:** el endpoint anterior `GET /api/orders?userId=...` sigue fuera del nuevo contrato de autorización y requiere auditoría independiente; no se atribuye al feature nuevo.
- **No afirmar:** ejecución exitosa en contenedores, CI del release o merge a `release/8.3.0` antes de observarlos.
