package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PostgresCarreraRepositoryAdapter implements CarreraRepositoryPort {

    private final SpringDataCarreraRepository springDataRepository;
    private final SpringDataUnidadAcademicaRepository springDataUnidadAcademicaRepository;
    private final SpringDataSedeLocalizacionRepository springDataSedeLocalizacionRepository;
    private final EstructuraAcademicaPersistenceMapper mapper;

    public PostgresCarreraRepositoryAdapter(SpringDataCarreraRepository springDataRepository,
                                           SpringDataUnidadAcademicaRepository springDataUnidadAcademicaRepository,
                                           SpringDataSedeLocalizacionRepository springDataSedeLocalizacionRepository,
                                           EstructuraAcademicaPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.springDataUnidadAcademicaRepository = springDataUnidadAcademicaRepository;
        this.springDataSedeLocalizacionRepository = springDataSedeLocalizacionRepository;
        this.mapper = mapper;
    }

    @Override
    public Carrera save(Carrera carrera) {
        UnidadAcademicaJpaEntity unidadAcademica = springDataUnidadAcademicaRepository.getReferenceById(carrera.getUnidadAcademicaId());
        Set<SedeLocalizacionJpaEntity> sedes = new HashSet<>();
        if (carrera.getSedesIds() != null && !carrera.getSedesIds().isEmpty()) {
            sedes.addAll(springDataSedeLocalizacionRepository.findAllById(carrera.getSedesIds()));
        }

        CarreraJpaEntity entity = mapper.toJpa(carrera, unidadAcademica, sedes);
        CarreraJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Carrera> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Carrera> findAll() {
        return springDataRepository.findAllWithSedes().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Carrera> findByUnidadAcademicaId(UUID unidadAcademicaId) {
        return springDataRepository.findByUnidadAcademicaId(unidadAcademicaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByUnidadAcademicaIdAndNombre(UUID unidadAcademicaId, String nombre) {
        return springDataRepository.existsByUnidadAcademicaIdAndNombre(unidadAcademicaId, nombre);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public long countByUnidadAcademicaId(UUID unidadAcademicaId) {
        return springDataRepository.countByUnidadAcademicaId(unidadAcademicaId);
    }

    @Override
    public boolean existsBySedeId(UUID sedeId) {
        return springDataRepository.existsBySedeId(sedeId);
    }
}
