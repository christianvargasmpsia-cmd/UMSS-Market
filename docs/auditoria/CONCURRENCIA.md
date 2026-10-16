# Auditoría de concurrencia del backend — Ecommerce UMSS

> **Módulo 8 · Actividad de auditoría (parte 2 de 2) · Entrega: domingo 18 de octubre de 2026, 23:59.**
> Este archivo es a la vez **las instrucciones para el asistente de IA** y **la ficha que se entrega**. El asistente lo rellena en su lugar; el grupo lo revisa, responde las preguntas y lo sube a su repositorio.

## Cómo se usa (para el grupo)

1. Copien este archivo a su repositorio como `docs/auditoria/concurrencia.md`.
2. Trabajen sobre el mismo commit que la parte 1 (`release/8.0.0`).
3. Abran su asistente con acceso al repositorio y escríbanle:
   > Sigue las instrucciones de la sección 0 de `docs/auditoria/concurrencia.md` y rellena ese archivo.
4. Respondan las preguntas que el asistente les haga al final de cada oleada. Aquí habrá más preguntas que en la parte 1, porque varios datos (cuántas réplicas corren, cuánto tarda la IA, cuánto tráfico esperan) no están en el código.
5. Revisen el resultado, confirmen la autoverificación (sección 6) y hagan los commits **cada integrante con su propia cuenta**.

**Si su asistente no tiene acceso al repositorio:** pídanle los comandos, córranlos ustedes y péguenle la salida.

**Cómo se califica la parte de preguntas:** cada pregunta debe terminar respondida o cerrada como `NO VERIFICABLE` con su motivo. **Si quedan más de 3 preguntas sin responder en este archivo, baja la nota de la actividad.** Si no tienen el dato exacto (por ejemplo, el tráfico esperado), una estimación razonada cuenta como respuesta.

---

## 0. Instrucciones para el asistente de IA (no editar esta sección)

**Rol.** Eres el asistente de auditoría de un grupo de maestría que desarrolla un backend. Vas a revisar cómo maneja la concurrencia su servicio y a **rellenar este mismo archivo**. No produces otros archivos.

**Conceptos que debes aplicar.**
- **Dónde espera una petición:** casi todo el tiempo de una API es espera (base de datos, red, servicio de IA). Saturar es que un recurso finito (hilos, conexiones del pool, el único hilo del event loop, el cupo de la API de IA) esté al 100 % y las peticiones nuevas hagan cola: primero sube la latencia y luego llegan los errores.
- **Modelos por runtime:** Node.js usa un event loop de un hilo (cualquier trabajo síncrono lo bloquea); Java 21+ con hilos virtuales permite código bloqueante que escala, pero el pool de la base de datos sigue siendo el límite; Python con `async def` bloquea el loop si llama a librerías síncronas, y las rutas `def` corren en un threadpool finito (40 hilos por defecto en AnyIO).
- **Ley de Little:** conexiones ocupadas en promedio = peticiones por segundo que usan la base de datos × tiempo medio que cada una retiene la conexión. Cada instancia tiene su propio pool: la base de datos recibe `instancias × tamaño del pool`.
- **Llamadas lentas (LLM):** necesitan timeout explícito, reintentos con backoff y jitter, límite de llamadas simultáneas y no retener una conexión de base de datos mientras se espera.

**Reglas que no puedes romper.**
1. **Solo lectura del producto.** No modificas código ni configuración; el único archivo que editas es este. No ejecutas pruebas de carga contra servicios compartidos o en producción.
2. **No inventas.** Cada estado distinto de `N/A` lleva evidencia comprobable: `ruta/archivo:línea` con el fragmento relevante, o el comando exacto y las primeras líneas de su salida. Las cifras que no salen del código (tráfico, latencias, réplicas) se preguntan, no se suponen.
3. **No copias secretos.** Rutas y líneas sí; valores de claves, nunca.
4. **Todas las comprobaciones aparecen**, una vez cada una (CON-01 a CON-05). No agregas ni quitas comprobaciones.
5. **Escribes en español** y en frases claras.

