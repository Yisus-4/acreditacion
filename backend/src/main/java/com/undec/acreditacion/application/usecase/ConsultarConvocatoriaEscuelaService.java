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
            if (ateneaOpt.isPresent()) {
                if (ateneaOpt.get().getEstado() == EstadoRegistroUsuariosAtenea.FINALIZADO) {
                    return "FINALIZADO";
                }
                return "USUARIOS_ATENEA";
            }
        }

        if (registroCaRepository != null) {
            Optional<RegistroConformacionCa> caOpt = registroCaRepository.findByProcesoId(procesoId);
            if (caOpt.isPresent()) {
                if (caOpt.get().getEstado() == EstadoRegistroCa.APROBADA
                        || (caOpt.get().getResolucionStoragePath() != null && !caOpt.get().getResolucionStoragePath().isBlank())) {
                    return "USUARIOS_ATENEA";
                }
                return "EN_AUTOEVALUACION";
            }
        }

        if (planTrabajoRepository != null) {
            Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
            if (planOpt.isPresent()) {
                boolean tieneActividadesCompletadas = planOpt.get().getActividades().stream()
                        .anyMatch(a -> a.getEstado() == EstadoActividad.COMPLETADA);
                if (tieneActividadesCompletadas) {
                    return "PLAN_TRABAJO";
                }
            }
        }

        return procesoRepository.findById(procesoId)
                .map(p -> p.getEstado().name())
                .orElse("INICIADO");
    }
}
