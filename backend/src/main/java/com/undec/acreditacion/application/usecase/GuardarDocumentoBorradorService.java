package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DocumentoNormativaDto;
import com.undec.acreditacion.application.input.GuardarDocumentoBorradorCommand;
import com.undec.acreditacion.application.input.GuardarDocumentoBorradorUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.NormativaValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GuardarDocumentoBorradorService implements GuardarDocumentoBorradorUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final DocumentStoragePort documentStoragePort;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;

    public GuardarDocumentoBorradorService(
        ProcesoAcreditacionRepositoryPort procesoRepository,
        PlanTrabajoRepositoryPort planTrabajoRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        DocumentoNormativaRepositoryPort documentoRepository,
        DocumentStoragePort documentStoragePort,
        CarreraRepositoryPort carreraRepository,
        RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.documentStoragePort = Objects.requireNonNull(documentStoragePort);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.registroConvocatoriaRepository = Objects.requireNonNull(registroConvocatoriaRepository);
    }

    @Override
    public DocumentoNormativaDto ejecutar(GuardarDocumentoBorradorCommand command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo");
        byte[] contenido = command.contenido();

        if (contenido == null || contenido.length < 5) {
            throw new NormativaValidationException("El archivo debe ser un documento PDF válido (%PDF-1.)");
        }

        String header = new String(contenido, 0, Math.min(contenido.length, 8), StandardCharsets.US_ASCII);
        if (!header.startsWith("%PDF-")) {
            throw new NormativaValidationException("El archivo debe ser un documento PDF válido (%PDF-1.)");
        }

        UUID procesoId = command.procesoId();
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado con ID: " + procesoId));

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado para el proceso ID: " + procesoId));

        ActividadPlanTrabajo actividadNormativa = NormativaMapperHelper.resolverActividadNormativa(plan);

        SolicitudNormativaCarrera solicitud = solicitudRepository.findByActividadId(actividadNormativa.getId())
            .orElseGet(() -> SolicitudNormativaCarrera.iniciar(proceso.getId(), actividadNormativa.getId()));

        if (solicitud.getEstado() == EstadoSolicitudNormativa.ENVIADA_A_REVISION || solicitud.getEstado() == EstadoSolicitudNormativa.COMPLETADA) {
            throw new EstadoInvalidoException("No se pueden subir documentos cuando la solicitud está en revisión o completada");
        }

        solicitud.iniciarCargaBorrador();
        solicitudRepository.save(solicitud);

        String siglaCarrera = carreraRepository.findById(proceso.getCarreraId())
            .map(Carrera::getSigla)
            .orElse("CARRERA");

        String codigoConvocatoria = registroConvocatoriaRepository.findByProcesoId(proceso.getId())
            .map(RegistroConvocatoria::getNroResolucionConeau)
            .orElse(proceso.getCodigo());

        String anio = String.valueOf(LocalDate.now().getYear());
        String carpetaRelativa = command.tipoDocumento().getCarpetaDestino().getRutaRelativa();
        String safeName = sanitizeFilename(command.nombreOriginal());
        String storagePath = siglaCarrera + "_" + codigoConvocatoria.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + anio
            + "/" + carpetaRelativa + UUID.randomUUID() + "_" + safeName;

        documentStoragePort.uploadFile(contenido, storagePath, "application/pdf");

        List<DocumentoNormativa> existentes = documentoRepository.findBySolicitudNormativaId(solicitud.getId());
        Optional<DocumentoNormativa> existente = command.tipoDocumento() == TipoDocumentoNormativa.ACTA_COMISION_CURRICULAR
            ? existentes.stream().filter(d -> d.getTipoDocumento() == TipoDocumentoNormativa.ACTA_COMISION_CURRICULAR && d.isObservado()).findFirst()
            : existentes.stream().filter(d -> d.getTipoDocumento() == command.tipoDocumento()).findFirst();

        DocumentoNormativa docAGuardar;
        if (existente.isPresent()) {
            DocumentoNormativa prev = existente.get();
            docAGuardar = new DocumentoNormativa(
                prev.getId(),
                solicitud.getId(),
                proceso.getId(),
                command.tipoDocumento(),
                command.tipoDocumento().getCarpetaDestino(),
                command.nombreOriginal(),
                storagePath,
                "application/pdf",
                contenido.length,
                true,
                false,
                null,
                command.subidoPor(),
                prev.getCreadoEn(),
                java.time.Instant.now()
            );
        } else {
            docAGuardar = DocumentoNormativa.crearBorrador(
                solicitud.getId(),
                proceso.getId(),
                command.tipoDocumento(),
                command.nombreOriginal(),
                storagePath,
                "application/pdf",
                contenido.length,
                command.subidoPor()
            );
        }

        DocumentoNormativa guardado = documentoRepository.save(docAGuardar);
        return NormativaMapperHelper.toDocDto(guardado);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "documento.pdf";
        }
        return filename.replaceAll("[/\\\\?%*:|\"<>]", "_").trim();
    }
}
