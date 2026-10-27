# Validación local — Demo 2 recomendaciones

Fecha: 26/10/2026, America/La_Paz. Rama inicial y final: `release/8.2.0`.
Estado inicial: `git status --short` vacío. No se creó commit, push, merge ni PR.
No existía `.github/workflows/`; se añadió el workflow sin sustituir trabajo previo.
El aporte y la fila de Melani se conservaron. No se modificó la lógica del ranking.

## Resultado final

**LISTA PARA DEMO como backend/API.** Ejecución local verificada; el resultado del
workflow remoto solo podrá confirmarse después de publicar el PR.

| Suite final | Ejecutados | Aprobados | Fallos/errores | Omitidos |
| --- | ---: | ---: | ---: | ---: |
| RecommendationsUnitTest | 8 | 8 | 0 | 0 |
| RecommendationsIntegrationTest (H2) | 9 | 9 | 0 | 0 |
| RecommendationsPostgresTest | 9 | 9 | 0 | 0 |
| OllamaRecommendationAdapterTest | 7 | 7 | 0 | 0 |
| RedTeamSecurityTest | 1 | 1 | 0 | 0 |
| AIServiceImplTest | 4 | 4 | 0 | 0 |
| OpenApiContractTest | 1 | 1 | 0 | 0 |
| **Total** | **39** | **39** | **0** | **0** |

BUILD SUCCESS, 50.727 s en la ejecución final. Durante esta preparación se ejecutaron
30 + 9 + 39 + 39 = 117 ejecuciones JUnit contando repeticiones; todas aprobaron.
No se ejecutó OllamaRecommendationLiveTest ni se exige Ollama real para aprobar.
No se afirma aprobación de toda la suite del repositorio.

JaCoCo final: GetRecommendationsUseCase 57/57 líneas y 42/42 ramas;
OllamaRecommendationAdapter 36/36 líneas y 14/14 ramas;
RecommendationsController y OpenApiConfig 1/1 líneas cada uno.
Se ejecutó `test jacoco:report`, no el gate global de `verify`.

## Comando exacto de la suite final en este equipo

