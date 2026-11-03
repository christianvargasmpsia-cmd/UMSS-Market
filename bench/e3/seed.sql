BEGIN;

INSERT INTO users (
    id, ru, nombre, email, password_hash,
    role, status, created_at
)
VALUES (
    '11111111-1111-4111-8111-111111111111',
    'E3-TEST-001',
    'Comprador E3',
    'comprador-e3@example.invalid',
    'solo-datos-experimentales',
    'COMPRADOR',
    'ACTIVE',
    TIMESTAMP '2026-11-01 12:00:00'
);

INSERT INTO publications (
    id, store_id, nombre, descripcion,
    precio, tipo, stock, modalidad_cobro,
    activa, created_at
)
VALUES (
    '22222222-2222-4222-8222-222222222222',
    '44444444-4444-4444-8444-444444444444',
    'Producto de prueba E3',
    'Publicacion fija para experimento de arquitectura',
    100.00,
    'PRODUCTO',
    100,
    'COMPLETO',
    TRUE,
    TIMESTAMP '2026-11-01 12:00:00'
);

INSERT INTO product_reviews (
    id, publication_id, buyer_id,
    rating, comment, created_at
)
SELECT
    (
        '33333333-3333-4333-8333-' ||
        LPAD(n::text, 12, '0')
    )::uuid,
    '22222222-2222-4222-8222-222222222222'::uuid,
    '11111111-1111-4111-8111-111111111111'::uuid,
    1 + (n % 5),
    'Resena experimental E3 numero ' || n,
    TIMESTAMPTZ '2026-11-01 12:00:00+00'
        + n * INTERVAL '1 second'
FROM generate_series(1, 100) AS serie(n);

COMMIT;
