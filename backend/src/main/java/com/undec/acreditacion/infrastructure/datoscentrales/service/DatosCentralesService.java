package com.undec.acreditacion.infrastructure.datoscentrales.service;

import com.undec.acreditacion.domain.entities.CodigosRol;
import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.*;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoActividadMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.designacion.persistence.DepartamentoAcademicoJpaEntity;
import com.undec.acreditacion.infrastructure.designacion.persistence.SpringDataDepartamentoAcademicoRepository;
import com.undec.acreditacion.infrastructure.estandares.persistence.EstandarJpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataEstandarRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DatosCentralesService {

    private static final Set<String> ROLES_VISION_INSTITUCIONAL = Set.of(
            CodigosRol.AEA,
            CodigosRol.SECRETARIA_ACADEMICA,
            CodigosRol.CONSULTA_INSTITUCIONAL,
            CodigosRol.DIRECTOR_ESCUELA,
            CodigosRol.DIRECTOR_CARRERA
    );

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository;
    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataAmbitoPracticaRepository ambitoPracticaRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;
    private final SpringDataEstandarRepository estandarRepository;
    private final SpringDataDepartamentoAcademicoRepository departamentoRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DatosCentralesService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository) {
        this(planRepository, cuerpoAcademicoRepository, espacioFisicoRepository,
                ambitoPracticaRepository, carreraRepository, actividadRepository, null, null, null, null);
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
                sincronizacionBidireccionalPlanService, null, null, null);
    }

    public DatosCentralesService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            SpringDataEstandarRepository estandarRepository) {
        this(planRepository, cuerpoAcademicoRepository, espacioFisicoRepository,
                ambitoPracticaRepository, carreraRepository, actividadRepository,
                sincronizacionBidireccionalPlanService, estandarRepository, null, null);
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
            SpringDataEstandarRepository estandarRepository,
            SpringDataDepartamentoAcademicoRepository departamentoRepository,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.planRepository = Objects.requireNonNull(planRepository);
        this.cuerpoAcademicoRepository = Objects.requireNonNull(cuerpoAcademicoRepository);
        this.espacioFisicoRepository = Objects.requireNonNull(espacioFisicoRepository);
        this.ambitoPracticaRepository = Objects.requireNonNull(ambitoPracticaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
        this.estandarRepository = estandarRepository;
        this.departamentoRepository = departamentoRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
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
        if (req.codigoResolucion() != null) {
            plan.setCodigoResolucion(req.codigoResolucion());
        }
        plan.setAnioAprobacion(req.anioAprobacion());
        if (req.anioInicio() != null) {
            plan.setAnioInicio(req.anioInicio());
        }
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
        return listarCuerpoAcademico(carreraId, null, false, query);
    }

    @Transactional(readOnly = true)
    public List<CuerpoAcademicoMaestroDto> listarCuerpoAcademico(
            UUID carreraId,
            UUID departamentoAcademicoId,
            boolean incluirInactivos,
            String query
    ) {
        UUID deptoRestringido = obtenerDepartamentoRestringidoDirectorOrNull();
        if (carreraId == null && deptoRestringido != null) {
            departamentoAcademicoId = deptoRestringido;
        }

        List<CuerpoAcademicoMaestroJpaEntity> base;
        if (carreraId != null) {
            base = cuerpoAcademicoRepository.findByCarreraId(carreraId);
        } else if (incluirInactivos) {
            base = cuerpoAcademicoRepository.findAll();
        } else if (query != null && !query.isBlank() && departamentoAcademicoId == null) {
            base = cuerpoAcademicoRepository.buscarPorTexto(query.trim());
        } else {
            base = cuerpoAcademicoRepository.findByActivoTrue();
        }

        final UUID filtroDepto = departamentoAcademicoId;
        final String q = (query != null && !query.isBlank()) ? query.trim().toLowerCase(Locale.ROOT) : null;

        return base.stream()
                .filter(d -> incluirInactivos || Boolean.TRUE.equals(d.getActivo()))
                .filter(d -> filtroDepto == null
                        || (d.getDepartamentoAcademico() != null && filtroDepto.equals(d.getDepartamentoAcademico().getId())))
                .filter(d -> q == null
                        || containsIgnoreCase(d.getApellido(), q)
                        || containsIgnoreCase(d.getNombre(), q)
                        || containsIgnoreCase(d.getDni(), q)
                        || containsIgnoreCase(d.getLegajo(), q)
                        || containsIgnoreCase(d.getNroDesignacionMapuche(), q))
                .sorted(Comparator
                        .comparing((CuerpoAcademicoMaestroJpaEntity d) -> d.getApellido() != null ? d.getApellido().toLowerCase(Locale.ROOT) : "")
                        .thenComparing(d -> d.getNombre() != null ? d.getNombre().toLowerCase(Locale.ROOT) : ""))
                .map(this::toDocenteDto)
                .collect(Collectors.toList());
    }

    public CuerpoAcademicoMaestroDto crearCuerpoAcademicoMaestro(CrearCuerpoAcademicoMaestroRequest req) {
        if (req == null) {
            throw new DomainValidationException("La solicitud de registro de docente es requerida");
        }
        if (req.dni() == null || req.dni().isBlank()) {
            throw new DomainValidationException("El número de documento es obligatorio");
        }
        if (req.apellido() == null || req.apellido().isBlank()) {
            throw new DomainValidationException("El apellido es obligatorio");
        }
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new DomainValidationException("El nombre es obligatorio");
        }
        if (req.categoriaDocente() == null || req.categoriaDocente().isBlank()) {
            throw new DomainValidationException("El cargo/categoría docente es obligatorio");
        }
        if (req.caracterDesignacion() == null || req.caracterDesignacion().isBlank()) {
            throw new DomainValidationException("El tipo/carácter de designación es obligatorio");
        }
        if (req.dedicacionDocente() == null || req.dedicacionDocente().isBlank()) {
            throw new DomainValidationException("La dedicación docente es obligatoria");
        }

        UUID deptoSolicitado = validarYResolverDepartamentoParaEscritura(req.departamentoAcademicoId());

        if (cuerpoAcademicoRepository.existsByDni(req.dni().trim())) {
            throw new DomainValidationException("Ya existe un docente con documento: " + req.dni());
        }

        String legajoNorm = normalizarLegajoOrNull(req.legajo());
        if (legajoNorm != null && cuerpoAcademicoRepository.findByLegajoIgnoreCase(legajoNorm).isPresent()) {
            throw new DomainValidationException("Ya existe un docente registrado con el legajo: " + legajoNorm);
        }

        DepartamentoAcademicoJpaEntity deptoEntity = resolverEntidadDepartamento(deptoSolicitado);

        CuerpoAcademicoMaestroJpaEntity docente = new CuerpoAcademicoMaestroJpaEntity();
        docente.setTipoDocumento(normalizarTipoDocumento(req.tipoDocumento()));
        docente.setDni(req.dni().trim());
        docente.setCuit(trimToNull(req.cuit()));
        docente.setLegajo(legajoNorm);
        docente.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        docente.setDepartamentoAcademico(deptoEntity);
        docente.setApellido(req.apellido().trim());
        docente.setNombre(req.nombre().trim());
        docente.setEmailInstitucional(trimToNull(req.emailInstitucional()));
        docente.setEmailPersonal(trimToNull(req.emailPersonal()));
        docente.setTelefono(trimToNull(req.telefono()));
        docente.setTituloGrado(trimToNull(req.tituloGrado()));
        docente.setMaximoTituloPosgrado(trimToNull(req.maximoTituloPosgrado()));
        docente.setCategoriaDocente(req.categoriaDocente().trim());
        docente.setCaracterDesignacion(req.caracterDesignacion().trim());
        docente.setDedicacionDocente(req.dedicacionDocente().trim());
        docente.setHorasSemanales(req.horasSemanales() != null ? req.horasSemanales() : 10);
        docente.setActivo(req.activo() == null || req.activo());
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

        validarAccesoDirectorSobreDocenteExistente(docente, req.departamentoAcademicoId());

        if (req.tipoDocumento() != null && !req.tipoDocumento().isBlank()) {
            docente.setTipoDocumento(normalizarTipoDocumento(req.tipoDocumento()));
        }
        if (req.dni() != null && !req.dni().isBlank()) {
            String nuevoDni = req.dni().trim();
            if (!nuevoDni.equals(docente.getDni()) && cuerpoAcademicoRepository.existsByDni(nuevoDni)) {
                throw new DomainValidationException("Ya existe otro docente con documento: " + nuevoDni);
            }
            docente.setDni(nuevoDni);
        }
        if (req.cuit() != null) {
            docente.setCuit(trimToNull(req.cuit()));
        }
        if (req.legajo() != null) {
            String legajoNorm = normalizarLegajoOrNull(req.legajo());
            if (legajoNorm != null) {
                cuerpoAcademicoRepository.findByLegajoIgnoreCase(legajoNorm).ifPresent(otro -> {
                    if (!id.equals(otro.getId())) {
                        throw new DomainValidationException("Ya existe otro docente registrado con el legajo: " + legajoNorm);
                    }
                });
            }
            docente.setLegajo(legajoNorm);
        }
        if (req.nroDesignacionMapuche() != null) {
            docente.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        }
        if (req.departamentoAcademicoId() != null && departamentoRepository != null) {
            DepartamentoAcademicoJpaEntity nuevoDepto = departamentoRepository.findById(req.departamentoAcademicoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el Departamento Académico ID: " + req.departamentoAcademicoId()));
            docente.setDepartamentoAcademico(nuevoDepto);
        }
        if (req.apellido() != null && !req.apellido().isBlank()) docente.setApellido(req.apellido().trim());
        if (req.nombre() != null && !req.nombre().isBlank()) docente.setNombre(req.nombre().trim());
        if (req.emailInstitucional() != null) docente.setEmailInstitucional(trimToNull(req.emailInstitucional()));
        if (req.emailPersonal() != null) docente.setEmailPersonal(trimToNull(req.emailPersonal()));
        if (req.telefono() != null) docente.setTelefono(trimToNull(req.telefono()));
        if (req.tituloGrado() != null) docente.setTituloGrado(trimToNull(req.tituloGrado()));
        if (req.maximoTituloPosgrado() != null) docente.setMaximoTituloPosgrado(trimToNull(req.maximoTituloPosgrado()));
        if (req.categoriaDocente() != null && !req.categoriaDocente().isBlank()) docente.setCategoriaDocente(req.categoriaDocente().trim());
        if (req.caracterDesignacion() != null && !req.caracterDesignacion().isBlank()) docente.setCaracterDesignacion(req.caracterDesignacion().trim());
        if (req.dedicacionDocente() != null && !req.dedicacionDocente().isBlank()) docente.setDedicacionDocente(req.dedicacionDocente().trim());
        if (req.horasSemanales() != null) docente.setHorasSemanales(req.horasSemanales());
        if (req.activo() != null) docente.setActivo(req.activo());
        docente.setActualizadoEn(Instant.now());

        return toDocenteDto(cuerpoAcademicoRepository.save(docente));
    }

    public CuerpoAcademicoMaestroDto agregarAsignacionDocente(
            UUID docenteId,
            CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem item
    ) {
        CuerpoAcademicoMaestroJpaEntity docente = cuerpoAcademicoRepository.findById(docenteId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente con ID: " + docenteId));
        validarAccesoDirectorSobreDocenteExistente(docente, null);

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
        validarAccesoDirectorSobreDocenteExistente(docente, null);

        if (docente.getAsignaciones() != null) {
            docente.getAsignaciones().removeIf(a -> asignacionId.equals(a.getId()));
        }
        docente.setActualizadoEn(Instant.now());
        return toDocenteDto(cuerpoAcademicoRepository.save(docente));
    }

    private UUID obtenerDepartamentoRestringidoDirectorOrNull() {
        if (authenticatedUserProvider == null) {
            return null;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean tieneVisionInstitucional = codes.stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
        if (!tieneVisionInstitucional && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            if (user.getDepartamentoAcademicoId() == null) {
                throw new DomainValidationException(
                        "Su usuario Director de Departamento aún no tiene asignado un Departamento Académico.");
            }
            return user.getDepartamentoAcademicoId();
        }
        return null;
    }

    private UUID validarYResolverDepartamentoParaEscritura(UUID requestedDeptoId) {
        if (authenticatedUserProvider != null) {
            Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
                boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
                if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
                    UUID miDepto = user.getDepartamentoAcademicoId();
                    if (miDepto == null) {
                        throw new DomainValidationException(
                                "Su usuario Director de Departamento no tiene asignado un Departamento Académico.");
                    }
                    if (requestedDeptoId != null && !miDepto.equals(requestedDeptoId)) {
                        throw new DomainValidationException(
                                "Un Director de Departamento solo puede registrar docentes en su propio Departamento Académico.");
                    }
                    return miDepto;
                }
            }
        }
        return requestedDeptoId;
    }

    private void validarAccesoDirectorSobreDocenteExistente(
            CuerpoAcademicoMaestroJpaEntity docente,
            UUID nuevoDeptoId
    ) {
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
            UUID deptoActualDocente = docente.getDepartamentoAcademico() != null
                    ? docente.getDepartamentoAcademico().getId()
                    : null;
            if (miDepto == null || !miDepto.equals(deptoActualDocente)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede modificar docentes pertenecientes a su propio Departamento Académico.");
            }
            if (nuevoDeptoId != null && !miDepto.equals(nuevoDeptoId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento no puede transferir docentes a otro Departamento Académico.");
            }
        }
    }

    private DepartamentoAcademicoJpaEntity resolverEntidadDepartamento(UUID departamentoId) {
        if (departamentoRepository == null) {
            return null;
        }
        if (departamentoId != null) {
            return departamentoRepository.findById(departamentoId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el Departamento Académico ID: " + departamentoId));
        }
        return departamentoRepository.findByCodigoIgnoreCase("SIN_ASIGNAR")
                .orElseThrow(() -> new DomainValidationException("El Departamento Académico es obligatorio (RN01)."));
    }

    private static String normalizarTipoDocumento(String raw) {
        if (raw == null || raw.isBlank()) {
            return "DNI";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarLegajoOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean containsIgnoreCase(String field, String lowerQuery) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(lowerQuery);
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

        DepartamentoAcademicoJpaEntity depto = d.getDepartamentoAcademico();

        return new CuerpoAcademicoMaestroDto(
                d.getId(),
                d.getTipoDocumento() != null ? d.getTipoDocumento() : "DNI",
                d.getDni(),
                d.getCuit(),
                d.getLegajo(),
                d.getNroDesignacionMapuche(),
                depto != null ? depto.getId() : null,
                depto != null ? depto.getCodigo() : null,
                depto != null ? depto.getDenominacion() : null,
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
