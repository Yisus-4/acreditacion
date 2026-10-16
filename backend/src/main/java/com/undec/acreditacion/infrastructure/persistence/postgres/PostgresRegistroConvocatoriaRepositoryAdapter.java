package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.RegistroConvocatoria;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresRegistroConvocatoriaRepositoryAdapter implements RegistroConvocatoriaRepositoryPort {

    private final SpringDataRegistroConvocatoriaRepository springDataRepository;
    private final ConvocatoriaPersistenceMapper mapper;

    public PostgresRegistroConvocatoriaRepositoryAdapter(
            SpringDataRegistroConvocatoriaRepository springDataRepository,
            ConvocatoriaPersistenceMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public RegistroConvocatoria save(RegistroConvocatoria registro) {
        RegistroConvocatoriaJpaEntity entity = mapper.toJpa(registro);
        RegistroConvocatoriaJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RegistroConvocatoria> findByProcesoId(UUID procesoId) {
        return springDataRepository.findByProcesoId(procesoId).map(mapper::toDomain);
    }
}
