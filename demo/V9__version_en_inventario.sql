-- Sesión 14 · Bloqueo optimista (lámina 21): la columna que Hibernate
-- compara en cada UPDATE (WHERE id = ? AND version = ?) y que incrementa al
-- guardar. DEFAULT 0 hace que las filas existentes queden con la versión 0;
-- sin él, la migración fallaría porque la columna es NOT NULL.
ALTER TABLE inventario ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
