package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSolicitudNotificacionRepository extends JpaRepository<SolicitudNotificacionJpaEntity, UUID> {

    List<SolicitudNotificacionJpaEntity> findByDestinatarioRolOrderByCreadoEnDesc(String destinatarioRol);

    List<SolicitudNotificacionJpaEntity> findByDestinatarioRolAndEstadoOrderByCreadoEnDesc(String destinatarioRol, String estado);

    List<SolicitudNotificacionJpaEntity> findAllByOrderByCreadoEnDesc();

    List<SolicitudNotificacionJpaEntity> findByEstadoOrderByCreadoEnDesc(String estado);

    long countByDestinatarioRolAndEstado(String destinatarioRol, String estado);

    long countByEstado(String estado);

    Optional<SolicitudNotificacionJpaEntity> findFirstByProcesoIdAndTituloAndEstado(UUID procesoId, String titulo, String estado);
}
