package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.EstadoActividad;
import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
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
                null, null, null);
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
            AuthenticatedUserProvider authenticatedUserProvider
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
    }

    @GetMapping
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
    public ResponseEntity<List<PlanEstudioFt001Dto>> listarPorCarrera(@PathVariable UUID carreraId) {
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId);
        List<PlanEstudioFt001Dto> dtos = planes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/carrera/{carreraId}/vigentes")
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
        boolean completada = resuelto.planTrabajoOpt()
                .flatMap(pt -> pt.getActividades().stream()
                        .filter(a -> "8.3".equals(a.getCodigoActividad()))
                        .findFirst())
                .map(a -> a.getEstado() == EstadoActividad.COMPLETADA)
                .orElse(false);

        return ResponseEntity.ok(Map.of(
                "completada", completada,
                "totalAsignaturas", resuelto.totalActivas()
        ));
    }

    @PostMapping("/validar-completar-8-3")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_ESCUELA', 'SCHOOL_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<Map<String, Object>> validarYCompletar83(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID planEstudioId
    ) {
        PlanYProcesoResuelto resuelto = resolverPlanYProceso(procesoId, planEstudioId);
        if (resuelto.planEntidad() == null || resuelto.totalActivas() == 0) {
            throw new DomainValidationException(
                    "No se puede completar la actividad 8.3: el plan de estudios no tiene actividades curriculares activas cargadas."
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
                            "Plan de estudios FT-001 y " + resuelto.totalActivas()
                                    + " actividades curriculares verificadas y sincronizadas con Académica"
                    );
                    planTrabajoRepository.saveActividad(act83);
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "completada", true,
                "totalAsignaturas", resuelto.totalActivas(),
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
        List<ActividadCurricularPlanDto> dtos = asigs.stream().map(a -> new ActividadCurricularPlanDto(
                a.getId(),
                id,
                a.getNumeroOrden(),
                a.getCodigo(),
                a.getNombre(),
                a.getGrupo(),
                a.getSubgrupo(),
                a.getHorasReloj(),
                a.getHorasPresenciales(),
                a.getHorasNoPresenciales(),
                a.getCorrelativas(),
                a.getEsObligatoria(),
                a.getEstadoDictado()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
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
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
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
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ActividadCurricularPlanDto> crearAsignatura(
            @PathVariable UUID planId,
            @RequestBody CrearAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto dto = crearAsignaturaPlanService.ejecutar(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{planId}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ActividadCurricularPlanDto> actualizarAsignatura(
            @PathVariable UUID planId,
            @PathVariable UUID asigId,
            @RequestBody ActualizarAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto dto = actualizarAsignaturaPlanService.ejecutar(planId, asigId, request);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{planId}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'DOCENTE') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<Void> eliminarAsignatura(
            @PathVariable UUID planId,
            @PathVariable UUID asigId
    ) {
        eliminarAsignaturaPlanService.ejecutar(planId, asigId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{planId}/asignaturas/{asigId}/estado-dictado")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
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
            throw new DomainValidationException("El estadoDictado es obligatorio (SE_DICTA o NO_SE_DICTA).");
        }
        try {
            asig.setEstadoDictado(EstadoDictadoActividad.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Estado de dictado inválido: " + raw);
        }
        ActividadCurricularPlanJpaEntity guardada = asignaturaRepository.save(asig);
        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionAsignatura(asig.getPlanEstudio(), guardada);
        }
        return ResponseEntity.ok(new ActividadCurricularPlanDto(
                guardada.getId(),
                planId,
                guardada.getNumeroOrden(),
                guardada.getCodigo(),
                guardada.getNombre(),
                guardada.getGrupo(),
                guardada.getSubgrupo(),
                guardada.getHorasReloj(),
                guardada.getHorasPresenciales(),
                guardada.getHorasNoPresenciales(),
                guardada.getCorrelativas(),
                guardada.getEsObligatoria(),
                guardada.getEstadoDictado()
        ));
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
                    .filter(a -> a.getEstadoDictado() != EstadoDictadoActividad.NO_SE_DICTA)
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
        asig.setCorrelativas(dto.correlativas());
        if (dto.esObligatoria() != null) {
            asig.setEsObligatoria(dto.esObligatoria());
        }
    }

    private PlanEstudioFt001Dto toDto(PlanEstudioFt001JpaEntity plan) {
        List<ActividadCurricularPlanDto> asigDtos = null;
        if (plan.getAsignaturas() != null) {
            asigDtos = plan.getAsignaturas().stream().map(a -> new ActividadCurricularPlanDto(
                    a.getId(),
                    plan.getId(),
                    a.getNumeroOrden(),
                    a.getCodigo(),
                    a.getNombre(),
                    a.getGrupo(),
                    a.getSubgrupo(),
                    a.getHorasReloj(),
                    a.getHorasPresenciales(),
                    a.getHorasNoPresenciales(),
                    a.getCorrelativas(),
                    a.getEsObligatoria(),
                    a.getEstadoDictado()
            )).collect(Collectors.toList());
        }

        UUID carreraId = plan.getCarrera() != null ? plan.getCarrera().getId() : null;
        String carreraNombre = plan.getCarrera() != null ? plan.getCarrera().getNombre() : null;

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
                plan.getActualizadoEn()
        );
    }
}
