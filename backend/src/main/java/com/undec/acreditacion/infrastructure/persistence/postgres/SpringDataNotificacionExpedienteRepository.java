package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataNotificacionExpedienteRepository extends JpaRepository<NotificacionExpedienteJpaEntity, UUID> {
    List<NotificacionExpedienteJpaEntity> findByProcesoIdOrderByEnviadoEnDesc(UUID procesoId);
}
