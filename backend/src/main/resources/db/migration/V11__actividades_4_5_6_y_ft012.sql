-- =============================================================================
-- V11__actividades_4_5_6_y_ft012.sql
-- Actividades 4, 5 y 6 del procedimiento oficial de acreditación UNdeC (Grado)
-- y Ficha de Formalización Electrónica FT-012 + Comprobante CONEAU Global ATENEA
-- =============================================================================

-- 1. Ampliación de registro_convocatoria (RG-001) con fecha de formalización electrónica
ALTER TABLE registro_convocatoria
    ADD COLUMN IF NOT EXISTS fecha_formalizacion_electronica DATE;

-- 2. Ampliación de documento_acreditacion para Actividad 4 (clasificación de origen y descripción)
ALTER TABLE documento_acreditacion
    ADD COLUMN IF NOT EXISTS clasificacion_origen VARCHAR(100),
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(500);

ALTER TABLE documento_acreditacion
    DROP CONSTRAINT IF EXISTS ck_documento_tipo;

-- 3. Tabla ficha_formalizacion_ft012 (FT-012: Ficha de Formalización Electrónica - Grado)
CREATE TABLE IF NOT EXISTS ficha_formalizacion_ft012 (
    id UUID PRIMARY KEY,
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    modalidad_dictado VARCHAR(50) NOT NULL DEFAULT 'PRESENCIAL',
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE_SOLICITUD',
    -- 1. Información institucional
    institucion_universitaria VARCHAR(200) NOT NULL DEFAULT 'Universidad Nacional de Chilecito',
    autoridad_apellido VARCHAR(100) NOT NULL DEFAULT 'Salcedo',
    autoridad_primer_nombre VARCHAR(100) NOT NULL DEFAULT 'César',
    autoridad_segundo_nombre VARCHAR(100) DEFAULT 'Alberto',
    autoridad_correo VARCHAR(150) NOT NULL DEFAULT 'csalcedo@undec.edu.ar',
    autoridad_telefono VARCHAR(50),
    unidad_academica VARCHAR(200) NOT NULL DEFAULT 'Universidad Nacional de Chilecito',
    sede_calle VARCHAR(150) NOT NULL DEFAULT '9 de Julio',
    sede_numero VARCHAR(30) NOT NULL DEFAULT '22',
    sede_piso VARCHAR(30),
    sede_departamento VARCHAR(30),
    sede_codigo_postal VARCHAR(20) NOT NULL DEFAULT '5360',
    sede_provincia VARCHAR(100) NOT NULL DEFAULT 'La Rioja',
    sede_localidad VARCHAR(100) NOT NULL DEFAULT 'Chilecito',
    sede_departamento_partido VARCHAR(100) NOT NULL DEFAULT 'Chilecito',
    nexo_apellido VARCHAR(100),
    nexo_primer_nombre VARCHAR(100),
    nexo_segundo_nombre VARCHAR(100),
    nexo_telefono VARCHAR(50),
    nexo_cargo VARCHAR(100),
    nexo_correo VARCHAR(150),
    -- 2. Información por carrera
    carrera_nombre VARCHAR(200),
    titulo_asimilado VARCHAR(200),
    carga_horaria_no_presencial VARCHAR(100),
    anio_inicio_dictado INTEGER,
    responsable_apellido VARCHAR(100),
    responsable_primer_nombre VARCHAR(100),
    responsable_segundo_nombre VARCHAR(100),
    responsable_telefono VARCHAR(50),
    responsable_correo VARCHAR(150),
    responsable_cargo VARCHAR(100) DEFAULT 'Director',
    coordinador_ca_apellido VARCHAR(100),
    coordinador_ca_primer_nombre VARCHAR(100),
    coordinador_ca_segundo_nombre VARCHAR(100),
    coordinador_ca_telefono VARCHAR(50),
    coordinador_ca_correo VARCHAR(150),
    -- Comprobante CONEAU Global ATENEA y fechas
    comprobante_nombre VARCHAR(255),
    comprobante_storage_path VARCHAR(500),
    comprobante_tamanio_bytes BIGINT,
    fecha_formalizacion_electronica DATE,
    observaciones TEXT,
    solicitado_por UUID,
    solicitado_en TIMESTAMP WITH TIME ZONE,
    remitido_por UUID,
    remitido_en TIMESTAMP WITH TIME ZONE,
    formalizado_por UUID,
    formalizado_en TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_ficha_formalizacion_ft012_proceso_id ON ficha_formalizacion_ft012 (proceso_id);
