# Trazabilidad final del alcance express

## Cobertura directa

No hay unidades ni historias generadas por diseño del alcance. Se revisó ../code-generation/traceability.json y se verificaron destinos existentes.

| ID | Estado | Implementación o evidencia |
|---|---|---|
| FR1 | OK | HealthIntegrationTest.java, acceso anónimo |
| FR2 | OK | HealthDatabaseProbe.java, SELECT 1 y prueba H2 |
| FR3 | OK | HealthController.java, 200/UP |
| FR4 | OK | HealthControllerTest.java y HealthDatabaseProbeTest.java, 503/errores |
| FR5 | OK | HealthDatabaseProbeTest.java, recuperación y cierre |
| NFR1 | OK | HealthControllerTest.java, JSON estricto |
| NFR2 | OK | HealthControllerTest.java, no-store |
| NFR3 | OK | HealthDatabaseProbe.java y prueba timeout |
| NFR4 | OK | code-summary.md y test-results.md, JaCoCo dedicado |
| NFR5 | Deferred / Unverified | code-summary.md y test-results.md, suite externa pendiente |

Los archivos Java residen bajo backend/umss-market-api/src/ en los paquetes infrastructure/controllers y infrastructure/health; los de pruebas en src/test/java. Todos los destinos OK de traceability.json existen.

## Veredicto

FAIL de cobertura total: 9/10 IDs tienen OK, NFR5 permanece sin cobertura OK completa. Se conserva esa brecha explícita; no se cambia Deferred a OK para hacer pasar la etapa.

