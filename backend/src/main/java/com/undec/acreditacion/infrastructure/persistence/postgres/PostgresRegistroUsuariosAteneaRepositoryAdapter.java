package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.ports.RegistroUsuariosAteneaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresRegistroUsuariosAteneaRepositoryAdapter implements RegistroUsuariosAteneaRepositoryPort {

    private final SpringDataRegistroUsuariosAteneaRepository repository;

    public PostgresRegistroUsuariosAteneaRepositoryAdapter(SpringDataRegistroUsuariosAteneaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RegistroUsuariosAtenea save(RegistroUsuariosAtenea registro) {
        RegistroUsuariosAteneaJpaEntity entity = RegistroUsuariosAteneaPersistenceMapper.toEntity(registro);
        RegistroUsuariosAteneaJpaEntity saved = repository.save(entity);
        return RegistroUsuariosAteneaPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<RegistroUsuariosAtenea> findByProcesoId(UUID procesoId) {
        return repository.findByProcesoId(procesoId)
                .map(RegistroUsuariosAteneaPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RegistroUsuariosAtenea> findById(UUID id) {
        return repository.findById(id)
                .map(RegistroUsuariosAteneaPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
