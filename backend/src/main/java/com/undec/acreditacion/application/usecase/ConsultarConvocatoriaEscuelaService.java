package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarConvocatoriaEscuelaUseCase;
import com.undec.acreditacion.application.input.ConvocatoriaCarreraItemDto;
import com.undec.acreditacion.application.input.ConvocatoriaEscuelaDetalleDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConsultarConvocatoriaEscuelaService implements ConsultarConvocatoriaEscuelaUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final RegistroUsuariosAteneaRepositoryPort registroUsuariosAteneaRepository;

    public ConsultarConvocatoriaEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository) {
        this(convocatoriaRepository, unidadAcademicaRepository, carreraRepository, procesoRepository, null, null, null);
    }

    public ConsultarConvocatoriaEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            RegistroUsuariosAteneaRepositoryPort registroUsuariosAteneaRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository);
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = planTrabajoRepository;
        this.registroCaRepository = registroCaRepository;
        this.registroUsuariosAteneaRepository = registroUsuariosAteneaRepository;
    }

    @Override
    public ConvocatoriaEscuelaDetalleDto obtenerPorId(UUID id) {
        Convocatoria c = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la convocatoria con ID: " + id));
        return mapearDetalleDto(c);
    }

    @Override
    public List<ConvocatoriaEscuelaDetalleDto> listarTodas() {
        return convocatoriaRepository.findAll().stream()
                .map(this::mapearDetalleDto)
                .toList();
    }

    private ConvocatoriaEscuelaDetalleDto mapearDetalleDto(Convocatoria c) {
        Optional<UnidadAcademica> escuelaOpt = unidadAcademicaRepository.findById(c.getUnidadAcademicaId());
        String escuelaNombre = escuelaOpt.map(UnidadAcademica::getNombre).orElse("Escuela Desconocida");
        String escuelaSigla = escuelaOpt.map(UnidadAcademica::getSigla).orElse("");

        List<ConvocatoriaCarreraItemDto> carrerasDto = new ArrayList<>();
        for (ConvocatoriaCarrera item : c.getCarreras()) {
            Optional<Carrera> carreraOpt = carreraRepository.findById(item.getCarreraId());
            String estadoProceso = null;
            if (item.getProcesoId() != null) {
                estadoProceso = resolverEstadoProcesoReal(item.getProcesoId());
            }

            carrerasDto.add(new ConvocatoriaCarreraItemDto(
                    item.getId(),
                    item.getCarreraId(),
                    carreraOpt.map(Carrera::getNombre).orElse("Carrera"),
                    carreraOpt.map(Carrera::getSigla).orElse(""),
                    carreraOpt.map(ca -> ca.getModalidad().name()).orElse("PRESENCIAL"),
                    carreraOpt.map(Carrera::getNivel).orElse(Carrera.NIVEL_GRADO),
                    carreraOpt.map(Carrera::getAreaDisciplinaria).orElse(""),
                    item.isSeleccionadaPorAea(),
                    item.isEsObligatoria(),
                    item.isSeleccionadaPorDe(),
                    item.getProcesoId(),
                    estadoProceso
            ));
        }

        return new ConvocatoriaEscuelaDetalleDto(
                c.getId(),
                c.getUnidadAcademicaId(),
                escuelaNombre,
                escuelaSigla,
                c.getCodigo(),
                c.getTitulo(),
                c.getTipoProceso(),
                c.getNroResolucionConeau(),
                c.getFechaInicio(),
                c.getFechaCierre(),
                c.getEstado(),
                c.getObservaciones(),
                c.getCreadoPor(),
                c.getConfirmadoPorDe(),
                c.getConfirmadoPorDeEn(),
                c.getCreadoEn(),
                c.getActualizadoEn(),
                carrerasDto
        );
    }

    private String resolverEstadoProcesoReal(UUID procesoId) {
        if (registroUsuariosAteneaRepository != null) {
            Optional<RegistroUsuariosAtenea> ateneaOpt = registroUsuariosAteneaRepository.findByProcesoId(procesoId);
            if (ateneaOpt.isPresent() && ateneaOpt.get().getEstado() == EstadoRegistroUsuariosAtenea.FINALIZADO) {
                return "FINALIZADO";
            }
        }

        Optional<PlanTrabajo> planOpt = planTrabajoRepository != null
                ? planTrabajoRepository.findByProcesoId(procesoId)
                : Optional.empty();
        boolean planEnviado = planOpt.map(PlanTrabajo::isEnviado).orElse(false);

        if (registroCaRepository != null) {
            Optional<RegistroConformacionCa> caOpt = registroCaRepository.findByProcesoId(procesoId);
            if (caOpt.isPresent()) {
                RegistroConformacionCa ca = caOpt.get();
                if (ca.getEstado() == EstadoRegistroCa.APROBADA
                        || (ca.getResolucionStoragePath() != null && !ca.getResolucionStoragePath().isBlank())) {
                    return "USUARIOS_ATENEA";
                }
                boolean caConAvanceReal = ca.getEstado() == EstadoRegistroCa.EN_CARGA_BORRADOR
                        || ca.getEstado() == EstadoRegistroCa.REMITIDA_A_AEA
                        || ca.getEstado() == EstadoRegistroCa.OBSERVADA
                        || (ca.getMiembros() != null && !ca.getMiembros().isEmpty());
                if (caConAvanceReal || planEnviado) {
                    return "EN_AUTOEVALUACION";
                }
            }
        }

        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            if (planEnviado) {
                return "EN_AUTOEVALUACION";
            }
            boolean normativaCompletada = plan.getActividades().stream()
                    .filter(PlanTrabajo::esActividadNormativa)
                    .anyMatch(a -> a.getEstado() == EstadoActividad.COMPLETADA);
            boolean formalizacionIniciadaOCompleta = plan.getActividades().stream()
                    .filter(PlanTrabajo::esActividadFormalizacionElectronica)
                    .anyMatch(a -> a.getEstado() == EstadoActividad.COMPLETADA || a.getEstado() == EstadoActividad.EN_PROGRESO);
            if (normativaCompletada && formalizacionIniciadaOCompleta) {
                return "PLAN_TRABAJO";
            }
            boolean algunaEnProgresoOCompletada = plan.getActividades().stream()
                    .filter(a -> !esFalsoPositivoAutoCompletado(a))
                    .anyMatch(a -> a.getEstado() == EstadoActividad.COMPLETADA || a.getEstado() == EstadoActividad.EN_PROGRESO);
            if (algunaEnProgresoOCompletada) {
                return "EN_FORMALIZACION";
            }
        }

        return procesoRepository.findById(procesoId)
                .map(p -> {
                    EstadoProcesoAcreditacion est = p.getEstado();
                    if (est == EstadoProcesoAcreditacion.FINALIZADO || est == EstadoProcesoAcreditacion.CANCELADO) {
                        return est.name();
                    }
                    return "INICIADO";
                })
                .orElse("INICIADO");
    }

    private boolean esFalsoPositivoAutoCompletado(ActividadPlanTrabajo a) {
        if (a.getEstado() != EstadoActividad.COMPLETADA || a.getCompletadaPor() != null) {
            return false;
        }
        String sintesis = a.getSintesisAcuerdos();
        return "Informado sobre el proceso de acreditación".equals(sintesis)
                || "Cronograma institucional y formulario RG-001 formulados y vigentes".equals(sintesis);
    }
}
