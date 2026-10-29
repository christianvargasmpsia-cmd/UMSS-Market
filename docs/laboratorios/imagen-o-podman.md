
# Laboratorio S08: imagen o Podman

- **Integrante(s):** Abad Melani Rodriguez Gonzales y Christian Bernardo Vargas Sandoval
- **Opción:** A — Auditar y adelgazar la imagen
- **Servicio o stack:** Backend UMSS Market API — Java 21 / Spring Boot
- **Fecha:** 29 de octubre de 2026
- **Máquina:** Windows, Docker Desktop 4.96.0, Docker Engine 29.9.1

## Opción A: antes y después

Imagen: `umss-market-api`  
Dockerfile: `backend/umss-market-api/Dockerfile`

| Medida | Antes | Después | Comando con el que se midió |
|---|---:|---:|---|
| Tamaño en disco (DISK USAGE) | 857 MB | 537 MB | `docker image ls` |
| Tamaño comprimido (CONTENT SIZE) | 288 MB | 163 MB | `docker image ls` |
| Número de capas | 8 | 8 | `docker image inspect ... --format "{{len .RootFS.Layers}}"` |
| Eficiencia y espacio desperdiciado (Dive) | 99.7597 % / 2.1 MB | 99.6351 % / 2.1 MB | `dive` con `CI=true` |
| Usuario con el que corre | `root` (`uid=0`) | `appuser` (`uid=999`) | `docker run --rm --entrypoint id ...` |
| CVE totales (herramienta: Trivy) | 86 | 52 | Trivy 0.76.1 |
| CVE HIGH + CRITICAL | 13 | 13 | Trivy 0.76.1 |
| Avisos de hadolint | 0 | 1 (`DL3066`, nivel info) | `hadolint` |
| ¿Arranca y responde? | No verificado | Sí, Spring Boot inicia y responde HTTP. `/api/health` no existe en esta rama | `docker run -p 8081:8080 ...` |

### Qué encontré en la imagen de antes

La imagen inicial tenía un tamaño de **857 MB en disco** y **288 MB comprimidos**.

La imagen tenía **8 capas reales**. Al revisar `docker image history`, las capas más grandes provenían principalmente de la imagen base y de la instalación del entorno Java. Se identificaron capas de aproximadamente **309 MB**, **117 MB** y **73.1 MB**. Además, el archivo JAR de la aplicación agregaba aproximadamente **70.6 MB**.

Dive reportó una eficiencia de **99.7597 %** y aproximadamente **2.1 MB de espacio desperdiciado**. Esto mostró que el principal problema de tamaño no era el espacio desperdiciado entre capas, sino el peso de la imagen base y del JDK completo utilizado para ejecutar la aplicación.

También se comprobó que la aplicación se ejecutaba como usuario `root`:

```text
uid=0(root) gid=0(root) groups=0(root)
```

Esto implicaba que el proceso Java se ejecutaba con privilegios de root dentro del contenedor.

El análisis con Trivy encontró **86 vulnerabilidades en total**:

| Origen | Total | LOW | MEDIUM | HIGH | CRITICAL |
|---|---:|---:|---:|---:|---:|
| Ubuntu 26.04 | 65 | 12 | 53 | 0 | 0 |
| `app.jar` | 21 | 0 | 8 | 7 | 6 |
| **Total** | **86** | **12** | **61** | **7** | **6** |

Las vulnerabilidades HIGH y CRITICAL no provenían de la imagen base, sino principalmente de dependencias Java contenidas dentro de `app.jar`.

Entre las dependencias señaladas por Trivy se encontraron componentes como:

- Jackson
- Guava
- Apache Tomcat Embedded
- PostgreSQL JDBC
- Spring Web MVC
- Apache Commons Lang
- Log4j API

En total existían **7 vulnerabilidades HIGH y 6 CRITICAL**, es decir, **13 HIGH + CRITICAL**.

### Qué cambié

#### 1. Cambio de JDK completo a JRE

La imagen base inicial utilizaba:

```dockerfile
FROM eclipse-temurin:21
```

