package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConvocatoriaEscuelaPersistenceMapper {

    public Convocatoria toDomain(ConvocatoriaJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<ConvocatoriaCarrera> itemsDomain = new ArrayList<>();
        if (entity.getCarreras() != null) {
            for (ConvocatoriaCarreraJpaEntity itemEntity : entity.getCarreras()) {
                itemsDomain.add(ConvocatoriaCarrera.rehydrate(
                        itemEntity.getId(),
                        entity.getId(),
                        itemEntity.getCarreraId(),
                        itemEntity.isSeleccionadaPorAea(),
                        itemEntity.isEsObligatoria(),
                        itemEntity.isSeleccionadaPorDe(),
                        itemEntity.getProcesoId(),
                        itemEntity.getCreadoEn(),
                        itemEntity.getActualizadoEn()
                ));
            }
        }

        return Convocatoria.rehydrate(
                entity.getId(),
                entity.getUnidadAcademicaId(),
                entity.getCodigo(),
                entity.getTitulo(),
                TipoProcesoAcreditacion.valueOf(entity.getTipoProceso()),
                entity.getNroResolucionConeau(),
                entity.getFechaInicio(),
                entity.getFechaCierre(),
                EstadoConvocatoria.valueOf(entity.getEstado()),
                entity.getObservaciones(),
                entity.getCreadoPor(),
                entity.getConfirmadoPorDe(),
                entity.getConfirmadoPorDeEn(),
                entity.getCreadoEn(),
                entity.getActualizadoEn(),
                itemsDomain
        );
    }

    public ConvocatoriaJpaEntity toEntity(Convocatoria domain) {
        if (domain == null) {
            return null;
        }

        ConvocatoriaJpaEntity entity = new ConvocatoriaJpaEntity(
                domain.getId(),
                domain.getUnidadAcademicaId(),
                domain.getCodigo(),
                domain.getTitulo(),
                domain.getTipoProceso().name(),
                domain.getNroResolucionConeau(),
                domain.getFechaInicio(),
                domain.getFechaCierre(),
                domain.getEstado().name(),
                domain.getObservaciones(),
                domain.getCreadoPor(),
                domain.getConfirmadoPorDe(),
                domain.getConfirmadoPorDeEn(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );

        if (domain.getCarreras() != null) {
            for (ConvocatoriaCarrera itemDomain : domain.getCarreras()) {
                ConvocatoriaCarreraJpaEntity itemEntity = new ConvocatoriaCarreraJpaEntity(
                        itemDomain.getId(),
                        entity,
                        itemDomain.getCarreraId(),
                        itemDomain.isSeleccionadaPorAea(),
                        itemDomain.isEsObligatoria(),
                        itemDomain.isSeleccionadaPorDe(),
                        itemDomain.getProcesoId(),
                        itemDomain.getCreadoEn(),
                        itemDomain.getActualizadoEn()
                );
                entity.addCarrera(itemEntity);
            }
        }

        return entity;
    }
}
