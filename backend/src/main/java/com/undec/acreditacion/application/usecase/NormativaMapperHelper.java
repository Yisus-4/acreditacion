package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.BadgePlazoDto;
import com.undec.acreditacion.application.input.DocumentoNormativaDto;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.domain.entities.*;

import java.time.LocalDate;
import java.util.Collections;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;

import java.util.List;

public final class NormativaMapperHelper {

    private NormativaMapperHelper() {
    }

    public static ActividadPlanTrabajo resolverActividadNormativa(PlanTrabajo plan) {
        return plan.getActividades().stream()
                .filter(a -> "1.3".equals(a.getCodigoActividad()) ||
                             (a.getNombre() != null && a.getNombre().toLowerCase().contains("normativa")))
                .findFirst()
                .or(() -> plan.getActividades().stream()
                        .filter(a -> a.getNumeroOrden() == 2 || (a.getCodigoActividad() != null && a.getCodigoActividad().contains("ACT-02")))
                        .findFirst())
                .orElseGet(() -> {
                    ActividadPlanTrabajo autoCreada = ActividadPlanTrabajo.crearActividadNormativa(plan.getId());
                    plan.agregarActividad(autoCreada);
                    return autoCreada;
                });
    }

    public static NormativaDetalleDto toDto(
        SolicitudNormativaCarrera solicitud,
        List<DocumentoNormativa> documentos,
        ProcesoAcreditacion proceso
    ) {
        BadgePlazoInfo badgeInfo = solicitud.calcularBadgePlazo(LocalDate.now());
        BadgePlazoDto badgeDto = new BadgePlazoDto(
            badgeInfo.estado(),
            badgeInfo.diasRestantes(),
            badgeInfo.etiqueta(),
            badgeInfo.cssClass()
        );

        List<DocumentoNormativaDto> docDtos = documentos != null
            ? documentos.stream().map(NormativaMapperHelper::toDocDto).toList()
            : Collections.emptyList();

        boolean esNueva = proceso != null && proceso.getTipoProceso() == TipoProcesoAcreditacion.NUEVA;
        boolean tieneHistorial = proceso != null && proceso.getTipoProceso() == TipoProcesoAcreditacion.EN_FUNCIONAMIENTO;

        return new NormativaDetalleDto(
            solicitud.getId(),
            solicitud.getProcesoId(),
            solicitud.getActividadId(),
            solicitud.getEstado(),
            solicitud.getFechaSolicitud(),
            solicitud.getSolicitadoPor(),
            solicitud.getFechaLimiteEntrega(),
            solicitud.getDiasHabilesPlazo(),
            solicitud.getFechaEnvio(),
            solicitud.getEnviadoPor(),
            solicitud.getCumplimientoPlazo(),
            solicitud.getObservacionesSolicitud(),
            solicitud.getObservacionesRevision(),
            solicitud.getFechaRevision(),
            solicitud.getRevisadoPor(),
            badgeDto,
            docDtos,
            esNueva,
            tieneHistorial
        );
    }

    public static DocumentoNormativaDto toDocDto(DocumentoNormativa doc) {
        return new DocumentoNormativaDto(
            doc.getId(),
            doc.getSolicitudNormativaId(),
            doc.getProcesoId(),
            doc.getTipoDocumento(),
            doc.getCarpetaDestino(),
            doc.getNombreOriginal(),
            doc.getStoragePath(),
            doc.getMimeType(),
            doc.getTamanioBytes(),
            doc.isEsBorrador(),
            doc.isObservado(),
            doc.getMotivoObservacion(),
            doc.getSubidoPor(),
            doc.getCreadoEn(),
            doc.getActualizadoEn()
        );
    }
}
