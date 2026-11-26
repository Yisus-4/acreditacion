-- =============================================================================
-- V49__solicitudes_designacion_docente_y_distribucion_horaria.sql
-- Fase 2: Sistema de Designación y Asignación Docente UNdeC
-- 1. solicitud_designacion (Anual / Extraordinaria por Departamento y Período)
-- 2. solicitud_docente_designacion (Múltiples cargos <= 50h RN02 + Distribución 6 actividades RF04/RN06)
-- 3. solicitud_asignacion_curricular (Clases vs. Exámenes Finales RF05/RF06.4/RN09)
-- =============================================================================

CREATE TABLE IF NOT EXISTS solicitud_designacion (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    departamento_academico_id UUID          NOT NULL REFERENCES departamento_academico(id) ON DELETE RESTRICT,
    periodo_academico_id      UUID          NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    tipo_solicitud            VARCHAR(25)   NOT NULL DEFAULT 'ANUAL'
        CHECK (tipo_solicitud IN ('ANUAL', 'EXTRAORDINARIA')),
    estado                    VARCHAR(25)   NOT NULL DEFAULT 'BORRADOR'
        CHECK (estado IN ('BORRADOR', 'PRESENTADA_SGA', 'APROBADA', 'OBSERVADA')),
    solicitud_origen_id       UUID          REFERENCES solicitud_designacion(id) ON DELETE SET NULL,
    observaciones_departamento TEXT,
    observaciones_sga         TEXT,
    fecha_emision             TIMESTAMPTZ,
    emitida_por_usuario_id    UUID          REFERENCES usuario(id) ON DELETE SET NULL,
    revisada_por_usuario_id   UUID          REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_revision            TIMESTAMPTZ,
    creado_en                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_anual_dpto_periodo
    ON solicitud_designacion (departamento_academico_id, periodo_academico_id)
    WHERE tipo_solicitud = 'ANUAL';

CREATE INDEX IF NOT EXISTS idx_solicitud_designacion_dpto
    ON solicitud_designacion (departamento_academico_id);

CREATE INDEX IF NOT EXISTS idx_solicitud_designacion_periodo
    ON solicitud_designacion (periodo_academico_id);

CREATE TABLE IF NOT EXISTS solicitud_docente_designacion (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_id            UUID          NOT NULL REFERENCES solicitud_designacion(id) ON DELETE CASCADE,
    docente_id              UUID          NOT NULL REFERENCES cuerpo_academico_maestro(id) ON DELETE RESTRICT,
    cargo_docente_id        UUID          NOT NULL REFERENCES cargo_docente(id) ON DELETE RESTRICT,
    dedicacion_docente_id   UUID          NOT NULL REFERENCES dedicacion_docente(id) ON DELETE RESTRICT,
    tipo_designacion_id     UUID          NOT NULL REFERENCES tipo_designacion_docente(id) ON DELETE RESTRICT,
    horas_dedicacion        INT           NOT NULL CHECK (horas_dedicacion > 0 AND horas_dedicacion <= 50),
    nro_designacion_mapuche VARCHAR(80),
    estado_item             VARCHAR(25)   NOT NULL DEFAULT 'NUEVO'
        CHECK (estado_item IN ('NUEVO', 'REPLICADO', 'MODIFICADO', 'EXCLUIDO')),
    horas_clases            INT           NOT NULL DEFAULT 0 CHECK (horas_clases >= 0),
    horas_investigacion     INT           NOT NULL DEFAULT 0 CHECK (horas_investigacion >= 0),
    horas_extension         INT           NOT NULL DEFAULT 0 CHECK (horas_extension >= 0),
    horas_gestion           INT           NOT NULL DEFAULT 0 CHECK (horas_gestion >= 0),
    horas_direccion_tesis   INT           NOT NULL DEFAULT 0 CHECK (horas_direccion_tesis >= 0),
    horas_otras             INT           NOT NULL DEFAULT 0 CHECK (horas_otras >= 0),
    observaciones           TEXT,
    creado_en               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_distribucion_no_supera_dedicacion CHECK (
        (horas_clases + horas_investigacion + horas_extension + horas_gestion + horas_direccion_tesis + horas_otras) <= horas_dedicacion
    )
);

CREATE INDEX IF NOT EXISTS idx_sol_doc_desig_solicitud
    ON solicitud_docente_designacion (solicitud_id);

CREATE INDEX IF NOT EXISTS idx_sol_doc_desig_docente
    ON solicitud_docente_designacion (docente_id);

CREATE TABLE IF NOT EXISTS solicitud_asignacion_curricular (
    id                       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    designacion_id           UUID          NOT NULL REFERENCES solicitud_docente_designacion(id) ON DELETE CASCADE,
    carrera_id               UUID          NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    plan_estudio_id          UUID          NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE RESTRICT,
    actividad_curricular_id  UUID          NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE RESTRICT,
    modalidad_participacion  VARCHAR(30)   NOT NULL DEFAULT 'DESARROLLO_CLASES'
        CHECK (modalidad_participacion IN ('DESARROLLO_CLASES', 'EVALUACION_FINALES')),
    horas_asignadas          INT           CHECK (horas_asignadas IS NULL OR horas_asignadas >= 0),
    observaciones            TEXT,
    creado_en                TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_solicitud_asignacion_curricular
        UNIQUE (designacion_id, actividad_curricular_id, modalidad_participacion)
);

CREATE INDEX IF NOT EXISTS idx_sol_asig_curr_designacion
    ON solicitud_asignacion_curricular (designacion_id);

CREATE INDEX IF NOT EXISTS idx_sol_asig_curr_actividad
    ON solicitud_asignacion_curricular (actividad_curricular_id);
