
# Auditoría 12-factor — G1 — UMSS Market

> **Módulo 8 · Actividad de auditoría (parte 1 de 2) · Entrega: domingo 18 de octubre de 2026, 23:59.**
> Este archivo es a la vez **las instrucciones para el asistente de IA** y **la ficha que se entrega**. El asistente lo rellena en su lugar; el grupo lo revisa, responde las preguntas y lo sube a su repositorio.

## Cómo se usa (para el grupo)

1. Copien este archivo a su repositorio como `docs/auditoria/12factor.md`.
2. Creen la etiqueta o rama `release/8.0.0` sobre su rama principal actual (es el estado de partida del módulo) y trabajen sobre ese commit.
3. Abran su asistente con acceso al repositorio (Claude Code, Codex, Cursor u otro) y escríbanle:
   > Sigue las instrucciones de la sección 0 de `docs/auditoria/12factor.md` y rellena ese archivo.
4. Respondan las preguntas que el asistente les haga al final de cada oleada. Pueden contestar en el chat o escribiendo la respuesta directamente en la sección 4.
5. Revisen el resultado, corran ustedes la autoverificación (sección 6) y hagan los commits **cada integrante con su propia cuenta**.

**Si su asistente no tiene acceso al repositorio** (un chat sin herramientas): pídanle que les indique qué comandos correr, córranlos ustedes y péguenle la salida. El resto del proceso es igual.

**Cómo se califica la parte de preguntas:** cada pregunta debe terminar respondida o cerrada como `NO VERIFICABLE` con su motivo. **Si quedan más de 3 preguntas sin responder en este archivo, baja la nota de la actividad.** No saber un dato no es un error; dejar sin respuesta lo que el grupo sí podría averiguar, sí.

---

## 0. Instrucciones para el asistente de IA (no editar esta sección)

**Rol.** Eres el asistente de auditoría de un grupo de maestría que desarrolla un backend. Vas a revisar su repositorio contra seis prácticas 12-factor y a **rellenar este mismo archivo**. No produces otros archivos.

**Reglas que no puedes romper.**
1. **Solo lectura del producto.** No modificas código, configuración ni dependencias del repositorio. El único archivo que editas es este.
2. **No inventas.** Cada estado distinto de `N/A` lleva evidencia comprobable: `ruta/archivo:línea` con el fragmento relevante, o el comando exacto que ejecutaste y las primeras líneas de su salida. Si no puedes comprobar algo, no lo supones: lo preguntas o lo marcas `NO VERIFICABLE` con su motivo.
3. **No copias secretos.** Si encuentras una contraseña, token o clave, reportas la ruta y la línea, **nunca el valor**. No abras ni muestres archivos `.env` ni de claves en el chat.
4. **Todas las comprobaciones aparecen**, una vez cada una (F12-01 a F12-06), aunque el resultado sea negativo. No agregas ni quitas comprobaciones.
5. **Escribes en español** y en frases claras.

**Estados permitidos.** `OK`, `PARCIAL`, `FALLA`, `N/A` (no aplica, con razón) y `NO VERIFICABLE` (no se pudo comprobar, con razón). Mientras trabajas puedes usar `PENDIENTE`, pero **al terminar no puede quedar ninguno**.

**Cómo preguntar sin bloquear.** Cuando te falte un dato que no está en el código (por ejemplo, cómo se despliega en producción):
- Marca la comprobación como `PENDIENTE`, agrega la pregunta a la sección 4 con un número (P1, P2…) y la comprobación a la que afecta, y **continúa con la siguiente**. Nunca te detengas a esperar.
- Al **terminar cada oleada**, muestra al grupo todas las preguntas nuevas juntas, cada una con su número. Si tu herramienta permite preguntas con opciones, úsalas; si no, escríbelas en el chat.
- Si el grupo responde, anota la respuesta en la sección 4 y actualiza la comprobación. Si no responde o no lo sabe, en la oleada 3 cierra la comprobación como `NO VERIFICABLE` y la pregunta como "sin respuesta".
- Pregunta solo lo necesario: **antes de preguntar, búscalo en el repositorio** (README, `docker-compose`, Dockerfile, manifiestos, configuración, scripts).

