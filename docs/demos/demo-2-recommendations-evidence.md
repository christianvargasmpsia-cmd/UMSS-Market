# Demo 2 — recomendaciones personalizadas — Christian Vargas

Entrega backend/API de release/8.2.0. No requiere frontend, captura automática ni Ollama activo.
Contrato oficial: `openapi.json` en la raíz. `ai-testing-agent/collections/umss-market-openapi.json`
es una copia compatible para las herramientas existentes; CI comprueba ambos contra el código.

## Caso de uso M8-REC-01 — Obtener recomendaciones propias

Actor: comprador autenticado. Objetivo: descubrir publicaciones disponibles relacionadas
con su historial. Precondiciones: JWT válido y catálogo persistido. Flujo: registrar
interacción → solicitar recomendaciones → recibir candidatos ordenados y explicación.
Alternativas: historial vacío/eliminado → catálogo general; Ollama indisponible → mismo
ranking con texto determinista; catálogo sin candidatos → lista vacía.

Este identificador es una extensión funcional M8 documentada aquí, no un UC ya numerado
en `docs/fsd/FSD_v4.md`. Ese FSD antiguo deja recomendaciones de clientes fuera de su
alcance; no se atribuye esta función a UC-006 (que analiza pruebas técnicas).

## Criterios de aceptación

1. Se exige autenticación; sin JWT devuelve 403, JWT inválido/vencido devuelve 401.
2. Solo se consideran las últimas 30 interacciones del principal JWT; `userId` externo no cambia la identidad.
3. Pesos: VIEW=1, LIKE=2, COMMENT=2, FAVORITE=3, PURCHASE=4. LIKE y COMMENT comparten peso.
4. Solo se devuelven publicaciones con `activa=true`.
5. Solo se devuelven publicaciones con `stock>0`.
6. Se excluyen las publicaciones existentes referenciadas por esas 30 interacciones propias.
7. `limit` limita los resultados, por defecto 10, válido 1..50. No garantiza completar el cupo.
8. Sin historial útil: `personalized=false`, afinidad cero y orden por UUID; no es popularidad.
9. Ollama solo explica: un fallo devuelve HTTP 200 y `aiStatus=FALLBACK`, conservando productos y orden.
10. Una interacción ajena o inexistente devuelve 404 al consultar/borrar. La lista solo muestra las propias.

La afinidad suma pesos de palabras coincidentes normalizadas de nombre/descripción,
divide por la cantidad de palabras únicas del candidato y añade afinidad de tienda
(0.25 por la suma de pesos de esa tienda). Desempata por UUID. No usa embeddings,
categorías de publicación, búsquedas, compras efectivas ni otros compradores.
PURCHASE es una señal declarada por API, no una compra verificada. No se integra SIIS
en esta demo: se usan exclusivamente cuentas sintéticas preexistentes en una base aislada.

## Preparación (PowerShell, desde la raíz)

Requisitos: Java 21, Maven (`mvn` o wrapper), Node 22+, Docker con PostgreSQL 16
o instalación local PostgreSQL. No reutilizar una base de producción.

```powershell
# PostgreSQL solo accesible en loopback; trust es exclusivamente para este contenedor de demo.
docker run --name umss-demo2-pg -p 127.0.0.1:15432:5432 -e POSTGRES_USER=demo -e POSTGRES_DB=umss_recommendations_demo -e POSTGRES_HOST_AUTH_METHOD=trust -d postgres:16
docker exec umss-demo2-pg pg_isready -U demo -d umss_recommendations_demo
# Esperar hasta que pg_isready indique accepting connections.
docker exec umss-demo2-pg createdb -U demo umss_recommendations_test

$env:SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:15432/umss_recommendations_demo'
$env:SPRING_DATASOURCE_USERNAME='demo'
$env:SPRING_DATASOURCE_PASSWORD=''
$env:APP_JWT_SECRET=([guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N'))
# Falla controlada del proveedor; el resto de la IA no cambia de configuración global.
$env:RECOMMENDATIONS_OLLAMA_BASE_URL='http://127.0.0.1:1'
$env:RECOMMENDATIONS_OLLAMA_TIMEOUT_MS='1000'
Push-Location backend/umss-market-api
./mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=18089'
```

Mantener esa terminal abierta. Hibernate usa el mecanismo existente `ddl-auto=update`
para crear el esquema en esta base vacía. No se introdujo Flyway: la migración SQL
existente de reseñas no es ejecutada automáticamente y no se necesita para recomendaciones.
No hay migración nueva ni cambio de esquema para esta entrega.

En una segunda terminal, desde la raíz:

```powershell
# Guardar la variable en esta sesión; nunca versionar su valor.
$env:UMSS_DEMO_BUYER_PASSWORD=([guid]::NewGuid().ToString('N'))
Get-Content docs/demos/recommendations-seed.sql -Raw | docker exec -i -e UMSS_DEMO_BUYER_PASSWORD umss-demo2-pg psql -U demo -d umss_recommendations_demo
$env:DEMO_BASE_URL='http://127.0.0.1:18089'
$env:DEMO_CONFIRM_ISOLATED='umss_recommendations_demo'
$env:DEMO_EXPECT_FALLBACK='true'
node docs/demos/recommendations-demo.cjs
```

Si se usa PostgreSQL local en lugar de Docker:

