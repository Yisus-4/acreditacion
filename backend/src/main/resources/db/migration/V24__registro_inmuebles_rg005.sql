-- =============================================================================
-- V24__registro_inmuebles_rg005.sql -- Módulo de Autoevaluación e Infraestructura
-- Registro de Inmuebles, Espacios Físicos y Documentación de Respaldo (RG-005)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Catálogo tipo_espacio_fisico
-- ----------------------------------------------------------------------------
CREATE TABLE tipo_espacio_fisico (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    codigo         VARCHAR(50)  NOT NULL,
    denominacion   VARCHAR(150) NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_tipo_espacio_fisico PRIMARY KEY (id),
    CONSTRAINT uq_tipo_espacio_fisico_codigo UNIQUE (codigo)
);

INSERT INTO tipo_espacio_fisico (codigo, denominacion) VALUES
    ('AULA_COMUN', 'Aula Común'),
    ('LAB_INFORMATICA', 'Laboratorio de Informática'),
    ('LAB_QUIMICA_BIOL', 'Laboratorio Químico / Biológico'),
    ('CENTRO_DOCUMENTACION', 'Biblioteca y Centro de Documentación'),
    ('TALLER', 'Taller Tecnológico / Práctico'),
    ('GABINETE', 'Gabinete de Estudio / Investigación'),
    ('CAM', 'Centro de Atención Médica (CAM)'),
    ('OFICINA_ADMIN', 'Oficina / Gestión Académica');

-- ----------------------------------------------------------------------------
-- 2. Tabla inmueble (RG-005)
-- ----------------------------------------------------------------------------
CREATE TABLE inmueble (
    id                      UUID           NOT NULL DEFAULT gen_random_uuid(),
    institucion_id          UUID           NOT NULL,
    nombre                  VARCHAR(255)   NOT NULL,
    domicilio               VARCHAR(255)   NOT NULL,
    provincia_id            BIGINT         NOT NULL,
    departamento_id         BIGINT         NOT NULL,
    localidad_id            BIGINT         NOT NULL,
    propiedad_institucion   BOOLEAN        NOT NULL DEFAULT TRUE,
    condicion_tenencia      VARCHAR(50)    NOT NULL DEFAULT 'PROPIO', -- PROPIO, COMODATO, ALQUILER, CESION
    superficie_cubierta_m2  NUMERIC(10, 2),
    superficie_total_m2     NUMERIC(10, 2),
    observaciones           TEXT,
    verificado              BOOLEAN        NOT NULL DEFAULT FALSE,
    fecha_verificacion      TIMESTAMPTZ,
    activo                  BOOLEAN        NOT NULL DEFAULT TRUE,
    creado_en               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_inmueble PRIMARY KEY (id),
    CONSTRAINT fk_inmueble_institucion
        FOREIGN KEY (institucion_id) REFERENCES institucion (id) ON DELETE CASCADE,
    CONSTRAINT fk_inmueble_provincia
        FOREIGN KEY (provincia_id) REFERENCES geografia_provincia (id),
    CONSTRAINT fk_inmueble_departamento
        FOREIGN KEY (departamento_id) REFERENCES geografia_departamento (id),
    CONSTRAINT fk_inmueble_localidad
        FOREIGN KEY (localidad_id) REFERENCES geografia_localidad (id)
);

CREATE INDEX ix_inmueble_institucion_id ON inmueble (institucion_id);
CREATE INDEX ix_inmueble_condicion_tenencia ON inmueble (condicion_tenencia);

-- ----------------------------------------------------------------------------
-- 3. Tabla intermedia inmueble_carreras (Vinculación con Carreras)
-- ----------------------------------------------------------------------------
CREATE TABLE inmueble_carreras (
    inmueble_id UUID NOT NULL,
    carrera_id  UUID NOT NULL,

    CONSTRAINT pk_inmueble_carreras PRIMARY KEY (inmueble_id, carrera_id),
    CONSTRAINT fk_inmueble_carreras_inmueble
        FOREIGN KEY (inmueble_id) REFERENCES inmueble (id) ON DELETE CASCADE,
    CONSTRAINT fk_inmueble_carreras_carrera
        FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE CASCADE
);

