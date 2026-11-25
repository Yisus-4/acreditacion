package com.undec.acreditacion.infrastructure.datoscentrales.service;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.*;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoActividadMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.estandares.persistence.EstandarJpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataEstandarRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DatosCentralesService {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository;
    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataAmbitoPracticaRepository ambitoPracticaRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;
    private final SpringDataEstandarRepository estandarRepository;

    public DatosCentralesService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository) {
        this(planRepository, cuerpoAcademicoRepository, espacioFisicoRepository,
                ambitoPracticaRepository, carreraRepository, actividadRepository, null, null);
    }

    public DatosCentralesService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService) {
        this(planRepository, cuerpoAcademicoRepository, espacioFisicoRepository,
                ambitoPracticaRepository, carreraRepository, actividadRepository,
                sincronizacionBidireccionalPlanService, null);
    }

    @Autowired
    public DatosCentralesService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            SpringDataEstandarRepository estandarRepository) {
        this.planRepository = Objects.requireNonNull(planRepository);
        this.cuerpoAcademicoRepository = Objects.requireNonNull(cuerpoAcademicoRepository);
        this.espacioFisicoRepository = Objects.requireNonNull(espacioFisicoRepository);
        this.ambitoPracticaRepository = Objects.requireNonNull(ambitoPracticaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
        this.estandarRepository = estandarRepository;
    }

    @Transactional(readOnly = true)
    public ResumenDatosCentralesDto obtenerResumen() {
        long planesVigentes = planRepository.countByEsMaestroTrueAndEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        long cuerpoActivo = cuerpoAcademicoRepository.countByActivoTrue();
        long espaciosFisicos = espacioFisicoRepository.countByEsMaestroTrue();
        long ambitosPractica = ambitoPracticaRepository.countByEsMaestroTrueAndActivoTrue();

        return new ResumenDatosCentralesDto(planesVigentes, cuerpoActivo, espaciosFisicos, ambitosPractica);
    }

    @Transactional(readOnly = true)
    public List<PlanEstudioMaestroDto> listarPlanesEstudio(UUID carreraId, EstadoVigenciaPlan estadoVigencia) {
        List<PlanEstudioFt001JpaEntity> entities;
        if (carreraId != null && estadoVigencia != null) {
            entities = planRepository.findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(carreraId, estadoVigencia);
        } else if (carreraId != null) {
            entities = planRepository.findByCarreraIdAndEsMaestroTrueOrderByAnioAprobacionDesc(carreraId);
        } else if (estadoVigencia != null) {
            entities = planRepository.findByEsMaestroTrueAndEstadoVigenciaOrderByAnioAprobacionDesc(estadoVigencia);
        } else {
            entities = planRepository.findByEsMaestroTrueOrderByAnioAprobacionDesc();
        }
        return entities.stream().map(this::toPlanDto).collect(Collectors.toList());
    }

    public PlanEstudioMaestroDto crearPlanEstudioMaestro(CrearPlanEstudioMaestroRequest req) {
        if (req == null) {
            throw new DomainValidationException("La solicitud de creación de plan es requerida");
        }
        if (req.carreraId() == null) {
            throw new DomainValidationException("El ID de la carrera es obligatorio");
        }
        if (req.nombrePlan() == null || req.nombrePlan().isBlank()) {
            throw new DomainValidationException("El nombre del plan de estudio es obligatorio");
        }

        CarreraJpaEntity carrera = carreraRepository.findById(req.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + req.carreraId()));

        EstandarJpaEntity estandarVigente = null;
        if (estandarRepository != null) {
            estandarVigente = estandarRepository
                    .findByCarreraIdAndEstadoVigencia(carrera.getId(), EstadoVigenciaEstandar.VIGENTE)
                    .orElseThrow(() -> new DomainValidationException(
                            "La carrera '" + carrera.getNombre()
                                    + "' no tiene un Estándar vigente configurado. "
                                    + "Configure un estándar vigente en Gestión Académica -> Estándares antes de crear un plan de estudio."));
        }

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setCarrera(carrera);
        plan.setEstandar(estandarVigente);
        plan.setProcesoId(null);
        plan.setNombrePlan(req.nombrePlan().trim());
        plan.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        plan.setEsMaestro(true);
        plan.setOrigenMaestroId(null);
        plan.setCodigoResolucion(req.codigoResolucion());
        plan.setAnioAprobacion(req.anioAprobacion());
        plan.setAnioInicio(req.anioInicio());
        plan.setDuracionAnios(req.duracionAnios());
        plan.setHorasPresenciales(req.horasPresenciales());
        plan.setHorasNoPresenciales(req.horasNoPresenciales());
        plan.setHorasTotales(req.horasTotales());
        plan.setHorasOptativasMinimas(req.horasOptativasMinimas());
        plan.setOrientacionesJson(req.orientacionesJson());
        plan.setFundamentacionModificaciones(req.fundamentacionModificaciones());
        plan.setEstructuraSintetica(req.estructuraSintetica());
        plan.setActividadesComunesCiclo(req.actividadesComunesCiclo());
        plan.setTituloIntermedioDenominacion(req.tituloIntermedioDenominacion());
        plan.setTituloIntermedioDuracion(req.tituloIntermedioDuracion());
        plan.setTituloIntermedioActividades(req.tituloIntermedioActividades());
        plan.setInstanciaFinalGraduacion(req.instanciaFinalGraduacion());
        plan.setOtraInformacion(req.otraInformacion());
        plan.setActivo(true);
        Instant now = Instant.now();
        plan.setCreadoEn(now);
        plan.setActualizadoEn(now);

        PlanEstudioFt001JpaEntity guardado = planRepository.save(plan);
        return toPlanDto(guardado);
    }

    public PlanEstudioMaestroDto cambiarEstadoVigenciaPlan(UUID id, EstadoVigenciaPlan nuevoEstado) {
        if (id == null) {
            throw new DomainValidationException("El ID del plan es requerido");
        }
        if (nuevoEstado == null) {
            throw new DomainValidationException("El nuevo estado de vigencia es requerido");
        }

        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + id));

        if (!Boolean.TRUE.equals(plan.getEsMaestro())) {
            throw new DomainValidationException("Solo se puede cambiar la vigencia a planes maestros");
        }

        plan.setEstadoVigencia(nuevoEstado);
        plan.setActualizadoEn(Instant.now());

        PlanEstudioFt001JpaEntity guardado = planRepository.save(plan);
        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionPlan(guardado);
        }
        return toPlanDto(guardado);
    }

    public PlanEstudioMaestroDto actualizarPlanEstudioMaestro(UUID id, CrearPlanEstudioMaestroRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos del plan de estudio son requeridos");
        }
        if (req.nombrePlan() == null || req.nombrePlan().isBlank()) {
            throw new DomainValidationException("El nombre del plan de estudio es obligatorio");
        }

        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + id));

        if (!Boolean.TRUE.equals(plan.getEsMaestro())) {
            throw new DomainValidationException("Solo se pueden editar planes maestros desde Gestión Académica");
        }

        if (req.carreraId() != null && (plan.getCarrera() == null || !req.carreraId().equals(plan.getCarrera().getId()))) {
            CarreraJpaEntity carrera = carreraRepository.findById(req.carreraId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + req.carreraId()));
            plan.setCarrera(carrera);
        }

        plan.setNombrePlan(req.nombrePlan().trim());
        plan.setAnioAprobacion(req.anioAprobacion());
        plan.setDuracionAnios(req.duracionAnios());
        plan.setHorasTotales(req.horasTotales());
        plan.setActualizadoEn(Instant.now());

        PlanEstudioFt001JpaEntity guardado = planRepository.save(plan);
        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionPlan(guardado);
        }
        return toPlanDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<CuerpoAcademicoMaestroDto> listarCuerpoAcademico(UUID carreraId, String query) {
        List<CuerpoAcademicoMaestroJpaEntity> list;
        if (carreraId != null && query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            list = cuerpoAcademicoRepository.findByCarreraId(carreraId).stream()
                    .filter(d -> (d.getApellido() != null && d.getApellido().toLowerCase().contains(q))
                            || (d.getNombre() != null && d.getNombre().toLowerCase().contains(q))
                            || (d.getDni() != null && d.getDni().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        } else if (carreraId != null) {
            list = cuerpoAcademicoRepository.findByCarreraId(carreraId);
        } else if (query != null && !query.isBlank()) {
            list = cuerpoAcademicoRepository.buscarPorTexto(query.trim());
        } else {
            list = cuerpoAcademicoRepository.findByActivoTrue();
        }
        return list.stream().map(this::toDocenteDto).collect(Collectors.toList());
    }

    public CuerpoAcademicoMaestroDto crearCuerpoAcademicoMaestro(CrearCuerpoAcademicoMaestroRequest req) {
        if (req == null) {
            throw new DomainValidationException("La solicitud de registro de docente es requerida");
        }
        if (req.dni() == null || req.dni().isBlank()) {
            throw new DomainValidationException("El DNI es obligatorio");
        }
        if (req.apellido() == null || req.apellido().isBlank()) {
            throw new DomainValidationException("El apellido es obligatorio");
        }
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new DomainValidationException("El nombre es obligatorio");
        }
        if (req.categoriaDocente() == null || req.categoriaDocente().isBlank()) {
            throw new DomainValidationException("La categoría docente es obligatoria");
        }
        if (req.caracterDesignacion() == null || req.caracterDesignacion().isBlank()) {
            throw new DomainValidationException("El carácter de designación es obligatorio");
        }
        if (req.dedicacionDocente() == null || req.dedicacionDocente().isBlank()) {
            throw new DomainValidationException("La dedicación docente es obligatoria");
        }
        if (cuerpoAcademicoRepository.existsByDni(req.dni().trim())) {
            throw new DomainValidationException("Ya existe un docente con DNI: " + req.dni());
        }

        CuerpoAcademicoMaestroJpaEntity docente = new CuerpoAcademicoMaestroJpaEntity();
        docente.setDni(req.dni().trim());
        docente.setCuit(req.cuit());
        docente.setApellido(req.apellido().trim());
        docente.setNombre(req.nombre().trim());
        docente.setEmailInstitucional(req.emailInstitucional());
        docente.setEmailPersonal(req.emailPersonal());
        docente.setTelefono(req.telefono());
        docente.setTituloGrado(req.tituloGrado());
        docente.setMaximoTituloPosgrado(req.maximoTituloPosgrado());
        docente.setCategoriaDocente(req.categoriaDocente().trim());
        docente.setCaracterDesignacion(req.caracterDesignacion().trim());
        docente.setDedicacionDocente(req.dedicacionDocente().trim());
        docente.setHorasSemanales(req.horasSemanales() != null ? req.horasSemanales() : 10);
        docente.setActivo(true);
        Instant now = Instant.now();
        docente.setCreadoEn(now);
        docente.setActualizadoEn(now);

        if (req.asignaciones() != null && !req.asignaciones().isEmpty()) {
            List<CuerpoAcademicoActividadMaestroJpaEntity> asignaciones = new ArrayList<>();
            for (CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem item : req.asignaciones()) {
                PlanEstudioFt001JpaEntity plan = planRepository.findById(item.planEstudioId())
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + item.planEstudioId()));
                ActividadCurricularPlanJpaEntity act = actividadRepository.findById(item.actividadCurricularId())
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad curricular ID: " + item.actividadCurricularId()));

                CuerpoAcademicoActividadMaestroJpaEntity asig = new CuerpoAcademicoActividadMaestroJpaEntity();
                asig.setDocente(docente);
                asig.setCarreraId(item.carreraId() != null ? item.carreraId() : plan.getCarrera().getId());
                asig.setPlanEstudio(plan);
                asig.setActividadCurricular(act);
                asig.setRolEnCatedra(item.rolEnCatedra() != null ? item.rolEnCatedra() : "PROFESOR_A_CARGO");
                asig.setCargaHorariaSemanal(item.cargaHorariaSemanal() != null ? item.cargaHorariaSemanal() : 4);
                asig.setCreadoEn(now);
                asignaciones.add(asig);
            }
            docente.setAsignaciones(asignaciones);
        }

        CuerpoAcademicoMaestroJpaEntity guardado = cuerpoAcademicoRepository.save(docente);
        return toDocenteDto(guardado);
    }

    public CuerpoAcademicoMaestroDto actualizarCuerpoAcademicoMaestro(UUID id, CrearCuerpoAcademicoMaestroRequest req) {
        if (id == null) {
            throw new DomainValidationException("El ID del docente es requerido");
        }
        if (req == null) {
            throw new DomainValidationException("La solicitud de actualización de docente es requerida");
        }
        CuerpoAcademicoMaestroJpaEntity docente = cuerpoAcademicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente con ID: " + id));

        if (req.dni() != null && !req.dni().isBlank()) {
            String nuevoDni = req.dni().trim();
            if (!nuevoDni.equals(docente.getDni()) && cuerpoAcademicoRepository.existsByDni(nuevoDni)) {
                throw new DomainValidationException("Ya existe otro docente con DNI: " + nuevoDni);
            }
            docente.setDni(nuevoDni);
        }
        if (req.cuit() != null) docente.setCuit(req.cuit());
        if (req.apellido() != null && !req.apellido().isBlank()) docente.setApellido(req.apellido().trim());
        if (req.nombre() != null && !req.nombre().isBlank()) docente.setNombre(req.nombre().trim());
        if (req.emailInstitucional() != null) docente.setEmailInstitucional(req.emailInstitucional());
        if (req.emailPersonal() != null) docente.setEmailPersonal(req.emailPersonal());
        if (req.telefono() != null) docente.setTelefono(req.telefono());
        if (req.tituloGrado() != null) docente.setTituloGrado(req.tituloGrado());
        if (req.maximoTituloPosgrado() != null) docente.setMaximoTituloPosgrado(req.maximoTituloPosgrado());
        if (req.categoriaDocente() != null && !req.categoriaDocente().isBlank()) docente.setCategoriaDocente(req.categoriaDocente().trim());
        if (req.caracterDesignacion() != null && !req.caracterDesignacion().isBlank()) docente.setCaracterDesignacion(req.caracterDesignacion().trim());
        if (req.dedicacionDocente() != null && !req.dedicacionDocente().isBlank()) docente.setDedicacionDocente(req.dedicacionDocente().trim());
        if (req.horasSemanales() != null) docente.setHorasSemanales(req.horasSemanales());
        docente.setActualizadoEn(Instant.now());

        return toDocenteDto(cuerpoAcademicoRepository.save(docente));
    }

    public CuerpoAcademicoMaestroDto agregarAsignacionDocente(
            UUID docenteId,
            CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem item
    ) {
        CuerpoAcademicoMaestroJpaEntity docente = cuerpoAcademicoRepository.findById(docenteId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente con ID: " + docenteId));
        if (item == null || item.planEstudioId() == null || item.actividadCurricularId() == null) {
            throw new DomainValidationException("El plan de estudio y la actividad curricular son obligatorios");
        }
        PlanEstudioFt001JpaEntity plan = planRepository.findById(item.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + item.planEstudioId()));
        ActividadCurricularPlanJpaEntity act = actividadRepository.findById(item.actividadCurricularId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad curricular ID: " + item.actividadCurricularId()));

        CuerpoAcademicoActividadMaestroJpaEntity asig = new CuerpoAcademicoActividadMaestroJpaEntity();
        asig.setDocente(docente);
        asig.setCarreraId(item.carreraId() != null ? item.carreraId() : plan.getCarrera().getId());
        asig.setPlanEstudio(plan);
        asig.setActividadCurricular(act);
        asig.setRolEnCatedra(item.rolEnCatedra() != null && !item.rolEnCatedra().isBlank() ? item.rolEnCatedra() : "PROFESOR_A_CARGO");
        asig.setCargaHorariaSemanal(item.cargaHorariaSemanal() != null ? item.cargaHorariaSemanal() : 4);
        asig.setCreadoEn(Instant.now());

        if (docente.getAsignaciones() == null) {
            docente.setAsignaciones(new ArrayList<>());
        }
        docente.getAsignaciones().add(asig);
        docente.setActualizadoEn(Instant.now());

        return toDocenteDto(cuerpoAcademicoRepository.save(docente));
    }

    public CuerpoAcademicoMaestroDto eliminarAsignacionDocente(UUID docenteId, UUID asignacionId) {
        CuerpoAcademicoMaestroJpaEntity docente = cuerpoAcademicoRepository.findById(docenteId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente con ID: " + docenteId));
        if (docente.getAsignaciones() != null) {
            docente.getAsignaciones().removeIf(a -> asignacionId.equals(a.getId()));
        }
        docente.setActualizadoEn(Instant.now());
        return toDocenteDto(cuerpoAcademicoRepository.save(docente));
    }

    private PlanEstudioMaestroDto toPlanDto(PlanEstudioFt001JpaEntity p) {
        return new PlanEstudioMaestroDto(
                p.getId(),
                p.getCarrera() != null ? p.getCarrera().getId() : null,
                p.getCarrera() != null ? p.getCarrera().getNombre() : null,
                p.getCarrera() != null ? p.getCarrera().getSigla() : null,
                p.getNombrePlan(),
                p.getEstadoVigencia(),
                p.getEsMaestro(),
                p.getCodigoResolucion(),
                p.getAnioAprobacion(),
                p.getAnioInicio(),
                p.getDuracionAnios(),
                p.getHorasPresenciales(),
                p.getHorasNoPresenciales(),
                p.getHorasTotales(),
                p.getHorasOptativasMinimas(),
                p.getActivo(),
                p.getAsignaturas() != null ? p.getAsignaturas().size() : 0,
                p.getCreadoEn(),
                p.getActualizadoEn(),
                p.getEstandar() != null ? p.getEstandar().getId() : null,
                p.getEstandar() != null ? p.getEstandar().getNombre() : null,
                p.getEstandar() != null ? p.getEstandar().getResolucionMinisterial() : null
        );
    }

    private CuerpoAcademicoMaestroDto toDocenteDto(CuerpoAcademicoMaestroJpaEntity d) {
        List<AsignacionCatedraDto> asigs = d.getAsignaciones() != null ? d.getAsignaciones().stream().map(a -> new AsignacionCatedraDto(
                a.getId(),
                a.getCarreraId(),
                a.getPlanEstudio() != null && a.getPlanEstudio().getCarrera() != null ? a.getPlanEstudio().getCarrera().getNombre() : null,
                a.getPlanEstudio() != null ? a.getPlanEstudio().getId() : null,
                a.getPlanEstudio() != null ? a.getPlanEstudio().getNombrePlan() : null,
                a.getActividadCurricular() != null ? a.getActividadCurricular().getId() : null,
                a.getActividadCurricular() != null ? a.getActividadCurricular().getNombre() : null,
                a.getRolEnCatedra(),
                a.getCargaHorariaSemanal()
        )).collect(Collectors.toList()) : List.of();

        return new CuerpoAcademicoMaestroDto(
                d.getId(),
                d.getDni(),
                d.getCuit(),
                d.getApellido(),
                d.getNombre(),
                d.getEmailInstitucional(),
                d.getEmailPersonal(),
                d.getTelefono(),
                d.getTituloGrado(),
                d.getMaximoTituloPosgrado(),
                d.getCategoriaDocente(),
                d.getCaracterDesignacion(),
                d.getDedicacionDocente(),
                d.getHorasSemanales(),
                d.getActivo(),
                asigs,
                d.getCreadoEn(),
                d.getActualizadoEn()
        );
    }
}
