-- =============================================================================
-- V4__estructura_academica.sql -- Estructura Administrativa y Académica
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Tabla unidad_academica
-- ----------------------------------------------------------------------------
CREATE TABLE unidad_academica (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    institucion_id UUID          NOT NULL,
    nombre         VARCHAR(255)  NOT NULL,
    sigla          VARCHAR(50),
    descripcion    TEXT,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_unidad_academica PRIMARY KEY (id),
    CONSTRAINT fk_unidad_academica_institucion
        FOREIGN KEY (institucion_id) REFERENCES institucion (id) ON DELETE CASCADE
);

CREATE INDEX ix_unidad_academica_institucion_id ON unidad_academica (institucion_id);

-- ----------------------------------------------------------------------------
-- Tabla sede_localizacion
-- ----------------------------------------------------------------------------
CREATE TABLE sede_localizacion (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    institucion_id  UUID          NOT NULL,
    nombre          VARCHAR(255)  NOT NULL,
    calle           VARCHAR(255)  NOT NULL,
    numero          VARCHAR(50)   NOT NULL,
    piso            VARCHAR(20),
    codigo_postal   VARCHAR(20)   NOT NULL,
    provincia_id    BIGINT        NOT NULL,
    departamento_id BIGINT        NOT NULL,
    localidad_id    BIGINT        NOT NULL,
    es_propia       BOOLEAN       NOT NULL DEFAULT TRUE,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_sede_localizacion PRIMARY KEY (id),
    CONSTRAINT fk_sede_localizacion_institucion
        FOREIGN KEY (institucion_id) REFERENCES institucion (id) ON DELETE CASCADE,
    CONSTRAINT fk_sede_localizacion_provincia
        FOREIGN KEY (provincia_id) REFERENCES geografia_provincia (id),
    CONSTRAINT fk_sede_localizacion_departamento
        FOREIGN KEY (departamento_id) REFERENCES geografia_departamento (id),
    CONSTRAINT fk_sede_localizacion_localidad
        FOREIGN KEY (localidad_id) REFERENCES geografia_localidad (id)
);

CREATE INDEX ix_sede_localizacion_institucion_id ON sede_localizacion (institucion_id);
CREATE INDEX ix_sede_localizacion_provincia_id ON sede_localizacion (provincia_id);
CREATE INDEX ix_sede_localizacion_departamento_id ON sede_localizacion (departamento_id);
CREATE INDEX ix_sede_localizacion_localidad_id ON sede_localizacion (localidad_id);

-- ----------------------------------------------------------------------------
-- Tabla carrera
-- ----------------------------------------------------------------------------
CREATE TABLE carrera (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    unidad_academica_id UUID          NOT NULL,
    nombre              VARCHAR(255)  NOT NULL,
    sigla               VARCHAR(50),
    nivel               VARCHAR(50)   NOT NULL DEFAULT 'GRADO' CHECK (nivel = 'GRADO'),
    area_disciplinaria  VARCHAR(150)  NOT NULL,
    modalidad           VARCHAR(50)   NOT NULL CHECK (modalidad IN ('PRESENCIAL', 'DISTANCIA')),
    estado              VARCHAR(50)   NOT NULL DEFAULT 'EN_FUNCIONAMIENTO' CHECK (estado IN ('NUEVA', 'EN_FUNCIONAMIENTO')),
    titulo_otorgado     VARCHAR(255),
    duracion_anios      INT,
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_carrera PRIMARY KEY (id),
    CONSTRAINT fk_carrera_unidad_academica
        FOREIGN KEY (unidad_academica_id) REFERENCES unidad_academica (id) ON DELETE RESTRICT
);

CREATE INDEX ix_carrera_unidad_academica_id ON carrera (unidad_academica_id);

-- ----------------------------------------------------------------------------
-- Tabla carrera_sede
-- ----------------------------------------------------------------------------
CREATE TABLE carrera_sede (
    carrera_id UUID NOT NULL,
    sede_id    UUID NOT NULL,

    CONSTRAINT pk_carrera_sede PRIMARY KEY (carrera_id, sede_id),
    CONSTRAINT fk_carrera_sede_carrera
        FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE CASCADE,
    CONSTRAINT fk_carrera_sede_sede
        FOREIGN KEY (sede_id) REFERENCES sede_localizacion (id) ON DELETE CASCADE
);

CREATE INDEX ix_carrera_sede_carrera_id ON carrera_sede (carrera_id);
CREATE INDEX ix_carrera_sede_sede_id ON carrera_sede (sede_id);

