package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.input.RemitirRegistroCaUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RemitirRegistroCaService implements RemitirRegistroCaUseCase {

    private static final String CARPETA_CONVOCATORIA = "01-Convocatoria y formalizacion CONEAU/";

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final DocumentStoragePort storagePort;
    private final CarreraRepositoryPort carreraRepository;
    private final UserRepository userRepository;
    private final CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase;

    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort
    ) {
        this(registroCaRepository, procesoRepository, storagePort, null, null, null);
    }

    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort,
            CarreraRepositoryPort carreraRepository,
            UserRepository userRepository,
            CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.storagePort = storagePort;
        this.carreraRepository = carreraRepository;
        this.userRepository = userRepository;
        this.crearSolicitudNotificacionUseCase = crearSolicitudNotificacionUseCase;
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

        generarNotificacionesMiembrosNuevos(proceso, usuarioDcId, miembros);

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
