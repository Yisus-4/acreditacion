-- =============================================================================
-- V27__eliminar_campos_ca_y_actualizar_autoridad_maxima.sql
-- 1. Eliminar campos obsoletos de la Comisión de Autoevaluación (CA):
--    - es_docente, cuenta_coneau_global_creada, cuenta_cvar_vinculada en miembro_comision_autoevaluacion
--    - es_docente en catalogo_miembro_comision
-- 2. Actualizar la máxima autoridad de la institución por Abogado César Salcedo
-- =============================================================================

-- 1. Eliminar columnas en miembro_comision_autoevaluacion
ALTER TABLE miembro_comision_autoevaluacion
    DROP COLUMN IF EXISTS es_docente,
    DROP COLUMN IF EXISTS cuenta_coneau_global_creada,
    DROP COLUMN IF EXISTS cuenta_cvar_vinculada;

-- 2. Eliminar columna en catalogo_miembro_comision
ALTER TABLE catalogo_miembro_comision
    DROP COLUMN IF EXISTS es_docente;

-- 3. Actualizar la máxima autoridad de la institución
UPDATE institucion_autoridad
SET apellido = 'Salcedo',
    primer_nombre = 'César',
    segundo_nombre = 'Alberto',
    cargo = 'Rector',
    email = 'csalcedo@undec.edu.ar'
WHERE tipo_autoridad = 'MAXIMA_AUTORIDAD';
