-- V14__crear_rol_director_escuela.sql
-- Creación del rol Director de Escuela (SCHOOL_DIRECTOR) para confirmación de convocatorias

INSERT INTO rol (codigo, nombre, descripcion, activo)
VALUES (
    'SCHOOL_DIRECTOR',
    'Director de Escuela',
    'Dirección de escuela académica y confirmación de carreras para convocatorias de acreditación.',
    TRUE
)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'CONVOCATORIA_READ',
    'CONVOCATORIA_WRITE',
    'ESTRUCTURA_ACADEMICA_READ',
    'ACCOUNT_SELF_READ',
    'ACCOUNT_SELF_UPDATE_PASSWORD',
    'SECURITY_USER_READ',
    'SECURITY_ROLE_READ'
)
WHERE r.codigo = 'SCHOOL_DIRECTOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
