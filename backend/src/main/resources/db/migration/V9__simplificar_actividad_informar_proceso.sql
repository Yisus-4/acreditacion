-- =============================================================================
-- V9__simplificar_actividad_informar_proceso.sql
-- Simplificación de Actividad 1: Informar sobre el proceso de acreditación
-- Agrega registro estructurado de áreas informadas y documentación remitida.
-- =============================================================================

ALTER TABLE actividad_plan_trabajo
    ADD COLUMN IF NOT EXISTS areas_informadas TEXT,
    ADD COLUMN IF NOT EXISTS documentacion_remitida TEXT;
