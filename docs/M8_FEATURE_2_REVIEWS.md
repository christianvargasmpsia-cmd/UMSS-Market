# Modulo 8 - Feature 2: persistencia de resenas y resumen inteligente

Responsable: Christian Bernardo Vargas Sandoval. Fecha: 19 de octubre de 2026.
Dependencia registrada antes de implementar: [M8-DEP-002](issues/M8-DEP-002-review-persistence.md).
Es un issue local versionado, no un issue publicado en GitHub.

## Modelo y persistencia

El producto se identifica por publicationId del catalogo. ProductReview almacena UUID,
publicationId, buyerId, rating (entero 1..5), comment (1..1000 caracteres, no blanco)
y createdAt en UTC. Se rechazan ratings decimales o en formato de texto, y comentarios
que no sean cadenas; no se permite la conversion silenciosa de tipos JSON. El comprador procede del JWT y debe existir en users; cualquier
buyerId recibido en el JSON se ignora. Se comprueba que la publicacion exista.

La tabla product_reviews tiene claves foraneas a users y publications, restricciones
de campos y un indice por publicacion, fecha e ID. Los DTO publicos no exponen buyerId.
El adaptador JPA filtra por publicacion y limita en SQL, con orden fecha/UUID descendente.
Se permiten varias resenas por comprador; no se exige compra verificada. Estas reglas
no estaban definidas en el alcance y no se agregan restricciones de negocio nuevas.
No se exponen endpoints de edicion ni eliminacion; el resumen nunca modifica originales.

## Migracion

Archivo: [V20261019_01__create_product_reviews.sql](../backend/umss-market-api/src/main/resources/db/migration/V20261019_01__create_product_reviews.sql).

El proyecto usa Hibernate ddl-auto=update en desarrollo. La nueva entidad permite que
Hibernate cree la tabla al iniciar. No se agrego Flyway: guardar un SQL en db/migration
no significa que Spring lo ejecute automaticamente.

Para un despliegue administrado, aplicar el SQL sobre la base destino antes de iniciar
la aplicacion. Las tablas users y publications deben existir. Ejemplo desde el backend,
con las variables de conexion PostgreSQL configuradas externamente:

```powershell
psql -v ON_ERROR_STOP=1 -d $env:PGDATABASE -f src/main/resources/db/migration/V20261019_01__create_product_reviews.sql
```

El script puede repetirse y no modifica filas existentes. IF NOT EXISTS no repara una
tabla previa con esquema incompatible. Las pruebas ejecutan el SQL en H2 con modo
PostgreSQL, incluyendo claves foraneas, CHECK e indice; no se aplico a tu base real.

## Endpoints

| Metodo y ruta | Acceso | Resultado |
|---|---|---|
| POST /api/publications/{publicationId}/reviews | JWT, comprador persistido | 201, resena guardada |
| GET /api/publications/{publicationId}/reviews?limit=20 | Publico | 200, lista de las mas recientes |
| GET /api/publications/{publicationId}/reviews/summary | Publico | 200, resumen o estado informativo |

limit acepta 1..50. La consulta no es una exportacion de todas las resenas.

Crear una resena (sustituir PRODUCT_ID y ACCESS_TOKEN por valores reales):

```bash
curl --request POST "$BASE_URL/api/publications/$PRODUCT_ID/reviews" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"rating":4,"comment":"La calidad es excelente. La entrega fue lenta."}'

curl "$BASE_URL/api/publications/$PRODUCT_ID/reviews?limit=20"
curl "$BASE_URL/api/publications/$PRODUCT_ID/reviews/summary"
```

Ejemplo ilustrativo de resumen de esa resena:

```json
{
  "publicationId":"00000000-0000-0000-0000-000000000001",
  "reviewCount":1,
  "consideredReviewCount":1,
  "sampleLimit":10,
  "status":"GENERATED",
  "summary":"Se analizaron 1 resenas. Positivos: [La calidad es excelente]. Negativos: [La entrega fue lenta].",
  "positiveAspects":[{"reviewId":"00000000-0000-0000-0000-000000000002","quote":"La calidad es excelente"}],
  "negativeAspects":[{"reviewId":"00000000-0000-0000-0000-000000000002","quote":"La entrega fue lenta"}]
}
```