Se cambió por una imagen destinada únicamente a ejecución:

```dockerfile
FROM eclipse-temurin:21-jre-noble
```

Como el archivo JAR ya se genera previamente con Maven, no es necesario incluir un JDK completo dentro de la imagen utilizada para ejecutar la aplicación.

Este cambio redujo considerablemente el tamaño:

```text
DISK USAGE:
857 MB → 537 MB

CONTENT SIZE:
288 MB → 163 MB
```

Esto representa aproximadamente:

- **320 MB menos en disco (~37.3 %)**
- **125 MB menos en tamaño comprimido (~43.4 %)**

#### 2. Ejecución con usuario no root

Se creó un grupo y un usuario específico para la aplicación:

```dockerfile
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup appuser
```

Antes de iniciar Spring Boot se agregó:

```dockerfile
USER appuser
```

Después del cambio se verificó:

```text
uid=999(appuser) gid=999(appgroup) groups=999(appgroup)
```

La aplicación dejó de ejecutarse como `root`.

#### 3. Creación de `.dockerignore`

Se agregó un archivo `.dockerignore` para evitar enviar al contexto de construcción archivos innecesarios o sensibles, por ejemplo:

- `.git`
- `.env`
- `.env.*`
- archivos del IDE
- logs
- documentación y archivos temporales

No se excluyó `target/`, ya que el Dockerfile actual necesita copiar desde esa carpeta el JAR previamente compilado.

### Resultado de Hadolint

Al analizar el Dockerfile optimizado, Hadolint reportó un único aviso:

```text
DL3066 info: Non-numeric user-id may not be resolvable by host system
```

El aviso corresponde al uso de:

```dockerfile
USER appuser
```

en lugar de utilizar directamente un UID numérico.

Se mantuvo `appuser` porque el usuario existe dentro de la imagen y se verificó correctamente que la aplicación se ejecuta como:

```text
uid=999(appuser)
```

Por lo tanto, se trata de un aviso informativo y no de un error que impida construir o ejecutar la imagen.

### Resultados después de los cambios

La imagen optimizada quedó con un tamaño de **537 MB en disco** y **163 MB comprimidos**, manteniendo **8 capas**.

Dive reportó:

```text
efficiency: 99.6351 %
wastedBytes: 2105366 bytes (2.1 MB)
```

Aunque la eficiencia porcentual disminuyó ligeramente, el espacio desperdiciado se mantuvo prácticamente igual mientras el tamaño total de la imagen se redujo significativamente.

El análisis de Trivy después de cambiar la imagen base encontró:

| Origen | Total | LOW | MEDIUM | HIGH | CRITICAL |
|---|---:|---:|---:|---:|---:|
| Ubuntu 24.04 | 31 | 4 | 27 | 0 | 0 |
| `app.jar` | 21 | 0 | 8 | 7 | 6 |
| **Total** | **52** | **4** | **35** | **7** | **6** |

Las vulnerabilidades totales disminuyeron:

```text
86 → 52
```

Esto representa una reducción de **34 vulnerabilidades**.

Las vulnerabilidades correspondientes al sistema operativo disminuyeron de:

```text
65 → 31
```

Sin embargo, las vulnerabilidades HIGH y CRITICAL se mantuvieron:

```text
13 → 13
```

Esto se debe a que las vulnerabilidades de mayor severidad se encuentran dentro de las dependencias Java empaquetadas en `app.jar`, y no en la imagen base de Java.

### Prueba de ejecución

El primer intento se realizó con:

```powershell
docker run --rm -p 8080:8080 mi-imagen:despues
```

Docker devolvió un error porque el puerto `8080` del equipo ya estaba ocupado:

```text
Only one usage of each socket address (protocol/network address/port) is normally permitted.
```

Para resolverlo se utilizó el puerto `8081` del host:

```text
localhost:8081 → contenedor:8080
```

Posteriormente se detectó otro problema. Spring Boot iniciaba correctamente, pero no podía conectarse a PostgreSQL porque la configuración utilizaba:

```text
localhost:5432
```

