-- =============================================================================
-- V36__unificar_inmuebles_padron_general.sql
-- Unifica los inmuebles en el Padrón General Institucional (es_maestro = true).
-- 1. Elimina copias clonadas redundantes (es_maestro = false) que ya existen en el padrón general.
-- 2. Promueve cualquier inmueble nuevo cargado por el usuario al padrón general (es_maestro = true).
-- =============================================================================

-- Eliminar duplicados clonados cuyo nombre ya existe en el padrón general
DELETE FROM inmueble
WHERE es_maestro = FALSE
  AND nombre IN (
      SELECT m.nombre
      FROM inmueble m
      WHERE m.es_maestro = TRUE
  );

-- Promover cualquier inmueble nuevo creado en modo proceso al padrón general
UPDATE inmueble
SET es_maestro = TRUE,
    proceso_id = NULL,
    verificado = FALSE,
    fecha_verificacion = NULL,
    estado_coneau = 'CARGADO'
WHERE es_maestro = FALSE;
