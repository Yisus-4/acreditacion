package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.application.ports.ModoClonacion;
import com.undec.acreditacion.application.ports.ResultadoSincronizacionDto;
import com.undec.acreditacion.application.ports.SeccionClonacion;
import com.undec.acreditacion.application.ports.SincronizacionAutoevaluacionPort;
import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoActividadMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.ConvocatoriaCarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ConvocatoriaCarreraPlanJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataConvocatoriaCarreraPlanRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataConvocatoriaRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class SincronizacionAutoevaluacionService implements SincronizacionAutoevaluacionPort {

    private final SpringDataProcesoAcreditacionRepository procesoRepository;
    private final SpringDataConvocatoriaRepository convocatoriaRepository;
    private final SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository;
    private final SpringDataPlanEstudioFt001Repository planEstudioRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoMaestroRepository;
    private final SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository;
    private final SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository;
    private final SpringDataInmuebleRepository inmuebleRepository;
    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository;
    private final SpringDataAmbitoPracticaRepository ambitoPracticaRepository;
    private final SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;

    public SincronizacionAutoevaluacionService(
            SpringDataProcesoAcreditacionRepository procesoRepository,
            SpringDataConvocatoriaRepository convocatoriaRepository,
            SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoMaestroRepository,
            SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository,
            SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository,
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository) {
        this(procesoRepository, convocatoriaRepository, convocatoriaCarreraPlanRepository,
                planEstudioRepository, actividadRepository, cuerpoAcademicoMaestroRepository,
                procesoDocenteRepository, procesoDocenteActividadRepository, inmuebleRepository,
                espacioFisicoRepository, espacioActividadRepository, ambitoPracticaRepository,
                autoevaluacionAmbitoRepository, null);
    }

    @Autowired
    public SincronizacionAutoevaluacionService(
            SpringDataProcesoAcreditacionRepository procesoRepository,
            SpringDataConvocatoriaRepository convocatoriaRepository,
            SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoMaestroRepository,
            SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository,
            SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository,
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository,
            SpringDataAmbitoPracticaRepository ambitoPracticaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService) {
        this.procesoRepository = procesoRepository;
        this.convocatoriaRepository = convocatoriaRepository;
        this.convocatoriaCarreraPlanRepository = convocatoriaCarreraPlanRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.actividadRepository = actividadRepository;
        this.cuerpoAcademicoMaestroRepository = cuerpoAcademicoMaestroRepository;
        this.procesoDocenteRepository = procesoDocenteRepository;
        this.procesoDocenteActividadRepository = procesoDocenteActividadRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.espacioFisicoRepository = espacioFisicoRepository;
        this.espacioActividadRepository = espacioActividadRepository;
        this.ambitoPracticaRepository = ambitoPracticaRepository;
        this.autoevaluacionAmbitoRepository = autoevaluacionAmbitoRepository;
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
    }

    @Override
    public ResultadoSincronizacionDto copiarDatosCentrales(UUID procesoId, ModoClonacion modo, Set<SeccionClonacion> secciones) {
        ProcesoAcreditacionJpaEntity proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        UUID carreraId = proceso.getCarreraId();
        Set<SeccionClonacion> secs = (secciones == null || secciones.isEmpty())
                ? EnumSet.allOf(SeccionClonacion.class)
                : secciones;

        int planesClonados = 0;
        int actividadesClonadas = 0;
        int docentesClonados = 0;
        int asignacionesDocentesClonadas = 0;
        int inmueblesClonados = 0;
        int espaciosClonados = 0;
        int vinculacionesEspacioActividadClonadas = 0;
        int ambitosPracticaClonados = 0;

        // Limpieza si el modo es REEMPLAZAR_COPIA
        if (modo == ModoClonacion.REEMPLAZAR_COPIA) {
            limpiarCopiasPrevias(procesoId, secs);
        }

        Map<UUID, PlanEstudioFt001JpaEntity> planMasterToCloneMap = new HashMap<>();
        Map<UUID, ActividadCurricularPlanJpaEntity> actMasterToCloneMap = new HashMap<>();

        // Paso 1: Planes de Estudio y Asignaturas
        if (secs.contains(SeccionClonacion.PLANES_Y_ASIGNATURAS)) {
            List<ConvocatoriaCarreraPlanJpaEntity> ccpList = convocatoriaCarreraPlanRepository
                    .findByProcesoIdAndSeleccionadoPorDeTrue(procesoId);

            if (ccpList.isEmpty()) {
                // Fallback: buscar planes vigentes de la carrera
                List<PlanEstudioFt001JpaEntity> masterPlanes = planEstudioRepository
                        .findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(carreraId, EstadoVigenciaPlan.VIGENTE);
                for (PlanEstudioFt001JpaEntity masterPlan : masterPlanes) {
                    PlanEstudioFt001JpaEntity clonePlan = planEstudioRepository
                            .findByProcesoIdAndOrigenMaestroId(procesoId, masterPlan.getId())
                            .orElse(null);

                    if (clonePlan == null) {
                        clonePlan = clonarPlanEstudio(masterPlan, procesoId);
                        clonePlan = planEstudioRepository.save(clonePlan);
                        planesClonados++;
                    }
                    planMasterToCloneMap.put(masterPlan.getId(), clonePlan);

                    int acts = sincronizarActividadesPlan(masterPlan, clonePlan, actMasterToCloneMap);
                    actividadesClonadas += acts;
                }
            } else {
                for (ConvocatoriaCarreraPlanJpaEntity ccp : ccpList) {
                    PlanEstudioFt001JpaEntity masterPlan = ccp.getPlanEstudioMaestro();
                    if (masterPlan == null) continue;

                    PlanEstudioFt001JpaEntity clonePlan = planEstudioRepository
                            .findByProcesoIdAndOrigenMaestroId(procesoId, masterPlan.getId())
                            .orElse(null);

                    if (clonePlan == null) {
                        clonePlan = clonarPlanEstudio(masterPlan, procesoId);
                        clonePlan = planEstudioRepository.save(clonePlan);
                        planesClonados++;
                    }
                    ccp.setPlanEstudioClon(clonePlan);
                    convocatoriaCarreraPlanRepository.save(ccp);

                    planMasterToCloneMap.put(masterPlan.getId(), clonePlan);
                    int acts = sincronizarActividadesPlan(masterPlan, clonePlan, actMasterToCloneMap);
                    actividadesClonadas += acts;
                }
            }
        } else {
            // Cargar planes y actividades ya clonados para remapeo de siguientes pasos
            cargarMapeosExistentes(procesoId, planMasterToCloneMap, actMasterToCloneMap);
        }

        // Paso 2: Cuerpo Académico y Asignaciones
        if (secs.contains(SeccionClonacion.CUERPO_ACADEMICO)) {
            List<CuerpoAcademicoMaestroJpaEntity> masterDocs = cuerpoAcademicoMaestroRepository.findByCarreraId(carreraId);
            Map<UUID, ProcesoCuerpoAcademicoJpaEntity> docMasterToCloneMap = new HashMap<>();

            for (CuerpoAcademicoMaestroJpaEntity masterDoc : masterDocs) {
                ProcesoCuerpoAcademicoJpaEntity procDoc = procesoDocenteRepository
                        .findByProcesoIdAndOrigenMaestroId(procesoId, masterDoc.getId())
                        .orElse(null);

                if (procDoc == null) {
                    procDoc = clonarDocente(masterDoc, procesoId);
                    procDoc = procesoDocenteRepository.save(procDoc);
                    docentesClonados++;
                }
                docMasterToCloneMap.put(masterDoc.getId(), procDoc);

                if (masterDoc.getAsignaciones() != null) {
                    for (CuerpoAcademicoActividadMaestroJpaEntity masterAsig : masterDoc.getAsignaciones()) {
                        UUID masterPlanId = masterAsig.getPlanEstudio() != null ? masterAsig.getPlanEstudio().getId() : null;
                        UUID masterActId = masterAsig.getActividadCurricular() != null ? masterAsig.getActividadCurricular().getId() : null;

                        PlanEstudioFt001JpaEntity clonedPlan = masterPlanId != null ? planMasterToCloneMap.get(masterPlanId) : null;
                        ActividadCurricularPlanJpaEntity clonedAct = masterActId != null ? actMasterToCloneMap.get(masterActId) : null;

                        if (clonedPlan != null && clonedAct != null) {
                            Optional<ProcesoCuerpoAcademicoActividadJpaEntity> existingAsig = procesoDocenteActividadRepository
                                    .findByProcesoIdAndDocenteIdAndPlanEstudioIdAndActividadCurricularId(
                                            procesoId, procDoc.getId(), clonedPlan.getId(), clonedAct.getId());

                            if (existingAsig.isEmpty()) {
                                ProcesoCuerpoAcademicoActividadJpaEntity procAsig = new ProcesoCuerpoAcademicoActividadJpaEntity();
                                procAsig.setProcesoId(procesoId);
                                procAsig.setDocente(procDoc);
                                procAsig.setCarreraId(carreraId);
                                procAsig.setPlanEstudio(clonedPlan);
                                procAsig.setActividadCurricular(clonedAct);
                                procAsig.setRolEnCatedra(masterAsig.getRolEnCatedra());
                                procAsig.setCargaHorariaSemanal(masterAsig.getCargaHorariaSemanal());
                                procAsig.setOrigenMaestro(masterAsig);
                                procAsig.setCreadoEn(Instant.now());
                                procesoDocenteActividadRepository.save(procAsig);
                                asignacionesDocentesClonadas++;
                            }
                        }
                    }
                }
            }
        }

        Map<UUID, InmuebleJpaEntity> inmuebleMasterToCloneMap = new HashMap<>();

        // Paso 3: Infraestructura (Inmuebles y Espacios Físicos con Remapeo de Jerarquía y Vínculos)
        if (secs.contains(SeccionClonacion.INFRAESTRUCTURA_Y_ESPACIOS)) {
            List<InmuebleJpaEntity> masterInmuebles = inmuebleRepository.findMaestrosActivos();

            for (InmuebleJpaEntity masterInm : masterInmuebles) {
                InmuebleJpaEntity cloneInm = inmuebleRepository
                        .findByProcesoIdAndOrigenMaestroId(procesoId, masterInm.getId())
                        .orElse(null);

                if (cloneInm == null) {
                    cloneInm = clonarInmueble(masterInm, procesoId);
                    cloneInm = inmuebleRepository.save(cloneInm);
                    inmueblesClonados++;
                } else if (!Boolean.TRUE.equals(cloneInm.getActivo())) {
                    cloneInm.setActivo(true);
                    cloneInm.setActualizadoEn(Instant.now());
                    cloneInm = inmuebleRepository.save(cloneInm);
                    inmueblesClonados++;
                }
                inmuebleMasterToCloneMap.put(masterInm.getId(), cloneInm);
            }

            // Dos pasadas para espacios físicos:
            // Pasada 1: Espacios padre (espacioPadre == null)
            Map<UUID, EspacioFisicoJpaEntity> espacioMasterToCloneMap = new HashMap<>();
            List<EspacioFisicoJpaEntity> masterEspacios = espacioFisicoRepository.findByEsMaestroTrue();

            for (EspacioFisicoJpaEntity masterEsp : masterEspacios) {
                if (masterEsp.getEspacioPadre() != null) {
                    continue; // Hijo, procesar en pasada 2
                }
                InmuebleJpaEntity targetInmueble = inmuebleMasterToCloneMap.get(masterEsp.getInmueble().getId());
                if (targetInmueble == null) {
                    continue;
                }

                EspacioFisicoJpaEntity cloneEsp = espacioFisicoRepository
                        .findByProcesoIdAndOrigenMaestroId(procesoId, masterEsp.getId())
                        .orElse(null);

                if (cloneEsp == null) {
                    cloneEsp = clonarEspacio(masterEsp, procesoId, targetInmueble, null);
                    cloneEsp = espacioFisicoRepository.save(cloneEsp);
                    espaciosClonados++;
                } else if (!Boolean.TRUE.equals(cloneEsp.getActivo())) {
                    cloneEsp.setActivo(true);
                    cloneEsp = espacioFisicoRepository.save(cloneEsp);
                    espaciosClonados++;
                }
                espacioMasterToCloneMap.put(masterEsp.getId(), cloneEsp);
            }

            // Pasada 2: Subespacios (espacioPadre != null)
            for (EspacioFisicoJpaEntity masterEsp : masterEspacios) {
                if (masterEsp.getEspacioPadre() == null) {
                    continue;
                }
                InmuebleJpaEntity targetInmueble = inmuebleMasterToCloneMap.get(masterEsp.getInmueble().getId());
                if (targetInmueble == null) {
                    continue;
                }

                EspacioFisicoJpaEntity parentClone = espacioMasterToCloneMap.get(masterEsp.getEspacioPadre().getId());

                EspacioFisicoJpaEntity cloneEsp = espacioFisicoRepository
                        .findByProcesoIdAndOrigenMaestroId(procesoId, masterEsp.getId())
                        .orElse(null);

                if (cloneEsp == null) {
                    cloneEsp = clonarEspacio(masterEsp, procesoId, targetInmueble, parentClone);
                    cloneEsp = espacioFisicoRepository.save(cloneEsp);
                    espaciosClonados++;
                } else {
                    boolean changed = false;
                    if (!Boolean.TRUE.equals(cloneEsp.getActivo())) {
                        cloneEsp.setActivo(true);
                        changed = true;
                        espaciosClonados++;
                    }
                    if (parentClone != null && cloneEsp.getEspacioPadre() == null) {
                        cloneEsp.setEspacioPadre(parentClone);
                        changed = true;
                    }
                    if (changed) {
                        espacioFisicoRepository.save(cloneEsp);
                    }
                }
                espacioMasterToCloneMap.put(masterEsp.getId(), cloneEsp);
            }

            // Clonar y remapear espacio_fisico_actividad_curricular
            for (EspacioFisicoJpaEntity masterEsp : masterEspacios) {
                EspacioFisicoJpaEntity clonedEsp = espacioMasterToCloneMap.get(masterEsp.getId());
                if (clonedEsp == null) continue;

                List<EspacioFisicoActividadCurricularJpaEntity> masterBindings = espacioActividadRepository
                        .findByEspacioFisicoId(masterEsp.getId());

                for (EspacioFisicoActividadCurricularJpaEntity mb : masterBindings) {
                    PlanEstudioFt001JpaEntity clonedPlan = mb.getPlanEstudio() != null
                            ? planMasterToCloneMap.get(mb.getPlanEstudio().getId()) : null;
                    ActividadCurricularPlanJpaEntity clonedAct = mb.getActividadCurricular() != null
                            ? actMasterToCloneMap.get(mb.getActividadCurricular().getId()) : null;

                    if (clonedPlan != null && clonedAct != null) {
                        List<EspacioFisicoActividadCurricularJpaEntity> existingBindings = espacioActividadRepository
                                .findByEspacioFisicoId(clonedEsp.getId());

                        boolean alreadyExists = existingBindings.stream().anyMatch(b ->
                                b.getPlanEstudio().getId().equals(clonedPlan.getId()) &&
                                b.getActividadCurricular().getId().equals(clonedAct.getId()));

                        if (!alreadyExists) {
                            EspacioFisicoActividadCurricularJpaEntity newBinding = new EspacioFisicoActividadCurricularJpaEntity(
                                    null,
                                    clonedEsp,
                                    carreraId,
                                    clonedPlan,
                                    clonedAct,
                                    mb.getComisionesAsignadas(),
                                    mb.getObservacionesUso()
                            );
                            espacioActividadRepository.save(newBinding);
                            vinculacionesEspacioActividadClonadas++;
                        }
                    }
                }
            }
        }

        // Paso 4: Ámbitos de Práctica (clonación aislada por proceso/carrera)
        if (secs.contains(SeccionClonacion.AMBITOS_PRACTICA)) {
            List<AutoevaluacionCarreraAmbitoJpaEntity> vinculacionesExistentes =
                    autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId);
            if (vinculacionesExistentes != null && !vinculacionesExistentes.isEmpty()) {
                List<AutoevaluacionCarreraAmbitoJpaEntity> huerfanas = vinculacionesExistentes.stream()
                        .filter(v -> v.getAmbito() == null
                                || Boolean.TRUE.equals(v.getAmbito().getEsMaestro())
                                || !procesoId.equals(v.getAmbito().getProcesoId()))
                        .toList();
                if (!huerfanas.isEmpty()) {
                    autoevaluacionAmbitoRepository.deleteAll(huerfanas);
                }
            }

            List<AmbitoPracticaJpaEntity> masterAmbitos = ambitoPracticaRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc();
            for (AmbitoPracticaJpaEntity masterAmbito : masterAmbitos) {
                Optional<AmbitoPracticaJpaEntity> existingClone = ambitoPracticaRepository
                        .findByProcesoIdAndOrigenMaestroId(procesoId, masterAmbito.getId());

                if (existingClone.isEmpty()) {
                    InmuebleJpaEntity clonedInmueble = null;
                    if (masterAmbito.getInmueble() != null) {
                        UUID masterInmId = masterAmbito.getInmueble().getId();
                        clonedInmueble = inmuebleMasterToCloneMap.get(masterInmId);
                        if (clonedInmueble == null) {
                            clonedInmueble = inmuebleRepository
                                    .findByProcesoIdAndOrigenMaestroId(procesoId, masterInmId)
                                    .orElse(null);
                        }
                    }

                    AmbitoPracticaJpaEntity clone = clonarAmbitoPractica(masterAmbito, procesoId, clonedInmueble);
                    ambitoPracticaRepository.save(clone);
                    ambitosPracticaClonados++;
                }
            }
        }

        return new ResultadoSincronizacionDto(
                procesoId,
                planesClonados,
                actividadesClonadas,
                docentesClonados,
                asignacionesDocentesClonadas,
                inmueblesClonados,
                espaciosClonados,
                vinculacionesEspacioActividadClonadas,
                ambitosPracticaClonados,
                modo,
                Instant.now()
        );
    }

    private int sincronizarActividadesPlan(
            PlanEstudioFt001JpaEntity masterPlan,
            PlanEstudioFt001JpaEntity clonePlan,
            Map<UUID, ActividadCurricularPlanJpaEntity> actMasterToCloneMap) {

        int count = 0;
        List<ActividadCurricularPlanJpaEntity> masterActs = actividadRepository
                .findByPlanEstudioIdOrderByNumeroOrdenAsc(masterPlan.getId());

        for (ActividadCurricularPlanJpaEntity masterAct : masterActs) {
            if (masterAct.getEstadoDictado() == EstadoDictadoActividad.NO_SE_DICTA) {
                continue;
            }
            ActividadCurricularPlanJpaEntity cloneAct = actividadRepository
                    .findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), masterAct.getId())
                    .orElse(null);

            if (cloneAct == null) {
                cloneAct = clonarActividad(masterAct, clonePlan);
                cloneAct = actividadRepository.save(cloneAct);
                if (sincronizacionBidireccionalPlanService != null) {
                    sincronizacionBidireccionalPlanService.copiarFichaEntreActividades(masterAct, cloneAct);
                }
                count++;
            }
            actMasterToCloneMap.put(masterAct.getId(), cloneAct);
        }
        return count;
    }

    private void cargarMapeosExistentes(
            UUID procesoId,
            Map<UUID, PlanEstudioFt001JpaEntity> planMasterToCloneMap,
            Map<UUID, ActividadCurricularPlanJpaEntity> actMasterToCloneMap) {

        List<PlanEstudioFt001JpaEntity> clones = planEstudioRepository.findByProcesoIdAndActivoTrue(procesoId);
        for (PlanEstudioFt001JpaEntity clonePlan : clones) {
            if (clonePlan.getOrigenMaestroId() != null) {
                planMasterToCloneMap.put(clonePlan.getOrigenMaestroId(), clonePlan);
            }
            List<ActividadCurricularPlanJpaEntity> acts = actividadRepository
                    .findByPlanEstudioIdOrderByNumeroOrdenAsc(clonePlan.getId());
            for (ActividadCurricularPlanJpaEntity act : acts) {
                if (act.getOrigenMaestroId() != null) {
                    actMasterToCloneMap.put(act.getOrigenMaestroId(), act);
                }
            }
        }
    }

    private void limpiarCopiasPrevias(UUID procesoId, Set<SeccionClonacion> secs) {
        if (secs.contains(SeccionClonacion.INFRAESTRUCTURA_Y_ESPACIOS)) {
            List<EspacioFisicoJpaEntity> espacios = espacioFisicoRepository.findByProcesoId(procesoId);
            for (EspacioFisicoJpaEntity esp : espacios) {
                espacioActividadRepository.deleteByEspacioFisicoId(esp.getId());
            }
            espacioFisicoRepository.deleteAll(espacios);

            List<InmuebleJpaEntity> inmuebles = inmuebleRepository.findByProcesoIdActivos(procesoId);
            inmuebleRepository.deleteAll(inmuebles);
        }

        if (secs.contains(SeccionClonacion.CUERPO_ACADEMICO)) {
            List<ProcesoCuerpoAcademicoActividadJpaEntity> asigs = procesoDocenteActividadRepository.findByProcesoId(procesoId);
            procesoDocenteActividadRepository.deleteAll(asigs);

            List<ProcesoCuerpoAcademicoJpaEntity> docs = procesoDocenteRepository.findByProcesoId(procesoId);
            procesoDocenteRepository.deleteAll(docs);
        }

        if (secs.contains(SeccionClonacion.PLANES_Y_ASIGNATURAS)) {
            List<PlanEstudioFt001JpaEntity> planes = planEstudioRepository.findByProcesoIdAndActivoTrue(procesoId);
            for (PlanEstudioFt001JpaEntity p : planes) {
                List<ActividadCurricularPlanJpaEntity> acts = actividadRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(p.getId());
                actividadRepository.deleteAll(acts);
            }
            planEstudioRepository.deleteAll(planes);
        }

        if (secs.contains(SeccionClonacion.AMBITOS_PRACTICA)) {
            List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones =
                    autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId);
            if (!vinculaciones.isEmpty()) {
                autoevaluacionAmbitoRepository.deleteAll(vinculaciones);
            }
            List<AmbitoPracticaJpaEntity> copiasAmbitos = ambitoPracticaRepository.findByProcesoId(procesoId);
            if (!copiasAmbitos.isEmpty()) {
                ambitoPracticaRepository.deleteAll(copiasAmbitos);
            }
        }
    }

    private PlanEstudioFt001JpaEntity clonarPlanEstudio(PlanEstudioFt001JpaEntity master, UUID procesoId) {
        PlanEstudioFt001JpaEntity clone = new PlanEstudioFt001JpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setCarrera(master.getCarrera());
        clone.setEstandar(master.getEstandar());
        clone.setProcesoId(procesoId);
        clone.setNombrePlan(master.getNombrePlan());
        clone.setEstadoVigencia(master.getEstadoVigencia());
        clone.setEsMaestro(false);
        clone.setOrigenMaestroId(master.getId());
        clone.setCodigoResolucion(master.getCodigoResolucion());
        clone.setAnioAprobacion(master.getAnioAprobacion());
        clone.setAnioInicio(master.getAnioInicio());
        clone.setDuracionAnios(master.getDuracionAnios());
        clone.setOrientacionesJson(master.getOrientacionesJson());
        clone.setFundamentacionModificaciones(master.getFundamentacionModificaciones());
        clone.setEstructuraSintetica(master.getEstructuraSintetica());
        clone.setActividadesComunesCiclo(master.getActividadesComunesCiclo());
        clone.setTituloIntermedioDenominacion(master.getTituloIntermedioDenominacion());
        clone.setTituloIntermedioDuracion(master.getTituloIntermedioDuracion());
        clone.setTituloIntermedioActividades(master.getTituloIntermedioActividades());
        clone.setInstanciaFinalGraduacion(master.getInstanciaFinalGraduacion());
        clone.setOtraInformacion(master.getOtraInformacion());
        clone.setHorasPresenciales(master.getHorasPresenciales());
        clone.setHorasNoPresenciales(master.getHorasNoPresenciales());
        clone.setHorasTotales(master.getHorasTotales());
        clone.setHorasOptativasMinimas(master.getHorasOptativasMinimas());
        clone.setActivo(true);
        clone.setCreadoEn(Instant.now());
        clone.setActualizadoEn(Instant.now());
        return clone;
    }

    private ActividadCurricularPlanJpaEntity clonarActividad(ActividadCurricularPlanJpaEntity master, PlanEstudioFt001JpaEntity clonePlan) {
        ActividadCurricularPlanJpaEntity clone = new ActividadCurricularPlanJpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setPlanEstudio(clonePlan);
        clone.setNumeroOrden(master.getNumeroOrden());
        clone.setCodigo(master.getCodigo());
        clone.setNombre(master.getNombre());
        clone.setGrupo(master.getGrupo());
        clone.setSubgrupo(master.getSubgrupo());
        clone.setHorasReloj(master.getHorasReloj());
        clone.setHorasPresenciales(master.getHorasPresenciales());
        clone.setHorasNoPresenciales(master.getHorasNoPresenciales());
        clone.setCorrelativas(master.getCorrelativas());
        clone.setEsObligatoria(master.getEsObligatoria());
        clone.setEstadoDictado(master.getEstadoDictado());
        clone.setDocenteResponsableId(master.getDocenteResponsableId());
        clone.setDocenteResponsableNombre(master.getDocenteResponsableNombre());
        clone.setOrigenMaestroId(master.getId());
        clone.setCreadoEn(Instant.now());
        return clone;
    }

    private ProcesoCuerpoAcademicoJpaEntity clonarDocente(CuerpoAcademicoMaestroJpaEntity master, UUID procesoId) {
        ProcesoCuerpoAcademicoJpaEntity clone = new ProcesoCuerpoAcademicoJpaEntity();
        clone.setProcesoId(procesoId);
        clone.setOrigenMaestroId(master.getId());
        clone.setDni(master.getDni());
        clone.setCuit(master.getCuit());
        clone.setApellido(master.getApellido());
        clone.setNombre(master.getNombre());
        clone.setEmailInstitucional(master.getEmailInstitucional());
        clone.setEmailPersonal(master.getEmailPersonal());
        clone.setTelefono(master.getTelefono());
        clone.setTituloGrado(master.getTituloGrado());
        clone.setMaximoTituloPosgrado(master.getMaximoTituloPosgrado());
        clone.setCategoriaDocente(master.getCategoriaDocente());
        clone.setCaracterDesignacion(master.getCaracterDesignacion());
        clone.setDedicacionDocente(master.getDedicacionDocente());
        clone.setHorasSemanales(master.getHorasSemanales());
        clone.setActivo(true);
        clone.setCreadoEn(Instant.now());
        clone.setActualizadoEn(Instant.now());
        return clone;
    }

    private InmuebleJpaEntity clonarInmueble(InmuebleJpaEntity master, UUID procesoId) {
        InmuebleJpaEntity clone = new InmuebleJpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setInstitucion(master.getInstitucion());
        clone.setNumeroOrden(master.getNumeroOrden());
        clone.setNombre(master.getNombre());
        clone.setDomicilio(master.getDomicilio());
        clone.setCalle(master.getCalle());
        clone.setNumero(master.getNumero());
        clone.setProvincia(master.getProvincia());
        clone.setDepartamentoGeografico(master.getDepartamentoGeografico());
        clone.setLocalidad(master.getLocalidad());
        clone.setSede(master.getSede());
        clone.setAnioConstruccion(master.getAnioConstruccion());
        clone.setAnioInicioInstitucion(master.getAnioInicioInstitucion());
        clone.setPropiedadInstitucion(master.getPropiedadInstitucion());
        clone.setCondicionTenencia(master.getCondicionTenencia());
        clone.setSituacion(master.getSituacion());
        clone.setVinculaTodasCarreras(master.getVinculaTodasCarreras());
        clone.setEsMaestro(false);
        clone.setProcesoId(procesoId);
        clone.setOrigenMaestroId(master.getId());
        clone.setSuperficieCubiertaM2(master.getSuperficieCubiertaM2());
        clone.setSuperficieTotalM2(master.getSuperficieTotalM2());
        clone.setObservaciones(master.getObservaciones());
        clone.setVerificado(master.getVerificado());
        clone.setFechaVerificacion(master.getFechaVerificacion());
        clone.setEstadoConeau(master.getEstadoConeau());
        clone.setActivo(true);
        clone.setCreadoEn(Instant.now());
        clone.setActualizadoEn(Instant.now());
        if (master.getCarreras() != null) {
            clone.setCarreras(new HashSet<>(master.getCarreras()));
        }
        return clone;
    }

    private EspacioFisicoJpaEntity clonarEspacio(
            EspacioFisicoJpaEntity master,
            UUID procesoId,
            InmuebleJpaEntity clonedInmueble,
            EspacioFisicoJpaEntity clonedParent) {

        EspacioFisicoJpaEntity clone = new EspacioFisicoJpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setInmueble(clonedInmueble);
        clone.setTipoEspacio(master.getTipoEspacio());
        clone.setDenominacion(master.getDenominacion());
        clone.setDescripcionInfraestructura(master.getDescripcionInfraestructura());
        clone.setCantidad(master.getCantidad());
        clone.setCapacidadPersonasSentadas(master.getCapacidadPersonasSentadas());
        clone.setCapacidadEstudiantes(master.getCapacidadEstudiantes());
        clone.setSuperficieM2(master.getSuperficieM2());
        clone.setEquipamientoDisponible(master.getEquipamientoDisponible());
        clone.setEstadoConeau(master.getEstadoConeau());
        clone.setEspacioPadre(clonedParent);
        clone.setTieneSubespacios(master.getTieneSubespacios());
        clone.setObservacionHigieneSeguridad(master.getObservacionHigieneSeguridad());
        clone.setActivo(true);
        clone.setEsMaestro(false);
        clone.setProcesoId(procesoId);
        clone.setOrigenMaestroId(master.getId());
        clone.setCreadoEn(Instant.now());
        return clone;
    }

    private AmbitoPracticaJpaEntity clonarAmbitoPractica(
            AmbitoPracticaJpaEntity master,
            UUID procesoId,
            InmuebleJpaEntity clonedInmueble) {

        Instant now = Instant.now();
        AmbitoPracticaJpaEntity clone = new AmbitoPracticaJpaEntity();
        clone.setId(UUID.randomUUID());
        clone.setNumeroOrden(master.getNumeroOrden());
        clone.setDenominacion(master.getDenominacion());
        clone.setTipoCategoria(master.getTipoCategoria());
        clone.setTipoAmbito(master.getTipoAmbito());
        clone.setTipoEstablecimiento(master.getTipoEstablecimiento());
        clone.setInmueble(clonedInmueble);

        String inmRef = master.getInmuebleNombreReferencia();
        if ((inmRef == null || inmRef.isBlank()) && master.getInmueble() != null) {
            inmRef = master.getInmueble().getNombre();
        }
        clone.setInmuebleNombreReferencia(inmRef);

        clone.setConvenio(master.getConvenio());
        clone.setDescripcion(master.getDescripcion());
        clone.setAnioConstruccion(master.getAnioConstruccion());
        clone.setSuperficieM2(master.getSuperficieM2());
        clone.setCapacidadAlumnos(master.getCapacidadAlumnos());
        clone.setDisponibilidadHoraria(master.getDisponibilidadHoraria());
        clone.setDireccion(master.getDireccion());
        clone.setLocalidad(master.getLocalidad());
        clone.setProvincia(master.getProvincia());
        clone.setDepartamento(master.getDepartamento());
        clone.setCodigoPostal(master.getCodigoPostal());
        clone.setTipoDependencia(master.getTipoDependencia());
        clone.setSector(master.getSector());
        clone.setActividadAlumnoCam(master.getActividadAlumnoCam());
        clone.setEspeciesAnimalesCultivos(master.getEspeciesAnimalesCultivos());
        clone.setNumeroAnimales(master.getNumeroAnimales());
        clone.setActividadesDemostrativas(master.getActividadesDemostrativas());
        clone.setCarrerasQueLoUsan(master.getCarrerasQueLoUsan());
        clone.setEsMaestro(false);
        clone.setProcesoId(procesoId);
        clone.setOrigenMaestroId(master.getId());
        clone.setActivo(true);
        clone.setCreadoEn(now);
        clone.setActualizadoEn(now);
        return clone;
    }
}
