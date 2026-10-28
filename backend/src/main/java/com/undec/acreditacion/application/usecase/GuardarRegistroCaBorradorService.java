package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.input.GuardarRegistroCaBorradorUseCase;
import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GuardarRegistroCaBorradorService implements GuardarRegistroCaBorradorUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final UserRepository userRepository;
    private final CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase;

    public GuardarRegistroCaBorradorService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this(registroCaRepository, procesoRepository, null, null, null);
    }

    public GuardarRegistroCaBorradorService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            UserRepository userRepository,
            CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.userRepository = userRepository;
        this.crearSolicitudNotificacionUseCase = crearSolicitudNotificacionUseCase;
    }

    @Override
    @Transactional
    public RegistroCaDto guardarBorrador(UUID procesoId, boolean coincideCurricular, List<MiembroCaInputDto> miembros) {
        return guardarBorrador(procesoId, null, coincideCurricular, miembros);
    }

    @Override
    @Transactional
    public RegistroCaDto guardarBorrador(UUID procesoId, UUID usuarioId, boolean coincideCurricular, List<MiembroCaInputDto> miembros) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        List<MiembroComisionAutoevaluacion> listaMiembros = RegistroCaMapper.toDomainList(miembros);
        ca.guardarBorrador(coincideCurricular, listaMiembros);

        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        // Generar solicitudes de notificación administrativa para miembros nuevos ad-hoc
        generarNotificacionesMiembrosNuevos(proceso, usuarioId, miembros);

        return RegistroCaMapper.toDto(guardado);
    }

    private void generarNotificacionesMiembrosNuevos(
            ProcesoAcreditacion proceso,
            UUID usuarioId,
            List<MiembroCaInputDto> miembros
    ) {
        if (crearSolicitudNotificacionUseCase == null || miembros == null || miembros.isEmpty()) {
            return;
        }

        String carreraNombre = null;
        if (carreraRepository != null && proceso.getCarreraId() != null) {
            carreraNombre = carreraRepository.findById(proceso.getCarreraId())
                    .map(Carrera::getNombre)
                    .orElse("Carrera de Grado");
        }

        String solicitanteNombre = null;
        if (userRepository != null && usuarioId != null) {
            solicitanteNombre = userRepository.findById(usuarioId)
                    .map(u -> (u.getUsername() != null && !u.getUsername().isBlank()) ? u.getUsername() : u.getEmail())
                    .orElse("Director de Carrera");
        }

        for (MiembroCaInputDto m : miembros) {
            if (m.pendienteAltaSistema()) {
                String doc = m.resolverDocumento();
                String cuit = (m.cuitPasaporte() != null && !m.cuitPasaporte().isBlank()) ? m.cuitPasaporte() : doc;
                String nombreCompleto = m.nombreCompleto();
                if ((nombreCompleto == null || nombreCompleto.isBlank()) && m.primerNombre() != null) {
                    nombreCompleto = m.primerNombre() + (m.segundoNombre() != null ? " " + m.segundoNombre() : "");
                }

                DatosPayloadAltaMiembro payload = new DatosPayloadAltaMiembro(
                        m.apellido(),
                        nombreCompleto,
                        doc,
                        cuit,
                        m.correo(),
                        m.correoGmail(),
                        m.cargoFuncion(),
                        m.esDocente()
                );

                crearSolicitudNotificacionUseCase.registrarSolicitudAltaMiembro(
                        proceso.getId(),
                        usuarioId,
                        solicitanteNombre,
                        carreraNombre,
                        payload
                );
            }
        }
    }
}