**Oleadas.** Trabaja en este orden y actualiza la sección 2 al terminar cada una:
- **Oleada 0 · Reconocimiento.** Rellena la sección 1 (commit, stack, carpetas de código, cómo se ejecuta) y escribe en la sección 2 un plan breve: qué carpetas revisarás y qué comandos usarás para cada comprobación. No preguntes nada todavía, salvo que no encuentres el código fuente.
- **Oleada 1 · Evidencia automática.** Ejecuta los comandos de cada comprobación (adáptalos a las carpetas reales del proyecto) y anota la evidencia. No decidas estados aún.
- **Oleada 2 · Juicio.** Con la evidencia, decide el estado de cada comprobación y escribe la observación. Lo que no se pueda decidir sin un dato externo, pregúntalo como se indica arriba. Al terminar, muestra las preguntas.
- **Oleada 3 · Cierre.** Integra las respuestas recibidas; cierra lo que quede sin respuesta como `NO VERIFICABLE`; escribe los hallazgos priorizados (sección 5); marca la autoverificación (sección 6) y rellena el resumen (sección 7). Termina diciendo al grupo cuántas preguntas quedaron sin respuesta.

**Si el proyecto tiene varios servicios** (por ejemplo, un backend en NestJS y otro en Python), evalúa cada comprobación sobre todos y anota la evidencia de cada uno; el estado es el del servicio peor evaluado.

---

## 1. Datos del repositorio (oleada 0)

- **Grupo:** G1 — UMSS Market (E-Commerce UMSS).
- **Repositorio:** https://github.com/christianvargasmpsia-cmd/Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales
- **Commit auditado (`release/8.0.0`):** 824eb0f5a00ef586d467d23dda8856ac576dabae
- **Fecha de la auditoría:** 2026-10-15
- **Stack detectado:** Java 21, Spring Boot 3.5.15, Maven, JPA/PostgreSQL, Ollama; Node.js con Newman y Playwright (agente CLI auxiliar). Evidencia: backend/umss-market-api/pom.xml:8,30,35,76; ai-testing-agent/package.json:8,28,30,34.
- **Carpetas de código fuente:** `backend/umss-market-api/src/main/`, `ai-testing-agent/src/`; `poc/` y `demo/` son prototipos, no servicios desplegados. No se encontró frontend Angular versionado.
- **Cómo se ejecuta (verificado):** README.md:504 y 505 documenta Maven; ai-testing-agent/package.json:9 define `npm run start:enhanced`. Arranque NO VERIFICABLE: `.\mvnw.cmd -v`, desde backend/umss-market-api, falló con `Cannot start maven from wrapper`. No se lanzó la aplicación contra la base configurada.
- **Integrantes y revisión:** Abad Melani Rodriguez Gonzales revisó F12-01, F12-02 y F12-03, registrando su aporte en el commit `b4ac2d3`. Christian Bernardo Vargas Sandoval completó y confirmó su revisión manual de F12-04, F12-05 y F12-06 el 15/10/2026. Cuenta con los commits anteriores `824eb0f` y `b5d5733`; falta registrar esta actualización con su propia identidad.

## 2. Plan y avance por oleadas

| Oleada | Qué se hizo | Estado |
|--------|-------------|--------|
| 0 · Reconocimiento | HEAD y rama verificados con git; inventario de archivos versionados. Revisar backend y agente CLI: búsquedas de configuración (01), inventario e historial sin valores sensibles (02), estado y escrituras (03), logging (04), clientes y puertos (05), build y cierre (06). Usar git grep, git ls-files, git log y lectura numerada con redacción. | Completada |
| 1 · Evidencia | Búsquedas por factor, inventario e historial dirigido; valores sensibles omitidos. Maven -v falla. | Completada |
| 2 · Juicio | Cinco fallas verificadas; construcción y apagado sin evidencia suficiente. P1 y P2 presentadas al grupo. | Completada |
| 3 · Cierre | Respuestas integradas: ejecución local sin prueba de apagado y revisión distribuida 50/50. F12-06 NO VERIFICABLE; revisión de Melani registrada en b4ac2d3 y revisión manual de Christian confirmada en F12-04/05/06. Falta registrar el aporte actual y confirmar el cierre del grupo. | Completada |

