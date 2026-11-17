PS D:\Chris\Maestria\Proyectos\Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales\backend\umss-market-api> docker exec umss-market-db-1 psql -U umss_market -d umss_market -c "SELECT id, order_id, event_type, status, attempt_count, last_error, created_at, processed_at FROM outbox_events ORDER BY created_at DESC LIMIT 10;"
                  id                  |               order_id               |        event_type        |  status   | attempt_count | last_error |        created_at         |        processed_at
--------------------------------------+--------------------------------------+--------------------------+-----------+---------------+------------+---------------------------+----------------------------
 a92895d4-b3e3-40cd-9678-1bf9a1e522e6 | a2c82f0e-9126-49bc-9e53-e2357ac23e63 | ORDER_CHECKOUT_CONFIRMED | PROCESSED |             0 |            | 2026-11-16 22:14:08.53971 | 2026-11-16 22:14:11.333895
(1 row)

PS D:\Chris\Maestria\Proyectos\Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales\backend\umss-market-api> docker logs --since 30m umss-market-api-1 2>&1 | Select-String -Pattern "outbox|email|mail|processed|order|SMTP" -CaseSensitive:$false

2026-11-16T22:14:11.334Z  INFO 1 --- [umss-market-api] [   scheduling-1] b.u.m.u.i.outbox.OutboxEventProcessor    :
OUTBOX PROCESSED eventId=a92895d4-b3e3-40cd-9678-1bf9a1e522e6 orderId=a2c82f0e-9126-49bc-9e53-e2357ac23e63 attempts=0


PS D:\Chris\Maestria\Proyectos\Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales\backend\umss-market-api> New-Item -ItemType Directory -Force -Path ".\evidencias\demo4"


    Directorio: D:\Chris\Maestria\Proyectos\Modulo-4---Arquitectura-del-Producto-y-Especificaciones-Funcionales\backend
    \umss-market-api\evidencias


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----        16/11/2026     18:17                demo4