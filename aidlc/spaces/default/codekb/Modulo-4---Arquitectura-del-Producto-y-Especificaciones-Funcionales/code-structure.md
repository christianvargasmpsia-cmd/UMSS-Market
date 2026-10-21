# Estructura del código

Raíz inspeccionada: backend/umss-market-api. El archivo pom.xml define el módulo Maven; src/main/java contiene implementación y src/test/java pruebas en paquetes espejo.

## Clasificación

Bajo bo/umss/market/umss_market_api/infrastructure:

- controllers/HealthController.java: adaptador HTTP.
- health/HealthDatabaseProbe.java: consulta de conectividad JDBC.
- config/SecurityConfig.java: configuración compartida de acceso HTTP, leída para determinar accesibilidad del endpoint.
- Pruebas espejo: HealthControllerTest, HealthDatabaseProbeTest, HealthIntegrationTest.

El inventario de src muestra domain/model, domain/ports, application/usecases, infrastructure/adapters y persistence; estas áreas se enumeraron, no se analizaron profundamente. No se inspeccionó frontend, infraestructura de despliegue ni código generado.
