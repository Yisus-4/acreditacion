package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import org.springframework.stereotype.Component;

@Component
public class DocumentoAcreditacionPersistenceMapper {

    public DocumentoAcreditacion toDomain(DocumentoAcreditacionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return DocumentoAcreditacion.rehydrate(
                entity.getId(),
                entity.getProcesoId(),
                entity.getActividadId(),
                entity.getNombreOriginal(),
                TipoDocumentoAcreditacion.valueOf(entity.getTipoDocumento()),
                entity.getClasificacionOrigen(),
                entity.getDescripcion(),
                entity.getMimeType(),
                entity.getTamanioBytes(),
                entity.getStoragePath(),
                entity.getSubidoPor(),
                entity.getCreadoEn()
        );
    }

    public DocumentoAcreditacionJpaEntity toJpa(DocumentoAcreditacion domain) {
        if (domain == null) {
            return null;
        }
        DocumentoAcreditacionJpaEntity entity = new DocumentoAcreditacionJpaEntity();
        entity.setId(domain.getId());
        entity.setProcesoId(domain.getProcesoId());
        entity.setActividadId(domain.getActividadId());
        entity.setNombreOriginal(domain.getNombreOriginal());
        entity.setTipoDocumento(domain.getTipoDocumento().name());
        entity.setClasificacionOrigen(domain.getClasificacionOrigen());
        entity.setDescripcion(domain.getDescripcion());
        entity.setMimeType(domain.getMimeType());
        entity.setTamanioBytes(domain.getTamanioBytes());
        entity.setStoragePath(domain.getStoragePath());
        entity.setSubidoPor(domain.getSubidoPor());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}
