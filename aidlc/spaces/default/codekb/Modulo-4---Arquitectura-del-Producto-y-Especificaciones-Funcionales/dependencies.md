# Dependencias

## Internas

HealthController → HealthDatabaseProbe → JdbcTemplate → DataSource. La sonda no usa entidades, repositorios comerciales ni casos de uso. Las pruebas dependen de estas dos clases; la integración carga el contexto Spring Boot y su seguridad compartida.

## Externas declaradas

pom.xml declara starters Spring Boot data-jpa, security, validation, web, graphql y test; spring-security-test, spring-graphql-test, PostgreSQL runtime, H2 test, JJWT, springdoc, Lombok y devtools opcional. JDBC llega a través de las dependencias existentes; no se añadió una dependencia para salud.

Compilación y empaquetado: maven-compiler-plugin y spring-boot-maven-plugin. Cobertura: JaCoCo. Versiones explícitas en technology-stack.md. No se evaluaron vulnerabilidades, versiones actuales externas ni dependencias transitivas resueltas.
