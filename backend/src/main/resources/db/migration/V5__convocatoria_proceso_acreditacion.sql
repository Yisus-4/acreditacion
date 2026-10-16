-- =============================================================================
-- V5__convocatoria_proceso_acreditacion.sql
-- Inicio de Convocatoria y Actividad "Informar sobre el proceso de acreditación"
-- (Bloque 2 - Sistema de Acreditación UNdeC)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Tabla: proceso_acreditacion
-- ----------------------------------------------------------------------------
CREATE TABLE proceso_acreditacion (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    carrera_id     UUID          NOT NULL,
    codigo         VARCHAR(50)   NOT NULL,
    titulo         VARCHAR(255)  NOT NULL,
    tipo_proceso   VARCHAR(50)   NOT NULL,
    estado         VARCHAR(50)   NOT NULL DEFAULT 'INICIADO',
    creado_por     UUID          NOT NULL,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_proceso_acreditacion PRIMARY KEY (id),
    CONSTRAINT fk_proceso_acreditacion_carrera
        FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE RESTRICT,
    CONSTRAINT fk_proceso_acreditacion_creado_por
        FOREIGN KEY (creado_por) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_proceso_tipo_proceso
        CHECK (tipo_proceso IN ('NUEVA', 'EN_FUNCIONAMIENTO')),
    CONSTRAINT ck_proceso_estado
        CHECK (estado IN ('INICIADO', 'EN_AUTOEVALUACION', 'FINALIZADO', 'CANCELADO'))
);

CREATE UNIQUE INDEX uq_proceso_acreditacion_codigo ON proceso_acreditacion (codigo);
CREATE INDEX ix_proceso_acreditacion_carrera_id ON proceso_acreditacion (carrera_id);
CREATE INDEX ix_proceso_acreditacion_estado ON proceso_acreditacion (estado);

-- Regla de Negocio: Máximo un proceso de acreditación activo por carrera simultáneamente
CREATE UNIQUE INDEX uq_proceso_carrera_activa 
    ON proceso_acreditacion (carrera_id) 
    WHERE estado IN ('INICIADO', 'EN_AUTOEVALUACION');

-- ----------------------------------------------------------------------------
-- 2. Tabla: registro_convocatoria (Metadatos CONEAU RG-001)
-- ----------------------------------------------------------------------------
CREATE TABLE registro_convocatoria (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    proceso_id            UUID          NOT NULL,
    nro_resolucion_coneau VARCHAR(100)  NOT NULL,
    fecha_inicio          DATE          NOT NULL,
    fecha_cierre          DATE          NOT NULL,
    observaciones         TEXT,
    creado_en             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_registro_convocatoria PRIMARY KEY (id),
    CONSTRAINT fk_registro_convocatoria_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT uq_registro_convocatoria_proceso UNIQUE (proceso_id),
    CONSTRAINT ck_registro_convocatoria_fechas CHECK (fecha_cierre >= fecha_inicio)
);

CREATE INDEX ix_registro_convocatoria_proceso_id ON registro_convocatoria (proceso_id);

-- ----------------------------------------------------------------------------
-- 3. Tabla: plan_trabajo (Estructura Flexible e Incremental)
-- ----------------------------------------------------------------------------
CREATE TABLE plan_trabajo (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    proceso_id     UUID          NOT NULL,
    codigo         VARCHAR(50)   NOT NULL,
    tipo           VARCHAR(50)   NOT NULL,
    estado         VARCHAR(50)   NOT NULL DEFAULT 'ACTIVO',
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_plan_trabajo PRIMARY KEY (id),
    CONSTRAINT fk_plan_trabajo_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT uq_plan_trabajo_proceso UNIQUE (proceso_id),
    CONSTRAINT ck_plan_trabajo_tipo
        CHECK (tipo IN ('PT_001_ACREDITACION', 'PT_003_SEGUIMIENTO')),
    CONSTRAINT ck_plan_trabajo_estado
        CHECK (estado IN ('ACTIVO', 'FINALIZADO'))
);

CREATE UNIQUE INDEX uq_plan_trabajo_codigo ON plan_trabajo (codigo);
CREATE INDEX ix_plan_trabajo_proceso_id ON plan_trabajo (proceso_id);

