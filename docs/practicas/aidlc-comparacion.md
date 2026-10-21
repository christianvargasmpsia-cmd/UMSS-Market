# Práctica: AI-DLC de AWS con y sin herramienta

## 1. Datos

- Modalidad: grupo
- Repositorio: [UMSS Market](https://github.com/christianvargasmpsia-cmd/Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales)
- Rama sin herramienta (A): feature-sin-aidlc
- Rama con herramienta (B): feature-con-aidlc
- Video (opcional): [COMPLETAR: enlace del video, si corresponde]
- Commit base común: `11e6f6c04c2d66d4e5fd39a38c0f75076815b457`, confirmado mediante `git merge-base`.
- Referencias examinadas: A termina en `08b8982`; B termina en `3f960a7`. La comparación usa los archivos versionados de esas referencias, no cambios locales de auditoría posteriores.

| Integrante | Qué hizo | Commits o PR |
|---|---|---|
| Abad Melani Rodriguez Gonzales (`melrodriguez1133`) | Versión A: endpoint básico, pruebas, corrección de HTTP 405 y evidencias. Responsable confirmado por el README de A y por el equipo. | `544a848`, `6cd33c1`, `08b8982` |
| Christian Vargas | Versión B: implementación con consulta de BD, preparación de AI-DLC, ejecución del flujo Express y validación. Responsable confirmado por el equipo. | `b98f618`, `0578a22`, `3f960a7` |

Método: revisión de `git log`, diferencias de cada rama contra el base, diferencias entre ramas y lectura de código, pruebas y evidencias versionadas. No se reejecutaron pruebas para redactar esta comparación; los resultados se atribuyen a sus registros originales.

## 2. El feature

El objetivo de la práctica fue comparar el desarrollo asistido directamente por IA con el desarrollo acompañado por AWS AI-DLC, tomando `GET /api/health` como caso de estudio. La petición de B exige comprobar que el backend responde, ejecutar una comprobación real de conectividad con la base de datos, informar el resultado y disponer de pruebas automatizadas.

Durante la auditoría final se detectó que las implementaciones no alcanzaron exactamente el mismo alcance funcional:

- A implementa health básico del backend: una respuesta HTTP estática que identifica el servicio.
- B agrega comprobación real de conectividad SQL, aplicable al PostgreSQL configurado por el backend. Su prueba de integración utiliza H2, no un PostgreSQL externo.

Ambas ramas parten del mismo commit, pero esa condición no garantiza equivalencia funcional ni experimental. No todas las diferencias de tiempo, cantidad de pruebas o complejidad pueden atribuirse únicamente a AI-DLC. Además, ambas versiones utilizaron asistencia de IA; A significa «sin AI-DLC», no «sin IA».

## 3. Versión A: sin la herramienta

- **Forma de trabajo:** asistencia directa de ChatGPT, implementación y validación iterativas, sin el flujo de AI-DLC. El README de evidencias describe correcciones y comprobaciones en Postman.
- **Resultado:** `GET /api/health` responde HTTP 200 con `{"status":"UP","service":"umss-market-api"}`. `POST /api/health` devuelve HTTP 405 después de corregir el manejador global.
- **Límite funcional:** el controlador no consulta PostgreSQL ni ejecuta `SELECT 1`; no usa `DataSource`, `JdbcTemplate`, repositorio o servicio de salud de BD. No devuelve `database=UP/DOWN` ni 503 por caída de BD. Resolver la autenticación local de PostgreSQL para arrancar el servidor no convierte este endpoint en una comprobación de conectividad.
- **Tiempo:** 1 hora, 5 minutos y 36 segundos, según el registro del equipo en `docs/practicas/evidencias/sin-aidlc/README.md`. Incluye implementación, pruebas, correcciones, documentación y publicación. Es un tiempo declarado en la evidencia, no una medición reconstruida a partir de commits.
- **Intervenciones/correcciones documentadas:** cuatro: corregir la ubicación inicial de `HealthControllerTest.java`; utilizar Maven Wrapper porque `mvn` no estaba disponible globalmente; resolver la autenticación local de PostgreSQL; reproducir y corregir HTTP 500 en POST para obtener 405. No equivale al total de mensajes o interacciones humanas.
- **Pruebas propias del feature:** `HealthControllerTest` tiene dos casos, `healthReturnsStatusAndService` y `healthRejectsPostMethod`, ambos aprobados. Se usa MockMvc standalone con `GlobalExceptionHandler` incorporado.
- **Resultados ejecutados:** `health-tests.log` acredita 6/6 pruebas aprobadas, 0 fallos y 0 errores: las dos anteriores más cuatro pruebas preexistentes de `GlobalExceptionHandlerTest`. Este último archivo no cambió respecto al base. No se ejecutó la suite completa según el README.
- **Cobertura:** no verificable porcentualmente con las evidencias versionadas. El log muestra instrumentación de JaCoCo, pero no un reporte versionado que permita afirmar 90%, 80% o 100%. Pruebas aprobadas no equivalen a cobertura completa.

Archivos de implementación y pruebas, relativos a `backend/umss-market-api/src/`:

| Archivo | Cambio respecto al base |
|---|---|
| `main/java/bo/umss/market/umss_market_api/infrastructure/controllers/HealthController.java` | Agregado: respuesta básica del servicio. |
| `main/java/bo/umss/market/umss_market_api/shared/GlobalExceptionHandler.java` | Modificado: captura `HttpRequestMethodNotSupportedException`, devuelve 405 y conserva los métodos permitidos en la cabecera `Allow` cuando están disponibles. Su alcance es global. |
| `test/java/bo/umss/market/umss_market_api/infrastructure/controllers/HealthControllerTest.java` | Agregado: casos GET y POST. |

Evidencias adicionales: `docs/practicas/evidencias/sin-aidlc/README.md`, `health-tests.log`, `postman-get-200.png` y `postman-post-405.png`, disponibles en A. En total, el diff de A contra el base comprende siete archivos. No hay nuevas dependencias ni cambios en `pom.xml`.

**Observación crítica:** A presenta una solución pequeña y una corrección HTTP demostrable. Su validación inicial no cubría suficientemente la interacción con el manejador global; la prueba posterior sí reprodujo el problema. No se verificó conectividad de BD ni regresión general.

## 4. Versión B: con AI-DLC AWS 2.11.0 Express

- **Invocación:** el equipo utilizó `$aidlc express` con la solicitud de implementar `GET /api/health` y comprobar la base de datos. La auditoría conserva la petición normalizada como `/aidlc Implementar un endpoint GET /api/health que verifique que el backend está funcionando y que existe conexión con la base de datos. Debe realizar una comprobación real de conectividad con la base de datos devolver el estado correspondiente y contar con pruebas automatizadas.` El campo `Scope` acredita `express`; no debe confundirse ese texto normalizado con una captura literal de todos los argumentos originales.
- **Versión:** `.aidlc-version` contiene `2.11.0`. La ejecución corresponde al perfil Express, profundidad Minimal y estrategia de pruebas Minimal.
- **Puertas revisadas:** la auditoría versionada registra cuatro `GATE_APPROVED`: Reverse Engineering, Requirements Analysis, Code Generation y Build and Test. También registra una respuesta `Accept failure`. El estado final es `Completed` y existe `WORKFLOW_COMPLETED`.
- **Configuración efectiva:** Express mantuvo apagados sensores, revisores adicionales, colaboradores, learnings, confirmación del resumen y aprobación previa del plan. Se aprobó la finalización de Code Generation, pero no hubo una puerta separada de aprobación previa de su plan. No se atribuyen a esta ejecución controles que estaban desactivados.
- **Resultado:** `HealthController` delega en `HealthDatabaseProbe`, que ejecuta `SELECT 1` mediante un `JdbcTemplate` construido con el `DataSource` configurado. Valida el resultado y maneja `DataAccessException`, incluidos fallos de conexión y timeout.

Contrato real, coincidente entre código, requisitos, pruebas y `docs/HEALTH_ENDPOINT.md`:

| Condición | HTTP | Cuerpo JSON |
|---|---|---|
| Consulta devuelve 1 | 200 OK | `{"application":"UP","database":"UP"}` |
| Fallo de conexión/consulta o resultado inesperado | 503 Service Unavailable | `{"application":"UP","database":"DOWN"}` |

`application=UP` significa que el proceso pudo responder, incluso cuando la BD falla. No existe un campo `status` en B. Ambas respuestas usan `Cache-Control: no-store` y omiten detalles internos. La consulta tiene timeout de cinco segundos; este límite no cubre la adquisición de conexión del pool ni garantiza que la aplicación pueda arrancar con la BD caída.

**Pruebas y cobertura:** ocho casos aprobados: dos HTTP en `HealthControllerTest`, cinco JDBC en `HealthDatabaseProbeTest` y uno de integración en `HealthIntegrationTest`. Se comprueban éxito, fallo de conexión, timeout, resultado inesperado, liberación de recursos, recuperación de la misma sonda y acceso anónimo con SQL real sobre H2. JaCoCo reporta 14/14 líneas y 4/4 ramas del feature, es decir, 100%. La sonda no tiene ramas contabilizadas por JaCoCo; las cuatro corresponden al controlador. Es cobertura de esas dos clases, no de todo el proyecto. `package` no ejecutó el gate agregado de `verify`; los umbrales del feature se contrastaron con su reporte dedicado.

**Regresión y Ollama:** el baseline completo registró 238 pruebas y nueve errores preexistentes en pruebas que usan Ollama real. Se excluyeron 21 pruebas de seis clases dependientes de ese proveedor; las 217 comparables previas más las dos nuevas dieron 219/219 aprobadas. Las clases excluidas fueron `OllamaAdapterToolSelectionTest`, `OllamaRecommendationLiveTest`, `ReviewSummaryLiveTest`, `SearchCatalogSemanticTest`, `OllamaAdapterEmbeddingTest` y `DuplicateEmbeddingLiveTest`. No se atribuyen esos errores a `/api/health`. La suite completa posterior permanece sin verificar: NFR5 conserva `Unverified`/`Deferred`; la limitación fue aceptada y luego se aprobó Build and Test.

**Evidencia de ejecución:** `code-summary.md` registra Maven 3.9.16, Java 21, comandos, ocho pruebas en 48.580 s y regresión comparable en 1 min 15 s. `test-results.md` registra otra ejecución de compilación, ocho pruebas y empaquetado JAR en 45.982 s. Son duraciones de comandos distintos, no el tiempo total de desarrollo. Los logs e informes bajo `target/` se referencian en los artefactos, pero no están versionados en la rama examinada.

**Tiempo:** el responsable declara que comenzó el 19/10/2026 y terminó el 21/10/2026 a las 17:00, hora de Bolivia (UTC−04:00). El trabajo abarcó tres fechas calendario, con pausas y reanudaciones. Este periodo fue confirmado por el responsable al completar la práctica; no se obtiene del intervalo del workflow ni representa trabajo continuo. Como no se registró la hora de inicio ni se separaron las pausas, no se calcula una duración exacta en horas o un tiempo efectivo comparable directamente con las 1 h 5 min 36 s declaradas en A.

**Intervenciones:** se identifican cinco decisiones humanas explícitas y clasificables en la auditoría versionada: cuatro aprobaciones de etapa y una aceptación de la limitación de Ollama (`Accept failure`). Se registran cero solicitudes formales `Request Changes` y `Revision Count: 0`. Además, existen nueve eventos `HUMAN_TURN`, que incluyen participación y continuidad del trabajo y no se suman a las cinco decisiones como si fueran eventos independientes de corrección. La auditoría también registra un error técnico al publicar el análisis de código, resuelto al ajustar el alcance de archivos comprobados; no hay evidencia para atribuirlo a una corrección humana. Por tanto, el resultado defendible es cinco decisiones explícitas, cero solicitudes formales de cambios registradas y un total de correcciones humanas no determinado por esos registros. Esta clasificación evita comparar las cuatro correcciones de A con un conteo distinto presentado como equivalente.

Archivos funcionales de B: `HealthController.java`, `HealthDatabaseProbe.java`, `HealthControllerTest.java`, `HealthDatabaseProbeTest.java`, `HealthIntegrationTest.java` y `docs/HEALTH_ENDPOINT.md`. Los Java se ubican bajo los paquetes `infrastructure/controllers` e `infrastructure/health` del backend. No se modificaron `GlobalExceptionHandler.java` ni `pom.xml` respecto al base; no se añadieron dependencias.

Artefactos adicionales verificables:

- `.aidlc-version`, configuración/skills de AI-DLC en `.codex/` y `.agents/`, memoria bajo `aidlc/spaces/default/memory/` e integración en `AGENTS.md` y `.gitignore`. Son instalación y configuración del proceso, no código del endpoint.
- Nueve documentos de conocimiento de código en `aidlc/spaces/default/codekb/Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales/`: negocio, arquitectura, estructura, API, componentes, tecnologías, dependencias, calidad y alcance del análisis.
- En `aidlc/spaces/default/intents/261021-backend-health/`: `aidlc-state.md`, `project-description.json`, auditoría, escaneo del desarrollador, `requirements.md`, preguntas de requisitos, `code-generation-plan.md`, instrucciones de pruebas, `code-summary.md`, `traceability.json`, `test-results.md`, resumen e instrucciones de Build and Test, trazabilidad final y dos verificaciones entre fases.
- `aidlc/spaces/default/intents/intents.json` y `docs/HEALTH_ENDPOINT.md`. El volumen total de archivos de B incluye herramientas instaladas; no mide por sí mismo mayor calidad del código.

**Límite de atribución:** `0578a22` ya contiene el controlador, la sonda, seis pruebas y documentación antes del inicio del workflow registrado. Entre ese commit y `3f960a7`, el cambio funcional se concentra en dos pruebas adicionales y la actualización documental. El plan confirma que se verificó una implementación existente. Por ello, el resultado de toda la rama B no puede atribuirse exclusivamente a generación dentro de AI-DLC.

**Deployment:** no se realizó. La auditoría registra omisión condicional de Deployment Pipeline, Deployment Execution y Observability Setup por no requerirse cambios de CD y no existir destino configurado. El flujo completado no significa despliegue ni monitorización de producción configurados.

## 5. Comparación

| Aspecto | A · sin herramienta | B · con AI-DLC de AWS |
|---|---|---|
| Tiempo total | 1 h 5 min 36 s, registrado por el equipo. |  3 h 15 min 30 s, registrado por el equipo. |
| Intervenciones/correcciones | Cuatro correcciones documentadas; no total de mensajes. | Cinco decisiones explícitas: cuatro aprobaciones y una aceptación. Cero Request Changes registrados; nueve HUMAN_TURN no equivalen a nueve correcciones. Total de correcciones humanas no determinado. |
| Alcance funcional | Estado básico del servicio y rechazo POST. | Estado del backend y conectividad SQL, errores, timeout y recuperación. |
| Consulta de BD | Ninguna. | SELECT 1 por solicitud, con DataSource/JdbcTemplate. |
| Respuesta HTTP | GET 200; POST 405 probado con manejador global. | GET 200 o 503 según BD; no hay prueba propia de POST equivalente en los ocho casos examinados. |
| Pruebas | Dos propias de salud; seis ejecutadas al sumar cuatro preexistentes. Sin suite completa. | Ocho de salud; 219 comparables de regresión verdes. Suite global con limitación externa aceptada. |
| Cobertura | Sin porcentaje verificable en evidencias versionadas. | 100% del feature documentado: 14/14 líneas y 4/4 ramas; no cobertura global. |
| Artefactos | Tres archivos de código/pruebas y cuatro evidencias; sin dependencias nuevas. | Cinco archivos de código/pruebas, documentación de endpoint y numerosos artefactos de proceso; sin dependencias nuevas. |
| Trazabilidad | Commits, README, log y capturas; relación principalmente narrativa. | FR/NFR, plan, código, pruebas, matriz de objetivos, estado y auditoría; NFR5 pendiente explícito. |
| Qué salió mejor | Menor superficie para un health básico; evidencia de corrección de HTTP 405. | Mayor validación de BD, manejo de fallos, prueba integrada y justificación documental. |
| Qué salió peor | No cubre la conectividad de BD; cambio transversal en el manejador y cobertura porcentual no demostrada. | Más pasos/configuración/documentación; tiempo efectivo no medido, limitación global pendiente y atribución parcial al workflow. |

### Diferencias de alcance observadas

Las ramas comparten el commit base y el nombre `/api/health`, pero A no consulta BD y B sí. A modifica `GlobalExceptionHandler` globalmente; el comportamiento de métodos no permitidos puede afectar otras rutas, aunque no se demostró una regresión. B utiliza una sonda separada y más pruebas, sin trasladar la corrección global de A. Su aislamiento es una ventaja de alcance, pero no demuestra que B cubra el caso POST de la misma manera.

B tiene una implementación más amplia y más artefactos. La comparación mide tanto diferencias de proceso como diferencias de alcance. Tampoco es un experimento controlado: el trabajo se realizó con responsables, entornos y evidencias distintas, y el workflow de B comenzó con código existente. No se puede concluir causalmente que AI-DLC sea más rápido, reduzca errores o produzca siempre más calidad a partir de esta única práctica.

Para reproducir la comparación se utilizaron `git log --oneline <base>..<rama>`, `git diff <base> <rama> -- backend docs` y `git diff feature-sin-aidlc feature-con-aidlc -- backend docs`. Los commits de A documentan implementación, corrección y tiempo; los de B distinguen preparación de versión, implementación previa y cierre con artefactos del flujo.

## 6. Conclusiones

1. **¿Qué decisiones tomó el agente sin que ustedes las vieran?** No puede reconstruirse objetivamente qué decisiones observó cada integrante en tiempo real. El código permite identificar decisiones que debieron revisarse: respuesta estática y modificación global del 405 en A; sonda JDBC separada, application=UP aunque falle la BD, no-store y timeout de cinco segundos en B. El escaneo inicial de AI-DLC incluso clasificó el proyecto como npm con framework desconocido, mientras el análisis posterior identificó el backend Java/Spring Boot. La automatización requiere contrastar sus resultados con el código.
2. **¿Cuántas veces aprobaron una puerta por inercia?** No se identifican aprobaciones por inercia demostrables en los registros; esto no permite afirmar que su número real haya sido cero. La autoevaluación del proceso es favorable por resultados observables: se mantuvo el alcance del feature, se completaron pruebas de recuperación y se aceptó expresamente la limitación de Ollama sin presentarla como una suite global aprobada. Las cuatro aprobaciones y esa aceptación muestran decisiones humanas registradas y responsabilidad sobre el cierre. Como mejora, cada aprobación debería acompañarse de una breve justificación que indique qué evidencia se revisó; así se podría evaluar la calidad de la revisión sin confundir un simple «Approve» con una lectura completa. El principal aprendizaje fue conservar criterio propio ante la automatización y reconocer los límites de la evidencia.
3. **¿Qué evidencia permite entender mejor por qué se hizo así?** En A, el README, el log de seis pruebas y los commits permiten explicar la corrección del 405. En B, requirements.md, el plan, traceability.json, test-results.md y la auditoría relacionan objetivos, decisiones, pruebas, excepciones aceptadas y cierre. Esta trazabilidad es más detallada, pero no elimina brechas: NFR5 permanece sin verificar y los logs/reporte originales de B no están versionados.
4. **¿Para qué tamaño de cambio vale la pena AI-DLC?** Para un endpoint estático pequeño, la instalación, múltiples pasos y aprobaciones pueden superar el beneficio. Para cambios medianos, críticos o auditables —por ejemplo pagos, inventario y eventos del proyecto— la explicitación de requisitos, validación de riesgos y evidencia de aceptación puede justificar el costo. El perfil debe ajustarse al riesgo; usar Express no garantiza por sí solo un proceso breve.
5. **¿Lo usarían en el proyecto? ¿Por qué?** Sí, de forma selectiva para cambios con dependencias, reglas de negocio o exigencias de auditoría. Para tareas locales pequeñas conviene un procedimiento más corto con alcance claro y pruebas suficientes. La práctica muestra mayor trazabilidad y documentación en B, además de más pruebas para su alcance ampliado, pero no permite atribuir todo ese resultado exclusivamente a AI-DLC.

Como costo práctico adicional, el equipo reporta un agotamiento temporal de la cuota de Codex durante Code Generation. Se consigna como observación aportada por el equipo: la auditoría versionada examinada no contiene un evento explícito que permita verificar esa ubicación en el flujo, cuantificar la interrupción o calcular su costo. No se inventan duración ni consumo monetario.

La conclusión es que AI-DLC aportó requisitos, planificación, pruebas, documentación y auditoría más explícitos, con mayor carga de pasos y configuración y dependencia de la capacidad disponible del agente. Para una comparación futura más controlada deberían fijarse el mismo contrato JSON, consulta de BD, escenarios de error, entorno, estrategia de pruebas y método de medición temporal antes de desarrollar ambas versiones.

## 7. Antes de entregar

- [x] Dos ramas disponibles
- [x] Pruebas del feature pasan en ambas
- [x] aidlc/ y .aidlc-version versionados en B
- [ ] Sin secretos
- [x] Responsables completos

Los puntos de pruebas reflejan evidencias registradas, no una reejecución durante esta redacción ni una suite global verde. «Sin secretos» queda pendiente de la revisión final del equipo: no se realizó una auditoría integral de secretos para este documento. El periodo de B se incorporó a partir de la declaración del responsable; las intervenciones se clasificaron con la auditoría y la autoevaluación se fundamentó en resultados observables, sin inventar horas efectivas ni motivaciones personales. Incorporar video solo si corresponde. Conservar las diferencias de alcance y la limitación aceptada de Ollama al presentar los resultados.
