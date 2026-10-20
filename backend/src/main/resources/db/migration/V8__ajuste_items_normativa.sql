-- =============================================================================
-- V8__ajuste_items_normativa.sql
-- Actualización de catálogo de documentos de normativa (Actividad 1.3 - PT-001)
-- =============================================================================

ALTER TABLE documento_normativa DROP CONSTRAINT IF EXISTS ck_documento_normativa_tipo;

ALTER TABLE documento_normativa ADD CONSTRAINT ck_documento_normativa_tipo CHECK (tipo_documento IN (
    'RM_VALIDEZ_NACIONAL',
    'CREACION_CARRERA',
    'REGLAMENTO_CARRERA',
    'ORDENANZA_PLAN_ESTUDIOS',
    'PLAN_ESTUDIOS_VIGENTE',
    'APROBACION_PROGRAMAS_ANALITICOS',
    'RESOLUCION_DIRECTOR_CARRERA',
    'RESOLUCION_COMISION_CURRICULAR',
    'PLAN_DESARROLLO_CARRERA',
    'PLAN_MEJORAS_PREVIO',
    'INFORME_CUMPLIMIENTO_PREVIO',
    'ACTA_COMISION_CURRICULAR',
    'RM_ESTANDARES_CARRERA',
    'RM_INCLUSION_ART_43',
    'RESOLUCION_CONEAU_ANTERIOR'
));