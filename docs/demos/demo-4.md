# DEMO EVALUADA 4 — UMSS MARKET

## Release 8.4.0

**Proyecto:** UMSS Market  
**Módulo:** Arquitectura del Producto y Especificaciones Funcionales  
**Entrega:** 16 de noviembre de 2026  
**Versión:** 8.4.0  
**Rama objetivo:** `release/8.4.0`

---

## 1. Objetivo

Presentar las funcionalidades implementadas para la Demo Evaluada 4 de UMSS Market, demostrando su funcionamiento en el producto, su correspondencia con el Functional Specification Document (FSD), sus criterios de aceptación y sus contratos de API.

Las funcionalidades se integran con el backend desarrollado en Spring Boot, PostgreSQL, Spring Security, autenticación JWT y la infraestructura de procesamiento de eventos del proyecto. La demo también presenta aportes transversales de seguridad, observabilidad, rendimiento y validación del contrato OpenAPI.

La demostración contempla las tres funcionalidades del equipo:

1. Gestión de imágenes de publicaciones.
2. Gestión de pedidos recibidos por tienda.
3. Notificaciones de pedidos mediante el patrón Transactional Outbox.

Además, se presentan los aportes individuales de Christian relacionados con seguridad/autorización, observabilidad y rendimiento, y contrato OpenAPI/CI.

## 2. Funcionalidades por integrante

| Integrante | Funcionalidad / aporte | Caso de uso | Alcance |
|---|---|---|---|
| Melani | Gestión de imágenes de publicaciones | UC-001 | Carga, listado, consulta y eliminación de imágenes |
| Melani | Gestión de pedidos recibidos por tienda | UC-002 | Consulta, detalle y actualización de estado |
| Melani | Notificaciones de pedidos mediante Outbox | UC-002 | Consumidor de eventos y envío de correos HTML |
| Christian | Seguridad y autorización por roles | Transversal a UC-001 / UC-002 | Configuración Spring Security, filtro JWT y verificación de acceso |
| Christian | Observabilidad y pruebas de rendimiento | Transversal | OpenTelemetry, Tempo, Grafana y pruebas k6 |
| Christian | Contrato OpenAPI y validación en CI | Transversal | Exportación desde Spring, verificación de operaciones y detección de drift |
| Christian | Integración de checkout y Outbox | UC-002 | Ajustes de seguridad e integración, pruebas y evidencias del procesamiento |

**Nota sobre trazabilidad:** la autoría Git de los commits `abe8cb3`, `3096704` y `e0969a4` corresponde a `melrodriguez1133`. Los aportes de Christian se respaldan separadamente con los commits `fdc8928`, `a452425` y `755f787`. La autoría de un commit se distingue de la participación funcional y de la colaboración del equipo.

---

## 3. Funcionalidad 1 — Gestión de imágenes de publicaciones

**Responsable funcional e implementación respaldada por commit:** Melani  
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

Descripción: `feat(images): add publication image management for UC-001`

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/abe8cb30a4a629e4abdf65deef12babf302294ee

---

## 4. Funcionalidad 2 — Gestión de pedidos recibidos por tienda

**Responsable funcional e implementación respaldada por commit:** Melani  
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

- `StoreReceivedOrdersController.java`
- `StoreReceivedOrdersControllerTest.java`
- `UpdateOrderStatusUseCase.java`
- `UpdateOrderStatusRequest.java`
- `SecurityConfig.java`
- `GlobalExceptionHandler.java`
- `docs/fsd/UC-002-pedidos-recibidos-emprendedor.md`

### 4.6. Commit de referencia

`3096704`

Descripción: `feat(orders): implement entrepreneur received orders management`

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/3096704f4af210d08892c4eda581b652dd4aeeba

---

## 5. Funcionalidad 3 — Notificaciones de pedidos mediante Outbox

**Responsable funcional e implementación respaldada por commit:** Melani  
**Caso de uso:** UC-002 — Procesar pedido.

### 5.1. Descripción

Se incorpora el envío de notificaciones por correo electrónico mediante el patrón Transactional Outbox.

