package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresUnidadAcademicaRepositoryAdapter implements UnidadAcademicaRepositoryPort {

    private final SpringDataUnidadAcademicaRepository springDataRepository;
    private final SpringDataInstitucionRepository springDataInstitucionRepository;
    private final EstructuraAcademicaPersistenceMapper mapper;

    public PostgresUnidadAcademicaRepositoryAdapter(SpringDataUnidadAcademicaRepository springDataRepository,
                                                   SpringDataInstitucionRepository springDataInstitucionRepository,
                                                   EstructuraAcademicaPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.springDataInstitucionRepository = springDataInstitucionRepository;
        this.mapper = mapper;
    }

    @Override
    public UnidadAcademica save(UnidadAcademica unidadAcademica) {
        InstitucionJpaEntity institucion = springDataInstitucionRepository.getReferenceById(unidadAcademica.getInstitucionId());
        UnidadAcademicaJpaEntity entity = mapper.toJpa(unidadAcademica, institucion);
        UnidadAcademicaJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UnidadAcademica> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<UnidadAcademica> findAll() {
        return springDataRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<UnidadAcademica> findByInstitucionId(UUID institucionId) {
        return springDataRepository.findByInstitucionId(institucionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByInstitucionIdAndNombre(UUID institucionId, String nombre) {
        return springDataRepository.existsByInstitucionIdAndNombre(institucionId, nombre);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
