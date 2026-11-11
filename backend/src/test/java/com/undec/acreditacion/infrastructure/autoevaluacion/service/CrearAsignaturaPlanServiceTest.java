package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CrearAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearAsignaturaPlanServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @InjectMocks
    private CrearAsignaturaPlanService service;

    private UUID planId;
    private PlanEstudioFt001JpaEntity plan;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setHorasPresenciales(0);
        plan.setHorasNoPresenciales(0);
        plan.setHorasTotales(0);
    }

    @Test
    @DisplayName("deberiaAsignarOrdenAutoincrementalCuandoNoSeProvee")
    void deberiaAsignarOrdenAutoincrementalCuandoNoSeProvee() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findMaxNumeroOrdenByPlanEstudioId(planId)).thenReturn(4);
        when(asignaturaRepository.save(any(ActividadCurricularPlanJpaEntity.class)))
                .thenAnswer(inv -> {
                    ActividadCurricularPlanJpaEntity entity = inv.getArgument(0);
                    if (entity.getId() == null) entity.setId(UUID.randomUUID());
                    return entity;
                });
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(new ArrayList<>());

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "AGR-105",
                "Topografía Aplicada",
                "T.A.",
                "2° Año",
                64,
                48,
                16,
                "Sin correlativas previas",
                true,
                null // Sin orden explicito
        );

        ActividadCurricularPlanDto result = service.crearAsignatura(planId, request);

        assertNotNull(result);
        assertEquals(5, result.numeroOrden());
        assertEquals("Topografía Aplicada", result.nombre());
        verify(asignaturaRepository).save(argThat(entity -> entity.getNumeroOrden() == 5));
    }

    @Test
    @DisplayName("deberiaRechazarHorasNegativas")
    void deberiaRechazarHorasNegativas() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        CrearAsignaturaRequest requestConRelojNegativo = new CrearAsignaturaRequest(
                "AGR-101",
                "Álgebra",
                "C.B.I.",
                "1° Año",
                -10,
                64,
                0,
                null,
                true,
                1
        );

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> service.crearAsignatura(planId, requestConRelojNegativo)
        );
        assertTrue(ex.getMessage().contains("horas"));
    }

    @Test
    @DisplayName("deberiaRechazarCodigoDuplicadoEnMismoPlan")
    void deberiaRechazarCodigoDuplicadoEnMismoPlan() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.existsByPlanEstudioIdAndCodigo(planId, "AGR-101")).thenReturn(true);

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "AGR-101",
                "Álgebra II",
                "C.B.I.",
                "1° Año",
                64,
                64,
                0,
                null,
                true,
                2
        );

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> service.crearAsignatura(planId, request)
        );
        assertTrue(ex.getMessage().contains("código"));
    }

    @Test
    @DisplayName("deberiaRecalcularHorasTotalesYPorCicloConNullSafety")
    void deberiaRecalcularHorasTotalesYPorCicloConNullSafety() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.save(any(ActividadCurricularPlanJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Asignaturas existentes del plan (una con valores null defensivos)
        ActividadCurricularPlanJpaEntity a1 = new ActividadCurricularPlanJpaEntity();
        a1.setHorasReloj(60);
        a1.setHorasPresenciales(50);
        a1.setHorasNoPresenciales(10);

        ActividadCurricularPlanJpaEntity a2 = new ActividadCurricularPlanJpaEntity();
        a2.setHorasReloj(null); // Defensive null
        a2.setHorasPresenciales(30);
        a2.setHorasNoPresenciales(null); // Defensive null

        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(a1, a2));

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "AGR-102",
                "Química General",
                "C.B.I.",
                "1° Año",
                80,
                60,
                20,
                null,
                true,
                3
        );

        service.crearAsignatura(planId, request);

        ArgumentCaptor<PlanEstudioFt001JpaEntity> planCaptor = ArgumentCaptor.forClass(PlanEstudioFt001JpaEntity.class);
        verify(planRepository).save(planCaptor.capture());
        PlanEstudioFt001JpaEntity savedPlan = planCaptor.getValue();

        assertEquals(80, savedPlan.getHorasPresenciales()); // 50 + 30
        assertEquals(10, savedPlan.getHorasNoPresenciales()); // 10 + 0
        assertEquals(90, savedPlan.getHorasTotales()); // 80 + 10 (o 60 + 0 + 30)
    }

    @Test
    @DisplayName("deberiaPermitirCrearPrimeraAsignaturaSinCorrelativasEnPlanVacio")
    void deberiaPermitirCrearPrimeraAsignaturaSinCorrelativasEnPlanVacio() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.save(any(ActividadCurricularPlanJpaEntity.class)))
                .thenAnswer(inv -> {
                    ActividadCurricularPlanJpaEntity entity = inv.getArgument(0);
                    if (entity.getId() == null) entity.setId(UUID.randomUUID());
                    return entity;
                });
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(List.of());

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "SIS-101",
                "Introducción a los Algoritmos",
                "C.B.I.",
                "1º",
                64,
                48,
                16,
                null,
                true,
                1
        );

        ActividadCurricularPlanDto result = service.crearAsignatura(planId, request);

        assertNotNull(result);
        assertEquals("Introducción a los Algoritmos", result.nombre());
        assertEquals("Sin correlativas previas", result.correlativas());
    }

    @Test
    @DisplayName("deberiaRechazarCorrelativasCuandoNoExistenAsignaturasPreviasEnElPlan")
    void deberiaRechazarCorrelativasCuandoNoExistenAsignaturasPreviasEnElPlan() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(List.of());

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "SIS-102",
                "Programación I",
                "C.B.I.",
                "1º",
                64,
                48,
                16,
                "Introducción a los Algoritmos",
                true,
                1
        );

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> service.crearAsignatura(planId, request)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("correlativas"));
        verify(asignaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deberiaPermitirCorrelativasCuandoExistenAsignaturasPreviasEnElPlan")
    void deberiaPermitirCorrelativasCuandoExistenAsignaturasPreviasEnElPlan() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        ActividadCurricularPlanJpaEntity previa = new ActividadCurricularPlanJpaEntity();
        previa.setId(UUID.randomUUID());
        previa.setNombre("Introducción a los Algoritmos");
        previa.setHorasReloj(64);
        previa.setHorasPresenciales(48);
        previa.setHorasNoPresenciales(16);

        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(previa));
        when(asignaturaRepository.save(any(ActividadCurricularPlanJpaEntity.class)))
                .thenAnswer(inv -> {
                    ActividadCurricularPlanJpaEntity entity = inv.getArgument(0);
                    if (entity.getId() == null) entity.setId(UUID.randomUUID());
                    return entity;
                });

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "SIS-102",
                "Programación I",
                "C.B.I.",
                "1º",
                64,
                48,
                16,
                "Introducción a los Algoritmos",
                true,
                2
        );

        ActividadCurricularPlanDto result = service.crearAsignatura(planId, request);

        assertNotNull(result);
        assertEquals("Introducción a los Algoritmos", result.correlativas());
    }
}
