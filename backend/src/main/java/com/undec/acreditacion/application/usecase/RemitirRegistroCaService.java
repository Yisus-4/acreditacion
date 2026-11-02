package com.undec.acreditacion.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.input.RemitirRegistroCaUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RemitirRegistroCaService implements RemitirRegistroCaUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemitirRegistroCaService.class);

    private static final String CARPETA_CONVOCATORIA = "01-Convocatoria y formalizacion CONEAU/";

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final DocumentStoragePort storagePort;
    private final CarreraRepositoryPort carreraRepository;
    private final UserRepository userRepository;
    private final CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort
    ) {
        this(registroCaRepository, procesoRepository, storagePort, null, null, null, null);
    }

    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort,
            CarreraRepositoryPort carreraRepository,
            UserRepository userRepository,
            CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase
    ) {
        this(registroCaRepository, procesoRepository, storagePort, carreraRepository, userRepository, crearSolicitudNotificacionUseCase, null);
    }

    @Autowired
    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort,
            CarreraRepositoryPort carreraRepository,
            UserRepository userRepository,
            CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase,
            PlanTrabajoRepositoryPort planTrabajoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.storagePort = storagePort;
        this.carreraRepository = carreraRepository;
        this.userRepository = userRepository;
        this.crearSolicitudNotificacionUseCase = crearSolicitudNotificacionUseCase;
        this.planTrabajoRepository = planTrabajoRepository;
    }

    @Override
    @Transactional
    public RegistroCaDto remitir(
            UUID procesoId,
            UUID usuarioDcId,
            boolean coincideCurricular,
            List<MiembroCaInputDto> miembros,
            byte[] resolucionBytes,
            String resolucionNombreOriginal
    ) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        String storagePath = null;
        String nombreFinal = null;
        Long tamanio = null;

        if (resolucionBytes != null && resolucionBytes.length > 0) {
            nombreFinal = (resolucionNombreOriginal != null && !resolucionNombreOriginal.isBlank())
                    ? resolucionNombreOriginal.trim()
                    : "Resolucion_Designacion_CA.pdf";
            String objectPath = CARPETA_CONVOCATORIA + "CA/Resoluciones/" + UUID.randomUUID() + "_" + nombreFinal;
            storagePath = storagePort.uploadFile(resolucionBytes, objectPath, DocumentoAcreditacion.MIME_PDF);
            tamanio = (long) resolucionBytes.length;
        }

        List<MiembroComisionAutoevaluacion> listaMiembros = RegistroCaMapper.toDomainList(miembros);
        ca.remitir(usuarioDcId, coincideCurricular, listaMiembros, storagePath, nombreFinal, tamanio);

        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        sincronizarActividadPlanTrabajo(procesoId, usuarioDcId, guardado);
        generarNotificacionesMiembrosNuevos(proceso, usuarioDcId, miembros);

        return RegistroCaMapper.toDto(guardado);
    }

    @Override
    @Transactional
    public RegistroCaDto adjuntarResolucion(
            UUID procesoId,
            UUID usuarioId,
            byte[] resolucionBytes,
            String resolucionNombreOriginal
    ) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        if (resolucionBytes == null || resolucionBytes.length == 0) {
            throw new DomainValidationException("Debe adjuntar el archivo PDF de la Resolución de Designación de la Comisión");
        }

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        String nombreFinal = (resolucionNombreOriginal != null && !resolucionNombreOriginal.isBlank())
                ? resolucionNombreOriginal.trim()
                : "Resolucion_Designacion_CA.pdf";
        String objectPath = CARPETA_CONVOCATORIA + "CA/Resoluciones/" + UUID.randomUUID() + "_" + nombreFinal;
        String storagePath = storagePort.uploadFile(resolucionBytes, objectPath, DocumentoAcreditacion.MIME_PDF);

        ca.actualizarResolucionAdjunta(storagePath, nombreFinal, (long) resolucionBytes.length);
        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        sincronizarActividadPlanTrabajo(procesoId, usuarioId, guardado);

        return RegistroCaMapper.toDto(guardado);
    }

    private void sincronizarActividadPlanTrabajo(UUID procesoId, UUID usuarioId, RegistroConformacionCa ca) {
        if (planTrabajoRepository == null) {
            return;
        }
        planTrabajoRepository.findByProcesoId(procesoId).ifPresent(plan -> {
            java.util.List<ActividadPlanTrabajo> actsCa = plan.getActividades().stream()
                    .filter(a -> a.getHoja() == HojaPlanTrabajo.INFO
                            && (a.getNombre().toLowerCase().contains("conformación de la ca")
                            || a.getNombre().toLowerCase().contains("conformacion de la ca")
                            || a.getNombre().toLowerCase().contains("conformación de ca")
                            || a.getNombre().toLowerCase().contains("conformacion de ca")
                            || "3.1".equals(a.getCodigoActividad())
                            || "3.2".equals(a.getCodigoActividad())))
                    .collect(java.util.stream.Collectors.toList());

            if (actsCa.isEmpty()) {
                ActividadPlanTrabajo nueva = new ActividadPlanTrabajo(
                        UUID.randomUUID(),
                        plan.getId(),
                        6,
                        "3.1",
                        "Solicitar la conformación de la CA de la carrera",
                        EstadoActividad.PENDIENTE,
                        null, null, null, null,
                        HojaPlanTrabajo.INFO,
                        null, null,
                        "Área de Evaluación y Acreditación (AEA) / Director de Carrera",
                        PrioridadActividad.ALTA,
                        java.time.Instant.now(),
                        java.time.Instant.now()
                );
                plan.agregarActividad(nueva);
                actsCa.add(planTrabajoRepository.saveActividad(nueva));
            }

            boolean tieneResolucion = ca.getResolucionStoragePath() != null && !ca.getResolucionStoragePath().isBlank();
            boolean enviadaOAprobada = ca.getEstado() == EstadoRegistroCa.REMITIDA_A_AEA || ca.getEstado() == EstadoRegistroCa.APROBADA;

            for (ActividadPlanTrabajo act : actsCa) {
                boolean es31 = "3.1".equals(act.getCodigoActividad()) || act.getNombre().toLowerCase().contains("solicitar");
                boolean es32 = "3.2".equals(act.getCodigoActividad()) || act.getNombre().toLowerCase().contains("formalizar");

                if (es31 && enviadaOAprobada) {
                    if (act.getEstado() != EstadoActividad.COMPLETADA) {
                        act.completar(usuarioId);
                        planTrabajoRepository.saveActividad(act);
                    }
                } else if (es32) {
                    if (tieneResolucion && act.getEstado() != EstadoActividad.COMPLETADA) {
                        act.completar(usuarioId);
                        planTrabajoRepository.saveActividad(act);
                    } else if (enviadaOAprobada && !tieneResolucion && act.getEstado() != EstadoActividad.COMPLETADA) {
                        act.cambiarEstado(EstadoActividad.EN_PROGRESO);
                        planTrabajoRepository.saveActividad(act);
                    }
                }
            }
        });
    }

    private void generarNotificacionesMiembrosNuevos(
            ProcesoAcreditacion proceso,
            UUID usuarioId,
            List<MiembroCaInputDto> miembros
    ) {
        if (miembros == null || miembros.isEmpty()) {
            return;
        }
        if (crearSolicitudNotificacionUseCase == null) {
            log.warn("No se pudo emitir solicitud de notificación de miembro nuevo: crearSolicitudNotificacionUseCase es null");
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
