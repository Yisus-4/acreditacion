-- =============================================================================
-- V18__actualizar_campos_rg002_y_catalogo_miembros_ca.sql
-- Actualización de campos oficiales RG-002 para la Comisión de Autoevaluación
-- y Creación del Catálogo Permanente de Miembros de Comisión
-- =============================================================================

-- 1. Actualización de tabla miembro_comision_autoevaluacion
ALTER TABLE miembro_comision_autoevaluacion
    ADD COLUMN cuit_pasaporte VARCHAR(50),
    ADD COLUMN correo_gmail VARCHAR(150),
    ADD COLUMN cargo_funcion VARCHAR(150),
    ADD COLUMN pendiente_alta_sistema BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Creación de tabla catalogo_miembro_comision
CREATE TABLE catalogo_miembro_comision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apellido VARCHAR(100) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    cuit_pasaporte VARCHAR(50) NOT NULL,
    correo_electronico VARCHAR(150) NOT NULL,
    correo_gmail VARCHAR(150) NOT NULL,
    cargo_funcion VARCHAR(150) NOT NULL,
    es_docente BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_catalogo_miembro_doc ON catalogo_miembro_comision (documento);
CREATE INDEX ix_catalogo_miembro_cuit ON catalogo_miembro_comision (cuit_pasaporte);
CREATE INDEX ix_catalogo_miembro_apellido ON catalogo_miembro_comision (apellido);

-- 3. Seed de ejemplos reales provistos por el usuario
INSERT INTO catalogo_miembro_comision (
    apellido, nombre_completo, documento, cuit_pasaporte,
    correo_electronico, correo_gmail, cargo_funcion, es_docente
) VALUES
('Salerno', 'Julio Luis', '8.386.710', '20-08386710-9', 'jsalerno@undec.edu.ar', 'salernojulioluis@gmail.com', 'Director de Carrera', TRUE),
('Albagli', 'Karen Janet', '35.850.007', '27-35850007-8', 'kalbagli91@gmail.com', 'albaagrimensura@gmail.com', 'Docente', TRUE);