CREATE INDEX ix_inmueble_carreras_carrera_id ON inmueble_carreras (carrera_id);

-- ----------------------------------------------------------------------------
-- 4. Tabla espacio_fisico
-- ----------------------------------------------------------------------------
CREATE TABLE espacio_fisico (
    id                      UUID           NOT NULL DEFAULT gen_random_uuid(),
    inmueble_id             UUID           NOT NULL,
    tipo_espacio_id         UUID           NOT NULL,
    denominacion            VARCHAR(255)   NOT NULL,
    capacidad_estudiantes   INTEGER        NOT NULL DEFAULT 0,
    superficie_m2           NUMERIC(10, 2),
    equipamiento_disponible TEXT,
    activo                  BOOLEAN        NOT NULL DEFAULT TRUE,
    creado_en               TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_espacio_fisico PRIMARY KEY (id),
    CONSTRAINT fk_espacio_fisico_inmueble
        FOREIGN KEY (inmueble_id) REFERENCES inmueble (id) ON DELETE CASCADE,
    CONSTRAINT fk_espacio_fisico_tipo
        FOREIGN KEY (tipo_espacio_id) REFERENCES tipo_espacio_fisico (id)
);

CREATE INDEX ix_espacio_fisico_inmueble_id ON espacio_fisico (inmueble_id);

-- ----------------------------------------------------------------------------
-- 5. Tabla inmueble_documentacion_respaldo (Carpeta MinIO: Normativa)
-- ----------------------------------------------------------------------------
CREATE TABLE inmueble_documentacion_respaldo (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    inmueble_id           UUID         NOT NULL,
    nombre_archivo        VARCHAR(255) NOT NULL,
    url_minio             VARCHAR(500) NOT NULL,
    ruta_repositorio      VARCHAR(500) NOT NULL DEFAULT '03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa',
    tipo_anexo            VARCHAR(100) NOT NULL, -- CONVENIO_COMODATO, CONTRATO_LOCACION, ESCRITURA_TITULO, OTRO
    descripcion           TEXT,
    subido_por_usuario_id UUID,
    fecha_subida          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_inmueble_documentacion_respaldo PRIMARY KEY (id),
    CONSTRAINT fk_inmueble_doc_inmueble
        FOREIGN KEY (inmueble_id) REFERENCES inmueble (id) ON DELETE CASCADE,
    CONSTRAINT fk_inmueble_doc_usuario
        FOREIGN KEY (subido_por_usuario_id) REFERENCES usuario (id) ON DELETE SET NULL
);

CREATE INDEX ix_inmueble_doc_inmueble_id ON inmueble_documentacion_respaldo (inmueble_id);