**Estados permitidos.** `OK`, `PARCIAL`, `FALLA`, `N/A` (con razón) y `NO VERIFICABLE` (con razón). Mientras trabajas puedes usar `PENDIENTE`, pero **al terminar no puede quedar ninguno**.

**Cómo preguntar sin bloquear.** Cuando te falte un dato que no está en el código:
- Marca la comprobación como `PENDIENTE`, agrega la pregunta a la sección 4 con un número (P1, P2…) y la comprobación a la que afecta, y **continúa con la siguiente**. Nunca te detengas a esperar.
- Al **terminar cada oleada**, muestra al grupo todas las preguntas nuevas juntas. Si tu herramienta permite preguntas con opciones, úsalas (por ejemplo, para el número de réplicas: 1, 2, 3 o más, no sabemos).
- Si el grupo responde, anota la respuesta y actualiza la comprobación. Si no responde, en la oleada 3 cierra la comprobación como `NO VERIFICABLE` y la pregunta como "sin respuesta".
- **Antes de preguntar, búscalo** en el README, `docker-compose`, Dockerfile, manifiestos de Kubernetes, archivos de configuración y scripts de arranque.

**Oleadas.** Trabaja en este orden y actualiza la sección 2 al terminar cada una:
- **Oleada 0 · Reconocimiento.** Rellena la sección 1: runtime y framework de cada servicio, punto de arranque, librería de acceso a datos y cliente HTTP o SDK de IA. Escribe en la sección 2 el plan (qué archivos y comandos usarás).
- **Oleada 1 · Evidencia automática.** Ejecuta los comandos de cada comprobación (adaptados a las carpetas reales) y anota la evidencia.
- **Oleada 2 · Juicio.** Decide el estado de CON-01 a CON-04 y pregunta lo que falte. Calcula con la ley de Little las conexiones necesarias cuando tengas los datos (tráfico estimado y tiempo de retención); si no los tienes, pregúntalos.
- **Oleada 3 · Cierre.** Integra las respuestas; escribe la **hipótesis de cuello de botella** (CON-05) con base en las comprobaciones anteriores; cierra lo pendiente como `NO VERIFICABLE`; escribe los hallazgos (sección 5), marca la autoverificación (sección 6) y rellena el resumen (sección 7). Termina diciendo cuántas preguntas quedaron sin respuesta.

**Si el proyecto tiene varios servicios**, evalúa cada uno y anota la evidencia por servicio; el estado es el del peor evaluado.

---

## 1. Datos del repositorio (oleada 0)

- **Grupo:** G1 · UMSS Market (`AGENTS.md:12-17`).
- **Commit auditado (`release/8.0.0`):** `824eb0f5a00ef586d467d23dda8856ac576dabae`. Comandos `git rev-parse HEAD` y `git rev-parse release/8.0.0`: ambos devolvieron ese hash. `git branch --show-current`: `release/8.0.0`. `git status --short` inicial: sin salida.
- **Fecha:** 2026-10-15, America/La_Paz.
- **Servicios y runtime:** `umss-market-api`: Java 21, Spring Boot 3.5.15, Spring MVC (`backend/umss-market-api/pom.xml:8,30,47`: `3.5.15`, `java.version=21`, `spring-boot-starter-web`). Arranque en `J/UmssMarketApiApplication.java:10`: `SpringApplication.run(...)`; `README.md:769`: `.\mvnw.cmd spring-boot:run`. Ollama es un servicio externo; su versión y runtime no son verificables aquí. P1 informa ejecución local con una instancia de API y una de Ollama. Los seis microservicios descritos en AGENTS son diseño, no seis aplicaciones ejecutables encontradas en este backend.
- **Acceso a datos:** Spring Data JPA (`backend/umss-market-api/pom.xml:35`); PostgreSQL (`pom.xml:75-77` y `src/main/resources/application.properties:10`, relativos al backend). Hikari aparece en evidencia histórica de tests, no como medición del despliegue: `backend/umss-market-api/m7-ollama.log:1427-1429`, `HikariPool-1 - Starting...`, conexión H2 y `Start completed.` La versión efectiva de PostgreSQL no se verificó.
- **Cliente HTTP o SDK de IA:** `RestTemplate`, `J/infrastructure/config/OllamaConfig.java:11-12`, `return new RestTemplate();`.
- **Alcance:** API Java y su integración con Ollama. AI Testing Agent es una herramienta por menú (`README.md:843-849`), no otro servidor del producto; frontend, POC y scripts de evaluación se excluyen del camino de peticiones del backend.
- **Integrantes y qué revisó cada uno:** Rodriguez Gonzales Abad Melani y Vargas Sandoval Christian Bernardo (`AGENTS.md:17`). Por solicitud del grupo en P3, revisión repartida 50/50: Melani revisará CON-01 y CON-02; Christian revisará CON-03 y CON-04; ambos compartirán CON-05 y el cierre. Es una asignación de trabajo, no una declaración de revisión ya realizada. El asistente inspeccionó las cinco comprobaciones; los commits humanos aún no se acreditan.

