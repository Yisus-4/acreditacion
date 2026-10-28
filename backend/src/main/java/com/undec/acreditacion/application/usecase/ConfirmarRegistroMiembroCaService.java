package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarRegistroMiembroCaUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
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
public class ConfirmarRegistroMiembroCaService implements ConfirmarRegistroMiembroCaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmarRegistroMiembroCaService.class);
    private static final String ROL_MIEMBRO_COMISION = "SELF_EVALUATION_COMMITTEE_MEMBER";

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CatalogoMiembroComisionRepositoryPort catalogoRepository;
    private final SolicitudNotificacionRepositoryPort solicitudNotificacionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final CarreraRepositoryPort carreraRepository;

    public ConfirmarRegistroMiembroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository
    ) {
        this(registroCaRepository, procesoRepository, catalogoRepository, null, null, null, null, null);
    }

    @Autowired
    public ConfirmarRegistroMiembroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository,
            SolicitudNotificacionRepositoryPort solicitudNotificacionRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            CarreraRepositoryPort carreraRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.catalogoRepository = catalogoRepository;
        this.solicitudNotificacionRepository = solicitudNotificacionRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.carreraRepository = carreraRepository;
    }

    @Override
    @Transactional
    public RegistroCaDto confirmarRegistro(UUID procesoId, UUID miembroId, UUID usuarioId) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proceso de acreditación no encontrado con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de conformación de CA para el proceso: " + procesoId));

        MiembroComisionAutoevaluacion miembro = ca.getMiembros().stream()
                .filter(m -> m.getId().equals(miembroId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el miembro con ID: " + miembroId + " en la comisión"));

        String doc = miembro.getDniCuil();
        String cuit = miembro.getCuitPasaporte() != null && !miembro.getCuitPasaporte().isBlank()
                ? miembro.getCuitPasaporte()
                : doc;
        String correo = miembro.getCorreo();
        String correoGmail = miembro.getCorreoGmail() != null && !miembro.getCorreoGmail().isBlank()
                ? miembro.getCorreoGmail()
                : correo;
        String cargo = miembro.getCargoFuncion() != null && !miembro.getCargoFuncion().isBlank()
                ? miembro.getCargoFuncion()
                : "Docente";

        Optional<CatalogoMiembroComision> existente = catalogoRepository.findByDocumento(doc);
        if (existente.isEmpty() && cuit != null && !cuit.isBlank()) {
            existente = catalogoRepository.findByCuitPasaporte(cuit);
        }

        if (existente.isEmpty()) {
            CatalogoMiembroComision nuevo = new CatalogoMiembroComision(
                    UUID.randomUUID(),
                    miembro.getApellido(),
                    miembro.getNombreCompleto(),
                    doc,
                    cuit,
                    correo,
                    correoGmail,
                    cargo,
                    miembro.isEsDocente(),
                    Instant.now()
            );
            catalogoRepository.save(nuevo);
        }

        // Dar de alta también la cuenta de usuario en el sistema (/admin/users)
        registrarUsuarioEnSistema(proceso, miembro, doc, correo, correoGmail);

        // Sincronizar solicitud de notificación pendiente si existe
        if (solicitudNotificacionRepository != null) {
            String titulo = "Solicitud de alta en catálogo institucional: " + miembro.getApellido() + ", " + miembro.getNombreCompleto();
            solicitudNotificacionRepository.findPendienteByProcesoIdAndTitulo(procesoId, titulo).ifPresent(notif -> {
                notif.aceptar(usuarioId);
                solicitudNotificacionRepository.save(notif);
            });
        }

        miembro.setPendienteAltaSistema(false);
        RegistroConformacionCa saved = registroCaRepository.save(ca);
        return RegistroCaMapper.toDto(saved);
    }

    private void registrarUsuarioEnSistema(ProcesoAcreditacion proceso, MiembroComisionAutoevaluacion miembro, String doc, String correo, String correoGmail) {
        if (userRepository == null || roleRepository == null || passwordHasher == null) {
            return;
        }
        try {
            String email = correo != null && !correo.isBlank()
                    ? correo.trim().toLowerCase()
                    : (correoGmail != null ? correoGmail.trim().toLowerCase() : "");
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
            if (proceso.getCarreraId() != null && carreraRepository != null) {
                unidadAcademicaId = carreraRepository.findById(proceso.getCarreraId())
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
            log.warn("No se pudo dar de alta automática como usuario del sistema al miembro {}: {}", correo, e.getMessage());
        }
    }
}