## 3. Comprobaciones

### F12-01 · Configuración fuera del código (factor III)

- **Qué se busca:** todo lo que cambia entre entornos (URLs, puertos, credenciales, banderas) se lee del entorno al arrancar, y la aplicación valida al inicio que esté presente. Mismo código en dev, pruebas y producción; solo cambia la configuración.
- **Señales de falla:** `localhost`, IPs o puertos escritos en el código; URLs de servicios fijas; condiciones del tipo `if (env === 'production')` repartidas por el código.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "localhost|127\.0\.0\.1|:5432|:3306|:6379|:27017|https?://" -- <carpetas de código>
  git grep -nE "process\.env|ConfigService|getOrThrow|@Value|@ConfigurationProperties|BaseSettings|os\.environ|os\.getenv" -- <carpetas de código>
  ```
- **Evidencia:** `backend/umss-market-api/src/main/resources/application.properties:3`: URL JDBC local fija; línea 18: `server.port=8080`; línea 22: `ia.enabled=${IA_HABILITADA:true}`. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/adapters/OllamaAdapter.java:25` y 28: URLs fijas de generación y embeddings; `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/config/SecurityConfig.java:41`: origen CORS local fijo. Agente: `ai-testing-agent/src/services/Ollama.service.js:11` usa `process.env.OLLAMA_BASE_URL` y línea 15 usa `OLLAMA_MODEL`. Búsqueda ejecutada: `git grep -n -E 'localhost|@Value|ConfigurationProperties|System.getenv|process.env' -- backend/umss-market-api/src/main ai-testing-agent/src ':!*.bak'` (salida filtrada para omitir líneas sensibles).
- **Estado:** FALLA
- **Observación:** Backend: FALLA por URLs de Ollama y CORS incrustadas en Java. Spring admite configuración externa, pero no se acredita validación integral al inicio. El agente usa variables con valores por defecto: PARCIAL. El resultado agregado refleja el peor componente.
- **Auditó:** Codex (análisis inicial). Abad Melani Rodriguez Gonzales (revisión humana, 15/10/2026). Se verificaron las URLs fijas de Ollama en OllamaAdapter.java:25,28 y la configuración CORS en SecurityConfig.java:41 mediante git grep. Se mantiene el estado FALLA. Commit de revisión: b4ac2d3 (Melani, 15/10/2026).

### F12-02 · Secretos fuera del repositorio y de su historial (factor III)

- **Qué se busca:** ninguna contraseña, token o clave en el código, en archivos versionados ni en el historial de git. El `.env` real está en `.gitignore`; solo se versiona un `.env.example` sin valores reales.
- **Señales de falla:** `.env` versionado; asignaciones del tipo `password = "..."` o `apiKey: '...'`; claves en `docker-compose.yml`, en un `Dockerfile` (`ENV` o `ARG`) o en archivos de configuración.
- **Comandos sugeridos** (no muestran valores):
  ```bash
  git ls-files | grep -E '(^|/)\.env$' ; git check-ignore -v .env
  git log --all --oneline -- .env
  git grep -c -I -i -E '(password|secret|api[_-]?key|token)[[:space:]]*[:=]' -- . ':!*.lock'
  git log --all --pickaxe-regex -S'(?i)password' --oneline --name-only
  gitleaks detect --source . --redact --no-banner   # opcional, si está instalado
  ```