Desde `backend/umss-market-api`:

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot'
$env:DEMO_POSTGRES_URL='jdbc:postgresql://127.0.0.1:15432/umss_recommendations_test'
$env:DEMO_POSTGRES_USER='demo'
$env:DEMO_POSTGRES_PASSWORD=''
& 'C:/Users/Edwing/.m2/wrapper/dists/apache-maven-3.9.16/0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0/bin/mvn.cmd' -o '-Dmaven.repo.local=C:/Users/Edwing/.m2/repository' '-Dtest=RecommendationsUnitTest,RecommendationsIntegrationTest,RecommendationsPostgresTest,OllamaRecommendationAdapterTest,RedTeamSecurityTest,AIServiceImplTest,OpenApiContractTest' '-Djacoco.append=false' test jacoco:report *> target/demo2-final-tests.log
```

Las ejecuciones anteriores usaron el mismo Maven:
primero `-Dtest=RecommendationsUnitTest,RecommendationsIntegrationTest,OllamaRecommendationAdapterTest,RedTeamSecurityTest,AIServiceImplTest,OpenApiContractTest`
con `test jacoco:report`; después `-Dtest=RecommendationsPostgresTest` con `test`;
finalmente dos veces el comando conjunto tras cambios de anotaciones y regresión del esquema.

## PostgreSQL real y HTTP

Servidor local instalado: PostgreSQL **18.4**, Windows, Java 21.0.12.
Se creó un clúster nuevo en `%TEMP%/umss-demo2-postgres`, loopback:15432,
con usuario `demo` y bases `umss_recommendations_test` y `umss_recommendations_demo`.
No se utilizó la base configurada por defecto ni el servicio PostgreSQL existente.
El clúster temporal se detiene al finalizar la revisión; la guía permite recrear la demo.
CI configura PostgreSQL 16; no se atribuye a esa versión una ejecución local no realizada.

Se verificó generación Hibernate del esquema, columnas UUID/TEXT/timestamps de
`interactions`, enum con los cinco tipos, persistencia/lectura/borrado por JPA y
consultas por usuario/fecha. No apareció incompatibilidad H2/PostgreSQL que exigiera
modificar producción. No hay Flyway configurado: el SQL versionado de reseñas no
se ejecuta automáticamente y es ajeno al esquema de recomendaciones.

Para la prueba HTTP se inició `UmssMarketApiApplication` con el classpath generado
por Surefire, configuración principal explícita, PostgreSQL demo, JWT aleatorio,
puerto 18089, Ollama apuntando a loopback:1 y timeout 1000 ms. No se guardó la clave.
Se cargó el seed con psql y se ejecutó:

```powershell
$env:UMSS_DEMO_BUYER_PASSWORD=([guid]::NewGuid().ToString('N'))
& 'E:/Programas/PostgreSQL/18/bin/psql.exe' -h 127.0.0.1 -p 15432 -U demo -d umss_recommendations_demo -f docs/demos/recommendations-seed.sql
$env:DEMO_CONFIRM_ISOLATED='umss_recommendations_demo'
$env:DEMO_EXPECT_FALLBACK='true'
node docs/demos/recommendations-demo.cjs > backend/umss-market-api/target/demo2-http-evidence.json
```

Resultado: PASS. A: `Tecnologia computadora accesorio 2`; B: `Deporte futbol accesorio 2`.
El script verifica afinidad positiva y resultados diferentes, sin sustituir el ranking
del backend. C obtuvo 12 publicaciones generales. Se verificaron JWT por login real,
flujo POST interacción → GET recomendaciones, stock cero/inactiva excluidos,
identidad no suplantable mediante userId, lectura/borrado ajenos 404,
limit=1/5/10 y errores 0/51/abc, anónimo 403 y JWT inválido 401, y fallback real del proveedor.

El primer intento del script HTTP falló al interpretar como JSON una respuesta de
error vacía. Se corrigió el lector del script y la ejecución completa posterior pasó.
No fue un fallo del backend ni se ocultó una prueba JUnit fallida.

## OpenAPI, Spectral y drift

Se generó el contrato mediante `/v3/api-docs` del contexto Spring Boot real.
Se corrigió una colisión del nombre `Item` con checkout: el esquema de recomendaciones
se llama ahora `RecommendationItem`; la respuesta HTTP conserva los mismos campos.
El test de contrato verifica esos campos, JWT, límites y respuestas.

Operaciones presentes en ambas copias:
GET `/api/recommendations`; GET/POST `/api/interactions`;
GET/DELETE `/api/interactions/{id}`.

Desde la raíz se ejecutó:

```powershell
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json openapi.json --update
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json ai-testing-agent/collections/umss-market-openapi.json --update
node docs/demos/check-openapi.cjs
node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi.json ai-testing-agent/collections/umss-market-openapi.json
npm.cmd install --prefix docs/demos/tools --ignore-scripts --no-audit --no-fund
node docs/demos/tools/node_modules/@stoplight/spectral-cli/dist/index.js lint openapi.json --ruleset .spectral.yaml --fail-severity error --format json --output backend/umss-market-api/target/spectral.json
```

Spectral 6.15.0: **0 errores, 89 warnings**, exit 0.
Warnings: 44 descripciones de operaciones, 44 tags no definidos de otras operaciones,
1 contacto general ausente. No hay warnings en las cinco operaciones de esta entrega.
No se desactivaron reglas: `.spectral.yaml` extiende `spectral:oas`.
El lockfile fija las dependencias para `npm ci` en CI.

Comparación normal: ambas copias coinciden, exit 0. Prueba negativa: se cambió únicamente
`info.version` en `target/openapi-drift-probe.json`, manteniendo el contrato oficial intacto;
`node docs/demos/check-openapi.cjs backend/umss-market-api/target/openapi-drift-probe.json openapi.json`
devolvió exit 1 como corresponde. Espacios y orden de claves no causan falsos positivos.

Workflow: `.github/workflows/openapi.yml`, cada PR y ejecución manual; pruebas y
generación, drift de ambas copias, Spectral y artefactos. No se ejecutó en GitHub todavía.

## Evidencias locales y cierre

Los reportes se generan en `backend/umss-market-api/target/` (ignorado por Git):
`demo2-final-tests.log`, `surefire-reports/`, `site/jacoco/`, `demo2-http-evidence.json`,
`openapi.json`, `spectral.json`, `demo2-drift-probe.log`.
No se versionan tokens, contraseñas, logs de servidor ni dependencias node_modules.
`git diff --check` sin errores; revisión de diff/status realizada.

Pendientes reales: publicar rama/PR y comprobar CI remoto; integrar tras revisión;
registrar enlace y evidencia en Classroom. No se consideran bloqueos de esta demo
la ausencia de frontend/captura automática ni el modelo Ollama apagado.
