-- =============================================================================
-- V29__predecesoras_actividad_plan_trabajo.sql
-- Predecesoras y dependencias lógicas de actividades del Plan de Trabajo (PRG-002)
-- =============================================================================

ALTER TABLE actividad_plan_trabajo
    ADD COLUMN IF NOT EXISTS predecesoras TEXT;
