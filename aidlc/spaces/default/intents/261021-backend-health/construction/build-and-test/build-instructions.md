# Compilación del backend

## Prerrequisitos y fuentes

Java 21, Maven 3.9.16 y dependencias ya disponibles. Fuentes: ../code-generation/code-generation-plan.md, unit-test-instructions.md y code-summary.md. H2 de pruebas; ninguna base externa. No agregar dependencias ni editar configuración productiva. El wrapper puede fallar en este equipo; se usa instalación local documentada.

## Comando ejecutado

Desde backend/umss-market-api:

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot'
& 'C:/Users/Edwing/.m2/wrapper/dists/apache-maven-3.9.16/0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0/bin/mvn.cmd' '-Dmaven.repo.local=C:/Users/Edwing/.m2/repository' '-Dtest=HealthControllerTest,HealthDatabaseProbeTest,HealthIntegrationTest' '-Djacoco.destFile=target/health-build-test.exec' '-Djacoco.dataFile=target/health-build-test.exec' '-Djacoco.append=false' package jacoco:report -l target/health-build-test.log
```

Este comando único empaqueta y ejecuta tanto unitarias como integración del feature. No ejecutar otra vez la integración como comando duplicado. Verificar BUILD SUCCESS, ocho pruebas y JAR en target/. La suite de regresión y sus seis exclusiones se documentan en code-summary.md; evidencia anterior válida sin cambios de fuente posteriores.

## Problemas del entorno

JAVA_HOME debe apuntar a Java21; el Java predeterminado puede ser 8. Acceso denegado al cache Maven requiere autorización del entorno, no cambiar código ni umbrales. El build no usa secretos ni conecta a producción.

