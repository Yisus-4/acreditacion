package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaCommand;
import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.persistence.postgres.ConvocatoriaCarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ConvocatoriaCarreraPlanJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataConvocatoriaCarreraPlanRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConfirmarConvocatoriaDirectorEscuelaService implements ConfirmarConvocatoriaDirectorEscuelaUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository;
    private final SpringDataPlanEstudioFt001Repository planEstudioRepository;

    public ConfirmarConvocatoriaDirectorEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository) {
        this(convocatoriaRepository, procesoRepository, registroRepository, planTrabajoRepository, carreraRepository, null, null);
    }

    public ConfirmarConvocatoriaDirectorEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository,
            SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.registroRepository = Objects.requireNonNull(registroRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.convocatoriaCarreraPlanRepository = convocatoriaCarreraPlanRepository;
        this.planEstudioRepository = planEstudioRepository;
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

                List<UUID> planesSeleccionados = null;
                if (command.planesPorCarrera() != null && command.planesPorCarrera().containsKey(carreraId)) {
                    planesSeleccionados = command.planesPorCarrera().get(carreraId);
                    if (planesSeleccionados == null || planesSeleccionados.isEmpty()) {
                        throw new DomainValidationException("Debe seleccionar al menos un plan de estudio vigente para la carrera " + carrera.getNombre());
                    }
                }

                if (planesSeleccionados != null && planEstudioRepository != null) {
                    for (UUID planId : planesSeleccionados) {
                        PlanEstudioFt001JpaEntity planEnt = planEstudioRepository.findById(planId)
                                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + planId));
                        if (!Boolean.TRUE.equals(planEnt.getEsMaestro()) || planEnt.getEstadoVigencia() != EstadoVigenciaPlan.VIGENTE) {
                            throw new DomainValidationException("El plan de estudio seleccionado no es un plan maestro vigente para la carrera " + carrera.getNombre());
                        }
                    }
                } else if (planesSeleccionados == null && planEstudioRepository != null) {
                    List<PlanEstudioFt001JpaEntity> planesVigentes = planEstudioRepository
                            .findByCarreraIdAndEstadoVigenciaAndEsMaestroTrue(carreraId, EstadoVigenciaPlan.VIGENTE);
                    if (!planesVigentes.isEmpty()) {
                        planesSeleccionados = planesVigentes.stream().map(PlanEstudioFt001JpaEntity::getId).toList();
                    }
                }

                // Si la carrera en esta convocatoria aún no tiene proceso creado, lo inicializamos de forma aislada
                if (item.getProcesoId() == null) {
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

                // Persistir planes confirmados en convocatoria_carrera_plan
                if (convocatoriaCarreraPlanRepository != null && planesSeleccionados != null && planEstudioRepository != null) {
                    for (UUID planId : planesSeleccionados) {
                        Optional<ConvocatoriaCarreraPlanJpaEntity> existingOpt = convocatoriaCarreraPlanRepository
                                .findByConvocatoriaCarreraIdAndPlanEstudioMaestroId(item.getId(), planId);
                        if (existingOpt.isPresent()) {
                            ConvocatoriaCarreraPlanJpaEntity existing = existingOpt.get();
                            existing.setSeleccionadoPorDe(true);
                            existing.setActualizadoEn(Instant.now());
                            convocatoriaCarreraPlanRepository.save(existing);
                        } else {
                            PlanEstudioFt001JpaEntity planEnt = planEstudioRepository.findById(planId).orElse(null);
                            if (planEnt != null) {
                                ConvocatoriaCarreraJpaEntity ccEntity = new ConvocatoriaCarreraJpaEntity();
                                ccEntity.setId(item.getId());
                                ConvocatoriaCarreraPlanJpaEntity ccp = new ConvocatoriaCarreraPlanJpaEntity(
                                        UUID.randomUUID(),
                                        ccEntity,
                                        carreraId,
                                        planEnt,
                                        null,
                                        true,
                                        true
                                );
                                convocatoriaCarreraPlanRepository.save(ccp);
                            }
                        }
                    }
                }
            }
        }

        return convocatoriaRepository.save(convocatoria);
    }
}
