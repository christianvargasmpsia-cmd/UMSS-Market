-- DEMO 2 / UC-008. Solo para PostgreSQL de prueba (NO produccion).
-- Reejecutable: no duplica publicaciones si se corre de nuevo para la misma tienda.
-- La tienda corresponde al emprendedor de la sesion Postman de Demo 2.
DO $$
DECLARE
    v_store UUID := '1da08089-9f73-44c0-af59-4d0b76ccfcb0';
    v_inserted INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.stores WHERE id = v_store) THEN
        RAISE EXCEPTION 'No existe tienda de demostracion: %', v_store;
    END IF;

    INSERT INTO public.publications
        (id, store_id, nombre, descripcion, precio, tipo, stock, activa, created_at, updated_at)
    SELECT gen_random_uuid(), v_store, p.nombre, p.descripcion, p.precio,
           'PRODUCTO', p.stock, TRUE, NOW() - p.antiguedad, NOW()
    FROM (VALUES
       ('DEMO2 Mochila agotada', 'Escenario stock 0', 100.00::numeric, 0, INTERVAL '4 days'),
       ('DEMO2 Mouse stock bajo', 'Escenario stock 2', 60.00::numeric, 2, INTERVAL '3 days'),
       ('DEMO2 Teclado stock bajo', 'Escenario stock 5', 90.00::numeric, 5, INTERVAL '2 days'),
       ('DEMO2 Mochila en existencia', 'Escenario stock normal 20', 150.00::numeric, 20, INTERVAL '1 day')
    ) AS p(nombre, descripcion, precio, stock, antiguedad)
    WHERE NOT EXISTS (
        SELECT 1 FROM public.publications existing
        WHERE existing.store_id = v_store AND existing.nombre = p.nombre
    );
    GET DIAGNOSTICS v_inserted = ROW_COUNT;
    RAISE NOTICE 'Publicaciones DEMO2 agregadas: %', v_inserted;
END $$;

-- Verificar que el escenario cuente 4 productos criticos, incluido el producto reserva.
SELECT id, nombre, stock, activa
FROM public.publications
WHERE store_id = '1da08089-9f73-44c0-af59-4d0b76ccfcb0'
  AND tipo = 'PRODUCTO' AND activa = TRUE
ORDER BY created_at DESC, id DESC;
