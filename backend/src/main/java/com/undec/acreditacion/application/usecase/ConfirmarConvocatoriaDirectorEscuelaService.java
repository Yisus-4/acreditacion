package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaCommand;
import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConfirmarConvocatoriaDirectorEscuelaService implements ConfirmarConvocatoriaDirectorEscuelaUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final CarreraRepositoryPort carreraRepository;

    public ConfirmarConvocatoriaDirectorEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.registroRepository = Objects.requireNonNull(registroRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
    }

    @Override
    public Convocatoria confirmar(ConfirmarConvocatoriaDirectorEscuelaCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando de confirmación es requerido");
        }
        if (command.convocatoriaId() == null) {
            throw new DomainValidationException("El ID de la convocatoria es requerido");
        }

        Convocatoria convocatoria = convocatoriaRepository.findById(command.convocatoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la convocatoria con ID: " + command.convocatoriaId()));

        convocatoria.confirmarPorDirectorEscuela(command.carrerasSeleccionadas(), command.confirmadoPorDe());

        // Para cada carrera confirmada, inicializar proceso de acreditación si no existe
        int anio = convocatoria.getFechaInicio().getYear();

        for (ConvocatoriaCarrera item : convocatoria.getCarreras()) {
            if (item.isSeleccionadaPorDe()) {
                UUID carreraId = item.getCarreraId();
                Carrera carrera = carreraRepository.findById(carreraId)
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + carreraId));

                // Si ya tiene un proceso vinculado, lo mantenemos
                if (item.getProcesoId() == null) {
                    Optional<ProcesoAcreditacion> activoOpt = procesoRepository.findActivoByCarreraId(carreraId);
                    if (activoOpt.isPresent()) {
                        item.vincularProceso(activoOpt.get().getId());
                    } else {
                        // Crear nuevo proceso formal para esta carrera dentro de la convocatoria
                        long correlativo = procesoRepository.countByAnio(anio) + 1;
                        String siglaCarrera = carrera.getSigla() != null && !carrera.getSigla().isBlank()
                                ? carrera.getSigla().trim().toUpperCase()
                                : "CARR";
                        String codigoProceso = String.format("CONV-%d-%s-%02d", anio, siglaCarrera, correlativo);
                        String tituloProceso = "Acreditación - " + carrera.getNombre() + " (" + anio + ")";

                        ProcesoAcreditacion nuevoProceso = ProcesoAcreditacion.iniciar(
                                carreraId,
                                codigoProceso,
                                tituloProceso,
                                convocatoria.getTipoProceso(),
                                command.confirmadoPorDe()
                        );
                        ProcesoAcreditacion guardado = procesoRepository.save(nuevoProceso);
                        item.vincularProceso(guardado.getId());

                        // Crear RegistroConvocatoria (RG-001)
                        String modalidadRg001 = carrera.getModalidad() == ModalidadCarrera.DISTANCIA ? "A_DISTANCIA" : "PRESENCIAL";
                        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                                guardado.getId(),
                                convocatoria.getNroResolucionConeau(),
                                convocatoria.getFechaInicio(),
                                convocatoria.getFechaCierre(),
                                convocatoria.getObservaciones(),
                                null,
                                carrera.getAreaDisciplinaria(),
                                modalidadRg001,
                                convocatoria.getTipoProceso().name(),
                                FaseEvaluacion.PRIMERA_FASE,
                                null
                        );
                        registroRepository.save(registro);

                        // Crear Plan de Trabajo (PT-001)
                        String codigoPlan = String.format("PT-001-%s-%d", siglaCarrera, anio);
                        PlanTrabajo plan = PlanTrabajo.inicializar(
                                guardado.getId(),
                                codigoPlan,
                                TipoPlanTrabajo.PT_001_ACREDITACION
                        );
                        planTrabajoRepository.save(plan);
                    }
                }
            }
        }

        return convocatoriaRepository.save(convocatoria);
    }
}
