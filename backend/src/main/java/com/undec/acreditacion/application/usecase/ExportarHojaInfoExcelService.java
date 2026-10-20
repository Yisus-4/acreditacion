package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ExportarHojaInfoExcelUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ExportarHojaInfoExcelService implements ExportarHojaInfoExcelUseCase {

    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final ExcelReportGeneratorPort excelReportGeneratorPort;

    public ExportarHojaInfoExcelService(
        PlanTrabajoRepositoryPort planTrabajoRepository,
        ProcesoAcreditacionRepositoryPort procesoRepository,
        CarreraRepositoryPort carreraRepository,
        RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        ExcelReportGeneratorPort excelReportGeneratorPort
    ) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.registroConvocatoriaRepository = Objects.requireNonNull(registroConvocatoriaRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.excelReportGeneratorPort = Objects.requireNonNull(excelReportGeneratorPort);
    }

    @Override
    public byte[] ejecutar(UUID planTrabajoId) {
        Objects.requireNonNull(planTrabajoId, "El ID del plan de trabajo es obligatorio");

        PlanTrabajo plan = planTrabajoRepository.findById(planTrabajoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado con ID: " + planTrabajoId));

        ProcesoAcreditacion proceso = procesoRepository.findById(plan.getProcesoId())
            .orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado con ID: " + plan.getProcesoId()));

        Carrera carrera = carreraRepository.findById(proceso.getCarreraId())
            .orElseThrow(() -> new ResourceNotFoundException("Carrera no encontrada con ID: " + proceso.getCarreraId()));

        RegistroConvocatoria convocatoria = registroConvocatoriaRepository.findByProcesoId(proceso.getId())
            .orElse(null);

        List<ActividadPlanTrabajo> actividades = plan.getActividades();

        Optional<ActividadPlanTrabajo> actividad2 = actividades.stream()
            .filter(a -> a.getNumeroOrden() == 2 || a.getCodigoActividad().contains("ACT-02"))
            .findFirst();

        Optional<SolicitudNormativaCarrera> solicitud = actividad2
            .flatMap(a -> solicitudRepository.findByActividadId(a.getId()));

        return excelReportGeneratorPort.generarHojaInfoExcel(
            plan,
            proceso,
            carrera,
            convocatoria,
            actividades,
            solicitud
        );
    }
}
