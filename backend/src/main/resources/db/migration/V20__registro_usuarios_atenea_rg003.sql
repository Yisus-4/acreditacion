-- =============================================================================
-- V20__registro_usuarios_atenea_rg003.sql
-- Tarea: Definir usuarios y permisos de acceso para la solicitud de acreditación
--        Registro RG-003: Registro de usuarios de ATENEA
-- =============================================================================

CREATE TABLE registro_usuarios_atenea (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    estado VARCHAR(50) NOT NULL DEFAULT 'EN_DEFINICION',
    fecha_limite DATE,
    generado_en TIMESTAMPTZ,
    aprobado_en TIMESTAMPTZ,
    creado_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    ruta_documento VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_registro_usuarios_atenea_proceso UNIQUE (proceso_id)
);

CREATE TABLE usuarios_permisos_atenea (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registro_id UUID NOT NULL REFERENCES registro_usuarios_atenea(id) ON DELETE CASCADE,
    grupo_usuario VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni_cuil VARCHAR(30) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    area_dependencia VARCHAR(150),
    cargo_funcion VARCHAR(150),
    tipo_acceso VARCHAR(50) NOT NULL DEFAULT 'CARGA',
    seccion_alcance VARCHAR(255),
    requiere_cuenta_nueva_coneau_global BOOLEAN NOT NULL DEFAULT FALSE,
    cuenta_coneau_global_creada BOOLEAN NOT NULL DEFAULT FALSE,
    habilitado_en_coneau BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_usuarios_permisos_atenea_registro_id ON usuarios_permisos_atenea (registro_id);
CREATE INDEX ix_usuarios_permisos_atenea_grupo ON usuarios_permisos_atenea (grupo_usuario);
CREATE INDEX ix_registro_usuarios_atenea_proceso_id ON registro_usuarios_atenea (proceso_id);
