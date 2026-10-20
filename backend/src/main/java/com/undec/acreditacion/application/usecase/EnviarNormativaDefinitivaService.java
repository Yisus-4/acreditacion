package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EnviarNormativaDefinitivaCommand;
import com.undec.acreditacion.application.input.EnviarNormativaDefinitivaUseCase;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EnviarNormativaDefinitivaService implements EnviarNormativaDefinitivaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final EmailNotificationPort emailNotificationPort;

    public EnviarNormativaDefinitivaService(
        ProcesoAcreditacionRepositoryPort procesoRepository,
        PlanTrabajoRepositoryPort planTrabajoRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        DocumentoNormativaRepositoryPort documentoRepository,
        CarreraRepositoryPort carreraRepository,
        RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
        EmailNotificationPort emailNotificationPort
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.registroConvocatoriaRepository = Objects.requireNonNull(registroConvocatoriaRepository);
        this.emailNotificationPort = Objects.requireNonNull(emailNotificationPort);
    }

    @Override
    public NormativaDetalleDto ejecutar(EnviarNormativaDefinitivaCommand command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo");
        UUID procesoId = command.procesoId();

        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado con ID: " + procesoId));

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado para el proceso ID: " + procesoId));

        ActividadPlanTrabajo actividadNormativa = NormativaMapperHelper.resolverActividadNormativa(plan);

        SolicitudNormativaCarrera solicitud = solicitudRepository.findByActividadId(actividadNormativa.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Solicitud de normativa no encontrada"));

        List<DocumentoNormativa> documentos = documentoRepository.findBySolicitudNormativaId(solicitud.getId());

        boolean esNueva = proceso.getTipoProceso() == TipoProcesoAcreditacion.NUEVA;
        boolean conHistorialConeau = proceso.getTipoProceso() == TipoProcesoAcreditacion.EN_FUNCIONAMIENTO;

        solicitud.enviarDefinitivo(command.usuarioId(), LocalDate.now(), documentos, esNueva, conHistorialConeau);

        for (DocumentoNormativa doc : documentos) {
            documentoRepository.save(doc);
        }

        SolicitudNormativaCarrera guardada = solicitudRepository.save(solicitud);

        String carreraNombre = carreraRepository.findById(proceso.getCarreraId())
            .map(Carrera::getNombre)
            .orElse("Carrera UNdeC");

        String codigoConvocatoria = registroConvocatoriaRepository.findByProcesoId(proceso.getId())
            .map(RegistroConvocatoria::getNroResolucionConeau)
            .orElse(proceso.getCodigo());

        emailNotificationPort.enviarRemisionRevision(
            proceso.getId(),
            "acreditacion@undec.edu.ar",
            "Área de Evaluación y Acreditación",
            carreraNombre,
            codigoConvocatoria,
            guardada.getCumplimientoPlazo(),
            documentos.size()
        );

        return NormativaMapperHelper.toDto(guardada, documentos, proceso);
    }
}
