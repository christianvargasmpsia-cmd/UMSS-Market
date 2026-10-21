# Requisitos: salud del backend

## Sources

- [desc] Initial description: Implementar un endpoint GET /api/health que verifique que el backend está funcionando y que existe conexión con la base de datos. Debe realizar una comprobación real de conectividad con la base de datos devolver el estado correspondiente y contar con pruebas automatizadas.
- [scope] Workflow-selected scope: express, profundidad Minimal y estrategia de pruebas Minimal.
- Solicitud anterior de esta conversación: no modificar funcionalidades no relacionadas con el feature.
- AGENTS.md: pruebas unitarias e integración, cobertura por feature de al menos 90% de líneas y 80% de ramas; secretos fuera del código.
- Análisis aprobado: ../../../../codekb/Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales/business-overview.md (finalidad operativa), architecture.md (flujo HTTP/JDBC) y code-structure.md (módulo y componentes existentes).
- requirements-analysis-questions.md: suficiencia de contexto y ausencia de preguntas pendientes.

## Objetivo y alcance

Permitir que una persona o comprobador HTTP determine si el backend responde y si puede ejecutar una consulta en su base de datos configurada. El feature ya tiene implementación y pruebas; se verifica y completa únicamente si se encuentran brechas respecto de estos requisitos. Todos los requisitos siguientes son Must.

## Requisitos funcionales

| ID | Requisito | Criterio de aceptación | Fuente |
|---|---|---|---|
| FR1 | Exponer GET /api/health sin parámetros obligatorios, accesible sin autenticación. | Dado el backend iniciado, cuando se solicita la ruta sin JWT, se obtiene JSON de salud y no un rechazo de autenticación. | [desc], comportamiento existente en análisis aprobado |
| FR2 | Ejecutar SELECT 1 mediante el DataSource real configurado en cada solicitud. | Dada una BD accesible, cuando se consulta salud, se ejecuta SQL de solo lectura y se valida que devuelve 1; no se sustituye por una constante o caché. | [desc], architecture.md |
| FR3 | Informar los estados de aplicación y BD con el contrato existente. | Dado SELECT 1 exitoso, cuando responde el endpoint, devuelve HTTP 200 y exactamente {"application":"UP","database":"UP"}. | [desc], contrato existente |
| FR4 | Manejar fallo de conexión, fallo/timeout SQL y resultado inesperado. | Dado cualquiera de esos fallos durante la comprobación, cuando responde el endpoint, devuelve HTTP 503 y exactamente {"application":"UP","database":"DOWN"}, sin excepción interna en el cuerpo. | [desc], contrato existente |
| FR5 | Permitir comprobaciones posteriores independientes. | Después de un fallo, una nueva consulta con BD accesible debe poder devolver UP; las conexiones y recursos adquiridos se liberan al terminar, también en errores. | Comprobación real por solicitud y confiabilidad de FR2/FR4 |

## Requisitos no funcionales

| ID | Requisito y verificación | Fuente |
|---|---|---|
| NFR1 | Respuesta mínima sin credenciales, URL JDBC, stack traces ni mensajes SQL internos; las pruebas comparan el cuerpo completo. | AGENTS.md y contrato existente |
| NFR2 | Cache-Control: no-store tanto en éxito como fallo. Verificar cabecera HTTP. | architecture.md, comportamiento existente |
| NFR3 | Conservar timeout JDBC de consulta de 5 segundos, local a la sonda. Verificar configuración y manejo de SQLTimeoutException. No representa un límite total de respuesta: adquisición de conexión depende del pool existente. | architecture.md |
| NFR4 | Pruebas automatizadas unitarias e integración, con ejecución real de SELECT 1 en H2 aislada y casos de error deterministas. JaCoCo debe demostrar al menos 90% líneas y 80% ramas sobre clases del feature; no basta cobertura global. Registrar comandos y resultados actuales. | [desc], AGENTS.md |
| NFR5 | No introducir regresiones ni modificar funcionalidades ajenas. Ejecutar la suite existente en la validación; registrar fallos preexistentes separadamente y resolver cualquier fallo introducido por el feature. | Restricción del usuario, reglas express |

## Restricciones y límites

- Trabajar en backend/umss-market-api con Java 21 y Spring Boot existentes; conservar HealthController y HealthDatabaseProbe si satisfacen los requisitos.
- No agregar tablas, migraciones ni escrituras de datos comerciales. No alterar seguridad compartida para habilitar una ruta que ya es accesible.
- application=UP significa que el proceso pudo atender la petición. Un proceso detenido no puede producir JSON; una aplicación que no arranca por falta de BD tampoco puede responder a este endpoint.
- No modificar pagos, inventario, catálogo, autenticación, recomendaciones ni configuración AWS. No realizar push a main/release ni operaciones sobre producción.
- H2 verifica integración SQL local; no equivale a una prueba contra un servidor PostgreSQL externo.

## Trazabilidad de validación prevista

| Requisitos | Componente | Verificación |
|---|---|---|
| FR1, FR2, FR3 | HealthController, HealthDatabaseProbe | HealthIntegrationTest: solicitud anónima y SQL real H2 |
| FR3, FR4, NFR1, NFR2 | HealthController | HealthControllerTest: código HTTP, cuerpo completo y no-store |
| FR4, FR5, NFR3 | HealthDatabaseProbe | HealthDatabaseProbeTest: fallo de conexión, timeout, resultado inesperado, cierre de recursos y recuperación si falta cobertura |
| NFR4 | Clases del feature | Maven y reporte JaCoCo por clase |
| NFR5 | Backend existente | Suite de regresión y revisión del diff |

## Fuera de alcance

Dashboard, frontend, alertas, métricas de negocio, comprobación de Ollama/Redis/RabbitMQ, garantía de disponibilidad del servicio, SLA de latencia total, validación del esquema, remediación de bcrypt preexistente y despliegue en producción. La documentación operativa puede explicar cómo consumir el endpoint sin modificar infraestructura externa.

## Assumptions & Open Questions

None.

## Estado de evidencia

Las seis pruebas exitosas y la cobertura completa comunicadas anteriormente son evidencia histórica. Esta etapa no ejecuta pruebas ni presenta esos resultados como una nueva validación. La etapa Build and Test debe registrar resultados actuales.
