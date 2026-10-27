\set ON_ERROR_STOP on
-- Only synthetic fixtures in the dedicated demo database. Never use on production.
BEGIN;
DO $$ BEGIN
  IF current_database() <> 'umss_recommendations_demo' THEN
    RAISE EXCEPTION 'Use only umss_recommendations_demo';
  END IF;
END $$;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\getenv demo_password UMSS_DEMO_BUYER_PASSWORD
SELECT length(:'demo_password') >= 12 AS password_ok \gset
\if :password_ok
\else
  \echo 'UMSS_DEMO_BUYER_PASSWORD must contain at least 12 characters'
  \quit 1
\endif
SELECT crypt(:'demo_password', gen_salt('bf', 12)) AS demo_hash \gset
INSERT INTO users (id,ru,nombre,email,password_hash,role,status,created_at,updated_at)
VALUES
 ('00000000-0000-0000-0000-000000000001','DEMO-A','Comprador A','buyer-a@demo.invalid',:'demo_hash','COMPRADOR','ACTIVE',now(),now()),
 ('00000000-0000-0000-0000-000000000002','DEMO-B','Comprador B','buyer-b@demo.invalid',:'demo_hash','COMPRADOR','ACTIVE',now(),now()),
 ('00000000-0000-0000-0000-000000000003','DEMO-C','Sin historial','buyer-c@demo.invalid',:'demo_hash','COMPRADOR','ACTIVE',now(),now()),
 ('00000000-0000-0000-0000-000000000004','DEMO-T','Tienda tecnologia','store-tech@demo.invalid',:'demo_hash','EMPRENDEDOR','ACTIVE',now(),now()),
 ('00000000-0000-0000-0000-000000000005','DEMO-D','Tienda deporte','store-sport@demo.invalid',:'demo_hash','EMPRENDEDOR','ACTIVE',now(),now())
ON CONFLICT (id) DO UPDATE SET password_hash = EXCLUDED.password_hash;
INSERT INTO stores (id,user_id,nombre,categoria,created_at,updated_at)
VALUES
 ('00000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000004','Demo tecnologia','Tecnologia',now(),now()),
 ('00000000-0000-0000-0000-000000000020','00000000-0000-0000-0000-000000000005','Demo deporte','Deporte',now(),now())
ON CONFLICT DO NOTHING;
INSERT INTO publications (id,store_id,nombre,descripcion,precio,stock,activa,created_at,updated_at)
SELECT ('00000000-0000-0000-0000-' || lpad((100+n)::text,12,'0'))::uuid,
       '00000000-0000-0000-0000-000000000010'::uuid,
       'Tecnologia computadora accesorio '||n,'Tecnologia computadora teclado electronica',50+n,5,true,now(),now()
FROM generate_series(1,6) n
ON CONFLICT DO NOTHING;
INSERT INTO publications (id,store_id,nombre,descripcion,precio,stock,activa,created_at,updated_at)
SELECT ('00000000-0000-0000-0000-' || lpad((200+n)::text,12,'0'))::uuid,
       '00000000-0000-0000-0000-000000000020'::uuid,
       'Deporte futbol accesorio '||n,'Deporte futbol entrenamiento balon',30+n,5,true,now(),now()
FROM generate_series(1,6) n
ON CONFLICT DO NOTHING;
INSERT INTO publications (id,store_id,nombre,descripcion,precio,stock,activa,created_at,updated_at)
VALUES
 ('00000000-0000-0000-0000-000000000301','00000000-0000-0000-0000-000000000010','Tecnologia agotada','Tecnologia computadora',50,0,true,now(),now()),
 ('00000000-0000-0000-0000-000000000302','00000000-0000-0000-0000-000000000020','Deporte inactivo','Deporte futbol',50,5,false,now(),now())
ON CONFLICT DO NOTHING;
COMMIT;
-- No real registration or SIIS validation: these are isolated academic fixtures.
