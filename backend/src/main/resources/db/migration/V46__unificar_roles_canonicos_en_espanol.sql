-- =============================================================================
-- V46__unificar_roles_canonicos_en_espanol.sql
-- Consolida los roles duplicados heredados en inglés en sus equivalentes
-- canónicos institucionales en español, preservando permisos y usuarios:
--   CAREER_DIRECTOR                  -> DIRECTOR_CARRERA
--   SCHOOL_DIRECTOR                  -> DIRECTOR_ESCUELA
--   SELF_EVALUATION_COMMITTEE_MEMBER -> COMISION_AUTOEVALUACION
--   AC_COORDINATOR / COORDINADOR_AEA -> AEA
--   INFORMATION_PROVIDER             -> PROVEEDOR_INFORMACION
-- =============================================================================

-- 1. Asegurar la existencia de todos los roles canónicos institucionales
INSERT INTO rol (codigo, nombre, descripcion, es_sistema, activo)
VALUES
    ('DIRECTOR_ESCUELA',
     'Director de Escuela',
     'Dirección de escuela académica y confirmación de carreras para convocatorias de acreditación.',
     TRUE, TRUE),
    ('DIRECTOR_CARRERA',
     'Director de Carrera',
     'Dirige y gestiona la carrera en el proceso de autoevaluación y acreditación.',
     TRUE, TRUE),
    ('DIRECTOR_DEPARTAMENTO',
     'Director de Departamento',
     'Coordina actividades curriculares y cuerpo docente del departamento académico.',
     TRUE, TRUE),
    ('COMISION_AUTOEVALUACION',
     'Miembro de Comisión de Autoevaluación',
     'Integra la Comisión de Autoevaluación (CA) de la carrera.',
     TRUE, TRUE),
    ('BIBLIOTECA',
     'Responsable de Biblioteca',
     'Gestiona información bibliográfica y equipamiento de biblioteca (RG-014).',
     TRUE, TRUE),
    ('PROVEEDOR_INFORMACION',
     'Proveedor de Información',
     'Provee información solicitada durante el proceso de acreditación.',
     TRUE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET es_sistema = TRUE,
    activo = TRUE;

-- 2. Transferir permisos (rol_permiso) desde roles obsoletos hacia roles canónicos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r_dest.id, rp.permiso_id
FROM rol_permiso rp
JOIN rol r_old ON rp.rol_id = r_old.id
JOIN rol r_dest ON r_dest.codigo = CASE r_old.codigo
    WHEN 'CAREER_DIRECTOR' THEN 'DIRECTOR_CARRERA'
    WHEN 'SCHOOL_DIRECTOR' THEN 'DIRECTOR_ESCUELA'
    WHEN 'SELF_EVALUATION_COMMITTEE_MEMBER' THEN 'COMISION_AUTOEVALUACION'
    WHEN 'AC_COORDINATOR' THEN 'AEA'
    WHEN 'COORDINADOR_AEA' THEN 'AEA'
    WHEN 'INFORMATION_PROVIDER' THEN 'PROVEEDOR_INFORMACION'
    WHEN 'DEV' THEN 'AEA'
    WHEN 'DEVELOPER' THEN 'AEA'
END
WHERE r_old.codigo IN (
    'CAREER_DIRECTOR',
    'SCHOOL_DIRECTOR',
    'SELF_EVALUATION_COMMITTEE_MEMBER',
    'AC_COORDINATOR',
    'COORDINADOR_AEA',
    'INFORMATION_PROVIDER',
    'DEV',
    'DEVELOPER'
)
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- 3. Migrar asignaciones de usuarios (usuario_rol) desde roles obsoletos hacia roles canónicos
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT ur.usuario_id, r_dest.id
FROM usuario_rol ur
JOIN rol r_old ON ur.rol_id = r_old.id
JOIN rol r_dest ON r_dest.codigo = CASE r_old.codigo
    WHEN 'CAREER_DIRECTOR' THEN 'DIRECTOR_CARRERA'
    WHEN 'SCHOOL_DIRECTOR' THEN 'DIRECTOR_ESCUELA'
    WHEN 'SELF_EVALUATION_COMMITTEE_MEMBER' THEN 'COMISION_AUTOEVALUACION'
    WHEN 'AC_COORDINATOR' THEN 'AEA'
    WHEN 'COORDINADOR_AEA' THEN 'AEA'
    WHEN 'INFORMATION_PROVIDER' THEN 'PROVEEDOR_INFORMACION'
    WHEN 'DEV' THEN 'AEA'
    WHEN 'DEVELOPER' THEN 'AEA'
END
WHERE r_old.codigo IN (
    'CAREER_DIRECTOR',
    'SCHOOL_DIRECTOR',
    'SELF_EVALUATION_COMMITTEE_MEMBER',
    'AC_COORDINATOR',
    'COORDINADOR_AEA',
    'INFORMATION_PROVIDER',
    'DEV',
    'DEVELOPER'
)
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

-- 4. Eliminar definitivamente los roles duplicados/obsoletos (ON DELETE CASCADE limpia pivotes)
DELETE FROM rol
WHERE codigo IN (
    'CAREER_DIRECTOR',
    'SCHOOL_DIRECTOR',
    'SELF_EVALUATION_COMMITTEE_MEMBER',
    'AC_COORDINATOR',
    'COORDINADOR_AEA',
    'INFORMATION_PROVIDER',
    'DEV',
    'DEVELOPER'
);
