-- =============================================================================
-- V26__guia_higiene_seguridad_gt003.sql
-- Módulo de Autoevaluación e Infraestructura
-- GT-003: Guía de Higiene y Seguridad de la UA y Documentación de Respaldo Mínima
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Tabla: guia_higiene_seguridad (GT-003)
-- ----------------------------------------------------------------------------
CREATE TABLE guia_higiene_seguridad (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),
    proceso_id                  UUID,
    institucion_id              UUID         NOT NULL,
    es_maestro                  BOOLEAN      NOT NULL DEFAULT TRUE,
    edicion                     VARCHAR(50)  NOT NULL DEFAULT '01',
    fecha_vigencia              DATE,
    elaborado_por               VARCHAR(255) DEFAULT 'Mara Rovero (Analista funcional DEA)',
    revisado_por                VARCHAR(255) DEFAULT 'Fernanda Beatriz Carmona / Carolina Manrique (SGI - DEA)',
    aprobado_por                VARCHAR(255),
    instancia_responsable_ua    TEXT,        -- Punto 6.2: Máx 2000 caracteres
    politica_unidades_apoyo     TEXT,        -- Punto 5.1: Máx 1600 caracteres
    aplica_unidades_apoyo       BOOLEAN      NOT NULL DEFAULT FALSE,
    verificado                  BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_verificacion          TIMESTAMPTZ,
    fecha_solicitud             TIMESTAMPTZ,
    fecha_limite                DATE,
    estado                      VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, EN_REVISION, COMPLETADO
    observaciones               TEXT,
    creado_en                   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en              TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_guia_higiene_seguridad PRIMARY KEY (id),
    CONSTRAINT fk_guia_higiene_institucion
        FOREIGN KEY (institucion_id) REFERENCES institucion (id) ON DELETE CASCADE
);

CREATE INDEX ix_guia_higiene_proceso_id ON guia_higiene_seguridad (proceso_id);
CREATE INDEX ix_guia_higiene_es_maestro ON guia_higiene_seguridad (es_maestro);

-- ----------------------------------------------------------------------------
-- 2. Tabla: guia_higiene_seguridad_documento (Documentación de Respaldo y Normativa)
-- ----------------------------------------------------------------------------
CREATE TABLE guia_higiene_seguridad_documento (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),
    guia_id                     UUID         NOT NULL,
    tipo_documento_clave        VARCHAR(100) NOT NULL, -- CERTIFICADO_ANUAL_INMUEBLES, CERTIFICADO_MUNICIPAL_BOMBEROS, PLAN_ANUAL_ACCION, INFORME_SEGUIMIENTO_ANUAL, NORMATIVA_RESPALDO
    tipo_documento_nombre       VARCHAR(200) NOT NULL,
    organismo_emisor            VARCHAR(255),
    numero_normativa            VARCHAR(100),
    descripcion                 TEXT,
    nombre_archivo              VARCHAR(255),
    url_minio                   VARCHAR(500),
    ruta_repositorio            VARCHAR(500) NOT NULL DEFAULT '03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa',
    obligatorio                 BOOLEAN      NOT NULL DEFAULT FALSE,
    cargado                     BOOLEAN      NOT NULL DEFAULT FALSE,
    subido_por_usuario_id       UUID,
    fecha_subida                TIMESTAMPTZ,
    creado_en                   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_guia_higiene_doc PRIMARY KEY (id),
    CONSTRAINT fk_guia_higiene_doc_guia
        FOREIGN KEY (guia_id) REFERENCES guia_higiene_seguridad (id) ON DELETE CASCADE
);

CREATE INDEX ix_guia_higiene_doc_guia_id ON guia_higiene_seguridad_documento (guia_id);

-- ----------------------------------------------------------------------------
-- 3. Semilla Inicial: GT-003 Institucional Maestra
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_inst_id UUID;
    v_guia_id UUID := 'c1000000-0000-0000-0000-000000000001';