-- ----------------------------------------------------------------------------
-- Permisos y Roles de Seguridad para Estructura Académica
-- ----------------------------------------------------------------------------
INSERT INTO permiso (codigo, nombre, descripcion) VALUES
    ('ESTRUCTURA_ACADEMICA_READ',
     'Consultar estructura académica',
     'Permite consultar unidades académicas, sedes y carreras.'),
    ('ESTRUCTURA_ACADEMICA_WRITE',
     'Administrar estructura académica',
     'Permite crear y modificar unidades académicas, sedes y carreras.')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a ADMINISTRATOR y AEA
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo IN ('ADMINISTRATOR', 'AEA')
  AND p.codigo IN ('ESTRUCTURA_ACADEMICA_READ', 'ESTRUCTURA_ACADEMICA_WRITE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Semillas de demostración para UNdeC
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_inst_id UUID;
    v_prov_id BIGINT;
    v_depto_chilecito_id BIGINT;
    v_loc_sarmientos_id BIGINT;
    v_loc_chilecito_id BIGINT;
    v_sede_sarmientos_id UUID;
    v_sede_centro_id UUID;
    v_ua_ingenieria_id UUID;
    v_ua_salud_id UUID;
    v_ua_sociales_id UUID;
    v_carrera_isi_id UUID;
    v_carrera_lsi_id UUID;
    v_carrera_enf_id UUID;
BEGIN
    -- 1. Institución UNdeC (obtener o crear si no existe)
    SELECT id INTO v_inst_id FROM institucion WHERE sigla = 'UNdeC' LIMIT 1;

    IF v_inst_id IS NULL THEN
        INSERT INTO institucion (id, nombre, sigla, logo_path, activo)
        VALUES (
            'a0000000-0000-0000-0000-000000000001'::uuid,
            'Universidad Nacional de Chilecito',
            'UNdeC',
            '/assets/images/UNdeC_ISOLOGO_EMBLEMA-_1_.webp',
            TRUE
        )
        RETURNING id INTO v_inst_id;
    END IF;

    -- 2. Localidades de Chilecito, La Rioja
    SELECT id INTO v_prov_id FROM geografia_provincia WHERE codigo = 'LR' LIMIT 1;
    SELECT id INTO v_depto_chilecito_id FROM geografia_departamento WHERE provincia_id = v_prov_id AND nombre = 'Chilecito' LIMIT 1;
    SELECT id INTO v_loc_sarmientos_id FROM geografia_localidad WHERE departamento_id = v_depto_chilecito_id AND nombre = 'Los Sarmientos' LIMIT 1;
    SELECT id INTO v_loc_chilecito_id FROM geografia_localidad WHERE departamento_id = v_depto_chilecito_id AND nombre = 'Chilecito' LIMIT 1;

    -- Si no existen las localidades por alguna razón, usamos fallback de la primera localidad disponible
    IF v_loc_sarmientos_id IS NULL THEN
        SELECT id INTO v_loc_sarmientos_id FROM geografia_localidad LIMIT 1;
    END IF;
    IF v_loc_chilecito_id IS NULL THEN
        v_loc_chilecito_id := v_loc_sarmientos_id;
    END IF;
    IF v_prov_id IS NULL THEN
        SELECT id INTO v_prov_id FROM geografia_provincia LIMIT 1;
    END IF;
    IF v_depto_chilecito_id IS NULL THEN
        SELECT id INTO v_depto_chilecito_id FROM geografia_departamento LIMIT 1;
    END IF;

    -- Sede Central institucional y autoridades de la UNdeC
    INSERT INTO institucion_sede (
        institucion_id, es_sede_central, calle, numero, codigo_postal,
        provincia_id, departamento_id, localidad_id
    ) VALUES (
        v_inst_id, TRUE, '9 de Julio', '22', '5360',
        v_prov_id, v_depto_chilecito_id, v_loc_chilecito_id
    ) ON CONFLICT DO NOTHING;

    INSERT INTO institucion_autoridad (
        institucion_id, tipo_autoridad, apellido, primer_nombre,
        telefono, email, cargo, ambito
    ) VALUES (
        v_inst_id, 'MAXIMA_AUTORIDAD', 'Caminoa', 'Germán',
        '03825-427200', 'rectorado@undec.edu.ar', 'Rector', 'Rectorado'
    ), (
        v_inst_id, 'ADMINISTRADOR_INSTITUCIONAL', 'Álvarez', 'María',
        '03825-427201', 'acreditacion@undec.edu.ar', 'Responsable AEA', 'Área de Evaluación y Acreditación'
    ) ON CONFLICT DO NOTHING;

    -- 3. Sedes de Localización UNdeC
    INSERT INTO sede_localizacion (id, institucion_id, nombre, calle, numero, piso, codigo_postal, provincia_id, departamento_id, localidad_id, es_propia, activo)
    VALUES (
        'b0000000-0000-0000-0000-000000000001'::uuid,
        v_inst_id,
        'Campus Los Sarmientos',
        'Ruta Los Peregrinos',
        's/n',
        NULL,
        '5361',
        v_prov_id,
        v_depto_chilecito_id,
        v_loc_sarmientos_id,
        TRUE,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO sede_localizacion (id, institucion_id, nombre, calle, numero, piso, codigo_postal, provincia_id, departamento_id, localidad_id, es_propia, activo)
    VALUES (
        'b0000000-0000-0000-0000-000000000002'::uuid,
        v_inst_id,
        'Sede Centro',
        '9 de Julio',
        '22',
        NULL,
        '5360',
        v_prov_id,
        v_depto_chilecito_id,
        v_loc_chilecito_id,
        TRUE,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    v_sede_sarmientos_id := 'b0000000-0000-0000-0000-000000000001'::uuid;
    v_sede_centro_id := 'b0000000-0000-0000-0000-000000000002'::uuid;

    -- 4. Unidades Académicas
    INSERT INTO unidad_academica (id, institucion_id, nombre, sigla, descripcion, activo)
    VALUES (
        'c0000000-0000-0000-0000-000000000001'::uuid,
        v_inst_id,
        'Escuela de Ingeniería',
        'EI',
        'Departamento de Ciencias Básicas y Tecnológicas - Escuela de Ingeniería',
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO unidad_academica (id, institucion_id, nombre, sigla, descripcion, activo)
    VALUES (
        'c0000000-0000-0000-0000-000000000002'::uuid,
        v_inst_id,
        'Escuela de Ciencias de la Salud',
        'ECS',
        'Escuela de Ciencias de la Salud de la UNdeC',
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO unidad_academica (id, institucion_id, nombre, sigla, descripcion, activo)
    VALUES (
        'c0000000-0000-0000-0000-000000000003'::uuid,
        v_inst_id,
        'Escuela de Ciencias Sociales',
        'ECSoc',
        'Escuela de Ciencias Sociales y Humanidades',
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    v_ua_ingenieria_id := 'c0000000-0000-0000-0000-000000000001'::uuid;
    v_ua_salud_id := 'c0000000-0000-0000-0000-000000000002'::uuid;

    -- 5. Carreras de Grado
    v_carrera_isi_id := 'd0000000-0000-0000-0000-000000000001'::uuid;
    v_carrera_lsi_id := 'd0000000-0000-0000-0000-000000000002'::uuid;
    v_carrera_enf_id := 'd0000000-0000-0000-0000-000000000003'::uuid;

    INSERT INTO carrera (id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria, modalidad, estado, titulo_otorgado, duracion_anios, activo)
    VALUES (
        v_carrera_isi_id,
        v_ua_ingenieria_id,
        'Ingeniería en Sistemas de Información',
        'ISI',
        'GRADO',
        'Ciencias Aplicadas',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Ingeniero/a en Sistemas de Información',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO carrera (id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria, modalidad, estado, titulo_otorgado, duracion_anios, activo)
    VALUES (
        v_carrera_lsi_id,
        v_ua_ingenieria_id,
        'Licenciatura en Sistemas de Información',
        'LSI',
        'GRADO',
        'Ciencias Aplicadas',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Licenciado/a en Sistemas de Información',
        4,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO carrera (id, unidad_academica_id, nombre, sigla, nivel, area_disciplinaria, modalidad, estado, titulo_otorgado, duracion_anios, activo)
    VALUES (
        v_carrera_enf_id,
        v_ua_salud_id,
        'Licenciatura en Enfermería',
        'LENF',
        'GRADO',
        'Ciencias de la Salud',
        'PRESENCIAL',
        'EN_FUNCIONAMIENTO',
        'Licenciado/a en Enfermería',
        5,
        TRUE
    )
    ON CONFLICT (id) DO NOTHING;

    -- 6. Asociación Carrera - Sede
    INSERT INTO carrera_sede (carrera_id, sede_id)
    VALUES
        (v_carrera_isi_id, v_sede_sarmientos_id),
        (v_carrera_lsi_id, v_sede_sarmientos_id),
        (v_carrera_enf_id, v_sede_sarmientos_id),
        (v_carrera_enf_id, v_sede_centro_id)
    ON CONFLICT (carrera_id, sede_id) DO NOTHING;

END $$;
