-- =============================================================================
-- V37__fix_gt003_unique_proceso_and_aea_nomenclature.sql
-- 1. Deduplica registros redundantes de guia_higiene_seguridad por proceso_id
-- 2. Añade índice único parcial uq_guia_higiene_proceso_id para evitar carreras concurrentes
-- 3. Normaliza nomenclatura residual de DEA a AEA
-- =============================================================================

-- 1. Deduplicación conservando el registro más reciente
WITH ranked_guias AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY proceso_id 
               ORDER BY actualizado_en DESC, creado_en DESC, id DESC
           ) AS rn
    FROM guia_higiene_seguridad
    WHERE proceso_id IS NOT NULL AND es_maestro = FALSE
)
DELETE FROM guia_higiene_seguridad
WHERE id IN (
    SELECT id FROM ranked_guias WHERE rn > 1
);

-- 2. Índice único parcial por proceso_id para instancias de autoevaluación
CREATE UNIQUE INDEX IF NOT EXISTS uq_guia_higiene_proceso_id
ON guia_higiene_seguridad (proceso_id)
WHERE proceso_id IS NOT NULL AND es_maestro = FALSE;

-- 3. Normalización de DEA -> AEA en metadatos históricos y observaciones de GT-003
UPDATE guia_higiene_seguridad
SET elaborado_por = REPLACE(elaborado_por, 'DEA', 'AEA'),
    revisado_por  = REPLACE(revisado_por, 'DEA', 'AEA'),
    observaciones = CASE 
        WHEN observaciones LIKE '%DEA%' THEN REPLACE(observaciones, 'DEA', 'AEA')
        ELSE observaciones
    END
WHERE elaborado_por LIKE '%DEA%' 
   OR revisado_por LIKE '%DEA%' 
   OR observaciones LIKE '%DEA%';
