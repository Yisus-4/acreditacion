package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.AprobarRegistroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.services.GeneradorRegistroCaPdfService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class AprobarRegistroCaService implements AprobarRegistroCaUseCase {

    private static final String CARPETA_CONVOCATORIA = "01-Convocatoria y formalizacion CONEAU/";

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final DocumentStoragePort storagePort;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;

    public AprobarRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            DocumentStoragePort storagePort,
            DocumentoAcreditacionRepositoryPort documentoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.storagePort = storagePort;
        this.documentoRepository = documentoRepository;
    }

    @Override
    public RegistroCaDto aprobar(UUID procesoId, UUID usuarioAeaId) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        Carrera carrera = carreraRepository.findById(proceso.getCarreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera vinculada al proceso"));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new DomainValidationException("No existe registro de conformación de CA para el proceso"));

        String anio = String.valueOf(LocalDate.now().getYear());
        String nombreArchivo = "RG-002_Registro de conformación de CA_" + normalizarNombre(carrera.getNombre()) + "_" + anio + ".pdf";
        String objectPath = CARPETA_CONVOCATORIA + nombreArchivo;

        byte[] pdfBytes = GeneradorRegistroCaPdfService.generarPdf(ca, carrera.getNombre(), anio);
        String storedPath = storagePort.uploadFile(pdfBytes, objectPath, DocumentoAcreditacion.MIME_PDF);

        ca.aprobar(usuarioAeaId, storedPath);
        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        // Actualizar automáticamente las actividades de CA (3.1 y 3.2) en PlanTrabajo marcándolas completadas en hoja INFO
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
                        com.undec.acreditacion.domain.entities.EstadoActividad.PENDIENTE,
                        null, null, null, null,
                        HojaPlanTrabajo.INFO,
                        null, null,
                        "Área de Evaluación y Acreditación (AEA) / Director de Carrera",
                        com.undec.acreditacion.domain.entities.PrioridadActividad.ALTA,
                        java.time.Instant.now(),
                        java.time.Instant.now()
                );
                plan.agregarActividad(nueva);
                actsCa.add(planTrabajoRepository.saveActividad(nueva));
            }

            for (ActividadPlanTrabajo actCa : actsCa) {
                if (actCa.getEstado() != com.undec.acreditacion.domain.entities.EstadoActividad.COMPLETADA) {
                    actCa.completar(usuarioAeaId);
                    planTrabajoRepository.saveActividad(actCa);
                }
            }

            ActividadPlanTrabajo actCaPrincipal = actsCa.get(0);

            // Registrar DocumentoAcreditacion formal
            DocumentoAcreditacion docFormal = DocumentoAcreditacion.crear(
                    procesoId,
                    actCaPrincipal.getId(),
                    nombreArchivo,
                    TipoDocumentoAcreditacion.REGISTRO_CONFORMACION_CA,
                    "AEA_OFICIAL",
                    "Registro formal RG-002 de Conformación de Comisión de Autoevaluación",
                    DocumentoAcreditacion.MIME_PDF,
                    (long) pdfBytes.length,
                    storedPath,
                    usuarioAeaId
            );
            documentoRepository.save(docFormal);
        });

        return RegistroCaMapper.toDto(guardado);
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) return "Carrera";
        return nombre.trim().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]", "").replace(" ", "_");
    }
}
