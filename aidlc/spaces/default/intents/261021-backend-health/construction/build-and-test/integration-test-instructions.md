# Integración de salud

## Ejecución

HealthIntegrationTest ya forma parte del comando único de build-instructions.md. Ejecuta GET /api/health sin JWT con filtros reales y H2, esperando 200 y JSON UP/UP. SQL SELECT 1 real, sin alterar datos de negocio. No duplicar el comando de pruebas.

## Evidencia y alcance

Prueba aprobada en esta etapa. H2 confirma integración JDBC local, no disponibilidad de PostgreSQL externo. Las pruebas de errores usan dobles JDBC controlados.

