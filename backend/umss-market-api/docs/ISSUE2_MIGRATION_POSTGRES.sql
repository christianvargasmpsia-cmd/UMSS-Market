-- SOLO PostgreSQL, entornos con registros previos: ejecutar previa copia de seguridad.
-- Evita que la cancelacion de pedidos legados devuelva stock que nunca fue descontado.
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS stock_reserved boolean NOT NULL DEFAULT false;

-- Nota: no marque pedidos anteriores como reservados sin una conciliacion real.
