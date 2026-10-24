-- V13__alta_carreras_art43_y_escuela_ciencias_naturales.sql
-- Alta de Escuela de Ciencias Naturales y carreras de grado Art. 43

DO $$
DECLARE
    v_inst_id UUID := 'a0000000-0000-0000-0000-000000000001'::uuid;
    v_sede_sarmientos_id UUID := 'b0000000-0000-0000-0000-000000000001'::uuid;
    v_sede_centro_id UUID := 'b0000000-0000-0000-0000-000000000002'::uuid;

    v_ua_ingenieria_id UUID := 'c0000000-0000-0000-0000-000000000001'::uuid;
    v_ua_salud_id UUID := 'c0000000-0000-0000-0000-000000000002'::uuid;
    v_ua_sociales_id UUID := 'c0000000-0000-0000-0000-000000000003'::uuid;
    v_ua_naturales_id UUID := 'c0000000-0000-0000-0000-000000000004'::uuid;

    v_carrera_abog_id UUID := 'd0000000-0000-0000-0000-000000000004'::uuid;
    v_carrera_iagro_id UUID := 'd0000000-0000-0000-0000-000000000005'::uuid;
    v_carrera_imec_id UUID := 'd0000000-0000-0000-0000-000000000006'::uuid;
    v_carrera_iagrim_id UUID := 'd0000000-0000-0000-0000-000000000007'::uuid;
BEGIN
    -- 1. Alta de Escuela de Ciencias Naturales
    INSERT INTO unidad_academica (id, institucion_id, nombre, sigla, descripcion, activo)
    VALUES (
        v_ua_naturales_id,
        v_inst_id,
        'Escuela de Ciencias Naturales',
        'ECNat',
        'Escuela de Ciencias Naturales y Ambientales de la UNdeC',
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- 2. Alta de Carreras de Grado Art. 43
    -- Abogacía (Ciencias Sociales)
    INSERT INTO carrera (
        id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria,
        modalidad, estado, titulo_otorgado, duracion_anios, activo
    ) VALUES (
        v_carrera_abog_id,
        v_ua_sociales_id,
        'Abogacía',
        'ABOG',
        'GRADO',
        'Ciencias Sociales',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Abogado/a',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- Ingeniería Agronómica (Ciencias Naturales)
    INSERT INTO carrera (
        id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria,
        modalidad, estado, titulo_otorgado, duracion_anios, activo
    ) VALUES (
        v_carrera_iagro_id,
        v_ua_naturales_id,
        'Ingeniería Agronómica',
        'IAGRO',
        'GRADO',
        'Ciencias Naturales',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Ingeniero/a Agrónomo/a',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- Ingeniería Mecatrónica (Ingeniería)
    INSERT INTO carrera (
        id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria,
        modalidad, estado, titulo_otorgado, duracion_anios, activo
    ) VALUES (
        v_carrera_imec_id,
        v_ua_ingenieria_id,
        'Ingeniería Mecatrónica',
        'IMEC',
        'GRADO',
        'Ingeniería y Tecnologías',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Ingeniero/a Mecatrónico/a',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- Ingeniería en Agrimensura (Ingeniería)
    INSERT INTO carrera (
        id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria,
        modalidad, estado, titulo_otorgado, duracion_anios, activo
    ) VALUES (
        v_carrera_iagrim_id,
        v_ua_ingenieria_id,
        'Ingeniería en Agrimensura',
        'IAGRIM',
        'GRADO',
        'Ingeniería y Tecnologías',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Ingeniero/a Agrimensor/a',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- 3. Vincular Carreras a Sedes
    INSERT INTO carrera_sede (carrera_id, sede_id)
    VALUES
        (v_carrera_abog_id, v_sede_sarmientos_id),
        (v_carrera_abog_id, v_sede_centro_id),
        (v_carrera_iagro_id, v_sede_sarmientos_id),
        (v_carrera_imec_id, v_sede_sarmientos_id),
        (v_carrera_iagrim_id, v_sede_sarmientos_id)
    ON CONFLICT (carrera_id, sede_id) DO NOTHING;

END $$;
