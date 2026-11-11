-- =============================================================================
-- V35__reset_verificado_inmuebles_maestros.sql
-- Garantiza que los inmuebles del Padrón Maestro (tronco principal / es_maestro = TRUE)
-- no figuren como verificados por defecto, de modo que la Actividad 4.1 (Infraestructura)
-- de cada carrera sólo figure como Finalizada una vez que se confirme y almacene
-- la copia (snapshot) asociada al proceso_id de la carrera.
-- =============================================================================

UPDATE inmueble
SET verificado = FALSE,
    fecha_verificacion = NULL,
    estado_coneau = 'CARGADO'
WHERE es_maestro = TRUE;
