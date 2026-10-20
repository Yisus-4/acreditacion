package com.undec.acreditacion.domain.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NotificacionExpediente {

    private final UUID id;
    private final UUID procesoId;
    private final TipoEventoNotificacion tipoEvento;
    private final String destinatarioEmail;
    private final String destinatarioNombre;
    private final String asunto;
    private final boolean enviadoExitosamente;
    private final String errorDetalle;
    private final Instant enviadoEn;

    public NotificacionExpediente(
        UUID id,
        UUID procesoId,
        TipoEventoNotificacion tipoEvento,
        String destinatarioEmail,
        String destinatarioNombre,
        String asunto,
        boolean enviadoExitosamente,
        String errorDetalle,
        Instant enviadoEn
    ) {
        this.id = Objects.requireNonNull(id, "El ID de la notificación es obligatorio");
        this.procesoId = Objects.requireNonNull(procesoId, "El ID del proceso es obligatorio");
        this.tipoEvento = Objects.requireNonNull(tipoEvento, "El tipo de evento es obligatorio");
        this.destinatarioEmail = Objects.requireNonNull(destinatarioEmail, "El email destinatario es obligatorio");
        this.destinatarioNombre = destinatarioNombre;
        this.asunto = Objects.requireNonNull(asunto, "El asunto es obligatorio");
        this.enviadoExitosamente = enviadoExitosamente;
        this.errorDetalle = errorDetalle;
        this.enviadoEn = enviadoEn != null ? enviadoEn : Instant.now();
    }

    public static NotificacionExpediente exitoso(
        UUID procesoId,
        TipoEventoNotificacion tipoEvento,
        String destinatarioEmail,
        String destinatarioNombre,
        String asunto
    ) {
        return new NotificacionExpediente(
            UUID.randomUUID(),
            procesoId,
            tipoEvento,
            destinatarioEmail,
            destinatarioNombre,
            asunto,
            true,
            null,
            Instant.now()
        );
    }

    public static NotificacionExpediente fallido(
        UUID procesoId,
        TipoEventoNotificacion tipoEvento,
        String destinatarioEmail,
        String destinatarioNombre,
        String asunto,
        String errorDetalle
    ) {
        return new NotificacionExpediente(
            UUID.randomUUID(),
            procesoId,
            tipoEvento,
            destinatarioEmail,
            destinatarioNombre,
            asunto,
            false,
            errorDetalle,
            Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public TipoEventoNotificacion getTipoEvento() {
        return tipoEvento;
    }

    public String getDestinatarioEmail() {
        return destinatarioEmail;
    }

    public String getDestinatarioNombre() {
        return destinatarioNombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public boolean isEnviadoExitosamente() {
        return enviadoExitosamente;
    }

    public String getErrorDetalle() {
        return errorDetalle;
    }

    public Instant getEnviadoEn() {
        return enviadoEn;
    }
}
