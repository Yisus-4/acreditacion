-- Designación de docente autorizado por actividad curricular para subir Programas Analíticos (PDF)
ALTER TABLE programa_analitico_asignatura
    ADD COLUMN IF NOT EXISTS docente_designado_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS docente_designado_nombre VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_programa_analitico_docente_designado
    ON programa_analitico_asignatura(docente_designado_id);
