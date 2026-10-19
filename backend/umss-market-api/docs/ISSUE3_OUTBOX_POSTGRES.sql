-- UMSS Market - Issue #3 (PostgreSQL 18): Transactional Outbox.
-- Ejecutar en una base con respaldo; idempotente para tablas e indice.
CREATE TABLE IF NOT EXISTS public.outbox_events (
    id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    event_type varchar(64) NOT NULL,
    payload text NOT NULL,
    status varchar(20) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    available_at timestamp NOT NULL,
    created_at timestamp NOT NULL,
    processed_at timestamp,
    last_error varchar(500),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PROCESSING','RETRY','PROCESSED','FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_outbox_due
    ON public.outbox_events (status, available_at, created_at);
CREATE TABLE IF NOT EXISTS public.consumed_order_events (
    event_id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    event_type varchar(64) NOT NULL,
    consumed_at timestamp NOT NULL
);
-- Diagnostico:
-- SELECT status, count(*) FROM public.outbox_events GROUP BY status;
-- SELECT count(*) FROM public.consumed_order_events;
