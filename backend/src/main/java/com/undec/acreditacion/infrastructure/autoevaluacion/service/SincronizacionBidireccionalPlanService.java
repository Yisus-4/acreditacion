package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.estandares.persistence.ActividadCurricularCargaHorariaAreaJpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.ActividadCurricularCargaHorariaResumenJpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.ActividadCurricularEvaluacion22JpaEntity;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataActividadCurricularCargaHorariaAreaRepository;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataActividadCurricularCargaHorariaResumenRepository;
import com.undec.acreditacion.infrastructure.estandares.persistence.SpringDataActividadCurricularEvaluacion22Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class SincronizacionBidireccionalPlanService {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository;
    private final SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository;
    private final SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository;

    public SincronizacionBidireccionalPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository
    ) {
        this(planRepository, asignaturaRepository, null, null, null);
    }

    @Autowired
    public SincronizacionBidireccionalPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository,
            SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository,
            SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository
    ) {
        this.planRepository = Objects.requireNonNull(planRepository);
        this.asignaturaRepository = Objects.requireNonNull(asignaturaRepository);
        this.evaluacion22Repository = evaluacion22Repository;
        this.cargaAreaRepository = cargaAreaRepository;
        this.cargaResumenRepository = cargaResumenRepository;
    }

    public void propagarCreacionAsignatura(
            PlanEstudioFt001JpaEntity planOrigen,
            ActividadCurricularPlanJpaEntity asigCreada
    ) {
        if (planOrigen == null || asigCreada == null || asigCreada.getId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(planOrigen.getEsMaestro())) {
            if (asigCreada.getEstadoDictado().esInactiva()) {
                return;
            }
            propagarNuevaAsignaturaMaestraAClones(planOrigen.getId(), asigCreada, null);
            return;
        }

        if (planOrigen.getOrigenMaestroId() == null || asigCreada.getOrigenMaestroId() != null) {
            return;
        }

        PlanEstudioFt001JpaEntity masterPlan = planRepository.findById(planOrigen.getOrigenMaestroId()).orElse(null);
        if (masterPlan == null) {
            return;
        }

        Integer maxOrden = asignaturaRepository.findMaxNumeroOrdenByPlanEstudioId(masterPlan.getId());
        int nextOrden = (maxOrden != null ? maxOrden : 0) + 1;

        String codigoMaster = asigCreada.getCodigo();
        if (codigoMaster != null && !codigoMaster.isBlank()
                && asignaturaRepository.existsByPlanEstudioIdAndCodigo(masterPlan.getId(), codigoMaster.trim())) {
            codigoMaster = null;
        }

        ActividadCurricularPlanJpaEntity masterAsig = clonarAsignatura(
                asigCreada, masterPlan, null, codigoMaster, nextOrden);
        masterAsig = asignaturaRepository.save(masterAsig);

        asigCreada.setOrigenMaestroId(masterAsig.getId());
        asignaturaRepository.save(asigCreada);

        recalcularHorasPlan(masterPlan);
        propagarNuevaAsignaturaMaestraAClones(masterPlan.getId(), masterAsig, planOrigen.getId());
    }

    public void propagarActualizacionAsignatura(
            PlanEstudioFt001JpaEntity planOrigen,
            ActividadCurricularPlanJpaEntity asigActualizada
    ) {
        if (planOrigen == null || asigActualizada == null || asigActualizada.getId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(planOrigen.getEsMaestro())) {
            List<PlanEstudioFt001JpaEntity> clones = planRepository.findByOrigenMaestroIdAndActivoTrue(planOrigen.getId());
            for (PlanEstudioFt001JpaEntity clonePlan : clones) {
                ActividadCurricularPlanJpaEntity cloneAsig = asignaturaRepository
                        .findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), asigActualizada.getId())
                        .orElse(null);

                if (cloneAsig != null) {
                    copiarCamposAsignatura(asigActualizada, cloneAsig);
                    asignaturaRepository.save(cloneAsig);
                    recalcularHorasPlan(clonePlan);
                } else if (!asigActualizada.getEstadoDictado().esInactiva()) {
                    Integer maxOrden = asignaturaRepository.findMaxNumeroOrdenByPlanEstudioId(clonePlan.getId());
                    int orden = asigActualizada.getNumeroOrden() != null
                            ? asigActualizada.getNumeroOrden()
                            : (maxOrden != null ? maxOrden : 0) + 1;
                    ActividadCurricularPlanJpaEntity nuevoClon = clonarAsignatura(
                            asigActualizada, clonePlan, asigActualizada.getId(), asigActualizada.getCodigo(), orden);
                    asignaturaRepository.save(nuevoClon);
                    recalcularHorasPlan(clonePlan);
                }
            }
            return;
        }

        if (asigActualizada.getOrigenMaestroId() == null) {
            return;
        }

        UUID masterAsigId = asigActualizada.getOrigenMaestroId();
        ActividadCurricularPlanJpaEntity masterAsig = asignaturaRepository.findById(masterAsigId).orElse(null);
        if (masterAsig == null) {
            return;
        }

        copiarCamposAsignatura(asigActualizada, masterAsig);
        asignaturaRepository.save(masterAsig);
        if (masterAsig.getPlanEstudio() != null) {
            recalcularHorasPlan(masterAsig.getPlanEstudio());
        }

        List<ActividadCurricularPlanJpaEntity> hermanos = asignaturaRepository.findByOrigenMaestroId(masterAsigId);
        for (ActividadCurricularPlanJpaEntity hermano : hermanos) {
            if (!hermano.getId().equals(asigActualizada.getId())) {
                copiarCamposAsignatura(asigActualizada, hermano);
                asignaturaRepository.save(hermano);
                if (hermano.getPlanEstudio() != null) {
                    recalcularHorasPlan(hermano.getPlanEstudio());
                }
            }
        }
    }

    public void propagarEliminacionAsignatura(
            PlanEstudioFt001JpaEntity planOrigen,
            ActividadCurricularPlanJpaEntity asigEliminada
    ) {
        if (planOrigen == null || asigEliminada == null || asigEliminada.getId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(planOrigen.getEsMaestro())) {
            List<ActividadCurricularPlanJpaEntity> clones = asignaturaRepository.findByOrigenMaestroId(asigEliminada.getId());
            for (ActividadCurricularPlanJpaEntity cloneAsig : clones) {
                PlanEstudioFt001JpaEntity clonePlan = cloneAsig.getPlanEstudio();
                asignaturaRepository.delete(cloneAsig);
                if (clonePlan != null) {
                    recalcularHorasPlan(clonePlan);
                }
            }
            return;
        }

        if (asigEliminada.getOrigenMaestroId() == null) {
            return;
        }

        UUID masterAsigId = asigEliminada.getOrigenMaestroId();
        ActividadCurricularPlanJpaEntity masterAsig = asignaturaRepository.findById(masterAsigId).orElse(null);
        if (masterAsig == null) {
            return;
        }

        List<ActividadCurricularPlanJpaEntity> hermanos = asignaturaRepository.findByOrigenMaestroId(masterAsigId);
        for (ActividadCurricularPlanJpaEntity hermano : hermanos) {
            if (!hermano.getId().equals(asigEliminada.getId())) {
                PlanEstudioFt001JpaEntity clonePlan = hermano.getPlanEstudio();
                asignaturaRepository.delete(hermano);
                if (clonePlan != null) {
                    recalcularHorasPlan(clonePlan);
                }
            }
        }

        PlanEstudioFt001JpaEntity masterPlan = masterAsig.getPlanEstudio();
        asignaturaRepository.delete(masterAsig);
        if (masterPlan != null) {
            recalcularHorasPlan(masterPlan);
        }
    }

    public void propagarActualizacionPlan(PlanEstudioFt001JpaEntity planOrigen) {
        if (planOrigen == null || planOrigen.getId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(planOrigen.getEsMaestro())) {
            List<PlanEstudioFt001JpaEntity> clones = planRepository.findByOrigenMaestroIdAndActivoTrue(planOrigen.getId());
            for (PlanEstudioFt001JpaEntity clone : clones) {
                copiarMetadatosPlan(planOrigen, clone);
                planRepository.save(clone);
            }
            return;
        }

        if (planOrigen.getOrigenMaestroId() == null) {
            return;
        }

        PlanEstudioFt001JpaEntity master = planRepository.findById(planOrigen.getOrigenMaestroId()).orElse(null);
        if (master == null) {
            return;
        }

        copiarMetadatosPlan(planOrigen, master);
        planRepository.save(master);

        List<PlanEstudioFt001JpaEntity> hermanos = planRepository.findByOrigenMaestroIdAndActivoTrue(master.getId());
        for (PlanEstudioFt001JpaEntity hermano : hermanos) {
            if (!hermano.getId().equals(planOrigen.getId())) {
                copiarMetadatosPlan(planOrigen, hermano);
                planRepository.save(hermano);
            }
        }
    }

    public void vincularOCrearMaestroParaPlanDeProceso(PlanEstudioFt001JpaEntity planProceso) {
        if (planProceso == null
                || planProceso.getProcesoId() == null
                || Boolean.TRUE.equals(planProceso.getEsMaestro())
                || planProceso.getOrigenMaestroId() != null
                || planProceso.getCarrera() == null) {
            return;
        }

        Instant now = Instant.now();
        PlanEstudioFt001JpaEntity master = new PlanEstudioFt001JpaEntity();
        master.setId(UUID.randomUUID());
        master.setCarrera(planProceso.getCarrera());
        master.setEstandar(planProceso.getEstandar());
        master.setProcesoId(null);
        master.setEsMaestro(true);
        master.setOrigenMaestroId(null);
        master.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);

        String nombreMaster = planProceso.getNombrePlan();
        if (nombreMaster == null || nombreMaster.isBlank()) {
            nombreMaster = planProceso.getCodigoResolucion() != null && !planProceso.getCodigoResolucion().isBlank()
                    ? planProceso.getCodigoResolucion().trim()
                    : "Plan " + (planProceso.getAnioAprobacion() != null ? planProceso.getAnioAprobacion() : 2026);
        }
        master.setNombrePlan(nombreMaster);
        copiarMetadatosPlan(planProceso, master);
        master.setActivo(true);
        master.setCreadoEn(now);
        master.setActualizadoEn(now);
        master = planRepository.save(master);

        planProceso.setEsMaestro(false);
        planProceso.setOrigenMaestroId(master.getId());
        if (planProceso.getNombrePlan() == null || planProceso.getNombrePlan().isBlank()) {
            planProceso.setNombrePlan(nombreMaster);
        }
        planRepository.save(planProceso);

        List<ActividadCurricularPlanJpaEntity> asigsProceso =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planProceso.getId());
        int orden = 1;
        for (ActividadCurricularPlanJpaEntity asigProc : asigsProceso) {
            if (asigProc.getOrigenMaestroId() != null) {
                continue;
            }
            ActividadCurricularPlanJpaEntity masterAsig = clonarAsignatura(
                    asigProc,
                    master,
                    null,
                    asigProc.getCodigo(),
                    asigProc.getNumeroOrden() != null ? asigProc.getNumeroOrden() : orden
            );
            masterAsig = asignaturaRepository.save(masterAsig);
            asigProc.setOrigenMaestroId(masterAsig.getId());
            asignaturaRepository.save(asigProc);
            orden++;
        }
    }

    private void propagarNuevaAsignaturaMaestraAClones(
            UUID masterPlanId,
            ActividadCurricularPlanJpaEntity masterAsig,
            UUID planExcluidoId
    ) {
        List<PlanEstudioFt001JpaEntity> clones = planRepository.findByOrigenMaestroIdAndActivoTrue(masterPlanId);
        for (PlanEstudioFt001JpaEntity clonePlan : clones) {
            if (planExcluidoId != null && clonePlan.getId().equals(planExcluidoId)) {
                continue;
            }
            if (asignaturaRepository.findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), masterAsig.getId()).isPresent()) {
                continue;
            }

            List<ActividadCurricularPlanJpaEntity> existentes =
                    asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(clonePlan.getId());

            ActividadCurricularPlanJpaEntity porCodigoSinVinculo = null;
            if (masterAsig.getCodigo() != null && !masterAsig.getCodigo().isBlank()) {
                porCodigoSinVinculo = existentes.stream()
                        .filter(a -> a.getOrigenMaestroId() == null
                                && a.getCodigo() != null
                                && masterAsig.getCodigo().trim().equalsIgnoreCase(a.getCodigo().trim()))
                        .findFirst()
                        .orElse(null);
            }

            if (porCodigoSinVinculo != null) {
                porCodigoSinVinculo.setOrigenMaestroId(masterAsig.getId());
                copiarCamposAsignatura(masterAsig, porCodigoSinVinculo);
                asignaturaRepository.save(porCodigoSinVinculo);
            } else {
                int orden = masterAsig.getNumeroOrden() != null
                        ? masterAsig.getNumeroOrden()
                        : existentes.size() + 1;
                ActividadCurricularPlanJpaEntity cloneAsig = clonarAsignatura(
                        masterAsig, clonePlan, masterAsig.getId(), masterAsig.getCodigo(), orden);
                asignaturaRepository.save(cloneAsig);
            }
            recalcularHorasPlan(clonePlan);
        }
    }

    private ActividadCurricularPlanJpaEntity clonarAsignatura(
            ActividadCurricularPlanJpaEntity origen,
            PlanEstudioFt001JpaEntity destinoPlan,
            UUID origenMaestroId,
            String codigo,
            int numeroOrden
    ) {
        ActividadCurricularPlanJpaEntity clone = new ActividadCurricularPlanJpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setPlanEstudio(destinoPlan);
        clone.setNumeroOrden(numeroOrden);
        clone.setCodigo(codigo);
        clone.setNombre(origen.getNombre());
        clone.setGrupo(origen.getGrupo());
        clone.setSubgrupo(origen.getSubgrupo());
        clone.setHorasReloj(origen.getHorasReloj());
        clone.setHorasPresenciales(origen.getHorasPresenciales());
        clone.setHorasNoPresenciales(origen.getHorasNoPresenciales());
        clone.setHorasSemanales(origen.getHorasSemanales());
        clone.setCorrelativas(origen.getCorrelativas());
        clone.setEsObligatoria(origen.getEsObligatoria());
        clone.setEstadoDictado(origen.getEstadoDictado());
        clone.setDocenteResponsableId(origen.getDocenteResponsableId());
        clone.setDocenteResponsableNombre(origen.getDocenteResponsableNombre());
        clone.setOrigenMaestroId(origenMaestroId);
        clone.setCreadoEn(Instant.now());
        return clone;
    }

    public void propagarFichaEstandarAsignatura(
            PlanEstudioFt001JpaEntity planOrigen,
            ActividadCurricularPlanJpaEntity asigOrigen
    ) {
        if (evaluacion22Repository == null || cargaAreaRepository == null || cargaResumenRepository == null
                || planOrigen == null || asigOrigen == null) {
            return;
        }
        if (Boolean.TRUE.equals(planOrigen.getEsMaestro())) {
            List<PlanEstudioFt001JpaEntity> clones = planRepository.findByOrigenMaestroIdAndActivoTrue(planOrigen.getId());
            for (PlanEstudioFt001JpaEntity clonePlan : clones) {
                asignaturaRepository.findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), asigOrigen.getId())
                        .ifPresent(dest -> copiarFichaEntreActividades(asigOrigen, dest));
            }
            return;
        }
        UUID masterAsigId = asigOrigen.getOrigenMaestroId();
        if (masterAsigId == null) {
            return;
        }
        asignaturaRepository.findById(masterAsigId)
                .ifPresent(masterAsig -> copiarFichaEntreActividades(asigOrigen, masterAsig));
        List<ActividadCurricularPlanJpaEntity> hermanos = asignaturaRepository.findByOrigenMaestroId(masterAsigId);
        for (ActividadCurricularPlanJpaEntity hermano : hermanos) {
            if (!hermano.getId().equals(asigOrigen.getId())
                    && hermano.getPlanEstudio() != null
                    && Boolean.TRUE.equals(hermano.getPlanEstudio().getActivo())) {
                copiarFichaEntreActividades(asigOrigen, hermano);
            }
        }
    }

    public void copiarFichaEntreActividades(
            ActividadCurricularPlanJpaEntity origen,
            ActividadCurricularPlanJpaEntity destino
    ) {
        if (evaluacion22Repository == null || cargaAreaRepository == null || cargaResumenRepository == null
                || origen == null || destino == null || Objects.equals(origen.getId(), destino.getId())) {
            return;
        }
        UUID estOrigenId = origen.getPlanEstudio() != null && origen.getPlanEstudio().getEstandar() != null
                ? origen.getPlanEstudio().getEstandar().getId() : null;
        UUID estDestinoId = destino.getPlanEstudio() != null && destino.getPlanEstudio().getEstandar() != null
                ? destino.getPlanEstudio().getEstandar().getId() : null;
        if (estOrigenId != null && estDestinoId != null && !estOrigenId.equals(estDestinoId)) {
            return;
        }

        List<ActividadCurricularEvaluacion22JpaEntity> origenEval =
                evaluacion22Repository.findByActividadCurricularId(origen.getId());
        evaluacion22Repository.deleteByActividadCurricularId(destino.getId());
        evaluacion22Repository.flush();
        if (!origenEval.isEmpty()) {
            List<ActividadCurricularEvaluacion22JpaEntity> clonesEval = origenEval.stream()
                    .map(e -> {
                        ActividadCurricularEvaluacion22JpaEntity c = new ActividadCurricularEvaluacion22JpaEntity();
                        c.setId(UUID.randomUUID());
                        c.setActividadCurricular(destino);
                        c.setEstandarItem(e.getEstandarItem());
                        c.setEscalaEvaluacion(e.getEscalaEvaluacion());
                        c.setCreadoEn(Instant.now());
                        return c;
                    })
                    .toList();
            evaluacion22Repository.saveAll(clonesEval);
        }

        List<ActividadCurricularCargaHorariaAreaJpaEntity> origenAreas =
                cargaAreaRepository.findByActividadCurricularId(origen.getId());
        cargaAreaRepository.deleteByActividadCurricularId(destino.getId());
        cargaAreaRepository.flush();
        if (!origenAreas.isEmpty()) {
            List<ActividadCurricularCargaHorariaAreaJpaEntity> clonesAreas = origenAreas.stream()
                    .map(a -> {
                        ActividadCurricularCargaHorariaAreaJpaEntity c = new ActividadCurricularCargaHorariaAreaJpaEntity();
                        c.setId(UUID.randomUUID());
                        c.setActividadCurricular(destino);
                        c.setAreaCargaHoraria(a.getAreaCargaHoraria());
                        c.setHorasTotalPresencial(a.getHorasTotalPresencial());
                        c.setHorasTotalDistancia(a.getHorasTotalDistancia());
                        c.setHorasPracticaPresencial(a.getHorasPracticaPresencial());
                        c.setHorasPracticaDistancia(a.getHorasPracticaDistancia());
                        return c;
                    })
                    .toList();
            cargaAreaRepository.saveAll(clonesAreas);
        }

        cargaResumenRepository.findById(origen.getId()).ifPresentOrElse(src -> {
            ActividadCurricularCargaHorariaResumenJpaEntity dest = cargaResumenRepository.findById(destino.getId())
                    .orElseGet(ActividadCurricularCargaHorariaResumenJpaEntity::new);
            dest.setActividadCurricularId(destino.getId());
            dest.setAmbitosFormacionPractica(src.getAmbitosFormacionPractica());
            dest.setHorasSemanalTotalPresencial(src.getHorasSemanalTotalPresencial());
            dest.setHorasSemanalTotalDistancia(src.getHorasSemanalTotalDistancia());
            dest.setHorasSemanalPracticaPresencial(src.getHorasSemanalPracticaPresencial());
            dest.setHorasSemanalPracticaDistancia(src.getHorasSemanalPracticaDistancia());
            dest.setActualizadoEn(Instant.now());
            cargaResumenRepository.save(dest);
        }, () -> cargaResumenRepository.findById(destino.getId()).ifPresent(cargaResumenRepository::delete));

        if (!Objects.equals(destino.getHorasSemanales(), origen.getHorasSemanales())) {
            destino.setHorasSemanales(origen.getHorasSemanales());
            asignaturaRepository.save(destino);
        }
    }

    private void copiarCamposAsignatura(
            ActividadCurricularPlanJpaEntity source,
            ActividadCurricularPlanJpaEntity target
    ) {
        if (source.getNumeroOrden() != null) {
            target.setNumeroOrden(source.getNumeroOrden());
        }
        target.setCodigo(source.getCodigo());
        target.setNombre(source.getNombre());
        target.setGrupo(source.getGrupo());
        target.setSubgrupo(source.getSubgrupo());
        target.setHorasReloj(source.getHorasReloj());
        target.setHorasPresenciales(source.getHorasPresenciales());
        target.setHorasNoPresenciales(source.getHorasNoPresenciales());
        target.setHorasSemanales(source.getHorasSemanales());
        target.setCorrelativas(source.getCorrelativas());
        target.setEsObligatoria(source.getEsObligatoria());
        target.setEstadoDictado(source.getEstadoDictado());
        target.setDocenteResponsableId(source.getDocenteResponsableId());
        target.setDocenteResponsableNombre(source.getDocenteResponsableNombre());
    }

    private void copiarMetadatosPlan(PlanEstudioFt001JpaEntity source, PlanEstudioFt001JpaEntity target) {
        if (source.getNombrePlan() != null && !source.getNombrePlan().isBlank()) {
            target.setNombrePlan(source.getNombrePlan().trim());
        }
        target.setCodigoResolucion(source.getCodigoResolucion());
        target.setAnioAprobacion(source.getAnioAprobacion());
        target.setAnioInicio(source.getAnioInicio());
        target.setDuracionAnios(source.getDuracionAnios());
        target.setEstadoVigencia(source.getEstadoVigencia());
        target.setEstructuraSintetica(source.getEstructuraSintetica());
        target.setInstanciaFinalGraduacion(source.getInstanciaFinalGraduacion());
        target.setOtraInformacion(source.getOtraInformacion());
        target.setHorasPresenciales(source.getHorasPresenciales());
        target.setHorasNoPresenciales(source.getHorasNoPresenciales());
        target.setHorasTotales(source.getHorasTotales());
        target.setActualizadoEn(Instant.now());
    }

    private void recalcularHorasPlan(PlanEstudioFt001JpaEntity plan) {
        List<ActividadCurricularPlanJpaEntity> asignaturas =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(plan.getId());
        int presenciales = 0;
        int noPresenciales = 0;
        int totales = 0;

        for (ActividadCurricularPlanJpaEntity a : asignaturas) {
            if (a.getEstadoDictado().esInactiva()) {
                continue;
            }
            int pres = a.getHorasPresenciales() != null ? a.getHorasPresenciales() : 0;
            int noPres = a.getHorasNoPresenciales() != null ? a.getHorasNoPresenciales() : 0;
            int reloj = a.getHorasReloj() != null ? a.getHorasReloj() : 0;

            presenciales += pres;
            noPresenciales += noPres;
            totales += (pres + noPres > 0 ? (pres + noPres) : reloj);
        }

        plan.setHorasPresenciales(presenciales);
        plan.setHorasNoPresenciales(noPresenciales);
        plan.setHorasTotales(totales);
        plan.setActualizadoEn(Instant.now());
        planRepository.save(plan);
    }
}
