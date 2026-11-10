-- =============================================================================
-- V33__instancia_planes_ambitos.sql
-- 1. Rol DOCENTE y permisos base
-- 2. Ampliación de responsable_practica para soporte de selección múltiple
-- 3. Índices de rendimiento para orden de actividades y filtrado de ámbitos
-- =============================================================================

-- 1. Sembrar rol DOCENTE
INSERT INTO rol (codigo, nombre, descripcion, activo, es_sistema, creado_en)
VALUES ('DOCENTE', 'Docente', 'Docente universitario responsable de actividades curriculares o prácticas formativas.', TRUE, TRUE, NOW())
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos básicos de cuenta propia al rol DOCENTE
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('ACCOUNT_SELF_READ', 'ACCOUNT_SELF_UPDATE_PASSWORD')
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- 2. Ampliar responsable_practica en autoevaluacion_carrera_ambito
ALTER TABLE autoevaluacion_carrera_ambito
    ALTER COLUMN responsable_practica TYPE VARCHAR(500);

-- 3. Índices de rendimiento
CREATE INDEX IF NOT EXISTS idx_actividad_curricular_plan_orden
    ON actividad_curricular_plan(plan_estudio_id, numero_orden);

CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_proc_util
    ON autoevaluacion_carrera_ambito(proceso_id, utilizado_por_carrera);
