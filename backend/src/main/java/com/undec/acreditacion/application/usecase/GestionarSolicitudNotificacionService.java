package com.undec.acreditacion.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.GestionarSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestionarSolicitudNotificacionService implements GestionarSolicitudNotificacionUseCase {

    private final SolicitudNotificacionRepositoryPort notificacionRepository;
    private final CatalogoMiembroComisionRepositoryPort catalogoRepository;
    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ObjectMapper objectMapper;

    public GestionarSolicitudNotificacionService(
            SolicitudNotificacionRepositoryPort notificacionRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ObjectMapper objectMapper
    ) {
        this.notificacionRepository = notificacionRepository;
        this.catalogoRepository = catalogoRepository;
        this.registroCaRepository = registroCaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SolicitudNotificacionDto aceptar(UUID id, UUID resolutorId) {
        SolicitudNotificacion notif = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de notificación con ID: " + id));

        if (SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO.equalsIgnoreCase(notif.getTipo())) {
            procesarAltaCatalogo(notif);
        }

        notif.aceptar(resolutorId);
        SolicitudNotificacion guardada = notificacionRepository.save(notif);
        return toDto(guardada);
    }

    @Override
    @Transactional
    public SolicitudNotificacionDto rechazar(UUID id, UUID resolutorId) {
        SolicitudNotificacion notif = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de notificación con ID: " + id));

        notif.rechazar(resolutorId);
        SolicitudNotificacion guardada = notificacionRepository.save(notif);
        return toDto(guardada);
    }

    @Override
    @Transactional
    public SolicitudNotificacionDto marcarLeida(UUID id) {
        SolicitudNotificacion notif = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de notificación con ID: " + id));

        notif.marcarLeida();
        SolicitudNotificacion guardada = notificacionRepository.save(notif);
        return toDto(guardada);
    }

    private void procesarAltaCatalogo(SolicitudNotificacion notif) {
        if (notif.getDatosPayload() == null || notif.getDatosPayload().isBlank()) {
            return;
        }

        try {
            DatosPayloadAltaMiembro payload = objectMapper.readValue(notif.getDatosPayload(), DatosPayloadAltaMiembro.class);
            if (payload == null) return;

            String doc = payload.documento() != null ? payload.documento().trim() : "";
            String cuit = (payload.cuitPasaporte() != null && !payload.cuitPasaporte().isBlank())
                    ? payload.cuitPasaporte().trim()
                    : doc;

            Optional<CatalogoMiembroComision> existente = Optional.empty();
            if (!doc.isBlank()) {
                existente = catalogoRepository.findByDocumento(doc);
            }
            if (existente.isEmpty() && !cuit.isBlank()) {
                existente = catalogoRepository.findByCuitPasaporte(cuit);
            }

            if (existente.isEmpty()) {
                CatalogoMiembroComision nuevo = new CatalogoMiembroComision(
                        UUID.randomUUID(),
                        payload.apellido() != null ? payload.apellido().trim() : "",
                        payload.nombreCompleto() != null ? payload.nombreCompleto().trim() : "",
                        doc,
                        cuit,
                        payload.correoElectronico() != null ? payload.correoElectronico().trim() : "",
                        payload.correoGmail() != null ? payload.correoGmail().trim() : "",
                        payload.cargoFuncion() != null ? payload.cargoFuncion().trim() : "Docente",
                        payload.esDocente(),
                        Instant.now()
                );
                catalogoRepository.save(nuevo);
            }

            // Si hay un proceso vinculado, marcar al miembro en la comisión como confirmado
            if (notif.getProcesoId() != null) {
                registroCaRepository.findByProcesoId(notif.getProcesoId()).ifPresent(ca -> {
                    boolean modificado = false;
                    for (MiembroComisionAutoevaluacion m : ca.getMiembros()) {
                        String mDoc = m.getDniCuil() != null ? m.getDniCuil().trim() : "";
                        String mCuit = m.getCuitPasaporte() != null ? m.getCuitPasaporte().trim() : "";
                        if ((!doc.isBlank() && doc.equalsIgnoreCase(mDoc)) || (!cuit.isBlank() && cuit.equalsIgnoreCase(mCuit))) {
                            m.setPendienteAltaSistema(false);
                            modificado = true;
                        }
                    }
                    if (modificado) {
                        registroCaRepository.save(ca);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el alta en catálogo institucional: " + e.getMessage(), e);
        }
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
