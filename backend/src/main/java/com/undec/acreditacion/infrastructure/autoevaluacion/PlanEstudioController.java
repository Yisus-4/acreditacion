package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.EstadoActividad;
import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.PlanEstudioVigenteDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CrearAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.PlanEstudioFt001Dto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ActualizarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.CrearAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.EliminarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.estandares.dto.FichaEstandarActividadDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarFichaEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.persistence.EstandarJpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataEstandarRepository;
import com.undec.acreditacion.infrastructure.estandares.service.ActividadEstandarFichaService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planes-estudio")
public class PlanEstudioController {

    public record DesignarDocenteResponsableRequest(UUID docenteUsuarioId, String docenteNombre) {}

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataProcesoAcreditacionRepository procesoRepository;
    private final CrearAsignaturaPlanService crearAsignaturaPlanService;
    private final ActualizarAsignaturaPlanService actualizarAsignaturaPlanService;
    private final EliminarAsignaturaPlanService eliminarAsignaturaPlanService;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final SpringDataEstandarRepository estandarRepository;
    private final ActividadEstandarFichaService actividadEstandarFichaService;

    public PlanEstudioController(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            CrearAsignaturaPlanService crearAsignaturaPlanService,
            ActualizarAsignaturaPlanService actualizarAsignaturaPlanService,
            EliminarAsignaturaPlanService eliminarAsignaturaPlanService
    ) {
        this(planRepository, asignaturaRepository, carreraRepository, procesoRepository,
                crearAsignaturaPlanService, actualizarAsignaturaPlanService, eliminarAsignaturaPlanService,
                null, null, null, null, null);
    }

    public PlanEstudioController(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            CrearAsignaturaPlanService crearAsignaturaPlanService,
            ActualizarAsignaturaPlanService actualizarAsignaturaPlanService,
            EliminarAsignaturaPlanService eliminarAsignaturaPlanService,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this(planRepository, asignaturaRepository, carreraRepository, procesoRepository,
                crearAsignaturaPlanService, actualizarAsignaturaPlanService, eliminarAsignaturaPlanService,
                sincronizacionBidireccionalPlanService, planTrabajoRepository, authenticatedUserProvider,
                null, null);
    }

