# Demo 3 — Aportes individuales (Melani)

**Rama de trabajo:** `feature/8.3-melani-pedidos-catalogo`  
**Base inicial:** `release/8.2.0` (`e142632`).  
**Integracion objetivo:** `release/8.3.0`, rama ya creada por el equipo; integración mediante revisión de PR pendiente.  
**Autoria de commits:** `melrodriguez1133`.

Este documento describe exclusivamente los aportes de Melani; la infraestructura Docker, analisis E3/k6 y release son responsabilidades coordinadas del equipo, pero no se atribuyen a Melani.

## Feature 1 — UC-009: historial propio de pedidos filtrado por estado

- **FSD:** `docs/fsd/UC-009-historial-pedidos-filtrado.md`; extension de UC-002 Procesar Pedido y BR-006.
- **API:** `GET /api/orders/me` y `GET /api/orders/me?status=PENDIENTE`.
- **Comportamiento:** utiliza UUID autenticado desde el JWT; filtro opcional `PENDIENTE`, `CONFIRMADO` o `CANCELADO`. Devuelve lista de pedidos propios.
- **Criterios:** CA-01 a CA-07 del FSD UC-009.
- **Codigo:** `MyOrdersController`, `GetOrdersUseCase`, `SecurityConfig`.
- **Pruebas nuevas:** `GetMyOrdersUseCaseTest`, `MyOrdersControllerTest`.
- **Commit:** `8ad8b58`.

## Feature 2 — UC-010: listado REST paginado de publicaciones

- **FSD:** `docs/fsd/UC-010-catalogo-rest-paginado.md`; extension de UC-001 Gestionar Publicaciones.
- **API:** `GET /api/catalog/publications?page=0&size=10`.
- **Comportamiento:** filtro de texto, tipo, precio minimo/maximo, tienda, categoria, disponibilidad; pagina base cero, orden estable del adaptador y metadatos.
- **Criterios:** CA-01 a CA-07 del FSD UC-010.
- **Codigo:** `PagedPublicationsController`, `PagedPublicationItem`, `PagedPublicationsResponse`.
- **Pruebas nuevas:** `PagedPublicationsControllerTest`.
- **Commit:** `7e59b16`.

## Ejecutar pruebas en PowerShell

Desde la raiz del repositorio, despues de seleccionar la rama personal:

```powershell
git fetch origin
git switch feature/8.3-melani-pedidos-catalogo
git pull --ff-only origin feature/8.3-melani-pedidos-catalogo
cd backend/umss-market-api
./mvnw.cmd -Dtest=GetMyOrdersUseCaseTest,MyOrdersControllerTest,PagedPublicationsControllerTest test
```

Si `mvnw.cmd` no existe en la copia local, utilizar `mvn -Dtest=GetMyOrdersUseCaseTest,MyOrdersControllerTest,PagedPublicationsControllerTest test`.

Antes de integrar, ejecutar tambien las pruebas preexistentes relevantes:

```powershell
./mvnw.cmd -Dtest=PublicationControllerTest,SearchCatalogUseCaseTest,JpaCatalogQueryAdapterTest,GraphqlCatalogControllerTest test
```

## Secuencia de demostracion (Postman)

1. Login como comprador; copiar JWT.
2. Enviar `GET /api/orders/me` con `Authorization: Bearer <JWT>`.
3. Enviar `GET /api/orders/me?status=PENDIENTE` con el mismo JWT.
4. Repetir `GET /api/orders/me` sin JWT; comprobar error de autenticacion.
5. Enviar `GET /api/catalog/publications?page=0&size=2`.
6. Enviar `GET /api/catalog/publications?page=1&size=2`.
7. Combinar filtros: `GET /api/catalog/publications?tipo=PRODUCTO&page=0&size=2`.
8. Enviar `GET /api/catalog/publications?page=-1&size=2`; comprobar error 400.
9. Mostrar que `GET /api/publications` conserva el contrato anterior.

## Validacion ejecutada (2026-11-04)

Evidencia detallada y resultados observados: [Registro HTTP y Maven](evidencias/demo-3-melani-http-2026-11-04.md).

- Suite completa Maven: **265 contabilizadas, 256 aprobadas, 0 fallidas, 0 errores, 9 omitidas**; BUILD SUCCESS. No sustituye una ejecución de CI.
- Clases nuevas: **9/9**; regresión de catálogo y GraphQL: **29/29**.
- UC-010: página 0, tamaño 2: HTTP 200, 2 ítems, 105 publicaciones y 53 páginas. Página -1: HTTP 400.
- UC-009: registro sintético HTTP 201, login correcto, tres consultas de estado con JWT HTTP 200; seguimiento de un pedido de PENDIENTE a CONFIRMADO y CANCELADO, con inclusión positiva y exclusión negativa correctas.
- **Observación:** sin JWT, /api/orders/me respondió HTTP **403**, no 401. Acceso bloqueado; revisar semántica si se exige 401.
- **Pendientes:** prueba de aislamiento entre dos compradores, segunda página y filtros combinados, token inválido, estado inválido, CI y revisión/merge del PR.


**Nota de alcance:** hay otros endpoints antiguos de pedidos con `userId` explicito que no se modifican en esta demo. El endpoint nuevo usa identidad autenticada.
