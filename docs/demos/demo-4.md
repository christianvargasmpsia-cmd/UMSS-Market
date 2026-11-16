# DEMO EVALUADA 4 — UMSS MARKET

## Release 8.4.0

**Proyecto:** UMSS Market  
**Módulo:** Arquitectura del Producto y Especificaciones Funcionales  
**Entrega:** 16 de noviembre de 2026  
**Versión:** 8.4.0  
**Rama:** `release/8.4.0`

---

## 1. Objetivo

Presentar las funcionalidades implementadas para la Demo Evaluada 4 de UMSS Market, demostrando su funcionamiento en el producto, su correspondencia con el Functional Specification Document (FSD), sus criterios de aceptación y sus contratos de API.

Las funcionalidades se integran con el backend desarrollado en Spring Boot, PostgreSQL, Spring Security, autenticación JWT y la infraestructura de procesamiento de eventos del proyecto.

La demostración contempla tres funcionalidades:

1. Gestión de imágenes de publicaciones.
2. Gestión de pedidos recibidos por tienda.
3. Notificaciones de pedidos mediante el patrón Transactional Outbox.

## 2. Funcionalidades por integrante

| Integrante | Funcionalidad | Caso de uso | Alcance |
|---|---|---|---|
| Melani | Gestión de imágenes de publicaciones | UC-001 | Carga, listado, consulta y eliminación de imágenes |
| Christian | Gestión de pedidos recibidos por tienda | UC-002 | Consulta, detalle y actualización de estado |
| Christian | Notificaciones de pedidos mediante Outbox | UC-002 | Procesamiento de eventos y envío de correos HTML |

**Nota sobre trazabilidad:** La tabla representa la distribución funcional del equipo. Los commits identificados en el repositorio deben contrastarse con la participación efectiva de cada integrante. Los tres commits de referencia consultados registran como autor Git a `melrodriguez1133`. La participación individual adicional debe respaldarse con evidencias correspondientes.

---

## 3. Funcionalidad 1 — Gestión de imágenes de publicaciones

**Responsable funcional:** Melani  
**Caso de uso:** UC-001 — Gestión de publicaciones.

### 3.1. Descripción

Se incorpora la gestión de imágenes asociadas a las publicaciones del marketplace.

El emprendedor propietario puede cargar imágenes, consultar las imágenes asociadas a una publicación, obtener su contenido y eliminarlas.

Los metadatos se almacenan en PostgreSQL mediante JPA, mientras que los archivos se guardan en almacenamiento local persistente configurado mediante Docker Compose.

La implementación busca mantener la separación entre los datos de las publicaciones y los archivos asociados, evitando almacenar directamente el contenido binario de las imágenes en los registros principales de las publicaciones.

