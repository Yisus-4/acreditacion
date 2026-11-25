-- Paso complementario: separa las filas configurables de Sección 3 en 3.1.1 (Carga total por bloque/área)
-- y 3.1.2 (Modalidades/áreas de formación práctica - Anexo III).

ALTER TABLE estandar_area_carga_horaria
    ADD COLUMN IF NOT EXISTS tipo_seccion VARCHAR(30) NOT NULL DEFAULT 'CARGA_TOTAL_311';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_estandar_area_tipo_seccion'
    ) THEN
        ALTER TABLE estandar_area_carga_horaria
            ADD CONSTRAINT chk_estandar_area_tipo_seccion
            CHECK (tipo_seccion IN ('CARGA_TOTAL_311', 'PRACTICA_312'));
    END IF;
END $$;

ALTER TABLE estandar_area_carga_horaria
    DROP CONSTRAINT IF EXISTS uq_estandar_area_codigo;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_estandar_area_tipo_codigo'
    ) THEN
        ALTER TABLE estandar_area_carga_horaria
            ADD CONSTRAINT uq_estandar_area_tipo_codigo UNIQUE (estandar_id, tipo_seccion, codigo);
    END IF;
END $$;

DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT id, tipo_seccion_22
        FROM estandar
    LOOP
        IF EXISTS (
            SELECT 1 FROM estandar_area_carga_horaria
            WHERE estandar_id = rec.id AND tipo_seccion = 'PRACTICA_312'
        ) THEN
            CONTINUE;
        END IF;

        IF rec.tipo_seccion_22 = 'EJES' THEN
            IF NOT EXISTS (
                SELECT 1
                FROM actividad_curricular_carga_horaria_area accha
                JOIN estandar_area_carga_horaria each2 ON each2.id = accha.area_carga_horaria_id
                WHERE each2.estandar_id = rec.id
            ) AND EXISTS (
                SELECT 1
                FROM estandar_area_carga_horaria
                WHERE estandar_id = rec.id
                  AND tipo_seccion = 'CARGA_TOTAL_311'
                  AND codigo = 'FORMACION_BASICA'
            ) THEN
                DELETE FROM estandar_area_carga_horaria
                WHERE estandar_id = rec.id AND tipo_seccion = 'CARGA_TOTAL_311';

                INSERT INTO estandar_area_carga_horaria
                    (estandar_id, tipo_seccion, orden, codigo, nombre, etiqueta_practica, habilitada)
                VALUES
                    (rec.id, 'CARGA_TOTAL_311', 1, 'CIENCIAS_BASICAS_INGENIERIA', 'Ciencias Básicas de la Ingeniería', 'Ciencias Básicas de la Ingeniería', TRUE),
                    (rec.id, 'CARGA_TOTAL_311', 2, 'TECNOLOGIAS_BASICAS', 'Tecnologías Básicas', 'Tecnologías Básicas', TRUE),
                    (rec.id, 'CARGA_TOTAL_311', 3, 'TECNOLOGIAS_APLICADAS', 'Tecnologías Aplicadas', 'Tecnologías Aplicadas', TRUE),
                    (rec.id, 'CARGA_TOTAL_311', 4, 'CIENCIAS_TECNOLOGIAS_COMPLEMENTARIAS', 'Ciencias y Tecnologías Complementarias', 'Ciencias y Tecnologías Complementarias', TRUE),
                    (rec.id, 'CARGA_TOTAL_311', 5, 'OTROS_CONTENIDOS', 'Otros contenidos', 'Otros contenidos', TRUE)
                ON CONFLICT (estandar_id, tipo_seccion, codigo) DO NOTHING;
            END IF;

            INSERT INTO estandar_area_carga_horaria
                (estandar_id, tipo_seccion, orden, codigo, nombre, etiqueta_practica, habilitada)
            VALUES
                (rec.id, 'PRACTICA_312', 1, 'INSTANCIAS_SUPERVISADAS_PRACTICA', 'Instancias supervisadas de formación práctica (prácticas en diferentes ámbitos tales como aula, laboratorios, campo u otros)', 'Instancias supervisadas de formación práctica (prácticas en diferentes ámbitos tales como aula, laboratorios, campo u otros)', TRUE),
                (rec.id, 'PRACTICA_312', 2, 'PROYECTO_INTEGRADOR', 'Proyecto Integrador', 'Proyecto Integrador', TRUE),
                (rec.id, 'PRACTICA_312', 3, 'PRACTICA_PROFESIONAL_SUPERVISADA', 'Práctica profesional supervisada', 'Práctica profesional supervisada', TRUE)
            ON CONFLICT (estandar_id, tipo_seccion, codigo) DO NOTHING;
        ELSE
            INSERT INTO estandar_area_carga_horaria
                (estandar_id, tipo_seccion, orden, codigo, nombre, etiqueta_practica, habilitada)
            VALUES
                (rec.id, 'CARGA_TOTAL_311', 5, 'OTROS_CONTENIDOS', 'Otros contenidos', 'Otros contenidos', TRUE)
            ON CONFLICT (estandar_id, tipo_seccion, codigo) DO NOTHING;

            INSERT INTO estandar_area_carga_horaria
                (estandar_id, tipo_seccion, orden, codigo, nombre, etiqueta_practica, habilitada)
            VALUES
                (rec.id, 'PRACTICA_312', 1, 'FORMACION_BASICA', 'Formación Básica', 'Formación Básica', TRUE),
                (rec.id, 'PRACTICA_312', 2, 'FORMACION_APLICADA', 'Formación Aplicada', 'Formación Aplicada', TRUE),
                (rec.id, 'PRACTICA_312', 3, 'FORMACION_PROFESIONAL', 'Formación Profesional', 'Formación Profesional', TRUE),
                (rec.id, 'PRACTICA_312', 4, 'FORMACION_COMPLEMENTARIA', 'Carga horaria asignada a formación complementaria', 'Carga horaria asignada a formación complementaria', TRUE)
            ON CONFLICT (estandar_id, tipo_seccion, codigo) DO NOTHING;

            INSERT INTO actividad_curricular_carga_horaria_area (
                id, actividad_curricular_id, area_carga_horaria_id,
                horas_total_presencial, horas_total_distancia,
                horas_practica_presencial, horas_practica_distancia
            )
            SELECT
                gen_random_uuid(),
                ac.actividad_curricular_id,
                a312.id,
                0,
                0,
                ac.horas_practica_presencial,
                ac.horas_practica_distancia
            FROM actividad_curricular_carga_horaria_area ac
            JOIN estandar_area_carga_horaria a311 ON a311.id = ac.area_carga_horaria_id
            JOIN estandar_area_carga_horaria a312 ON a312.estandar_id = a311.estandar_id
                AND a312.tipo_seccion = 'PRACTICA_312'
                AND a312.codigo = a311.codigo
            WHERE a311.estandar_id = rec.id
              AND a311.tipo_seccion = 'CARGA_TOTAL_311'
              AND (ac.horas_practica_presencial > 0 OR ac.horas_practica_distancia > 0)
            ON CONFLICT (actividad_curricular_id, area_carga_horaria_id) DO NOTHING;

            UPDATE actividad_curricular_carga_horaria_area ac
            SET horas_practica_presencial = 0,
                horas_practica_distancia = 0
            FROM estandar_area_carga_horaria a311
            WHERE a311.id = ac.area_carga_horaria_id
              AND a311.estandar_id = rec.id
              AND a311.tipo_seccion = 'CARGA_TOTAL_311'
              AND (ac.horas_practica_presencial > 0 OR ac.horas_practica_distancia > 0);
        END IF;
    END LOOP;
END $$;