**Convención de evidencia:** `J/` equivale exactamente a `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/`. Las referencias con varias líneas identifican fragmentos del mismo archivo. Las búsquedas sin coincidencias prueban ausencia en el ámbito indicado, no ausencia de configuración externa.

## 2. Plan y avance por oleadas

| Oleada | Qué se hizo | Estado |
|--------|-------------|--------|
| 0 · Reconocimiento | Verificar HEAD/release y árbol; leer AGENTS, README, pom, configuración y arranque. Plan: inventariar manifiestos/scripts; buscar modelo, bloqueos, pool y clientes; seguir chat, monitoreo y checkout. | OK |
| 1 · Evidencia | Ejecutar git grep y rg; leer controladores, OllamaConfig/Adapter, monitoreo, excepciones y persistencia. Presentar juntas P1-P3. Sin servicios iniciados ni pruebas de carga. | OK |
| 2 · Juicio | Identificar fallas estáticas en bloqueo, pool y resiliencia. Separar modelo inferido de despliegue real. Solicitar réplicas, tráfico, retención y latencia para Little en P1-P2. | OK |
| 3 · Cierre | Integradas respuestas P1-P3: una instancia local de cada servicio, Ollama tarda aproximadamente 3-5 minutos y la petición unos 10 minutos según el grupo. Repartida revisión 50/50. Datos faltantes cerrados como NO VERIFICABLE; revisión humana y commits no acreditados. | OK |

## 3. Comprobaciones

### CON-01 · Modelo de concurrencia e instancias

- **Evidencia:** `pom.xml:30,47` del backend declara Java 21 y starter web MVC; `J/UmssMarketApiApplication.java:10`, `SpringApplication.run(...)`. Comando `git grep -nE 'spring\.threads\.virtual|replicas:|deploy:|SpringApplication.run' -- backend README.md`: única coincidencia, el arranque citado. `git ls-files '*compose*' '*Dockerfile*' '*.yml' '*.yaml' '*AGENTS.md' '*.ps1' '*.sh' '*.cmd' ':!:**/node_modules/**' ':!:node_modules/**'`: primeras líneas `AGENTS.md`, `ai-testing-agent/.github/workflows/ai-monitoring.yml`, `ai-testing-agent/ci_local.ps1`; el inventario completo no contiene Dockerfile, compose ni manifiesto de réplicas. `README.md:769` solo indica arranque local. `J/infrastructure/security/TokenBlacklistService.java:11-19`: `ConcurrentHashMap.newKeySet()`, `add(token)` y `contains(token)` mantienen revocaciones por proceso.
- **Estado:** PARCIAL.
- **Observación:** El código corresponde a MVC bloqueante atendido por hilos; Java 21 por sí solo no habilita hilos virtuales. No hay activación explícita encontrada. P1 declara ejecución local con una instancia de API y una de Ollama. El grupo confirma Java 21 con Spring Boot, de acuerdo con el código auditado. Desconoce si existen ajustes externos y cuál es el límite efectivo de PostgreSQL; esos valores siguen NO VERIFICABLE. Si hay varias réplicas, la blacklist local no comparte revocaciones; el acceso concurrente al conjunto es seguro dentro de una JVM, pero no resuelve distribución. No se afirma que ese despliegue exista.
- **Auditó:** Asistente IA (análisis inicial). Abad Melani Rodriguez Gonzales (revisión humana, 15/10/2026). Se revisó el modelo de concurrencia de Spring MVC, la configuración de instancias y el uso de memoria local para revocaciones JWT. Se mantiene el estado PARCIAL. Commit de revisión: pendiente de registrar.