| Situacion | Respuesta |
|---|---|
| Producto inexistente, tambien al crear o listar | 404 |
| Rating/texto/limit invalidos o JSON malformado | 400 |
| Creacion sin autenticacion | 403 (comportamiento actual de Spring Security) |
| Token invalido o usuario del JWT inexistente | 401 |
| Sin resenas | 200, NO_REVIEWS, reviewCount=0; ninguna llamada a IA |
| Sin fragmentos/opiniones con evidencia suficiente | 200, NO_EVIDENCE; listas vacias |
| Error, timeout o respuesta invalida de Ollama | 200, UNAVAILABLE; listas vacias |

UNAVAILABLE informa: "No se pudo generar el resumen. Las resenas originales siguen
disponibles." No se disfraza una falla como un producto sin opiniones.

## Resumen con evidencia verificable

1. Consulta las 10 ultimas resenas de la publicacion. Descarta defensivamente datos
   de otro producto aunque un adaptador defectuoso los devuelva.
2. Divide comentarios en fragmentos por punto, interrogacion, exclamacion o salto de
   linea. Considera hasta 24 fragmentos completos de 4..240 caracteres. No trunca ni
   reescribe citas. Se omiten fragmentos con marcadores frecuentes de instrucciones.
3. Envia indices locales, textos y calificaciones originales como JSON, separado del
   mensaje de sistema. No envia datos del comprador ni IDs de otras publicaciones.
4. Se solicita a Ollama hasta tres indices positivos y tres negativos, en JSON sujeto
   a esquema. No genera la sintesis ni opiniones nuevas. Si devuelve mas indices
   validos, el backend muestra solamente los primeros tres de cada categoria.
5. El backend rechaza campos extra, indices inexistentes, duplicados, listas invalidas
   y referencias compartidas entre positivos y negativos, incluso despues del tercer
   indice; valida toda la seleccion antes de limitarla. Construye una sintesis
   extractiva y los aspectos con las citas originales y sus reviewId.

reviewCount es el numero de resenas distintas presentes en los fragmentos enviados
al modelo, no el total de resenas del producto. consideredReviewCount es la cantidad
consultada para preparar la muestra. Si el corte de 24 fragmentos o los filtros
excluyen resenas, estos numeros pueden diferir. En UNAVAILABLE, reviewCount describe
la muestra del intento fallido; no afirma que se obtuvo un analisis exitoso.

La IA no tiene herramientas para ejecutar comandos, SQL o consultar otros productos.
Una instruccion maliciosa nunca puede introducir texto libre en el resumen: la salida
aceptada contiene exclusivamente indices de la muestra. Los filtros de palabras son
una defensa conservadora adicional, no una garantia de detectar toda inyeccion.

## Configuracion de Ollama

| Variable de entorno | Por defecto |
|---|---|
| REVIEWS_OLLAMA_BASE_URL | http://localhost:11434 |
| REVIEWS_OLLAMA_MODEL | qwen2.5:1.5b |
| REVIEWS_OLLAMA_TIMEOUT_MS | 45000 |

Usa CPU (num_gpu=0), contexto 4096, hasta 80 tokens de salida, temperatura cero,
stream=false y keep_alive=5m. El timeout incluye recibir el cuerpo de respuesta HTTP.
Existe una sola solicitud concurrente por instancia de este adaptador, sin cola ni
reintentos automaticos. Una solicitud simultanea recibe UNAVAILABLE. No se mantiene
una transaccion SQL abierta mientras se espera al modelo en ejecucion normal.

La precarga real del modelo/contexto de resumen tardo 37.1 segundos en este equipo.
Para una demostracion, puedes cargarlo antes de consultar el endpoint desde PowerShell:

```powershell
$body = @{
  model = 'qwen2.5:1.5b'
  stream = $false
  keep_alive = '10m'
  options = @{ num_ctx = 4096; num_predict = 80; num_gpu = 0; temperature = 0 }
} | ConvertTo-Json -Depth 4
Invoke-RestMethod 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 90
```

Esta precarga manual no elimina el limite de 45 segundos del endpoint.
El timeout de resumen es independiente del de recomendaciones. Reiniciar el backend
tras cambiar configuracion. La cancelacion cliente no garantiza detener inmediatamente
el computo en cualquier servidor remoto.

