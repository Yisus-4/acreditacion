-- =============================================================================
-- V16__rg001_plan_trabajo_borrador_envio.sql
-- Separación de estados de borrador y envío para RG-001 y Plan de Trabajo (PT-001)
-- =============================================================================

ALTER TABLE registro_convocatoria
    ADD COLUMN IF NOT EXISTS estado_rg001 VARCHAR(30) NOT NULL DEFAULT 'BORRADOR';

ALTER TABLE plan_trabajo
    ADD COLUMN IF NOT EXISTS estado_envio VARCHAR(30) NOT NULL DEFAULT 'BORRADOR';

-- Sincronizar procesos donde ACT-02 ya haya sido completada históricamente
UPDATE registro_convocatoria
SET estado_rg001 = 'ENVIADO'
WHERE EXISTS (
    SELECT 1 FROM actividad_plan_trabajo a
    JOIN plan_trabajo pt ON pt.id = a.plan_trabajo_id
    WHERE pt.proceso_id = registro_convocatoria.proceso_id
      AND a.codigo_actividad = 'ACT-02-ELABORAR-PLAN-TRABAJO'
      AND a.estado = 'COMPLETADA'
);

UPDATE plan_trabajo
SET estado_envio = 'ENVIADO'
WHERE EXISTS (
    SELECT 1 FROM actividad_plan_trabajo a
    WHERE a.plan_trabajo_id = plan_trabajo.id
      AND a.codigo_actividad = 'ACT-02-ELABORAR-PLAN-TRABAJO'
      AND a.estado = 'COMPLETADA'
);
