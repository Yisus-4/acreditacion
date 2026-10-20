package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.springframework.stereotype.Component;

@Component
public class ConvocatoriaPersistenceMapper {

    public ProcesoAcreditacion toDomain(ProcesoAcreditacionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcesoAcreditacion.rehydrate(
                entity.getId(),
                entity.getCarreraId(),
                entity.getCodigo(),
                entity.getTitulo(),
                TipoProcesoAcreditacion.valueOf(entity.getTipoProceso()),
                EstadoProcesoAcreditacion.valueOf(entity.getEstado()),
                entity.getCreadoPor(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public ProcesoAcreditacionJpaEntity toJpa(ProcesoAcreditacion domain) {
        if (domain == null) {
            return null;
        }
        ProcesoAcreditacionJpaEntity entity = new ProcesoAcreditacionJpaEntity();
        entity.setId(domain.getId());
        entity.setCarreraId(domain.getCarreraId());
        entity.setCodigo(domain.getCodigo());
        entity.setTitulo(domain.getTitulo());
        entity.setTipoProceso(domain.getTipoProceso().name());
        entity.setEstado(domain.getEstado().name());
        entity.setCreadoPor(domain.getCreadoPor());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public RegistroConvocatoria toDomain(RegistroConvocatoriaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        FaseEvaluacion fase = entity.getFaseEvaluacion() != null
                ? FaseEvaluacion.valueOf(entity.getFaseEvaluacion())
                : FaseEvaluacion.PRIMERA_FASE;

        return RegistroConvocatoria.rehydrate(
                entity.getId(),
                entity.getProcesoId(),
                entity.getNroResolucionConeau(),
                entity.getFechaInicio(),
                entity.getFechaCierre(),
                entity.getObservaciones(),
                entity.getRmEstandares(),
                fase,
                entity.getFechaLimitePresentacionConeau(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public RegistroConvocatoriaJpaEntity toJpa(RegistroConvocatoria domain) {
        if (domain == null) {
            return null;
        }
        RegistroConvocatoriaJpaEntity entity = new RegistroConvocatoriaJpaEntity();
        entity.setId(domain.getId());
        entity.setProcesoId(domain.getProcesoId());
        entity.setNroResolucionConeau(domain.getNroResolucionConeau());
        entity.setFechaInicio(domain.getFechaInicio());
        entity.setFechaCierre(domain.getFechaCierre());
        entity.setObservaciones(domain.getObservaciones());
        entity.setRmEstandares(domain.getRmEstandares());
        entity.setFaseEvaluacion(domain.getFaseEvaluacion() != null ? domain.getFaseEvaluacion().name() : FaseEvaluacion.PRIMERA_FASE.name());
        entity.setFechaLimitePresentacionConeau(domain.getFechaLimitePresentacionConeau());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }
}
