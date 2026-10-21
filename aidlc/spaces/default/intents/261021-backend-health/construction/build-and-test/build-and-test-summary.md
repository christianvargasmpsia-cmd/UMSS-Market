# Compilación y pruebas: resultado parcial

## Resumen

JAR Spring Boot generado, 8/8 pruebas de salud y cobertura del feature 100% líneas/ramas. Regresión comparable anterior 219/219 verde. Documentos de instrucciones acotados a los casos existentes; sin nuevos tests ni cambios productivos en esta etapa.

## Estado de preparación

Build-ready y test-ready para salud local: comprobados. Deployment-ready global: no certificado, pues NFR5 sigue Unverified. No se despliega ni se da por verde la suite completa. Detalles y evidencias en test-results.md; entradas: code-generation-plan.md, unit-test-instructions.md y code-summary.md de la etapa previa.

El usuario aceptó continuar con esta limitación el 2026-10-21 mediante `Accept failure`. La etapa se presenta para aprobación con fallo aceptado, sin convertir NFR5 en Met.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| NFR1 | requirements.md / seguridad | JSON sin detalles internos | Comparación estricta de JSON exitosa en 2 estados | HealthControllerTest, health-build-test.log | Build and Test | Met |
| NFR2 | requirements.md / caché | no-store en ambos estados | Cabeceras verificadas en ambos estados | HealthControllerTest | Build and Test | Met |
| NFR3 | requirements.md / timeout | statement 5 s y manejo timeout | verify setQueryTimeout(5), SQLTimeoutException controlada | HealthDatabaseProbeTest | Build and Test | Met |
| NFR4-LINE | AGENTS.md, requirements.md NFR4 | ≥90% líneas del feature | 14/14, 100% | target/site/jacoco/jacoco.csv; health-build-test.exec | Build and Test | Met |
| NFR4-BRANCH | AGENTS.md, requirements.md NFR4 | ≥80% ramas del feature | 4/4, 100% | mismo informe JaCoCo dedicado | Build and Test | Met |
| NFR4-TEST | requirements.md NFR4, Testing Contract | pruebas por requisito y feliz por componente, integración real | 8/8 aprobadas; H2 real, errores simulados | health-build-test.log, pruebas | Build and Test | Met |
| NFR5 | requirements.md / regresión completa | suite existente sin regresiones | 219/219 comparables verdes; 21 pruebas externas no repetidas | health-regression.log, health-baseline.log | Build and Test | Unverified |

El contrato test-after se respetó: implementación existente conservada y pruebas completadas después. Su obligación de mantener verde la suite completa no se declara cumplida con las exclusiones locales. No hay umbrales reducidos ni exclusiones de cobertura añadidas.


