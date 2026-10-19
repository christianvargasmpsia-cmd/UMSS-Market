# Issue #3 - Transactional Outbox (UMSS Market)

Base: `release/8.0.0` commit `77f6d18`. Rama de trabajo: `feature/3-outbox-eventos`.

## Alcance

- `CreateOrderUseCase`: `ORDER_CREATED` tras guardar pedido e items; transaccion Spring compartida.
- `UpdateOrderStatusUseCase`: `ORDER_STATUS_CHANGED` solo cuando el estado realmente cambia.
- `SimulatedCheckoutUseCase`: `ORDER_CHECKOUT_CONFIRMED` una vez por id de compra; no se duplica en reintentos del mismo request.
- `OutboxEventWriter.record()` con propagacion `MANDATORY`; el evento nunca se registra en una nueva transaccion desligada del pedido.
- `OutboxDispatcher` revisa eventos cada 5 s y delega a un `OutboxEventProcessor` transaccional por evento.
- `PESSIMISTIC_WRITE`: multiple workers no deben consumir dos veces un mismo evento. Reintentos con espera exponencial y hasta 5 intentos. Se conserva `FAILED` para inspeccion.
- `InboxOrderEventConsumer`: consumidor **interno** idempotente que guarda un comprobante por `eventId` en `consumed_order_events`. No envia correos, webhook, Kafka ni realiza pagos: esos destinos requeririan integrar su propio consumidor idempotente.
- `PROCESSING` solo existe durante la transaccion del procesador. Si el proceso cae antes del commit, PostgreSQL hace rollback y vuelve a `PENDING` / `RETRY`; no hay lock persistente atascado.
- Tres use cases conservan sobrecargas de constructor para los tests antiguos que se instancian con mocks. El constructor que Spring usa esta marcado `@Autowired` y exige `OutboxEventWriter`; en los tests antiguos con mocks el writer es `null`, y esos tests validan solamente negocio, no Outbox.
- Migracion independiente `docs/ISSUE3_OUTBOX_POSTGRES.sql`. No se modifica `application.properties` local.

## Aplicar en Windows PowerShell (en raiz repo)

```powershell
$zip = "$env:USERPROFILE\Downloads\UMSS_Issue3_Transactional_Outbox.zip"
$folder = "$env:TEMP\umss-issue3-outbox"
Expand-Archive -LiteralPath $zip -DestinationPath $folder -Force
$patch = Join-Path $folder 'UMSS_Issue3_Transactional_Outbox\ISSUE3_OUTBOX.patch'
git branch --show-current
git apply --check -- "$patch"
# Solo si --check no produjo errores:
git apply -- "$patch"
git status --short
```

**El parche no hace commits ni stages.** Los respaldos `_respaldo_*` y `application.properties` se mantienen ajenos.

## Verificar

```powershell
cd backend\umss-market-api
.\mvnw.cmd '-Dtest=TransactionalOutboxTest,SimulatedCheckoutTest,OrderReservationUseCaseTest' test
.\mvnw.cmd test
```

Todas las pruebas nuevas deben pasar y las anteriores continuar pasando. Si falla compilacion, enviar error completo sin forzar merge. Los tests Java utilizan H2; validar la migracion por separado en PostgreSQL (no se requiere ejecutar SQL manual de negocio para los tests).

Para deshabilitar temporalmente el worker puede usar `OUTBOX_PROCESSOR_ENABLED=false` (Spring Boot vincula esa variable con `outbox.processor.enabled`), o `--outbox.processor.enabled=false` al arrancar. Propiedades `outbox.poll-delay-ms` (5000) y `outbox.max-attempts` (5).

## Evidencia PostgreSQL

```sql
SELECT status, count(*) AS eventos FROM public.outbox_events GROUP BY status;
SELECT event_type, order_id, attempt_count, created_at, processed_at, last_error
  FROM public.outbox_events ORDER BY created_at DESC LIMIT 10;
SELECT event_id, order_id, event_type, consumed_at
  FROM public.consumed_order_events ORDER BY consumed_at DESC LIMIT 10;
```

## Limitaciones honestas

Este codigo garantiza registro en TX local y un consumidor interno idempotente **dentro de la misma base**. No asegura exactamente una entrega a un servicio remoto; integrar un consumidor externo exige clave de idempotencia propia y gestion at-least-once. No confundir tests en H2 con una prueba de dos workers sobre PostgreSQL real.