### 3.2. Endpoints implementados

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/publications/{publicationId}/images` | Cargar imagen |
| GET | `/api/publications/{publicationId}/images` | Listar imágenes |
| GET | `/api/publications/{publicationId}/images/{imageId}/content` | Obtener contenido |
| DELETE | `/api/publications/{publicationId}/images/{imageId}` | Eliminar imagen |

### 3.3. Reglas de negocio y seguridad

- Solo el emprendedor propietario de la publicación puede cargar o eliminar imágenes.
- Las operaciones se protegen mediante autenticación JWT y autorización por rol.
- Se verifica que la tienda pertenezca al usuario autenticado.
- Se admiten imágenes JPEG, PNG y WebP.
- El tamaño máximo permitido es de 5 MiB.
- Se comprueba la firma binaria del archivo.
- Se verifica que el Content-Type declarado coincida con el formato detectado.
- Los nombres de archivo son generados mediante UUID.
- Los archivos se almacenan utilizando nombres generados por el servidor y no el nombre proporcionado por el cliente.
- Se comprueba que la imagen consultada corresponda a la publicación indicada.

### 3.4. Criterios de aceptación

| ID | Criterio | Resultado esperado |
|---|---|---|
| CA-01 | Emprendedor propietario carga imagen válida | HTTP 201 |
| CA-02 | Consultar imágenes de una publicación | HTTP 200 |
| CA-03 | Obtener contenido de imagen existente | HTTP 200 |
| CA-04 | Eliminar imagen propia | HTTP 204 |
| CA-05 | Comprador intenta cargar imagen | HTTP 403 |
| CA-06 | Emprendedor intenta modificar publicación ajena | HTTP 403 |
| CA-07 | Archivo con formato no permitido | HTTP 400 |
| CA-08 | Archivo superior a 5 MiB | HTTP 400 en la validación del controlador |
| CA-09 | Imagen inexistente | HTTP 404 |

Los códigos anteriores representan los resultados esperados de los escenarios de aceptación. Su cumplimiento debe respaldarse mediante las pruebas correspondientes.

### 3.5. Evidencia técnica

Archivos principales:

- `PublicationImageController.java`
- `PublicationImageResponse.java`
- `PublicationImageEntity.java`
- `JpaPublicationImageRepository.java`
- `PublicationImageControllerTest.java`
- `SecurityConfig.java`
- `GlobalExceptionHandler.java`
- `compose.images.yaml`
- `docs/demo4/UC001-imagenes.md`

### 3.6. Commit de referencia

`abe8cb3`

Descripción:

`feat(images): add publication image management for UC-001`

Commit completo:

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/abe8cb30a4a629e4abdf65deef12babf302294ee

---

## 4. Funcionalidad 2 — Gestión de pedidos recibidos por tienda

**Responsable funcional:** Christian  
**Caso de uso:** UC-002 — Procesar pedido.

### 4.1. Descripción

Se implementa la consulta y administración de pedidos recibidos por una tienda.

El emprendedor autenticado puede consultar los pedidos que contienen productos de su tienda, revisar el detalle correspondiente y actualizar el estado cuando se cumplen las reglas de negocio.

La respuesta filtra los artículos para evitar exponer productos pertenecientes a otras tiendas.

La funcionalidad complementa el proceso de compra del marketplace, permitiendo que los emprendedores administren los pedidos correspondientes a sus establecimientos.

### 4.2. Endpoints implementados

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/stores/{storeId}/orders` | Listar pedidos recibidos |
| GET | `/api/stores/{storeId}/orders/{orderId}` | Consultar detalle |
| PATCH | `/api/stores/{storeId}/orders/{orderId}/status` | Actualizar estado |

### 4.3. Reglas de negocio y seguridad

- Se requiere un usuario autenticado mediante JWT.
- Solo los usuarios con rol EMPRENDEDOR pueden acceder.
- El usuario debe ser propietario de la tienda consultada.
- No se exponen artículos de otras tiendas.
- Se calcula el subtotal correspondiente a los artículos propios.
- Se utiliza bloqueo de la orden para la actualización de estado.
- No se permite modificar el estado global de un pedido multitienda desde una sola tienda.
- Se respetan las transiciones permitidas por el caso de uso.
- Los pedidos inexistentes deben producir una respuesta HTTP 404.
- Los conflictos de negocio deben producir respuestas HTTP 409 cuando corresponda.

### 4.4. Criterios de aceptación

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

Los resultados deben comprobarse mediante pruebas automatizadas y pruebas HTTP con autenticación JWT.

### 4.5. Evidencia técnica

Archivos principales:

- `StoreReceivedOrdersController.java`
- `StoreReceivedOrdersControllerTest.java`
- `UpdateOrderStatusUseCase.java`
- `UpdateOrderStatusRequest.java`
- `SecurityConfig.java`
- `GlobalExceptionHandler.java`
- `docs/fsd/UC-002-pedidos-recibidos-emprendedor.md`

### 4.6. Commit de referencia

`3096704`

Descripción:

`feat(orders): implement entrepreneur received orders management`

Commit completo:

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/3096704f4af210d08892c4eda581b652dd4aeeba

---

## 5. Funcionalidad 3 — Notificaciones de pedidos mediante Outbox

**Responsable funcional:** Christian  
**Caso de uso:** UC-002 — Procesar pedido.

