-- ======================================================================
-- data.sql — Auto-seed para desarrollo local
-- Ubicación: src/main/resources/data.sql
-- Requisitos recomendados (application.properties):
--   spring.jpa.hibernate.ddl-auto=update
--   spring.jpa.defer-datasource-initialization=true
--   spring.sql.init.mode=always
--   spring.sql.init.continue-on-error=true
-- ======================================================================

-- =========================
-- Rubros
-- =========================
INSERT INTO rubro (id, nombre) VALUES
  (1, 'Plomeria'),
  (2, 'Electricidad'),
  (3, 'Pintura'),
  (4, 'Carpinteria'),
  (7, 'Servicios Generales')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Habilidades
-- =========================
INSERT INTO habilidad (id, nombre, rubro_id) VALUES
  (1, 'Reparacion de canerias', 1),
  (2, 'Instalacion de sanitarios', 1),
  (3, 'Cableado electrico', 2),
  (4, 'Instalacion de luminarias', 2),
  (5, 'Pintura de interiores', 3),
  (6, 'Pintura de exteriores', 3),
  (7, 'Fabricacion de muebles', 4),
  (8, 'Reparacion de puertas', 4),
  (9,  'Electricidad General', 7),
  (10, 'Electricidad General', 7),
  (11, 'Electricidad General', 7)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Zonas
-- =========================
INSERT INTO zona (id, nombre) VALUES
  (1, 'Centro'),
  (2, 'Norte'),
  (3, 'Sur'),
  (4, 'Este'),
  (5, 'Oeste')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Usuarios
-- =========================
INSERT INTO usuario (id, nombre, apellido, direccion) VALUES
  (1, 'Ana', 'Garcia', 'Italia 100'),
  (2, 'Luis', 'Fernandez', 'Belgrano 200'),
  (3, 'Sofia', 'Martinez', 'Rivadavia 300'),
  (4, 'Tomas', 'Suarez', 'Sarmiento 400'),
  (5, 'Valentina', 'Paz', 'Mitre 500')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Prestadores
-- =========================
INSERT INTO prestador (id, nombre, apellido, email, telefono, direccion, estado, precio_hora, zona_id) VALUES
  (1, 'Juan',   'Perez',   'juan.perez@mail.com',   '1122334455', 'Calle Falsa 123',     'ACTIVO',   1500, 1),
  (2, 'Maria',  'Gomez',   'maria.gomez@mail.com',  '1144556677', 'Av. Siempreviva 742', 'ACTIVO',   2000, 2),
  (3, 'Carlos', 'Lopez',   'carlos.lopez@mail.com', '1133665599', 'San Martin 555',      'INACTIVO', 1800, 3),
  (4, 'Lucia',  'Rossi',   'lucia.rossi@mail.com',  '1160011223', 'Peru 123',            'ACTIVO',   1700, 1),
  (5, 'Diego',  'Sosa',    'diego.sosa@mail.com',   '1161122334', 'Chile 456',           'ACTIVO',   1600, 2),
  (6, 'Paula',  'Vega',    'paula.vega@mail.com',   '1162233445', 'Mexico 789',          'ACTIVO',   1900, 2),
  (7, 'Hernan', 'Ibarra',  'hernan.ibarra@mail.com','1163344556', 'Brasil 321',          'ACTIVO',   1550, 3)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Prestador-Habilidad (SIN ON CONFLICT; idempotente con WHERE NOT EXISTS)
-- =========================

-- Electricidad (rubro 2): habilidades 3 (Cableado), 4 (Luminarias)
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 1, 3 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 1 AND habilidad_id = 3
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 1, 4 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 1 AND habilidad_id = 4
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 2, 3 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 2 AND habilidad_id = 3
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 2, 4 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 2 AND habilidad_id = 4
);

