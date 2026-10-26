# Demo 2 — UMSS Market — release/8.2.0

**Fecha:** 26 de octubre de 2026. **Estado:** verificadas suite Java local y consulta PostgreSQL; pendientes datos para segunda pagina, contrato regenerado tras correccion, PR/CI y entrega.

| Integrante | Nueva funcionalidad demo 2 | Caso de uso FSD | Criterios de aceptacion | Evidencia individual |
| --- | --- | --- | --- | --- |
| Abad Melani Rodriguez Gonzales | Alertas de stock bajo por tienda: listado, resumen, aislamiento y paginacion (nueva) | [UC-008](../fsd/UC-008-alertas-inventario.md), extension de UC-001 | CA-01 a CA-09; pruebas `InventoryAlertsIntegrationTest`; OpenAPI + Postman; EXPLAIN | PR de implementacion: **agregar enlace despues de abrir PR** |
| Christian Bernardo Vargas Sandoval | **Confirmar nueva funcionalidad propia con Christian**, no repetir issue #6 como nuevo sin cambios | Registrar UC y CA nuevos | Demostracion funcional y pruebas individuales | PR de Christian: **pendiente** |

## Mejoras / pruebas recuperadas del lunes anterior (NO nuevas funcionalidades)

- **Issue #2:** reserva atomica de inventario. Demostrar con `JpaPublicationAtomicReservationTest` y Postman. [PR #8](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/8).
- **Issue #3:** Transactional Outbox. `TransactionalOutboxTest`: el registro compartido el 26/10/2026 mostro 6 pruebas aprobadas; los logs `OUTBOXFAILED` son fallos simulados por `FlakyConsumer`. [PR #9](https://github.com/christianvargasmpsia-cmd/UMSS-Market/pull/9).
- Estos issues son evidencia complementaria. La nueva funcionalidad evaluable es UC-008, sujeta a la consigna y a su funcionamiento comprobado.

## Checklist obligatorio
- [ ] Rama de presentacion `release/8.2.0` integra los cambios y no rompe funcionalidades existentes.
- [x] Pruebas automatizadas de UC-008 + issues #2, #3 con resultado `BUILD SUCCESS` (diagnostico local 26/10: 239 tests, 0 fallos, 0 errores).
- [ ] `openapi.json` **generado desde Springdoc** y commit en la raiz; **no copiar un ejemplo a mano**.
- [ ] `.github/workflows/openapi-contract.yml` aprobado por GitHub Actions: drift + Spectral.
- [ ] Endpoints REST presentados incluidos en OpenAPI; GraphQL mantiene SDL independiente.
- [ ] Postman: login JWT real, stock bajo, resumen, filtros, errores 400 y 403.
- [x] PostgreSQL: `idx_publications_inventory_alerts` confirmado; EXPLAIN 120 filas, Seq Scan, ejecucion 0.052 ms. No atribuir esa diferencia al indice.
- [ ] Agregar datos de demostracion reejecutables y evidenciar dos paginas distintas (`docs/sql/demo2_inventory_seed.sql`).
- [x] Outbox local del pedido `52eaa860-6e57-48b8-8bba-adc5af9618e1`: evento `ORDER_CREATED`, `PROCESSED`, 0 reintentos; corroborar en captura.
- [ ] Evidencia de PR de cada integrante y tabla final copiada en Classroom.
- [ ] Revisar que el docente acepte el alcance de funcionalidad nueva / release 8.2.0.

## Comandos de validacion

Desde la raiz en Windows (Java 21, Maven wrapper, Node.js 22, PostgreSQL):

```powershell
.\scripts\demo2-verificar.ps1
.\scripts\demo2-bootstrap-openapi.ps1
git add openapi.json
```

Las pruebas Java del CI usan H2, no sustituyen la inspeccion con PostgreSQL de prueba.
El contrato de Spring Boot requiere arrancar contexto de pruebas; si el CI falla se deben analizar logs antes de integrar.

## Como exponer la nueva funcionalidad

1. Postman: autenticarse y guardar token de emprendedor.
2. GET `.../low-stock?threshold=5&page=0&size=2`: mostrar pagina y total.
3. GET `.../low-stock?threshold=5&page=1&size=2`: mostrar siguiente pagina.
4. GET `.../summary?threshold=5`: mostrar conteos.
5. Cambiar tienda a una ajena y demostrar 403; enviar `threshold=-1` y mostrar 400.
6. Ejecutar JUnit y mostrar conteo de pruebas. Despues presentar EXPLAIN en PostgreSQL real.
7. GitHub Actions: mostrar CI verificando que el archivo versionado coincida con el codigo y respete Spectral.

### Nota sobre diapositivas del docente
La clase M8 S05 (p. 4 y 38) anuncia pregunta fija de paginacion e indices con EXPLAIN en Demo 2. El laboratorio de OpenAPI en CI (p. 34) se marca demostrativo, pero el aviso de Classroom compartido requiere contrato y drift + Spectral. Se atienden ambos.

## Prueba de carga opcional (k6)

Con `k6` instalado, variables de entorno `STORE_ID` y `JWT` cargadas con datos reales de la base de PRUEBAS:

```powershell
$env:BASE_URL = 'http://localhost:8080'
$env:STORE_ID = 'UUID_TIENDA_DE_PRUEBAS'
$env:JWT = 'JWT_DE_EMPRENDEDOR'
k6 run .\scripts\k6-inventory-read.js
```

Recoger p50 (mediana), p95, p99 y tasa de fallos, el comando y el equipo usado. No atribuir umbrales alcanzados sin el informe. El escenario es pequeno (2 VUs / 15 s) y NO sustituye EXPLAIN ni tests de concurrencia.
