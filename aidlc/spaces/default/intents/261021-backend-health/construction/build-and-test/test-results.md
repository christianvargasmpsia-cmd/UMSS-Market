# Resultados de compilación y pruebas

## Ejecución actual

Fecha local: 2026-10-21. Compilación, pruebas filtradas y empaquetado JAR Spring Boot: BUILD SUCCESS, 45.982 s. 8 pruebas, 0 fallos, 0 errores, 0 omitidas. Archivo target/umss-market-api-0.0.1-SNAPSHOT.jar creado. Se ejecutó el comando de build-instructions.md desde backend/umss-market-api con dependencias locales y autorización fuera del sandbox para acceso a JAR.

Reporte JaCoCo de esta etapa: target/health-build-test.exec, append=false; log confirma lectura de ese archivo. HealthController 7/7 líneas y 4/4 ramas; HealthDatabaseProbe 7/7 líneas y sin ramas contabilizadas. Total 14/14 líneas y 4/4 ramas. Comando package no ejecuta el gate agregado de verify; se comprueban los mínimos del feature directamente en el reporte sin alterar configuración.

## Regresión y limitación

Code Generation ejecutó baseline completo: 238 pruebas, 9 errores preexistentes de Ollama. Después: conjunto comparable 219/219 verde (217 previas y 2 nuevas). Se excluyeron 21 pruebas en seis clases con proveedor real: OllamaAdapterToolSelectionTest, OllamaRecommendationLiveTest, ReviewSummaryLiveTest, SearchCatalogSemanticTest, OllamaAdapterEmbeddingTest y DuplicateEmbeddingLiveTest. Detalles y comandos en ../code-generation/code-summary.md.

Build and Test verifica los logs existentes de regresión y vuelve a ejecutar una sola vez la compilación, empaquetado y ocho casos de salud. No duplica la integración H2 ya incluida en ese filtro. No se repiten pruebas verdes sin motivo. La regresión completa posterior sigue Unverified, por lo que esta etapa no se presenta como exitosa en conjunto.

## Diagnóstico e impacto

La limitación corresponde a pruebas ajenas al feature que usan Ollama vivo. No se identifica un defecto de salud ni un cambio de biblioteca, algoritmo o configuración de salud que repare la disponibilidad del proveedor externo. No hay reparación de código justificada que devolver a Code Generation. Reparar o reconfigurar Ollama ampliaría el alcance y necesita diagnóstico específico; esfuerzo/costo no estimables con la evidencia actual. Aceptar la limitación no cambia ni reduce el objetivo: queda registrado Unverified. Cero loop-backs utilizados.

## Aceptación de la limitación

El 2026-10-21 el usuario respondió `accept failure`, registrado como `Accept failure`. Autoriza continuar a la aprobación de esta etapa conservando la limitación de NFR5. No implica que las pruebas externas hayan pasado ni cambia el veredicto Unverified.

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