- **Evidencia:** `backend/umss-market-api/src/main/resources/application.properties:7`: contraseña literal no vacía, sin referencia al entorno (valor omitido). `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/security/JwtService.java:17` y 18: declaración de secreto JWT literal (valor omitido). `ai-testing-agent/src/services/auth.service.js:598` y 599: contraseña fija de prueba (valor omitido). `git check-ignore -v .env backend/umss-market-api/.env ai-testing-agent/.env` devuelve únicamente `ai-testing-agent/.gitignore:8:.env ai-testing-agent/.env`. Inventario: solo `ai-testing-agent/.env.example`; no se abrió. `git log --all --format='%h %as' --name-only -- .env ':(glob)**/.env'`: sin salida. `git log --all --format='%h %as' -G 'password' -- backend/umss-market-api/src/main/resources/application.properties`: primeras líneas `0d49894 2026-10-14`, `c2229b6 2026-09-27`, `44f3d70 2026-09-27`. `git rev-parse --is-shallow-repository`: `false`. Se ejecutó además `git grep -c -I -i -E '(password|secret|api[_-]?key|token)[[:space:]]*[:=]' -- backend/umss-market-api/src/main ai-testing-agent/src ':!*.bak'`: application.properties devuelve 1 y auth.service.js devuelve 5; son coincidencias, no cinco secretos confirmados. Gitleaks no está disponible.
- **Estado:** FALLA
- **Observación:** FALLA: credenciales literales en backend y agente. No se verificó su vigencia ni se intentó usarlas. La revisión dirigida del historial no certifica ausencia de otros secretos. No se abrieron archivos .env ni de claves.
- **Auditó:** Codex (análisis inicial). Abad Melani Rodriguez Gonzales (revisión humana, 15/10/2026). Se comprobó una coincidencia de configuración de contraseña en application.properties mediante git grep -c, sin exponer su valor. La auditoría original documenta además un secreto JWT literal. Se mantiene el estado FALLA según las evidencias originales; la verificación del valor incrustado y del secreto JWT queda pendiente de confirmación. Commit de revisión: b4ac2d3 (Melani, 15/10/2026).

### F12-03 · Procesos sin estado (factor VI)

- **Qué se busca:** nada importante vive en la memoria del proceso ni en su disco local entre peticiones: sesiones, archivos subidos y cachés compartidas van a servicios de apoyo (base de datos, Redis, almacenamiento de objetos). Así se pueden levantar varias instancias.
- **Señales de falla:** archivos subidos guardados en una carpeta local; sesiones en memoria; un `Map` o diccionario global usado como caché compartida; trabajos programados que asumen una sola instancia.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "writeFileSync|writeFile\(|createWriteStream|diskStorage|mkdirSync|open\(.*['\"]w|UploadFile" -- <carpetas de código>
  git grep -nE "express-session|MemoryStore|new Map\(|@Cron|setInterval\(" -- <carpetas de código>
  ```
- **Evidencia:** `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/security/TokenBlacklistService.java:11` y 12: `private final Set<String> blacklistedTokens` con `ConcurrentHashMap.newKeySet()`; líneas 15 y 19: inserción y consulta. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/controllers/AuthController.java:142`: `tokenBlacklistService.blacklist(token)`; `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/security/JwtAuthenticationFilter.java:50`: consulta esa lista. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/monitoring/AIMonitoringService.java:97`: `Files.writeString`. Agente CLI: `ai-testing-agent/src/utils/FileManager.js:52` y 66, `fs.writeFileSync` para artefactos. Búsqueda ejecutada: `git grep -n -E 'ConcurrentHashMap|static.*Map|HttpSession|@Scheduled|Files\.|FileOutputStream|MultipartFile|SessionCreationPolicy' -- backend/umss-market-api/src/main`; encontró lista de revocación y monitoreo local.
- **Estado:** FALLA
- **Observación:** FALLA en backend: las revocaciones JWT se pierden al reiniciar y no se comparten entre réplicas. El monitoreo depende del disco local. Los reportes de la CLI auxiliar son artefactos de pruebas, no sesiones entre peticiones.
- **Auditó:** Codex (análisis inicial). Abad Melani Rodriguez Gonzales (revisión humana, 15/10/2026). Se comprobó el uso de ConcurrentHashMap.newKeySet() en TokenBlacklistService.java:12 y su utilización desde AuthController.java:142. Se mantiene el estado FALLA porque las revocaciones JWT se almacenan en memoria local. Commit de revisión: b4ac2d3 (Melani, 15/10/2026).

### F12-04 · Logs a la salida estándar (factor XI)

- **Qué se busca:** la aplicación escribe sus eventos en la salida estándar (idealmente en formato estructurado, por ejemplo JSON); recogerlos y guardarlos es tarea de la plataforma.
- **Señales de falla:** el logger escribe en archivos propios (`app.log`, rotación de archivos); `console.log` sueltos con datos sensibles.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "transports\.File|DailyRotateFile|pino\.destination|FileHandler|RotatingFileHandler|logging\.file|logback.*FILE" -- .
  git grep -nE "winston|pino|nestjs-pino|Logger|logging\.basicConfig|logback" -- <carpetas de código>
  ```
