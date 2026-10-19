# Módulo 8 — Feature 1: recomendaciones personalizadas

Responsable: Christian Bernardo Vargas Sandoval. Fecha: 19 de octubre de 2026.

## Funcionamiento

`GetRecommendationsUseCase` lee las últimas 30 interacciones del usuario del JWT y
las relaciona con las publicaciones persistidas. Ignora referencias a publicaciones
eliminadas y, como defensa adicional, cualquier interacción con otro `userId`.

- Pesos: `VIEW=1`, `LIKE=2`, `COMMENT=2`, `FAVORITE=3`, `PURCHASE=4`.
- Preferencias: términos únicos del nombre y descripción de cada publicación del
  historial; se normalizan mayúsculas y acentos y se eliminan conectores comunes.
- Afinidad de contenido: suma de pesos de los términos coincidentes dividida por
  el número de términos únicos del candidato (mínimo divisor 1).
- Afinidad de tienda: se añade `0.25 × suma de pesos de interacciones con esa tienda`.
- Orden: afinidad descendente y UUID ascendente en empates; se aplica el límite al final.
- Disponibilidad: `activa=true` y `stock > 0`; valores nulos se excluyen.
- Se excluyen las publicaciones del historial considerado para sugerir alternativas.
- Sin historial útil, `personalized=false`: publicaciones disponibles por UUID,
  con afinidad cero. Un catálogo sin candidatos devuelve `items: []` con HTTP 200.

`affinity` es una puntuación interna, no un porcentaje ni probabilidad. No hay una
categoría persistida en el modelo actual de publicación; no se inventan categorías.
No se utilizan datos de otros compradores, datos externos ni modelos entrenados.

## Configuración y autenticación

Requiere Java 21 y la base de datos configurada por el proyecto. Antes de iniciar el
backend, configurar `APP_JWT_SECRET` con una clave aleatoria segura de al menos
32 bytes UTF-8, o proporcionar `app.jwt.secret` desde configuración externa.
No guardar la clave en Git. La configuración de pruebas es independiente.
La clave fija anterior se retiró: los JWT emitidos con ella dejan de ser válidos
al utilizar una clave nueva, por lo que se debe iniciar sesión nuevamente.

El endpoint obtiene el comprador exclusivamente del principal JWT. Un parámetro
`userId` adicional no cambia la identidad ni permite acceder a otro historial.
La regla de acceso exige autenticación; no se añade una restricción de rol porque
las cuentas existentes también pueden actuar como compradores.

## Endpoint y ejemplos

`GET /api/recommendations?limit=10`

`limit`: entero de 1 a 50; por defecto 10.

```bash
curl --get "$BASE_URL/api/recommendations" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "limit=3"
```

Respuesta ilustrativa (los IDs y puntuaciones dependen del catálogo e historial):

```json
{
  "personalized": true,
  "explanation": "Opciones relacionadas con tus interacciones recientes.",
  "aiStatus": "GENERATED",
  "items": [
    {
      "id": "00000000-0000-0000-0000-000000000003",
      "name": "Libro calculo",
      "price": 10.00,
      "stock": 5,
      "storeId": "00000000-0000-0000-0000-000000000010",
      "affinity": 0.75
    }
  ]
}
```

Para construir el historial mediante la API existente:

