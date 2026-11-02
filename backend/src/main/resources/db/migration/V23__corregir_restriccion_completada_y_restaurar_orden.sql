-- V23: Corrección de restricción ck_actividad_completada para permitir completitud automática o institucional
-- y restauración de numero_orden en actividades del plan de trabajo.

-- 1. Actualizar restricción ck_actividad_completada para no forzar completada_por no nulo en completitudes institucionales
ALTER TABLE actividad_plan_trabajo DROP CONSTRAINT IF EXISTS ck_actividad_completada;

ALTER TABLE actividad_plan_trabajo
    ADD CONSTRAINT ck_actividad_completada
        CHECK (
            (estado <> 'COMPLETADA') OR (
                completada_en IS NOT NULL
            )
        );

-- 2. Restaurar numero_orden para actividades con número negativo generado temporalmente
UPDATE actividad_plan_trabajo
SET numero_orden = ABS(numero_orden) - 1000
WHERE numero_orden < -1000;
