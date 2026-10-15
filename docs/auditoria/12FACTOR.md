# Auditoría 12-factor — <NOMBRE DEL GRUPO>

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

- **Grupo:** <nombre>
- **Repositorio:** <URL>
- **Commit auditado (`release/8.0.0`):** <hash>
- **Fecha de la auditoría:** <aaaa-mm-dd>
- **Stack detectado:** <lenguajes, frameworks, base de datos, cola, otros servicios>
- **Carpetas de código fuente:** <por ejemplo `src/`, `backend/app/`>
- **Cómo se ejecuta (verificado):** <comando y si funcionó>
- **Integrantes y qué revisó cada uno:** <nombre → comprobaciones>

## 2. Plan y avance por oleadas

| Oleada | Qué se hizo | Estado |
|--------|-------------|--------|
| 0 · Reconocimiento | | |
| 1 · Evidencia | | |
| 2 · Juicio | | |
| 3 · Cierre | | |

## 3. Comprobaciones

### F12-01 · Configuración fuera del código (factor III)

- **Qué se busca:** todo lo que cambia entre entornos (URLs, puertos, credenciales, banderas) se lee del entorno al arrancar, y la aplicación valida al inicio que esté presente. Mismo código en dev, pruebas y producción; solo cambia la configuración.
- **Señales de falla:** `localhost`, IPs o puertos escritos en el código; URLs de servicios fijas; condiciones del tipo `if (env === 'production')` repartidas por el código.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "localhost|127\.0\.0\.1|:5432|:3306|:6379|:27017|https?://" -- <carpetas de código>
  git grep -nE "process\.env|ConfigService|getOrThrow|@Value|@ConfigurationProperties|BaseSettings|os\.environ|os\.getenv" -- <carpetas de código>
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

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
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### F12-03 · Procesos sin estado (factor VI)

- **Qué se busca:** nada importante vive en la memoria del proceso ni en su disco local entre peticiones: sesiones, archivos subidos y cachés compartidas van a servicios de apoyo (base de datos, Redis, almacenamiento de objetos). Así se pueden levantar varias instancias.
- **Señales de falla:** archivos subidos guardados en una carpeta local; sesiones en memoria; un `Map` o diccionario global usado como caché compartida; trabajos programados que asumen una sola instancia.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "writeFileSync|writeFile\(|createWriteStream|diskStorage|mkdirSync|open\(.*['\"]w|UploadFile" -- <carpetas de código>
  git grep -nE "express-session|MemoryStore|new Map\(|@Cron|setInterval\(" -- <carpetas de código>
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### F12-04 · Logs a la salida estándar (factor XI)

- **Qué se busca:** la aplicación escribe sus eventos en la salida estándar (idealmente en formato estructurado, por ejemplo JSON); recogerlos y guardarlos es tarea de la plataforma.
- **Señales de falla:** el logger escribe en archivos propios (`app.log`, rotación de archivos); `console.log` sueltos con datos sensibles.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "transports\.File|DailyRotateFile|pino\.destination|FileHandler|RotatingFileHandler|logging\.file|logback.*FILE" -- .
  git grep -nE "winston|pino|nestjs-pino|Logger|logging\.basicConfig|logback" -- <carpetas de código>
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### F12-05 · Servicios de apoyo intercambiables (factor IV)

- **Qué se busca:** base de datos, colas, caché, almacenamiento y servicio de IA se conectan por una URL o variables de configuración, de modo que cambiar uno (por ejemplo, la base local por una gestionada) no exige tocar el código.
- **Señales de falla:** el cliente de un servicio se construye con host, puerto o credenciales fijas; el código depende de un proveedor concreto sin una capa que lo aísle.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "DATABASE_URL|REDIS_URL|AMQP_URL|S3_|BUCKET|OPENAI|ANTHROPIC|_URL\b" -- <carpetas de código>
  git grep -nE "createConnection|TypeOrmModule\.forRoot\(|PrismaClient|create_engine|MongoClient|new Redis\(" -- <carpetas de código>
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### F12-06 · Construcción inmutable y apagado limpio (factores V y IX)

- **Qué se busca:** se construye una vez (imagen o artefacto) y se ejecuta igual en todos los entornos, sin secretos dentro de la imagen; y el proceso, al recibir `SIGTERM`, deja de aceptar peticiones, termina lo que tiene en curso y cierra conexiones.
- **Señales de falla:** `COPY .env` o `ENV` con secretos en el Dockerfile; `CMD` en forma de texto (`CMD npm start`) en lugar de forma exec; ningún manejo de cierre (en NestJS falta `app.enableShutdownHooks()`; en Spring alguien puso `server.shutdown: immediate`; en FastAPI no se cierran recursos en el `lifespan`).
- **Comandos sugeridos:**
  ```bash
  git ls-files | grep -iE "dockerfile|compose" 
  git grep -nE "COPY \.env|^ENV .*(PASS|SECRET|KEY|TOKEN)|^CMD [^\[]" -- '*Dockerfile*'
  git grep -nE "SIGTERM|enableShutdownHooks|onModuleDestroy|beforeApplicationShutdown|server\.shutdown|lifespan" -- .
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

## 4. Preguntas al grupo

| N.º | Comprobación | Pregunta | Respuesta del grupo | Estado (respondida / sin respuesta) |
|-----|--------------|----------|---------------------|--------------------------------------|
| P1 | | | | |

## 5. Hallazgos priorizados

| # | Comprobación | Severidad (alta / media / baja) | Qué falla | Corrección propuesta | Responsable | ¿Antes de contenedorizar (S07)? |
|---|--------------|---------------------------------|-----------|----------------------|-------------|---------------------------------|
| 1 | | | | | | |

## 6. Autoverificación (la marca el asistente; el grupo la confirma)

- [ ] Las seis comprobaciones F12-01 a F12-06 están en la sección 3, una vez cada una.
- [ ] Ninguna quedó en `PENDIENTE` ni sin estado; todos los estados son de los cinco permitidos.
- [ ] Todo estado distinto de `N/A` y `NO VERIFICABLE` tiene evidencia con ruta y línea, o con comando y salida.
- [ ] Cada `NO VERIFICABLE` y cada `N/A` explica su motivo.
- [ ] Todas las preguntas de la sección 4 están respondidas o marcadas "sin respuesta".
- [ ] Los hallazgos de severidad alta tienen responsable.
- [ ] No hay ningún secreto copiado en este archivo.
- [ ] No se modificó código del producto (`git diff --stat <commit auditado>..HEAD` solo muestra este archivo y el de aportes).
- [ ] Cada integrante aparece como auditor de al menos una comprobación y tiene al menos un commit con su propia identidad.

## 7. Resumen

| OK | PARCIAL | FALLA | N/A | NO VERIFICABLE | Preguntas sin respuesta |
|---:|--------:|------:|----:|---------------:|------------------------:|
| | | | | | |

**Conclusión en dos líneas:** <qué está bien y qué hay que corregir primero>