- **Evidencia:** `backend/umss-market-api/src/main/resources/application.properties:30`: `ai.monitoring.enabled=true`; línea 37: `ai.monitoring.log-path=logs/ai-monitoring.jsonl`. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/monitoring/AIMonitoringService.java:97` y 103 escriben con `Files.writeString` y `StandardOpenOption.APPEND`; líneas 64 y 65 guardan pregunta y respuesta. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/application/services/AIServiceImpl.java:276`: impresión de pregunta a consola. Agente: `ai-testing-agent/src/utils/Logger.js:11`: `console.log`. Evidencia localizada con `git grep -n -E 'Logger|System.out|console.log|Files\.' -- backend/umss-market-api/src/main ai-testing-agent/src ':!*.bak'` y lectura numerada del servicio de monitoreo.
- **Estado:** FALLA
- **Observación:** FALLA en backend: el monitoreo activo escribe eventos en un archivo propio y registra preguntas y respuestas que pueden contener información privada. El agente usa consola; sus reportes de pruebas no equivalen a logs operativos.
- **Auditó:** Christian Bernardo Vargas Sandoval, revisión manual realizada el 15/10/2026. Se confirmó en application.properties que el monitoreo está habilitado y en AIMonitoringService.java que escribe un archivo con APPEND y registra pregunta y respuesta. **Conclusión:** se mantiene FALLA porque los eventos se almacenan en un archivo propio en lugar de delegar su almacenamiento a la plataforma mediante salida estándar; además, debe revisarse el contenido de las consultas antes de registrarlo.

### F12-05 · Servicios de apoyo intercambiables (factor IV)

- **Qué se busca:** base de datos, colas, caché, almacenamiento y servicio de IA se conectan por una URL o variables de configuración, de modo que cambiar uno (por ejemplo, la base local por una gestionada) no exige tocar el código.
- **Señales de falla:** el cliente de un servicio se construye con host, puerto o credenciales fijas; el código depende de un proveedor concreto sin una capa que lo aísle.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "DATABASE_URL|REDIS_URL|AMQP_URL|S3_|BUCKET|OPENAI|ANTHROPIC|_URL\b" -- <carpetas de código>
  git grep -nE "createConnection|TypeOrmModule\.forRoot\(|PrismaClient|create_engine|MongoClient|new Redis\(" -- <carpetas de código>
  ```
- **Evidencia:** `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/adapters/OllamaAdapter.java:25` y 28: `http://localhost:11434/api/generate` y `http://localhost:11434/api/embed`. `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/config/OllamaConfig.java:12`: `return new RestTemplate()`. `backend/umss-market-api/src/main/resources/application.properties:3`: conexión PostgreSQL en propiedades; `backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/persistence/repositories/JpaOrderItemRepository.java:12`: `extends JpaRepository`. Agente: `ai-testing-agent/src/services/llm.service.js:18` y 23 permite `OLLAMA_BASE_URL` y `OLLAMA_MODEL`. Búsqueda ejecutada: `git grep -n -E 'localhost|process.env|baseURL' -- ai-testing-agent/src/config ai-testing-agent/src/services/llm.service.js ai-testing-agent/src/services/AI.service.js` con exclusión de líneas sensibles; primeras coincidencias llm.service.js:17 y 18.
- **Estado:** FALLA
- **Observación:** FALLA en backend: cambiar el host de Ollama exige editar Java, aunque exista un adaptador. JPA y la URL configurable del agente son avances parciales. No se presupone Redis o RabbitMQ implementado por figurar en documentación.
- **Auditó:** Christian Bernardo Vargas Sandoval, revisión manual realizada el 15/10/2026. Se confirmaron las URLs fijas de Ollama en OllamaAdapter.java y no se encontró configuración externa para esas direcciones. **Conclusión:** se mantiene FALLA porque cambiar la instancia de Ollama requiere editar el código Java.

