# Demo 4 - Notificaciones HTML de pedidos

## 1. Objetivo

Implementar el envio de notificaciones por correo electronico
cuando un comprador registra un pedido en UMSS Market.

La solucion utiliza Spring Boot, Spring Mail, SMTP, Mailpit
y el patron Transactional Outbox existente.

## 2. Arquitectura

Flujo principal:

1. El comprador se autentica mediante JWT.
2. El comprador ejecuta POST /api/orders.
3. El sistema registra el pedido con estado PENDIENTE.
4. Se registra un evento ORDER_CREATED en el Outbox.
5. El procesador Outbox entrega el evento al consumidor.
6. EmailOrderEventConsumer construye el mensaje HTML.
7. JavaMailSender envia el correo mediante SMTP.
8. Mailpit recibe el mensaje.
9. El consumidor registra la recepcion del evento para
   evitar duplicaciones durante el reprocesamiento.

## 3. Componentes implementados

### EmailOrderEventConsumer

Consumidor encargado de procesar eventos de pedidos y
enviar notificaciones mediante JavaMailSender.

Utiliza MimeMessageHelper con codificacion UTF-8 y
contenido HTML.

El registro de recepcion se realiza despues del envio
SMTP satisfactorio.

### OrderEmailTemplate

Genera el contenido HTML de la notificacion.

Incluye:
- Identificacion de UMSS Market.
- Saludo al comprador.
- Identificador del pedido.
- Estado PENDIENTE.
- Presentacion visual del mensaje.

### InboxOrderEventConsumer

El consumidor original permanece disponible cuando
notifications.email.enabled=false o no se configura
la propiedad.

### Spring Mail

Se incorpora la dependencia spring-boot-starter-mail
en el archivo pom.xml.

## 4. Configuracion Docker

### compose.email.yaml

Habilita el envio de correo y configura:

- NOTIFICATIONS_EMAIL_ENABLED=true
- SPRING_MAIL_HOST=mailpit
- SPRING_MAIL_PORT=1025
- Tiempo de espera de conexion: 3000 ms
- Tiempo de espera SMTP: 3000 ms
- Tiempo de espera de escritura: 3000 ms

### compose.mailpit.yaml

Incorpora Mailpit como servidor SMTP de pruebas.

Accesos locales:
- Interfaz web: http://localhost:8025
- Servidor SMTP: localhost:1025

Mailpit se utiliza para pruebas y demostraciones.
No constituye un servicio de correo de produccion.

## 5. Pruebas automatizadas

Se ejecutaron cuatro pruebas unitarias del consumidor.

Resultado observado:

Tests run: 4
Failures: 0
Errors: 0
BUILD SUCCESS

Las pruebas cubren:
- Generacion y envio de correo HTML.
- Ausencia de registro de consumo cuando falla SMTP.
- Prevencion de duplicados para eventos consumidos.
- Ausencia de envio cuando falta el comprador.

## 6. Prueba de extremo a extremo

Se realizo una prueba con la API y los servicios Docker.

Pedido creado:
d805c937-d77b-45bb-a625-e9bbdba5160d

Evento Outbox:
77667a80-dd70-4507-b59c-606136bee1fe

Resultado:
- Pedido creado correctamente.
- Evento procesado por Outbox.
- Estado registrado en logs: PROCESSED.
- Intentos fallidos previos: 0.
- Cantidad de correos en Mailpit: de 1 a 2.
- Mensaje HTML presente.
- Identificador del pedido incluido en HTML.
- Estado PENDIENTE incluido en HTML.

## 7. Verificacion de codificacion

Se inspecciono la cabecera MIME original del mensaje.

Subject: =?UTF-8?Q?UMSS_Market_|_=C2=A1Pedido_registrado!?=

MIME-Version: 1.0
Content-Type: text/html;charset=UTF-8

La decodificacion UTF-8 produjo:

UMSS Market | ¡Pedido registrado!

Resultado de validacion: True.

Se observo una representacion incorrecta del caracter
de apertura de exclamacion en una consulta a la API de
Mailpit, pero la cabecera MIME original se decodifico
correctamente.

## 8. Idempotencia y recuperacion

El consumidor comprueba los eventos previamente consumidos
para evitar reenvios.

El registro de consumo se realiza despues del envio SMTP.

Las pruebas unitarias de duplicacion y fallo SMTP
finalizaron correctamente.

Pendiente:
- Verificar mediante una prueba de integracion el
  reprocesamiento del mismo evento.
- Documentar el comportamiento ante fallos entre el envio
  SMTP y el registro de consumo.

Nota: el envio SMTP y el registro en base de datos no
constituyen una unica transaccion atomica. Por ello,
la implementacion no garantiza entrega exactamente una vez.

## 9. Evidencias

Evidencias recomendadas para la entrega:

1. Resultado de las pruebas unitarias.
2. Pedido creado mediante la API.
3. Registro OUTBOX PROCESSED.
4. Correo HTML visualizado en Mailpit.
5. Cabecera MIME UTF-8 correctamente decodificada.

## 10. Estado de la implementacion

La creacion de pedidos, el procesamiento Outbox y el
envio de correo HTML fueron verificados mediante una
prueba de extremo a extremo.

Las cuatro pruebas unitarias del consumidor finalizaron
correctamente.

Queda pendiente la comprobacion especifica de
reprocesamiento del evento en el entorno integrado.