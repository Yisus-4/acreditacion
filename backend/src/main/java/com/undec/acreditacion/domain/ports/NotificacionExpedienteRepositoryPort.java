package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.NotificacionExpediente;

import java.util.List;
import java.util.UUID;

public interface NotificacionExpedienteRepositoryPort {
    NotificacionExpediente save(NotificacionExpediente notificacion);
    List<NotificacionExpediente> findByProcesoId(UUID procesoId);
}
