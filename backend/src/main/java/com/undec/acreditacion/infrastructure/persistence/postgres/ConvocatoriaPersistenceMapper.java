package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.RegistroConvocatoria;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;
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
        return RegistroConvocatoria.rehydrate(
                entity.getId(),
                entity.getProcesoId(),
                entity.getNroResolucionConeau(),
                entity.getFechaInicio(),
                entity.getFechaCierre(),
                entity.getObservaciones(),
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
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }
}
