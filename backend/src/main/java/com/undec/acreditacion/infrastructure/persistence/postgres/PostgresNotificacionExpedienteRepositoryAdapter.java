package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.NotificacionExpediente;
import com.undec.acreditacion.domain.ports.NotificacionExpedienteRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PostgresNotificacionExpedienteRepositoryAdapter implements NotificacionExpedienteRepositoryPort {

    private final SpringDataNotificacionExpedienteRepository repository;
    private final NormativaPersistenceMapper mapper;

    public PostgresNotificacionExpedienteRepositoryAdapter(
        SpringDataNotificacionExpedienteRepository repository,
        NormativaPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public NotificacionExpediente save(NotificacionExpediente notificacion) {
        NotificacionExpedienteJpaEntity entity = mapper.toJpa(notificacion);
        NotificacionExpedienteJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<NotificacionExpediente> findByProcesoId(UUID procesoId) {
        return repository.findByProcesoIdOrderByEnviadoEnDesc(procesoId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
