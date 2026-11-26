-- =============================================================================
-- V48__departamentos_academicos_catalogos_docentes_y_estados_asignatura.sql
-- Fase 1: Sistema de Designación y Asignación Docente UNdeC
-- 1. Roles canónicos: SECRETARIA_ACADEMICA y CONSULTA_INSTITUCIONAL
-- 2. Tabla departamento_academico + vínculo en usuario
-- 3. Catálogos parametrizables: cargo_docente, dedicacion_docente,
--    tipo_designacion_docente y periodo_academico
-- 4. Extensión de cuerpo_academico_maestro (RN01, RF01.1-RF01.6)
-- 5. Extensión de carrera (codigo) y actividad_curricular_plan
--    (3 estados RN09: ACTIVA, SOLO_EXAMENES_FINALES, INACTIVA + horas_semanales)
-- =============================================================================

-- 1. Roles institucionales nuevos
INSERT INTO rol (codigo, nombre, descripcion, es_sistema, activo)
VALUES
    ('SECRETARIA_ACADEMICA',
     'Secretaría de Gestión Académica',
     'Recibe, controla, observa y aprueba las solicitudes de designación y asignación docente emitidas por los Departamentos.',
     TRUE, TRUE),
    ('CONSULTA_INSTITUCIONAL',
     'Consulta Institucional',
     'Acceso de solo lectura a reportes consolidados de planta docente, cobertura curricular y comparativos por período.',
     TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET es_sistema = TRUE,
    activo = TRUE;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo IN ('SECRETARIA_ACADEMICA', 'CONSULTA_INSTITUCIONAL', 'DIRECTOR_DEPARTAMENTO')
  AND p.codigo = 'ESTRUCTURA_ACADEMICA_READ'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- 2. Tabla departamento_academico (3.2, RF02.1, RN01)
CREATE TABLE IF NOT EXISTS departamento_academico (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    institucion_id UUID          REFERENCES institucion(id) ON DELETE SET NULL,
    codigo         VARCHAR(50)   NOT NULL,
    denominacion   VARCHAR(255)  NOT NULL,
    descripcion    TEXT,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    es_sistema     BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_departamento_academico_codigo UNIQUE (codigo)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_departamento_academico_codigo_ci
    ON departamento_academico (UPPER(codigo));

INSERT INTO departamento_academico (institucion_id, codigo, denominacion, descripcion, activo, es_sistema)
VALUES
    ((SELECT id FROM institucion ORDER BY creado_en ASC LIMIT 1),
     'SIN_ASIGNAR',
     'Sin asignar / A clasificar',
     'Departamento inicial transitorio para reclasificar docentes preexistentes.',
     TRUE, TRUE),
    ((SELECT id FROM institucion ORDER BY creado_en ASC LIMIT 1),
     'CBASICAS',
     'Departamento de Ciencias Básicas y Tecnológicas',
     'Departamento Académico UNdeC.',
     TRUE, FALSE),
    ((SELECT id FROM institucion ORDER BY creado_en ASC LIMIT 1),
     'CSOCIALES',
     'Departamento de Ciencias Sociales, Jurídicas y Económicas',
     'Departamento Académico UNdeC.',
     TRUE, FALSE),
    ((SELECT id FROM institucion ORDER BY creado_en ASC LIMIT 1),
     'EDUCACION_SALUD',
     'Departamento de Ciencias de la Educación y de la Salud',
     'Departamento Académico UNdeC.',
     TRUE, FALSE)
ON CONFLICT (codigo) DO NOTHING;

-- 3. Vínculo de usuario con departamento_academico (RNF01)
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS departamento_academico_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario_departamento_academico') THEN
        ALTER TABLE usuario
            ADD CONSTRAINT fk_usuario_departamento_academico
            FOREIGN KEY (departamento_academico_id) REFERENCES departamento_academico(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_usuario_departamento_academico_id
    ON usuario (departamento_academico_id);

-- 4. Catálogos parametrizables docentes (3.3, 3.4, RF03.1-03.2, RF10.3, RN03-RN04, RNF06)
CREATE TABLE IF NOT EXISTS cargo_docente (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo         VARCHAR(50)   NOT NULL,
    nombre         VARCHAR(150)  NOT NULL,
    orden          INT           NOT NULL DEFAULT 1,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_cargo_docente_codigo UNIQUE (codigo)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cargo_docente_codigo_ci
    ON cargo_docente (UPPER(codigo));

INSERT INTO cargo_docente (codigo, nombre, orden, activo)
VALUES
    ('TITULAR',  'Profesor Titular',           1, TRUE),
    ('ASOCIADO', 'Profesor Asociado',          2, TRUE),
    ('ADJUNTO',  'Profesor Adjunto',           3, TRUE),
    ('JTP',      'Jefe de Trabajos Prácticos', 4, TRUE),
    ('AUXILIAR', 'Auxiliar Docente',           5, TRUE)
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS dedicacion_docente (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo          VARCHAR(50)   NOT NULL,
    nombre          VARCHAR(150)  NOT NULL,
    horas_semanales INT           NOT NULL CHECK (horas_semanales > 0 AND horas_semanales <= 50),
    orden           INT           NOT NULL DEFAULT 1,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_dedicacion_docente_codigo UNIQUE (codigo)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dedicacion_docente_codigo_ci
    ON dedicacion_docente (UPPER(codigo));

INSERT INTO dedicacion_docente (codigo, nombre, horas_semanales, orden, activo)
VALUES
    ('EXCLUSIVA', 'Exclusiva', 40, 1, TRUE),
    ('PARCIAL',   'Parcial',   20, 2, TRUE),
    ('SIMPLE',    'Simple',    10, 3, TRUE)
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS tipo_designacion_docente (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo         VARCHAR(50)   NOT NULL,
    nombre         VARCHAR(150)  NOT NULL,
    es_rentado     BOOLEAN       NOT NULL DEFAULT TRUE,
    orden          INT           NOT NULL DEFAULT 1,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_tipo_designacion_docente_codigo UNIQUE (codigo)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tipo_designacion_docente_codigo_ci
    ON tipo_designacion_docente (UPPER(codigo));

INSERT INTO tipo_designacion_docente (codigo, nombre, es_rentado, orden, activo)
VALUES
    ('INTERINO_RENTADO',    'Interino Rentado',    TRUE,  1, TRUE),
    ('INTERINO_AD_HONOREM', 'Interino ad honorem', FALSE, 2, TRUE),
    ('REGULAR_RENTADO',     'Regular Rentado',     TRUE,  3, TRUE),
    ('REGULAR_AD_HONOREM',  'Regular ad honorem',  FALSE, 4, TRUE),
    ('CONTRATADO',          'Contratado',          TRUE,  5, TRUE)
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS periodo_academico (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo         VARCHAR(30)   NOT NULL,
    denominacion   VARCHAR(150)  NOT NULL,
    anio           INT           NOT NULL,
    fecha_inicio   DATE,
    fecha_fin      DATE,
    es_actual      BOOLEAN       NOT NULL DEFAULT FALSE,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_periodo_academico_codigo UNIQUE (codigo)
);

INSERT INTO periodo_academico (codigo, denominacion, anio, fecha_inicio, fecha_fin, es_actual, activo)
VALUES
    ('2026', 'Ciclo Lectivo 2026', 2026, DATE '2026-03-01', DATE '2026-12-31', TRUE, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- 5. Extensión de cuerpo_academico_maestro (3.1, RF01.1-RF01.6, RN01)
ALTER TABLE cuerpo_academico_maestro
    ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(20) NOT NULL DEFAULT 'DNI',
    ADD COLUMN IF NOT EXISTS legajo VARCHAR(50),
    ADD COLUMN IF NOT EXISTS nro_designacion_mapuche VARCHAR(80),
    ADD COLUMN IF NOT EXISTS departamento_academico_id UUID;

UPDATE cuerpo_academico_maestro
SET departamento_academico_id = (SELECT id FROM departamento_academico WHERE codigo = 'SIN_ASIGNAR' LIMIT 1)
WHERE departamento_academico_id IS NULL;

ALTER TABLE cuerpo_academico_maestro
    ALTER COLUMN departamento_academico_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cuerpo_academico_departamento') THEN
        ALTER TABLE cuerpo_academico_maestro
            ADD CONSTRAINT fk_cuerpo_academico_departamento
            FOREIGN KEY (departamento_academico_id) REFERENCES departamento_academico(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cuerpo_academico_departamento
    ON cuerpo_academico_maestro (departamento_academico_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cuerpo_academico_legajo
    ON cuerpo_academico_maestro (UPPER(BTRIM(legajo)))
    WHERE legajo IS NOT NULL AND BTRIM(legajo) <> '';

-- 6. Extensión de carrera (3.5)
ALTER TABLE carrera
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(50);

UPDATE carrera
SET codigo = sigla
WHERE codigo IS NULL AND sigla IS NOT NULL;

-- 7. Extensión de actividad_curricular_plan (3.7, RF02.6, RF02.9, RN09)
ALTER TABLE actividad_curricular_plan
    DROP CONSTRAINT IF EXISTS chk_actividad_curricular_estado_dictado;

ALTER TABLE actividad_curricular_plan
    ALTER COLUMN estado_dictado TYPE VARCHAR(30);

UPDATE actividad_curricular_plan
SET estado_dictado = 'ACTIVA'
WHERE estado_dictado = 'SE_DICTA';

UPDATE actividad_curricular_plan
SET estado_dictado = 'INACTIVA'
WHERE estado_dictado = 'NO_SE_DICTA';

ALTER TABLE actividad_curricular_plan
    ALTER COLUMN estado_dictado SET DEFAULT 'ACTIVA';

ALTER TABLE actividad_curricular_plan
    ADD CONSTRAINT chk_actividad_curricular_estado_dictado
    CHECK (estado_dictado IN ('ACTIVA', 'SOLO_EXAMENES_FINALES', 'INACTIVA'));

ALTER TABLE actividad_curricular_plan
    ADD COLUMN IF NOT EXISTS horas_semanales INT;

UPDATE actividad_curricular_plan a
SET horas_semanales = COALESCE(r.horas_semanal_total_presencial, 0) + COALESCE(r.horas_semanal_total_distancia, 0)
FROM actividad_curricular_carga_horaria_resumen r
WHERE r.actividad_curricular_id = a.id
  AND a.horas_semanales IS NULL
  AND (COALESCE(r.horas_semanal_total_presencial, 0) + COALESCE(r.horas_semanal_total_distancia, 0)) > 0;
