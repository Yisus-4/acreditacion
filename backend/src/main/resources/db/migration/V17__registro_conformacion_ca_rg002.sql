-- =============================================================================
-- V17__registro_conformacion_ca_rg002.sql
-- Tarea: Solicitar conformación de la comisión de autoevaluación (CA)
--        Registro RG-002 e Instructivo IT-001 (Carreras de GRADO)
-- =============================================================================

CREATE TABLE registro_conformacion_ca (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE_SOLICITUD',
    coincide_comision_curricular BOOLEAN NOT NULL DEFAULT FALSE,
    solicitado_en TIMESTAMPTZ,
    solicitado_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_limite_dc DATE,
    remitido_en TIMESTAMPTZ,
    remitido_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    aprobado_en TIMESTAMPTZ,
    aprobado_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    observaciones_revision TEXT,
    resolucion_storage_path VARCHAR(500),
    resolucion_nombre_original VARCHAR(255),
    resolucion_tamanio_bytes BIGINT,
    registro_generado_storage_path VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_registro_ca_proceso UNIQUE (proceso_id)
);

CREATE TABLE miembro_comision_autoevaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registro_ca_id UUID NOT NULL REFERENCES registro_conformacion_ca(id) ON DELETE CASCADE,
    apellido VARCHAR(100) NOT NULL,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    dni_cuil VARCHAR(30) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(50),
    claustro_rol VARCHAR(50) NOT NULL,
    es_docente BOOLEAN NOT NULL DEFAULT TRUE,
    cuenta_coneau_global_creada BOOLEAN NOT NULL DEFAULT FALSE,
    cuenta_cvar_vinculada BOOLEAN NOT NULL DEFAULT FALSE,
    numero_orden INT NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_miembro_ca_registro_id ON miembro_comision_autoevaluacion (registro_ca_id);
CREATE INDEX ix_registro_ca_proceso_id ON registro_conformacion_ca (proceso_id);
