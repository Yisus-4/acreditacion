package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresSedeLocalizacionRepositoryAdapter implements SedeLocalizacionRepositoryPort {

    private final SpringDataSedeLocalizacionRepository springDataRepository;
    private final SpringDataInstitucionRepository springDataInstitucionRepository;
    private final SpringDataProvinciaRepository springDataProvinciaRepository;
    private final SpringDataDepartamentoRepository springDataDepartamentoRepository;
    private final SpringDataLocalidadRepository springDataLocalidadRepository;
    private final EstructuraAcademicaPersistenceMapper mapper;

    public PostgresSedeLocalizacionRepositoryAdapter(SpringDataSedeLocalizacionRepository springDataRepository,
                                                     SpringDataInstitucionRepository springDataInstitucionRepository,
                                                     SpringDataProvinciaRepository springDataProvinciaRepository,
                                                     SpringDataDepartamentoRepository springDataDepartamentoRepository,
                                                     SpringDataLocalidadRepository springDataLocalidadRepository,
                                                     EstructuraAcademicaPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.springDataInstitucionRepository = springDataInstitucionRepository;
        this.springDataProvinciaRepository = springDataProvinciaRepository;
        this.springDataDepartamentoRepository = springDataDepartamentoRepository;
        this.springDataLocalidadRepository = springDataLocalidadRepository;
        this.mapper = mapper;
    }

    @Override
    public SedeLocalizacion save(SedeLocalizacion sede) {
        InstitucionJpaEntity institucion = springDataInstitucionRepository.getReferenceById(sede.getInstitucionId());
        ProvinciaJpaEntity provincia = springDataProvinciaRepository.getReferenceById(sede.getProvinciaId());
        DepartamentoJpaEntity departamento = springDataDepartamentoRepository.getReferenceById(sede.getDepartamentoId());
        LocalidadJpaEntity localidad = springDataLocalidadRepository.getReferenceById(sede.getLocalidadId());

        SedeLocalizacionJpaEntity entity = mapper.toJpa(sede, institucion, provincia, departamento, localidad);
        SedeLocalizacionJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SedeLocalizacion> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SedeLocalizacion> findAll() {
        return springDataRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SedeLocalizacion> findByInstitucionId(UUID institucionId) {
        return springDataRepository.findByInstitucionId(institucionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SedeLocalizacion> findByIds(List<UUID> ids) {
        return springDataRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
