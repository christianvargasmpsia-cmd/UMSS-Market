# Verificación y pruebas de GET /api/health

Implementación existente conservada: HealthController responde 200 con application=UP y database=UP, o 503 con database=DOWN; ambos cuerpos son mínimos y llevan Cache-Control: no-store. HealthDatabaseProbe ejecuta SELECT 1 en cada comprobación con JdbcTemplate, timeout local de consulta de 5 segundos y liberación de recursos. No se demostró defecto productivo.

## Cambios y alcance

- HealthDatabaseProbeTest.java: dos pruebas nuevas para éxito JDBC y cierre de recursos, y recuperación de la misma sonda tras una conexión fallida. Cinco casos JDBC, dos HTTP y una integración H2: ocho en total.
- docs/HEALTH_ENDPOINT.md: inventario actualizado.
- Plan: solo casillas completadas; este resumen y traceability.json registran evidencia.

Impacto bajo: solo pruebas y documentación. Sin cambios productivos, dependencias, seguridad, esquema ni arquitectura. No se ejecutaron migraciones, commits ni despliegue.

## Entorno y comandos efectivos

Java: C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot (21.0.12). El Java predeterminado del shell es 8 y no sirve para este proyecto. Maven 3.9.16 local sustituye al wrapper según el plan. Las pruebas H2 requirieron ejecución autorizada fuera del sandbox que impedía abrir JAR; no se cambió código para eludir ese fallo.

Desde backend/umss-market-api, en PowerShell:

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot'
& 'C:/Users/Edwing/.m2/wrapper/dists/apache-maven-3.9.16/0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0/bin/mvn.cmd' '-Dmaven.repo.local=C:/Users/Edwing/.m2/repository' '-Dtest=HealthControllerTest,HealthDatabaseProbeTest,HealthIntegrationTest' '-Djacoco.destFile=target/health-feature.exec' '-Djacoco.dataFile=target/health-feature.exec' '-Djacoco.append=false' '-Djacoco.outputDirectory=target/site/jacoco-health' test jacoco:report -l target/health-feature.log
```

Resultado actual: 8 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS, 48.580 s. Ejecución dedicada target/health-feature.exec con append=false. El plugin ignoró la propiedad outputDirectory: el informe efectivo es target/site/jacoco/index.html y jacoco.csv, generado desde ese archivo dedicado (confirmado por el log), no desde datos acumulados.

| Clase | Líneas cubiertas/total | Ramas cubiertas/total |
|---|---|---|
| HealthController | 7/7 (100%) | 4/4 (100%) |
| HealthDatabaseProbe | 7/7 (100%) | 0/0 (sin ramas contabilizadas por JaCoCo) |
| Feature | 14/14 (100%) | 4/4 (100%) |

Supera 90% líneas y 80% ramas. No se agregaron exclusiones de cobertura ni se alteraron umbrales.

## Baseline y regresión

Baseline completo previo conservado en target/health-baseline.log: 238 pruebas, 0 fallos de aserción, 9 errores, 0 omitidas. Errores externos: OllamaAdapterToolSelectionTest (7 de 15), OllamaRecommendationLiveTest (1 de 1), ReviewSummaryLiveTest (1 de 2), por espera agotada de Ollama vivo. No se atribuyen a salud ni se declara verde la suite completa.

Para comparación repetible posterior se excluyeron exactamente seis clases que usan Ollama real: esas tres clases (18 pruebas) y SearchCatalogSemanticTest, OllamaAdapterEmbeddingTest, DuplicateEmbeddingLiveTest (una prueba cada una). Las llamadas reales están comprobadas en sus fuentes; estas últimas tres pasaron en baseline, pero dependen del proveedor externo. Total excluido: 21 pruebas; baseline comparable: 217 pruebas aprobadas.

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot'
& 'C:/Users/Edwing/.m2/wrapper/dists/apache-maven-3.9.16/0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0/bin/mvn.cmd' '-Dmaven.repo.local=C:/Users/Edwing/.m2/repository' '-Dtest=*,!OllamaAdapterToolSelectionTest,!OllamaRecommendationLiveTest,!ReviewSummaryLiveTest,!SearchCatalogSemanticTest,!OllamaAdapterEmbeddingTest,!DuplicateEmbeddingLiveTest' test -l target/health-regression.log
```

Resultado posterior: 219 pruebas aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS, 01:15 min. Son las 217 comparables más las dos nuevas. No hay regresiones observadas en ese conjunto. La suite completa posterior con Ollama vivo no se repitió; esta es una limitación explícita de validación, sin cambios a esas pruebas o funciones.

H2 aislada demuestra SQL real e integración anónima local; no demuestra disponibilidad de PostgreSQL externo. El timeout de consulta no limita el tiempo de adquisición del pool. La suite de regresión no regenera el informe dedicado de cobertura del feature.