-- ----------------------------------------------------------------------------
-- 6. Permisos de Seguridad
-- ----------------------------------------------------------------------------
INSERT INTO permiso (codigo, nombre, descripcion) VALUES
    ('AUTOEVALUACION_READ', 'Consultar módulo de Autoevaluación', 'Permite consultar las 6 dimensiones y registros de autoevaluación.'),
    ('AUTOEVALUACION_WRITE', 'Gestionar registros de Autoevaluación', 'Permite cargar y actualizar información de autoevaluación e infraestructura.')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo IN ('ADMINISTRATOR', 'AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA')
  AND p.codigo IN ('AUTOEVALUACION_READ', 'AUTOEVALUACION_WRITE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 7. Datos iniciales de demostración para UNdeC
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_inst_id UUID;
    v_prov_id BIGINT;
    v_depto_id BIGINT;
    v_loc_id BIGINT;
    v_inmueble_1_id UUID := 'b1000000-0000-0000-0000-000000000001'::uuid;
    v_inmueble_2_id UUID := 'b1000000-0000-0000-0000-000000000002'::uuid;
    v_inmueble_3_id UUID := 'b1000000-0000-0000-0000-000000000003'::uuid;
    v_tipo_aula UUID;
    v_tipo_lab UUID;
    v_tipo_biblio UUID;
    v_tipo_cam UUID;
    v_tipo_taller UUID;
    v_carrera_isi UUID;
    v_carrera_lsi UUID;
    v_carrera_enf UUID;
BEGIN
    SELECT id INTO v_inst_id FROM institucion WHERE sigla = 'UNdeC' LIMIT 1;
    IF v_inst_id IS NULL THEN
        SELECT id INTO v_inst_id FROM institucion LIMIT 1;
    END IF;

    SELECT id INTO v_prov_id FROM geografia_provincia WHERE codigo = 'LR' LIMIT 1;
    SELECT id INTO v_depto_id FROM geografia_departamento WHERE provincia_id = v_prov_id AND nombre ILIKE '%Chilecito%' LIMIT 1;
    SELECT id INTO v_loc_id FROM geografia_localidad WHERE departamento_id = v_depto_id LIMIT 1;

    SELECT id INTO v_tipo_aula FROM tipo_espacio_fisico WHERE codigo = 'AULA_COMUN';
    SELECT id INTO v_tipo_lab FROM tipo_espacio_fisico WHERE codigo = 'LAB_INFORMATICA';
    SELECT id INTO v_tipo_biblio FROM tipo_espacio_fisico WHERE codigo = 'CENTRO_DOCUMENTACION';
    SELECT id INTO v_tipo_cam FROM tipo_espacio_fisico WHERE codigo = 'CAM';
    SELECT id INTO v_tipo_taller FROM tipo_espacio_fisico WHERE codigo = 'TALLER';

    SELECT id INTO v_carrera_isi FROM carrera WHERE nombre ILIKE '%Sistemas de Información%' LIMIT 1;
    SELECT id INTO v_carrera_lsi FROM carrera WHERE nombre ILIKE '%Sistemas%' AND id <> v_carrera_isi LIMIT 1;
    SELECT id INTO v_carrera_enf FROM carrera WHERE nombre ILIKE '%Enfermería%' LIMIT 1;

    IF v_inst_id IS NOT NULL AND v_prov_id IS NOT NULL AND v_depto_id IS NOT NULL AND v_loc_id IS NOT NULL THEN
        -- Inmueble 1: Campus Los Sarmientos
        INSERT INTO inmueble (id, institucion_id, nombre, domicilio, provincia_id, departamento_id, localidad_id,
                              propiedad_institucion, condicion_tenencia, superficie_cubierta_m2, superficie_total_m2,
                              observaciones, verificado, activo)
        VALUES (v_inmueble_1_id, v_inst_id, 'Campus Los Sarmientos - Pabellón A', 'Ruta Prov. 12 Km 2.5',
                v_prov_id, v_depto_id, v_loc_id, TRUE, 'PROPIO', 1250.00, 2800.00,
                'Sede principal con aulas comunes, biblioteca y laboratorios centrales.', TRUE, TRUE)
        ON CONFLICT (id) DO NOTHING;

        -- Espacios Inmueble 1
        IF v_tipo_aula IS NOT NULL THEN
            INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, capacidad_estudiantes, superficie_m2)
            VALUES (v_inmueble_1_id, v_tipo_aula, 'Aula Magna 1', 80, 110.00);
        END IF;
        IF v_tipo_lab IS NOT NULL THEN
            INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, capacidad_estudiantes, superficie_m2, equipamiento_disponible)
            VALUES (v_inmueble_1_id, v_tipo_lab, 'Laboratorio de Redes y Software', 30, 65.00, '25 puestos de trabajo con PC');
        END IF;
        IF v_tipo_biblio IS NOT NULL THEN
            INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, capacidad_estudiantes, superficie_m2)
            VALUES (v_inmueble_1_id, v_tipo_biblio, 'Biblioteca Central y Sala de Lectura', 50, 140.00);
        END IF;

        -- Carreras Inmueble 1
        IF v_carrera_isi IS NOT NULL THEN
            INSERT INTO inmueble_carreras (inmueble_id, carrera_id) VALUES (v_inmueble_1_id, v_carrera_isi) ON CONFLICT DO NOTHING;
        END IF;
        IF v_carrera_lsi IS NOT NULL THEN
            INSERT INTO inmueble_carreras (inmueble_id, carrera_id) VALUES (v_inmueble_1_id, v_carrera_lsi) ON CONFLICT DO NOTHING;
        END IF;

        -- Inmueble 2: Sede Centro - CAM
        INSERT INTO inmueble (id, institucion_id, nombre, domicilio, provincia_id, departamento_id, localidad_id,
                              propiedad_institucion, condicion_tenencia, superficie_cubierta_m2, superficie_total_m2,
                              observaciones, verificado, activo)
        VALUES (v_inmueble_2_id, v_inst_id, 'Sede Centro - Centro de Atención Médica (CAM)', 'Calle 9 de Julio 345',
                v_prov_id, v_depto_id, v_loc_id, FALSE, 'COMODATO', 480.00, 600.00,
                'Inmueble cedido por el Ministerio de Salud para prácticas formativas.', TRUE, TRUE)
        ON CONFLICT (id) DO NOTHING;

        IF v_tipo_cam IS NOT NULL THEN
            INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, capacidad_estudiantes, superficie_m2)
            VALUES (v_inmueble_2_id, v_tipo_cam, 'Consultorios de Práctica Clínica', 20, 120.00);
        END IF;

        IF v_carrera_enf IS NOT NULL THEN
            INSERT INTO inmueble_carreras (inmueble_id, carrera_id) VALUES (v_inmueble_2_id, v_carrera_enf) ON CONFLICT DO NOTHING;
        END IF;

        INSERT INTO inmueble_documentacion_respaldo (inmueble_id, nombre_archivo, url_minio, ruta_repositorio, tipo_anexo, descripcion)
        VALUES (v_inmueble_2_id, 'Convenio_Comodato_CAM_2025_2028.pdf', '/minio/acreditacion/03_01/Convenio_Comodato_CAM_2025_2028.pdf',
                '03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa',
                'CONVENIO_COMODATO', 'Convenio tripartito de comodato con disponibilidad garantizada');

        -- Inmueble 3: Anexo Talleres Tecnológicos
        INSERT INTO inmueble (id, institucion_id, nombre, domicilio, provincia_id, departamento_id, localidad_id,
                              propiedad_institucion, condicion_tenencia, superficie_cubierta_m2, superficie_total_m2,
                              observaciones, verificado, activo)
        VALUES (v_inmueble_3_id, v_inst_id, 'Anexo Talleres Tecnológicos', 'Av. San Martín 820',
                v_prov_id, v_depto_id, v_loc_id, FALSE, 'ALQUILER', 320.00, 450.00,
                'Alquiler para talleres y extensión territorial.', FALSE, TRUE)
        ON CONFLICT (id) DO NOTHING;

        IF v_tipo_taller IS NOT NULL THEN
            INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, capacidad_estudiantes, superficie_m2)
            VALUES (v_inmueble_3_id, v_tipo_taller, 'Taller de Prototipado y Robótica', 25, 95.00);
        END IF;

        IF v_carrera_isi IS NOT NULL THEN
            INSERT INTO inmueble_carreras (inmueble_id, carrera_id) VALUES (v_inmueble_3_id, v_carrera_isi) ON CONFLICT DO NOTHING;
        END IF;
    END IF;
END $$;
