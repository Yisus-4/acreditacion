package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class PostgresInstitucionRepositoryAdapter implements InstitucionRepositoryPort {

    private final SpringDataInstitucionRepository springDataRepository;
    private final InstitucionPersistenceMapper mapper;

    public PostgresInstitucionRepositoryAdapter(SpringDataInstitucionRepository springDataRepository,
                                              InstitucionPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Institucion save(Institucion institucion) {
        InstitucionJpaEntity entity = mapper.toEntity(institucion);
        InstitucionJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }
        return springDataRepository.existsByNombreIgnoreCase(nombre.trim());
    }

    @Override
    public boolean existsBySigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return false;
        }
        return springDataRepository.existsBySigla(sigla.trim());
    }

    @Override
    public Optional<Institucion> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Institucion> findByNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByNombreIgnoreCase(nombre.trim()).map(mapper::toDomain);
    }

    @Override
    public Optional<Institucion> findBySigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findBySiglaIgnoreCase(sigla.trim()).map(mapper::toDomain);
    }

    @Override
    public List<Institucion> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