    @Autowired
    public PlanEstudioController(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            CrearAsignaturaPlanService crearAsignaturaPlanService,
            ActualizarAsignaturaPlanService actualizarAsignaturaPlanService,
            EliminarAsignaturaPlanService eliminarAsignaturaPlanService,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            SpringDataEstandarRepository estandarRepository,
            ActividadEstandarFichaService actividadEstandarFichaService
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.carreraRepository = carreraRepository;
        this.procesoRepository = procesoRepository;
        this.crearAsignaturaPlanService = crearAsignaturaPlanService;
        this.actualizarAsignaturaPlanService = actualizarAsignaturaPlanService;
        this.eliminarAsignaturaPlanService = eliminarAsignaturaPlanService;
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
        this.planTrabajoRepository = planTrabajoRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.estandarRepository = estandarRepository;
        this.actividadEstandarFichaService = actividadEstandarFichaService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlanEstudioFt001Dto>> listar(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) UUID procesoId
    ) {
        if (procesoId != null) {
            List<PlanEstudioFt001JpaEntity> planes = planRepository.findByProcesoIdAndActivoTrue(procesoId);
            if (carreraId != null && !planes.isEmpty()) {
                planes = planes.stream()
                        .filter(p -> p.getCarrera() != null && carreraId.equals(p.getCarrera().getId()))
                        .collect(Collectors.toList());
            }
            List<PlanEstudioFt001Dto> dtos = planes.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }
        if (carreraId != null) {
            return listarPorCarrera(carreraId);
        }
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findAll();
        List<PlanEstudioFt001Dto> dtos = planes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/carrera/{carreraId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlanEstudioFt001Dto>> listarPorCarrera(@PathVariable UUID carreraId) {
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId);
        List<PlanEstudioFt001Dto> dtos = planes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/carrera/{carreraId}/vigentes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlanEstudioVigenteDto>> listarVigentesPorCarrera(@PathVariable UUID carreraId) {
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(
                carreraId, EstadoVigenciaPlan.VIGENTE);
        List<PlanEstudioVigenteDto> dtos = planes.stream()
                .map(p -> new PlanEstudioVigenteDto(
                        p.getId(),
                        carreraId,
                        p.getCarrera() != null ? p.getCarrera().getNombre() : null,
                        p.getNombrePlan(),
                        p.getCodigoResolucion(),
                        p.getAnioAprobacion(),
                        p.getEstadoVigencia(),
                        p.getEsMaestro()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/estado-actividad-8-3")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> estadoActividad83(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID planEstudioId
    ) {
        PlanYProcesoResuelto resuelto = resolverPlanYProceso(procesoId, planEstudioId);
        EvaluacionCompletitud83 eval = evaluarCompletitud83(resuelto.planEntidad());
        boolean completada = resuelto.planTrabajoOpt()
                .flatMap(pt -> pt.getActividades().stream()
                        .filter(a -> "8.3".equals(a.getCodigoActividad()))
                        .findFirst())
                .map(a -> a.getEstado() == EstadoActividad.COMPLETADA)
                .orElse(false);

        return ResponseEntity.ok(Map.of(
                "completada", completada,
                "totalAsignaturas", eval.totalActivas(),
                "asignaturasCompletas", eval.asignaturasCompletas(),
                "todasCompletas", eval.todasCompletas()
        ));
    }

    @PostMapping("/validar-completar-8-3")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<Map<String, Object>> validarYCompletar83(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID planEstudioId
    ) {
        PlanYProcesoResuelto resuelto = resolverPlanYProceso(procesoId, planEstudioId);
        EvaluacionCompletitud83 eval = evaluarCompletitud83(resuelto.planEntidad());
        if (resuelto.planEntidad() == null || eval.totalActivas() == 0) {
            throw new DomainValidationException(
                    "No se puede completar la actividad 8.3: el plan de estudios no tiene actividades curriculares activas cargadas."
            );
        }
        if (actividadEstandarFichaService != null && !eval.todasCompletas()) {
            long faltantes = eval.totalActivas() - eval.asignaturasCompletas();
            throw new DomainValidationException(
                    "No se puede completar la actividad 8.3: faltan " + faltantes
                            + " actividad(es) curricular(es) con campos obligatorios y estándar CONEAU (Secciones 2.2 y 3) completos."
            );
        }

        if (resuelto.planTrabajoOpt().isPresent() && planTrabajoRepository != null) {
            Optional<ActividadPlanTrabajo> act83Opt = resuelto.planTrabajoOpt().get().getActividades().stream()
                    .filter(a -> "8.3".equals(a.getCodigoActividad()))
                    .findFirst();

            if (act83Opt.isPresent()) {
                ActividadPlanTrabajo act83 = act83Opt.get();
                if (act83.getEstado() != EstadoActividad.COMPLETADA) {
                    UUID usuarioId = authenticatedUserProvider != null
                            ? authenticatedUserProvider.getAuthenticatedUserIdOrNull()
                            : null;
                    act83.completar(
                            usuarioId,
                            "Plan de estudios FT-001 y " + eval.totalActivas()
                                    + " actividades curriculares verificadas y sincronizadas con Académica"
                    );
                    planTrabajoRepository.saveActividad(act83);
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "completada", true,
                "totalAsignaturas", eval.totalActivas(),
                "asignaturasCompletas", eval.asignaturasCompletas(),
                "mensaje", "Actividad 8.3 (Plan de Estudios FT-001) marcada como completada exitosamente."
        ));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PlanEstudioFt001Dto> obtenerPorId(@PathVariable UUID id) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + id));
        return ResponseEntity.ok(toDto(plan));
    }

    @GetMapping("/{id}/asignaturas")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ActividadCurricularPlanDto>> listarAsignaturas(@PathVariable UUID id) {
        List<ActividadCurricularPlanJpaEntity> asigs = asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(id);
        Map<UUID, ActividadEstandarFichaService.CompletitudEstado> completitud = Map.of();
        if (actividadEstandarFichaService != null && !asigs.isEmpty()) {
            PlanEstudioFt001JpaEntity plan = planRepository.findById(id).orElse(null);
            if (plan != null) {
                completitud = actividadEstandarFichaService.calcularEstadosCompletitud(plan, asigs);
            }
        }
        final Map<UUID, ActividadEstandarFichaService.CompletitudEstado> completitudFinal = completitud;
        List<ActividadCurricularPlanDto> dtos = asigs.stream()
                .map(a -> toActividadDto(a, id, completitudFinal.get(a.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<PlanEstudioFt001Dto> crear(@RequestBody PlanEstudioFt001Dto dto) {
        if (dto.carreraId() == null) {
            throw new DomainValidationException("El ID de la carrera es obligatorio");
        }
        CarreraJpaEntity carrera = carreraRepository.findById(dto.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera con ID: " + dto.carreraId()));

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
        plan.setCarrera(carrera);
        if (estandarRepository != null) {
            EstandarJpaEntity estandarAsignado;
            if (dto.estandarId() != null) {
                estandarAsignado = estandarRepository.findById(dto.estandarId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No se encontró el estándar con ID: " + dto.estandarId()));
                if (estandarAsignado.getCarrera() == null
                        || !carrera.getId().equals(estandarAsignado.getCarrera().getId())) {
                    throw new DomainValidationException(
                            "El estándar seleccionado no pertenece a la carrera del plan.");
                }
            } else {
                estandarAsignado = estandarRepository
                        .findByCarreraIdAndEstadoVigencia(carrera.getId(), EstadoVigenciaEstandar.VIGENTE)
                        .orElseThrow(() -> new DomainValidationException(
                                "La carrera '" + carrera.getNombre()
                                        + "' no tiene un Estándar vigente configurado. "
                                        + "Configure un estándar vigente en Gestión Académica -> Estándares antes de crear un plan de estudio."));
            }
            plan.setEstandar(estandarAsignado);
        }
        mapearDatosPlan(plan, dto);
        if (plan.getProcesoId() != null) {
            plan.setEsMaestro(false);
            if (procesoRepository.findById(plan.getProcesoId()).isEmpty()) {
                UUID resolvedProcesoId = procesoRepository.findByCarreraIdAndEstadoIn(
                        carrera.getId(),
                        List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
                ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null);
                plan.setProcesoId(resolvedProcesoId);
            }
        }
        Instant now = Instant.now();
        plan.setCreadoEn(now);
        plan.setActualizadoEn(now);

        if (dto.asignaturas() != null && !dto.asignaturas().isEmpty()) {
            List<ActividadCurricularPlanJpaEntity> listaAsignaturas = new ArrayList<>();
            for (ActividadCurricularPlanDto asigDto : dto.asignaturas()) {
                ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
                asig.setId(asigDto.id() != null ? asigDto.id() : UUID.randomUUID());
                asig.setPlanEstudio(plan);
                mapearDatosAsignatura(asig, asigDto);
                asig.setCreadoEn(now);
                listaAsignaturas.add(asig);
            }
            plan.setAsignaturas(listaAsignaturas);
        }

        PlanEstudioFt001JpaEntity guardado = planRepository.save(plan);
        if (guardado.getProcesoId() != null && sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.vincularOCrearMaestroParaPlanDeProceso(guardado);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<PlanEstudioFt001Dto> actualizar(@PathVariable UUID id, @RequestBody PlanEstudioFt001Dto dto) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + id));

        mapearDatosPlan(plan, dto);
        if (plan.getProcesoId() != null && procesoRepository.findById(plan.getProcesoId()).isEmpty()) {
            UUID carreraId = plan.getCarrera() != null ? plan.getCarrera().getId() : dto.carreraId();
            UUID resolvedProcesoId = carreraId != null ? procesoRepository.findByCarreraIdAndEstadoIn(
                    carreraId,
                    List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
            ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null) : null;
            plan.setProcesoId(resolvedProcesoId);
        }
        plan.setActualizadoEn(Instant.now());

        PlanEstudioFt001JpaEntity actualizado = planRepository.save(plan);
        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionPlan(actualizado);
        }
        return ResponseEntity.ok(toDto(actualizado));
    }

    @PostMapping("/{planId}/asignaturas")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ActividadCurricularPlanDto> crearAsignatura(
            @PathVariable UUID planId,
            @RequestBody CrearAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto dto = crearAsignaturaPlanService.ejecutar(planId, request);
        sincronizarCompletitudAutomaticaActividad83(planId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{planId}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ActividadCurricularPlanDto> actualizarAsignatura(
            @PathVariable UUID planId,
            @PathVariable UUID asigId,
            @RequestBody ActualizarAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto dto = actualizarAsignaturaPlanService.ejecutar(planId, asigId, request);
        sincronizarCompletitudAutomaticaActividad83(planId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{planId}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<Void> eliminarAsignatura(
            @PathVariable UUID planId,
            @PathVariable UUID asigId
    ) {
        eliminarAsignaturaPlanService.ejecutar(planId, asigId);
        sincronizarCompletitudAutomaticaActividad83(planId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{planId}/asignaturas/{asigId}/estado-dictado")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ActividadCurricularPlanDto> cambiarEstadoDictado(
            @PathVariable UUID planId,
            @PathVariable UUID asigId,
            @RequestBody java.util.Map<String, String> body
    ) {
        ActividadCurricularPlanJpaEntity asig = asignaturaRepository.findById(asigId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura con ID: " + asigId));
        if (!asig.getPlanEstudio().getId().equals(planId)) {
            throw new DomainValidationException("La asignatura no pertenece al plan de estudio indicado.");
        }
        String raw = body != null ? body.get("estadoDictado") : null;
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException("El estadoDictado es obligatorio (ACTIVA, SOLO_EXAMENES_FINALES o INACTIVA).");
        }
        try {
            asig.setEstadoDictado(EstadoDictadoActividad.fromString(raw));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Estado de dictado inválido: " + raw);
        }
        ActividadCurricularPlanJpaEntity guardada = asignaturaRepository.save(asig);
        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionAsignatura(asig.getPlanEstudio(), guardada);
        }
        sincronizarCompletitudAutomaticaActividad83(planId);
        ActividadEstandarFichaService.CompletitudEstado estado =
                (actividadEstandarFichaService != null && guardada.getPlanEstudio() != null)
                        ? actividadEstandarFichaService.calcularEstadosCompletitud(
                                guardada.getPlanEstudio(), List.of(guardada)).get(guardada.getId())
                        : null;
        return ResponseEntity.ok(toActividadDto(guardada, planId, estado));
    }

    @GetMapping("/{planId}/cumplimiento-estandar")
    @Transactional(readOnly = true)
    public ResponseEntity<com.undec.acreditacion.infrastructure.estandares.dto.CumplimientoCargaHorariaPlanDto> obtenerCumplimientoEstandar(
            @PathVariable UUID planId
    ) {
        if (actividadEstandarFichaService == null) {
            throw new DomainValidationException("El servicio de fichas de estándar no está disponible.");
        }
        return ResponseEntity.ok(actividadEstandarFichaService.calcularCumplimientoHorarioPlan(planId));
    }

    @GetMapping("/{planId}/asignaturas/{asigId}/ficha-estandar")
    @Transactional(readOnly = true)
    public ResponseEntity<FichaEstandarActividadDto> obtenerFichaEstandar(
            @PathVariable UUID planId,
            @PathVariable UUID asigId
    ) {
        if (actividadEstandarFichaService == null) {
            throw new DomainValidationException("El servicio de fichas de estándar no está disponible.");
        }
        return ResponseEntity.ok(actividadEstandarFichaService.obtenerFicha(planId, asigId));
    }

    @PutMapping("/{planId}/asignaturas/{asigId}/ficha-estandar")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<FichaEstandarActividadDto> guardarFichaEstandar(
            @PathVariable UUID planId,
            @PathVariable UUID asigId,
            @RequestBody GuardarFichaEstandarRequest request
    ) {
        if (actividadEstandarFichaService == null) {
            throw new DomainValidationException("El servicio de fichas de estándar no está disponible.");
        }
        FichaEstandarActividadDto ficha = actividadEstandarFichaService.guardarFicha(planId, asigId, request);
        sincronizarCompletitudAutomaticaActividad83(planId);
        return ResponseEntity.ok(ficha);
    }

    @PatchMapping("/{planId}/asignaturas/{asigId}/docente-responsable")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ActividadCurricularPlanDto> designarDocenteResponsable(
            @PathVariable UUID planId,
            @PathVariable UUID asigId,
            @RequestBody DesignarDocenteResponsableRequest body
    ) {
        if (actividadEstandarFichaService == null) {
            throw new DomainValidationException("El servicio de fichas de estándar no está disponible.");
        }
        ActividadCurricularPlanJpaEntity actualizada = actividadEstandarFichaService.designarDocenteResponsable(
                planId,
                asigId,
                body != null ? body.docenteUsuarioId() : null,
                body != null ? body.docenteNombre() : null
        );
        PlanEstudioFt001JpaEntity plan = actualizada.getPlanEstudio();
        ActividadEstandarFichaService.CompletitudEstado estado = plan != null
                ? actividadEstandarFichaService.calcularEstadosCompletitud(plan, List.of(actualizada)).get(actualizada.getId())
                : null;
        return ResponseEntity.ok(toActividadDto(actualizada, planId, estado));
    }

    private record EvaluacionCompletitud83(
            long totalActivas,
            long asignaturasCompletas,
            boolean todasCompletas
    ) {
    }

    private EvaluacionCompletitud83 evaluarCompletitud83(PlanEstudioFt001JpaEntity plan) {
        if (plan == null || plan.getId() == null) {
            return new EvaluacionCompletitud83(0, 0, false);
        }
        List<ActividadCurricularPlanJpaEntity> activas = asignaturaRepository
                .findByPlanEstudioIdOrderByNumeroOrdenAsc(plan.getId()).stream()
                .filter(a -> !a.getEstadoDictado().esInactiva())
                .toList();
        if (activas.isEmpty()) {
            return new EvaluacionCompletitud83(0, 0, false);
        }
        if (actividadEstandarFichaService == null) {
            return new EvaluacionCompletitud83(activas.size(), activas.size(), true);
        }
        Map<UUID, ActividadEstandarFichaService.CompletitudEstado> estados =
                actividadEstandarFichaService.calcularEstadosCompletitud(plan, activas);
        boolean tieneEstandar = plan.getEstandar() != null;
        long completas = activas.stream()
                .filter(a -> camposObligatoriosAsignaturaCompletos(a)
                        && tieneEstandar
                        && estados.get(a.getId()) != null
                        && estados.get(a.getId()).seccion22Completa()
                        && estados.get(a.getId()).seccion3Completa())
                .count();
        return new EvaluacionCompletitud83(activas.size(), completas, completas == activas.size());
    }

    private boolean camposObligatoriosAsignaturaCompletos(ActividadCurricularPlanJpaEntity a) {
        return a != null
                && a.getCodigo() != null && !a.getCodigo().isBlank()
                && a.getNombre() != null && !a.getNombre().isBlank()
                && a.getGrupo() != null && !a.getGrupo().isBlank()
                && a.getSubgrupo() != null && !a.getSubgrupo().isBlank()
                && a.getHorasReloj() != null && a.getHorasReloj() > 0;
    }

    private void sincronizarCompletitudAutomaticaActividad83(UUID planId) {
        if (planId == null || planTrabajoRepository == null || actividadEstandarFichaService == null) {
            return;
        }
        PlanYProcesoResuelto resuelto = resolverPlanYProceso(null, planId);
        if (resuelto.planEntidad() == null || resuelto.planTrabajoOpt().isEmpty()) {
            return;
        }
        Optional<ActividadPlanTrabajo> act83Opt = resuelto.planTrabajoOpt().get().getActividades().stream()
                .filter(a -> "8.3".equals(a.getCodigoActividad()))
                .findFirst();
        if (act83Opt.isEmpty()) {
            return;
        }
        ActividadPlanTrabajo act83 = act83Opt.get();
        EvaluacionCompletitud83 eval = evaluarCompletitud83(resuelto.planEntidad());
        if (eval.todasCompletas()) {
            if (act83.getEstado() != EstadoActividad.COMPLETADA) {
                UUID usuarioId = authenticatedUserProvider != null
                        ? authenticatedUserProvider.getAuthenticatedUserIdOrNull()
                        : null;
                act83.completar(
                        usuarioId,
                        "Actividad 8.3 completada automáticamente: " + eval.totalActivas()
                                + " actividades curriculares con campos obligatorios y estándar CONEAU completos"
                );
                planTrabajoRepository.saveActividad(act83);
            }
        } else if (act83.getEstado() == EstadoActividad.COMPLETADA) {
            act83.cambiarEstado(eval.totalActivas() > 0 ? EstadoActividad.EN_PROGRESO : EstadoActividad.PENDIENTE);
            planTrabajoRepository.saveActividad(act83);
        }
    }

    private record PlanYProcesoResuelto(
            PlanEstudioFt001JpaEntity planEntidad,
            UUID effectiveProcesoId,
            long totalActivas,
            Optional<PlanTrabajo> planTrabajoOpt
    ) {
    }

    private PlanYProcesoResuelto resolverPlanYProceso(UUID procesoId, UUID planEstudioId) {
        UUID effectiveProcesoId = procesoId;
        PlanEstudioFt001JpaEntity planEntidad = planEstudioId != null
                ? planRepository.findById(planEstudioId).orElse(null)
                : null;

        if (effectiveProcesoId == null && planEntidad != null) {
            if (planEntidad.getProcesoId() != null) {
                effectiveProcesoId = planEntidad.getProcesoId();
            } else if (planEntidad.getCarrera() != null) {
                effectiveProcesoId = procesoRepository.findByCarreraIdAndEstadoIn(
                        planEntidad.getCarrera().getId(),
                        List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
                ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null);
            }
        }

        if (planEntidad == null && effectiveProcesoId != null) {
            List<PlanEstudioFt001JpaEntity> porProceso = planRepository.findByProcesoIdAndActivoTrue(effectiveProcesoId);
            if (!porProceso.isEmpty()) {
                planEntidad = porProceso.get(0);
            } else {
                ProcesoAcreditacionJpaEntity proc = procesoRepository.findById(effectiveProcesoId).orElse(null);
                if (proc != null && proc.getCarreraId() != null) {
                    planEntidad = planRepository.findFirstByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(proc.getCarreraId()).orElse(null);
                }
            }
        }

        long totalActivas = 0;
        if (planEntidad != null) {
            totalActivas = asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planEntidad.getId()).stream()
                    .filter(a -> !a.getEstadoDictado().esInactiva())
                    .count();
        }

        Optional<PlanTrabajo> planTrabajoOpt = Optional.empty();
        if (planTrabajoRepository != null) {
            if (effectiveProcesoId != null) {
                planTrabajoOpt = planTrabajoRepository.findByProcesoId(effectiveProcesoId);
            }
            if (planTrabajoOpt.isEmpty() && planEntidad != null && planEntidad.getCarrera() != null) {
                UUID resolved = procesoRepository.findByCarreraIdAndEstadoIn(
                        planEntidad.getCarrera().getId(),
                        List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
                ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null);
                if (resolved != null) {
                    effectiveProcesoId = resolved;
                    planTrabajoOpt = planTrabajoRepository.findByProcesoId(resolved);
                }
            }
        }

        return new PlanYProcesoResuelto(planEntidad, effectiveProcesoId, totalActivas, planTrabajoOpt);
    }

    private void mapearDatosPlan(PlanEstudioFt001JpaEntity plan, PlanEstudioFt001Dto dto) {
        if (dto.nombrePlan() != null && !dto.nombrePlan().isBlank()) {
            plan.setNombrePlan(dto.nombrePlan().trim());
        } else if (plan.getNombrePlan() == null || plan.getNombrePlan().isBlank()) {
            String fallback = (dto.codigoResolucion() != null && !dto.codigoResolucion().isBlank())
                    ? dto.codigoResolucion().trim()
                    : "Plan " + (dto.anioAprobacion() != null ? dto.anioAprobacion() : 2026);
            plan.setNombrePlan(fallback);
        }
        plan.setProcesoId(dto.procesoId());
        plan.setCodigoResolucion(dto.codigoResolucion());
        plan.setAnioAprobacion(dto.anioAprobacion());
        plan.setAnioInicio(dto.anioInicio());
        plan.setDuracionAnios(dto.duracionAnios());
        plan.setOrientacionesJson(dto.orientacionesJson());
        plan.setFundamentacionModificaciones(dto.fundamentacionModificaciones());
        plan.setEstructuraSintetica(dto.estructuraSintetica());
        plan.setHorasPresenciales(dto.horasPresenciales());
        plan.setHorasNoPresenciales(dto.horasNoPresenciales());
        plan.setHorasTotales(dto.horasTotales());
        plan.setHorasOptativasMinimas(dto.horasOptativasMinimas());
        plan.setActividadesComunesCiclo(dto.actividadesComunesCiclo());
        plan.setTituloIntermedioDenominacion(dto.tituloIntermedioDenominacion());
        plan.setTituloIntermedioDuracion(dto.tituloIntermedioDuracion());
        plan.setTituloIntermedioActividades(dto.tituloIntermedioActividades());
        plan.setInstanciaFinalGraduacion(dto.instanciaFinalGraduacion());
        plan.setOtraInformacion(dto.otraInformacion());
        if (dto.activo() != null) {
            plan.setActivo(dto.activo());
        }
    }

    private void mapearDatosAsignatura(ActividadCurricularPlanJpaEntity asig, ActividadCurricularPlanDto dto) {
        asig.setNumeroOrden(dto.numeroOrden());
        asig.setCodigo(dto.codigo());
        asig.setNombre(dto.nombre());
        asig.setGrupo(dto.grupo());
        asig.setSubgrupo(dto.subgrupo());
        asig.setHorasReloj(dto.horasReloj());
        asig.setHorasPresenciales(dto.horasPresenciales());
        asig.setHorasNoPresenciales(dto.horasNoPresenciales());
        asig.setHorasSemanales(dto.horasSemanales());
        asig.setCorrelativas(dto.correlativas());
        if (dto.esObligatoria() != null) {
            asig.setEsObligatoria(dto.esObligatoria());
        }
        if (dto.estadoDictado() != null) {
            asig.setEstadoDictado(dto.estadoDictado());
        }
    }

    private ActividadCurricularPlanDto toActividadDto(
            ActividadCurricularPlanJpaEntity a,
            UUID planId,
            ActividadEstandarFichaService.CompletitudEstado estado
    ) {
        return new ActividadCurricularPlanDto(
                a.getId(),
                planId,
                a.getNumeroOrden(),
                a.getCodigo(),
                a.getNombre(),
                a.getGrupo(),
                a.getSubgrupo(),
                a.getHorasReloj(),
                a.getHorasPresenciales(),
                a.getHorasNoPresenciales(),
                a.getHorasSemanales(),
                a.getCorrelativas(),
                a.getEsObligatoria(),
                a.getEstadoDictado(),
                a.getDocenteResponsableId(),
                a.getDocenteResponsableNombre(),
                estado != null && estado.seccion22Completa(),
                estado != null && estado.seccion3Completa()
        );
    }

    private PlanEstudioFt001Dto toDto(PlanEstudioFt001JpaEntity plan) {
        List<ActividadCurricularPlanDto> asigDtos = null;
        if (plan.getAsignaturas() != null) {
            Map<UUID, ActividadEstandarFichaService.CompletitudEstado> completitud =
                    (actividadEstandarFichaService != null && !plan.getAsignaturas().isEmpty())
                            ? actividadEstandarFichaService.calcularEstadosCompletitud(plan, plan.getAsignaturas())
                            : Map.of();
            asigDtos = plan.getAsignaturas().stream()
                    .map(a -> toActividadDto(a, plan.getId(), completitud.get(a.getId())))
                    .collect(Collectors.toList());
        }

        UUID carreraId = plan.getCarrera() != null ? plan.getCarrera().getId() : null;
        String carreraNombre = plan.getCarrera() != null ? plan.getCarrera().getNombre() : null;
        UUID estandarId = plan.getEstandar() != null ? plan.getEstandar().getId() : null;
        String estandarNombre = plan.getEstandar() != null ? plan.getEstandar().getNombre() : null;
        String estandarResolucion = plan.getEstandar() != null ? plan.getEstandar().getResolucionMinisterial() : null;

        return new PlanEstudioFt001Dto(
                plan.getId(),
                carreraId,
                carreraNombre,
                plan.getProcesoId(),
                plan.getNombrePlan(),
                plan.getCodigoResolucion(),
                plan.getAnioAprobacion(),
                plan.getAnioInicio(),
                plan.getDuracionAnios(),
                plan.getOrientacionesJson(),
                plan.getFundamentacionModificaciones(),
                plan.getEstructuraSintetica(),
                plan.getHorasPresenciales(),
                plan.getHorasNoPresenciales(),
                plan.getHorasTotales(),
                plan.getHorasOptativasMinimas(),
                plan.getActividadesComunesCiclo(),
                plan.getTituloIntermedioDenominacion(),
                plan.getTituloIntermedioDuracion(),
                plan.getTituloIntermedioActividades(),
                plan.getInstanciaFinalGraduacion(),
                plan.getOtraInformacion(),
                plan.getActivo(),
                asigDtos,
                plan.getCreadoEn(),
                plan.getActualizadoEn(),
                estandarId,
                estandarNombre,
                estandarResolucion
        );
    }
}
