-- PostgreSQL de PRUEBAS: indice parcial del caso UC-008.
-- Es el mismo indice ya creado en pgAdmin: NO crear uno duplicado con otro nombre.
CREATE INDEX IF NOT EXISTS idx_publications_inventory_alerts
    ON public.publications (store_id, stock, created_at DESC, id DESC)
    WHERE activa = TRUE AND tipo = 'PRODUCTO';

ANALYZE public.publications;

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'publications'
ORDER BY indexname;

-- Analizar el filtro y paginacion reales.
-- Con ~120 registros el optimizador puede preferir Seq Scan; eso NO invalida el indice.
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, nombre, stock
FROM public.publications
WHERE store_id = '1da08089-9f73-44c0-af59-4d0b76ccfcb0'
  AND activa = TRUE AND tipo = 'PRODUCTO'
  AND stock IS NOT NULL AND stock >= 0 AND stock <= 5
ORDER BY created_at DESC, id DESC
LIMIT 2 OFFSET 0;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, nombre, stock
FROM public.publications
WHERE store_id = '1da08089-9f73-44c0-af59-4d0b76ccfcb0'
  AND activa = TRUE AND tipo = 'PRODUCTO'
  AND stock IS NOT NULL AND stock >= 0 AND stock <= 5
ORDER BY created_at DESC, id DESC
LIMIT 2 OFFSET 2;

-- Aislar prueba de elegibilidad del indice; NO dejar desactivado Seq Scan permanentemente.
BEGIN;
SET LOCAL enable_seqscan = off;
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, nombre, stock
FROM public.publications
WHERE store_id = '1da08089-9f73-44c0-af59-4d0b76ccfcb0'
  AND activa = TRUE AND tipo = 'PRODUCTO'
  AND stock IS NOT NULL AND stock >= 0 AND stock <= 5
ORDER BY created_at DESC, id DESC LIMIT 2 OFFSET 0;
ROLLBACK;
