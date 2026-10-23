-- =============================================================================
-- V12__convocatoria_escuela_seleccion_carreras.sql
-- Convocatoria por Escuela (Unidad Académica) con selección múltiple de carreras,
-- asignación de obligatoriedad por AEA y confirmación por el Director de Escuela.
-- =============================================================================

-- 1. Tabla principal: convocatoria (a nivel de Escuela / Unidad Académica)
CREATE TABLE IF NOT EXISTS convocatoria (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unidad_academica_id    UUID NOT NULL REFERENCES unidad_academica(id) ON DELETE RESTRICT,
    codigo                 VARCHAR(50) NOT NULL UNIQUE,
    titulo                 VARCHAR(255) NOT NULL,
    tipo_proceso           VARCHAR(50) NOT NULL DEFAULT 'EN_FUNCIONAMIENTO',
    nro_resolucion_coneau  VARCHAR(100),
    fecha_inicio           DATE NOT NULL,
    fecha_cierre           DATE NOT NULL,
    estado                 VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE_DIRECTOR_ESCUELA',
    observaciones          TEXT,
    creado_por             UUID NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    confirmado_por_de      UUID REFERENCES usuario(id) ON DELETE SET NULL,
    confirmado_por_de_en   TIMESTAMPTZ,
    creado_en              TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_convocatoria_fechas CHECK (fecha_cierre >= fecha_inicio),
    CONSTRAINT ck_convocatoria_tipo CHECK (tipo_proceso IN ('NUEVA', 'EN_FUNCIONAMIENTO')),
    CONSTRAINT ck_convocatoria_estado CHECK (estado IN (
        'PENDIENTE_DIRECTOR_ESCUELA',
        'CONFIRMADA_DIRECTOR_ESCUELA',
        'EN_CURSO',
        'FINALIZADA',
        'CANCELADA'
    ))
);

-- 2. Tabla de items: convocatoria_carrera (carreras propuestas por AEA y decididas por DE)
CREATE TABLE IF NOT EXISTS convocatoria_carrera (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    convocatoria_id        UUID NOT NULL REFERENCES convocatoria(id) ON DELETE CASCADE,
    carrera_id             UUID NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    seleccionada_por_aea   BOOLEAN NOT NULL DEFAULT TRUE,
    es_obligatoria         BOOLEAN NOT NULL DEFAULT FALSE,
    seleccionada_por_de    BOOLEAN NOT NULL DEFAULT FALSE,
    proceso_id             UUID REFERENCES proceso_acreditacion(id) ON DELETE SET NULL,
    creado_en              TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_convocatoria_carrera UNIQUE (convocatoria_id, carrera_id)
);

CREATE INDEX IF NOT EXISTS ix_convocatoria_unidad_academica ON convocatoria (unidad_academica_id);
CREATE INDEX IF NOT EXISTS ix_convocatoria_estado ON convocatoria (estado);
CREATE INDEX IF NOT EXISTS ix_convocatoria_carrera_convocatoria ON convocatoria_carrera (convocatoria_id);
CREATE INDEX IF NOT EXISTS ix_convocatoria_carrera_carrera ON convocatoria_carrera (carrera_id);
CREATE INDEX IF NOT EXISTS ix_convocatoria_carrera_proceso ON convocatoria_carrera (proceso_id);

-- 3. Vincular proceso_acreditacion con convocatoria_id si aplica
ALTER TABLE proceso_acreditacion
    ADD COLUMN IF NOT EXISTS convocatoria_id UUID REFERENCES convocatoria(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_proceso_acreditacion_convocatoria_id ON proceso_acreditacion (convocatoria_id);
