package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EliminarAsignaturaPlanServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @Mock
    private SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository;

    @Mock
    private SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository;

    @InjectMocks
    private EliminarAsignaturaPlanService service;

    private UUID planId;
    private UUID procesoId;
    private UUID asigId;
    private PlanEstudioFt001JpaEntity plan;
    private ActividadCurricularPlanJpaEntity asignatura;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        procesoId = UUID.randomUUID();
        asigId = UUID.randomUUID();

        plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setProcesoId(procesoId);

        asignatura = new ActividadCurricularPlanJpaEntity();
        asignatura.setId(asigId);
        asignatura.setPlanEstudio(plan);
        asignatura.setCodigo("AGR-101");
        asignatura.setNombre("Física");
        asignatura.setHorasReloj(60);
        asignatura.setHorasPresenciales(50);
        asignatura.setHorasNoPresenciales(10);
    }

    @Test
    @DisplayName("deberiaPermitirEliminarSiAsignaturaTieneNombreSimilarPeroNoTokenExacto")
    void deberiaPermitirEliminarSiAsignaturaTieneNombreSimilarPeroNoTokenExacto() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));

        // Ambito 1 tiene "Física II", NO debe bloquear "Física"
        AutoevaluacionCarreraAmbitoJpaEntity vinculo1 = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo1.setProcesoId(procesoId);
        vinculo1.setUtilizadoPorCarrera(true);
        vinculo1.setAsignaturasVinculadas("Física II, Química Orgánica");
        AmbitoPracticaJpaEntity a1 = new AmbitoPracticaJpaEntity();
        a1.setDenominacion("Laboratorio de Física Avanzada");
        a1.setEsMaestro(false);
        a1.setProcesoId(procesoId);
        a1.setActivo(true);
        vinculo1.setAmbito(a1);

        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculo1));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura));

        assertDoesNotThrow(() -> service.eliminarAsignatura(planId, asigId));
        verify(asignaturaRepository).delete(asignatura);
    }

    @Test
    @DisplayName("deberiaBloquearEliminacionSiAsignaturaVinculadaAAmbitoConTokenMatching")
    void deberiaBloquearEliminacionSiAsignaturaVinculadaAAmbitoConTokenMatching() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));

        // Vinculo tiene "Química; Física; Análisis", sí debe bloquear por token matching
        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setProcesoId(procesoId);
        vinculo.setUtilizadoPorCarrera(true);
        vinculo.setAsignaturasVinculadas("Química; Física; Análisis");
        AmbitoPracticaJpaEntity a = new AmbitoPracticaJpaEntity();
        a.setDenominacion("Laboratorio de Física y Química");
        a.setEsMaestro(false);
        a.setProcesoId(procesoId);
        a.setActivo(true);
        vinculo.setAmbito(a);

        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculo));

        BusinessRuleConflictException ex = assertThrows(
                BusinessRuleConflictException.class,
                () -> service.eliminarAsignatura(planId, asigId)
        );
        assertTrue(ex.getMessage().contains("Laboratorio de Física y Química"));
        verify(asignaturaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deberiaBloquearEliminacionSiEsCorrelativaDeOtraAsignaturaConTokenMatching")
    void deberiaBloquearEliminacionSiEsCorrelativaDeOtraAsignaturaConTokenMatching() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));
        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of());

        // Otra asignatura que tiene correlativa "Física, Álgebra"
        ActividadCurricularPlanJpaEntity otraAsig = new ActividadCurricularPlanJpaEntity();
        otraAsig.setId(UUID.randomUUID());
        otraAsig.setPlanEstudio(plan);
        otraAsig.setNombre("Física II");
        otraAsig.setCorrelativas("Física, Álgebra");

        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura, otraAsig));

        BusinessRuleConflictException ex = assertThrows(
                BusinessRuleConflictException.class,
                () -> service.eliminarAsignatura(planId, asigId)
        );
        assertTrue(ex.getMessage().contains("Física II"));
        verify(asignaturaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deberiaEliminarAsignaturaSinDependenciasYRecalcularHoras")
    void deberiaEliminarAsignaturaSinDependenciasYRecalcularHoras() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));
        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of());

        // Asignatura restante en el plan
        ActividadCurricularPlanJpaEntity restante = new ActividadCurricularPlanJpaEntity();
        restante.setId(UUID.randomUUID());
        restante.setPlanEstudio(plan);
        restante.setNombre("Geometría");
        restante.setCorrelativas("Sin correlativas");
        restante.setHorasReloj(40);
        restante.setHorasPresenciales(40);
        restante.setHorasNoPresenciales(0);

        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura, restante)) // Before delete check
                .thenReturn(List.of(restante)); // After delete recalculation

        service.eliminarAsignatura(planId, asigId);

        verify(asignaturaRepository).delete(asignatura);
        verify(planRepository).save(argThat(p ->
                p.getHorasPresenciales() == 40 &&
                p.getHorasNoPresenciales() == 0 &&
                p.getHorasTotales() == 40
        ));
    }

    @Test
    @DisplayName("deberiaIgnorarVinculacionHuerfanaAAmbitoMaestroYPermitirEliminar")
    void deberiaIgnorarVinculacionHuerfanaAAmbitoMaestroYPermitirEliminar() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));

        AmbitoPracticaJpaEntity ambitoMaestro = new AmbitoPracticaJpaEntity();
        ambitoMaestro.setId(UUID.randomUUID());
        ambitoMaestro.setDenominacion("Gabinete de Enfermería");
        ambitoMaestro.setEsMaestro(true);
        ambitoMaestro.setProcesoId(null);
        ambitoMaestro.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoHuerfano = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoHuerfano.setProcesoId(procesoId);
        vinculoHuerfano.setUtilizadoPorCarrera(true);
        vinculoHuerfano.setAsignaturasVinculadas("Física");
        vinculoHuerfano.setAmbito(ambitoMaestro);

        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculoHuerfano));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura));

        assertDoesNotThrow(() -> service.eliminarAsignatura(planId, asigId));
        verify(asignaturaRepository).delete(asignatura);
    }

    @Test
    @DisplayName("deberiaBloquearPorVinculoRelacionalExactoSinAfectarAsignaturaHomonima")
    void deberiaBloquearPorVinculoRelacionalExactoSinAfectarAsignaturaHomonima() {
        UUID otraId = UUID.randomUUID();
        ActividadCurricularPlanJpaEntity asigAcademica = new ActividadCurricularPlanJpaEntity();
        asigAcademica.setId(asigId);
        asigAcademica.setPlanEstudio(plan);
        asigAcademica.setCodigo("101");
        asigAcademica.setNombre("Analisis 1");
        asigAcademica.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);

        ActividadCurricularPlanJpaEntity asigHomonima = new ActividadCurricularPlanJpaEntity();
        asigHomonima.setId(otraId);
        asigHomonima.setPlanEstudio(plan);
        asigHomonima.setCodigo("ASIG-01");
        asigHomonima.setNombre("analisis 1");
        asigHomonima.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);

        AmbitoPracticaJpaEntity campo = new AmbitoPracticaJpaEntity();
        campo.setId(UUID.randomUUID());
        campo.setDenominacion("Campo Experimental");
        campo.setEsMaestro(false);
        campo.setProcesoId(procesoId);
        campo.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setId(UUID.randomUUID());
        vinculo.setProcesoId(procesoId);
        vinculo.setUtilizadoPorCarrera(true);
        vinculo.setAmbito(campo);

        AutoevaluacionAmbitoActividadJpaEntity linkExacto =
                new AutoevaluacionAmbitoActividadJpaEntity(UUID.randomUUID(), vinculo, asigHomonima);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asigAcademica));
        when(asignaturaRepository.findById(otraId)).thenReturn(Optional.of(asigHomonima));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asigAcademica, asigHomonima));
        when(autoevaluacionAmbitoActividadRepository.findActiveUsagesByActividadAndProceso(asigId, procesoId))
                .thenReturn(List.of());
        when(autoevaluacionAmbitoActividadRepository.findActiveUsagesByActividadAndProceso(otraId, procesoId))
                .thenReturn(List.of(linkExacto));
        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculo));

        assertDoesNotThrow(() -> service.eliminarAsignatura(planId, asigId));
        verify(asignaturaRepository).delete(asigAcademica);

        BusinessRuleConflictException ex = assertThrows(
                BusinessRuleConflictException.class,
                () -> service.eliminarAsignatura(planId, otraId)
        );
        assertTrue(ex.getMessage().contains("Campo Experimental"));
        verify(asignaturaRepository, never()).delete(asigHomonima);
    }

    @Test
    @DisplayName("deberiaPermitirEliminarDuplicadoHomonimoConTextoLegadoYBloquearUltima")
    void deberiaPermitirEliminarDuplicadoHomonimoConTextoLegadoYBloquearUltima() {
        UUID otraId = UUID.randomUUID();
        ActividadCurricularPlanJpaEntity duplicada = new ActividadCurricularPlanJpaEntity();
        duplicada.setId(otraId);
        duplicada.setPlanEstudio(plan);
        duplicada.setCodigo("ASIG-01");
        duplicada.setNombre("física");
        duplicada.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);

        AmbitoPracticaJpaEntity ambitoClon = new AmbitoPracticaJpaEntity();
        ambitoClon.setId(UUID.randomUUID());
        ambitoClon.setDenominacion("Laboratorio Central");
        ambitoClon.setEsMaestro(false);
        ambitoClon.setProcesoId(procesoId);
        ambitoClon.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoLegado = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoLegado.setId(UUID.randomUUID());
        vinculoLegado.setProcesoId(procesoId);
        vinculoLegado.setUtilizadoPorCarrera(true);
        vinculoLegado.setAsignaturasVinculadas("física");
        vinculoLegado.setAmbito(ambitoClon);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(otraId)).thenReturn(Optional.of(duplicada));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));
        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculoLegado));

        // Mientras existe otra homónima activa en el plan, permite eliminar el duplicado
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura, duplicada))
                .thenReturn(List.of(asignatura));

        assertDoesNotThrow(() -> service.eliminarAsignatura(planId, otraId));
        verify(asignaturaRepository).delete(duplicada);

        // Cuando queda la última con ese nombre, el texto legado sí bloquea
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura));

        BusinessRuleConflictException ex = assertThrows(
                BusinessRuleConflictException.class,
                () -> service.eliminarAsignatura(planId, asigId)
        );
        assertTrue(ex.getMessage().contains("Laboratorio Central"));
    }

    @Test
    @DisplayName("deberiaIgnorarVinculacionDeOtraCarreraEnMismoProceso")
    void deberiaIgnorarVinculacionDeOtraCarreraEnMismoProceso() {
        UUID carreraPlanId = UUID.randomUUID();
        UUID otraCarreraId = UUID.randomUUID();
        CarreraJpaEntity carreraPlan = new CarreraJpaEntity();
        carreraPlan.setId(carreraPlanId);
        plan.setCarrera(carreraPlan);

        AmbitoPracticaJpaEntity ambitoClon = new AmbitoPracticaJpaEntity();
        ambitoClon.setId(UUID.randomUUID());
        ambitoClon.setDenominacion("Laboratorio Otra Carrera");
        ambitoClon.setEsMaestro(false);
        ambitoClon.setProcesoId(procesoId);
        ambitoClon.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoOtraCarrera = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoOtraCarrera.setId(UUID.randomUUID());
        vinculoOtraCarrera.setProcesoId(procesoId);
        vinculoOtraCarrera.setCarreraId(otraCarreraId);
        vinculoOtraCarrera.setUtilizadoPorCarrera(true);
        vinculoOtraCarrera.setAsignaturasVinculadas("Física");
        vinculoOtraCarrera.setAmbito(ambitoClon);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));
        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(procesoId))
                .thenReturn(List.of(vinculoOtraCarrera));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura));

        assertDoesNotThrow(() -> service.eliminarAsignatura(planId, asigId));
        verify(asignaturaRepository).delete(asignatura);
    }
}
