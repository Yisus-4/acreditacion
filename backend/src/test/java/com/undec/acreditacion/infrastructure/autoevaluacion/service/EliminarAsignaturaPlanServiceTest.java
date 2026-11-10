package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
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
}
