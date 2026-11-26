package com.undec.acreditacion.infrastructure.designacion.service;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SolicitudDesignacionService {

    public static final int TOPE_HORAS_SEMANALES_DOCENTE = 50;

    private static final Set<String> ROLES_VISION_INSTITUCIONAL = Set.of(
            CodigosRol.AEA,
            CodigosRol.SECRETARIA_ACADEMICA,
            CodigosRol.CONSULTA_INSTITUCIONAL,
            CodigosRol.DIRECTOR_ESCUELA,
            CodigosRol.DIRECTOR_CARRERA
    );

    private final SpringDataSolicitudDesignacionRepository solicitudRepository;
    private final SpringDataSolicitudDocenteDesignacionRepository designacionRepository;
    private final SpringDataSolicitudAsignacionCurricularRepository asignacionRepository;
    private final SpringDataDepartamentoAcademicoRepository departamentoRepository;
    private final SpringDataPeriodoAcademicoRepository periodoRepository;
    private final SpringDataCargoDocenteRepository cargoRepository;
    private final SpringDataDedicacionDocenteRepository dedicacionRepository;
    private final SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository;
    private final SpringDataCuerpoAcademicoMaestroRepository docenteRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public SolicitudDesignacionService(
            SpringDataSolicitudDesignacionRepository solicitudRepository,
            SpringDataSolicitudDocenteDesignacionRepository designacionRepository,
            SpringDataSolicitudAsignacionCurricularRepository asignacionRepository,
            SpringDataDepartamentoAcademicoRepository departamentoRepository,
            SpringDataPeriodoAcademicoRepository periodoRepository,
            SpringDataCargoDocenteRepository cargoRepository,
            SpringDataDedicacionDocenteRepository dedicacionRepository,
            SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository,
            SpringDataCuerpoAcademicoMaestroRepository docenteRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.designacionRepository = Objects.requireNonNull(designacionRepository);
        this.asignacionRepository = Objects.requireNonNull(asignacionRepository);
        this.departamentoRepository = Objects.requireNonNull(departamentoRepository);
        this.periodoRepository = Objects.requireNonNull(periodoRepository);
        this.cargoRepository = Objects.requireNonNull(cargoRepository);
        this.dedicacionRepository = Objects.requireNonNull(dedicacionRepository);
        this.tipoDesignacionRepository = Objects.requireNonNull(tipoDesignacionRepository);
        this.docenteRepository = Objects.requireNonNull(docenteRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SolicitudDesignacionResumenDto> listarSolicitudes(UUID periodoId, UUID departamentoId) {
        UUID deptoEfectivo = resolverDepartamentoLectura(departamentoId);
        List<SolicitudDesignacionJpaEntity> solicitudes;
        if (deptoEfectivo != null && periodoId != null) {
            solicitudes = solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdOrderByCreadoEnDesc(
                    deptoEfectivo, periodoId);
        } else if (deptoEfectivo != null) {
            solicitudes = solicitudRepository.findByDepartamentoAcademicoIdOrderByCreadoEnDesc(deptoEfectivo);
        } else if (periodoId != null) {
            solicitudes = solicitudRepository.findByPeriodoAcademicoIdOrderByCreadoEnDesc(periodoId);
        } else {
            solicitudes = solicitudRepository.findAllByOrderByCreadoEnDesc();
        }
        return solicitudes.stream().map(this::toResumenDto).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDesignacionDetalleDto obtenerDetalleSolicitud(UUID solicitudId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarAccesoLecturaDepartamento(solicitud.getDepartamentoAcademico().getId());
        return toDetalleDto(solicitud);
    }

    public SolicitudDesignacionDetalleDto crearSolicitud(CrearSolicitudDesignacionRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la solicitud son obligatorios.");
        }
        if (req.periodoAcademicoId() == null) {
            throw new DomainValidationException("El Período Académico es obligatorio.");
        }
        UUID deptoId = resolverDepartamentoEscritura(req.departamentoAcademicoId());
        if (deptoId == null) {
            throw new DomainValidationException("El Departamento Académico es obligatorio.");
        }

        DepartamentoAcademicoJpaEntity depto = departamentoRepository.findById(deptoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Departamento Académico ID: " + deptoId));
        PeriodoAcademicoJpaEntity periodo = periodoRepository.findById(req.periodoAcademicoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Período Académico ID: " + req.periodoAcademicoId()));

        TipoSolicitudDesignacion tipo = req.tipoSolicitud() != null ? req.tipoSolicitud() : TipoSolicitudDesignacion.ANUAL;
        if (tipo == TipoSolicitudDesignacion.ANUAL) {
            boolean yaExiste = solicitudRepository
                    .findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(depto.getId(), periodo.getId(), TipoSolicitudDesignacion.ANUAL)
                    .isPresent();
            if (yaExiste) {
                throw new DomainValidationException(
                        "Ya existe una solicitud ANUAL para el departamento '" + depto.getDenominacion()
                                + "' en el período '" + periodo.getDenominacion() + "'.");
            }
        }

        SolicitudDesignacionJpaEntity entity = new SolicitudDesignacionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDepartamentoAcademico(depto);
        entity.setPeriodoAcademico(periodo);
        entity.setTipoSolicitud(tipo);
        entity.setEstado(EstadoSolicitudDesignacion.BORRADOR);
        entity.setObservacionesDepartamento(trimToNull(req.observacionesDepartamento()));
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        SolicitudDesignacionJpaEntity saved = solicitudRepository.save(entity);
        return toDetalleDto(saved);
    }

    public DesignacionDocenteItemDto agregarDesignacion(UUID solicitudId, GuardarDesignacionDocenteRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la designación son obligatorios.");
        }
        if (req.docenteId() == null || req.cargoDocenteId() == null
                || req.dedicacionDocenteId() == null || req.tipoDesignacionId() == null) {
            throw new DomainValidationException("Docente, Cargo, Dedicación y Tipo de Designación son obligatorios.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);

        CuerpoAcademicoMaestroJpaEntity docente = docenteRepository.findById(req.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente ID: " + req.docenteId()));
        CargoDocenteJpaEntity cargo = cargoRepository.findById(req.cargoDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cargo docente ID: " + req.cargoDocenteId()));
        DedicacionDocenteJpaEntity dedicacion = dedicacionRepository.findById(req.dedicacionDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la dedicación docente ID: " + req.dedicacionDocenteId()));
        TipoDesignacionDocenteJpaEntity tipoDesig = tipoDesignacionRepository.findById(req.tipoDesignacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de designación ID: " + req.tipoDesignacionId()));

        int horasNuevas = dedicacion.getHorasSemanales() != null ? dedicacion.getHorasSemanales() : 0;
        int horasPreviasPeriodo = sumarHorasActivasDocenteEnPeriodo(
                docente.getId(), solicitud.getPeriodoAcademico().getId(), null);
        validarTope50Horas(docente, horasPreviasPeriodo, horasNuevas);

        SolicitudDocenteDesignacionJpaEntity entity = new SolicitudDocenteDesignacionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setSolicitud(solicitud);
        entity.setDocente(docente);
        entity.setCargoDocente(cargo);
        entity.setDedicacionDocente(dedicacion);
        entity.setTipoDesignacion(tipoDesig);
        entity.setHorasDedicacion(horasNuevas);
        entity.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        entity.setEstadoItem(EstadoItemDesignacion.NUEVO);
        entity.setObservaciones(trimToNull(req.observaciones()));
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        int totalDocentePeriodo = horasPreviasPeriodo + horasNuevas;
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public DesignacionDocenteItemDto actualizarDesignacion(
            UUID solicitudId,
            UUID designacionId,
            GuardarDesignacionDocenteRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la designación son obligatorios.");
        }
        if (req.cargoDocenteId() == null || req.dedicacionDocenteId() == null || req.tipoDesignacionId() == null) {
            throw new DomainValidationException("Cargo, Dedicación y Tipo de Designación son obligatorios.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);

        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        CargoDocenteJpaEntity cargo = cargoRepository.findById(req.cargoDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cargo docente ID: " + req.cargoDocenteId()));
        DedicacionDocenteJpaEntity dedicacion = dedicacionRepository.findById(req.dedicacionDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la dedicación docente ID: " + req.dedicacionDocenteId()));
        TipoDesignacionDocenteJpaEntity tipoDesig = tipoDesignacionRepository.findById(req.tipoDesignacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de designación ID: " + req.tipoDesignacionId()));

        int horasNuevas = dedicacion.getHorasSemanales() != null ? dedicacion.getHorasSemanales() : 0;
        if (entity.totalHorasDistribuidas() > horasNuevas) {
            throw new DomainValidationException(
                    "No se puede reducir la dedicación a " + horasNuevas + " hs semanales porque ya hay "
                            + entity.totalHorasDistribuidas() + " hs distribuidas en actividades. Ajuste la distribución horaria primero (RN06).");
        }

        int horasOtrasDesignaciones = sumarHorasActivasDocenteEnPeriodo(
                entity.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), entity.getId());
        validarTope50Horas(entity.getDocente(), horasOtrasDesignaciones, horasNuevas);

        entity.setCargoDocente(cargo);
        entity.setDedicacionDocente(dedicacion);
        entity.setTipoDesignacion(tipoDesig);
        entity.setHorasDedicacion(horasNuevas);
        entity.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        entity.setObservaciones(trimToNull(req.observaciones()));
        if (entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            entity.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
        }
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        int totalDocentePeriodo = horasOtrasDesignaciones + horasNuevas;
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public void eliminarDesignacion(UUID solicitudId, UUID designacionId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        designacionRepository.delete(entity);
        solicitud.setActualizadoEn(Instant.now());
    }

    public DesignacionDocenteItemDto actualizarDistribucionHoraria(
            UUID solicitudId,
            UUID designacionId,
            GuardarDistribucionHorariaRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("La distribución horaria es obligatoria.");
        }
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);

        int clases = validarHoraNoNegativa(req.horasClases(), "Desarrollo de clases");
        int inv = validarHoraNoNegativa(req.horasInvestigacion(), "Investigación");
        int ext = validarHoraNoNegativa(req.horasExtension(), "Extensión");
        int gest = validarHoraNoNegativa(req.horasGestion(), "Gestión");
        int tesis = validarHoraNoNegativa(req.horasDireccionTesis(), "Dirección de tesis/TF");
        int otras = validarHoraNoNegativa(req.horasOtras(), "Otras actividades académicas");

        int total = clases + inv + ext + gest + tesis + otras;
        int topeDedicacion = entity.getHorasDedicacion() != null ? entity.getHorasDedicacion() : 0;
        if (total > topeDedicacion) {
            throw new DomainValidationException(
                    "La suma de horas distribuidas (" + total + " hs) supera la carga horaria de la dedicación ("
                            + topeDedicacion + " hs semanales) (RN06).");
        }

        entity.setHorasClases(clases);
        entity.setHorasInvestigacion(inv);
        entity.setHorasExtension(ext);
        entity.setHorasGestion(gest);
        entity.setHorasDireccionTesis(tesis);
        entity.setHorasOtras(otras);
        if (entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            entity.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
        }
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        int totalDocentePeriodo = sumarHorasActivasDocenteEnPeriodo(
                saved.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), null);
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public AsignacionCurricularPeriodoDto agregarAsignacionCurricular(
            UUID solicitudId,
            UUID designacionId,
            AgregarAsignacionCurricularRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la asignación curricular son obligatorios.");
        }
        if (req.carreraId() == null || req.planEstudioId() == null || req.actividadCurricularId() == null) {
            throw new DomainValidationException("Carrera, Plan de Estudio y Asignatura son obligatorios.");
        }
        if (req.horasAsignadas() != null && req.horasAsignadas() < 0) {
            throw new DomainValidationException("Las horas asignadas a la asignatura no pueden ser negativas.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity designacion = obtenerDesignacionEnSolicitud(solicitudId, designacionId);

        CarreraJpaEntity carrera = carreraRepository.findById(req.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + req.carreraId()));
        PlanEstudioFt001JpaEntity plan = planRepository.findById(req.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + req.planEstudioId()));
        ActividadCurricularPlanJpaEntity actividad = actividadRepository.findById(req.actividadCurricularId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura ID: " + req.actividadCurricularId()));

        if (plan.getCarrera() != null && !plan.getCarrera().getId().equals(carrera.getId())) {
            throw new DomainValidationException("El plan de estudio seleccionado no pertenece a la carrera indicada.");
        }
        if (actividad.getPlanEstudio() != null && !actividad.getPlanEstudio().getId().equals(plan.getId())) {
            throw new DomainValidationException("La asignatura seleccionada no pertenece al plan de estudio indicado.");
        }

        ModalidadParticipacionDocente modalidad = req.modalidadParticipacion() != null
                ? req.modalidadParticipacion()
                : ModalidadParticipacionDocente.DESARROLLO_CLASES;

        validarCoherenciaRN09(actividad, modalidad);

        boolean duplicada = asignacionRepository.existsByDesignacionIdAndActividadCurricularIdAndModalidadParticipacion(
                designacion.getId(), actividad.getId(), modalidad);
        if (duplicada) {
            throw new DomainValidationException(
                    "El docente ya tiene asignada la asignatura '" + actividad.getNombre()
                            + "' con modalidad " + modalidad + " en esta designación.");
        }

        SolicitudAsignacionCurricularJpaEntity entity = new SolicitudAsignacionCurricularJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDesignacion(designacion);
        entity.setCarrera(carrera);
        entity.setPlanEstudio(plan);
        entity.setActividadCurricular(actividad);
        entity.setModalidadParticipacion(modalidad);
        entity.setHorasAsignadas(req.horasAsignadas());
        entity.setObservaciones(trimToNull(req.observaciones()));
        entity.setCreadoEn(Instant.now());

        SolicitudAsignacionCurricularJpaEntity saved = asignacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());
        return toAsignacionDto(saved);
    }

    public void eliminarAsignacionCurricular(UUID solicitudId, UUID designacionId, UUID asignacionId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity designacion = obtenerDesignacionEnSolicitud(solicitudId, designacionId);

        SolicitudAsignacionCurricularJpaEntity asig = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignación curricular ID: " + asignacionId));
        if (!asig.getDesignacion().getId().equals(designacion.getId())) {
            throw new ResourceNotFoundException("La asignación curricular no pertenece a la designación indicada.");
        }
        asignacionRepository.delete(asig);
        solicitud.setActualizadoEn(Instant.now());
    }

    // --- Helpers de Reglas de Negocio y Seguridad ---

    private void validarCoherenciaRN09(
            ActividadCurricularPlanJpaEntity actividad,
            ModalidadParticipacionDocente modalidad
    ) {
        EstadoDictadoActividad estado = actividad.getEstadoDictado() != null
                ? actividad.getEstadoDictado()
                : EstadoDictadoActividad.ACTIVA;

        if (estado.esInactiva()) {
            throw new DomainValidationException(
                    "No se puede asignar la asignatura '" + actividad.getNombre()
                            + "' porque se encuentra en estado INACTIVA (RN09).");
        }
        if (estado == EstadoDictadoActividad.SOLO_EXAMENES_FINALES
                && modalidad == ModalidadParticipacionDocente.DESARROLLO_CLASES) {
            throw new DomainValidationException(
                    "La asignatura '" + actividad.getNombre()
                            + "' está habilitada únicamente para SOLO EXÁMENES FINALES; no se permite asignar desarrollo de clases (RN09).");
        }
    }

    private void validarTope50Horas(
            CuerpoAcademicoMaestroJpaEntity docente,
            int horasPreviasPeriodo,
            int horasNuevas
    ) {
        int total = horasPreviasPeriodo + horasNuevas;
        if (total > TOPE_HORAS_SEMANALES_DOCENTE) {
            throw new DomainValidationException(
                    "El docente " + docente.getApellido() + ", " + docente.getNombre()
                            + " superaría el tope institucional de " + TOPE_HORAS_SEMANALES_DOCENTE
                            + " hs semanales en el período (acumuladas: " + horasPreviasPeriodo
                            + " hs + nuevas: " + horasNuevas + " hs = " + total + " hs) (RN02).");
        }
    }

    private int sumarHorasActivasDocenteEnPeriodo(UUID docenteId, UUID periodoId, UUID excluirDesignacionId) {
        List<SolicitudDocenteDesignacionJpaEntity> activas =
                designacionRepository.findActivasByDocenteAndPeriodo(docenteId, periodoId);
        int sum = 0;
        for (SolicitudDocenteDesignacionJpaEntity d : activas) {
            if (excluirDesignacionId != null && excluirDesignacionId.equals(d.getId())) {
                continue;
            }
            if (d.getHorasDedicacion() != null) {
                sum += d.getHorasDedicacion();
            }
        }
        return sum;
    }

    private SolicitudDesignacionJpaEntity obtenerSolicitudOCascada(UUID solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de designación ID: " + solicitudId));
    }

    private SolicitudDocenteDesignacionJpaEntity obtenerDesignacionEnSolicitud(UUID solicitudId, UUID designacionId) {
        SolicitudDocenteDesignacionJpaEntity entity = designacionRepository.findById(designacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la designación docente ID: " + designacionId));
        if (!entity.getSolicitud().getId().equals(solicitudId)) {
            throw new ResourceNotFoundException("La designación no pertenece a la solicitud indicada.");
        }
        return entity;
    }

    private void validarEscrituraSobreSolicitud(SolicitudDesignacionJpaEntity solicitud) {
        validarAccesoEscrituraDepartamento(solicitud.getDepartamentoAcademico().getId());
        if (!esEditable(solicitud.getEstado())) {
            throw new DomainValidationException(
                    "La solicitud se encuentra en estado " + solicitud.getEstado()
                            + " y no admite modificaciones. Solo se pueden editar solicitudes en estado BORRADOR u OBSERVADA.");
        }
    }

    private static boolean esEditable(EstadoSolicitudDesignacion estado) {
        return estado == EstadoSolicitudDesignacion.BORRADOR || estado == EstadoSolicitudDesignacion.OBSERVADA;
    }

    private UUID resolverDepartamentoLectura(UUID requestedDeptoId) {
        if (authenticatedUserProvider == null) {
            return requestedDeptoId;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return requestedDeptoId;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean tieneVisionInstitucional = codes.stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
        if (!tieneVisionInstitucional && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null) {
                throw new DomainValidationException(
                        "Su usuario Director de Departamento no tiene un Departamento Académico asignado.");
            }
            return miDepto;
        }
        return requestedDeptoId;
    }

    private void validarAccesoLecturaDepartamento(UUID deptoSolicitudId) {
        if (authenticatedUserProvider == null) {
            return;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean tieneVisionInstitucional = codes.stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
        if (!tieneVisionInstitucional && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null || !miDepto.equals(deptoSolicitudId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede consultar solicitudes de su propio Departamento Académico (RNF01).");
            }
        }
    }

    private UUID resolverDepartamentoEscritura(UUID requestedDeptoId) {
        if (authenticatedUserProvider == null) {
            return requestedDeptoId;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return requestedDeptoId;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
        if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null) {
                throw new DomainValidationException(
                        "Su usuario Director de Departamento no tiene un Departamento Académico asignado.");
            }
            if (requestedDeptoId != null && !miDepto.equals(requestedDeptoId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede gestionar solicitudes de su propio Departamento Académico (RNF01).");
            }
            return miDepto;
        }
        return requestedDeptoId;
    }

    private void validarAccesoEscrituraDepartamento(UUID deptoSolicitudId) {
        if (authenticatedUserProvider == null) {
            return;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
        if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null || !miDepto.equals(deptoSolicitudId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede modificar solicitudes de su propio Departamento Académico (RNF01).");
            }
        }
    }

    private static int validarHoraNoNegativa(Integer valor, String etiqueta) {
        int v = valor != null ? valor : 0;
        if (v < 0) {
            throw new DomainValidationException("Las horas de '" + etiqueta + "' no pueden ser negativas.");
        }
        return v;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    // --- Mappers a DTOs inmutables dentro de @Transactional (OSIV=false safe) ---

    private SolicitudDesignacionResumenDto toResumenDto(SolicitudDesignacionJpaEntity s) {
        List<SolicitudDocenteDesignacionJpaEntity> items =
                designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(s.getId());
        long activas = items.stream().filter(i -> i.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO).count();
        long conAlerta = items.stream()
                .filter(i -> i.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO)
                .filter(i -> i.totalHorasDistribuidas() < (i.getHorasDedicacion() != null ? i.getHorasDedicacion() : 0))
                .count();

        DepartamentoAcademicoJpaEntity depto = s.getDepartamentoAcademico();
        PeriodoAcademicoJpaEntity periodo = s.getPeriodoAcademico();
        return new SolicitudDesignacionResumenDto(
                s.getId(),
                depto.getId(),
                depto.getCodigo(),
                depto.getDenominacion(),
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getDenominacion(),
                periodo.getAnio(),
                s.getTipoSolicitud(),
                s.getEstado(),
                esEditable(s.getEstado()),
                activas,
                conAlerta,
                s.getObservacionesDepartamento(),
                s.getObservacionesSga(),
                s.getFechaEmision(),
                s.getCreadoEn(),
                s.getActualizadoEn()
        );
    }

    private SolicitudDesignacionDetalleDto toDetalleDto(SolicitudDesignacionJpaEntity s) {
        List<SolicitudDocenteDesignacionJpaEntity> items =
                designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(s.getId());

        List<UUID> itemIds = items.stream().map(SolicitudDocenteDesignacionJpaEntity::getId).toList();
        Map<UUID, List<SolicitudAsignacionCurricularJpaEntity>> asigsPorDesignacion = itemIds.isEmpty()
                ? Map.of()
                : asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(itemIds).stream()
                        .collect(Collectors.groupingBy(a -> a.getDesignacion().getId()));

        UUID periodoId = s.getPeriodoAcademico().getId();
        Map<UUID, Integer> totalHorasPorDocente = new HashMap<>();
        for (SolicitudDocenteDesignacionJpaEntity item : items) {
            UUID docId = item.getDocente().getId();
            totalHorasPorDocente.computeIfAbsent(docId,
                    id -> sumarHorasActivasDocenteEnPeriodo(id, periodoId, null));
        }

        List<DesignacionDocenteItemDto> designacionDtos = items.stream()
                .map(i -> toDesignacionDto(
                        i,
                        asigsPorDesignacion.getOrDefault(i.getId(), List.of()),
                        totalHorasPorDocente.getOrDefault(i.getDocente().getId(), 0)))
                .toList();

        DepartamentoAcademicoJpaEntity depto = s.getDepartamentoAcademico();
        PeriodoAcademicoJpaEntity periodo = s.getPeriodoAcademico();

        return new SolicitudDesignacionDetalleDto(
                s.getId(),
                depto.getId(),
                depto.getCodigo(),
                depto.getDenominacion(),
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getDenominacion(),
                periodo.getAnio(),
                s.getTipoSolicitud(),
                s.getEstado(),
                esEditable(s.getEstado()),
                s.getObservacionesDepartamento(),
                s.getObservacionesSga(),
                s.getFechaEmision(),
                s.getCreadoEn(),
                s.getActualizadoEn(),
                designacionDtos
        );
    }

    private DesignacionDocenteItemDto toDesignacionDto(
            SolicitudDocenteDesignacionJpaEntity d,
            List<SolicitudAsignacionCurricularJpaEntity> asignaciones,
            int totalHorasDocentePeriodo
    ) {
        int dedHoras = d.getHorasDedicacion() != null ? d.getHorasDedicacion() : 0;
        int distribuidas = d.totalHorasDistribuidas();
        int sinDistribuir = Math.max(0, dedHoras - distribuidas);
        boolean alerta = d.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO && sinDistribuir > 0;

        CuerpoAcademicoMaestroJpaEntity doc = d.getDocente();
        CargoDocenteJpaEntity cargo = d.getCargoDocente();
        DedicacionDocenteJpaEntity ded = d.getDedicacionDocente();
        TipoDesignacionDocenteJpaEntity tipo = d.getTipoDesignacion();

        return new DesignacionDocenteItemDto(
                d.getId(),
                d.getSolicitud().getId(),
                doc.getId(),
                doc.getDni(),
                doc.getLegajo(),
                doc.getApellido(),
                doc.getNombre(),
                cargo.getId(),
                cargo.getCodigo(),
                cargo.getNombre(),
                ded.getId(),
                ded.getCodigo(),
                ded.getNombre(),
                dedHoras,
                tipo.getId(),
                tipo.getCodigo(),
                tipo.getNombre(),
                d.getNroDesignacionMapuche(),
                d.getEstadoItem(),
                d.getHorasClases(),
                d.getHorasInvestigacion(),
                d.getHorasExtension(),
                d.getHorasGestion(),
                d.getHorasDireccionTesis(),
                d.getHorasOtras(),
                distribuidas,
                sinDistribuir,
                alerta,
                totalHorasDocentePeriodo,
                d.getObservaciones(),
                asignaciones.stream().map(this::toAsignacionDto).toList()
        );
    }

    private AsignacionCurricularPeriodoDto toAsignacionDto(SolicitudAsignacionCurricularJpaEntity a) {
        CarreraJpaEntity carrera = a.getCarrera();
        PlanEstudioFt001JpaEntity plan = a.getPlanEstudio();
        ActividadCurricularPlanJpaEntity act = a.getActividadCurricular();
        return new AsignacionCurricularPeriodoDto(
                a.getId(),
                carrera.getId(),
                carrera.getNombre(),
                carrera.getSigla(),
                plan.getId(),
                plan.getNombrePlan(),
                plan.getEstadoVigencia(),
                act.getId(),
                act.getCodigo(),
                act.getNombre(),
                act.getEstadoDictado(),
                act.getHorasSemanales(),
                a.getModalidadParticipacion(),
                a.getHorasAsignadas(),
                a.getObservaciones()
        );
    }
}