-- Pintura (rubro 3): habilidades 5 (Interiores), 6 (Exteriores)
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 3, 5 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 3 AND habilidad_id = 5
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 4, 5 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 4 AND habilidad_id = 5
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 4, 6 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 4 AND habilidad_id = 6
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 5, 5 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 5 AND habilidad_id = 5
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 6, 6 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 6 AND habilidad_id = 6
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 7, 5 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 7 AND habilidad_id = 5
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 7, 6 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 7 AND habilidad_id = 6
);

-- (Opcional) Plomería (rubro 1): habilidades 1 (Cañerías), 2 (Sanitarios)
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 1, 1 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 1 AND habilidad_id = 1
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 2, 2 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 2 AND habilidad_id = 2
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 4, 1 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 4 AND habilidad_id = 1
);
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT 5, 2 WHERE NOT EXISTS (
  SELECT 1 FROM prestador_habilidad WHERE prestador_id = 5 AND habilidad_id = 2
);


-- =========================
-- Solicitudes (estado: CREADA)
-- =========================
INSERT INTO solicitud (
  id, usuario_id, servicio_id, categoria_id, descripcion, estado,
  cotizacion_aceptada_id, prestador_asignado_id, created_at, updated_at
) VALUES
  (1001, 1, 5001, 3, 'Pintar living y pasillo',          'CREADA', NULL, NULL, NOW(), NOW()),
  (1002, 2, 5002, 3, 'Pintura exterior de balcon',       'CREADA', NULL, NULL, NOW(), NOW()),
  (1003, 3, 5003, 2, 'Revision de cableado en cocina',   'CREADA', NULL, NULL, NOW(), NOW()),
  (1004, 4, 5004, 1, 'Cambio de sanitario en bano',      'CREADA', NULL, 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Calificaciones (idempotentes sin ON CONFLICT)
-- =========================
INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2001, 1001, 4, 5, 'Excelente trabajo', NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 4 AND solicitud_id = 1001 AND usuario_id = 1
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2002, 1002, 4, 5, 'Muy prolija', NOW(), 2
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 4 AND solicitud_id = 1002 AND usuario_id = 2
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2003, 1003, 4, 4, 'Buen resultado', NOW(), 3
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 4 AND solicitud_id = 1003 AND usuario_id = 3
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2004, 1001, 5, 4, 'Cumplio tiempos', NOW(), 2
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 5 AND solicitud_id = 1001 AND usuario_id = 2
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2005, 1002, 5, 5, 'Muy bien', NOW(), 3
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 5 AND solicitud_id = 1002 AND usuario_id = 3
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2006, 1004, 5, 4, 'Correcto', NOW(), 4
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 5 AND solicitud_id = 1004 AND usuario_id = 4
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2007, 1001, 6, 4, 'Bien', NOW(), 3
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 6 AND solicitud_id = 1001 AND usuario_id = 3
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2008, 1002, 6, 3, 'Podria mejorar', NOW(), 4
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 6 AND solicitud_id = 1002 AND usuario_id = 4
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2009, 1003, 6, 4, 'Aceptable', NOW(), 5
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 6 AND solicitud_id = 1003 AND usuario_id = 5
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2010, 1001, 7, 4, 'Bien en general', NOW(), 4
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 7 AND solicitud_id = 1001 AND usuario_id = 4
);

INSERT INTO calificacion (id, solicitud_id, prestador_id, puntuacion, comentario, created_at, usuario_id)
SELECT 2011, 1002, 7, 4, 'Trabajo correcto', NOW(), 5
WHERE NOT EXISTS (
  SELECT 1 FROM calificacion WHERE prestador_id = 7 AND solicitud_id = 1002 AND usuario_id = 5
);

-- ======================================================================
-- Reajuste de secuencias (evita choque con IDs manuales)
-- ======================================================================
WITH s AS (SELECT pg_get_serial_sequence('rubro','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM rubro), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('habilidad','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM habilidad), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('zona','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM zona), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('usuario','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM usuario), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('prestador','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM prestador), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('solicitud','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM solicitud), false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('calificacion','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq, (SELECT COALESCE(MAX(id),0)+1 FROM calificacion), false) END FROM s;
