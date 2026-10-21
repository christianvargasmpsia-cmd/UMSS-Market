# Pruebas de salud

## Configuración

Desde backend/umss-market-api, usar Java 21, Maven y las dependencias ya declaradas: JUnit, Mockito, Spring Boot Test, H2 y JaCoCo. H2 aislada; no BD externa. No escribir secretos.

## Comando del feature

```powershell
./mvnw.cmd '-Dtest=HealthControllerTest,HealthDatabaseProbeTest,HealthIntegrationTest' test jacoco:report
```

Si el wrapper falla por el entorno, usar la instalación Maven 3.9.16 existente con -Dmaven.repo.local=C:/Users/Edwing/.m2/repository y el mismo filtro exacto. Documentar comando efectivo. Los fallos de permisos del sandbox deben resolverse con escalación autorizada, no cambiando código.

## Casos y cobertura

Conservar los seis casos existentes; añadir éxito JDBC unitario con cierre de recursos y recuperación de la misma sonda después de fallo de conexión, usando H2 o mocks JDBC controlados. Validar JSON completo y no-store en éxito y fallo, acceso anónimo, SELECT 1 real en integración, timeout y resultado inesperado. Verificar cobertura de las dos clases productivas: al menos 90% líneas y 80% ramas. No confundir reporte acumulado con ejecución actual; usar archivo de ejecución dedicado si es necesario.

