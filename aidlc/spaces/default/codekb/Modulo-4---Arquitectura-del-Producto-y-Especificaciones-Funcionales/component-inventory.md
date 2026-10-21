# Inventario del alcance analizado

## HealthController

Adaptador GET /api/health; depende de HealthDatabaseProbe y Spring MVC. Traduce boolean a HTTP/JSON y establece no-store.

## HealthDatabaseProbe

Componente Spring que construye JdbcTemplate con DataSource; SELECT 1, timeout 5 segundos y manejo de DataAccessException.

## SecurityConfig

Configuración compartida de Spring Security, CORS, filtro JWT y encoder. Permite la ruta de salud por la regla general. Se inspeccionó sin modificarla.

## HealthTests

HealthControllerTest: dos casos HTTP con mock. HealthDatabaseProbeTest: tres casos JDBC de fallo, timeout y resultado inesperado con cierre de recursos. HealthIntegrationTest: un caso de aplicación completa con solicitud anónima; la configuración de H2 se debe confirmar al ejecutar pruebas.

## MavenBuild

pom.xml: Java, dependencias, compilación, cobertura y empaquetado. Versiones en technology-stack.md; detalle de relaciones en dependencies.md.
