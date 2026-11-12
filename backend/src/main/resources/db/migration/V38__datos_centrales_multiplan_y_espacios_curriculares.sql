-- =============================================================================
-- Migration: V38__datos_centrales_multiplan_y_espacios_curriculares.sql
-- Description: Multi-plan study programs, master catalogs, decoupled snapshots,
--              and space-curricular activity binding.
-- =============================================================================

-- 1. Alter plan_estudio_ft001
ALTER TABLE plan_estudio_ft001
    ADD COLUMN IF NOT EXISTS nombre_plan VARCHAR(150),
    ADD COLUMN IF NOT EXISTS estado_vigencia VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    ADD COLUMN IF NOT EXISTS es_maestro BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_plan_estudio_estado_vigencia') THEN
        ALTER TABLE plan_estudio_ft001
            ADD CONSTRAINT chk_plan_estudio_estado_vigencia
            CHECK (estado_vigencia IN ('VIGENTE', 'NO_VIGENTE', 'HISTORICO'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_plan_estudio_origen_maestro') THEN
        ALTER TABLE plan_estudio_ft001
            ADD CONSTRAINT fk_plan_estudio_origen_maestro
            FOREIGN KEY (origen_maestro_id) REFERENCES plan_estudio_ft001(id) ON DELETE SET NULL;
    END IF;
END $$;

UPDATE plan_estudio_ft001
SET nombre_plan = COALESCE(NULLIF(TRIM(codigo_resolucion), ''), 'Plan ' || anio_aprobacion::text, 'Plan de Estudio')
WHERE nombre_plan IS NULL;

ALTER TABLE plan_estudio_ft001
    ALTER COLUMN nombre_plan SET NOT NULL;

UPDATE plan_estudio_ft001
SET es_maestro = FALSE
WHERE proceso_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_plan_estudio_es_maestro ON plan_estudio_ft001(es_maestro);
CREATE INDEX IF NOT EXISTS idx_plan_estudio_vigencia ON plan_estudio_ft001(carrera_id, estado_vigencia) WHERE es_maestro = TRUE;
CREATE INDEX IF NOT EXISTS idx_plan_estudio_origen_maestro ON plan_estudio_ft001(origen_maestro_id);

-- 2. Alter actividad_curricular_plan & programa_analitico_asignatura
ALTER TABLE actividad_curricular_plan
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_actividad_curricular_origen_maestro') THEN
        ALTER TABLE actividad_curricular_plan
            ADD CONSTRAINT fk_actividad_curricular_origen_maestro
            FOREIGN KEY (origen_maestro_id) REFERENCES actividad_curricular_plan(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_actividad_origen_maestro ON actividad_curricular_plan(origen_maestro_id);

ALTER TABLE programa_analitico_asignatura
    ADD COLUMN IF NOT EXISTS es_maestro BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_prog_analitico_origen_maestro') THEN
        ALTER TABLE programa_analitico_asignatura
            ADD CONSTRAINT fk_prog_analitico_origen_maestro
            FOREIGN KEY (origen_maestro_id) REFERENCES programa_analitico_asignatura(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_prog_analitico_es_maestro ON programa_analitico_asignatura(es_maestro);
CREATE INDEX IF NOT EXISTS idx_prog_analitico_origen_maestro ON programa_analitico_asignatura(origen_maestro_id);

-- 3. Alter inmueble & espacio_fisico
ALTER TABLE inmueble
    ADD COLUMN IF NOT EXISTS es_maestro BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS proceso_id UUID,
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inmueble_origen_maestro') THEN
        ALTER TABLE inmueble
            ADD CONSTRAINT fk_inmueble_origen_maestro
            FOREIGN KEY (origen_maestro_id) REFERENCES inmueble(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inmueble_origen_maestro ON inmueble(origen_maestro_id);

ALTER TABLE espacio_fisico
    ADD COLUMN IF NOT EXISTS es_maestro BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS proceso_id UUID,
    ADD COLUMN IF NOT EXISTS origen_maestro_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_espacio_fisico_proceso') THEN
        ALTER TABLE espacio_fisico
            ADD CONSTRAINT fk_espacio_fisico_proceso
            FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_espacio_fisico_origen_maestro') THEN
        ALTER TABLE espacio_fisico
            ADD CONSTRAINT fk_espacio_fisico_origen_maestro
            FOREIGN KEY (origen_maestro_id) REFERENCES espacio_fisico(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_espacio_fisico_es_maestro ON espacio_fisico(es_maestro);
CREATE INDEX IF NOT EXISTS idx_espacio_fisico_proceso_id ON espacio_fisico(proceso_id);
CREATE INDEX IF NOT EXISTS idx_espacio_fisico_origen_maestro ON espacio_fisico(origen_maestro_id);

-- 4. Alter autoevaluacion_carrera_ambito
ALTER TABLE autoevaluacion_carrera_ambito
    ADD COLUMN IF NOT EXISTS plan_estudio_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_autoeval_ambito_plan') THEN
        ALTER TABLE autoevaluacion_carrera_ambito
            ADD CONSTRAINT fk_autoeval_ambito_plan
            FOREIGN KEY (plan_estudio_id) REFERENCES plan_estudio_ft001(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_plan ON autoevaluacion_carrera_ambito(plan_estudio_id);

-- 5. Convocatoria Carrera Plan (Multi-Plan link)
CREATE TABLE IF NOT EXISTS convocatoria_carrera_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    convocatoria_carrera_id UUID NOT NULL REFERENCES convocatoria_carrera(id) ON DELETE CASCADE,
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    plan_estudio_maestro_id UUID NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE RESTRICT,
    plan_estudio_clon_id UUID REFERENCES plan_estudio_ft001(id) ON DELETE SET NULL,
    sugerido_por_aea BOOLEAN NOT NULL DEFAULT TRUE,
    seleccionado_por_de BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_convocatoria_carrera_plan UNIQUE (convocatoria_carrera_id, plan_estudio_maestro_id)
);

CREATE INDEX IF NOT EXISTS idx_conv_carrera_plan_cc ON convocatoria_carrera_plan(convocatoria_carrera_id);
CREATE INDEX IF NOT EXISTS idx_conv_carrera_plan_carrera ON convocatoria_carrera_plan(carrera_id);
CREATE INDEX IF NOT EXISTS idx_conv_carrera_plan_maestro ON convocatoria_carrera_plan(plan_estudio_maestro_id);
CREATE INDEX IF NOT EXISTS idx_conv_carrera_plan_clon ON convocatoria_carrera_plan(plan_estudio_clon_id);

-- 6. Cuerpo Académico Central (Master Faculty & Assignments)
CREATE TABLE IF NOT EXISTS cuerpo_academico_maestro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dni VARCHAR(20) NOT NULL,
    cuit VARCHAR(25),
    apellido VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    email_institucional VARCHAR(150),
    email_personal VARCHAR(150),
    telefono VARCHAR(50),
    titulo_grado VARCHAR(255),
    maximo_titulo_posgrado VARCHAR(255),
    categoria_docente VARCHAR(50) NOT NULL,
    caracter_designacion VARCHAR(50) NOT NULL,
    dedicacion_docente VARCHAR(50) NOT NULL,
    horas_semanales INT NOT NULL DEFAULT 10,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cuerpo_academico_maestro_dni UNIQUE (dni)
);

CREATE INDEX IF NOT EXISTS idx_cuerpo_academico_maestro_dni ON cuerpo_academico_maestro(dni);
CREATE INDEX IF NOT EXISTS idx_cuerpo_academico_maestro_categoria ON cuerpo_academico_maestro(categoria_docente);

CREATE TABLE IF NOT EXISTS cuerpo_academico_actividad_maestro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    docente_id UUID NOT NULL REFERENCES cuerpo_academico_maestro(id) ON DELETE CASCADE,
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE CASCADE,
    plan_estudio_id UUID NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE CASCADE,
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    rol_en_catedra VARCHAR(50) NOT NULL DEFAULT 'PROFESOR_A_CARGO',
    carga_horaria_semanal INT NOT NULL DEFAULT 4,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cuerpo_actividad_maestro UNIQUE (docente_id, plan_estudio_id, actividad_curricular_id)
);

CREATE INDEX IF NOT EXISTS idx_cuerpo_act_maestro_docente ON cuerpo_academico_actividad_maestro(docente_id);
CREATE INDEX IF NOT EXISTS idx_cuerpo_act_maestro_plan ON cuerpo_academico_actividad_maestro(plan_estudio_id);
CREATE INDEX IF NOT EXISTS idx_cuerpo_act_maestro_act ON cuerpo_academico_actividad_maestro(actividad_curricular_id);

-- 7. Proceso Cuerpo Académico (Decoupled Clones)
CREATE TABLE IF NOT EXISTS proceso_cuerpo_academico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    origen_maestro_id UUID REFERENCES cuerpo_academico_maestro(id) ON DELETE SET NULL,
    dni VARCHAR(20) NOT NULL,
    cuit VARCHAR(25),
    apellido VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    email_institucional VARCHAR(150),
    email_personal VARCHAR(150),
    telefono VARCHAR(50),
    titulo_grado VARCHAR(255),
    maximo_titulo_posgrado VARCHAR(255),
    categoria_docente VARCHAR(50) NOT NULL,
    caracter_designacion VARCHAR(50) NOT NULL,
    dedicacion_docente VARCHAR(50) NOT NULL,
    horas_semanales INT NOT NULL DEFAULT 10,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_proceso_cuerpo_academico_proc_dni UNIQUE (proceso_id, dni)
);

CREATE INDEX IF NOT EXISTS idx_proc_cuerpo_acad_proceso ON proceso_cuerpo_academico(proceso_id);
CREATE INDEX IF NOT EXISTS idx_proc_cuerpo_acad_origen ON proceso_cuerpo_academico(origen_maestro_id);

CREATE TABLE IF NOT EXISTS proceso_cuerpo_academico_actividad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    docente_id UUID NOT NULL REFERENCES proceso_cuerpo_academico(id) ON DELETE CASCADE,
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE CASCADE,
    plan_estudio_id UUID NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE CASCADE,
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    rol_en_catedra VARCHAR(50) NOT NULL DEFAULT 'PROFESOR_A_CARGO',
    carga_horaria_semanal INT NOT NULL DEFAULT 4,
    origen_maestro_id UUID REFERENCES cuerpo_academico_actividad_maestro(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_proc_cuerpo_actividad UNIQUE (proceso_id, docente_id, plan_estudio_id, actividad_curricular_id)
);

CREATE INDEX IF NOT EXISTS idx_proc_cuerpo_act_proc ON proceso_cuerpo_academico_actividad(proceso_id);
CREATE INDEX IF NOT EXISTS idx_proc_cuerpo_act_plan ON proceso_cuerpo_academico_actividad(plan_estudio_id);
CREATE INDEX IF NOT EXISTS idx_proc_cuerpo_act_act ON proceso_cuerpo_academico_actividad(actividad_curricular_id);

-- 8. Espacio Físico Actividad Curricular (Space -> Career -> Plan -> Course Binding)
CREATE TABLE IF NOT EXISTS espacio_fisico_actividad_curricular (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    espacio_fisico_id UUID NOT NULL REFERENCES espacio_fisico(id) ON DELETE CASCADE,
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    plan_estudio_id UUID NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE RESTRICT,
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    comisiones_asignadas INT NOT NULL DEFAULT 1,
    observaciones_uso TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_espacio_plan_actividad UNIQUE (espacio_fisico_id, plan_estudio_id, actividad_curricular_id)
);

CREATE INDEX IF NOT EXISTS idx_espacio_act_espacio ON espacio_fisico_actividad_curricular(espacio_fisico_id);
CREATE INDEX IF NOT EXISTS idx_espacio_act_carrera ON espacio_fisico_actividad_curricular(carrera_id);
CREATE INDEX IF NOT EXISTS idx_espacio_act_plan ON espacio_fisico_actividad_curricular(plan_estudio_id);
CREATE INDEX IF NOT EXISTS idx_espacio_act_actividad ON espacio_fisico_actividad_curricular(actividad_curricular_id);

-- 9. Historical Convocatorias Backfill into convocatoria_carrera_plan
INSERT INTO convocatoria_carrera_plan (
    convocatoria_carrera_id, carrera_id, plan_estudio_maestro_id,
    plan_estudio_clon_id, sugerido_por_aea, seleccionado_por_de
)
SELECT
    cc.id,
    cc.carrera_id,
    p.id AS plan_estudio_maestro_id,
    CASE WHEN cc.proceso_id IS NOT NULL THEN (
        SELECT p_clon.id FROM plan_estudio_ft001 p_clon
        WHERE p_clon.proceso_id = cc.proceso_id
        ORDER BY p_clon.creado_en DESC LIMIT 1
    ) ELSE NULL END AS plan_estudio_clon_id,
    COALESCE(cc.seleccionada_por_aea, TRUE),
    COALESCE(cc.seleccionada_por_de, TRUE)
FROM convocatoria_carrera cc
JOIN plan_estudio_ft001 p ON p.carrera_id = cc.carrera_id AND p.es_maestro = TRUE
ON CONFLICT (convocatoria_carrera_id, plan_estudio_maestro_id) DO NOTHING;
