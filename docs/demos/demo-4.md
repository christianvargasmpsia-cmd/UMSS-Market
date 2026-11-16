# Demo Evaluada 4 — UMSS Market
## Release 8.4.0

**Proyecto:** UMSS Market  
**Módulo:** Arquitectura del Producto y Especificaciones Funcionales  
**Entrega:** 16 de noviembre de 2026  
**Versión:** 8.4.0

---

## 1. Objetivo

Presentar las funcionalidades implementadas para la Demo Evaluada 4,
demostrando su funcionamiento en el producto, su correspondencia
con el Functional Specification Document (FSD), sus criterios
de aceptación y sus contratos de API.

Las funcionalidades se integran con el backend Spring Boot,
PostgreSQL, Spring Security, JWT y la infraestructura de eventos
del proyecto.

## 2. Funcionalidades por integrante

| Integrante | Funcionalidad | Caso de uso | Alcance |
|---|---|---|---|
| Melani | Gestión de imágenes de publicaciones | UC-001 | Carga, listado, consulta y eliminación de imágenes |
| Christian | Gestión de pedidos recibidos por tienda | UC-002 | Consulta, detalle y actualización de estado |
| Christian | Notificaciones de pedidos mediante Outbox | UC-002 | Procesamiento de eventos y envío de correos HTML |

La atribución individual debe coincidir con la autoría y participación
efectivamente registrada en los commits y evidencias del equipo.

---

## 3. Funcionalidad 1 — Gestión de imágenes de publicaciones

**Responsable:** Melani  
**Caso de uso:** UC-001 — Gestión de publicaciones.

### Descripción

Se incorpora la gestión de imágenes asociadas a las publicaciones.
El emprendedor propietario puede cargar imágenes, consultar las
imágenes asociadas a una publicación y eliminarlas.

Los metadatos se almacenan en PostgreSQL y los archivos se guardan
en almacenamiento local persistente mediante Docker Compose.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/publications/{publicationId}/images` | Cargar imagen |
| GET | `/api/publications/{publicationId}/images` | Listar imágenes |
| GET | `/api/publications/{publicationId}/images/{imageId}/content` | Obtener contenido |
| DELETE | `/api/publications/{publicationId}/images/{imageId}` | Eliminar imagen |

### Reglas de negocio y seguridad

- Solo el emprendedor propietario de la publicación puede cargar
  o eliminar imágenes.
- Se requiere autenticación JWT para las operaciones de escritura.
- Se verifica que la tienda pertenezca al usuario autenticado.
- Se admiten imágenes JPEG, PNG y WebP.
- El tamaño máximo permitido es 5 MiB.
- Se comprueba la firma binaria del archivo.
- Se verifica que el Content-Type declarado coincida con el formato.
- Los nombres de archivo son generados mediante UUID.
- Los archivos se almacenan fuera del nombre proporcionado por el cliente.

### Criterios de aceptación

| ID | Criterio | Resultado esperado |
|---|---|---|
| CA-01 | Emprendedor propietario carga imagen válida | HTTP 201 |
| CA-02 | Consultar imágenes de una publicación | HTTP 200 |
| CA-03 | Obtener contenido de imagen existente | HTTP 200 |
| CA-04 | Eliminar imagen propia | HTTP 204 |
| CA-05 | Comprador intenta cargar imagen | HTTP 403 |
| CA-06 | Emprendedor intenta modificar publicación ajena | HTTP 403 |
| CA-07 | Archivo con formato no permitido | HTTP 400 |
| CA-08 | Archivo superior a 5 MiB | HTTP 413 en la prueba de integración HTTP |
| CA-09 | Imagen inexistente | HTTP 404 |

**Evidencia técnica:**

- `PublicationImageController.java`
- `PublicationImageEntity.java`
- `JpaPublicationImageRepository.java`
- `PublicationImageControllerTest.java`
- `compose.images.yaml`
- `docs/demo4/UC001-imagenes.md`

**Commit:** `abe8cb3`

---

## 4. Funcionalidad 2 — Gestión de pedidos recibidos por tienda

**Responsable:** Christian  
**Caso de uso:** UC-002 — Procesar pedido.

### Descripción

Se implementa la consulta y administración de pedidos recibidos
por una tienda.

El emprendedor autenticado puede consultar los pedidos que contienen
productos de su tienda, revisar el detalle correspondiente y actualizar
el estado cuando se cumplen las reglas de negocio.

La respuesta filtra los artículos para evitar exponer productos
pertenecientes a otras tiendas.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/stores/{storeId}/orders` | Listar pedidos recibidos |
| GET | `/api/stores/{storeId}/orders/{orderId}` | Consultar detalle |
| PATCH | `/api/stores/{storeId}/orders/{orderId}/status` | Actualizar estado |

