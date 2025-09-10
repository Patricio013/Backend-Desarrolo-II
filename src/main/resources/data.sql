-- ======================================================================
-- data.sql — Seed inicial con 50 prestadores y calificaciones embebidas
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
  (9, 'Electricidad General', 7),
  (10, 'Albañileria General', 7),
  (11, 'Jardineria General', 7)
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
-- Prestadores (50 registros con trabajos_finalizados)
-- =========================
INSERT INTO prestador (id, nombre, apellido, email, telefono, direccion, estado, precio_hora, zona_id, trabajos_finalizados) VALUES
-- id, nombre, apellido, email, tel, direccion, estado, precio, zona, trabajos
(1, 'Juan','Perez','juan.perez@mail.com','111111111','Dir 1','ACTIVO',1500,1,12),
(2, 'Maria','Gomez','maria.gomez@mail.com','111111112','Dir 2','ACTIVO',1800,2,22),
(3, 'Carlos','Lopez','carlos.lopez@mail.com','111111113','Dir 3','INACTIVO',1700,3,5),
(4, 'Lucia','Rossi','lucia.rossi@mail.com','111111114','Dir 4','ACTIVO',2000,4,33),
(5, 'Diego','Sosa','diego.sosa@mail.com','111111115','Dir 5','ACTIVO',1600,5,41),
(6, 'Paula','Vega','paula.vega@mail.com','111111116','Dir 6','ACTIVO',1750,1,19),
(7, 'Hernan','Ibarra','hernan.ibarra@mail.com','111111117','Dir 7','ACTIVO',1850,2,9),
(8, 'Sergio','Mendez','sergio.mendez@mail.com','111111118','Dir 8','ACTIVO',2100,3,44),
(9, 'Laura','Acosta','laura.acosta@mail.com','111111119','Dir 9','ACTIVO',1700,4,17),
(10,'Valeria','Nuñez','valeria.nunez@mail.com','111111120','Dir 10','INACTIVO',1900,5,3),
(11,'Martin','Diaz','martin.diaz@mail.com','111111121','Dir 11','ACTIVO',1600,1,25),
(12,'Sofia','Ruiz','sofia.ruiz@mail.com','111111122','Dir 12','ACTIVO',1550,2,8),
(13,'Ezequiel','Benitez','eze.benitez@mail.com','111111123','Dir 13','ACTIVO',2200,3,28),
(14,'Nicolas','Paredes','nico.paredes@mail.com','111111124','Dir 14','ACTIVO',1400,4,12),
(15,'Gaston','Ferreyra','gaston.f@mail.com','111111125','Dir 15','ACTIVO',1650,5,16),
(16,'Agustina','Luna','agus.luna@mail.com','111111126','Dir 16','ACTIVO',1750,1,29),
(17,'Mauro','Bravo','mauro.bravo@mail.com','111111127','Dir 17','ACTIVO',1950,2,21),
(18,'Camila','Rivas','camila.rivas@mail.com','111111128','Dir 18','ACTIVO',2000,3,37),
(19,'Federico','Torres','fede.torres@mail.com','111111129','Dir 19','ACTIVO',1850,4,14),
(20,'Julieta','Moreno','julieta.m@mail.com','111111130','Dir 20','ACTIVO',1750,5,31),
(21,'Mateo','Castro','mateo.castro@mail.com','111111131','Dir 21','ACTIVO',1800,1,23),
(22,'Daniela','Molina','daniela.m@mail.com','111111132','Dir 22','ACTIVO',1900,2,15),
(23,'Sebastian','Silva','sebastian.s@mail.com','111111133','Dir 23','ACTIVO',1650,3,7),
(24,'Florencia','Ortiz','flor.ortiz@mail.com','111111134','Dir 24','ACTIVO',1500,4,10),
(25,'Tomas','Navarro','tomas.n@mail.com','111111135','Dir 25','ACTIVO',1600,5,6),
(26,'Micaela','Rojas','mica.rojas@mail.com','111111136','Dir 26','ACTIVO',1700,1,19),
(27,'Joaquin','Arias','joa.arias@mail.com','111111137','Dir 27','ACTIVO',1850,2,22),
(28,'Candela','Meza','candela.m@mail.com','111111138','Dir 28','ACTIVO',1950,3,33),
(29,'Ramiro','Vazquez','ramiro.v@mail.com','111111139','Dir 29','ACTIVO',2000,4,11),
(30,'Milagros','Herrera','mili.h@mail.com','111111140','Dir 30','ACTIVO',1800,5,13),
(31,'Pablo','Juarez','pablo.j@mail.com','111111141','Dir 31','ACTIVO',1600,1,4),
(32,'Cecilia','Farias','ceci.f@mail.com','111111142','Dir 32','ACTIVO',1750,2,12),
(33,'Franco','Campos','franco.c@mail.com','111111143','Dir 33','ACTIVO',1850,3,27),
(34,'Martina','Dominguez','marti.d@mail.com','111111144','Dir 34','ACTIVO',1900,4,30),
(35,'Andres','Suarez','andres.s@mail.com','111111145','Dir 35','ACTIVO',2000,5,42),
(36,'Pilar','Medina','pilar.m@mail.com','111111146','Dir 36','ACTIVO',2100,1,8),
(37,'Lucas','Aguilar','lucas.a@mail.com','111111147','Dir 37','ACTIVO',1600,2,6),
(38,'Carla','Muñoz','carla.m@mail.com','111111148','Dir 38','ACTIVO',1700,3,5),
(39,'Bruno','Peralta','bruno.p@mail.com','111111149','Dir 39','ACTIVO',1750,4,16),
(40,'Belen','Godoy','belen.g@mail.com','111111150','Dir 40','ACTIVO',1850,5,19),
(41,'Enzo','Roldan','enzo.r@mail.com','111111151','Dir 41','ACTIVO',1900,1,21),
(42,'Rocio','Carrizo','rocio.c@mail.com','111111152','Dir 42','ACTIVO',2000,2,28),
(43,'Lautaro','Vera','lautaro.v@mail.com','111111153','Dir 43','ACTIVO',2100,3,33),
(44,'Magdalena','Acosta','magda.a@mail.com','111111154','Dir 44','ACTIVO',1700,4,12),
(45,'Ignacio','Soto','ignacio.s@mail.com','111111155','Dir 45','ACTIVO',1650,5,14),
(46,'Josefina','Rey','jose.rey@mail.com','111111156','Dir 46','ACTIVO',1550,1,9),
(47,'Alan','Quiroga','alan.q@mail.com','111111157','Dir 47','ACTIVO',1600,2,17),
(48,'Noelia','Bravo','noe.b@mail.com','111111158','Dir 48','ACTIVO',1750,3,20),
(49,'Cristian','Ocampo','cris.o@mail.com','111111159','Dir 49','ACTIVO',1850,4,24),
(50,'Tamara','Rios','tami.r@mail.com','111111160','Dir 50','ACTIVO',1950,5,29)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- Calificaciones (prestador_calificacion)
-- Cada prestador con 3–6 estrellas
-- =========================
INSERT INTO prestador_calificacion (prestador_id, puntuacion) VALUES
(1,5),(1,4),(1,5),
(2,4),(2,4),(2,5),(2,3),
(3,3),(3,4),(3,2),
(4,5),(4,5),(4,4),(4,5),
(5,4),(5,5),(5,4),
(6,4),(6,3),(6,4),
(7,5),(7,4),(7,5),
(8,3),(8,4),(8,3),
(9,5),(9,5),(9,4),
(10,2),(10,3),(10,2),
(11,4),(11,5),(11,4),
(12,3),(12,4),(12,3),
(13,5),(13,5),(13,5),
(14,4),(14,3),(14,4),
(15,5),(15,5),(15,4),
(16,4),(16,5),(16,4),
(17,3),(17,3),(17,4),
(18,5),(18,4),(18,5),
(19,4),(19,5),(19,4),
(20,5),(20,5),(20,5),
(21,4),(21,3),(21,4),
(22,5),(22,4),(22,5),
(23,3),(23,4),(23,2),
(24,5),(24,4),(24,5),
(25,4),(25,3),(25,4),
(26,5),(26,5),(26,4),
(27,4),(27,5),(27,3),
(28,5),(28,5),(28,5),
(29,4),(29,4),(29,5),
(30,5),(30,5),(30,4),
(31,3),(31,4),(31,3),
(32,5),(32,4),(32,5),
(33,4),(33,5),(33,4),
(34,5),(34,5),(34,4),
(35,5),(35,5),(35,5),
(36,3),(36,3),(36,4),
(37,4),(37,3),(37,4),
(38,5),(38,5),(38,4),
(39,4),(39,5),(39,4),
(40,5),(40,5),(40,5),
(41,4),(41,4),(41,5),
(42,5),(42,5),(42,4),
(43,4),(43,5),(43,4),
(44,3),(44,4),(44,3),
(45,5),(45,4),(45,5),
(46,4),(46,3),(46,4),
(47,5),(47,5),(47,5),
(48,4),(48,5),(48,4),
(49,5),(49,4),(49,5),
(50,4),(50,5),(50,4);

