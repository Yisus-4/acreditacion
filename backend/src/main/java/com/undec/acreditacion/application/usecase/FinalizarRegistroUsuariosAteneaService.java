package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.FinalizarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import com.undec.acreditacion.domain.services.GeneradorRegistroUsuariosAteneaPdfService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class FinalizarRegistroUsuariosAteneaService implements FinalizarRegistroUsuariosAteneaUseCase {

    private static final String CARPETA_CONVOCATORIA = "01-Convocatoria y formalizacion CONEAU/";

    private final RegistroUsuariosAteneaRepositoryPort registroUsuariosRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final DocumentStoragePort storagePort;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;

    public FinalizarRegistroUsuariosAteneaService(
            RegistroUsuariosAteneaRepositoryPort registroUsuariosRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            DocumentStoragePort storagePort,
            DocumentoAcreditacionRepositoryPort documentoRepository
    ) {
        this.registroUsuariosRepository = registroUsuariosRepository;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.registroConvocatoriaRepository = registroConvocatoriaRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.storagePort = storagePort;
        this.documentoRepository = documentoRepository;
    }

    @Override
    public RegistroUsuariosAteneaDto finalizar(UUID procesoId, UUID usuarioAeaId) {
        RegistroUsuariosAtenea registro = registroUsuariosRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el registro de usuarios de ATENEA para el proceso: " + procesoId));

        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        Carrera carrera = carreraRepository.findById(proceso.getCarreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera vinculada al proceso"));

        String cicloFase = "1raFase";
        Optional<RegistroConvocatoria> convOpt = registroConvocatoriaRepository.findByProcesoId(procesoId);
        if (convOpt.isPresent() && convOpt.get().getFaseEvaluacion() != null) {
            FaseEvaluacion fase = convOpt.get().getFaseEvaluacion();
            cicloFase = switch (fase) {
                case PRIMERA_FASE -> "1raFase";
                case SEGUNDA_FASE -> "2daFase";
                case NUEVO_CICLO -> "NuevoCiclo";
                case SEGUIMIENTO -> "Seguimiento";
            };
        }

        String anio = String.valueOf(LocalDate.now().getYear());
        String nombreCarrera = normalizarNombre(carrera.getNombre());
        String nombreArchivo = "RG-003_Registro de usuarios de ATENEA_" + nombreCarrera + "_" + cicloFase + "_" + anio + ".pdf";
        String objectPath = CARPETA_CONVOCATORIA + nombreArchivo;

        String estadoCarreraStr = convOpt
                .map(RegistroConvocatoria::getEstadoCarrera)
                .map(e -> e.toLowerCase().replace('_', ' '))
                .orElse("en funcionamiento");
        String tipoConvocatoria = "Grado - " + estadoCarreraStr + " - " + (cicloFase != null ? cicloFase : "1era Fase");
        String nroExpedienteTad = convOpt
                .map(RegistroConvocatoria::getNroResolucionConeau)
                .filter(n -> !n.isBlank())
                .orElseGet(proceso::getCodigo);

        byte[] pdfBytes = GeneradorRegistroUsuariosAteneaPdfService.generarPdf(
                registro, carrera.getNombre(), tipoConvocatoria, anio, nroExpedienteTad
        );
        String storedPath = storagePort.uploadFile(pdfBytes, objectPath, DocumentoAcreditacion.MIME_PDF);

        registro.finalizar(usuarioAeaId, storedPath);
        RegistroUsuariosAtenea guardado = registroUsuariosRepository.save(registro);

        // Actualizar automáticamente la actividad 3.3 en PlanTrabajo (hoja INFO) marcándola COMPLETADA
        planTrabajoRepository.findByProcesoId(procesoId).ifPresent(plan -> {
            Optional<ActividadPlanTrabajo> actUsuariosOpt = plan.getActividades().stream()
                    .filter(a -> a.getHoja() == HojaPlanTrabajo.INFO
                            && ("3.3".equals(a.getCodigoActividad())
                            || a.getNombre().toLowerCase().contains("definir usuarios")))
                    .findFirst();

            ActividadPlanTrabajo actUsuarios = actUsuariosOpt.orElseGet(() -> {
                ActividadPlanTrabajo nueva = new ActividadPlanTrabajo(
                        UUID.randomUUID(),
                        plan.getId(),
                        8,
                        "3.3",
                        "Definir usuarios y permisos de acceso para la solicitud de acreditación",
                        EstadoActividad.PENDIENTE,
                        null, null, null, null,
                        HojaPlanTrabajo.INFO,
                        null, null,
                        "Área de Evaluación y Acreditación (AEA)",
                        PrioridadActividad.MEDIA,
                        Instant.now(),
                        Instant.now()
                );
                plan.agregarActividad(nueva);
                return planTrabajoRepository.saveActividad(nueva);
            });

            if (actUsuarios.getEstado() != EstadoActividad.COMPLETADA) {
                actUsuarios.completar(usuarioAeaId);
                planTrabajoRepository.saveActividad(actUsuarios);
            }

            // Registrar DocumentoAcreditacion formal RG-003
            DocumentoAcreditacion docFormal = DocumentoAcreditacion.crear(
                    procesoId,
                    actUsuarios.getId(),
                    nombreArchivo,
                    TipoDocumentoAcreditacion.REGISTRO_USUARIOS_ATENEA,
                    "AEA_OFICIAL",
                    "Registro formal RG-003 de Usuarios de CONEAU Global ATENEA",
                    DocumentoAcreditacion.MIME_PDF,
                    (long) pdfBytes.length,
                    storedPath,
                    usuarioAeaId
            );
            documentoRepository.save(docFormal);
        });

        return RegistroUsuariosAteneaMapper.toDto(guardado);
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) return "Carrera";
        String sinAcentos = java.text.Normalizer.normalize(nombre.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.replaceAll("[^a-zA-Z0-9 ]", "").replace(" ", "_");
    }
}
