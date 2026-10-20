package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.springframework.stereotype.Component;

@Component
public class NormativaPersistenceMapper {

    public SolicitudNormativaJpaEntity toJpa(SolicitudNormativaCarrera domain) {
        if (domain == null) return null;
        SolicitudNormativaJpaEntity jpa = new SolicitudNormativaJpaEntity();
        jpa.setId(domain.getId());
        jpa.setProcesoId(domain.getProcesoId());
        jpa.setActividadId(domain.getActividadId());
        jpa.setEstado(domain.getEstado().name());
        jpa.setFechaSolicitud(domain.getFechaSolicitud());
        jpa.setSolicitadoPor(domain.getSolicitadoPor());
        jpa.setFechaLimiteEntrega(domain.getFechaLimiteEntrega());
        jpa.setDiasHabilesPlazo(domain.getDiasHabilesPlazo());
        jpa.setFechaEnvio(domain.getFechaEnvio());
        jpa.setEnviadoPor(domain.getEnviadoPor());
        jpa.setCumplimientoPlazo(domain.getCumplimientoPlazo() != null ? domain.getCumplimientoPlazo().name() : null);
        jpa.setObservacionesSolicitud(domain.getObservacionesSolicitud());
        jpa.setObservacionesRevision(domain.getObservacionesRevision());
        jpa.setFechaRevision(domain.getFechaRevision());
        jpa.setRevisadoPor(domain.getRevisadoPor());
        jpa.setCreadoEn(domain.getCreadoEn());
        jpa.setActualizadoEn(domain.getActualizadoEn());
        return jpa;
    }

    public SolicitudNormativaCarrera toDomain(SolicitudNormativaJpaEntity jpa) {
        if (jpa == null) return null;
        EstadoSolicitudNormativa estado = EstadoSolicitudNormativa.valueOf(jpa.getEstado());
        CumplimientoPlazo cumplimiento = jpa.getCumplimientoPlazo() != null
            ? CumplimientoPlazo.valueOf(jpa.getCumplimientoPlazo())
            : null;

        return new SolicitudNormativaCarrera(
            jpa.getId(),
            jpa.getProcesoId(),
            jpa.getActividadId(),
            estado,
            jpa.getFechaSolicitud(),
            jpa.getSolicitadoPor(),
            jpa.getFechaLimiteEntrega(),
            jpa.getDiasHabilesPlazo(),
            jpa.getFechaEnvio(),
            jpa.getEnviadoPor(),
            cumplimiento,
            jpa.getObservacionesSolicitud(),
            jpa.getObservacionesRevision(),
            jpa.getFechaRevision(),
            jpa.getRevisadoPor(),
            jpa.getCreadoEn(),
            jpa.getActualizadoEn()
        );
    }

    public DocumentoNormativaJpaEntity toJpa(DocumentoNormativa domain) {
        if (domain == null) return null;
        DocumentoNormativaJpaEntity jpa = new DocumentoNormativaJpaEntity();
        jpa.setId(domain.getId());
        jpa.setSolicitudNormativaId(domain.getSolicitudNormativaId());
        jpa.setProcesoId(domain.getProcesoId());
        jpa.setTipoDocumento(domain.getTipoDocumento().name());
        jpa.setCarpetaDestino(domain.getCarpetaDestino().name());
        jpa.setNombreOriginal(domain.getNombreOriginal());
        jpa.setStoragePath(domain.getStoragePath());
        jpa.setMimeType(domain.getMimeType());
        jpa.setTamanioBytes(domain.getTamanioBytes());
        jpa.setEsBorrador(domain.isEsBorrador());
        jpa.setObservado(domain.isObservado());
        jpa.setMotivoObservacion(domain.getMotivoObservacion());
        jpa.setSubidoPor(domain.getSubidoPor());
        jpa.setCreadoEn(domain.getCreadoEn());
        jpa.setActualizadoEn(domain.getActualizadoEn());
        return jpa;
    }

    public DocumentoNormativa toDomain(DocumentoNormativaJpaEntity jpa) {
        if (jpa == null) return null;
        TipoDocumentoNormativa tipo = TipoDocumentoNormativa.valueOf(jpa.getTipoDocumento());
        CarpetaMinioDestino carpeta = CarpetaMinioDestino.valueOf(jpa.getCarpetaDestino());

        return new DocumentoNormativa(
            jpa.getId(),
            jpa.getSolicitudNormativaId(),
            jpa.getProcesoId(),
            tipo,
            carpeta,
            jpa.getNombreOriginal(),
            jpa.getStoragePath(),
            jpa.getMimeType(),
            jpa.getTamanioBytes(),
            jpa.isEsBorrador(),
            jpa.isObservado(),
            jpa.getMotivoObservacion(),
            jpa.getSubidoPor(),
            jpa.getCreadoEn(),
            jpa.getActualizadoEn()
        );
    }

    public NotificacionExpedienteJpaEntity toJpa(NotificacionExpediente domain) {
        if (domain == null) return null;
        NotificacionExpedienteJpaEntity jpa = new NotificacionExpedienteJpaEntity();
        jpa.setId(domain.getId());
        jpa.setProcesoId(domain.getProcesoId());
        jpa.setTipoEvento(domain.getTipoEvento().name());
        jpa.setDestinatarioEmail(domain.getDestinatarioEmail());
        jpa.setDestinatarioNombre(domain.getDestinatarioNombre());
        jpa.setAsunto(domain.getAsunto());
        jpa.setEnviadoExitosamente(domain.isEnviadoExitosamente());
        jpa.setErrorDetalle(domain.getErrorDetalle());
        jpa.setEnviadoEn(domain.getEnviadoEn());
        return jpa;
    }

    public NotificacionExpediente toDomain(NotificacionExpedienteJpaEntity jpa) {
        if (jpa == null) return null;
        TipoEventoNotificacion evento = TipoEventoNotificacion.valueOf(jpa.getTipoEvento());

        return new NotificacionExpediente(
            jpa.getId(),
            jpa.getProcesoId(),
            evento,
            jpa.getDestinatarioEmail(),
            jpa.getDestinatarioNombre(),
            jpa.getAsunto(),
            jpa.isEnviadoExitosamente(),
            jpa.getErrorDetalle(),
            jpa.getEnviadoEn()
        );
    }
}