El procesamiento de eventos permite desacoplar la operación de negocio del envío de correos electrónicos.

Cuando se genera un evento relacionado con un pedido, el consumidor correspondiente procesa la notificación y construye un mensaje HTML con información del comprador y del pedido.

La infraestructura utiliza Spring Mail, SMTP y Mailpit para las pruebas y demostraciones locales.

### 5.2. Arquitectura de procesamiento

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

Plantilla encargada de generar el contenido HTML de las notificaciones. Incluye identificación de UMSS Market, saludo al comprador, identificador y estado del pedido, descripción de la notificación y codificación UTF-8.

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

`compose.email.yaml` habilita el consumidor de correo, configura el servidor SMTP y define tiempos de espera de conexión y envío.

`compose.mailpit.yaml` incorpora Mailpit como servidor SMTP de pruebas, con interfaz web en el puerto 8025 y SMTP en el puerto 1025.

| Parámetro | Valor |
|---|---|
| Habilitación de correo | `NOTIFICATIONS_EMAIL_ENABLED=true` |
| Servidor SMTP | `mailpit` |
| Puerto SMTP | `1025` |
| Interfaz web | `http://localhost:8025` |
| Formato del mensaje | HTML |
| Codificación | UTF-8 |

### 5.6. Evidencia técnica

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

Descripción: `feat(demo4): notificaciones HTML de pedidos con Outbox y Mailpit`

https://github.com/christianvargasmpsia-cmd/UMSS-Market/commit/e0969a4abf0c0029cf7ec5254333a56f4374a976

### 5.8. Consideraciones técnicas

El envío SMTP y el registro de recepción en PostgreSQL no constituyen una única transacción atómica. Por esta razón, no se debe afirmar que existe garantía de entrega exactamente una vez. La verificación del reprocesamiento de eventos en el entorno integrado debe complementarse con pruebas específicas.

---

## 6. Aportes individuales de Christian

Esta sección agrega las contribuciones transversales de Christian, sin reemplazar ni reasignar las funcionalidades implementadas por Melani.

### 6.1. Seguridad JWT y autorización por roles

**Responsable:** Christian  
**Commit:** `a452425` — `feat: completar seguridad y observabilidad para demo 4`.

Se trabajó en la configuración y validación de la seguridad del backend mediante Spring Security y JWT.

Archivos modificados:

- `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/config/SecurityConfig.java`
- `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/security/JwtAuthenticationFilter.java`

Evidencias:

- `backend/umss-market-api/evidencias/demo4/01-emprendedor-inventario-200.png`
- `backend/umss-market-api/evidencias/demo4/02-comprador-inventario-403.png`
- `backend/umss-market-api/evidencias/demo4/03-security-config.png`

Resultados demostrados:

| Escenario | Resultado observado |
|---|---|
| Emprendedor autorizado consulta inventario | HTTP 200 |
| Comprador intenta acceder al endpoint restringido | HTTP 403 |

Este aporte respalda la demostración de autenticación y autorización por roles. No sustituye las reglas de propiedad específicas implementadas en cada caso de uso.

### 6.2. Observabilidad con OpenTelemetry, Tempo y Grafana

**Responsable:** Christian  
**Commit:** `a452425`.

Se incorporó y configuró infraestructura de observabilidad distribuida para recolectar y consultar trazas del backend.

Archivos versionados:

- `observability/tempo/tempo.yaml`
- `observability/otel/collector.yaml`
- `observability/grafana/provisioning/datasources/tempo.yaml`
- `compose.yaml`

La configuración contempla Micrometer Tracing y exportación OTLP desde el backend, OpenTelemetry Collector como receptor y encaminador, Grafana Tempo como almacenamiento de trazas y Grafana como interfaz de consulta.

| Variable | Valor |
|---|---|
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` |
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | `http://otel-collector:4318/v1/traces` |

Versiones de infraestructura registradas: Tempo 2.8.2, OpenTelemetry Collector 0.128.0 y Grafana 12.1.0.

Evidencias:

- `15-grafana-tempo-trazas.png`
- `16-grafana-tempo-detalle-traza.png`

