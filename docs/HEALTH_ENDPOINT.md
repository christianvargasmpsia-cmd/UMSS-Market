# GET /api/health

Endpoint publico de diagnostico del backend. En cada solicitud ejecuta `SELECT 1`
contra el `DataSource` configurado, sin consultar ni modificar datos del negocio.

| Resultado | HTTP | JSON |
|---|---|---|
| Consulta devuelve 1 | 200 | `{"application":"UP","database":"UP"}` |
| Falla de conexion, consulta o resultado inesperado | 503 | `{"application":"UP","database":"DOWN"}` |

`application: UP` indica que el backend puede atender la solicitud; no implica
que todas sus dependencias esten disponibles. Si el proceso esta detenido no
puede emitir esta respuesta. La comprobacion cubre conectividad SQL, no la
integridad del esquema ni otros servicios externos.

La respuesta usa `Cache-Control: no-store` y no revela excepciones, credenciales
ni detalles de infraestructura. La consulta tiene un timeout JDBC de 5 segundos;
el tiempo de espera para obtener una conexion depende de la configuracion del
pool existente. No se modifican los timeouts de consultas del negocio.

## Pruebas

Desde `backend/umss-market-api`:

```powershell
./mvnw.cmd '-Dtest=HealthControllerTest,HealthDatabaseProbeTest,HealthIntegrationTest' test jacoco:report
```

Las pruebas verifican ambos estados HTTP/JSON, fallo al obtener conexion, timeout
SQL, resultado inesperado, exito JDBC con cierre de recursos y recuperacion de
la misma sonda despues de una conexion fallida (ocho casos en total). La integracion ejecuta el
endpoint sin autenticacion, con los filtros reales y la base H2 de pruebas.
El reporte JaCoCo se genera en `target/site/jacoco/index.html`.