### F12-06 · Construcción inmutable y apagado limpio (factores V y IX)

- **Qué se busca:** se construye una vez (imagen o artefacto) y se ejecuta igual en todos los entornos, sin secretos dentro de la imagen; y el proceso, al recibir `SIGTERM`, deja de aceptar peticiones, termina lo que tiene en curso y cierra conexiones.
- **Señales de falla:** `COPY .env` o `ENV` con secretos en el Dockerfile; `CMD` en forma de texto (`CMD npm start`) en lugar de forma exec; ningún manejo de cierre (en NestJS falta `app.enableShutdownHooks()`; en Spring alguien puso `server.shutdown: immediate`; en FastAPI no se cierran recursos en el `lifespan`).
- **Comandos sugeridos:**
  ```bash
  git ls-files | grep -iE "dockerfile|compose" 
  git grep -nE "COPY \.env|^ENV .*(PASS|SECRET|KEY|TOKEN)|^CMD [^\[]" -- '*Dockerfile*'
  git grep -nE "SIGTERM|enableShutdownHooks|onModuleDestroy|beforeApplicationShutdown|server\.shutdown|lifespan" -- .
  ```
- **Evidencia:** `git ls-files '*Dockerfile*' '*compose*' ':!:**/node_modules/**'`: sin salida. `git grep -n -E 'SIGTERM|enableShutdownHooks|PreDestroy|server.shutdown|timeout-per-shutdown-phase' -- backend/umss-market-api/src/main ai-testing-agent/src`: sin salida (código 1: sin coincidencias). `backend/umss-market-api/pom.xml:111`: `spring-boot-maven-plugin`; línea 8: Spring Boot 3.5.15. `README.md:769`: `.\mvnw.cmd spring-boot:run`. `ai-testing-agent/package.json:9`: `node src/index.enhanced.js`. `.\mvnw.cmd -v`, ejecutado desde backend/umss-market-api: código 1, primera línea `icm : No se puede indizar en una matriz nula.`; termina con `Cannot start maven from wrapper`. No se ejecutaron build, arranque ni prueba SIGTERM. La ausencia de hooks explícitos no demuestra que Spring carezca de cierre gestionado.
- **Estado:** NO VERIFICABLE
- **Observación:** El grupo confirmó en P1: solo ejecución local, sin prueba de apagado. Se mantiene NO VERIFICABLE: no existe evidencia de reutilización del artefacto entre entornos ni de cierre durante una petición. La ausencia de hooks explícitos no demuestra que Spring carezca de cierre gestionado. El wrapper falla al consultar su versión.
- **Auditó:** Christian Bernardo Vargas Sandoval, revisión manual realizada el 15/10/2026. Se confirmó el plugin spring-boot-maven-plugin en pom.xml, la ausencia de Dockerfile y que no se encontró configuración explícita de apagado. No se realizaron pruebas de reutilización del artefacto ni de apagado. **Conclusión:** se mantiene NO VERIFICABLE por falta de evidencia experimental; la ausencia de configuración explícita no demuestra un apagado incorrecto de Spring.

## 4. Preguntas al grupo

| N.º | Comprobación | Pregunta | Respuesta del grupo | Estado (respondida / sin respuesta) |
|-----|--------------|----------|---------------------|--------------------------------------|
| P1 | F12-06 | ¿Cómo se despliega y qué evidencia existe de reutilizar el artefacto y apagar durante peticiones? | Solo ejecución local; sin prueba de apagado, según respuesta del grupo. F12-06 sigue NO VERIFICABLE por falta de evidencia operativa. | respondida |
| P2 | Revisión humana / todas | ¿Qué revisó cada integrante y qué commits corresponden a la auditoría? | Reparto 50/50: Melani revisó F12-01 a F12-03, con aporte en b4ac2d3. Christian confirmó su revisión manual de F12-04 a F12-06 el 15/10/2026: monitoreo habilitado con APPEND y registro de pregunta/respuesta, URLs de Ollama fijas sin configuración externa, plugin Maven presente y Dockerfile ausente; sin configuración explícita de apagado encontrada ni pruebas de artefacto/apagado realizadas. Cuenta con los commits previos 824eb0f y b5d5733; esta actualización todavía no tiene commit propio. | respondida |

