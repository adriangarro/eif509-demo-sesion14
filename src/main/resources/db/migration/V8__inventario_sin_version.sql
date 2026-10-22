-- Sesión 14 · Punto de partida de la demostración: el inventario sin la
-- columna de versión, como en un proyecto que nunca usó bloqueo optimista.
-- (La columna existía desde la V4; en la demostración se agrega de nuevo con
-- la migración preparada en demo/V9__version_en_inventario.sql.)
ALTER TABLE inventario DROP COLUMN version;
