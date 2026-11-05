# UC-010 — Consultar el catalogo con paginacion REST (Demo 3)

**Base:** FSD v4, UC-001 Gestionar Publicaciones (extension de consulta del catalogo).  
**Actor:** visitante / comprador.  
**Endpoint nuevo:** `GET /api/catalog/publications?page=0&size=10`.

## Flujo principal
1. El visitante consulta el catalogo sin autenticarse.
2. Combina opcionalmente filtros `texto`, `tipo`, `precioMin`, `precioMax`, `storeId`, `categoria`, `disponible`.
3. El backend valida filtros, pagina base cero y tamanio.
4. Reutiliza `CatalogQueryPort.buscar` con paginacion real en JPA, ya utilizada por GraphQL.
5. Devuelve `items`, `page`, `size`, `totalElements` y `totalPages`.
6. No cambia `GET /api/publications`, cuya respuesta antigua sigue siendo una lista.

## Criterios de aceptacion
- CA-01: pagina base cero; `page` de 0 a 99999 y `size` de 1 a 50.
- CA-02: permite combinar filtros sin leer publicaciones inactivas ni tiendas inactivas (reglas existentes del adaptador).
- CA-03: orden determinista por `createdAt DESC, id DESC` (adaptador existente).
- CA-04: metadatos expresan total real de elementos y paginas, incluso con resultados vacios.
- CA-05: pagina y limites invalidos, precios negativos o rango invertido dan HTTP 400.
- CA-06: no devuelve embeddings ni campos internos del modelo.
- CA-07: el endpoint `GET /api/publications` conserva su contrato previo de lista.

## Implementacion y pruebas
- `PagedPublicationsController`, `PagedPublicationItem`, `PagedPublicationsResponse`.
- Reutiliza `CatalogQueryPort` y `JpaCatalogQueryAdapter` sin modificaciones.
- `PagedPublicationsControllerTest` verifica pagina, conversion base cero/uno, filtros, metadatos, seguridad de DTO y errores.
- Revalidar tambien `JpaCatalogQueryAdapterTest`, `GraphqlCatalogControllerTest`, `PublicationControllerTest`.

## Demo prevista
- `GET /api/catalog/publications?page=0&size=2` muestra primera pagina.
- `GET /api/catalog/publications?page=1&size=2` muestra segunda pagina.
- `GET /api/catalog/publications?texto=pan&tipo=PRODUCTO&page=0&size=2` muestra filtros.
- `GET /api/catalog/publications?page=-1&size=2` devuelve 400.
- `GET /api/publications` mantiene respuesta `[]` o lista antigua (no se altera).

**Estado:** codigo versionado; ejecutar tests y Postman antes de aprobar estos criterios.