-- =========================
-- Habilidades asignadas (bloques simples para demo)
-- =========================
-- Los primeros 10 → Electricidad
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 3 FROM prestador WHERE id BETWEEN 1 AND 10;
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 4 FROM prestador WHERE id BETWEEN 1 AND 10;

-- 11–20 → Pintura
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 5 FROM prestador WHERE id BETWEEN 11 AND 20;
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 6 FROM prestador WHERE id BETWEEN 11 AND 20;

-- 21–30 → Plomería
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 1 FROM prestador WHERE id BETWEEN 21 AND 30;
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 2 FROM prestador WHERE id BETWEEN 21 AND 30;

-- 31–40 → Carpintería
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 7 FROM prestador WHERE id BETWEEN 31 AND 40;
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 8 FROM prestador WHERE id BETWEEN 31 AND 40;

-- 41–50 → Generales
INSERT INTO prestador_habilidad (prestador_id, habilidad_id)
SELECT id, 9 FROM prestador WHERE id BETWEEN 41 AND 50;

-- =========================
-- Solicitudes de prueba
-- =========================
INSERT INTO solicitud (id, usuario_id, servicio_id, categoria_id, descripcion, estado,
  cotizacion_aceptada_id, prestador_asignado_id, created_at, updated_at) VALUES
  (1001, 1, 5001, 3, 'Pintar living y pasillo', 'CREADA', NULL, NULL, NOW(), NOW()),
  (1002, 2, 5002, 3, 'Pintura exterior de balcon', 'CREADA', NULL, NULL, NOW(), NOW()),
  (1003, 3, 5003, 2, 'Revision de cableado en cocina', 'CREADA', NULL, NULL, NOW(), NOW()),
  (1004, 4, 5004, 1, 'Cambio de sanitario en bano', 'CREADA', NULL, 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ======================================================================
-- Reajuste de secuencias
-- ======================================================================
WITH s AS (SELECT pg_get_serial_sequence('rubro','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM rubro),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('habilidad','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM habilidad),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('zona','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM zona),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('usuario','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM usuario),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('prestador','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM prestador),false) END FROM s;

WITH s AS (SELECT pg_get_serial_sequence('solicitud','id') AS seq)
SELECT CASE WHEN seq IS NOT NULL THEN setval(seq,(SELECT COALESCE(MAX(id),0)+1 FROM solicitud),false) END FROM s;