-- ----------------------------------------------------------------------------
-- 4. Tabla: actividad_plan_trabajo
-- ----------------------------------------------------------------------------
CREATE TABLE actividad_plan_trabajo (
    id                         UUID          NOT NULL DEFAULT gen_random_uuid(),
    plan_trabajo_id            UUID          NOT NULL,
    numero_orden               INT           NOT NULL,
    codigo_actividad           VARCHAR(50)   NOT NULL,
    nombre                     VARCHAR(255)  NOT NULL,
    estado                     VARCHAR(50)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_reunion_coordinacion TIMESTAMPTZ,
    sintesis_acuerdos          TEXT,
    completada_en              TIMESTAMPTZ,
    completada_por             UUID,
    creado_en                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en             TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_actividad_plan_trabajo PRIMARY KEY (id),
    CONSTRAINT fk_actividad_plan_trabajo_plan
        FOREIGN KEY (plan_trabajo_id) REFERENCES plan_trabajo (id) ON DELETE CASCADE,
    CONSTRAINT fk_actividad_completada_por
        FOREIGN KEY (completada_por) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT uq_actividad_plan_orden UNIQUE (plan_trabajo_id, numero_orden),
    CONSTRAINT ck_actividad_estado
        CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'COMPLETADA', 'CANCELADA')),
    CONSTRAINT ck_actividad_completada
        CHECK (
            (estado <> 'COMPLETADA') OR (
                fecha_reunion_coordinacion IS NOT NULL AND
                sintesis_acuerdos IS NOT NULL AND
                completada_en IS NOT NULL AND
                completada_por IS NOT NULL
            )
        )
);

CREATE INDEX ix_actividad_plan_trabajo_plan_id ON actividad_plan_trabajo (plan_trabajo_id);
CREATE INDEX ix_actividad_plan_trabajo_estado ON actividad_plan_trabajo (estado);

-- ----------------------------------------------------------------------------
-- 5. Tabla: documento_acreditacion (Metadatos MinIO)
-- ----------------------------------------------------------------------------
CREATE TABLE documento_acreditacion (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    proceso_id      UUID          NOT NULL,
    actividad_id    UUID,
    nombre_original VARCHAR(255)  NOT NULL,
    tipo_documento  VARCHAR(100)  NOT NULL,
    mime_type       VARCHAR(100)  NOT NULL DEFAULT 'application/pdf',
    tamanio_bytes   BIGINT        NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    subido_por      UUID          NOT NULL,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_documento_acreditacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_acreditacion_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_documento_acreditacion_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividad_plan_trabajo (id) ON DELETE SET NULL,
    CONSTRAINT fk_documento_subido_por
        FOREIGN KEY (subido_por) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_documento_tipo
        CHECK (tipo_documento IN ('NORMATIVA_CONVOCATORIA', 'ACTA_REUNION_COORDINACION')),
    CONSTRAINT ck_documento_mime_pdf
        CHECK (mime_type = 'application/pdf'),
    CONSTRAINT ck_documento_tamanio
        CHECK (tamanio_bytes > 0 AND tamanio_bytes <= 20971520) -- Máximo 20 MB
);

CREATE INDEX ix_documento_acreditacion_proceso_id ON documento_acreditacion (proceso_id);
CREATE INDEX ix_documento_acreditacion_actividad_id ON documento_acreditacion (actividad_id);

-- ----------------------------------------------------------------------------
-- 6. Semilla de Seguridad: Rol AEA y Permisos del Módulo de Convocatorias
-- ----------------------------------------------------------------------------
INSERT INTO rol (codigo, nombre, descripcion, es_sistema) VALUES
    ('AEA',
     'Área de Evaluación y Acreditación',
     'Personal del Área de Evaluación y Acreditación responsable de convocatorias y planes de trabajo.',
     TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO permiso (codigo, nombre, descripcion) VALUES
    ('CONVOCATORIA_READ',
     'Consultar convocatorias',
     'Permite consultar convocatorias de acreditación, registros RG-001 y planes de trabajo.'),
    ('CONVOCATORIA_WRITE',
     'Crear y editar convocatorias',
     'Permite abrir convocatorias de acreditación y registrar metadatos RG-001.'),
    ('ACTIVIDAD_MANAGE',
     'Gestionar actividades de acreditación',
     'Permite registrar reuniones, acuerdos y cargar documentación en actividades del plan de trabajo.')
ON CONFLICT (codigo) DO NOTHING;

-- Asignación de permisos al rol ADMINISTRATOR
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('CONVOCATORIA_READ', 'CONVOCATORIA_WRITE', 'ACTIVIDAD_MANAGE')
WHERE r.codigo = 'ADMINISTRATOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignación de permisos al rol AEA
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('CONVOCATORIA_READ', 'CONVOCATORIA_WRITE', 'ACTIVIDAD_MANAGE')
WHERE r.codigo = 'AEA'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignación de permisos al rol AC_COORDINATOR (Coordinador de Acreditación histórico)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('CONVOCATORIA_READ', 'CONVOCATORIA_WRITE', 'ACTIVIDAD_MANAGE')
WHERE r.codigo = 'AC_COORDINATOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignación de lectura al rol CAREER_DIRECTOR (Director de Carrera)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('CONVOCATORIA_READ')
WHERE r.codigo = 'CAREER_DIRECTOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