### Reglas de negocio y seguridad

- Se requiere un usuario autenticado mediante JWT.
- Solo los usuarios con rol EMPRENDEDOR pueden acceder.
- El usuario debe ser propietario de la tienda consultada.
- No se exponen artículos de otras tiendas.
- Se calcula el subtotal correspondiente a los artículos propios.
- Se utiliza bloqueo de la orden para la actualización de estado.
- No se permite modificar el estado global de un pedido multitienda
  desde una sola tienda.
- Se respetan las transiciones permitidas por el caso de uso.

### Criterios de aceptación

| ID | Criterio | Resultado esperado |
|---|---|---|
| CA-01 | Emprendedor consulta pedidos de su tienda | HTTP 200 |
| CA-02 | Filtrar pedidos por estado | Resultados correspondientes |
| CA-03 | Consultar detalle de pedido propio | HTTP 200 |
| CA-04 | Usuario sin autenticación | HTTP 401 |
| CA-05 | Comprador intenta consultar pedidos recibidos | HTTP 403 |
| CA-06 | Emprendedor consulta tienda ajena | HTTP 403 |
| CA-07 | Pedido inexistente | HTTP 404 |
| CA-08 | Pedido multitienda intenta cambiar estado global | HTTP 409 |
| CA-09 | Transición de estado no permitida | HTTP 409 |

**Evidencia técnica:**

- `StoreReceivedOrdersController.java`
- `StoreReceivedOrdersControllerTest.java`
- `UpdateOrderStatusUseCase.java`
- `docs/fsd/UC-002-pedidos-recibidos-emprendedor.md`

**Commit:** `3096704`

---

## 5. Funcionalidad 3 — Notificaciones de pedidos mediante Outbox

**Responsable:** Christian  
**Caso de uso:** UC-002 — Procesar pedido.

### Descripción

Se incorpora el envío de notificaciones por correo electrónico
mediante el patrón Outbox.

El procesamiento de eventos permite desacoplar la operación
de negocio del envío de correos electrónicos.

Se utiliza una plantilla HTML para presentar la información
de las notificaciones de pedidos.

La infraestructura incluye procesamiento de eventos, reintentos
y registro de errores.

### Componentes

- `EmailOrderEventConsumer.java`
- `OrderEmailTemplate.java`
- `InboxOrderEventConsumer.java`
- Infraestructura Outbox existente.
- Configuración de correo mediante Spring Boot.
- Mailpit para pruebas locales del envío de correos.

### Criterios de aceptación

| ID | Criterio | Resultado esperado |
|---|---|---|
| CA-01 | Se genera evento de pedido | Evento registrado para procesamiento |
| CA-02 | Se procesa un evento de notificación | Se ejecuta el consumidor correspondiente |
| CA-03 | Se procesa el correo | Se genera mensaje HTML |
| CA-04 | Servicio SMTP disponible | Correo aceptado por el servidor |
| CA-05 | Servicio SMTP no disponible | Se aplica el mecanismo de reintentos |
| CA-06 | Evento procesado correctamente | Se registra su procesamiento |
| CA-07 | Ocurre un error de procesamiento | Se registra el error |