### 5.1. Descripción

Se incorpora el envío de notificaciones por correo electrónico mediante el patrón Transactional Outbox.

El procesamiento de eventos permite desacoplar la operación de negocio del envío de correos electrónicos.

Cuando se genera un evento relacionado con un pedido, el consumidor correspondiente procesa la notificación y construye un mensaje HTML con información del comprador y del pedido.

La infraestructura utiliza Spring Mail, SMTP y Mailpit para las pruebas y demostraciones locales.

### 5.2. Arquitectura de procesamiento

El flujo de notificación contempla las siguientes etapas:

1. El comprador realiza una operación relacionada con un pedido.
2. El backend registra el pedido y su evento correspondiente.
3. El evento queda disponible en la infraestructura Outbox.
4. El procesador de eventos recupera el evento pendiente.
5. Se ejecuta el consumidor correspondiente.
6. `EmailOrderEventConsumer` obtiene los datos del pedido y del comprador.
7. `OrderEmailTemplate` genera el contenido HTML.
8. `JavaMailSender` prepara y envía el mensaje mediante SMTP.
9. Mailpit recibe el correo durante la demostración.
10. Se registra el evento consumido para evitar su procesamiento duplicado en condiciones normales.

### 5.3. Componentes implementados

**EmailOrderEventConsumer.java**

Consumidor responsable del procesamiento de eventos de pedidos y del envío de notificaciones mediante `JavaMailSender`.

**OrderEmailTemplate.java**

Plantilla encargada de generar el contenido HTML de las notificaciones.

Incluye:

- Identificación de UMSS Market.
- Saludo al comprador.
- Identificador del pedido.
- Estado del pedido.
- Descripción de la notificación.
- Codificación UTF-8.

**InboxOrderEventConsumer.java**

Se conserva el consumidor original de registro local cuando el envío de correo no está habilitado.

**Infraestructura Outbox**

Se reutiliza la infraestructura existente para procesar eventos y gestionar los reintentos correspondientes ante errores de procesamiento.

### 5.4. Criterios de aceptación

| ID | Criterio | Resultado esperado |
|---|---|---|
| CA-01 | Se genera un evento de pedido | Evento registrado para procesamiento |
| CA-02 | Se procesa un evento de notificación | Se ejecuta el consumidor correspondiente |
| CA-03 | Se procesa el correo | Se genera mensaje HTML |
| CA-04 | Servicio SMTP disponible | Correo aceptado por el servidor |
| CA-05 | Servicio SMTP no disponible | El error se propaga para permitir el mecanismo de reintentos |
| CA-06 | Evento procesado correctamente | Se registra su recepción |
| CA-07 | Ocurre un error de procesamiento | Se registra o propaga el error para su tratamiento |

### 5.5. Configuración de demostración

Archivo `compose.email.yaml`:

- Habilita el consumidor de correo.
- Configura el servidor SMTP.
- Define los tiempos de espera de conexión y envío.

Archivo `compose.mailpit.yaml`:

- Incorpora Mailpit como servidor SMTP de pruebas.
- Expone la interfaz web en el puerto 8025.
- Expone el servidor SMTP en el puerto 1025.

Configuración principal:

| Parámetro | Valor |
|---|---|
| Habilitación de correo | `NOTIFICATIONS_EMAIL_ENABLED=true` |
| Servidor SMTP | `mailpit` |
| Puerto SMTP | `1025` |
| Interfaz web | `http://localhost:8025` |
| Formato del mensaje | HTML |
| Codificación | UTF-8 |

Mailpit permite visualizar los correos generados durante la demostración sin utilizar un servicio externo de correo electrónico.

### 5.6. Evidencia técnica

Archivos principales:

- `EmailOrderEventConsumer.java`
- `OrderEmailTemplate.java`
- `InboxOrderEventConsumer.java`
- `EmailOrderEventConsumerTest.java`
- `pom.xml`
- `compose.email.yaml`
- `compose.mailpit.yaml`
- `docs/demo4/README.md`