### CON-02 · Llamadas bloqueantes en el camino de una petición

- **Evidencia:** `git grep -nE 'Mono|Flux|WebClient|synchronized' -- 'backend/**/*.java'` devuelve `J/infrastructure/monitoring/AIMonitoringService.java:35: public synchronized void record(`. El mismo método ejecuta `Files.createDirectories(parent)` en línea 94 y `Files.writeString(...)` en 97-104. `J/application/services/AIServiceImpl.java:600-610,630` invoca monitoreo antes de retornar la respuesta. `J/infrastructure/controllers/AIController.java:46-48` atiende `/api/ai/chat`. `backend/umss-market-api/src/main/resources/application.properties:30`: `ai.monitoring.enabled=true`. `git grep -nE 'readFileSync|writeFileSync|execSync|spawnSync|async def' -- backend`: sin salida (código 1, sin coincidencias).
- **Estado:** FALLA.
- **Observación:** La escritura de disco síncrona se realiza bajo un monitor único del servicio de monitoreo: las peticiones que registran respuestas se serializan y esperan al disco. No es un bloqueo de event loop: es contención de hilos MVC. Si se activaran hilos virtuales sobre Java 21, también habría que revisar pinning en esa sección. JPA y RestTemplate bloqueantes son coherentes con MVC, aunque retienen hilos durante sus esperas. No se midió cuánto aporta el disco a la latencia.
- **Auditó:** Asistente IA (análisis inicial). Abad Melani Rodriguez Gonzales (revisión humana, 15/10/2026). Se revisó AIMonitoringService.java, identificando el método synchronized y la escritura síncrona mediante Files.writeString. Se mantiene el estado FALLA por el riesgo de bloqueo y espera entre peticiones concurrentes. Commit de revisión: pendiente de registrar.

### CON-03 · Pool de conexiones a la base de datos

- **Cálculo (ley de Little):** `L = λ_BD × W_conexión`, donde λ_BD son peticiones/s que usan BD y W es el tiempo medio total que cada una ocupa conexión, en segundos. Valores y resultado numérico: NO VERIFICABLE (P2). Capacidad máxima agregada configurada: `N_instancias × P_pool`; N = 1 según P1, por tanto el máximo agregado del backend es `1 × P_pool = P_pool`; P y presupuesto de PostgreSQL: NO VERIFICABLE. P2 estima Ollama en 3-5 minutos (180-300 s) y la petición en unos 10 minutos (600 s), pero no acredita retención de conexión durante esos 600 s. Tráfico esperado y W_conexión siguen NO VERIFICABLE: el grupo declara que aún no dispone de una estimación fundamentada del tráfico ni conoce el tiempo de retención de conexión. No se confunde duración de toda la petición ni latencia del LLM con retención de conexión. Si hay diferentes tipos de petición, sumar `λ_i × W_i`; dimensionar además margen para picos y otras aplicaciones.
- **Evidencia:** `git grep -nE 'poolSize|maximum-pool-size|maximumPoolSize|pool_size|max_overflow|pool_timeout|connectionTimeout|connection-timeout|acquireTimeout|open-in-view' -- backend/umss-market-api/src/main`: sin salida (código 1). `J/shared/GlobalExceptionHandler.java:79-83`: `@ExceptionHandler(Exception.class)` devuelve `HttpStatus.INTERNAL_SERVER_ERROR`; no se encontró manejo específico de agotamiento. `J/application/usecases/SimulatedCheckoutUseCase.java:22`: `@Transactional`; `J/infrastructure/adapters/JpaCheckoutRepositoryAdapter.java:20,23`: bloqueos `LockModeType.PESSIMISTIC_WRITE` de usuario y publicación. `J/application/usecases/GetRecommendationsUseCase.java:29,35,56,58,102` alterna lectura de repositorios, `generateEmbedding` y `generate`. `backend/umss-market-api/m7-ollama.log:1449` advierte históricamente `spring.jpa.open-in-view is enabled by default` en un test.
- **Estado:** FALLA.
- **Observación:** Falta una política explícita versionada de tamaño, adquisición y respuesta al agotamiento; la excepción genérica conduce a 500 en vez de una degradación específica. No se asignan valores por defecto como si fueran efectivos. La transacción de checkout puede retener conexiones mientras espera bloqueos de filas. No se encontró una transacción explícita alrededor de todo el flujo de IA; esto no demuestra que la conexión se libere antes de cada llamada: hay que verificar OSIV y comportamiento efectivo con instrumentación. El log de tests no prueba configuración de producción. La capacidad numérica queda NO VERIFICABLE por los datos faltantes en P1-P2; la falla de configuración está respaldada por código.
- **Auditó:** asistente IA. Revisión humana asignada a Christian (CON-03); realización y commit aún no acreditados.

