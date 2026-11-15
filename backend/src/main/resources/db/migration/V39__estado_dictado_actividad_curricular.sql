ALTER TABLE actividad_curricular_plan
    ADD COLUMN IF NOT EXISTS estado_dictado VARCHAR(20) NOT NULL DEFAULT 'SE_DICTA';

ALTER TABLE actividad_curricular_plan
    DROP CONSTRAINT IF EXISTS chk_actividad_curricular_estado_dictado;

ALTER TABLE actividad_curricular_plan
    ADD CONSTRAINT chk_actividad_curricular_estado_dictado
        CHECK (estado_dictado IN ('SE_DICTA', 'NO_SE_DICTA'));
