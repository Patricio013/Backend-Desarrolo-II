-- ======================================================================
-- data.sql - Seed mínimo adaptado a IDs internos/externos y fecha/horario
-- ======================================================================

-- =========================
-- Rubros
-- =========================
INSERT INTO rubro (id, external_id, nombre) VALUES
  (1, 1, 'Plomeria'),
  (2, 2, 'Electricidad'),
  (3, 3, 'Pintura')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Habilidades
-- =========================
INSERT INTO habilidad (id, external_id, nombre, rubro_id) VALUES
  (1, 1, 'Reparacion de canerias', 1),
  (2, 2, 'Instalacion de sanitarios', 1),
  (3, 3, 'Cableado electrico', 2),
  (4, 4, 'Pintura de interiores', 3)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Zonas
-- =========================
INSERT INTO zona (id, external_id, nombre) VALUES
  (1, 1, 'Centro'),
  (2, 2, 'Norte')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Usuarios
-- =========================
INSERT INTO usuario (id, nombre, apellido, direccion) VALUES
  (1, 'Ana', 'Garcia', 'Italia 100'),
  (2, 'Luis', 'Fernandez', 'Belgrano 200')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Prestadores (internal_id PK + external_id)
-- =========================
INSERT INTO prestador (internal_id, external_id, nombre, apellido, email, telefono, direccion, estado, precio_hora, zona_id, trabajos_finalizados) VALUES
  (1, 1, 'Juan','Perez','juan.perez@mail.com','111111111','Dir 1','ACTIVO',1500,1,12),
  (2, 2, 'Maria','Gomez','maria.gomez@mail.com','111111112','Dir 2','ACTIVO',1800,2,22),
  (3, 3, 'Carlos','Lopez','carlos.lopez@mail.com','111111113','Dir 3','ACTIVO',1700,1,15)
ON CONFLICT (internal_id) DO NOTHING;

-- =========================
-- Calificaciones (prestador_calificacion) -> usa prestador.internal_id
-- =========================
INSERT INTO prestador_calificacion (prestador_id, puntuacion) VALUES
  (1,5),(1,4),(1,5),
  (2,5),(2,4),(2,5),
  (3,3),(3,4),(3,3);

-- =========================
-- Habilidades asignadas (prestador_id -> internal_id)
-- =========================
INSERT INTO prestador_habilidad (prestador_id, habilidad_id) VALUES
  (1,1), (1,2),
  (2,4),
  (3,1);

-- =========================
-- Solicitudes (usa external_id y fecha/horario)
-- =========================
INSERT INTO solicitud (external_id, usuario_id, rubro_id, habilidad_id, titulo, descripcion, estado,
  prestador_asignado_id, fue_cotizada, es_critica, fecha, horario, created_at, updated_at) VALUES
  (1001, 1, 3, NULL, 'Pintar living', 'Pintar living y pasillo', 'CREADA', NULL, false, false, '2025-09-12', '10:00', NOW(), NOW()),
  (1002, 2, 3, 4, 'Pintura exterior', 'Pintura exterior de balcon', 'CREADA', NULL, false, false, '2025-09-12', '11:00', NOW(), NOW()),
  (1003, 1, 1, 1, 'Reparación de cañería', 'Perdida en la cocina', 'ASIGNADA', 1, true, false, '2025-09-12', '09:00', NOW(), NOW())
ON CONFLICT (external_id) DO NOTHING;

-- ======================================================================
-- Reajuste de secuencias (PK)
-- ======================================================================
WITH s AS (SELECT pg_get_serial_sequence('rubro','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM rubro),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('habilidad','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM habilidad),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('zona','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM zona),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('usuario','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM usuario),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('prestador','internal_id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(internal_id),0)+1 FROM prestador),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('solicitud','internal_id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(internal_id),0)+1 FROM solicitud),false) END FROM s;

