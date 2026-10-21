# Comprobaciones de seguridad del feature

## Casos existentes

HealthControllerTest compara estrictamente JSON para ambos estados y no-store; HealthIntegrationTest confirma acceso público esperado sin JWT. Ambos forman parte del comando único de build-instructions.md. Sin entradas de usuario en SELECT 1 y sin detalles internos de conexión en respuesta.

## Límites

No hay cambios de autenticación ni secretos. No se agregan SAST/DAST o análisis de vulnerabilidades como objetivos nuevos. Bcrypt preexistente queda fuera de este feature. No se declara una auditoría integral de seguridad.

