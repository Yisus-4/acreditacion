-- Migration: V34__add_carrera_id_autoevaluacion_ambito.sql
-- Module: Autoevaluación Institucional (Ámbitos de Formación Práctica)
-- Target Table: autoevaluacion_carrera_ambito

-- 1. Add carrera_id column referencing carrera table
ALTER TABLE autoevaluacion_carrera_ambito
    ADD COLUMN IF NOT EXISTS carrera_id UUID REFERENCES carrera(id) ON DELETE SET NULL;

-- 2. Backfill existing records from proceso_acreditacion
UPDATE autoevaluacion_carrera_ambito a
SET carrera_id = p.carrera_id
FROM proceso_acreditacion p
WHERE a.proceso_id = p.id AND a.carrera_id IS NULL;

-- 3. Indexes for career and process+career queries
CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_carrera
    ON autoevaluacion_carrera_ambito(carrera_id);

CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_proc_carrera
    ON autoevaluacion_carrera_ambito(proceso_id, carrera_id);

-- 4. Drop legacy single-process unique constraint
ALTER TABLE autoevaluacion_carrera_ambito
    DROP CONSTRAINT IF EXISTS uq_proceso_ambito;

-- 5. Create PostgreSQL 16 composite unique index with NULLS NOT DISTINCT
CREATE UNIQUE INDEX IF NOT EXISTS uq_autoeval_proc_carrera_ambito
    ON autoevaluacion_carrera_ambito (proceso_id, carrera_id, ambito_id) NULLS NOT DISTINCT;

COMMENT ON COLUMN autoevaluacion_carrera_ambito.carrera_id
    IS 'Identificador de la carrera de grado evaluada que vincula el ámbito de práctica';
