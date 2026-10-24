-- V15: Asociar usuario con Unidad Académica (Escuela) para control de visibilidad y acceso
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS unidad_academica_id UUID REFERENCES unidad_academica(id);

CREATE INDEX IF NOT EXISTS idx_usuario_unidad_academica_id ON usuario(unidad_academica_id);

-- Asignar la Escuela de Ingeniería (EI) al usuario jesus@undec.edu.ar
UPDATE usuario
SET unidad_academica_id = 'c0000000-0000-0000-0000-000000000001'::uuid
WHERE email = 'jesus@undec.edu.ar';