### 5.7. Commit de referencia

`e0969a4`

Descripción:

`feat(demo4): notificaciones HTML de pedidos con Outbox y Mailpit`

Commit completo:

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/e0969a4abf0c0029cf7ec5254333a56f4374a976

### 5.8. Consideraciones técnicas

La implementación registra el evento consumido después del envío SMTP satisfactorio.

El envío SMTP y el registro de recepción en PostgreSQL no constituyen una única transacción atómica.

Por esta razón, no se debe afirmar que existe garantía de entrega exactamente una vez.

La verificación del reprocesamiento de eventos en el entorno integrado debe complementarse con pruebas específicas.

---

## 6. Contrato OpenAPI

Los endpoints expuestos por las funcionalidades deben formar parte del contrato OpenAPI generado desde el código Spring Boot.

### 6.1. Endpoints UC-001

- POST `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images/{imageId}/content`
- DELETE `/api/publications/{publicationId}/images/{imageId}`

### 6.2. Endpoints UC-002

- GET `/api/stores/{storeId}/orders`
- GET `/api/stores/{storeId}/orders/{orderId}`
- PATCH `/api/stores/{storeId}/orders/{orderId}/status`

### 6.3. Workflows de validación

Los workflows existentes son:

- `.github/workflows/openapi.yml`
- `.github/workflows/openapi-contract.yml`

El archivo `openapi.json` debe generarse desde Spring Boot y mantenerse actualizado en el repositorio.

**Verificación pendiente de entrega:**

- Comprobar que las siete operaciones nuevas estén incluidas en el contrato.
- Verificar que los parámetros y respuestas estén documentados.
- Verificar los esquemas de seguridad JWT.
- Comprobar que ambos workflows finalicen correctamente.
- Confirmar que no existan diferencias entre el contrato generado y el versionado.

La presencia de los workflows no implica por sí sola que hayan finalizado correctamente.

---

## 7. Pruebas y validación

La validación de la Demo 4 debe contemplar los siguientes niveles:

1. Ejecución de pruebas unitarias.
2. Ejecución de pruebas de integración.
3. Verificación de autenticación JWT.
4. Verificación de roles y propiedad de recursos.
5. Verificación de respuestas HTTP.
6. Prueba de carga y almacenamiento de imágenes.
7. Prueba de consulta y actualización de pedidos recibidos.
8. Prueba del procesamiento de notificaciones.
9. Validación de OpenAPI y CI.

### 7.1. Evidencias de ejecución

Se deben registrar los resultados reales de:

- Ejecuciones Maven.
- Pruebas HTTP mediante Postman.
- Pruebas de autorización.
- Consultas a PostgreSQL.
- Pruebas de almacenamiento de imágenes.
- Procesamiento de eventos Outbox.
- Recepción de correos en Mailpit.
- Resultados de GitHub Actions.

No se deben sustituir resultados reales por valores estimados.

### 7.2. Pruebas específicas de imágenes

Se deben comprobar los siguientes escenarios:

- Carga de una imagen válida.
- Listado de imágenes de una publicación.
- Consulta del contenido de una imagen.
- Eliminación de una imagen.
- Intento de carga sin autenticación.
- Intento de carga con rol comprador.
- Intento de modificación de una publicación ajena.
- Archivo con formato no permitido.
- Archivo superior al tamaño máximo.

### 7.3. Pruebas específicas de pedidos

Se deben comprobar:

- Listado de pedidos de la tienda propia.
- Filtrado por estado.
- Consulta del detalle.
- Intento de acceso a una tienda ajena.
- Intento de acceso con rol comprador.
- Pedido inexistente.
- Pedido multitienda.
- Actualización de estado.
- Transiciones de estado no permitidas.

### 7.4. Pruebas específicas de notificaciones

Se deben comprobar:

- Generación del evento.
- Ejecución del consumidor de correo.
- Construcción del mensaje HTML.
- Envío mediante SMTP.
- Recepción en Mailpit.
- Manejo de errores SMTP.
- Registro del evento consumido.
- Prevención del procesamiento duplicado en condiciones normales.