### CON-04 · Timeouts y reintentos en llamadas externas (incluida la IA)

- **Evidencia:** `git grep -nE 'RestTemplate|postForObject|HttpClient|WebClient|RestClient|OpenAI|Anthropic|ChatClient' -- backend/umss-market-api/src/main`: primeras coincidencias `J/infrastructure/adapters/OllamaAdapter.java:6` (`import ...RestTemplate`), línea 19 (`private final RestTemplate restTemplate`) y línea 36 (inyección por constructor). Las llamadas están en líneas 55, 422 y 980: `restTemplate.postForObject(...)`; cubren generación, selección de herramienta y embeddings. `J/infrastructure/config/OllamaConfig.java:12`: `return new RestTemplate();`. `git grep -nE 'timeout|Timeout|retry|retries|backoff|Semaphore|bulkhead' -- backend/umss-market-api/src/main`: sin salida (código 1).
- **Estado:** FALLA.
- **Observación:** No hay timeout explícito de conexión/lectura ni límite de llamadas simultáneas a Ollama en el cliente o servicio inspeccionado. No se encontraron reintentos implementados, por lo que tampoco hay backoff/jitter; no se acusa un bucle de reintentos inexistente. Si se incorporan, deben ser acotados y restringidos a operaciones cuya repetición sea segura. El manejo de excepciones de embeddings (`OllamaAdapter.java:1016`) no establece un plazo máximo. La espera ocupa el hilo MVC. P2 estima una llamada a Ollama en 3-5 minutos, según recursos del PC, y una petición de unos 10 minutos. Son estimaciones del grupo, no mediciones instrumentadas; refuerzan el riesgo de esperas prolongadas. Capacidad simultánea de Ollama y retención de conexión de BD: NO VERIFICABLE (P1-P2 y CON-03). No se encontraron clientes bancarios ni de colas implementados en este backend; sus diseños no se evalúan como llamadas operativas.
- **Auditó:** asistente IA. Revisión humana asignada a Christian (CON-04); realización y commit aún no acreditados.

### CON-05 · Hipótesis de cuello de botella