## 5. Hallazgos priorizados

| # | Comprobación | Severidad (alta / media / baja) | Qué falla | Corrección propuesta | Responsable | ¿Antes de contenedorizar (S07)? |
|---|--------------|---------------------------------|-----------|----------------------|-------------|---------------------------------|
| 1 | F12-02 | alta | Contraseña de BD y secreto JWT literales; credencial fija en agente. | Externalizar y validar al inicio; evaluar exposición, revocar/rotar valores afectados y sanear historial de forma coordinada. Añadir exclusión de .env en raíz/backend y escaneo del historial. | Christian Bernardo Vargas Sandoval (propuesto; aceptación no confirmada) | Sí |
| 2 | F12-03 | alta | Revocaciones JWT en memoria de una JVM. | Persistir revocaciones en servicio compartido con expiración y probar reinicio/dos réplicas. | Abad Melani Rodriguez Gonzales (propuesta; aceptación no confirmada) | Sí |
| 3 | F12-01 / F12-05 | media | Ollama y CORS fijados en Java; configuración sin validación integral acreditada. | Parametrizar endpoints, orígenes y parámetros por entorno; validar presencia/formato al arrancar. | Christian Bernardo Vargas Sandoval (propuesto) | Sí |
| 4 | F12-04 / F12-03 | media | Monitoreo en disco local y contenido de consultas en logs. | Emitir eventos estructurados a stdout, redactar datos y delegar retención en la plataforma. | Abad Melani Rodriguez Gonzales (propuesta) | Sí |
| 5 | F12-06 | media | Sin evidencia de promoción inmutable ni prueba de apagado; wrapper no funciona aquí. | Diagnosticar wrapper, construir artefacto identificable y aportar prueba de SIGTERM con petición en curso y cierre de conexiones. | Ambos integrantes (propuesto) | Sí, verificar durante la preparación |

## 6. Autoverificación (la marca el asistente; el grupo la confirma)

- [x] Las seis comprobaciones F12-01 a F12-06 están en la sección 3, una vez cada una.
- [x] Ninguna quedó en `PENDIENTE` ni sin estado; todos los estados son de los cinco permitidos.
- [x] Todo estado distinto de `N/A` y `NO VERIFICABLE` tiene evidencia con ruta y línea, o con comando y salida.
- [x] Cada `NO VERIFICABLE` y cada `N/A` explica su motivo.
- [x] Todas las preguntas de la sección 4 están respondidas o marcadas "sin respuesta".
- [x] Los hallazgos de severidad alta tienen responsable propuesto; el grupo debe confirmar la asignación.
- [x] No hay ningún secreto copiado en este archivo.
- [x] No se modificó código del producto durante la auditoría. Los cambios realizados para la entrega corresponden a documentos Markdown de auditoría.
- [ ] Cada integrante completó su revisión y tiene un commit propio de esta auditoría. La revisión manual de Christian de F12-04/05/06 está confirmada; falta registrar esta actualización con su propia identidad. Melani tiene el commit b4ac2d3, aunque F12-02 conserva la salvedad de confirmación indicada en su apartado. No se atribuye un commit nuevo inexistente.

## 7. Resumen

| OK | PARCIAL | FALLA | N/A | NO VERIFICABLE | Preguntas sin respuesta |
|---:|--------:|------:|----:|---------------:|------------------------:|
| 0 | 0 | 5 | 0 | 1 | 0 |

**Conclusión en dos líneas:** Hay separación de adaptadores, persistencia JPA y configuración externa parcial en el agente.
Primero deben corregirse secretos versionados y revocaciones locales; luego configuración, logs y verificación de construcción/apagado.
