-- =============================================================================
-- V21__matriz_permisos_atenea_rg003.sql
-- Matriz de Permisos del Sistema CONEAU Global ATENEA (Registro RG-003)
-- =============================================================================

CREATE TABLE matriz_permisos_atenea (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registro_id UUID NOT NULL REFERENCES registro_usuarios_atenea(id) ON DELETE CASCADE,
    proceso_actividad VARCHAR(255) NOT NULL,
    item VARCHAR(255) NOT NULL,
    seccion VARCHAR(100),
    tipo_permiso VARCHAR(50) NOT NULL,
    permiso_sgi_aea BOOLEAN NOT NULL DEFAULT TRUE,
    permiso_ca BOOLEAN NOT NULL DEFAULT FALSE,
    orden INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_matriz_permisos_atenea_registro_id ON matriz_permisos_atenea (registro_id);
CREATE INDEX ix_matriz_permisos_atenea_orden ON matriz_permisos_atenea (registro_id, orden);
