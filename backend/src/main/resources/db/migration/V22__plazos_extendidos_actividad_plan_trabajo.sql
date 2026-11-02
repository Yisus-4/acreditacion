-- =============================================================================
-- V22__plazos_extendidos_actividad_plan_trabajo.sql
-- Historial de plazos extendidos por actividad del Plan de Trabajo (sin retroceso)
-- =============================================================================

ALTER TABLE actividad_plan_trabajo
    ADD COLUMN IF NOT EXISTS plazos_extendidos TEXT;
