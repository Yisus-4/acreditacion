-- V42: Consolidar AEA como único rol administrador general del sistema y eliminar ADMINISTRATOR

-- 1. Asegurar el rol AEA con nombre y descripción de administrador general
INSERT INTO rol (codigo, nombre, descripcion, es_sistema, activo)
VALUES (
    'AEA',
    'Área de Evaluación y Acreditación (Administrador)',
    'Administrador general del sistema de evaluación y acreditación.',
    TRUE,
    TRUE
)
ON CONFLICT (codigo) DO UPDATE
SET nombre = 'Área de Evaluación y Acreditación (Administrador)',
    descripcion = 'Administrador general del sistema de evaluación y acreditación.',
    es_sistema = TRUE,
    activo = TRUE;

-- 2. Transferir a AEA todos los permisos que tenía ADMINISTRATOR y asegurar todos los permisos del sistema
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r_aea.id, p.id
FROM rol r_aea
CROSS JOIN permiso p
WHERE r_aea.codigo = 'AEA'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- 3. Migrar todos los usuarios con rol ADMINISTRATOR (incluido admin@undec.edu.ar) al rol AEA
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT ur.usuario_id, r_aea.id
FROM usuario_rol ur
JOIN rol r_adm ON ur.rol_id = r_adm.id
JOIN rol r_aea ON r_aea.codigo = 'AEA'
WHERE r_adm.codigo = 'ADMINISTRATOR'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

-- 4. Eliminar definitivamente el rol ADMINISTRATOR (ON DELETE CASCADE limpia usuario_rol y rol_permiso)
DELETE FROM rol WHERE codigo = 'ADMINISTRATOR';
