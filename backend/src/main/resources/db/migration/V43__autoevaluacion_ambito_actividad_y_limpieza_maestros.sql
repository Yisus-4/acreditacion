DELETE FROM autoevaluacion_carrera_ambito aca
USING ambito_practica ap
WHERE aca.ambito_id = ap.id
  AND (ap.es_maestro = TRUE OR ap.proceso_id IS NULL);

CREATE TABLE IF NOT EXISTS autoevaluacion_ambito_actividad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    autoevaluacion_ambito_id UUID NOT NULL REFERENCES autoevaluacion_carrera_ambito(id) ON DELETE CASCADE,
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_autoeval_ambito_actividad UNIQUE (autoevaluacion_ambito_id, actividad_curricular_id)
);

CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_act_ambito
    ON autoevaluacion_ambito_actividad(autoevaluacion_ambito_id);
CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_act_actividad
    ON autoevaluacion_ambito_actividad(actividad_curricular_id);
