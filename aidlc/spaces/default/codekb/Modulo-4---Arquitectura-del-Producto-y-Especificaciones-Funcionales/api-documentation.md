# API de salud

## GET /api/health

Sin parámetros ni cuerpo. SecurityConfig permite acceso mediante anyRequest().permitAll(); el filtro JWT compartido no se analizó profundamente. HealthIntegrationTest expresa la expectativa de acceso anónimo.

| Resultado | HTTP | JSON |
|---|---|---|
| SELECT 1 devuelve 1 | 200 | {"application":"UP","database":"UP"} |
| DataAccessException o resultado distinto | 503 | {"application":"UP","database":"DOWN"} |

Cabecera Cache-Control: no-store. No expone mensajes SQL ni datos de conexión. No se encontró configuración específica de rate limiting en los archivos analizados; no se evaluaron controles externos.

## API interna

HealthDatabaseProbe.isUp(): boolean, consulta SQL de solo lectura mediante DataSource inyectado. Timeout de statement de 5 segundos, no límite total de respuesta. Las demás APIs del marketplace quedan fuera de este análisis parcial.
