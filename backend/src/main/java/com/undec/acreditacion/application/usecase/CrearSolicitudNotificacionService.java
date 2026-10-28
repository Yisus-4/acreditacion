package com.undec.acreditacion.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CrearSolicitudNotificacionService implements CrearSolicitudNotificacionUseCase {

    private final SolicitudNotificacionRepositoryPort notificacionRepository;
    private final ObjectMapper objectMapper;

    public CrearSolicitudNotificacionService(
            SolicitudNotificacionRepositoryPort notificacionRepository,
            ObjectMapper objectMapper
    ) {
        this.notificacionRepository = notificacionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SolicitudNotificacionDto registrarSolicitudAltaMiembro(
            UUID procesoId,
            UUID solicitanteId,
            String solicitanteNombre,
            String carreraNombre,
            DatosPayloadAltaMiembro payload
    ) {
        String titulo = "Solicitud de alta en catálogo institucional: " + payload.apellido() + ", " + payload.nombreCompleto();
        
        Optional<SolicitudNotificacion> pendiente = notificacionRepository.findPendienteByProcesoIdAndTitulo(procesoId, titulo);
        if (pendiente.isPresent()) {
            return toDto(pendiente.get());
        }

        String mensaje = "Solicitud de alta en catálogo institucional: " + payload.nombreCompleto() + " " + payload.apellido() +
                " (" + payload.documento() + ") para la carrera " + (carreraNombre != null ? carreraNombre : "de Grado");

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // fallback minimal json
            payloadJson = "{\"documento\":\"" + payload.documento() + "\"}";
        }

        SolicitudNotificacion notif = new SolicitudNotificacion(
                UUID.randomUUID(),
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                titulo,
                mensaje,
                payloadJson,
                procesoId,
                carreraNombre,
                solicitanteId,
                solicitanteNombre,
                SolicitudNotificacion.ROL_DESTINATARIO_AEA,
                SolicitudNotificacion.ESTADO_PENDIENTE,
                false,
                null,
                null,
                Instant.now()
        );

        SolicitudNotificacion guardada = notificacionRepository.save(notif);
        return toDto(guardada);
    }

    private SolicitudNotificacionDto toDto(SolicitudNotificacion s) {
        if (s == null) return null;
        return new SolicitudNotificacionDto(
                s.getId(),
                s.getTipo(),
                s.getTitulo(),
                s.getMensaje(),
                s.getDatosPayload(),
                s.getProcesoId(),
                s.getCarreraNombre(),
                s.getSolicitadoPor(),
                s.getSolicitadoPorNombre(),
                s.getDestinatarioRol(),
                s.getEstado(),
                s.isLeida(),
                s.getResueltoPor(),
                s.getResueltoEn(),
                s.getCreadoEn()
        );
    }
}