Dentro de un contenedor, `localhost` hace referencia al propio contenedor y no a la máquina Windows donde se estaba ejecutando PostgreSQL.

El problema se resolvió ejecutando el contenedor con la URL de conexión proporcionada como variable de entorno y utilizando `host.docker.internal`:

```powershell
docker run --rm `
  -p 8081:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/umss_market" `
  -e SPRING_DATASOURCE_USERNAME="TU_USUARIO" `
  -e SPRING_DATASOURCE_PASSWORD="TU_PASSWORD" `
  mi-imagen:despues
```

Con esta configuración, la aplicación inició correctamente y pudo comunicarse con PostgreSQL.

Para mantener compatibilidad tanto al ejecutar Spring Boot directamente en Windows como dentro de Docker, es preferible mantener `localhost` como valor por defecto en `application.properties` y permitir que Docker lo sobrescriba mediante variables de entorno.

Por ejemplo:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/umss_market}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:...}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:...}
```

De esta forma:

```text
Spring Boot ejecutado directamente
→ localhost:5432

Spring Boot ejecutado en Docker
→ host.docker.internal:5432
```

### Comprobación HTTP

Se realizó la petición:

```powershell
curl.exe http://localhost:8081/api/health
```

La aplicación respondió:

```json
{
  "success": false,
  "message": "No static resource api/health.",
  "timestamp": "2026-10-29T18:55:44.10676992"
}
```

Esta respuesta confirma que el contenedor está levantado, el puerto `8081` está correctamente redirigido al puerto `8080` y Spring Boot está recibiendo y procesando peticiones HTTP.

Sin embargo, `/api/health` **no existe en esta rama del proyecto**, por lo que no puede utilizarse como prueba de un endpoint funcional de negocio. Para una validación completa se debe realizar posteriormente una petición a un endpoint existente de UMSS Market.

### Qué no pude resolver o no me convenció

Las **13 vulnerabilidades HIGH + CRITICAL** permanecen después de optimizar la imagen.

Estas vulnerabilidades no provienen de Ubuntu ni de la imagen base de Eclipse Temurin, sino de dependencias contenidas dentro de `app.jar`.

Entre las dependencias afectadas se encontraron:

- Apache Tomcat Embedded
- Spring Web MVC
- Jackson
- Guava
- PostgreSQL JDBC

Para reducir estas vulnerabilidades será necesario actualizar las dependencias correspondientes en el `pom.xml` o actualizar la versión de Spring Boot que gestiona varias de estas dependencias.

También se mantuvieron aproximadamente **2.1 MB de espacio desperdiciado** según Dive. Al tratarse de una cantidad pequeña en comparación con los **537 MB** totales de la imagen, no se consideró prioritario realizar más cambios únicamente para reducir este valor.

Hadolint continúa mostrando el aviso informativo `DL3066` por utilizar el nombre `appuser` en `USER` en vez de un UID numérico. Sin embargo, el usuario existe correctamente dentro de la imagen y se verificó que el proceso ya no se ejecuta como root.

Finalmente, el endpoint `/api/health` no existe en esta rama del proyecto. La prueba permitió comprobar que la aplicación responde HTTP, pero queda como mejora adicional probar un endpoint real existente y obtener una respuesta funcional.

## Comandos usados

### Verificación del entorno

```powershell
java -version

.\mvnw.cmd -version

docker version
```

Versiones de Docker utilizadas:

```text
Docker Client: 29.9.1
Docker Engine: 29.9.1
Docker Desktop: 4.96.0
```

### Compilación del proyecto

```powershell
.\mvnw.cmd clean package
```

### Construcción de la imagen inicial

```powershell
docker build -t umss-market-api:actual .

docker image ls

docker tag umss-market-api:actual mi-imagen:antes
```

### Tamaño y capas — antes

```powershell
docker image ls mi-imagen:antes

docker image history mi-imagen:antes

docker image inspect mi-imagen:antes --format "{{len .RootFS.Layers}}"
```

### Dive — antes

