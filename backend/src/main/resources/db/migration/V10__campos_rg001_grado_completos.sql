-- =============================================================================
-- V10__campos_rg001_grado_completos.sql
-- Campos completos del RG-001 (Registro de participación en acreditaciones - Nivel Grado)
-- =============================================================================

ALTER TABLE registro_convocatoria
    ADD COLUMN IF NOT EXISTS area_disciplinaria VARCHAR(150),
    ADD COLUMN IF NOT EXISTS modalidad_carrera VARCHAR(50) NOT NULL DEFAULT 'PRESENCIAL',
    ADD COLUMN IF NOT EXISTS estado_carrera VARCHAR(50) NOT NULL DEFAULT 'EN_FUNCIONAMIENTO';

ALTER TABLE registro_convocatoria
    ALTER COLUMN nro_resolucion_coneau DROP NOT NULL;

ALTER TABLE registro_convocatoria
    DROP CONSTRAINT IF EXISTS ck_registro_convocatoria_modalidad,
    ADD CONSTRAINT ck_registro_convocatoria_modalidad
        CHECK (modalidad_carrera IN ('PRESENCIAL', 'A_DISTANCIA', 'DISTANCIA')),
    DROP CONSTRAINT IF EXISTS ck_registro_convocatoria_estado_carrera,
    ADD CONSTRAINT ck_registro_convocatoria_estado_carrera
        CHECK (estado_carrera IN ('NUEVA', 'EN_FUNCIONAMIENTO'));

-- Sincronizar datos existentes en registro_convocatoria a partir de proceso_acreditacion y carrera
UPDATE registro_convocatoria rc
SET estado_carrera = pa.tipo_proceso,
    area_disciplinaria = COALESCE(rc.area_disciplinaria, c.area_disciplinaria),
    modalidad_carrera = CASE
        WHEN c.modalidad = 'DISTANCIA' THEN 'A_DISTANCIA'
        ELSE 'PRESENCIAL'
    END
FROM proceso_acreditacion pa
JOIN carrera c ON c.id = pa.carrera_id
WHERE rc.proceso_id = pa.id;
