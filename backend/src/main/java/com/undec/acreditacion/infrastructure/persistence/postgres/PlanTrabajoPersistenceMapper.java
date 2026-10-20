package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlanTrabajoPersistenceMapper {

    public PlanTrabajo toDomain(PlanTrabajoJpaEntity entity, List<ActividadPlanTrabajoJpaEntity> actividadesJpa) {
        if (entity == null) {
            return null;
        }
        List<ActividadPlanTrabajo> actividades = (actividadesJpa != null)
                ? actividadesJpa.stream().map(this::toDomainActividad).collect(Collectors.toList())
                : Collections.emptyList();

        return PlanTrabajo.rehydrate(
                entity.getId(),
                entity.getProcesoId(),
                entity.getCodigo(),
                TipoPlanTrabajo.valueOf(entity.getTipo()),
                EstadoPlanTrabajo.valueOf(entity.getEstado()),
                entity.getCreadoEn(),
                entity.getActualizadoEn(),
                actividades
        );
    }

    public PlanTrabajoJpaEntity toJpa(PlanTrabajo domain) {
        if (domain == null) {
            return null;
        }
        PlanTrabajoJpaEntity entity = new PlanTrabajoJpaEntity();
        entity.setId(domain.getId());
        entity.setProcesoId(domain.getProcesoId());
        entity.setCodigo(domain.getCodigo());
        entity.setTipo(domain.getTipo().name());
        entity.setEstado(domain.getEstado().name());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public ActividadPlanTrabajo toDomainActividad(ActividadPlanTrabajoJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        HojaPlanTrabajo hoja = entity.getHoja() != null
                ? HojaPlanTrabajo.valueOf(entity.getHoja())
                : HojaPlanTrabajo.INFO;

        PrioridadActividad prioridad = entity.getPrioridad() != null
                ? PrioridadActividad.valueOf(entity.getPrioridad())
                : PrioridadActividad.MEDIA;

        return ActividadPlanTrabajo.rehydrate(
                entity.getId(),
                entity.getPlanTrabajoId(),
                entity.getNumeroOrden(),
                entity.getCodigoActividad(),
                entity.getNombre(),
                EstadoActividad.valueOf(entity.getEstado()),
                entity.getFechaReunionCoordinacion(),
                entity.getSintesisAcuerdos(),
                entity.getCompletadaEn(),
                entity.getCompletadaPor(),
                hoja,
                entity.getFechaInicioEstimada(),
                entity.getFechaFinEstimada(),
                entity.getResponsable(),
                prioridad,
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public ActividadPlanTrabajoJpaEntity toJpaActividad(ActividadPlanTrabajo domain) {
        if (domain == null) {
            return null;
        }
        ActividadPlanTrabajoJpaEntity entity = new ActividadPlanTrabajoJpaEntity();
        entity.setId(domain.getId());
        entity.setPlanTrabajoId(domain.getPlanTrabajoId());
        entity.setNumeroOrden(domain.getNumeroOrden());
        entity.setCodigoActividad(domain.getCodigoActividad());
        entity.setNombre(domain.getNombre());
        entity.setEstado(domain.getEstado().name());
        entity.setFechaReunionCoordinacion(domain.getFechaReunionCoordinacion());
        entity.setSintesisAcuerdos(domain.getSintesisAcuerdos());
        entity.setCompletadaEn(domain.getCompletadaEn());
        entity.setCompletadaPor(domain.getCompletadaPor());
        entity.setHoja(domain.getHoja() != null ? domain.getHoja().name() : HojaPlanTrabajo.INFO.name());
        entity.setFechaInicioEstimada(domain.getFechaInicioEstimada());
        entity.setFechaFinEstimada(domain.getFechaFinEstimada());
        entity.setResponsable(domain.getResponsable());
        entity.setPrioridad(domain.getPrioridad() != null ? domain.getPrioridad().name() : PrioridadActividad.MEDIA.name());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }
}