```powershell
# psql debe estar en PATH; adaptar -U/-p al clúster AISLADO creado previamente.
psql -h 127.0.0.1 -p 15432 -U demo -d umss_recommendations_demo -f docs/demos/recommendations-seed.sql
```

El seed verifica el nombre exacto de la base. Crea tres compradores, dos tiendas
y 14 publicaciones: 6 de tecnología, 6 de deporte, una agotada y una inactiva.
Utiliza pgcrypto y bcrypt cost 12, tomando la contraseña desde el entorno.
Reejecutarlo renueva las contraseñas de las cinco cuentas sintéticas; no borra datos.
El script HTTP reinicia únicamente las interacciones de buyer-a/b/c@demo.invalid
mediante sus endpoints autenticados para repetir la demostración. No usar en otra instancia.

## Defensa paso a paso

1. Mostrar la base aislada, las 14 publicaciones y los dos registros no disponibles.
2. Login de A y consultar antes de registrar historial: `personalized=false`.
3. Registrar FAVORITE sobre la publicación de tecnología `...000101`; consultar: afinidad positiva y alternativas tecnológicas.
4. Login de B, registrar PURCHASE sobre deporte `...000201`; consultar: alternativas deportivas diferentes.
5. Comparar listas y comprobar que sus respectivas publicaciones fuente no se repiten.
6. Login de C, sin interacciones: `personalized=false`, 12 disponibles, afinidad cero.
7. Mostrar que `...000301` (stock cero) y `...000302` (inactiva) nunca aparecen.
8. Mostrar limit=1/5/10, errores de 0/51/abc, y 404 al consultar/borrar una interacción de A con JWT de B.
9. Mostrar `aiStatus=FALLBACK`: proveedor caído sin afectar el ranking.
10. Abrir `http://127.0.0.1:18089/swagger-ui/index.html`, usar Authorize y mostrar `/api/recommendations` y `/api/interactions`.
11. Mostrar pruebas, `openapi.json`, Spectral y workflow. No afirmar CI remoto verde antes de ejecutarlo en GitHub.

El script `recommendations-demo.cjs` ejecuta y comprueba estos escenarios por HTTP real;
imprime solo respuestas de recomendaciones, sin contraseñas ni tokens.

### Requests manuales (cURL/Postman; reemplazar placeholders, no guardar tokens)

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{"email":"buyer-a@demo.invalid","password":"<UMSS_DEMO_BUYER_PASSWORD>"}

POST {{baseUrl}}/api/interactions
Authorization: Bearer <TOKEN_A>
Content-Type: application/json

{"publicationId":"00000000-0000-0000-0000-000000000101","type":"FAVORITE"}

GET {{baseUrl}}/api/recommendations?limit=5
Authorization: Bearer <TOKEN_A>

GET {{baseUrl}}/api/interactions
Authorization: Bearer <TOKEN_A>

GET {{baseUrl}}/api/interactions/<INTERACTION_ID_A>
Authorization: Bearer <TOKEN_B>
```

Repetir login con `buyer-b@demo.invalid` y `buyer-c@demo.invalid`; la contraseña es la
variable usada en el seed. No se requiere registrar usuarios ni simular validación SIIS.

## Tests, contrato y Spectral

```powershell
$env:DEMO_POSTGRES_URL='jdbc:postgresql://127.0.0.1:15432/umss_recommendations_test'
$env:DEMO_POSTGRES_USER='demo'
$env:DEMO_POSTGRES_PASSWORD=''
Push-Location backend/umss-market-api
./mvnw.cmd '-Dtest=RecommendationsUnitTest,RecommendationsIntegrationTest,RecommendationsPostgresTest,OllamaRecommendationAdapterTest,RedTeamSecurityTest,AIServiceImplTest,OpenApiContractTest' '-Djacoco.append=false' test jacoco:report
Pop-Location
node docs/demos/check-openapi.cjs
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json ai-testing-agent/collections/umss-market-openapi.json
npm ci --prefix docs/demos/tools --ignore-scripts
node docs/demos/tools/node_modules/@stoplight/spectral-cli/dist/index.js lint openapi.json --ruleset .spectral.yaml --fail-severity error --format json --output backend/umss-market-api/target/spectral.json
```

`RecommendationsPostgresTest` requiere el nombre exacto `umss_recommendations_test`;
usa `create-drop` solo allí y rollback por escenario. Sin variable se omite: eso NO
es evidencia PostgreSQL. Ollama live no forma parte del requisito de éxito.
`OpenApiContractTest` obtiene `/v3/api-docs` del contexto Spring real, comprueba seguridad,
límites y operaciones y exporta `target/openapi.json`. No redacta un contrato manual.

Si el código cambió intencionalmente, después de revisar el contrato generado:

```powershell
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json openapi.json --update
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json ai-testing-agent/collections/umss-market-openapi.json --update
```

CI ejecuta generación, comparación estructural (ignora solo espacios/orden de claves)
y Spectral en cada PR. Un drift falla; los temporales permanecen en `target/`, ignorado.
Los resultados de esta revisión y comandos locales exactos están en
`docs/demos/demo-2-recommendations-validation.md`.

## Límites que explicar honestamente

Catálogo e historial se cargan en memoria antes de filtrar/limitar; no se ha medido
rendimiento a escala. No se presenta paginación SQL ni EXPLAIN como implementados.
La disponibilidad se revalida en compra. No hay captura automática ni interfaz nueva.
Las recomendaciones están completas para la demo por API, no son un modelo entrenado.
