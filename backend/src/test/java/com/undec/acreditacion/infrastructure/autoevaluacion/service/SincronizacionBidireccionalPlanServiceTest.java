package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SincronizacionBidireccionalPlanServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @InjectMocks
    private SincronizacionBidireccionalPlanService service;

    private CarreraJpaEntity carrera;
    private PlanEstudioFt001JpaEntity masterPlan;
    private PlanEstudioFt001JpaEntity clonePlan;

    @BeforeEach
    void setUp() {
        carrera = new CarreraJpaEntity();
        carrera.setId(UUID.randomUUID());
        carrera.setNombre("Ingeniería Agronómica");

        masterPlan = new PlanEstudioFt001JpaEntity();
        masterPlan.setId(UUID.randomUUID());
        masterPlan.setCarrera(carrera);
        masterPlan.setNombrePlan("Agro-1");
        masterPlan.setEsMaestro(true);
        masterPlan.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        masterPlan.setActivo(true);

        clonePlan = new PlanEstudioFt001JpaEntity();
        clonePlan.setId(UUID.randomUUID());
        clonePlan.setCarrera(carrera);
        clonePlan.setNombrePlan("Agro-1");
        clonePlan.setProcesoId(UUID.randomUUID());
        clonePlan.setEsMaestro(false);
        clonePlan.setOrigenMaestroId(masterPlan.getId());
        clonePlan.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        clonePlan.setActivo(true);
    }

    @Test
    @DisplayName("Alta de actividad curricular en Académica se refleja automáticamente en Autoevaluación")
    void deberiaPropagarCreacionDesdeMaestroAClones() {
        ActividadCurricularPlanJpaEntity asigMaster = crearAsignatura(masterPlan, "101", "Análisis I", EstadoDictadoActividad.ACTIVA, null);

        when(planRepository.findByOrigenMaestroIdAndActivoTrue(masterPlan.getId())).thenReturn(List.of(clonePlan));
        when(asignaturaRepository.findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), asigMaster.getId()))
                .thenReturn(Optional.empty());
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(clonePlan.getId()))
                .thenReturn(List.of());
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarCreacionAsignatura(masterPlan, asigMaster);

        verify(asignaturaRepository).save(argThat(a ->
                clonePlan.getId().equals(a.getPlanEstudio().getId())
                        && asigMaster.getId().equals(a.getOrigenMaestroId())
                        && "101".equals(a.getCodigo())
                        && "Análisis I".equals(a.getNombre())
        ));
    }

    @Test
    @DisplayName("Actividad curricular marcada NO_SE_DICTA en Académica no se crea en clones de Autoevaluación")
    void noDebePropagarCreacionSiNoSeDictaEnMaestro() {
        ActividadCurricularPlanJpaEntity asigMaster = crearAsignatura(masterPlan, "102", "Optativa", EstadoDictadoActividad.INACTIVA, null);

        service.propagarCreacionAsignatura(masterPlan, asigMaster);

        verifyNoInteractions(planRepository);
        verifyNoInteractions(asignaturaRepository);
    }

    @Test
    @DisplayName("Alta de actividad curricular en Autoevaluación se refleja en Académica y clones hermanos")
    void deberiaPropagarCreacionDesdeAutoevaluacionAMaestroYClonesHermanos() {
        PlanEstudioFt001JpaEntity siblingClone = new PlanEstudioFt001JpaEntity();
        siblingClone.setId(UUID.randomUUID());
        siblingClone.setCarrera(carrera);
        siblingClone.setProcesoId(UUID.randomUUID());
        siblingClone.setEsMaestro(false);
        siblingClone.setOrigenMaestroId(masterPlan.getId());
        siblingClone.setActivo(true);

        ActividadCurricularPlanJpaEntity asigClone = crearAsignatura(clonePlan, "201", "Edafología", EstadoDictadoActividad.ACTIVA, null);

        when(planRepository.findById(masterPlan.getId())).thenReturn(Optional.of(masterPlan));
        when(asignaturaRepository.findMaxNumeroOrdenByPlanEstudioId(masterPlan.getId())).thenReturn(1);
        when(asignaturaRepository.existsByPlanEstudioIdAndCodigo(masterPlan.getId(), "201")).thenReturn(false);
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(any())).thenReturn(List.of());
        when(planRepository.findByOrigenMaestroIdAndActivoTrue(masterPlan.getId())).thenReturn(List.of(clonePlan, siblingClone));
        when(asignaturaRepository.findByPlanEstudioIdAndOrigenMaestroId(eq(siblingClone.getId()), any())).thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarCreacionAsignatura(clonePlan, asigClone);

        assertNotNull(asigClone.getOrigenMaestroId());
        verify(asignaturaRepository).save(argThat(a ->
                masterPlan.getId().equals(a.getPlanEstudio().getId())
                        && a.getOrigenMaestroId() == null
                        && "Edafología".equals(a.getNombre())
        ));
        verify(asignaturaRepository).save(argThat(a ->
                siblingClone.getId().equals(a.getPlanEstudio().getId())
                        && asigClone.getOrigenMaestroId().equals(a.getOrigenMaestroId())
        ));
    }

    @Test
    @DisplayName("Edición y cambio de dictado en Académica actualiza o crea clon en Autoevaluación")
    void deberiaPropagarActualizacionYDictadoDesdeMaestro() {
        ActividadCurricularPlanJpaEntity asigMaster = crearAsignatura(masterPlan, "101", "Análisis Matemático I", EstadoDictadoActividad.ACTIVA, null);
        ActividadCurricularPlanJpaEntity asigClon = crearAsignatura(clonePlan, "101", "Análisis 1", EstadoDictadoActividad.ACTIVA, asigMaster.getId());

        when(planRepository.findByOrigenMaestroIdAndActivoTrue(masterPlan.getId())).thenReturn(List.of(clonePlan));
        when(asignaturaRepository.findByPlanEstudioIdAndOrigenMaestroId(clonePlan.getId(), asigMaster.getId()))
                .thenReturn(Optional.of(asigClon));
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(clonePlan.getId())).thenReturn(List.of(asigClon));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarActualizacionAsignatura(masterPlan, asigMaster);

        assertEquals("Análisis Matemático I", asigClon.getNombre());
        verify(asignaturaRepository).save(asigClon);
    }

    @Test
    @DisplayName("Edición en Autoevaluación actualiza el registro maestro de Académica")
    void deberiaPropagarActualizacionDesdeAutoevaluacionAMaestro() {
        ActividadCurricularPlanJpaEntity asigMaster = crearAsignatura(masterPlan, "101", "Análisis I", EstadoDictadoActividad.ACTIVA, null);
        ActividadCurricularPlanJpaEntity asigClon = crearAsignatura(clonePlan, "101", "Análisis I Actualizada", EstadoDictadoActividad.ACTIVA, asigMaster.getId());

        when(asignaturaRepository.findById(asigMaster.getId())).thenReturn(Optional.of(asigMaster));
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(masterPlan.getId())).thenReturn(List.of(asigMaster));
        when(asignaturaRepository.findByOrigenMaestroId(asigMaster.getId())).thenReturn(List.of(asigClon));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarActualizacionAsignatura(clonePlan, asigClon);

        assertEquals("Análisis I Actualizada", asigMaster.getNombre());
        verify(asignaturaRepository).save(asigMaster);
    }

    @Test
    @DisplayName("Eliminación en Académica elimina los clones en Autoevaluación y viceversa")
    void deberiaPropagarEliminacionEnAmbasDirecciones() {
        ActividadCurricularPlanJpaEntity asigMaster = crearAsignatura(masterPlan, "101", "Análisis I", EstadoDictadoActividad.ACTIVA, null);
        ActividadCurricularPlanJpaEntity asigClon = crearAsignatura(clonePlan, "101", "Análisis I", EstadoDictadoActividad.ACTIVA, asigMaster.getId());

        // Caso 1: Eliminación desde Académica
        when(asignaturaRepository.findByOrigenMaestroId(asigMaster.getId())).thenReturn(List.of(asigClon));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(clonePlan.getId())).thenReturn(List.of());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarEliminacionAsignatura(masterPlan, asigMaster);
        verify(asignaturaRepository).delete(asigClon);

        // Caso 2: Eliminación desde Autoevaluación hacia Académica
        when(asignaturaRepository.findById(asigMaster.getId())).thenReturn(Optional.of(asigMaster));
        when(asignaturaRepository.findByOrigenMaestroId(asigMaster.getId())).thenReturn(List.of());
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(masterPlan.getId())).thenReturn(List.of());

        service.propagarEliminacionAsignatura(clonePlan, asigClon);
        verify(asignaturaRepository).delete(asigMaster);
    }

    @Test
    @DisplayName("Actualización de metadatos de plan se propaga en ambas direcciones")
    void deberiaPropagarActualizacionDeMetadatosDePlan() {
        masterPlan.setNombrePlan("Agro-2026 Actualizado");
        when(planRepository.findByOrigenMaestroIdAndActivoTrue(masterPlan.getId())).thenReturn(List.of(clonePlan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.propagarActualizacionPlan(masterPlan);

        assertEquals("Agro-2026 Actualizado", clonePlan.getNombrePlan());
        verify(planRepository).save(clonePlan);
    }

    @Test
    @DisplayName("Plan creado directo en Autoevaluación crea su plan maestro en Académica y enlaza asignaturas")
    void deberiaCrearMaestroYVincularCuandoPlanSeCreaEnAutoevaluacion() {
        PlanEstudioFt001JpaEntity planSinMaestro = new PlanEstudioFt001JpaEntity();
        planSinMaestro.setId(UUID.randomUUID());
        planSinMaestro.setCarrera(carrera);
        planSinMaestro.setProcesoId(UUID.randomUUID());
        planSinMaestro.setCodigoResolucion("RES-CS-2026");
        planSinMaestro.setAnioAprobacion(2026);
        planSinMaestro.setEsMaestro(false);
        planSinMaestro.setOrigenMaestroId(null);

        ActividadCurricularPlanJpaEntity asigInicial = crearAsignatura(planSinMaestro, "101", "Química", EstadoDictadoActividad.ACTIVA, null);

        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planSinMaestro.getId())).thenReturn(List.of(asigInicial));
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.vincularOCrearMaestroParaPlanDeProceso(planSinMaestro);

        assertNotNull(planSinMaestro.getOrigenMaestroId());
        assertNotNull(asigInicial.getOrigenMaestroId());
        verify(planRepository).save(argThat(p ->
                Boolean.TRUE.equals(p.getEsMaestro())
                        && p.getProcesoId() == null
                        && p.getId().equals(planSinMaestro.getOrigenMaestroId())
        ));
    }

    private ActividadCurricularPlanJpaEntity crearAsignatura(
            PlanEstudioFt001JpaEntity plan,
            String codigo,
            String nombre,
            EstadoDictadoActividad estadoDictado,
            UUID origenMaestroId
    ) {
        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setPlanEstudio(plan);
        asig.setNumeroOrden(1);
        asig.setCodigo(codigo);
        asig.setNombre(nombre);
        asig.setGrupo("C.B.I.");
        asig.setSubgrupo("1º");
        asig.setHorasReloj(100);
        asig.setHorasPresenciales(100);
        asig.setHorasNoPresenciales(0);
        asig.setEstadoDictado(estadoDictado);
        asig.setOrigenMaestroId(origenMaestroId);
        asig.setCreadoEn(Instant.now());
        return asig;
    }
}