En Tempo se observaron trazas de operaciones del servicio `umss-market-api`, incluyendo solicitudes HTTP y la tarea `outbox-dispatcher.dispatch`.

**Precisión:** las evidencias confirman que Tempo recibe y permite consultar trazas del backend. No se ha confirmado todavía que la traza seleccionada corresponda a una solicitud concreta de k6. La correlación directa debe verificarse antes de presentarla como demostrada.

### 6.3. Pruebas de rendimiento con k6

**Responsable:** Christian  
**Commit de evidencia:** `a452425`.

**Endpoint público de publicaciones:** se registraron tres ejecuciones con 10 solicitudes por segundo durante 60 segundos. Cada ejecución procesó 601 solicitudes, sin errores.

| Ejecución | Solicitudes | Errores | p95 |
|---|---:|---:|---:|
| 1 | 601 | 0 | 6.05 ms |
| 2 | 601 | 0 | 4.79 ms |
| 3 | 601 | 0 | 4.62 ms |

La mediana de los tres valores p95 fue de **4.79 ms**.

**Endpoint de inventario low-stock:**

| Métrica | Resultado |
|---|---:|
| Usuarios virtuales | 2 |
| Duración | 15 s |
| Solicitudes | 3,155 |
| Solicitudes fallidas | 0 |
| Promedio | 9.19 ms |
| Mediana | 7.28 ms |
| p95 | 18.37 ms |
| p99 | 26.78 ms |
| Máximo | 60.49 ms |
| Throughput aproximado | 210.255 req/s |

Evidencia versionada: `backend/umss-market-api/evidencias/demo4/13-k6-inventario-resultados.png`.

El percentil 95 indica que el 95 % de las solicitudes medidas tuvo una duración igual o inferior al valor reportado, mientras que el 5 % restante tardó más. Los resultados corresponden a las ejecuciones documentadas y no deben extrapolarse a cargas o entornos diferentes.

### 6.4. Contrato OpenAPI y validación en CI

**Responsable:** Christian  
**Commit:** `755f787` — `test(demo4): validate OpenAPI contract and CI`.

Se fortaleció la validación del contrato de API para comprobar que las operaciones requeridas por la Demo 4 estén presentes en el documento OpenAPI generado desde el código Spring Boot.

Archivos modificados:

- `.github/workflows/openapi-contract.yml`
- `backend/umss-market-api/src/test/java/bo/umss/market/umss_market_api/infrastructure/contracts/OpenApiContractExportTest.java`
- `docs/demos/check-openapi.cjs`
- `openapi.json`

Actividades realizadas:

1. Exportación del contrato OpenAPI desde Spring Boot mediante `OpenApiContractExportTest`.
2. Actualización del archivo versionado `openapi.json` a partir del contrato generado.
3. Ampliación del verificador para comprobar las operaciones requeridas.
4. Aserciones para rutas de imágenes y pedidos recibidos.
5. Configuración del workflow de GitHub Actions para ejecutar pruebas Maven, verificar el contrato y ejecutar el análisis de Spectral.

Resultado de validación local:

- **14 operaciones requeridas presentes.**
- **Sin diferencias (drift) entre el contrato generado y el versionado.**
- **31 pruebas Maven ejecutadas en la selección configurada: cero fallos, cero errores y cero omitidas.**

| Grupo | Operaciones |
|---|---:|
| Recomendaciones | 1 |
| Interacciones | 4 |
| Imágenes de publicaciones | 4 |
| Alertas de inventario | 2 |
| Pedidos recibidos | 3 |
| **Total** | **14** |