---

## 8. Pregunta obligatoria: k6 y OpenTelemetry

La demostración debe incluir las pruebas de rendimiento y observabilidad del sistema.

### 8.1. k6

Se debe abrir el reporte de la prueba ejecutada e identificar:

- El endpoint sometido a carga.
- El número de usuarios virtuales.
- La duración de la prueba.
- La métrica `http_req_duration`.
- El percentil 95 (`p(95)`).
- Los resultados de solicitudes exitosas y fallidas.

Se debe explicar qué representa el percentil 95 y cómo permite evaluar el comportamiento del sistema bajo carga.

### 8.2. OpenTelemetry y Grafana Tempo

Se debe realizar la siguiente demostración:

1. Abrir Grafana.
2. Ingresar a Explore.
3. Seleccionar Tempo.
4. Localizar una traza correspondiente a la ejecución de k6.
5. Mostrar la duración total de la traza.
6. Identificar los spans principales.
7. Explicar qué operación consume más tiempo.
8. Relacionar el resultado con el comportamiento observado durante la prueba de rendimiento.

### 8.3. Registro de resultados

| Métrica | Evidencia |
|---|---|
| Endpoint probado | Registrar endpoint real |
| Usuarios virtuales | Registrar configuración de k6 |
| Duración | Registrar configuración de k6 |
| p95 | Registrar resultado real |
| Trace ID | Registrar identificador real |
| Duración total de la traza | Registrar valor observado |
| Tramo de mayor duración | Explicar según la traza |

Los resultados deben corresponder a ejecuciones reales y estar respaldados por reportes o capturas.

---

## 9. Arquitectura y despliegue

Las funcionalidades de la Demo 4 se integran en el backend de UMSS Market.

### 9.1. Tecnologías involucradas

- Java 21.
- Spring Boot.
- Spring Security.
- JWT.
- PostgreSQL.
- Spring Data JPA.
- Docker Compose.
- OpenAPI.
- Transactional Outbox.
- Spring Mail.
- SMTP.
- Mailpit.
- k6.
- OpenTelemetry.
- Grafana Tempo.

### 9.2. Componentes principales

| Componente | Responsabilidad |
|---|---|
| Spring Boot | Exposición de API REST |
| Spring Security | Autenticación y autorización |
| JWT | Identificación del usuario |
| PostgreSQL | Persistencia de información |
| JPA | Acceso a datos |
| Docker Compose | Ejecución de servicios |
| Outbox | Procesamiento de eventos |
| Spring Mail | Envío de notificaciones |
| Mailpit | Recepción de correos de prueba |
| OpenAPI | Documentación de contratos |
| k6 | Pruebas de rendimiento |
| OpenTelemetry | Instrumentación y trazabilidad |
| Grafana Tempo | Consulta de trazas |

---

## 10. Resultado esperado

El release 8.4.0 integra las tres funcionalidades definidas para la Demo Evaluada 4:

1. Gestión de imágenes de publicaciones correspondiente a UC-001.
2. Gestión de pedidos recibidos por tienda correspondiente a UC-002.
3. Notificaciones de pedidos mediante Outbox correspondiente a UC-002.

La implementación incorpora control de acceso, persistencia de datos, almacenamiento de archivos, administración de pedidos y procesamiento de notificaciones.

La aceptación final de la demostración requiere verificar:

- El funcionamiento de los endpoints.
- Los criterios de aceptación.
- La seguridad y propiedad de los recursos.
- Las pruebas automatizadas.
- El contrato OpenAPI.
- La integración continua.
- Las evidencias de rendimiento mediante k6.
- Las trazas de OpenTelemetry mediante Grafana Tempo.

La documentación y las evidencias deben representar los resultados efectivamente obtenidos durante la ejecución del producto.

---

**Fin del documento.**

**Proyecto:** UMSS Market  
**Demo:** Evaluada 4  
**Release:** 8.4.0  
**Fecha:** 16 de noviembre de 2026