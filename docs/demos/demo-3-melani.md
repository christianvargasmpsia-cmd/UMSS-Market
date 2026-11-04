# Demo 3 — Aportes individuales (Melani)

**Rama de trabajo:** `feature/8.3-melani-pedidos-catalogo`  
**Base inicial:** `release/8.2.0` (`e142632`).  
**Integracion objetivo:** `release/8.3.0`, pendiente de creacion por el equipo.  
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

**Verificaciones pendientes:** ejecucion local de Maven, pruebas HTTP reales con datos, seguridad por JWT, OpenAPI/CI y PR de integracion. No se afirma que los criterios esten aprobados hasta contar con dichas evidencias.

**Nota de alcance:** hay otros endpoints antiguos de pedidos con `userId` explicito que no se modifican en esta demo. El endpoint nuevo usa identidad autenticada.
