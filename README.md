# 🛒 UMSS Market

> Marketplace universitario para la comunidad de la Universidad Mayor de San Simón (UMSS).

## 📌 Descripción

UMSS Market es un marketplace universitario orientado a digitalizar y centralizar la oferta de productos y servicios de emprendimientos estudiantiles.

La plataforma permite gestionar usuarios, tiendas, publicaciones, catálogo e interacciones. Sobre esta base se incorporaron capacidades de Inteligencia Artificial para realizar consultas en lenguaje natural, búsqueda semántica y recuperación contextual de información mediante RAG.

Además, se desarrolló un **AI Testing Agent** que integra herramientas para pruebas de API, pruebas End-to-End y análisis asistido por Inteligencia Artificial.

La evolución de la solución fue:

```text
Marketplace
    ↓
Tool Calling
    ↓
RAG #1
    ↓
RAG #2
    ↓
RAG #3
    ↓
RAG #4
    ↓
RAG #5
    ↓
AI Testing Agent
    ↓
MCP + Postman + Playwright
    ↓
AI Test Analyzer
```

---

# 📋 Índice

- [Problema](#-problema)
- [Solución](#-solución)
- [Objetivos](#-objetivos)
- [Usuarios](#-usuarios)
- [Funcionalidades](#-funcionalidades)
- [Arquitectura](#-arquitectura)
- [Stack tecnológico](#-stack-tecnológico)
- [Inteligencia Artificial](#-inteligencia-artificial)
- [Evolución de la solución](#-evolución-de-la-solución)
- [Capacidades RAG](#-capacidades-rag)
- [AI Testing Agent](#-ai-testing-agent)
- [Requisitos previos](#-requisitos-previos)
- [Configuración](#-configuración)
- [Instalación y ejecución](#-instalación-y-ejecución)
- [Pruebas](#-pruebas)
- [Variables de entorno](#-variables-de-entorno)
- [Seguridad](#-seguridad)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Alcance](#-alcance)
- [Mejoras futuras](#-mejoras-futuras)
- [Equipo](#-equipo)
- [Checklist de entrega](#-checklist-de-entrega)

---

# ❗ Problema

Los emprendimientos universitarios necesitan un medio centralizado para mostrar sus productos y servicios.

Actualmente, parte de esta información puede gestionarse mediante:

- Redes sociales.
- Mensajería.
- Publicaciones manuales.
- Comunicación directa entre compradores y emprendedores.

Esto dificulta:

- Encontrar productos.
- Consultar información específica.
- Descubrir emprendimientos.
- Centralizar la oferta disponible.
- Aprovechar el historial de interacción del usuario.

Por otro lado, una búsqueda tradicional basada únicamente en coincidencias de texto no siempre permite interpretar correctamente la intención del usuario.

---

# 💡 Solución

UMSS Market centraliza la información de:

- Usuarios.
- Tiendas.
- Publicaciones.
- Catálogo.
- Interacciones.

La solución incorpora Inteligencia Artificial para permitir consultas en lenguaje natural y recuperar información relevante desde los datos existentes.

El sistema no utiliza IA como sustituto de la base de datos.

La IA se utiliza para:

1. Interpretar la consulta.
2. Seleccionar la capacidad apropiada.
3. Recuperar información relevante.
4. Construir contexto.
5. Generar una respuesta basada en dicho contexto.

---

# 🎯 Objetivos

## Objetivo general

Desarrollar un marketplace universitario que permita centralizar emprendimientos estudiantiles y mejorar la consulta de información mediante capacidades de Inteligencia Artificial.

## Objetivos específicos

- Centralizar tiendas y publicaciones.
- Facilitar la búsqueda de productos.
- Permitir consultas en lenguaje natural.
- Implementar recuperación semántica.
- Utilizar información existente como contexto para la IA.
- Incorporar capacidades RAG.
- Automatizar pruebas mediante un AI Testing Agent.
- Integrar pruebas de API y End-to-End.
- Incorporar análisis asistido por IA sobre los resultados de testing.

---

# 👥 Usuarios

| Usuario | Descripción |
|---|---|
| **Comprador** | Consulta publicaciones, tiendas y realiza interacciones con el catálogo. |
| **Emprendedor** | Administra una tienda y sus publicaciones. |
| **Administrador** | Gestiona y supervisa información de la plataforma. |

---

# ✨ Funcionalidades

## Usuarios

- Registro.
- Inicio de sesión.
- Autenticación.
- Gestión de usuarios.
- Gestión de roles.
- Gestión de estados.

## Tiendas

- Creación de tiendas.
- Actualización de tiendas.
- Consulta de tiendas.
- Consulta del perfil público.
- Asociación de publicaciones.

## Publicaciones

- Creación.
- Actualización.
- Consulta.
- Consulta detallada.
- Gestión de estado.
- Precio.
- Stock.

## Catálogo

- Consulta de publicaciones.
- Filtrado.
- Búsqueda semántica.

## Interacciones

El sistema registra interacciones de los usuarios con publicaciones.

Estas interacciones pueden utilizarse posteriormente como contexto para las capacidades de IA.

---

# 🏗️ Arquitectura

El backend está organizado separando las responsabilidades principales:

```text
                    ┌──────────────────────┐
                    │      REST API        │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │     Controllers      │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │      Use Cases       │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │       Domain         │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │       Ports          │
                    └──────────┬───────────┘
                               ↓
              ┌────────────────┴────────────────┐
              ↓                                 ↓
   ┌──────────────────────┐          ┌──────────────────────┐
   │ PostgreSQL Adapters  │          │    Ollama Adapter    │
   └──────────────────────┘          └──────────────────────┘
```

La implementación utiliza conceptos de:

- Clean Architecture.
- Dependency Rule.
- Ports & Adapters.
- Separación entre dominio e infraestructura.
- Casos de uso.
- Adaptadores de persistencia.
- Adaptador para Ollama.

---

# 🛠️ Stack tecnológico

| Capa | Tecnología | Uso |
|---|---|---|
| Backend | Spring Boot | API y lógica del sistema |
| Lenguaje | Java | Desarrollo del backend |
| Build | Maven | Compilación y pruebas |
| Base de datos | PostgreSQL | Persistencia |
| API | REST | Comunicación con el backend |
| Seguridad | JWT | Autenticación |
| IA | Ollama | Ejecución local de modelos |
| Embeddings | `nomic-embed-text` | Representación semántica |
| LLM | `qwen2.5-coder:7b` | Generación y análisis |
| API Testing | Postman / Newman | Pruebas de API |
| E2E Testing | Playwright | Pruebas End-to-End |
| AI Testing Agent | Node.js / npm | Automatización del testing |
| Integración | MCP | Conexión del agente con herramientas |

> Las versiones exactas deben mantenerse alineadas con `pom.xml`, `package.json` y las configuraciones reales del proyecto.

---

# 🤖 Inteligencia Artificial

La integración de IA se construyó de manera incremental.

## Embeddings

```text
nomic-embed-text
```

Se utiliza para transformar información textual en representaciones vectoriales que permiten realizar recuperación semántica.

## Modelo generativo

```text
qwen2.5-coder:7b
```

Se utiliza mediante Ollama para procesar consultas y generar respuestas a partir del contexto recuperado.

---

# 🔄 Evolución de la solución

## Etapa 1 — Tool Calling

La primera aproximación consistió en conectar el lenguaje natural con herramientas reales del backend.

El modelo podía determinar qué capacidad debía utilizar según la consulta.

```text
Consulta del usuario
        ↓
Tool Calling
        ↓
Selección de herramienta
        ↓
Backend
        ↓
Información
        ↓
Respuesta
```

Esta etapa permitió establecer el vínculo entre la consulta en lenguaje natural y las capacidades reales del sistema.

## Etapa 2 — RAG

Posteriormente se evolucionó la solución hacia Retrieval-Augmented Generation.

```text
Pregunta del usuario
        ↓
Interpretación
        ↓
Retrieval
        ↓
Información relevante
        ↓
Contexto
        ↓
LLM
        ↓
Respuesta
```

La ventaja principal es que el modelo no necesita responder únicamente desde su conocimiento general, sino que utiliza información recuperada desde UMSS Market.

---

# 🧠 Capacidades RAG

La solución implementa cinco capacidades RAG.

## RAG #1 — Búsqueda semántica del catálogo

Permite buscar publicaciones mediante el significado de la consulta.

### Ejemplo

Consulta:

```text
Necesito algo para programar
```

Resultado validado:

```text
Laptop Lenovo ThinkPad
```

### Flujo

```text
Usuario
   ↓
SEARCH_CATALOG
   ↓
Embedding
   ↓
nomic-embed-text
   ↓
Retrieval semántico
   ↓
Top-K publicaciones
   ↓
Contexto
   ↓
qwen2.5-coder:7b
   ↓
Respuesta
```

### Componentes relacionados

```text
SearchCatalogUseCase
SearchCatalogSemanticTest
OllamaAdapterEmbeddingTest
```

---

## RAG #2 — Detalle de publicación

Permite responder preguntas sobre una publicación concreta.

### Ejemplos

```text
¿Qué características tiene la laptop?

¿Cuánto cuesta?

¿Cuánto stock tiene?

¿Qué información tiene esta publicación?
```

### Información utilizada

- Publicación.
- Precio.
- Stock.
- Descripción.
- Información disponible del producto.

### Componentes relacionados

```text
GetPublicationDetailUseCase
GetPublicationDetailSemanticUseCase
PublicationEntity
GetPublicationDetailSemanticUseCaseTest
```

---

## RAG #3 — Tiendas y emprendedores

Permite consultar información relacionada con tiendas y emprendimientos.

### Ejemplos

```text
¿Qué tienda vende productos para programación?

¿Qué ofrece esta tienda?

¿Qué productos tiene este emprendimiento?
```

### Componentes relacionados

```text
GetStorePublicProfileUseCase
SearchStoresBySemanticUseCase
GetStoresUseCase
```

---

## RAG #4 — Interacciones del usuario

Permite utilizar el historial de interacciones almacenado en el sistema como contexto.

### Ejemplos

```text
¿Con qué publicaciones interactué?

¿Qué productos me interesaron?

¿Qué publicaciones consulté anteriormente?
```

### Componentes relacionados

```text
GetUserInteractionsUseCase
GetUserInteractionsSemanticUseCase
CreateInteractionUseCase
```

---

## RAG #5 — Recomendaciones

Utiliza información recuperada desde diferentes fuentes para generar recomendaciones.

Puede utilizar información relacionada con:

- Publicaciones.
- Tiendas.
- Interacciones.
- Consultas del usuario.

### Ejemplos

```text
¿Qué me recomiendas para estudiar programación?

¿Qué productos podrían interesarme?

Recomiéndame productos similares.
```

### Componente relacionado

```text
GetRecommendationsUseCase
```

---

# 🧪 Validación de los RAG

Los RAG fueron validados mediante pruebas automatizadas y pruebas funcionales.

Entre las pruebas relacionadas con IA y recuperación se encuentran:

```text
SearchCatalogSemanticTest
GetPublicationDetailSemanticUseCaseTest
OllamaAdapterEmbeddingTest
OllamaAdapterToolSelectionTest
```

La ejecución general de las pruebas del backend se realiza mediante:

```powershell
cd .\backend\umss-market-api\
.\mvnw.cmd test
```

La prueba funcional del RAG #1 puede validarse mediante:

```text
Necesito algo para programar
```

y comprobando la recuperación de una publicación semánticamente relacionada.

---

# 🤖 AI Testing Agent

El proyecto incluye un AI Testing Agent desarrollado con Node.js.

Su objetivo es centralizar la ejecución y análisis de pruebas mediante diferentes agentes y servicios.

El flujo general es:

```text
AI Testing Agent
       ↓
   Menú principal
       ↓
 ┌─────┼──────────────┐
 ↓     ↓              ↓
MCP   Playwright    Analyzer
 ↓     ↓              ↓
API   E2E           IA
Tests Tests        Analysis
```

## Ejecución

```powershell
cd .\ai-testing-agent\
npm install
npm run start:enhanced
```

El sistema presenta un menú desde el cual se seleccionan las diferentes capacidades.

---

# 🔌 MCP Postman Agent

El MCP Postman Agent permite trabajar con colecciones de Postman y ejecutar pruebas de API mediante Newman.

Flujo:

```text
Colección Postman
        ↓
MCP
        ↓
Postman Agent
        ↓
Newman
        ↓
Resultados
        ↓
AI Test Analyzer
```

La API key de Postman se configura mediante una variable de entorno.

La API key real nunca debe almacenarse en el repositorio.

---

# 🌐 AI Playwright Testing Agent

El AI Playwright Testing Agent permite ejecutar pruebas End-to-End.

Puede trabajar con escenarios como:

- Login.
- Búsqueda de publicaciones.
- Consulta de productos.
- Consulta de tiendas.
- Otros escenarios disponibles en la suite.

Flujo:

```text
Escenario
   ↓
AI Playwright Agent
   ↓
Playwright
   ↓
Aplicación
   ↓
Resultado
```

---

# 🧠 AI Test Analyzer

El AI Test Analyzer analiza los resultados obtenidos por las pruebas.

Puede utilizar IA para:

- Analizar resultados.
- Identificar fallos.
- Clasificar problemas.
- Identificar posibles causas.
- Generar recomendaciones.

Flujo:

```text
Resultados de pruebas
        ↓
AI Test Analyzer
        ↓
Ollama
        ↓
Análisis
        ↓
Reporte
```

---

# 📦 Requisitos previos

Antes de ejecutar el proyecto se requiere:

- Git.
- Java JDK.
- Maven o Maven Wrapper.
- Node.js.
- npm.
- PostgreSQL.
- Ollama.

Para las capacidades de IA se requiere disponer de Ollama y los modelos configurados por el proyecto.

---

# 🗄️ PostgreSQL

Crear una base de datos:

```text
umss_market
```

Configuración utilizada por el backend:

```text
Host: localhost
Puerto: 5432
Base de datos: umss_market
Usuario: postgres
```

La contraseña debe configurarse localmente.

---

# ⚙️ Configuración del backend

Archivo:

```text
backend/umss-market-api/src/main/resources/application.properties
```

Configuración:

```properties
spring.application.name=umss-market-api

spring.datasource.url=jdbc:postgresql://localhost:5432/umss_market
spring.datasource.username=postgres
spring.datasource.password=${DATABASE_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

server.port=8080

logging.level.org.springframework.security=DEBUG

ia.enabled=${IA_HABILITADA:true}
```

Configurar la contraseña localmente.

### PowerShell

```powershell
$env:DATABASE_PASSWORD="TU_PASSWORD_LOCAL"
$env:IA_HABILITADA="true"
```

No introducir la contraseña real en el repositorio.

---

# 🧠 Configuración de Ollama

El proyecto utiliza Ollama para las funcionalidades de IA.

URL:

```text
http://localhost:11434
```

Modelos:

```text
nomic-embed-text
qwen2.5-coder:7b
```

El AI Testing Agent utiliza:

```text
http://localhost:11434/v1
```

---

# 🚀 Instalación

## 1. Clonar

```bash
git clone <URL_DEL_REPOSITORIO>
```

Entrar al proyecto:

```bash
cd Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales
```

---

# ▶️ Ejecutar backend

Entrar al backend:

```powershell
cd .\backend\umss-market-api\
```

Preparar dependencias:

```powershell
.\mvnw.cmd clean install
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Servidor:

```text
http://localhost:8080
```

---

# ▶️ Ejecutar AI Testing Agent

En otra terminal:

```powershell
cd .\ai-testing-agent\
```

Instalar dependencias:

```powershell
npm install
```

Ejecutar:

```powershell
npm run start:enhanced
```

---

# 🧪 Pruebas

## Pruebas del backend

```powershell
cd .\backend\umss-market-api\
.\mvnw.cmd test
```

Esta ejecución permite validar la suite automatizada del backend.

## Pruebas RAG

Se validan las capacidades:

```text
RAG #1 — Catálogo
RAG #2 — Detalle de publicación
RAG #3 — Tiendas y emprendedores
RAG #4 — Interacciones
RAG #5 — Recomendaciones
```

Las pruebas relacionadas con IA incluyen:

```text
SearchCatalogSemanticTest
GetPublicationDetailSemanticUseCaseTest
OllamaAdapterEmbeddingTest
OllamaAdapterToolSelectionTest
```

## Pruebas del AI Testing Agent

Ejecutar:

```powershell
cd .\ai-testing-agent\
npm run start:enhanced
```

Desde el menú se pueden ejecutar:

```text
MCP Postman Agent
AI Playwright Testing Agent
AI Test Analyzer
```

---

# 📊 Reportes

Los reportes generados por el sistema pueden almacenarse en:

```text
ai-testing-agent/reports/
```

Los reportes generados automáticamente deben mantenerse fuera del repositorio cuando estén incluidos en `.gitignore`.

Los reportes que se conserven para documentación no deben contener:

- API keys.
- Contraseñas.
- Tokens.
- JWT.
- Credenciales.

---

# 🔑 Variables de entorno

## AI Testing Agent

Archivo local:

```text
ai-testing-agent/.env
```

Plantilla:

```text
ai-testing-agent/.env.example
```

Contenido de referencia:

```env
POSTMAN_API_KEY=TU_POSTMAN_API_KEY

OLLAMA_BASE_URL=http://localhost:11434/v1
OLLAMA_MODEL=qwen2.5-coder:7b

PLAYWRIGHT_API_BASE_URL=http://localhost:8080

TEST_USER_EMAIL=usuario_de_prueba@umss.edu.bo
TEST_USER_PASSWORD=TU_PASSWORD_DE_PRUEBA
```

### Regla

```text
.env.example → SÍ se sube
.env         → NO se sube
```

El archivo `.env.example` nunca debe contener valores sensibles reales.

---

# 🔒 Seguridad

El proyecto utiliza:

- JWT.
- Control de acceso por roles.
- Validación de datos.
- Protección de endpoints.
- Manejo centralizado de excepciones.
- Variables de entorno para secretos.

## Nunca subir

```text
API Keys
Passwords
Tokens
JWT Secrets
Credenciales PostgreSQL
Credenciales de usuarios
Secretos de servicios externos
```

---

# 🛡️ Seguridad de Git

Antes de realizar un commit:

```powershell
git status
```

Verificar que `.env` no aparezca como archivo pendiente.

Comprobar que `.env` esté siendo ignorado:

```powershell
git check-ignore -v .env
```

Revisar si `.env` aparece en el historial:

```powershell
git log --all --oneline -- .env
```

Buscar posibles API keys:

```powershell
git grep -n -i "api_key" $(git rev-list --all)
```

Buscar patrones de Postman:

```powershell
git grep -n -i "PMAK-" $(git rev-list --all)
```

Si una credencial fue expuesta:

1. Revocarla.
2. Generar una nueva.
3. Eliminar el secreto de los archivos versionados.
4. Revisar el historial.
5. Configurar la nueva credencial localmente.
6. Verificar nuevamente `.gitignore`.

---

# 📁 Estructura principal

```text
Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales/
│
├── backend/
│   └── umss-market-api/
│       ├── src/
│       │   ├── main/
│       │   └── test/
│       ├── pom.xml
│       ├── mvnw
│       └── mvnw.cmd
│
├── ai-testing-agent/
│   ├── src/
│   │   ├── agents/
│   │   ├── analyzers/
│   │   ├── config/
│   │   ├── generators/
│   │   ├── reporters/
│   │   ├── runners/
│   │   ├── services/
│   │   └── skills/
│   ├── collections/
│   ├── docs/
│   ├── generated-tests/
│   ├── reports/
│   ├── test-results/
│   ├── package.json
│   ├── package-lock.json
│   ├── playwright.config.js
│   ├── .env.example
│   └── .gitignore
│
├── README.md
└── .gitignore
```

Los directorios generados como `target/`, `node_modules/`, `test-results/` y otros archivos temporales deben permanecer ignorados según el `.gitignore`.

---

# 📌 Alcance de esta entrega

## Implementado y demostrable

- Registro e inicio de sesión.
- Gestión de usuarios.
- Gestión de roles.
- Gestión de tiendas.
- Gestión de publicaciones.
- Catálogo.
- Interacciones.
- Tool Calling.
- Búsqueda semántica.
- RAG #1 — Catálogo.
- RAG #2 — Detalle de publicación.
- RAG #3 — Tiendas y emprendedores.
- RAG #4 — Interacciones.
- RAG #5 — Recomendaciones.
- Embeddings con `nomic-embed-text`.
- Integración con Ollama.
- AI Testing Agent.
- MCP Postman Agent.
- AI Playwright Testing Agent.
- AI Test Analyzer.

## Fuera del alcance demostrado

No forman parte de la funcionalidad principal demostrada en esta entrega:

- Pasarela de pago en línea completa.
- Seguimiento logístico de entregas.
- Sistema completo de calificaciones y reseñas.
- Chat comprador-emprendedor.
- Analytics avanzados.

Estas funcionalidades pueden ser consideradas para futuras iteraciones.

---

# 🚀 Mejoras futuras

Entre las posibles mejoras se encuentran:

- Analytics avanzados.
- Recomendaciones más personalizadas.
- Chat inteligente.
- Predicción de demanda.
- Aplicación móvil.
- Dashboards analíticos.
- Mayor automatización.
- Escalamiento de las capacidades de IA.

---

# 👨‍💻 Equipo

**Grupo:** E-Commerce UMSS

**Integrantes:**

- Abad Melani Rodriguez Gonzales
- Christian Bernardo Vargas Sandoval

---

# 📊 Estado del proyecto

La solución integra el marketplace y las capacidades de Inteligencia Artificial de manera progresiva.

```text
┌─────────────────────────┐
│      UMSS MARKET        │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│      TOOL CALLING       │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│        RAG #1            │
│        Catálogo          │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│        RAG #2            │
│  Detalle publicación     │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│        RAG #3            │
│ Tiendas / emprendedores  │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│        RAG #4            │
│     Interacciones        │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│        RAG #5            │
│    Recomendaciones       │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│   AI TESTING AGENT      │
└────────────┬────────────┘
             ↓
      ┌──────┼──────┐
      ↓      ↓      ↓
   Postman Playwright Analyzer
      │      │      │
      └──────┼──────┘
             ↓
       Resultados + IA
```

---

# ✅ Checklist de entrega

> Este checklist debe reflejar el estado real del repositorio. No marcar un punto únicamente porque esté escrito en el README.

- [x] El docente tiene acceso al repositorio.
- [x] Existe un `README.md`.
- [x] El README explica cómo levantar el proyecto.
- [x] Existe un `.env.example`.
- [x] `.env` está incluido en `.gitignore`.
- [x] No existen API keys reales en el código versionado.
- [x] No existen contraseñas reales en el repositorio.
- [x] No existen tokens o secretos JWT en Git.
- [x] Se revisó el historial de Git.
- [x] Las claves comprometidas fueron revocadas y reemplazadas cuando correspondía.
- [x] El backend puede levantarse correctamente.
- [x] PostgreSQL está documentado y configurado.
- [x] Los cinco RAG están documentados y cuentan con pruebas.
- [x] RAG #1 — Búsqueda semántica del catálogo.
- [x] RAG #2 — Detalle de publicación.
- [x] RAG #3 — Tiendas y emprendedores.
- [x] RAG #4 — Interacciones del usuario.
- [x] RAG #5 — Recomendaciones.
- [x] El AI Testing Agent está documentado.
- [x] El MCP Postman Agent puede ejecutarse correctamente.
- [x] El AI Playwright Testing Agent puede ejecutarse correctamente.
- [x] El AI Test Analyzer puede analizar los resultados.
- [x] Los reportes de testing están documentados.
- [x] Las pruebas principales están documentadas.
- [x] Las versiones están alineadas con los archivos reales del proyecto.
- [x] Los comandos principales de instalación y ejecución fueron probados.
- [x] No existen credenciales sensibles en README, capturas, logs ni archivos versionados.

---

# 🔐 Regla final

```text
.env.example → SÍ se sube
.env         → NO se sube

API Keys     → NUNCA
Passwords    → NUNCA
Tokens       → NUNCA
JWT Secrets  → NUNCA
```

El repositorio debe poder ser clonado y configurado por otro integrante o por el docente sin necesidad de acceder a credenciales privadas.

## Desarrollo reproducible y rendimiento (Módulo 4)

- Stack local con Docker multi-stage, PostgreSQL persistente y healthchecks: [`compose.yaml`](compose.yaml) y [`docs/feature-1-docker-stack.md`](docs/feature-1-docker-stack.md).
- Prueba de catálogo con k6, tres corridas y mediana p95/errores: [`bench/README.md`](bench/README.md) y [`docs/feature-2-k6-performance.md`](docs/feature-2-k6-performance.md).

Para iniciar el stack: `docker compose up -d --wait`. Para ejecutar la prueba de rendimiento en Windows: `bench\run-three.ps1`.