## Limitaciones

- Resumen extractivo, no una reformulacion generativa libre. La clasificacion de tono
  puede equivocarse; las citas permiten revisar la evidencia, no prueban veracidad.
- No se resume todo el historial: muestra reciente y acotada; no hay estimacion estadistica.
- Los fragmentos largos, demasiado cortos o detectados como instrucciones se omiten;
  puede haber opiniones legitimas no incluidas. No se alteran las resenas originales.
- Texto de citas y resenas debe mostrarse como texto en el cliente, nunca ejecutarse
  como HTML o comandos. No se implemento una nueva interfaz.
- No hay edicion, eliminacion, nuevas calificaciones, verificacion de compra ni cache.
- H2 no sustituye una validacion de carga o compatibilidad operacional en PostgreSQL.

## Pruebas y cobertura

Desde backend/umss-market-api con Java 21:

```powershell
mvn -Previews-coverage "-DexcludedGroups=ollama" "-Djacoco.append=false" verify
mvn "-Dtest=ReviewSummaryLiveTest" test
```

El primer comando verifica la suite local, incluyendo tests unitarios, Spring Boot +
MockMvc + H2, servidor HTTP simulado y migracion SQL. Excluye pruebas de modelos reales.
El segundo usa el Qwen instalado, con dos opiniones ficticias y sin datos de usuarios.
El wrapper puede sustituir mvn si esta operativo en el entorno.

El perfil reviews-coverage incluye casos de uso, DTO, modelo, adaptadores, controlador
y configuracion de seguridad; exige al menos 90 % de lineas y 80 % de ramas. Reportes:
target/site/jacoco/index.html, target/site/jacoco/jacoco.xml y target/surefire-reports.

Casos: persistir y recargar desde BD, propietario del JWT, datos invalidos, productos
inexistentes, aislamiento entre productos, conteo exacto de muestra, sin resenas,
fragmentos maliciosos, referencias inventadas, timeout, desconexion, HTTP/JSON invalidos,
concurrencia y recuperacion. Las pruebas no borran ni modifican resenas reales.

## Evidencia de validacion local (19 de octubre de 2026)

- Suite local: 196 pruebas, 0 fallos, 0 errores; BUILD SUCCESS.
- Feature 2 agrega 19 pruebas locales: 8 unitarias, 5 de integracion Spring/JPA/HTTP,
  5 del transporte Ollama contra servidor controlado y 1 de migracion SQL.
- JaCoCo, 15 clases con codigo ejecutable del perfil: 151/151 lineas y 102/102 ramas
  (100 % en ambas metricas). Verificacion de umbrales aprobada.
- Dependencia local M8-DEP-002 resuelta mediante persistencia implementada y probada.
- Prueba adicional real con qwen2.5:1.5b precargado: aprobada en 6035 ms;
  seleccion positiva [0] y negativa [1] correctas. La carga en frio agoto el
  timeout en intentos previos; la precarga manual posterior tardo 37.1 segundos.

### Correccion de selecciones extensas

Con cinco resenas, Qwen podia devolver cinco indices positivos validos aunque se
solicitaban tres. Antes se rechazaba toda la respuesta como UNAVAILABLE. Ahora se
validan todos los indices y se muestran los primeros tres por categoria. No se
aceptan indices inventados o compartidos entre categorias, tampoco fuera del limite
visible. Se agregaron pruebas de regresion para ambos casos.

La consulta manual al endpoint del backend con cinco resenas devolvio GENERATED,
tres positivos y tres negativos. La clasificacion de sentimiento sigue siendo una
limitacion del modelo: las citas son originales, pero su categoria puede ser erronea.

Validacion posterior a la correccion: 20 pruebas locales de resenas aprobadas y
2 pruebas adicionales con Ollama real aprobadas. Cobertura: 151/151 lineas y
100/100 ramas. La suite completa ejecuto 197 pruebas; 195 pasaron y dos fallaron
en TransactionalOutboxTest (processesPendingAndIgnoresDuplicateDispatch y
retriesAfterTemporaryFailureAndRecovers), fuera del codigo modificado. Los fallos
se repitieron en una segunda ejecucion; la suite global no queda aprobada.
