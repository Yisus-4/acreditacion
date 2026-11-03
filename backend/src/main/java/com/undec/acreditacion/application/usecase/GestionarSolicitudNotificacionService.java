package com.undec.acreditacion.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.GestionarSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestionarSolicitudNotificacionService implements GestionarSolicitudNotificacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(GestionarSolicitudNotificacionService.class);
    private static final String ROL_MIEMBRO_COMISION = "SELF_EVALUATION_COMMITTEE_MEMBER";

    private final SolicitudNotificacionRepositoryPort notificacionRepository;
    private final CatalogoMiembroComisionRepositoryPort catalogoRepository;
    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;

    public GestionarSolicitudNotificacionService(
            SolicitudNotificacionRepositoryPort notificacionRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ObjectMapper objectMapper
    ) {
        this(notificacionRepository, catalogoRepository, registroCaRepository, objectMapper, null, null, null, null, null);
    }

    @Autowired
    public GestionarSolicitudNotificacionService(
            SolicitudNotificacionRepositoryPort notificacionRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository
    ) {
        this.notificacionRepository = notificacionRepository;
        this.catalogoRepository = catalogoRepository;
        this.registroCaRepository = registroCaRepository;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
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

        if (SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO.equalsIgnoreCase(notif.getTipo())) {
            desmarcarPendienteAltaEnComision(notif);
        }

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
                        Instant.now()
                );
                catalogoRepository.save(nuevo);
            }

            // Registrar cuenta de usuario en /admin/users si no existe
            registrarUsuarioEnSistema(notif.getProcesoId(), payload, doc);

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

    private void desmarcarPendienteAltaEnComision(SolicitudNotificacion notif) {
        if (notif.getProcesoId() == null || notif.getDatosPayload() == null || notif.getDatosPayload().isBlank()) {
            return;
        }
        try {
            DatosPayloadAltaMiembro payload = objectMapper.readValue(notif.getDatosPayload(), DatosPayloadAltaMiembro.class);
            if (payload == null) return;
            String doc = payload.documento() != null ? payload.documento().trim() : "";
            String cuit = payload.cuitPasaporte() != null ? payload.cuitPasaporte().trim() : "";
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
        } catch (Exception e) {
            log.warn("No se pudo actualizar estado pendienteAltaSistema en comisión al rechazar notificación: {}", e.getMessage());
        }
    }

    private void registrarUsuarioEnSistema(UUID procesoId, DatosPayloadAltaMiembro payload, String doc) {
        if (userRepository == null || roleRepository == null || passwordHasher == null) {
            return;
        }
        try {
            String email = payload.correoElectronico() != null && !payload.correoElectronico().isBlank()
                    ? payload.correoElectronico().trim().toLowerCase()
                    : (payload.correoGmail() != null ? payload.correoGmail().trim().toLowerCase() : "");
            if (email.isBlank() || userRepository.existsByEmail(email)) {
                return;
            }

            String baseUsername = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
            if (baseUsername.length() < 3) {
                baseUsername = "miembro_" + (doc != null && !doc.isBlank() ? doc.replaceAll("[^0-9]", "") : "ca");
            }
            String username = baseUsername;
            if (userRepository.existsByUsername(username)) {
                String suffix = (doc != null && !doc.isBlank())
                        ? doc.replaceAll("[^a-zA-Z0-9]", "")
                        : String.valueOf((int) (Math.random() * 900 + 100));
                username = baseUsername + "_" + suffix;
            }

            UUID unidadAcademicaId = null;
            if (procesoId != null && procesoRepository != null && carreraRepository != null) {
                unidadAcademicaId = procesoRepository.findById(procesoId)
                        .filter(p -> p.getCarreraId() != null)
                        .flatMap(p -> carreraRepository.findById(p.getCarreraId()))
                        .map(Carrera::getUnidadAcademicaId)
                        .orElse(null);
            }

            String cleanDoc = doc != null ? doc.replaceAll("[^a-zA-Z0-9]", "") : "";
            String rawPassword = cleanDoc.length() >= 6 ? cleanDoc : "undec123";
            String passwordHash = passwordHasher.hash(rawPassword);

            User newUser = User.register(username, email, passwordHash, unidadAcademicaId);
            roleRepository.findByCode(ROL_MIEMBRO_COMISION).ifPresent(newUser::assignRole);
            userRepository.save(newUser);
        } catch (Exception e) {
            log.warn("No se pudo dar de alta automática como usuario del sistema al miembro {}: {}", payload.correoElectronico(), e.getMessage());
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
