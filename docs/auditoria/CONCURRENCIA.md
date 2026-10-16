# Auditoría de concurrencia del backend — <NOMBRE DEL GRUPO>

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

- **Grupo:** <nombre>
- **Commit auditado (`release/8.0.0`):** <hash>
- **Fecha de la auditoría:** <aaaa-mm-dd>
- **Servicios y runtime:** <por ejemplo: api · Node 22 + NestJS 11; ia · Python 3.12 + FastAPI>
- **Acceso a datos:** <TypeORM, Prisma, JPA/Hibernate, SQLAlchemy…> · **base de datos:** <…>
- **Cliente HTTP o SDK de IA:** <axios, fetch, httpx, RestClient, SDK de OpenAI o Anthropic…>
- **Integrantes y qué revisó cada uno:** <nombre → comprobaciones>

## 2. Plan y avance por oleadas

| Oleada | Qué se hizo | Estado |
|--------|-------------|--------|
| 0 · Reconocimiento | | |
| 1 · Evidencia | | |
| 2 · Juicio | | |
| 3 · Cierre | | |

## 3. Comprobaciones

### CON-01 · Modelo de concurrencia e instancias

- **Qué se busca:** saber cómo atiende peticiones simultáneas cada servicio (event loop, hilos, hilos virtuales, procesos o *workers*) y **cuántas instancias** corren en el entorno donde se despliega.
- **Señales de falla:** nadie sabe cuántas instancias corren; varias instancias pero un trabajo programado o un estado en memoria que supone una sola.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "NestFactory|app\.listen|cluster|worker_threads|spring\.threads\.virtual|uvicorn|gunicorn|granian|--workers|replicas:|deploy:" -- .
  ```
- **Evidencia:**
- **Estado:**
- **Observación:** <modelo de cada servicio y número de instancias>
- **Auditó:**

### CON-02 · Llamadas bloqueantes en el camino de una petición

- **Qué se busca:** que ningún manejador de petición haga trabajo síncrono que congele el servicio: lectura de archivos síncrona, bucles pesados, `JSON.parse` de archivos grandes, librerías síncronas dentro de código asíncrono.
- **Señales de falla:** en Node, `readFileSync`, `execSync`, cálculos pesados en el handler; en Python, `requests.` o `time.sleep` dentro de `async def`, o un SDK síncrono de IA llamado desde código asíncrono; en Java, bloqueo en WebFlux o `synchronized` alrededor de E/S con JDK anterior a 24.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "readFileSync|writeFileSync|execSync|spawnSync|JSON\.parse\(fs" -- <carpetas de código>
  git grep -nE "async def" -- '*.py'    # luego revisar si dentro se usa requests., time.sleep o un SDK síncrono
  git grep -nE "Mono|Flux|WebClient|synchronized" -- '*.java'
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### CON-03 · Pool de conexiones a la base de datos

- **Qué se busca:** que el tamaño del pool, el tiempo de espera para obtener una conexión y el error que devuelve la API cuando el pool se agota estén **definidos a propósito**, y que `instancias × tamaño del pool` no supere lo que admite la base de datos. Lo ideal al agotarse es un error rápido y claro (por ejemplo, 503), no una espera larga ni un 500 genérico.
- **Señales de falla:** valores por defecto sin revisar; pool enorme "por si acaso"; transacciones abiertas mientras se espera al servicio de IA.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "poolSize|max:|connectionLimit|connection_limit|maximum-pool-size|maximumPoolSize|pool_size|max_overflow|pool_timeout|connectionTimeout|acquireTimeout" -- .
  ```
- **Cálculo (ley de Little):** conexiones ocupadas en promedio = <peticiones por segundo> × <segundos de retención> = <resultado>. Total en la base de datos = <instancias> × <tamaño del pool> = <resultado>.
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### CON-04 · Timeouts y reintentos en llamadas externas (incluida la IA)

- **Qué se busca:** que toda llamada a otro servicio (HTTP, IA, colas) tenga **timeout explícito**; que los reintentos usen backoff con jitter y solo en operaciones idempotentes; y que las llamadas al LLM tengan un límite de llamadas simultáneas y no retengan conexiones de base de datos.
- **Señales de falla:** clientes HTTP sin timeout; reintentos inmediatos en bucle; la petición del usuario espera sin límite la respuesta del LLM.
- **Comandos sugeridos:**
  ```bash
  git grep -nE "axios|fetch\(|HttpService|httpx|requests\.|RestClient|WebClient|OpenAI|Anthropic|ChatClient" -- <carpetas de código>
  git grep -nE "timeout|retry|retries|backoff|AbortSignal|maxRetries|Semaphore|p-limit|bulkhead" -- <carpetas de código>
  ```
- **Evidencia:**
- **Estado:**
- **Observación:**
- **Auditó:**

### CON-05 · Hipótesis de cuello de botella

- **Qué se busca:** una frase que diga **qué recurso se saturará primero** cuando crezca el tráfico y por qué, basada en CON-01 a CON-04. Se comprobará con la primera prueba de carga con k6 (S04).
- **Formato:** "Bajo carga, se saturará primero <recurso> en <endpoint o flujo>, porque <motivo basado en CON-0x>."
- **Hipótesis:**
- **Estado:** <`OK` si la hipótesis está fundada en evidencia de CON-01 a CON-04; `NO VERIFICABLE` si faltan datos para formularla>
- **Auditó:**

## 4. Preguntas al grupo

| N.º | Comprobación | Pregunta | Respuesta del grupo | Estado (respondida / sin respuesta) |
|-----|--------------|----------|---------------------|--------------------------------------|
| P1 | | | | |

## 5. Hallazgos priorizados

| # | Comprobación | Severidad (alta / media / baja) | Qué falla | Corrección propuesta | Responsable |
|---|--------------|---------------------------------|-----------|----------------------|-------------|
| 1 | | | | | |

## 6. Autoverificación (la marca el asistente; el grupo la confirma)

- [ ] Las cinco comprobaciones CON-01 a CON-05 están en la sección 3, una vez cada una.
- [ ] Ninguna quedó en `PENDIENTE` ni sin estado; todos los estados son de los cinco permitidos.
- [ ] Todo estado distinto de `N/A` y `NO VERIFICABLE` tiene evidencia con ruta y línea, o con comando y salida.
- [ ] Las cifras usadas (tráfico, latencias, réplicas) vienen del código o de una respuesta del grupo, no de una suposición.
- [ ] CON-05 está formulada con el formato pedido y cita las comprobaciones en que se basa.
- [ ] Todas las preguntas de la sección 4 están respondidas o marcadas "sin respuesta".
- [ ] No hay ningún secreto copiado en este archivo.
- [ ] No se modificó código del producto.
- [ ] Cada integrante aparece como auditor de al menos una comprobación y tiene al menos un commit con su propia identidad.

## 7. Resumen

| OK | PARCIAL | FALLA | N/A | NO VERIFICABLE | Preguntas sin respuesta |
|---:|--------:|------:|----:|---------------:|------------------------:|
| | | | | | |

**Hipótesis de cuello de botella (copia de CON-05):** <…>
