-- =============================================================================
-- V7__gestion_normativa_carrera_actividad_2.sql
-- Gestión de Normativa de la Carrera (Actividad 2 - PT-001)
-- Sistema de Acreditación UNdeC (Nivel: GRADO)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Tabla: solicitud_normativa_carrera
-- ----------------------------------------------------------------------------
CREATE TABLE solicitud_normativa_carrera (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid(),
    proceso_id              UUID          NOT NULL,
    actividad_id            UUID          NOT NULL,
    estado                  VARCHAR(50)   NOT NULL DEFAULT 'PENDIENTE_SOLICITUD',
    fecha_solicitud         TIMESTAMPTZ,
    solicitado_por          UUID,
    fecha_limite_entrega    DATE,
    dias_habiles_plazo      INT           NOT NULL DEFAULT 5,
    fecha_envio             TIMESTAMPTZ,
    enviado_por             UUID,
    cumplimiento_plazo      VARCHAR(30),
    observaciones_solicitud TEXT,
    observaciones_revision  TEXT,
    fecha_revision          TIMESTAMPTZ,
    revisado_por            UUID,
    creado_en               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_solicitud_normativa PRIMARY KEY (id),
    CONSTRAINT fk_solicitud_normativa_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_normativa_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividad_plan_trabajo (id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_normativa_solicitado_por
        FOREIGN KEY (solicitado_por) REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT fk_solicitud_normativa_enviado_por
        FOREIGN KEY (enviado_por) REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT fk_solicitud_normativa_revisado_por
        FOREIGN KEY (revisado_por) REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT uq_solicitud_normativa_actividad UNIQUE (actividad_id),
    CONSTRAINT ck_solicitud_normativa_estado
        CHECK (estado IN ('PENDIENTE_SOLICITUD', 'SOLICITADA', 'EN_CARGA_BORRADOR', 'ENVIADA_A_REVISION', 'OBSERVADA', 'COMPLETADA')),
    CONSTRAINT ck_solicitud_normativa_cumplimiento
        CHECK (cumplimiento_plazo IS NULL OR cumplimiento_plazo IN ('EN_TERMINO', 'FUERA_DE_TERMINO')),
    CONSTRAINT ck_solicitud_normativa_dias_habiles
        CHECK (dias_habiles_plazo > 0)
);

CREATE INDEX ix_solicitud_normativa_proceso ON solicitud_normativa_carrera (proceso_id);
CREATE INDEX ix_solicitud_normativa_actividad ON solicitud_normativa_carrera (actividad_id);
CREATE INDEX ix_solicitud_normativa_estado ON solicitud_normativa_carrera (estado);

-- ----------------------------------------------------------------------------
-- 2. Tabla: documento_normativa
-- ----------------------------------------------------------------------------
CREATE TABLE documento_normativa (
    id                     UUID          NOT NULL DEFAULT gen_random_uuid(),
    solicitud_normativa_id UUID          NOT NULL,
    proceso_id             UUID          NOT NULL,
    tipo_documento         VARCHAR(100)  NOT NULL,
    carpeta_destino        VARCHAR(255)  NOT NULL,
    nombre_original        VARCHAR(255)  NOT NULL,
    storage_path           VARCHAR(500)  NOT NULL,
    mime_type              VARCHAR(100)  NOT NULL DEFAULT 'application/pdf',
    tamanio_bytes          BIGINT        NOT NULL,
    es_borrador            BOOLEAN       NOT NULL DEFAULT TRUE,
    observado              BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_observacion     TEXT,
    subido_por             UUID          NOT NULL,
    creado_en              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_documento_normativa PRIMARY KEY (id),
    CONSTRAINT fk_documento_normativa_solicitud
        FOREIGN KEY (solicitud_normativa_id) REFERENCES solicitud_normativa_carrera (id) ON DELETE CASCADE,
    CONSTRAINT fk_documento_normativa_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_documento_normativa_subido_por
        FOREIGN KEY (subido_por) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_documento_normativa_tipo CHECK (tipo_documento IN (
        'RM_ESTANDARES_CARRERA',
        'RM_INCLUSION_ART_43',
        'ORDENANZA_PLAN_ESTUDIOS',
        'RESOLUCION_DIRECTOR_CARRERA',
        'RESOLUCION_COMISION_CURRICULAR',
        'ACTA_COMISION_CURRICULAR',
        'RESOLUCION_CONEAU_ANTERIOR',
        'PLAN_MEJORAS_PREVIO',
        'INFORME_CUMPLIMIENTO_PREVIO'
    )),
    CONSTRAINT ck_documento_normativa_mime CHECK (mime_type = 'application/pdf'),
    CONSTRAINT ck_documento_normativa_tamanio CHECK (tamanio_bytes > 0 AND tamanio_bytes <= 20971520)
);

CREATE INDEX ix_documento_normativa_solicitud ON documento_normativa (solicitud_normativa_id);
CREATE INDEX ix_documento_normativa_proceso ON documento_normativa (proceso_id);
CREATE INDEX ix_documento_normativa_tipo ON documento_normativa (tipo_documento);
CREATE INDEX ix_documento_normativa_borrador ON documento_normativa (es_borrador);

-- ----------------------------------------------------------------------------
-- 3. Tabla: notificacion_expediente
-- ----------------------------------------------------------------------------
CREATE TABLE notificacion_expediente (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid(),
    proceso_id           UUID          NOT NULL,
    tipo_evento          VARCHAR(50)   NOT NULL,
    destinatario_email   VARCHAR(150)  NOT NULL,
    destinatario_nombre  VARCHAR(150),
    asunto               VARCHAR(255)  NOT NULL,
    enviado_exitosamente BOOLEAN       NOT NULL DEFAULT FALSE,
    error_detalle        TEXT,
    enviado_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_notificacion_expediente PRIMARY KEY (id),
    CONSTRAINT fk_notificacion_expediente_proceso
        FOREIGN KEY (proceso_id) REFERENCES proceso_acreditacion (id) ON DELETE CASCADE,
    CONSTRAINT ck_notificacion_evento CHECK (tipo_evento IN (
        'SOLICITUD_NORMATIVA_DC',
        'ENVIO_DOCUMENTACION_AEA',
        'OBSERVACION_NORMATIVA_DC',
        'APROBACION_NORMATIVA_DC'
    ))
);

CREATE INDEX ix_notificacion_expediente_proceso ON notificacion_expediente (proceso_id);
CREATE INDEX ix_notificacion_expediente_evento ON notificacion_expediente (tipo_evento);

-- ----------------------------------------------------------------------------
-- 4. Semilla de Seguridad: Permisos y Asignaciones
-- ----------------------------------------------------------------------------
INSERT INTO permiso (codigo, nombre, descripcion) VALUES
    ('NORMATIVA_SOLICITAR', 'Solicitar normativa a dirección de carrera', 'Permite a AEA o ADMIN iniciar solicitud y fijar plazo de 5 días hábiles.'),
    ('NORMATIVA_CARGAR_BORRADOR', 'Cargar borradores de normativa', 'Permite subir preliminares y guardar borradores de normativa.'),
    ('NORMATIVA_ENVIAR', 'Enviar normativa definitiva a revisión', 'Permite formalizar la entrega definitiva congelando la edición.'),
    ('NORMATIVA_REVISAR', 'Revisar y dictaminar sobre normativa', 'Permite emitir observaciones o aprobar la documentación completando la Actividad 2.'),
    ('NORMATIVA_EXPORT_INFO', 'Consultar y exportar Hoja INFO', 'Permite ver la grilla y descargar la Hoja INFO en formato Excel .xlsx.')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
    'NORMATIVA_SOLICITAR', 'NORMATIVA_CARGAR_BORRADOR', 'NORMATIVA_ENVIAR', 'NORMATIVA_REVISAR', 'NORMATIVA_EXPORT_INFO'
) WHERE r.codigo = 'ADMINISTRATOR' ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
    'NORMATIVA_SOLICITAR', 'NORMATIVA_REVISAR', 'NORMATIVA_EXPORT_INFO'
) WHERE r.codigo = 'AEA' ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
    'NORMATIVA_CARGAR_BORRADOR', 'NORMATIVA_ENVIAR', 'NORMATIVA_EXPORT_INFO'
) WHERE r.codigo = 'CAREER_DIRECTOR' ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN ('NORMATIVA_EXPORT_INFO')
WHERE r.codigo = 'SELF_EVALUATION_COMMITTEE_MEMBER' ON CONFLICT DO NOTHING;