BEGIN
    SELECT id INTO v_inst_id FROM institucion LIMIT 1;

    INSERT INTO guia_higiene_seguridad (
        id, institucion_id, es_maestro, edicion, fecha_vigencia,
        elaborado_por, revisado_por,
        instancia_responsable_ua,
        politica_unidades_apoyo, aplica_unidades_apoyo,
        verificado, estado, observaciones
    ) VALUES (
        v_guia_id, v_inst_id, TRUE, '01', '2025-05-16',
        'Mara Rovero (Analista funcional DEA)',
        'Fernanda Beatriz Carmona / Carolina Manrique (Secretaría de Gestión Institucional - DEA)',
        'El Área de Higiene, Seguridad y Medio Ambiente Laboral, dependiente de la Secretaría de Gestión Institucional de la Universidad Nacional de Chilecito, es la instancia responsable de la formulación, supervisión, verificación de condiciones edilicias, protocolos de evacuación, señalética, matafuegos y cumplimiento de normas de salubridad y bioseguridad en todas las sedes y unidades académicas de la institución.',
        'La política institucional establece que toda unidad de apoyo externa o anexo vinculado a prácticas formativas de grado debe contar con verificación previa de condiciones de seguridad e higiene, seguro de responsabilidad civil y certificado de habilitación municipal/bomberos vigente avalado por el responsable del área.',
        TRUE,
        TRUE, 'COMPLETADO',
        'Guía oficial GT-003 verificada y remitida a DEA para guarda en subcarpeta Normativa de 03_01.'
    ) ON CONFLICT (id) DO NOTHING;

    -- Documentos mínimos obligatorios
    INSERT INTO guia_higiene_seguridad_documento (
        guia_id, tipo_documento_clave, tipo_documento_nombre, organismo_emisor, numero_normativa,
        descripcion, nombre_archivo, url_minio, obligatorio, cargado, fecha_subida
    ) VALUES
        (v_guia_id, 'CERTIFICADO_ANUAL_INMUEBLES', 'Certificado de Higiene y Seguridad de Inmuebles',
         'Área de Higiene, Seguridad y Medio Ambiente Laboral UNdeC', 'CERT-HYSL-2026-001',
         'Certificado emitido al inicio del año académico en curso para todos los inmuebles de la institución.',
         'Certificado_HyS_Inmuebles_UNdeC_2026.pdf',
         '/minio/acreditacion/03_01/Certificado_HyS_Inmuebles_UNdeC_2026.pdf',
         TRUE, TRUE, now()),

        (v_guia_id, 'CERTIFICADO_MUNICIPAL_BOMBEROS', 'Certificado Municipal y de Bomberos Vigente',
         'Cuerpo de Bomberos de la Policía de La Rioja / Municipalidad de Chilecito', 'HAB-BOMB-CHIL-2026',
         'Certificado de habilitación de seguridad contra incendios, sistemas de evacuación y desinfección vigente.',
         'Certificado_Bomberos_Municipal_Chilecito_2026.pdf',
         '/minio/acreditacion/03_01/Certificado_Bomberos_Municipal_Chilecito_2026.pdf',
         TRUE, TRUE, now()),

        (v_guia_id, 'PLAN_ANUAL_ACCION', 'Plan Anual de Acción en Higiene y Seguridad',
         'Secretaría de Gestión Institucional', 'RES-SGI-045/2026',
         'Plan anual de acción con cronograma de simulacros, recarga de matafuegos, capacitaciones y mantenimiento edilicio a realizar en el año en curso.',
         'Plan_Anual_Accion_HyS_2026.pdf',
         '/minio/acreditacion/03_01/Plan_Anual_Accion_HyS_2026.pdf',
         TRUE, TRUE, now()),

        (v_guia_id, 'INFORME_SEGUIMIENTO_ANUAL', 'Informe de Seguimiento del Plan Anual del Año Anterior',
         'Área de Higiene y Seguridad UNdeC', 'INF-HYSL-012/2025',
         'Informe de resultados, auditorías realizadas y nivel de cumplimiento del plan de higiene y seguridad del ciclo anterior.',
         'Informe_Seguimiento_Plan_HyS_2025.pdf',
         '/minio/acreditacion/03_01/Informe_Seguimiento_Plan_HyS_2025.pdf',
         TRUE, TRUE, now()),

        (v_guia_id, 'NORMATIVA_RESPALDO', 'Ordenanza de Creación del Área de Higiene y Seguridad Laboral',
         'Consejo Superior UNdeC', 'Ordenanza HCS Nº 008/2018',
         'Normativa institucional que regula la política de seguridad y salud en el trabajo en la universidad.',
         'Ord_HCS_008_2018_Reglamento_HyS.pdf',
         '/minio/acreditacion/03_01/Ord_HCS_008_2018_Reglamento_HyS.pdf',
         FALSE, TRUE, now());

END $$;
