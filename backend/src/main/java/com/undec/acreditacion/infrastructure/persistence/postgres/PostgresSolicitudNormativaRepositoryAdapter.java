package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.SolicitudNormativaCarrera;
import com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresSolicitudNormativaRepositoryAdapter implements SolicitudNormativaRepositoryPort {

    private final SpringDataSolicitudNormativaRepository repository;
    private final NormativaPersistenceMapper mapper;

    public PostgresSolicitudNormativaRepositoryAdapter(
        SpringDataSolicitudNormativaRepository repository,
        NormativaPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public SolicitudNormativaCarrera save(SolicitudNormativaCarrera solicitud) {
        SolicitudNormativaJpaEntity entity = mapper.toJpa(solicitud);
        SolicitudNormativaJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SolicitudNormativaCarrera> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SolicitudNormativaCarrera> findByProcesoId(UUID procesoId) {
        return repository.findByProcesoId(procesoId).map(mapper::toDomain);
    }

    @Override
    public Optional<SolicitudNormativaCarrera> findByActividadId(UUID actividadId) {
        return repository.findByActividadId(actividadId).map(mapper::toDomain);
    }
}