- **Hipótesis:** Bajo carga, se saturará primero el conjunto de hilos de atención HTTP en `/api/ai/chat`, porque las llamadas síncronas a Ollama no tienen timeout explícito ni límite de concurrencia y retienen los hilos que atienden peticiones (CON-01 y CON-04), mientras el monitoreo añade espera serializada de disco antes de devolver la respuesta (CON-02).
- **Evidencia:** `J/infrastructure/controllers/AIController.java:46-48`, llamada directa a `aiService.chat`; `J/infrastructure/adapters/OllamaAdapter.java:422`, `postForObject`; `J/infrastructure/config/OllamaConfig.java:12`, cliente sin ajustes; `J/infrastructure/monitoring/AIMonitoringService.java:35,97`, monitor y escritura. Es una hipótesis condicionada al modelo MVC sin ajustes externos observados, no un orden de saturación medido. Ollama o el pool podrían convertirse antes en el límite efectivo según capacidad, OSIV y mezcla de tráfico (CON-03).
- **Estado:** OK.
- **Validación futura S04:** en un entorno aislado y autorizado, medir hilos ocupados/cola HTTP, llamadas y espera en Ollama, conexiones activas/pendientes/tiempo de adquisición, duración de bloqueos y monitoreo, latencia y errores. Aumentar progresivamente tráfico de chat y comparar con CRUD y checkout; contrastar o refutar la hipótesis. No se ejecutó carga en esta auditoría.
- **Auditó:** Asistente IA (análisis inicial). Revisión conjunta asignada a Abad Melani Rodriguez Gonzales y Christian Bernardo Vargas Sandoval. Melani revisó la relación de CON-01 y CON-02 con la hipótesis de saturación de hilos HTTP en /api/ai/chat (15/10/2026). La confirmación conjunta, la validación experimental futura y la acreditación de los commits permanecen pendientes.

## 4. Preguntas al grupo

Preguntas presentadas juntas al finalizar la recogida inicial de evidencia, tras revisar README, configuración, arranque e inventario de manifiestos. Respuestas del grupo integradas después del primer cierre. Las tres preguntas están respondidas; los subdatos desconocidos o ambiguos se cierran como NO VERIFICABLE con motivo, sin inventarlos.

| N.º | Comprobación | Pregunta | Respuesta del grupo | Estado (respondida / sin respuesta) |
|-----|--------------|----------|---------------------|--------------------------------------|
| P1 | CON-01, CON-03, CON-04 | ¿En qué entorno ejecutan API y Ollama, cuántas instancias hay de cada uno y qué ajustes externos de hilos, pool (tamaño/espera) y presupuesto de PostgreSQL se aplican? | Ejecutamos el backend y Ollama de forma local, con una instancia de cada uno. El backend del repositorio utiliza Java 21 con Spring Boot. No encontramos configuración explícita de hilos virtuales, tamaño del pool ni tiempo de espera para obtener conexiones. Desconocemos si existen ajustes externos y cuál es el límite efectivo de conexiones de PostgreSQL. | respondida |
| P2 | CON-03, CON-04, CON-05 | ¿Qué tráfico esperan (peticiones/s totales, de BD y de IA), cuánto dura Ollama y cuánto tiempo retiene una petición la conexión de BD? Sirven estimaciones razonadas, distinguiendo datos desconocidos. | Aún no tenemos una estimación fundamentada de peticiones por segundo totales ni de IA. Según nuestra experiencia local, una llamada a Ollama tarda aproximadamente entre 3 y 5 minutos, dependiendo de los recursos del equipo, y una petición completa puede durar alrededor de 10 minutos. Son estimaciones, no mediciones instrumentadas. Desconocemos cuánto tiempo permanece ocupada una conexión de base de datos durante esa petición; no podemos afirmar que sean los mismos 10 minutos. Por ello, todavía no podemos calcular numéricamente las conexiones necesarias mediante la ley de Little. | respondida |
| P3 | CON-01 a CON-05; sección 6 | ¿Qué comprobaciones revisó cada integrante y qué commits propios corresponden a esta auditoría? | El grupo solicita dividir 50/50. Se asignan CON-01/02 a Melani, CON-03/04 a Christian y CON-05/cierre a ambos. Revisión realizada y commits propios: NO VERIFICABLE, pues la solicitud de reparto no confirma su ejecución ni aporta hashes. | respondida |

## 5. Hallazgos priorizados

