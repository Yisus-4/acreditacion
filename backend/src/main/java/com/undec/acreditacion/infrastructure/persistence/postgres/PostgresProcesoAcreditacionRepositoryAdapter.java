package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PostgresProcesoAcreditacionRepositoryAdapter implements ProcesoAcreditacionRepositoryPort {

    private final SpringDataProcesoAcreditacionRepository springDataRepository;
    private final ConvocatoriaPersistenceMapper mapper;

    public PostgresProcesoAcreditacionRepositoryAdapter(
            SpringDataProcesoAcreditacionRepository springDataRepository,
            ConvocatoriaPersistenceMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public ProcesoAcreditacion save(ProcesoAcreditacion proceso) {
        ProcesoAcreditacionJpaEntity entity = mapper.toJpa(proceso);
        ProcesoAcreditacionJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProcesoAcreditacion> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProcesoAcreditacion> findActivoByCarreraId(UUID carreraId) {
        List<String> estadosActivos = List.of(
                EstadoProcesoAcreditacion.INICIADO.name(),
                EstadoProcesoAcreditacion.EN_AUTOEVALUACION.name()
        );
        return springDataRepository.findByCarreraIdAndEstadoIn(carreraId, estadosActivos)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProcesoAcreditacion> findAllByFiltro(ConvocatoriaFiltro filtro) {
        UUID carreraId = filtro != null ? filtro.carreraId() : null;
        String estado = (filtro != null && filtro.estado() != null) ? filtro.estado().name() : null;
        String tipoProceso = (filtro != null && filtro.tipoProceso() != null) ? filtro.tipoProceso().name() : null;
        Integer anio = filtro != null ? filtro.anio() : null;

        return springDataRepository.findAllByFiltro(carreraId, estado, tipoProceso, anio)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByAnio(int anio) {
        return springDataRepository.countByAnio(anio);
    }
}
