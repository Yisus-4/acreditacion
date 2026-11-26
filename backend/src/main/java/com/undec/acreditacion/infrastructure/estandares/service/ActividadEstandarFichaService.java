package com.undec.acreditacion.infrastructure.estandares.service;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataProgramaAnaliticoAsignaturaRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.estandares.dto.CumplimientoCargaHorariaPlanDto;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarDetalleDto;
import com.undec.acreditacion.infrastructure.estandares.dto.FichaEstandarActividadDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarFichaEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUserRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestiona la ficha de evaluación de Sección 2.2 (competencias/ejes/bloques) y
 * Sección 3 (carga horaria) por asignatura de un plan de estudio, además de la
 * designación del docente responsable.
 */
@Service
@Transactional
public class ActividadEstandarFichaService {

    public record CompletitudEstado(boolean seccion22Completa, boolean seccion3Completa) {}

    private record AreaMeta(UUID id, TipoSeccionCargaHoraria tipoSeccion, String codigo) {}

    private static final Set<String> ROLES_GESTORES = Set.of(
            "AEA",
            "SECRETARIA_ACADEMICA",
            "DIRECTOR_ESCUELA",
            "DIRECTOR_CARRERA",
            "DIRECTOR_DEPARTAMENTO",
            "COMISION_AUTOEVALUACION"
    );

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataEstandarEscalaEvaluacionRepository escalaRepository;
    private final SpringDataEstandarGrupo22Repository grupoRepository;
    private final SpringDataEstandarItem22Repository itemRepository;
    private final SpringDataEstandarAreaCargaHorariaRepository areaRepository;
    private final SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository;
    private final SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository;
    private final SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository;
    private final EstandarService estandarService;
    private final SpringDataUserRepository userRepository;
    private final SpringDataProgramaAnaliticoAsignaturaRepository programaRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ActividadEstandarFichaService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataEstandarEscalaEvaluacionRepository escalaRepository,
            SpringDataEstandarGrupo22Repository grupoRepository,
            SpringDataEstandarItem22Repository itemRepository,
            SpringDataEstandarAreaCargaHorariaRepository areaRepository,
            SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository,
            SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository,
            SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository,
            EstandarService estandarService
    ) {
        this(planRepository, asignaturaRepository, escalaRepository, grupoRepository, itemRepository,
                areaRepository, evaluacion22Repository, cargaAreaRepository, cargaResumenRepository,
                estandarService, null, null, null, null);
    }

    @Autowired
    public ActividadEstandarFichaService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataEstandarEscalaEvaluacionRepository escalaRepository,
            SpringDataEstandarGrupo22Repository grupoRepository,
            SpringDataEstandarItem22Repository itemRepository,
            SpringDataEstandarAreaCargaHorariaRepository areaRepository,
            SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository,
            SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository,
            SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository,
            EstandarService estandarService,
            SpringDataUserRepository userRepository,
            SpringDataProgramaAnaliticoAsignaturaRepository programaRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.planRepository = Objects.requireNonNull(planRepository);
        this.asignaturaRepository = Objects.requireNonNull(asignaturaRepository);
        this.escalaRepository = Objects.requireNonNull(escalaRepository);
        this.grupoRepository = Objects.requireNonNull(grupoRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.areaRepository = Objects.requireNonNull(areaRepository);
        this.evaluacion22Repository = Objects.requireNonNull(evaluacion22Repository);
        this.cargaAreaRepository = Objects.requireNonNull(cargaAreaRepository);
        this.cargaResumenRepository = Objects.requireNonNull(cargaResumenRepository);
        this.estandarService = Objects.requireNonNull(estandarService);
        this.userRepository = userRepository;
        this.programaRepository = programaRepository;
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public FichaEstandarActividadDto obtenerFicha(UUID planId, UUID asigId) {
        PlanEstudioFt001JpaEntity plan = findPlanOrThrow(planId);
        ActividadCurricularPlanJpaEntity asig = findAsignaturaDePlanOrThrow(planId, asigId);
        EstandarJpaEntity estandar = requireEstandar(plan);

        return buildFichaDto(plan, asig, estandar);
    }

    public FichaEstandarActividadDto guardarFicha(UUID planId, UUID asigId, GuardarFichaEstandarRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la ficha del estándar son obligatorios.");
        }
        PlanEstudioFt001JpaEntity plan = findPlanOrThrow(planId);
        ActividadCurricularPlanJpaEntity asig = findAsignaturaDePlanOrThrow(planId, asigId);
        if (!puedeEditarFicha(asig)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el docente responsable designado o la gestión académica puede completar la ficha del estándar de esta asignatura.");
        }
        EstandarJpaEntity estandar = requireEstandar(plan);

        guardarEvaluacion22(asig, estandar, req.respuestas22());
        guardarCargaHoraria3(asig, estandar, req.cargasPorArea(), req.resumenCarga());

        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarFichaEstandarAsignatura(plan, asig);
        }

        return buildFichaDto(plan, asig, estandar);
    }

    public ActividadCurricularPlanJpaEntity designarDocenteResponsable(
            UUID planId,
            UUID asigId,
            UUID docenteUsuarioId,
            String docenteNombre
    ) {
        if (!esUsuarioGestorOAnonimo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo la gestión académica puede designar al docente responsable de una asignatura.");
        }
        PlanEstudioFt001JpaEntity plan = findPlanOrThrow(planId);
        ActividadCurricularPlanJpaEntity asig = findAsignaturaDePlanOrThrow(planId, asigId);

        String resolvedName = trimToNull(docenteNombre);
        if (docenteUsuarioId != null && userRepository != null) {
            UserJpaEntity user = userRepository.findById(docenteUsuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el usuario docente ID: " + docenteUsuarioId));
            if (resolvedName == null) {
                resolvedName = user.getUsername() != null && !user.getUsername().isBlank()
                        ? user.getUsername()
                        : user.getEmail();
            }
        }
        if (docenteUsuarioId == null) {
            resolvedName = null;
        }

        asig.setDocenteResponsableId(docenteUsuarioId);
        asig.setDocenteResponsableNombre(resolvedName);
        ActividadCurricularPlanJpaEntity saved = asignaturaRepository.save(asig);

        if (programaRepository != null) {
            final String finalName = resolvedName;
            programaRepository.findByActividadCurricularId(saved.getId()).ifPresent(prog -> {
                prog.setDocenteDesignadoId(docenteUsuarioId);
                prog.setDocenteDesignadoNombre(finalName);
                prog.setActualizadoEn(Instant.now());
                programaRepository.save(prog);
            });
        }

        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionAsignatura(plan, saved);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<UUID, CompletitudEstado> calcularEstadosCompletitud(
            PlanEstudioFt001JpaEntity plan,
            List<ActividadCurricularPlanJpaEntity> asignaturas
    ) {
        if (plan == null || plan.getEstandar() == null || asignaturas == null || asignaturas.isEmpty()) {
            return Map.of();
        }
        EstandarJpaEntity estandar = plan.getEstandar();
        List<UUID> asigIds = asignaturas.stream().map(ActividadCurricularPlanJpaEntity::getId).toList();

        Set<UUID> escalasValidas = escalaRepository.findByEstandarIdOrderByOrdenAsc(estandar.getId()).stream()
                .map(EstandarEscalaEvaluacionJpaEntity::getId)
                .collect(Collectors.toSet());
        Set<UUID> itemsHabilitados = resolveEnabledItemIds(estandar.getId());
        boolean hab311 = esHabilitado311(estandar);
        boolean hab312 = esHabilitado312(estandar);
        List<AreaMeta> areasHabilitadasMeta =
                areaRepository.findByEstandarIdOrderByOrdenAsc(estandar.getId()).stream()
                        .filter(a -> Boolean.TRUE.equals(a.getHabilitada()))
                        .filter(a -> {
                            TipoSeccionCargaHoraria t = a.getTipoSeccion() != null
                                    ? a.getTipoSeccion()
                                    : TipoSeccionCargaHoraria.CARGA_TOTAL_311;
                            return (t == TipoSeccionCargaHoraria.CARGA_TOTAL_311 && hab311)
                                    || (t == TipoSeccionCargaHoraria.PRACTICA_312 && hab312);
                        })
                        .map(a -> new AreaMeta(
                                a.getId(),
                                a.getTipoSeccion() != null ? a.getTipoSeccion() : TipoSeccionCargaHoraria.CARGA_TOTAL_311,
                                a.getCodigo()))
                        .toList();

        Map<UUID, List<ActividadCurricularEvaluacion22JpaEntity>> evalByAsig =
                evaluacion22Repository.findByActividadCurricularIdIn(asigIds).stream()
                        .collect(Collectors.groupingBy(e -> e.getActividadCurricular().getId()));
        Map<UUID, List<ActividadCurricularCargaHorariaAreaJpaEntity>> cargaByAsig =
                cargaAreaRepository.findByActividadCurricularIdIn(asigIds).stream()
                        .collect(Collectors.groupingBy(c -> c.getActividadCurricular().getId()));
        Map<UUID, ActividadCurricularCargaHorariaResumenJpaEntity> resumenByAsig =
                cargaResumenRepository.findByActividadCurricularIdIn(asigIds).stream()
                        .collect(Collectors.toMap(
                                ActividadCurricularCargaHorariaResumenJpaEntity::getActividadCurricularId,
                                Function.identity(),
                                (a, b) -> a));

        Map<UUID, CompletitudEstado> result = new HashMap<>();
        for (ActividadCurricularPlanJpaEntity asig : asignaturas) {
            boolean sec22 = evaluarSeccion22Completa(
                    estandar, itemsHabilitados, escalasValidas,
                    evalByAsig.getOrDefault(asig.getId(), List.of()));
            boolean sec3 = evaluarSeccion3Completa(
                    asig, areasHabilitadasMeta,
                    cargaByAsig.getOrDefault(asig.getId(), List.of()),
                    resumenByAsig.get(asig.getId()));
            result.put(asig.getId(), new CompletitudEstado(sec22, sec3));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CumplimientoCargaHorariaPlanDto calcularCumplimientoHorarioPlan(UUID planId) {
        PlanEstudioFt001JpaEntity plan = findPlanOrThrow(planId);
        EstandarJpaEntity estandar = requireEstandar(plan);

        boolean hab311 = esHabilitado311(estandar);
        ModoSeccion311 modo311 = resolverModo311(estandar);
        boolean hab312 = esHabilitado312(estandar);
        ModoSeccion312 modo312 = resolverModo312(estandar);

        List<ActividadCurricularPlanJpaEntity> asignaturasSeDicta =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId).stream()
                        .filter(a -> !a.getEstadoDictado().esInactiva())
                        .toList();

        List<EstandarAreaCargaHorariaJpaEntity> todasAreas =
                areaRepository.findByEstandarIdOrderByOrdenAsc(estandar.getId());

        List<EstandarAreaCargaHorariaJpaEntity> areas311 = hab311
                ? todasAreas.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getHabilitada()))
                        .filter(a -> a.getTipoSeccion() == null || a.getTipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311)
                        .toList()
                : List.of();
        List<EstandarAreaCargaHorariaJpaEntity> areas312 = hab312
                ? todasAreas.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getHabilitada()))
                        .filter(a -> a.getTipoSeccion() == TipoSeccionCargaHoraria.PRACTICA_312)
                        .toList()
                : List.of();

        List<UUID> asigIds = asignaturasSeDicta.stream()
                .map(ActividadCurricularPlanJpaEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        List<ActividadCurricularCargaHorariaAreaJpaEntity> cargas = asigIds.isEmpty()
                ? List.of()
                : cargaAreaRepository.findByActividadCurricularIdIn(asigIds);
        Map<UUID, ActividadCurricularCargaHorariaResumenJpaEntity> resumenByAsig = asigIds.isEmpty()
                ? Map.of()
                : cargaResumenRepository.findByActividadCurricularIdIn(asigIds).stream()
                        .collect(Collectors.toMap(
                                ActividadCurricularCargaHorariaResumenJpaEntity::getActividadCurricularId,
                                Function.identity(),
                                (a, b) -> a));

        Map<UUID, List<ActividadCurricularCargaHorariaAreaJpaEntity>> cargaByAsig = cargas.stream()
                .filter(c -> c.getActividadCurricular() != null && c.getActividadCurricular().getId() != null)
                .collect(Collectors.groupingBy(c -> c.getActividadCurricular().getId()));

        List<AreaMeta> areasMeta = new ArrayList<>();
        for (EstandarAreaCargaHorariaJpaEntity a : areas311) {
            areasMeta.add(new AreaMeta(a.getId(), TipoSeccionCargaHoraria.CARGA_TOTAL_311, a.getCodigo()));
        }
        for (EstandarAreaCargaHorariaJpaEntity a : areas312) {
            areasMeta.add(new AreaMeta(a.getId(), TipoSeccionCargaHoraria.PRACTICA_312, a.getCodigo()));
        }

        int asignaturasConSec3 = 0;
        for (ActividadCurricularPlanJpaEntity asig : asignaturasSeDicta) {
            if (evaluarSeccion3Completa(
                    asig,
                    areasMeta,
                    cargaByAsig.getOrDefault(asig.getId(), List.of()),
                    resumenByAsig.get(asig.getId()))) {
                asignaturasConSec3++;
            }
        }

        Map<UUID, int[]> suma311PorArea = new HashMap<>();
        Map<UUID, int[]> suma312PorArea = new HashMap<>();
        for (ActividadCurricularCargaHorariaAreaJpaEntity c : cargas) {
            if (c.getAreaCargaHoraria() == null || c.getAreaCargaHoraria().getId() == null) {
                continue;
            }
            UUID areaId = c.getAreaCargaHoraria().getId();
            int[] acc311 = suma311PorArea.computeIfAbsent(areaId, k -> new int[2]);
            acc311[0] += safeZero(c.getHorasTotalPresencial());
            acc311[1] += safeZero(c.getHorasTotalDistancia());

            int[] acc312 = suma312PorArea.computeIfAbsent(areaId, k -> new int[2]);
            acc312[0] += safeZero(c.getHorasPracticaPresencial());
            acc312[1] += safeZero(c.getHorasPracticaDistancia());
        }

        int totalPres311 = 0;
        int totalDist311 = 0;
        boolean cumpleGlobal = true;
        List<CumplimientoCargaHorariaPlanDto.FilaCumplimientoDto> filas311 = new ArrayList<>();
        for (EstandarAreaCargaHorariaJpaEntity area : areas311) {
            int[] acc = suma311PorArea.getOrDefault(area.getId(), new int[2]);
            int pres = acc[0];
            int dist = acc[1];
            int total = pres + dist;
            totalPres311 += pres;
            totalDist311 += dist;

            Integer min = area.getHorasMinimasCarrera();
            boolean tieneMin = min != null && min > 0;
            Integer dif = tieneMin ? (total - min) : null;
            boolean cumple = !tieneMin || total >= min;
            if (!cumple) {
                cumpleGlobal = false;
            }
            filas311.add(new CumplimientoCargaHorariaPlanDto.FilaCumplimientoDto(
                    area.getId(),
                    area.getCodigo(),
                    area.getNombre(),
                    pres,
                    dist,
                    total,
                    tieneMin ? min : null,
                    dif,
                    tieneMin,
                    cumple
            ));
        }
        int totalAcum311 = totalPres311 + totalDist311;
        Integer minTotal311 = hab311 && estandar.getHorasMinimasTotal311() != null && estandar.getHorasMinimasTotal311() > 0
                ? estandar.getHorasMinimasTotal311()
                : null;
        boolean tieneMinTotal311 = minTotal311 != null;
        Integer difTotal311 = tieneMinTotal311 ? (totalAcum311 - minTotal311) : null;
        boolean cumpleTotal311 = !tieneMinTotal311 || totalAcum311 >= minTotal311;
        if (!cumpleTotal311) {
            cumpleGlobal = false;
        }

        int totalPres312 = 0;
        int totalDist312 = 0;
        List<CumplimientoCargaHorariaPlanDto.FilaCumplimientoDto> filas312 = new ArrayList<>();
        for (EstandarAreaCargaHorariaJpaEntity area : areas312) {
            int[] acc = suma312PorArea.getOrDefault(area.getId(), new int[2]);
            int pres = acc[0];
            int dist = acc[1];
            int total = pres + dist;
            totalPres312 += pres;
            totalDist312 += dist;

            Integer min = area.getHorasMinimasCarrera();
            boolean tieneMin = min != null && min > 0;
            Integer dif = tieneMin ? (total - min) : null;
            boolean cumple = !tieneMin || total >= min;
            if (!cumple) {
                cumpleGlobal = false;
            }
            filas312.add(new CumplimientoCargaHorariaPlanDto.FilaCumplimientoDto(
                    area.getId(),
                    area.getCodigo(),
                    area.getEtiquetaPractica() != null && !area.getEtiquetaPractica().isBlank()
                            ? area.getEtiquetaPractica()
                            : area.getNombre(),
                    pres,
                    dist,
                    total,
                    tieneMin ? min : null,
                    dif,
                    tieneMin,
                    cumple
            ));
        }
        int totalAcum312 = totalPres312 + totalDist312;
        Integer minTotal312 = hab312 && estandar.getHorasMinimasTotal312() != null && estandar.getHorasMinimasTotal312() > 0
                ? estandar.getHorasMinimasTotal312()
                : null;
        boolean tieneMinTotal312 = minTotal312 != null;
        Integer difTotal312 = tieneMinTotal312 ? (totalAcum312 - minTotal312) : null;
        boolean cumpleTotal312 = !tieneMinTotal312 || totalAcum312 >= minTotal312;
        if (!cumpleTotal312) {
            cumpleGlobal = false;
        }

        return new CumplimientoCargaHorariaPlanDto(
                plan.getId(),
                plan.getNombrePlan(),
                estandar.getId(),
                estandar.getNombre(),
                asignaturasSeDicta.size(),
                asignaturasConSec3,
                hab311,
                modo311,
                totalPres311,
                totalDist311,
                totalAcum311,
                minTotal311,
                difTotal311,
                tieneMinTotal311,
                cumpleTotal311,
                filas311,
                hab312,
                modo312,
                totalPres312,
                totalDist312,
                totalAcum312,
                minTotal312,
                difTotal312,
                tieneMinTotal312,
                cumpleTotal312,
                filas312,
                cumpleGlobal
        );
    }

    private void guardarEvaluacion22(
            ActividadCurricularPlanJpaEntity asig,
            EstandarJpaEntity estandar,
            List<FichaEstandarActividadDto.Respuesta22Dto> respuestasReq
    ) {
        evaluacion22Repository.deleteByActividadCurricularId(asig.getId());
        evaluacion22Repository.flush();

        if (estandar.getTipoSeccion22() == TipoSeccionEstandar22.NINGUNA
                || estandar.getTipoEvaluacion22() == TipoEvaluacionEstandar22.NINGUNA) {
            return;
        }

        Set<UUID> enabledItemIds = resolveEnabledItemIds(estandar.getId());
        if (enabledItemIds.isEmpty()) {
            return;
        }

        Map<UUID, EstandarItem22JpaEntity> itemsById = itemRepository.findByGrupoEstandarIdOrderByOrdenAsc(estandar.getId())
                .stream()
                .filter(i -> enabledItemIds.contains(i.getId()))
                .collect(Collectors.toMap(EstandarItem22JpaEntity::getId, Function.identity()));
        Map<UUID, EstandarEscalaEvaluacionJpaEntity> escalasById = escalaRepository.findByEstandarIdOrderByOrdenAsc(estandar.getId())
                .stream()
                .collect(Collectors.toMap(EstandarEscalaEvaluacionJpaEntity::getId, Function.identity()));

        List<FichaEstandarActividadDto.Respuesta22Dto> raw = respuestasReq != null ? respuestasReq : List.of();
        Map<String, ActividadCurricularEvaluacion22JpaEntity> deduplicated = new LinkedHashMap<>();
        Map<UUID, UUID> singleScalePerItem = new HashMap<>();

        for (FichaEstandarActividadDto.Respuesta22Dto r : raw) {
            if (r == null || r.estandarItemId() == null || r.escalaEvaluacionId() == null) {
                continue;
            }
            EstandarItem22JpaEntity item = itemsById.get(r.estandarItemId());
            if (item == null) {
                throw new DomainValidationException(
                        "Se envió una respuesta para un ítem que no pertenece a un grupo habilitado del estándar.");
            }
            EstandarEscalaEvaluacionJpaEntity escala = escalasById.get(r.escalaEvaluacionId());
            if (escala == null) {
                throw new DomainValidationException(
                        "Se envió una escala de evaluación que no pertenece al estándar del plan.");
            }
            if (estandar.getTipoEvaluacion22() == TipoEvaluacionEstandar22.ESCALA_UNICA) {
                UUID prev = singleScalePerItem.putIfAbsent(item.getId(), escala.getId());
                if (prev != null && !prev.equals(escala.getId())) {
                    throw new DomainValidationException(
                            "En evaluación por escala única cada ítem solo admite una opción seleccionada.");
                }
            }
            String pairKey = item.getId() + "::" + escala.getId();
            if (!deduplicated.containsKey(pairKey)) {
                ActividadCurricularEvaluacion22JpaEntity entity = new ActividadCurricularEvaluacion22JpaEntity();
                entity.setId(UUID.randomUUID());
                entity.setActividadCurricular(asig);
                entity.setEstandarItem(item);
                entity.setEscalaEvaluacion(escala);
                entity.setCreadoEn(Instant.now());
                deduplicated.put(pairKey, entity);
            }
        }

        if (estandar.getTipoEvaluacion22() == TipoEvaluacionEstandar22.ESCALA_UNICA) {
            long missing = enabledItemIds.stream().filter(id -> !singleScalePerItem.containsKey(id)).count();
            if (missing > 0) {
                throw new DomainValidationException(
                        "Debe seleccionar una opción de la escala para todos los ítems habilitados de la Sección 2.2 ("
                                + missing + " sin responder).");
            }
        } else if (deduplicated.isEmpty()) {
            throw new DomainValidationException(
                    "Debe marcar al menos una competencia o habilidad en la Sección 2.2.");
        }

        evaluacion22Repository.saveAll(deduplicated.values());
    }

    private void guardarCargaHoraria3(
            ActividadCurricularPlanJpaEntity asig,
            EstandarJpaEntity estandar,
            List<FichaEstandarActividadDto.CargaAreaDto> cargasReq,
            FichaEstandarActividadDto.ResumenCargaDto resumenReq
    ) {
        boolean hab311 = esHabilitado311(estandar);
        boolean hab312 = esHabilitado312(estandar);
        List<EstandarAreaCargaHorariaJpaEntity> areasHabilitadas =
                areaRepository.findByEstandarIdOrderByOrdenAsc(estandar.getId()).stream()
                        .filter(a -> Boolean.TRUE.equals(a.getHabilitada()))
                        .filter(a -> {
                            TipoSeccionCargaHoraria t = a.getTipoSeccion() != null
                                    ? a.getTipoSeccion()
                                    : TipoSeccionCargaHoraria.CARGA_TOTAL_311;
                            return (t == TipoSeccionCargaHoraria.CARGA_TOTAL_311 && hab311)
                                    || (t == TipoSeccionCargaHoraria.PRACTICA_312 && hab312);
                        })
                        .toList();
        Map<UUID, EstandarAreaCargaHorariaJpaEntity> areasById = areasHabilitadas.stream()
                .collect(Collectors.toMap(EstandarAreaCargaHorariaJpaEntity::getId, Function.identity()));

        cargaAreaRepository.deleteByActividadCurricularId(asig.getId());
        cargaAreaRepository.flush();

        Map<UUID, FichaEstandarActividadDto.CargaAreaDto> inputByAreaId = new HashMap<>();
        if (cargasReq != null) {
            for (FichaEstandarActividadDto.CargaAreaDto dto : cargasReq) {
                if (dto == null || dto.areaCargaHorariaId() == null) {
                    continue;
                }
                if (!areasById.containsKey(dto.areaCargaHorariaId())) {
                    throw new DomainValidationException(
                            "Se envió carga horaria para un área no habilitada en el estándar.");
                }
                inputByAreaId.put(dto.areaCargaHorariaId(), dto);
            }
        }

        List<EstandarAreaCargaHorariaJpaEntity> areas311 = areasHabilitadas.stream()
                .filter(a -> a.getTipoSeccion() == null || a.getTipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311)
                .toList();
        List<EstandarAreaCargaHorariaJpaEntity> areas312 = areasHabilitadas.stream()
                .filter(a -> a.getTipoSeccion() == TipoSeccionCargaHoraria.PRACTICA_312)
                .toList();

        int total311Pres = 0;
        int total311Dist = 0;
        int total312Pres = 0;
        int total312Dist = 0;
        Map<String, Integer> totalPresPorCodigo = new HashMap<>();
        Map<String, Integer> totalDistPorCodigo = new HashMap<>();
        List<ActividadCurricularCargaHorariaAreaJpaEntity> toSave = new ArrayList<>();

        for (EstandarAreaCargaHorariaJpaEntity area : areas311) {
            FichaEstandarActividadDto.CargaAreaDto dto = inputByAreaId.get(area.getId());
            int totalPres = nonNegativeInt(dto != null ? dto.horasTotalPresencial() : 0,
                    "horas totales presenciales (" + area.getNombre() + ")");
            int totalDist = nonNegativeInt(dto != null ? dto.horasTotalDistancia() : 0,
                    "horas totales a distancia (" + area.getNombre() + ")");

            total311Pres += totalPres;
            total311Dist += totalDist;
            if (area.getCodigo() != null) {
                String codKey = area.getCodigo().toUpperCase(Locale.ROOT);
                totalPresPorCodigo.merge(codKey, totalPres, Integer::sum);
                totalDistPorCodigo.merge(codKey, totalDist, Integer::sum);
            }

            ActividadCurricularCargaHorariaAreaJpaEntity entity = new ActividadCurricularCargaHorariaAreaJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setActividadCurricular(asig);
            entity.setAreaCargaHoraria(area);
            entity.setHorasTotalPresencial(totalPres);
            entity.setHorasTotalDistancia(totalDist);
            entity.setHorasPracticaPresencial(0);
            entity.setHorasPracticaDistancia(0);
            toSave.add(entity);
        }

        for (EstandarAreaCargaHorariaJpaEntity area : areas312) {
            FichaEstandarActividadDto.CargaAreaDto dto = inputByAreaId.get(area.getId());
            int pracPres = nonNegativeInt(dto != null ? dto.horasPracticaPresencial() : 0,
                    "horas prácticas presenciales (" + area.getNombre() + ")");
            int pracDist = nonNegativeInt(dto != null ? dto.horasPracticaDistancia() : 0,
                    "horas prácticas a distancia (" + area.getNombre() + ")");

            String codKey = area.getCodigo() != null ? area.getCodigo().toUpperCase(Locale.ROOT) : "";
            if (totalPresPorCodigo.containsKey(codKey)) {
                int maxPres = totalPresPorCodigo.get(codKey);
                int maxDist = totalDistPorCodigo.getOrDefault(codKey, 0);
                if (pracPres > maxPres) {
                    throw new DomainValidationException(
                            "Las horas de formación práctica presencial (" + pracPres
                                    + ") no pueden superar las horas totales presenciales (" + maxPres
                                    + ") en el área '" + area.getNombre() + "'.");
                }
                if (pracDist > maxDist) {
                    throw new DomainValidationException(
                            "Las horas de formación práctica a distancia (" + pracDist
                                    + ") no pueden superar las horas totales a distancia (" + maxDist
                                    + ") en el área '" + area.getNombre() + "'.");
                }
            }

            total312Pres += pracPres;
            total312Dist += pracDist;

            ActividadCurricularCargaHorariaAreaJpaEntity entity = new ActividadCurricularCargaHorariaAreaJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setActividadCurricular(asig);
            entity.setAreaCargaHoraria(area);
            entity.setHorasTotalPresencial(0);
            entity.setHorasTotalDistancia(0);
            entity.setHorasPracticaPresencial(pracPres);
            entity.setHorasPracticaDistancia(pracDist);
            toSave.add(entity);
        }

        int total311 = total311Pres + total311Dist;
        int total312 = total312Pres + total312Dist;
        int horasReloj = asig.getHorasReloj() != null ? asig.getHorasReloj() : 0;
        if (!areas311.isEmpty() && horasReloj > 0 && total311 != horasReloj) {
            throw new DomainValidationException(
                    "La suma de cargas horarias del punto 3.1.1 (" + total311
                            + " h) debe coincidir con la carga horaria total de la asignatura (" + horasReloj + " h).");
        }
        if (!areas311.isEmpty() && (total312Pres > total311Pres || total312Dist > total311Dist)) {
            throw new DomainValidationException(
                    "Las horas de formación práctica del punto 3.1.2 (Presencial: " + total312Pres
                            + " h, Distancia: " + total312Dist
                            + " h) no pueden superar la carga horaria total declarada en el punto 3.1.1 (Presencial: "
                            + total311Pres + " h, Distancia: " + total311Dist + " h).");
        }

        String ambitos = resumenReq != null ? trimToNull(resumenReq.ambitosFormacionPractica()) : null;
        if (total312 > 0 && ambitos == null) {
            throw new DomainValidationException(
                    "Debe especificar los ámbitos de formación práctica en el punto 3.1.3 cuando declara horas prácticas en el punto 3.1.2.");
        }

        int semTotalPres = nonNegativeInt(resumenReq != null ? resumenReq.horasSemanalTotalPresencial() : 0,
                "carga horaria semanal total presencial");
        int semTotalDist = nonNegativeInt(resumenReq != null ? resumenReq.horasSemanalTotalDistancia() : 0,
                "carga horaria semanal total a distancia");
        int semPracPres = nonNegativeInt(resumenReq != null ? resumenReq.horasSemanalPracticaPresencial() : 0,
                "carga horaria semanal práctica presencial");
        int semPracDist = nonNegativeInt(resumenReq != null ? resumenReq.horasSemanalPracticaDistancia() : 0,
                "carga horaria semanal práctica a distancia");

        if (horasReloj > 0 && (semTotalPres + semTotalDist) <= 0) {
            throw new DomainValidationException(
                    "Debe indicar la carga horaria semanal total (punto 3.2) mayor a 0.");
        }
        if (semPracPres > semTotalPres || semPracDist > semTotalDist) {
            throw new DomainValidationException(
                    "La carga horaria semanal destinada a formación práctica no puede superar la carga horaria semanal total.");
        }

        if (!toSave.isEmpty()) {
            cargaAreaRepository.saveAll(toSave);
        }

        ActividadCurricularCargaHorariaResumenJpaEntity resumen = cargaResumenRepository.findById(asig.getId())
                .orElseGet(ActividadCurricularCargaHorariaResumenJpaEntity::new);
        resumen.setActividadCurricularId(asig.getId());
        resumen.setAmbitosFormacionPractica(ambitos);
        resumen.setHorasSemanalTotalPresencial(semTotalPres);
        resumen.setHorasSemanalTotalDistancia(semTotalDist);
        resumen.setHorasSemanalPracticaPresencial(semPracPres);
        resumen.setHorasSemanalPracticaDistancia(semPracDist);
        resumen.setActualizadoEn(Instant.now());
        cargaResumenRepository.save(resumen);

        if ((semTotalPres + semTotalDist) > 0) {
            asig.setHorasSemanales(semTotalPres + semTotalDist);
            asignaturaRepository.save(asig);
        }
    }

    private FichaEstandarActividadDto buildFichaDto(
            PlanEstudioFt001JpaEntity plan,
            ActividadCurricularPlanJpaEntity asig,
            EstandarJpaEntity estandar
    ) {
        EstandarDetalleDto estandarDetalle = estandarService.toDetalleDto(estandar);

        List<ActividadCurricularEvaluacion22JpaEntity> evalEntities =
                evaluacion22Repository.findByActividadCurricularId(asig.getId());
        List<FichaEstandarActividadDto.Respuesta22Dto> respuestas22 = evalEntities.stream()
                .map(e -> new FichaEstandarActividadDto.Respuesta22Dto(
                        e.getEstandarItem().getId(),
                        e.getEscalaEvaluacion().getId()))
                .toList();

        List<ActividadCurricularCargaHorariaAreaJpaEntity> cargaEntities =
                cargaAreaRepository.findByActividadCurricularId(asig.getId());
        List<FichaEstandarActividadDto.CargaAreaDto> cargasPorArea = cargaEntities.stream()
                .map(c -> new FichaEstandarActividadDto.CargaAreaDto(
                        c.getAreaCargaHoraria().getId(),
                        c.getHorasTotalPresencial(),
                        c.getHorasTotalDistancia(),
                        c.getHorasPracticaPresencial(),
                        c.getHorasPracticaDistancia()))
                .toList();

        ActividadCurricularCargaHorariaResumenJpaEntity resumenEntity =
                cargaResumenRepository.findById(asig.getId()).orElse(null);
        FichaEstandarActividadDto.ResumenCargaDto resumenDto = resumenEntity != null
                ? new FichaEstandarActividadDto.ResumenCargaDto(
                        resumenEntity.getAmbitosFormacionPractica(),
                        resumenEntity.getHorasSemanalTotalPresencial(),
                        resumenEntity.getHorasSemanalTotalDistancia(),
                        resumenEntity.getHorasSemanalPracticaPresencial(),
                        resumenEntity.getHorasSemanalPracticaDistancia())
                : new FichaEstandarActividadDto.ResumenCargaDto(null, 0, 0, 0, 0);

        Set<UUID> itemsHabilitados = estandarDetalle.grupos22().stream()
                .filter(EstandarDetalleDto.Grupo22Dto::habilitado)
                .flatMap(g -> g.items().stream())
                .map(EstandarDetalleDto.Item22Dto::id)
                .collect(Collectors.toSet());
        Set<UUID> escalasValidas = estandarDetalle.escalas().stream()
                .map(EstandarDetalleDto.EscalaDto::id)
                .collect(Collectors.toSet());
        boolean hab311 = esHabilitado311(estandar);
        boolean hab312 = esHabilitado312(estandar);
        List<AreaMeta> areasHabilitadasMeta = estandarDetalle.areasCargaHoraria().stream()
                .filter(EstandarDetalleDto.AreaCargaHorariaDto::habilitada)
                .filter(a -> {
                    TipoSeccionCargaHoraria t = a.tipoSeccion() != null
                            ? a.tipoSeccion()
                            : TipoSeccionCargaHoraria.CARGA_TOTAL_311;
                    return (t == TipoSeccionCargaHoraria.CARGA_TOTAL_311 && hab311)
                            || (t == TipoSeccionCargaHoraria.PRACTICA_312 && hab312);
                })
                .map(a -> new AreaMeta(
                        a.id(),
                        a.tipoSeccion() != null ? a.tipoSeccion() : TipoSeccionCargaHoraria.CARGA_TOTAL_311,
                        a.codigo()))
                .toList();

        boolean sec22Completa = evaluarSeccion22Completa(estandar, itemsHabilitados, escalasValidas, evalEntities);
        boolean sec3Completa = evaluarSeccion3Completa(asig, areasHabilitadasMeta, cargaEntities, resumenEntity);

        return new FichaEstandarActividadDto(
                plan.getId(),
                asig.getId(),
                asig.getCodigo(),
                asig.getNombre(),
                asig.getHorasReloj(),
                asig.getDocenteResponsableId(),
                asig.getDocenteResponsableNombre(),
                puedeEditarFicha(asig),
                estandarDetalle,
                respuestas22,
                cargasPorArea,
                resumenDto,
                sec22Completa,
                sec3Completa,
                sec22Completa && sec3Completa
        );
    }

    private boolean evaluarSeccion22Completa(
            EstandarJpaEntity estandar,
            Set<UUID> itemsHabilitados,
            Set<UUID> escalasValidas,
            List<ActividadCurricularEvaluacion22JpaEntity> evalEntities
    ) {
        if (estandar.getTipoSeccion22() == TipoSeccionEstandar22.NINGUNA
                || estandar.getTipoEvaluacion22() == TipoEvaluacionEstandar22.NINGUNA
                || itemsHabilitados.isEmpty()) {
            return true;
        }
        List<ActividadCurricularEvaluacion22JpaEntity> validas = evalEntities.stream()
                .filter(e -> e.getEstandarItem() != null
                        && itemsHabilitados.contains(e.getEstandarItem().getId())
                        && e.getEscalaEvaluacion() != null
                        && escalasValidas.contains(e.getEscalaEvaluacion().getId()))
                .toList();

        if (estandar.getTipoEvaluacion22() == TipoEvaluacionEstandar22.ESCALA_UNICA) {
            Set<UUID> respondidos = validas.stream()
                    .map(e -> e.getEstandarItem().getId())
                    .collect(Collectors.toSet());
            return respondidos.containsAll(itemsHabilitados);
        }
        return !validas.isEmpty();
    }

    private boolean evaluarSeccion3Completa(
            ActividadCurricularPlanJpaEntity asig,
            List<AreaMeta> areasHabilitadas,
            List<ActividadCurricularCargaHorariaAreaJpaEntity> cargaEntities,
            ActividadCurricularCargaHorariaResumenJpaEntity resumen
    ) {
        if (areasHabilitadas.isEmpty()) {
            return true;
        }
        Map<UUID, AreaMeta> metaById = areasHabilitadas.stream()
                .collect(Collectors.toMap(AreaMeta::id, Function.identity(), (a, b) -> a));
        boolean tiene311 = areasHabilitadas.stream()
                .anyMatch(a -> a.tipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311);

        int horasReloj = asig.getHorasReloj() != null ? asig.getHorasReloj() : 0;
        int total311Pres = 0;
        int total311Dist = 0;
        int total312Pres = 0;
        int total312Dist = 0;
        Map<String, Integer> totalPresPorCodigo = new HashMap<>();
        Map<String, Integer> totalDistPorCodigo = new HashMap<>();
        List<ActividadCurricularCargaHorariaAreaJpaEntity> filas312Respondidas = new ArrayList<>();

        for (ActividadCurricularCargaHorariaAreaJpaEntity c : cargaEntities) {
            if (c.getAreaCargaHoraria() == null) {
                continue;
            }
            AreaMeta meta = metaById.get(c.getAreaCargaHoraria().getId());
            if (meta == null) {
                continue;
            }
            if (meta.tipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311) {
                int tp = safeZero(c.getHorasTotalPresencial());
                int td = safeZero(c.getHorasTotalDistancia());
                total311Pres += tp;
                total311Dist += td;
                if (meta.codigo() != null) {
                    String codKey = meta.codigo().toUpperCase(Locale.ROOT);
                    totalPresPorCodigo.merge(codKey, tp, Integer::sum);
                    totalDistPorCodigo.merge(codKey, td, Integer::sum);
                }
            } else {
                int pp = safeZero(c.getHorasPracticaPresencial());
                int pd = safeZero(c.getHorasPracticaDistancia());
                total312Pres += pp;
                total312Dist += pd;
                filas312Respondidas.add(c);
            }
        }

        for (ActividadCurricularCargaHorariaAreaJpaEntity c : filas312Respondidas) {
            AreaMeta meta = metaById.get(c.getAreaCargaHoraria().getId());
            String codKey = meta != null && meta.codigo() != null ? meta.codigo().toUpperCase(Locale.ROOT) : "";
            if (totalPresPorCodigo.containsKey(codKey)) {
                if (safeZero(c.getHorasPracticaPresencial()) > totalPresPorCodigo.get(codKey)
                        || safeZero(c.getHorasPracticaDistancia()) > totalDistPorCodigo.getOrDefault(codKey, 0)) {
                    return false;
                }
            }
        }

        int total311 = total311Pres + total311Dist;
        int total312 = total312Pres + total312Dist;

        if (tiene311 && horasReloj > 0 && total311 != horasReloj) {
            return false;
        }
        if (tiene311 && (total312Pres > total311Pres || total312Dist > total311Dist)) {
            return false;
        }
        if (resumen == null) {
            return false;
        }
        if (total312 > 0 && trimToNull(resumen.getAmbitosFormacionPractica()) == null) {
            return false;
        }
        int semTP = safeZero(resumen.getHorasSemanalTotalPresencial());
        int semTD = safeZero(resumen.getHorasSemanalTotalDistancia());
        int semPP = safeZero(resumen.getHorasSemanalPracticaPresencial());
        int semPD = safeZero(resumen.getHorasSemanalPracticaDistancia());
        if (horasReloj > 0 && (semTP + semTD) <= 0) {
            return false;
        }
        return semPP <= semTP && semPD <= semTD;
    }

    private Set<UUID> resolveEnabledItemIds(UUID estandarId) {
        Set<UUID> enabledGroupIds = grupoRepository.findByEstandarIdOrderByOrdenAsc(estandarId).stream()
                .filter(g -> Boolean.TRUE.equals(g.getHabilitado()))
                .map(EstandarGrupo22JpaEntity::getId)
                .collect(Collectors.toSet());
        if (enabledGroupIds.isEmpty()) {
            return Set.of();
        }
        return itemRepository.findByGrupoIdInOrderByOrdenAsc(enabledGroupIds).stream()
                .map(EstandarItem22JpaEntity::getId)
                .collect(Collectors.toSet());
    }

    private boolean puedeEditarFicha(ActividadCurricularPlanJpaEntity asig) {
        if (authenticatedUserProvider == null) {
            return true;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return true;
        }
        User user = userOpt.get();
        if (esGestor(user)) {
            return true;
        }
        return tieneRol(user, "DOCENTE")
                && asig.getDocenteResponsableId() != null
                && asig.getDocenteResponsableId().equals(user.getId());
    }

    private boolean esUsuarioGestorOAnonimo() {
        if (authenticatedUserProvider == null) {
            return true;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        return userOpt.map(this::esGestor).orElse(true);
    }

    private boolean esGestor(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r != null && r.getCode() != null && ROLES_GESTORES.contains(r.getCode().toUpperCase(Locale.ROOT)));
    }

    private boolean tieneRol(User user, String code) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r != null && code.equalsIgnoreCase(r.getCode()));
    }

    private PlanEstudioFt001JpaEntity findPlanOrThrow(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + planId));
    }

    private ActividadCurricularPlanJpaEntity findAsignaturaDePlanOrThrow(UUID planId, UUID asigId) {
        ActividadCurricularPlanJpaEntity asig = asignaturaRepository.findById(asigId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura ID: " + asigId));
        if (asig.getPlanEstudio() == null || !planId.equals(asig.getPlanEstudio().getId())) {
            throw new DomainValidationException("La asignatura no pertenece al plan de estudio indicado.");
        }
        return asig;
    }

    private EstandarJpaEntity requireEstandar(PlanEstudioFt001JpaEntity plan) {
        if (plan.getEstandar() == null) {
            throw new DomainValidationException(
                    "El plan de estudio '" + plan.getNombrePlan() + "' no tiene un estándar asignado.");
        }
        return plan.getEstandar();
    }

    private boolean esHabilitado311(EstandarJpaEntity estandar) {
        return estandar.getHabilitar311() == null || Boolean.TRUE.equals(estandar.getHabilitar311());
    }

    private boolean esHabilitado312(EstandarJpaEntity estandar) {
        return estandar.getHabilitar312() == null || Boolean.TRUE.equals(estandar.getHabilitar312());
    }

    private ModoSeccion311 resolverModo311(EstandarJpaEntity estandar) {
        if (estandar.getModo311() != null) {
            return estandar.getModo311();
        }
        return estandar.getTipoSeccion22() == TipoSeccionEstandar22.EJES
                ? ModoSeccion311.BLOQUES_CURRICULARES
                : ModoSeccion311.AREAS_TEMATICAS;
    }

    private ModoSeccion312 resolverModo312(EstandarJpaEntity estandar) {
        if (estandar.getModo312() != null) {
            return estandar.getModo312();
        }
        return estandar.getTipoSeccion22() == TipoSeccionEstandar22.EJES
                ? ModoSeccion312.MODALIDADES_PRACTICAS
                : ModoSeccion312.AREAS_PRACTICAS;
    }

    private int nonNegativeInt(Integer val, String fieldLabel) {
        int n = val != null ? val : 0;
        if (n < 0) {
            throw new DomainValidationException("El campo '" + fieldLabel + "' no puede ser negativo.");
        }
        return n;
    }

    private int safeZero(Integer val) {
        return val != null ? val : 0;
    }

    private String trimToNull(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        return val.trim();
    }
}