```powershell
docker run --rm -e CI=true -v /var/run/docker.sock:/var/run/docker.sock wagoodman/dive:v0.13.1@sha256:f1886e6c32c094fc41a623c1989f5cb3e48aa766da5f0be233f911fc1d85ce10 mi-imagen:antes
```

### Usuario — antes

```powershell
docker image inspect mi-imagen:antes --format "{{.Config.User}}"

docker run --rm --entrypoint id mi-imagen:antes
```

Resultado:

```text
uid=0(root) gid=0(root) groups=0(root)
```

### Trivy — antes

```powershell
docker run --rm `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v trivy-cache:/root/.cache `
  aquasec/trivy:0.76.1@sha256:790fd66d1697395d8693d1acaa1ea702406a20f109b6fc7bac8db623eca6c06c `
  image --timeout 30m --scanners vuln mi-imagen:antes
```

### Hadolint — antes

```powershell
Get-Content Dockerfile | docker run --rm -i hadolint/hadolint:v2.15.1@sha256:32dac94127fd60b7b7e3fbfc65e1383b9b5e25c9bfd7b8536de7a539fe68a12d
```

Resultado:

```text
0 avisos
```

### Construcción de la imagen optimizada

```powershell
docker build -t umss-market-api:optimizada .

docker tag umss-market-api:optimizada mi-imagen:despues
```

### Tamaño y capas — después

```powershell
docker image ls mi-imagen:despues

docker image history mi-imagen:despues

docker image inspect mi-imagen:despues --format "{{len .RootFS.Layers}}"
```

### Dive — después

```powershell
docker run --rm -e CI=true -v /var/run/docker.sock:/var/run/docker.sock wagoodman/dive:v0.13.1@sha256:f1886e6c32c094fc41a623c1989f5cb3e48aa766da5f0be233f911fc1d85ce10 mi-imagen:despues
```

### Usuario — después

```powershell
docker image inspect mi-imagen:despues --format "{{.Config.User}}"

docker run --rm --entrypoint id mi-imagen:despues
```

Resultado:

```text
appuser
uid=999(appuser) gid=999(appgroup) groups=999(appgroup)
```

### Trivy — después

```powershell
docker run --rm `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v trivy-cache:/root/.cache `
  aquasec/trivy:0.76.1@sha256:790fd66d1697395d8693d1acaa1ea702406a20f109b6fc7bac8db623eca6c06c `
  image --timeout 30m --scanners vuln mi-imagen:despues
```

### Hadolint — después

```powershell
Get-Content Dockerfile | docker run --rm -i hadolint/hadolint:v2.15.1@sha256:32dac94127fd60b7b7e3fbfc65e1383b9b5e25c9bfd7b8536de7a539fe68a12d
```

Resultado:

```text
DL3066 info: Non-numeric user-id may not be resolvable by host system
```

### Primer intento de ejecución

```powershell
docker run --rm -p 8080:8080 mi-imagen:despues
```

Falló porque el puerto `8080` del host estaba ocupado.

### Ejecución con otro puerto

```powershell
docker run --rm -p 8081:8080 mi-imagen:despues
```

Spring Boot inició, pero PostgreSQL no era accesible utilizando `localhost:5432`.

### Ejecución final con PostgreSQL accesible desde Docker

```powershell
docker run --rm `
  -p 8081:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/umss_market" `
  -e SPRING_DATASOURCE_USERNAME="TU_USUARIO" `
  -e SPRING_DATASOURCE_PASSWORD="TU_PASSWORD" `
  mi-imagen:despues
```

Con esta configuración la aplicación inició correctamente.

### Prueba HTTP

```powershell
curl.exe http://localhost:8081/api/health
```

Respuesta:

```json
{
  "success": false,
  "message": "No static resource api/health.",
  "timestamp": "2026-10-29T18:55:44.10676992"
}
```

La respuesta confirmó que Spring Boot estaba recibiendo peticiones HTTP, aunque el endpoint `/api/health` no existe actualmente en esta rama.

## Opción B: Podman

**No aplica.**

Para este laboratorio se seleccionó la **Opción A: auditar y adelgazar la imagen Docker del backend de UMSS Market**.