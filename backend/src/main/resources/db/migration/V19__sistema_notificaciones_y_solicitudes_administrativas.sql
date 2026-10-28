-- =============================================================================
-- V19__sistema_notificaciones_y_solicitudes_administrativas.sql
-- Sistema de Notificaciones y Solicitudes Administrativas Globales (AEA / ADMIN)
-- Desacopla las altas y trámites institucionales de las convocatorias específicas
-- =============================================================================

CREATE TABLE solicitud_notificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    datos_payload JSONB,
    proceso_id UUID REFERENCES proceso_acreditacion(id) ON DELETE SET NULL,
    carrera_nombre VARCHAR(255),
    solicitado_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    solicitado_por_nombre VARCHAR(255),
    destinatario_rol VARCHAR(50) NOT NULL DEFAULT 'AEA',
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    resuelto_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    resuelto_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_solicitud_notif_estado ON solicitud_notificacion (estado);
CREATE INDEX ix_solicitud_notif_dest_rol ON solicitud_notificacion (destinatario_rol);
CREATE INDEX ix_solicitud_notif_leida ON solicitud_notificacion (leida);
CREATE INDEX ix_solicitud_notif_proceso ON solicitud_notificacion (proceso_id);
CREATE INDEX ix_solicitud_notif_creado ON solicitud_notificacion (creado_en DESC);
