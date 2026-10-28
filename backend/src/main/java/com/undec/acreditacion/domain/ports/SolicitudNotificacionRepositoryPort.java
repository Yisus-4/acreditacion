package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.SolicitudNotificacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudNotificacionRepositoryPort {

    Optional<SolicitudNotificacion> findById(UUID id);

    List<SolicitudNotificacion> listar(String destinatarioRol, String estado);

    long contarPendientes(String destinatarioRol);

    Optional<SolicitudNotificacion> findPendienteByProcesoIdAndTitulo(UUID procesoId, String titulo);

    SolicitudNotificacion save(SolicitudNotificacion notificacion);
}
