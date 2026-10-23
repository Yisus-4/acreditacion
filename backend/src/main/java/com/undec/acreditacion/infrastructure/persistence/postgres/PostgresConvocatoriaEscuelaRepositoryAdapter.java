package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Convocatoria;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class PostgresConvocatoriaEscuelaRepositoryAdapter implements ConvocatoriaEscuelaRepositoryPort {

    private final SpringDataConvocatoriaRepository springDataRepository;
    private final ConvocatoriaEscuelaPersistenceMapper mapper;

    public PostgresConvocatoriaEscuelaRepositoryAdapter(
            SpringDataConvocatoriaRepository springDataRepository,
            ConvocatoriaEscuelaPersistenceMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    @Transactional
    public Convocatoria save(Convocatoria convocatoria) {
        ConvocatoriaJpaEntity entity = mapper.toEntity(convocatoria);
        ConvocatoriaJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Convocatoria> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Convocatoria> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByAnio(int anio) {
        return springDataRepository.countByAnio(anio);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
