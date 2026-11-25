package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class PostgresRegistroConformacionCaRepositoryAdapter implements RegistroConformacionCaRepositoryPort {

    private final SpringDataRegistroConformacionCaRepository repository;

    public PostgresRegistroConformacionCaRepositoryAdapter(SpringDataRegistroConformacionCaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RegistroConformacionCa save(RegistroConformacionCa registro) {
        RegistroConformacionCaJpaEntity entity = RegistroCaPersistenceMapper.toEntity(registro);
        RegistroConformacionCaJpaEntity saved = repository.save(entity);
        return RegistroCaPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<RegistroConformacionCa> findByProcesoId(UUID procesoId) {
        return repository.findByProcesoId(procesoId)
                .map(RegistroCaPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RegistroConformacionCa> findById(UUID id) {
        return repository.findById(id)
                .map(RegistroCaPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<RegistroConformacionCa> findAll() {
        return repository.findAll().stream()
                .map(RegistroCaPersistenceMapper::toDomain)
                .toList();
    }
}
