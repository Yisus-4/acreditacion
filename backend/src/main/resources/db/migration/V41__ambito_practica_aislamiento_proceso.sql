ALTER TABLE ambito_practica
    ADD COLUMN IF NOT EXISTS es_maestro BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS proceso_id UUID REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID REFERENCES ambito_practica(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ambito_practica_proceso_id ON ambito_practica(proceso_id);
CREATE INDEX IF NOT EXISTS idx_ambito_practica_es_maestro_activo ON ambito_practica(es_maestro, activo);
