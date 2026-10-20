package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        if (filtro == null || esFiltroVacio(filtro)) {
            return springDataRepository.findAllByOrderByCreadoEnDesc()
                    .stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());
        }

        Specification<ProcesoAcreditacionJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtro.carreraId() != null) {
                predicates.add(cb.equal(root.get("carreraId"), filtro.carreraId()));
            }
            if (filtro.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.estado().name()));
            }
            if (filtro.tipoProceso() != null) {
                predicates.add(cb.equal(root.get("tipoProceso"), filtro.tipoProceso().name()));
            }
            if (filtro.anio() != null) {
                predicates.add(cb.like(root.get("codigo"), "CONV-" + filtro.anio() + "-%"));
            }
            query.orderBy(cb.desc(root.get("creadoEn")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return springDataRepository.findAll(spec)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    private boolean esFiltroVacio(ConvocatoriaFiltro f) {
        return f.carreraId() == null && f.estado() == null && f.tipoProceso() == null && f.anio() == null;
    }

    @Override
    public long countByAnio(int anio) {
        return springDataRepository.countByAnio(anio);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