**Evidencia técnica:**

- `EmailOrderEventConsumer.java`
- `OrderEmailTemplate.java`
- `EmailOrderEventConsumerTest.java`
- `compose.email.yaml`
- `compose.mailpit.yaml`
- `docs/demo4/README.md`

**Commit:** `e0969a4`

---

## 6. Contrato OpenAPI

Los endpoints expuestos por las funcionalidades deben formar parte
del contrato OpenAPI generado desde el código Spring Boot.

Se debe verificar la presencia de las siguientes operaciones:

### UC-001

- POST `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images/{imageId}/content`
- DELETE `/api/publications/{publicationId}/images/{imageId}`

### UC-002

- GET `/api/stores/{storeId}/orders`
- GET `/api/stores/{storeId}/orders/{orderId}`
- PATCH `/api/stores/{storeId}/orders/{orderId}/status`

El archivo `openapi.json` debe regenerarse desde Spring Boot
y actualizarse en el repositorio.

Los workflows existentes son:

- `.github/workflows/openapi.yml`
- `.github/workflows/openapi-contract.yml`

**Verificación pendiente:** comprobar que las siete operaciones
nuevas estén incluidas en el contrato versionado y que ambos
workflows finalicen correctamente.

---

## 7. Pruebas y validación

La validación debe contemplar:

1. Ejecución de pruebas unitarias.
2. Ejecución de pruebas de integración.
3. Verificación de autenticación JWT.
4. Verificación de roles y propiedad de recursos.
5. Verificación de respuestas HTTP.
6. Prueba de carga y almacenamiento de imágenes.
7. Prueba de pedidos recibidos.
8. Prueba del procesamiento de notificaciones.
9. Validación de OpenAPI y CI.

### Evidencia de ejecución

Registrar los resultados reales de las ejecuciones de Maven,
pruebas HTTP y validaciones de CI.

No se deben sustituir resultados reales por valores estimados.

---

## 8. Pregunta obligatoria: k6 y OpenTelemetry

La demostración debe incluir:

### k6

- Abrir el reporte de la prueba ejecutada.
- Identificar la métrica `http_req_duration`.
- Mostrar el percentil 95 (`p(95)`).
- Explicar qué endpoint se sometió a carga.
- Explicar el número de usuarios virtuales y duración de la prueba.

### OpenTelemetry y Grafana Tempo

- Abrir Grafana.
- Entrar en Explore.
- Seleccionar Tempo.
- Localizar una traza lenta de la ejecución de k6.
- Mostrar la duración total de la traza.
- Identificar los spans principales.
- Explicar en qué tramo se consume más tiempo y por qué.

**Registro de resultados:**

| Métrica | Evidencia |
|---|---|
| Endpoint probado | Registrar endpoint real |
| Usuarios virtuales | Registrar configuración de k6 |
| Duración | Registrar configuración de k6 |
| p95 | Registrar resultado real |
| Trace ID | Registrar identificador real |
| Tramo de mayor duración | Explicar según la traza |

---

## 9. Arquitectura y despliegue

Las funcionalidades se integran en el backend de UMSS Market.

Tecnologías involucradas:

- Java 21.
- Spring Boot.
- Spring Security.
- JWT.
- PostgreSQL.
- JPA.
- Docker Compose.
- OpenAPI.
- Outbox.
- SMTP y Mailpit.
- k6.
- OpenTelemetry y Grafana Tempo.

---

## 10. Resultado esperado

El release 8.4.0 integra las tres funcionalidades de la Demo 4,
manteniendo la separación de responsabilidades, la seguridad
de los recursos y la trazabilidad de las operaciones.

La aceptación final requiere verificar el contrato OpenAPI,
la integración continua y las evidencias de k6 y OpenTelemetry.