package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresSolicitudNotificacionRepositoryAdapter implements SolicitudNotificacionRepositoryPort {

    private final SpringDataSolicitudNotificacionRepository repository;

    public PostgresSolicitudNotificacionRepositoryAdapter(SpringDataSolicitudNotificacionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SolicitudNotificacion> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SolicitudNotificacion> listar(String destinatarioRol, String estado) {
        List<SolicitudNotificacionJpaEntity> entities;
        boolean hasRol = destinatarioRol != null && !destinatarioRol.isBlank() && !"ALL".equalsIgnoreCase(destinatarioRol);
        boolean hasEstado = estado != null && !estado.isBlank() && !"ALL".equalsIgnoreCase(estado);

        if (hasRol && hasEstado) {
            entities = repository.findByDestinatarioRolAndEstadoOrderByCreadoEnDesc(destinatarioRol, estado);
        } else if (hasRol) {
            entities = repository.findByDestinatarioRolOrderByCreadoEnDesc(destinatarioRol);
        } else if (hasEstado) {
            entities = repository.findByEstadoOrderByCreadoEnDesc(estado);
        } else {
            entities = repository.findAllByOrderByCreadoEnDesc();
        }

        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public long contarPendientes(String destinatarioRol) {
        if (destinatarioRol != null && !destinatarioRol.isBlank() && !"ALL".equalsIgnoreCase(destinatarioRol)) {
            return repository.countByDestinatarioRolAndEstado(destinatarioRol, SolicitudNotificacion.ESTADO_PENDIENTE);
        }
        return repository.countByEstado(SolicitudNotificacion.ESTADO_PENDIENTE);
    }

    @Override
    public Optional<SolicitudNotificacion> findPendienteByProcesoIdAndTitulo(UUID procesoId, String titulo) {
        return repository.findFirstByProcesoIdAndTituloAndEstado(procesoId, titulo, SolicitudNotificacion.ESTADO_PENDIENTE)
                .map(this::toDomain);
    }

    @Override
    public SolicitudNotificacion save(SolicitudNotificacion notificacion) {
        SolicitudNotificacionJpaEntity entity = toEntity(notificacion);
        SolicitudNotificacionJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private SolicitudNotificacion toDomain(SolicitudNotificacionJpaEntity entity) {
        if (entity == null) return null;
        return new SolicitudNotificacion(
                entity.getId(),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getMensaje(),
                entity.getDatosPayload(),
                entity.getProcesoId(),
                entity.getCarreraNombre(),
                entity.getSolicitadoPor(),
                entity.getSolicitadoPorNombre(),
                entity.getDestinatarioRol(),
                entity.getEstado(),
                entity.isLeida(),
                entity.getResueltoPor(),
                entity.getResueltoEn(),
                entity.getCreadoEn()
        );
    }

    private SolicitudNotificacionJpaEntity toEntity(SolicitudNotificacion domain) {
        if (domain == null) return null;
        SolicitudNotificacionJpaEntity entity = new SolicitudNotificacionJpaEntity();
        entity.setId(domain.getId());
        entity.setTipo(domain.getTipo());
        entity.setTitulo(domain.getTitulo());
        entity.setMensaje(domain.getMensaje());
        entity.setDatosPayload(domain.getDatosPayload());
        entity.setProcesoId(domain.getProcesoId());
        entity.setCarreraNombre(domain.getCarreraNombre());
        entity.setSolicitadoPor(domain.getSolicitadoPor());
        entity.setSolicitadoPorNombre(domain.getSolicitadoPorNombre());
        entity.setDestinatarioRol(domain.getDestinatarioRol());
        entity.setEstado(domain.getEstado());
        entity.setLeida(domain.isLeida());
        entity.setResueltoPor(domain.getResueltoPor());
        entity.setResueltoEn(domain.getResueltoEn());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}
