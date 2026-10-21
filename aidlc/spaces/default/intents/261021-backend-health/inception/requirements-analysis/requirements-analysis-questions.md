# Aclaraciones de requisitos

## Fuentes y suficiencia

La solicitud explícita, la restricción de no modificar funcionalidades ajenas y el análisis de código aprobado permiten concretar este feature sin preguntas adicionales. No se registran respuestas inferidas como decisiones del usuario.

| Dimensión | Evidencia y resolución |
|---|---|
| Funcional | GET /api/health, estado del backend y comprobación real de BD solicitados explícitamente. |
| No funcional | Preservar JSON mínimo, no-store y timeout SQL existentes; cobertura del repositorio 90% líneas/80% ramas. |
| Escenarios | BD disponible, conexión fallida, timeout y resultado inesperado; identificados en pruebas existentes. |
| Negocio | Diagnóstico operativo del backend de UMSS Market, sin modificar operaciones comerciales. |
| Técnico | Módulo Java 21/Spring Boot existente, DataSource y JdbcTemplate; no migración de stack. |
| Calidad | Pruebas unitarias e integración con H2, medición JaCoCo del feature y comprobación de regresiones. |

## Decisiones ya observadas

El contrato existente devuelve 200 con application=UP/database=UP y 503 con application=UP/database=DOWN. Se conserva como comportamiento de compatibilidad, sujeto a aprobación de requirements.md; no se atribuye al usuario una elección previa de nombres de campos.

No se fija un SLA total ni se configura un entorno de producción. El timeout de consulta de 5 segundos no cubre adquisición de conexión. No se vuelve a preguntar por información ya presente en el análisis aprobado.

## Preguntas pendientes

None.