Comando de exportación, ejecutado desde `backend/umss-market-api`:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=OpenApiContractExportTest" test
```

Actualización del contrato versionado, desde la raíz del repositorio:

```powershell
node ..\..\docs\demos\check-openapi.cjs target\generated-openapi.json ..\..\openapi.json --update
```

Comprobación:

```powershell
node docs/demos/check-openapi.cjs
```

Salida: `OpenAPI: 14 operaciones requeridas presentes; sin drift.`

La validación local no equivale a una ejecución exitosa del workflow remoto. El resultado de GitHub Actions debe comprobarse en el repositorio después de publicar los commits.

### 6.5. Integración de checkout y Outbox

**Responsable del aporte registrado:** Christian  
**Commit:** `fdc8928` — `Transactional Outbox y notificaciones`.

Este commit contiene ajustes relacionados con la integración del checkout, la seguridad y las pruebas/evidencias del procesamiento de eventos.

Archivos registrados:

- `SecurityConfig.java`
- `SimulatedCheckoutController.java`
- `JwtAuthenticationFilter.java`
- `SimulatedCheckoutTest.java`
- `backend/umss-market-api/evidencias/demo4/demo-4.md`
- `logs-consumer.txt`
- `outbox-processed.txt`

El aporte se describe como integración, ajustes de seguridad y checkout, pruebas y generación de evidencias del procesamiento. La implementación inicial de `EmailOrderEventConsumer` y `OrderEmailTemplate` se mantiene atribuida al commit de Melani `e0969a4`; no se atribuye su autoría a Christian.

---

## 7. Contrato OpenAPI

Los endpoints expuestos por las funcionalidades deben formar parte del contrato OpenAPI generado desde el código Spring Boot.

### 7.1. Endpoints UC-001 — Imágenes

- POST `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images`
- GET `/api/publications/{publicationId}/images/{imageId}/content`
- DELETE `/api/publications/{publicationId}/images/{imageId}`

### 7.2. Endpoints UC-002 — Pedidos recibidos

- GET `/api/stores/{storeId}/orders`
- GET `/api/stores/{storeId}/orders/{orderId}`
- PATCH `/api/stores/{storeId}/orders/{orderId}/status`

### 7.3. Validación del contrato

El verificador también comprueba operaciones de recomendaciones, interacciones y alertas de inventario. El resultado local fue de 14 operaciones requeridas presentes y sin drift entre el contrato generado y `openapi.json`.

### 7.4. Workflows

Workflows identificados:

- `.github/workflows/openapi.yml`
- `.github/workflows/openapi-contract.yml`

El workflow `openapi-contract.yml` incluye pruebas Maven, exportación/validación del contrato y chequeo de Spectral.

**Estado:** la validación local se completó correctamente. La ejecución remota de GitHub Actions debe comprobarse en la pestaña Actions del repositorio; no se declara como exitosa hasta disponer de ese resultado.

---

## 8. Pruebas y validación

La validación de la Demo 4 contempla pruebas unitarias y de integración, autenticación JWT, roles y propiedad de recursos, respuestas HTTP, imágenes, pedidos, notificaciones, OpenAPI/CI, rendimiento k6 y observabilidad.

### 8.1. Resultado de pruebas Maven

Se ejecutó la selección de pruebas configurada para la validación de Demo 4:

- `JpaPublicationAtomicReservationTest`
- `TransactionalOutboxTest`
- `InventoryAlertsIntegrationTest`
- `PublicationImageControllerTest`
- `StoreReceivedOrdersControllerTest`
- `OpenApiContractExportTest`

Resultado registrado: **31 tests, cero failures, cero errors y cero skipped; BUILD SUCCESS**.

Este resultado corresponde a la selección indicada y no debe interpretarse como ejecución de la totalidad de pruebas existentes en todo el repositorio.

### 8.2. Evidencias de ejecución

Evidencias versionadas en `backend/umss-market-api/evidencias/demo4/`:

- `01-emprendedor-inventario-200.png`
- `02-comprador-inventario-403.png`
- `03-security-config.png`
- `13-k6-inventario-resultados.png`
- `15-grafana-tempo-trazas.png`
- `16-grafana-tempo-detalle-traza.png`
- `uc002-pedidos-recibidos.json`

### 8.3. Pruebas específicas de imágenes

Se deben comprobar carga de imagen válida, listado, consulta del contenido, eliminación, intentos sin autenticación, con rol comprador, modificación de publicación ajena, formato no permitido y tamaño superior al máximo.

### 8.4. Pruebas específicas de pedidos

Se deben comprobar listado de pedidos de tienda propia, filtrado por estado, consulta del detalle, acceso a tienda ajena, acceso con rol comprador, pedido inexistente, pedido multitienda, actualización de estado y transiciones no permitidas.

### 8.5. Pruebas específicas de notificaciones

Se deben comprobar generación del evento, ejecución del consumidor de correo, construcción HTML, envío SMTP, recepción en Mailpit, manejo de errores SMTP, registro del evento consumido y prevención de procesamiento duplicado en condiciones normales.

---

## 9. Pregunta obligatoria: k6 y OpenTelemetry

### 9.1. Resultados k6

**Endpoint de publicaciones:** tres pruebas con 10 solicitudes por segundo durante 60 segundos. Cada una registró 601 solicitudes y cero errores. Los valores p95 fueron 6.05 ms, 4.79 ms y 4.62 ms; la mediana fue 4.79 ms.

**Endpoint de inventario low-stock:** 2 usuarios virtuales durante 15 segundos, 3,155 solicitudes sin errores, promedio 9.19 ms, mediana 7.28 ms, p95 18.37 ms, p99 26.78 ms y máximo 60.49 ms. Throughput aproximado: 210.255 solicitudes por segundo.

El percentil 95 indica el tiempo que no supera el 95 % de las solicitudes observadas. No representa el tiempo máximo ni el promedio.

### 9.2. OpenTelemetry y Grafana Tempo

La infraestructura configurada permite abrir Grafana, ingresar a Explore, seleccionar Tempo, consultar trazas del servicio `umss-market-api`, abrir el detalle, identificar spans y explicar sus duraciones.

En las evidencias disponibles se muestran trazas HTTP y la tarea `outbox-dispatcher.dispatch`.

**Pendiente para completar la pregunta obligatoria:** seleccionar una traza generada por una solicitud de k6, verificar su correspondencia temporal o por atributos y registrar su Trace ID, duración total y span de mayor duración. No se debe afirmar que una traza genérica de Tempo corresponde a k6 sin comprobarlo.

### 9.3. Registro de resultados

| Métrica | Resultado |
|---|---|
| Endpoint de inventario | `/api/stores/{storeId}/inventory/low-stock` |
| Usuarios virtuales | 2 |
| Duración | 15 s |
| Solicitudes | 3,155 |
| Errores | 0 |
| p95 | 18.37 ms |
| Trace ID de una solicitud k6 | Pendiente de correlacionar |
| Duración total de esa traza | Pendiente de registrar |
| Span de mayor duración | Pendiente de identificar en la traza correlacionada |

---

## 10. Arquitectura y despliegue

### 10.1. Tecnologías involucradas

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

### 10.2. Componentes principales

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

## 11. Resultado y estado de cierre

El release 8.4.0 integra las tres funcionalidades del equipo: gestión de imágenes de publicaciones (UC-001), gestión de pedidos recibidos por tienda (UC-002) y notificaciones de pedidos mediante Outbox (UC-002).

Además, se documentan los aportes individuales de Christian en seguridad/autorización, observabilidad y rendimiento, y contrato OpenAPI/CI.

Resultados comprobados localmente:

- Seguridad: evidencia de HTTP 200 para emprendedor y HTTP 403 para comprador en el endpoint de inventario.
- k6: pruebas registradas con cero errores en los escenarios documentados.
- Maven: 31 pruebas de la selección configurada, sin fallos ni errores.
- OpenAPI: 14 operaciones requeridas presentes y sin drift local.
- Tempo: trazas del servicio visibles en Grafana.

Antes de declarar cerrada la demostración deben verificarse la ejecución exitosa de GitHub Actions, la traza de Tempo correspondiente a una solicitud concreta de k6, la publicación de la rama `release/8.4.0` con documentos y evidencias definitivos, y el enlace del video con permisos para el docente.

---

**Fin del documento.**

**Proyecto:** UMSS Market  
**Demo:** Evaluada 4  
**Release:** 8.4.0  
**Fecha:** 16 de noviembre de 2026