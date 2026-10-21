# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-21
- Commit: 0578a22f90fc8226073689a83034bfdf28f362c0
- Análisis inicial focalizado, profundidad mínima. Sin ejecución de pruebas ni modificaciones de aplicación.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: backend-health
fingerprint: d22e3b935d39588184ccdb1efe0f6265c45a9c53
analyzed:
  paths:
    - backend/umss-market-api/pom.xml
    - backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/controllers/HealthController.java
    - backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/health/HealthDatabaseProbe.java
    - backend/umss-market-api/src/main/java/bo/umss/market/umss_market_api/infrastructure/config/SecurityConfig.java
    - backend/umss-market-api/src/test/java/bo/umss/market/umss_market_api/infrastructure/controllers/HealthControllerTest.java
    - backend/umss-market-api/src/test/java/bo/umss/market/umss_market_api/infrastructure/health/HealthDatabaseProbeTest.java
    - backend/umss-market-api/src/test/java/bo/umss/market/umss_market_api/infrastructure/health/HealthIntegrationTest.java
  components:
    - HealthController
    - HealthDatabaseProbe
    - SecurityConfig
    - HealthTests
    - MavenBuild
shallow:
  paths:
    - backend/umss-market-api/src/
```
