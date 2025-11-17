-- ======================================================================
-- data.sql - Seed mínimo adaptado a IDs internos/externos y fecha/horario
-- ======================================================================

-- =========================
-- Rubros
-- =========================
INSERT INTO rubro (id, external_id, nombre) VALUES
  (1, 1, 'Plomeria'),
  (2, 2, 'Electricidad'),
  (3, 3, 'Pintura'),
  (10, 300, 'Gasistas')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Habilidades
-- =========================
INSERT INTO habilidad (id, external_id, nombre, rubro_id) VALUES
  (1, 1, 'Reparacion de canerias', 1),
  (2, 2, 'Instalacion de sanitarios', 1),
  (3, 3, 'Cableado electrico', 2),
  (4, 4, 'Pintura de interiores', 3),
  (5, 5, 'Emergencias electricas', 2),
  (6, 6, 'Barnizado exterior', 3),
  (7, 7, 'Destapaciones', 1),
  (8, 120, 'Cambio en el baño', 1),
  (10, 501, 'Gasista matriculado', 10)
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
  (3, 3, 'Carlos','Lopez','carlos.lopez@mail.com','111111113','Dir 3','ACTIVO',1700,1,15),
  (4, 4, 'Sofia','Martinez','sofia.martinez@mail.com','111111114','Dir 4','ACTIVO',1650,1,18),
  (5, 5, 'Diego','Ruiz','diego.ruiz@mail.com','111111115','Dir 5','ACTIVO',1550,2,9),
  (6, 6, 'Laura','Benitez','laura.benitez@mail.com','111111116','Dir 6','ACTIVO',1600,1,11),
  (7, 7, 'Marcelo','Ibarra','marcelo.ibarra@mail.com','111111117','Dir 7','ACTIVO',1400,2,7),
  (101, 101, 'Juan','Perez','juan.perez@example.com','111111118','Laplace 1200','ACTIVO',4500,1,5),
  (102, 102, 'Maria','Lopez','maria.lopez@example.com','111111119','Amenabar 900','ACTIVO',4700,1,7),
  (103, 103, 'Carlos','Gomez','carlos.gomez@example.com','111111120','Cabildo 450','ACTIVO',4800,2,6),
  (104, 104, 'Gabriel','Sosa','gabriel.sosa@example.com','111111121','Laplace 1450','ACTIVO',4600,1,8),
  (105, 105, 'Luciana','Rios','luciana.rios@example.com','111111122','Amenabar 980','ACTIVO',4550,2,6),
  (106, 106, 'Diego','Valdez','diego.valdez@example.com','111111123','Cabildo 4500','ACTIVO',4750,1,9)
ON CONFLICT (internal_id) DO NOTHING;

-- =========================
-- Calificaciones (prestador_calificacion) -> usa prestador.internal_id
-- =========================
INSERT INTO prestador_calificacion (prestador_id, puntuacion) VALUES
  (1,5),(1,4),(1,5),
  (2,5),(2,4),(2,5),
  (3,3),(3,4),(3,3),
  (4,5),(4,5),(4,4),
  (5,4),(5,4),(5,5),
  (6,5),(6,4),
  (7,3),(7,4),
  (101,5),(101,4),
  (102,5),(102,5),
  (103,4),(103,4),
  (104,5),(104,4),
  (105,5),(105,5),
  (106,4),(106,4);

-- =========================
-- Habilidades asignadas (prestador_id -> internal_id)
-- =========================
INSERT INTO prestador_habilidad (prestador_id, habilidad_id) VALUES
  (1,1), (1,7), (1,8),
  (2,4), (2,6),
  (3,3),
  (4,3), (4,5),
  (5,5),
  (6,4), (6,6),
  (7,2), (7,8),
  (101,10),
  (102,10),
  (103,10),
  (104,10),
  (105,10),
  (106,10);

-- =========================
-- Solicitudes (usa external_id y fecha/horario)
-- =========================
INSERT INTO solicitud (external_id, usuario_id, rubro_id, habilidad_id, titulo, descripcion, estado,
  prestador_asignado_id, fue_cotizada, es_critica, fecha, horario, created_at, updated_at, preferencia_ventana_str) VALUES
  -- Solicitud con habilidad principal (pintura) y rubro nulo para probar selección por habilidad
  (1001, 1, NULL, 4, 'Pintar living', 'Pintar living y pasillo', 'CREADA', NULL, false, false, '2025-09-12', '10:00', NOW(), NOW(), NULL),
  -- Solicitud con habilidad de emergencias, habrá fallback a rubro para completar top 3
  (1002, 2, NULL, 5, 'Apagones repetidos', 'Salta la térmica con frecuencia', 'CREADA', NULL, false, true, '2025-09-12', '18:00', NOW(), NOW(), NULL),
  -- Solicitud legacy con rubro pero sin habilidad
  (1003, 1, 1, NULL, 'Reparación de cañería', 'Perdida en la cocina', 'ASIGNADA', 1, true, false, '2025-09-12', '09:00', NOW(), NOW(), '09:00-10:00'),
  -- Solicitud con habilidad 120 (ejemplo del webhook) para validar resolución automática de rubro
  (120045, 2, NULL, 120, 'Cambio en el baño', 'Pérdida en la canilla del baño.', 'CREADA', NULL, false, false, '2025-09-12', '13:00', NOW(), NOW(), NULL),
  -- Escenario gasistas: listo para recibir rechazos (round 1 abierto)
  (9001, 1, 300, 501, 'Arreglo pérdida de gas', 'Pico en cocina', 'COTIZANDO', NULL, true, false, CURRENT_DATE, '10:00', NOW(), NOW(), NULL)
ON CONFLICT (external_id) DO NOTHING;

-- =========================
-- Invitaciones emitidas para solicitud 9001 (round 1)
-- =========================
INSERT INTO solicitud_invitacion (solicitud_id, prestador_id, round, enviado_at, rechazada, cotizacion_id_externo, rechazo_motivo)
SELECT s.internal_id, p.internal_id, 1, NOW(), FALSE, NULL, NULL
FROM solicitud s
JOIN prestador p ON p.external_id IN (101, 102, 103)
WHERE s.external_id = 9001
  AND NOT EXISTS (
        SELECT 1 FROM solicitud_invitacion si
        WHERE si.solicitud_id = s.internal_id
          AND si.prestador_id = p.internal_id
          AND si.round = 1
    );

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
