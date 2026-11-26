-- =====================================================================
-- V50: Auditoría de Cambios (RF08 / RN14), Trazabilidad de Clonación
--      por Renglón (RF07 / RN11-RN13) y Flujo SGA (RF09 / RN15)
-- =====================================================================

ALTER TABLE solicitud_docente_designacion
    ADD COLUMN IF NOT EXISTS designacion_origen_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_sol_doc_desig_origen'
    ) THEN
        ALTER TABLE solicitud_docente_designacion
            ADD CONSTRAINT fk_sol_doc_desig_origen
            FOREIGN KEY (designacion_origen_id)
            REFERENCES solicitud_docente_designacion(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sol_doc_desig_origen
    ON solicitud_docente_designacion(designacion_origen_id);

CREATE TABLE IF NOT EXISTS solicitud_designacion_cambio (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_id       UUID         NOT NULL REFERENCES solicitud_designacion(id) ON DELETE CASCADE,
    designacion_id     UUID         NULL REFERENCES solicitud_docente_designacion(id) ON DELETE SET NULL,
    tipo_evento        VARCHAR(40)  NOT NULL,
    campo_modificado   VARCHAR(100) NULL,
    valor_anterior     TEXT         NULL,
    valor_nuevo        TEXT         NULL,
    motivo             TEXT         NULL,
    usuario_id         UUID         NULL REFERENCES usuario(id) ON DELETE SET NULL,
    usuario_nombre     VARCHAR(320) NULL,
    fecha_cambio       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sol_desig_cambio_tipo_evento CHECK (tipo_evento IN (
        'CREACION',
        'CLONACION_PERIODO_ANTERIOR',
        'ALTA_DESIGNACION',
        'MODIFICACION_DESIGNACION',
        'MODIFICACION_DISTRIBUCION',
        'EXCLUSION_DESIGNACION',
        'REACTIVACION_DESIGNACION',
        'ALTA_ASIGNACION',
        'BAJA_ASIGNACION',
        'PRESENTACION_SGA',
        'APROBACION_SGA',
        'OBSERVACION_SGA'
    ))
);

CREATE INDEX IF NOT EXISTS idx_sol_desig_cambio_solicitud
    ON solicitud_designacion_cambio(solicitud_id, fecha_cambio DESC);

CREATE INDEX IF NOT EXISTS idx_sol_desig_cambio_designacion
    ON solicitud_designacion_cambio(designacion_id);