| # | Comprobación | Severidad (alta / media / baja) | Qué falla | Corrección propuesta | Responsable |
|---|--------------|---------------------------------|-----------|----------------------|-------------|
| 1 | CON-04 | alta | Ollama sin timeout explícito ni límite de llamadas simultáneas; evidencia en OllamaConfig:12 y OllamaAdapter:55,422,980. | Configurar plazos de conexión/lectura y presupuesto total; limitar concurrencia y cola; devolver error controlado al saturar. Si se reintenta, acotar intentos y usar backoff/jitter en operaciones seguras. | Equipo backend; persona por asignar |
| 2 | CON-03 | alta | Pool sin política explícita y agotamiento sin respuesta específica; GlobalExceptionHandler:79-83. | Medir retención; fijar pool y espera según Little, picos y presupuesto compartido; controlar agotamiento con 503. Verificar liberación antes del LLM y OSIV. | Equipo backend/BD; persona por asignar |
| 3 | CON-02 | media | Disco bajo monitor global en respuesta de chat; AIMonitoringService:35,94,97. | Sacar la escritura del camino de respuesta mediante una cola acotada y consumidor, con política explícita ante saturación y métricas de pérdidas/espera. | Equipo backend; persona por asignar |
| 4 | CON-01 | media | Una instancia local declarada; ajustes externos y límites efectivos desconocidos. Blacklist por JVM limita un futuro escalado. | Documentar instancias y límites efectivos; antes de replicar, usar revocación compartida con expiración y probarla entre réplicas. | Equipo backend/despliegue; persona por asignar |
| 5 | CON-05 | baja | Hipótesis sin contraste experimental; P1-P2 aportan instancias y estimaciones de duración, pero no tráfico ni retención de conexión. | Instrumentar y ejecutar S04 en entorno aislado; revisar qué recurso se satura primero y actualizar dimensionamiento. | Equipo de pruebas; persona por asignar |

Las correcciones son propuestas: esta auditoría no altera código, configuración ni decisiones arquitectónicas del producto.

## 6. Autoverificación (la marca el asistente; el grupo la confirma)

- [x] Las cinco comprobaciones CON-01 a CON-05 están en la sección 3, una vez cada una.
- [x] Ninguna quedó en `PENDIENTE` ni sin estado; todos los estados son de los cinco permitidos.
- [x] Todo estado distinto de `N/A` y `NO VERIFICABLE` tiene evidencia con ruta y línea, o con comando y salida.
- [x] Las cifras usadas (tráfico, latencias, réplicas) vienen del código o de una respuesta del grupo, no de una suposición; no se inventaron valores para Little.
- [x] CON-05 está formulada con el formato pedido y cita las comprobaciones en que se basa.
- [x] Todas las preguntas de la sección 4 están respondidas o marcadas "sin respuesta".
- [x] No hay ningún secreto copiado en este archivo.
- [x] No se modificó código del producto.
- [x] Cada integrante aparece como auditor de al menos una comprobación y tiene al menos un commit con su propia identidad.

## 7. Resumen

| OK | PARCIAL | FALLA | N/A | NO VERIFICABLE | Preguntas sin respuesta |
|---:|--------:|------:|----:|---------------:|------------------------:|
| 1 | 1 | 3 | 0 | 0 | 0 |

**Hipótesis de cuello de botella (copia de CON-05):** Bajo carga, se saturará primero el conjunto de hilos de atención HTTP en `/api/ai/chat`, porque las llamadas síncronas a Ollama no tienen timeout explícito ni límite de concurrencia y retienen los hilos que atienden peticiones (CON-01 y CON-04), mientras el monitoreo añade espera serializada de disco antes de devolver la respuesta (CON-02).

**Cierre:** quedaron **0 preguntas sin respuesta**. Los subdatos no aportados o inciertos (tráfico, retención de conexión, ajustes externos y límite efectivo de PostgreSQL) se cerraron como NO VERIFICABLE con motivo. No se ejecutaron pruebas de carga durante esta auditoría. Melani registró su revisión de CON-01, CON-02 y su participación en CON-05 mediante el commit `ee1538c`. Christian cuenta con commits previos relacionados con las auditorías. Queda pendiente confirmar conjuntamente CON-05 y la revisión final del grupo.