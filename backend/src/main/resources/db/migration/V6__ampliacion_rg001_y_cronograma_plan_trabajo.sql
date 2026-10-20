-- =============================================================================
-- V6__ampliacion_rg001_y_cronograma_plan_trabajo.sql
-- Elaborar Plan de Trabajo para acreditación de carreras (PRG-001 Hito 2)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Ampliación de registro_convocatoria (Metadatos CONEAU RG-001)
-- ----------------------------------------------------------------------------
ALTER TABLE registro_convocatoria
    ADD COLUMN rm_estandares VARCHAR(100),
    ADD COLUMN fase_evaluacion VARCHAR(50) DEFAULT 'PRIMERA_FASE',
    ADD COLUMN fecha_limite_presentacion_coneau DATE;

ALTER TABLE registro_convocatoria
    ADD CONSTRAINT ck_registro_convocatoria_fase
        CHECK (fase_evaluacion IN ('PRIMERA_FASE', 'SEGUNDA_FASE', 'NUEVO_CICLO', 'SEGUIMIENTO')),
    ADD CONSTRAINT ck_registro_convocatoria_fecha_limite
        CHECK (fecha_limite_presentacion_coneau IS NULL OR fecha_limite_presentacion_coneau >= fecha_cierre);

-- ----------------------------------------------------------------------------
-- 2. Ampliación de actividad_plan_trabajo (Cronograma Hojas INFO y CONEAU)
-- ----------------------------------------------------------------------------
ALTER TABLE actividad_plan_trabajo
    ADD COLUMN hoja VARCHAR(20) DEFAULT 'INFO',
    ADD COLUMN fecha_inicio_estimada DATE,
    ADD COLUMN fecha_fin_estimada DATE,
    ADD COLUMN responsable VARCHAR(150),
    ADD COLUMN prioridad VARCHAR(20) DEFAULT 'MEDIA';

ALTER TABLE actividad_plan_trabajo
    ADD CONSTRAINT ck_actividad_hoja
        CHECK (hoja IN ('INFO', 'CONEAU')),
    ADD CONSTRAINT ck_actividad_prioridad
        CHECK (prioridad IN ('ALTA', 'MEDIA', 'BAJA')),
    ADD CONSTRAINT ck_actividad_fechas_estimadas
        CHECK (fecha_inicio_estimada IS NULL OR fecha_fin_estimada IS NULL OR fecha_fin_estimada >= fecha_inicio_estimada);

-- Ajuste de restricción de completitud:
-- Permite que actividades distintas a ACT-01 (reunión de coordinación) se completen con auditoría (completada_en y completada_por)
-- sin forzar fecha_reunion_coordinacion ni sintesis_acuerdos.
ALTER TABLE actividad_plan_trabajo DROP CONSTRAINT IF EXISTS ck_actividad_completada;

ALTER TABLE actividad_plan_trabajo
    ADD CONSTRAINT ck_actividad_completada
        CHECK (
            (estado <> 'COMPLETADA') OR (
                completada_en IS NOT NULL AND
                completada_por IS NOT NULL
            )
        );
