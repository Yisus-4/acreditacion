-- =============================================================================
-- V32__programa_analitico_proceso_opcional.sql
-- Hacer proceso_id opcional en programa_analitico_asignatura y agregar columnas de vigencia
-- =============================================================================

ALTER TABLE programa_analitico_asignatura ALTER COLUMN proceso_id DROP NOT NULL;

ALTER TABLE programa_analitico_asignatura ADD COLUMN IF NOT EXISTS vigencia_desde INTEGER;
ALTER TABLE programa_analitico_asignatura ADD COLUMN IF NOT EXISTS vigencia_hasta INTEGER;

CREATE INDEX IF NOT EXISTS idx_prog_analitico_actividad ON programa_analitico_asignatura(actividad_curricular_id);
