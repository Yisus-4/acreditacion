-- =============================================================================
-- V4_1__carrera_descripcion.sql -- Agregar campo descripcion a carrera
-- =============================================================================

ALTER TABLE carrera ADD COLUMN IF NOT EXISTS descripcion TEXT;
