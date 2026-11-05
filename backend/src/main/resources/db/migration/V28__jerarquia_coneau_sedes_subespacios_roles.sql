-- =============================================================================
-- V28__jerarquia_coneau_sedes_subespacios_roles.sql
-- 1. Nueva entidad oficial Unidad Académica (CONEAU Global)
-- 2. Vinculación de Sedes a Unidad Académica
-- 3. Vinculación obligatoria de Inmuebles a Sede
-- 4. Soporte para Subespacios (1 nivel), Observaciones de Higiene y Seguridad
-- 5. Nuevos roles: INFRAESTRUCTURA, BEDELIA, HIGIENE_SEGURIDAD y permisos
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Tabla unidad_academica_coneau
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS unidad_academica_coneau (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    institucion_id UUID          NOT NULL,
    denominacion   VARCHAR(255)  NOT NULL,
    activa         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_unidad_academica_coneau PRIMARY KEY (id),
    CONSTRAINT fk_unidad_academica_coneau_institucion
        FOREIGN KEY (institucion_id) REFERENCES institucion (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_unidad_academica_coneau_institucion_id 
    ON unidad_academica_coneau (institucion_id);

-- Sembrar Unidad Académica oficial CONEAU de la UNdeC
DO $$
DECLARE
    v_inst_id UUID;
    v_ua_id UUID := 'a0000000-0000-0000-0000-000000000001'::uuid;
BEGIN
    SELECT id INTO v_inst_id FROM institucion LIMIT 1;
    IF v_inst_id IS NOT NULL THEN
        INSERT INTO unidad_academica_coneau (id, institucion_id, denominacion, activa, creado_en, actualizado_en)
        VALUES (v_ua_id, v_inst_id, 'Universidad Nacional de Chilecito (Undec)', TRUE, now(), now())
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. Vinculación de Sede Localización a Unidad Académica
-- ----------------------------------------------------------------------------
ALTER TABLE sede_localizacion
    ADD COLUMN IF NOT EXISTS unidad_academica_id UUID;

DO $$
DECLARE
    v_ua_id UUID := 'a0000000-0000-0000-0000-000000000001'::uuid;
BEGIN
    -- Asignar la UA existente por defecto a las sedes actuales
    UPDATE sede_localizacion
    SET unidad_academica_id = v_ua_id
    WHERE unidad_academica_id IS NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_sede_localizacion_unidad_academica'
    ) THEN
        ALTER TABLE sede_localizacion
            ADD CONSTRAINT fk_sede_localizacion_unidad_academica
            FOREIGN KEY (unidad_academica_id) REFERENCES unidad_academica_coneau (id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_sede_localizacion_unidad_academica_id 
    ON sede_localizacion (unidad_academica_id);

-- ----------------------------------------------------------------------------
-- 3. Vinculación de Inmueble a Sede Localización
-- ----------------------------------------------------------------------------
ALTER TABLE inmueble
    ADD COLUMN IF NOT EXISTS sede_id UUID;

DO $$
DECLARE
    v_sede_sarmientos UUID := 'b0000000-0000-0000-0000-000000000001'::uuid;
    v_sede_centro UUID := 'b0000000-0000-0000-0000-000000000002'::uuid;
BEGIN
    -- Si el inmueble dice "Centro", asignarle Sede Centro; al resto, Campus Los Sarmientos
    UPDATE inmueble
    SET sede_id = v_sede_centro
    WHERE sede_id IS NULL AND (nombre ILIKE '%Centro%' OR domicilio ILIKE '%Centro%' OR domicilio ILIKE '%9 de Julio%');

    UPDATE inmueble
    SET sede_id = v_sede_sarmientos
    WHERE sede_id IS NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_inmueble_sede'
    ) THEN
        ALTER TABLE inmueble
            ADD CONSTRAINT fk_inmueble_sede
            FOREIGN KEY (sede_id) REFERENCES sede_localizacion (id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_inmueble_sede_id ON inmueble (sede_id);

-- ----------------------------------------------------------------------------
-- 4. Soporte para Subespacios y Observaciones de Higiene y Seguridad
-- ----------------------------------------------------------------------------
ALTER TABLE espacio_fisico
    ADD COLUMN IF NOT EXISTS espacio_padre_id UUID,
    ADD COLUMN IF NOT EXISTS tiene_subespacios BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS observacion_higiene_seguridad TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_espacio_fisico_padre'
    ) THEN
        ALTER TABLE espacio_fisico
            ADD CONSTRAINT fk_espacio_fisico_padre
            FOREIGN KEY (espacio_padre_id) REFERENCES espacio_fisico (id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_espacio_fisico_padre_id ON espacio_fisico (espacio_padre_id);
CREATE INDEX IF NOT EXISTS ix_espacio_fisico_superficie_m2 ON espacio_fisico (superficie_m2);

-- ----------------------------------------------------------------------------
-- 5. Roles y Permisos de Seguridad
-- ----------------------------------------------------------------------------
INSERT INTO rol (codigo, nombre, descripcion, es_sistema) VALUES
    ('INFRAESTRUCTURA', 'Responsable de Infraestructura', 'Gestión integral del relevamiento edilicio, inmuebles y espacios físicos (RG-005).', TRUE),
    ('BEDELIA', 'Bedelía', 'Gestión de espacios físicos, aulas y logística edilicia para la docencia.', TRUE),
    ('HIGIENE_SEGURIDAD', 'Higiene y Seguridad', 'Auditoría de condiciones edilicias, carga de observaciones de seguridad en espacios y gestión de Guía GT-003.', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO permiso (codigo, nombre, descripcion) VALUES
    ('INFRAESTRUCTURA_READ', 'Consultar infraestructura', 'Permite consultar el registro de inmuebles y espacios físicos.'),
    ('INFRAESTRUCTURA_WRITE', 'Gestionar infraestructura', 'Permite crear, modificar y dar de baja inmuebles y espacios físicos.'),
    ('INFRAESTRUCTURA_HYS_WRITE', 'Modificar HyS en espacios', 'Permite cargar y actualizar observaciones de Higiene y Seguridad en espacios físicos.'),
    ('HIGIENE_SEGURIDAD_READ', 'Consultar Guía HyS', 'Permite consultar la Guía de Higiene y Seguridad laboral (GT-003).'),
    ('HIGIENE_SEGURIDAD_WRITE', 'Gestionar Guía HyS', 'Permite actualizar y cargar normativa en la Guía de Higiene y Seguridad laboral (GT-003).')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a ADMINISTRATOR y AEA (acceso total)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo IN ('ADMINISTRATOR', 'AEA')
  AND p.codigo IN (
      'INFRAESTRUCTURA_READ', 'INFRAESTRUCTURA_WRITE', 'INFRAESTRUCTURA_HYS_WRITE',
      'HIGIENE_SEGURIDAD_READ', 'HIGIENE_SEGURIDAD_WRITE'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignar permisos a INFRAESTRUCTURA
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'INFRAESTRUCTURA'
  AND p.codigo IN (
      'INFRAESTRUCTURA_READ', 'INFRAESTRUCTURA_WRITE', 'INFRAESTRUCTURA_HYS_WRITE',
      'HIGIENE_SEGURIDAD_READ', 'AUTOEVALUACION_READ'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignar permisos a BEDELIA
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'BEDELIA'
  AND p.codigo IN (
      'INFRAESTRUCTURA_READ', 'INFRAESTRUCTURA_WRITE', 'INFRAESTRUCTURA_HYS_WRITE',
      'HIGIENE_SEGURIDAD_READ', 'AUTOEVALUACION_READ'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Asignar permisos a HIGIENE_SEGURIDAD (solo lectura en infra + escritura en HyS)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'HIGIENE_SEGURIDAD'
  AND p.codigo IN (
      'INFRAESTRUCTURA_READ', 'INFRAESTRUCTURA_HYS_WRITE',
      'HIGIENE_SEGURIDAD_READ', 'HIGIENE_SEGURIDAD_WRITE',
      'AUTOEVALUACION_READ'
  )
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Corregir asignación histórica de AUTOEVALUACION_* a SCHOOL_DIRECTOR y CAREER_DIRECTOR
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo IN ('SCHOOL_DIRECTOR', 'CAREER_DIRECTOR')
  AND p.codigo IN ('AUTOEVALUACION_READ', 'AUTOEVALUACION_WRITE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
