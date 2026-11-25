-- Sección 3 configurable por subsección (3.1.1 y 3.1.2):
-- Permite activar/desactivar 3.1.1 y 3.1.2 independientemente, elegir su modo
-- (Áreas temáticas vs. Bloques curriculares en 3.1.1; Áreas prácticas vs. Modalidades prácticas en 3.1.2)
-- y definir horas mínimas globales para la carrera (totales y por fila habilitada).

ALTER TABLE estandar
    ADD COLUMN IF NOT EXISTS habilitar_311 BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS modo_311 VARCHAR(30) NOT NULL DEFAULT 'AREAS_TEMATICAS',
    ADD COLUMN IF NOT EXISTS horas_minimas_total_311 INT,
    ADD COLUMN IF NOT EXISTS habilitar_312 BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS modo_312 VARCHAR(30) NOT NULL DEFAULT 'AREAS_PRACTICAS',
    ADD COLUMN IF NOT EXISTS horas_minimas_total_312 INT;

ALTER TABLE estandar_area_carga_horaria
    ADD COLUMN IF NOT EXISTS horas_minimas_carrera INT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_modo_311') THEN
        ALTER TABLE estandar
            ADD CONSTRAINT chk_estandar_modo_311
            CHECK (modo_311 IN ('AREAS_TEMATICAS', 'BLOQUES_CURRICULARES'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_modo_312') THEN
        ALTER TABLE estandar
            ADD CONSTRAINT chk_estandar_modo_312
            CHECK (modo_312 IN ('AREAS_PRACTICAS', 'MODALIDADES_PRACTICAS'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_horas_min_311') THEN
        ALTER TABLE estandar
            ADD CONSTRAINT chk_estandar_horas_min_311
            CHECK (horas_minimas_total_311 IS NULL OR horas_minimas_total_311 >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_horas_min_312') THEN
        ALTER TABLE estandar
            ADD CONSTRAINT chk_estandar_horas_min_312
            CHECK (horas_minimas_total_312 IS NULL OR horas_minimas_total_312 >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_area_horas_min_carrera') THEN
        ALTER TABLE estandar_area_carga_horaria
            ADD CONSTRAINT chk_estandar_area_horas_min_carrera
            CHECK (horas_minimas_carrera IS NULL OR horas_minimas_carrera >= 0);
    END IF;
END $$;

-- Backfill de estándares existentes con tipo_seccion_22 = 'EJES'
UPDATE estandar
SET modo_311 = 'BLOQUES_CURRICULARES',
    modo_312 = 'MODALIDADES_PRACTICAS'
WHERE tipo_seccion_22 = 'EJES';
