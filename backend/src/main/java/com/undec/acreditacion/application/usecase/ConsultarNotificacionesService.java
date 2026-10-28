package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarNotificacionesUseCase;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultarNotificacionesService implements ConsultarNotificacionesUseCase {

    private final SolicitudNotificacionRepositoryPort repository;

    public ConsultarNotificacionesService(SolicitudNotificacionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<SolicitudNotificacionDto> listar(String rol, String estado) {
        return repository.listar(rol, estado).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long contarPendientes(String rol) {
        return repository.contarPendientes(rol);
    }

    public SolicitudNotificacionDto toDto(SolicitudNotificacion s) {
        if (s == null) return null;
        return new SolicitudNotificacionDto(
                s.getId(),
                s.getTipo(),
                s.getTitulo(),
                s.getMensaje(),
                s.getDatosPayload(),
                s.getProcesoId(),
                s.getCarreraNombre(),
                s.getSolicitadoPor(),
                s.getSolicitadoPorNombre(),
                s.getDestinatarioRol(),
                s.getEstado(),
                s.isLeida(),
                s.getResueltoPor(),
                s.getResueltoEn(),
                s.getCreadoEn()
        );
    }
}