```bash
curl --request POST "$BASE_URL/api/interactions" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"publicationId":"00000000-0000-0000-0000-000000000001","type":"FAVORITE"}'

curl "$BASE_URL/api/interactions" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

La creación asigna siempre el usuario del JWT. La consulta individual y el borrado
`/api/interactions/{id}` verifican el propietario. Un ID ajeno y uno inexistente
producen el mismo 404 para evitar revelar la existencia de registros privados.

| Situación | Respuesta |
|---|---|
| Recomendaciones, incluso sin historial o sin candidatos | 200 |
| `limit` fuera de rango o no numérico | 400 |
| JWT inválido o vencido | 401 |
| Sin autenticación | 403, comportamiento actual de Spring Security |
| Interacción ajena o inexistente | 404 |

## Ollama y respuesta controlada

El ranking sigue siendo determinista. El puerto RecommendationTextPort solicita una
explicacion opcional a OllamaRecommendationAdapter. El modelo predeterminado es
qwen2.5:1.5b, ya disponible en el equipo; se usa POST /api/generate con stream=false.

| Variable de entorno | Valor por defecto |
|---|---|
| RECOMMENDATIONS_OLLAMA_BASE_URL | http://localhost:11434 |
| RECOMMENDATIONS_OLLAMA_MODEL | qwen2.5:1.5b |
| RECOMMENDATIONS_OLLAMA_TIMEOUT_MS | 20000 |

No necesitas configurar estas variables para usar el Ollama local. Basta mantener
Ollama iniciado y reiniciar el backend con los cambios. Para otra maquina, configurar
la URL del servidor Ollama accesible; este adaptador no configura autenticacion cloud.

Se envian como JSON los nombres de los tres primeros candidatos (maximo 120
caracteres cada uno) y si la seleccion fue personalizada. No se envian identificadores,
historial bruto ni datos personales. Las instrucciones del sistema estan separadas
de los datos. Se limita el contexto a 1024 tokens, la salida a 96 tokens y se mantiene
el modelo cargado 5 minutos. La solicitud usa num_gpu=0 para ejecutar en CPU: el
registro local mostro la carga detenida con Vulkan/GT 630; la precarga en CPU
termino en 8.7 segundos. Esto no cambia la configuracion global de Ollama.
No hay reintentos automaticos ni cola: solo una llamada
concurrente por instancia de backend para no saturar la CPU.

La espera total de la llamada HTTP esta limitada, incluida la recepcion del cuerpo.
Al agotar el tiempo se cancela la solicitud cliente; esto no garantiza que un servidor
remoto detenga inmediatamente su calculo. La primera carga del modelo puede exceder
el timeout y producir una respuesta alternativa. El limite no incluye consultas SQL.

El JSON conserva personalized e items y agrega explanation y aiStatus:

| aiStatus | Comportamiento |
|---|---|
| GENERATED | Ollama devolvio texto no vacio de hasta 1500 caracteres |
| FALLBACK | HTTP 200 con la lista calculada y explicacion determinista indicando indisponibilidad de IA |
| NOT_REQUESTED | No hay candidatos; no se llama a Ollama |

FALLBACK cubre timeout, conexion rechazada, HTTP no exitoso (incluido modelo ausente),
JSON malformado, respuesta incompleta, texto vacio/excesivo, interrupcion y generacion
ocupada. La respuesta no expone excepciones, URLs internas ni detalles del proveedor.
Los errores de base de datos no se ocultan como fallas de IA.

Ejemplo sin candidatos (sin llamada a IA):

```json
{
  "personalized": false,
  "items": [],
  "explanation": "No hay publicaciones disponibles para recomendar.",
  "aiStatus": "NOT_REQUESTED"
}
```

Con candidatos y Ollama caido, items conserva esos candidatos, aiStatus es FALLBACK
y explanation termina con: "La explicacion con IA no esta disponible en este momento."

Se mantiene getRecommendations(UUID, String) para el chat, ahora con la explicacion.
El router anterior de AIServiceImpl sigue usando su proveedor y configuracion propios;
para probar directamente qwen2.5:1.5b en este feature usar GET /api/recommendations.

La salida de IA es orientativa: puede contener imprecisiones y nunca modifica los
items, precios, stock, permisos u orden. Se debe presentar como texto, no ejecutar
HTML o instrucciones del modelo. El prompt reduce riesgos de instrucciones incluidas
en nombres, pero no garantiza que el modelo ignore todas las instrucciones maliciosas.

La afinidad lexica no reconoce todos los sinonimos. Se recorre el catalogo en memoria
y el adaptador existente carga el historial antes de limitarlo. No se midio rendimiento
con grandes volumenes ni se implemento cache. La disponibilidad debe revalidarse en
checkout. No se modifico la interfaz ni la captura automatica de interacciones.

## Validación reproducible

Desde `backend/umss-market-api`, con Java 21 y Maven:

```powershell
mvn -Precommendations-coverage "-DexcludedGroups=ollama" "-Djacoco.append=false" verify
```

También se puede utilizar `./mvnw.cmd` cuando el wrapper esté disponible en el
entorno. El perfil mantiene los umbrales de 90 % de líneas y 80 % de ramas,
aplicados a las clases del feature y al soporte modificado de seguridad y errores.
El comando ejecuta la suite existente además de los nuevos tests. El reporte HTML
queda en `target/site/jacoco/index.html` y el XML en `target/site/jacoco/jacoco.xml`.

Pruebas nuevas:

- `RecommendationsUnitTest`: preferencias distintas, ponderaciones, acentos,
  desempates, límite, disponibilidad, catálogo vacío, historial ajeno o eliminado,
  identidad requerida y contrato textual del chat.
- `RecommendationsIntegrationTest`: Spring Boot + MockMvc + H2, JWT real,
  historial persistido de dos compradores, aislamiento de lectura y borrado,
  creación para el propietario del JWT, límites inválidos y proveedor IA caído.

H2 comprueba la integración y persistencia local; esta suite no mide carga ni
sustituye una prueba de rendimiento sobre PostgreSQL.


### Pruebas del proveedor

OllamaRecommendationAdapterTest usa un servidor HTTP local controlado para verificar
payload, modelo, limites, timeout real, errores de conexion/HTTP/JSON, interrupcion,
recuperacion y concurrencia; las respuestas alternativas conservan los productos.
RecommendationsUnitTest y RecommendationsIntegrationTest verifican tambien los
estados GENERATED, FALLBACK y NOT_REQUESTED.

Prueba opcional contra tu modelo real, usando solamente productos ficticios:

```powershell
mvn "-Dtest=OllamaRecommendationLiveTest" test
```

La suite local excluye la etiqueta ollama para no depender de modelos instalados.
El perfil de cobertura incluye expresamente el nuevo adaptador HTTP.

Contrato HTTP utilizado: [API oficial de generacion de Ollama](https://docs.ollama.com/api/generate).


### Resultado verificado con Qwen (19 de octubre de 2026)

- Suite local: 177 pruebas, 0 fallos y 0 errores; BUILD SUCCESS.
- Cobertura de las 11 clases del perfil: 201/203 lineas (99.01 %) y 56/56 ramas (100 %).
- GetRecommendationsUseCase y OllamaRecommendationAdapter: 100 % de lineas y ramas.
- Prueba adicional OllamaRecommendationLiveTest: aprobada con qwen2.5:1.5b real.
  Una explicacion con el modelo ya cargado tardo 4962 ms. No es una garantia de latencia.
- Diagnostico: las cargas iniciales con Vulkan/GT 630 superaron 20 y 60 segundos.
  La precarga explicita en CPU termino en 8687 ms. El adaptador envia num_gpu=0.
- Los escenarios negativos utilizan un servidor HTTP controlado, sin detener tu Ollama.

Para probar manualmente, reiniciar el backend, mantener Ollama activo y consultar
GET /api/recommendations?limit=3 con un JWT valido y publicaciones disponibles.
Revisar explanation y aiStatus en la respuesta. No es necesario cambiar variables
para el modelo local instalado. El resto del chat conserva su router anterior.
