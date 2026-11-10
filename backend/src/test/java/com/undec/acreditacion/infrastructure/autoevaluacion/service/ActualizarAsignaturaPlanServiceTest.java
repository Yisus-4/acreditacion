package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActualizarAsignaturaPlanServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @InjectMocks
    private ActualizarAsignaturaPlanService service;

    private UUID planId;
    private UUID asigId;
    private PlanEstudioFt001JpaEntity plan;
    private ActividadCurricularPlanJpaEntity asignatura;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        asigId = UUID.randomUUID();

        plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);

        asignatura = new ActividadCurricularPlanJpaEntity();
        asignatura.setId(asigId);
        asignatura.setPlanEstudio(plan);
        asignatura.setCodigo("AGR-101");
        asignatura.setNombre("Física I");
        asignatura.setGrupo("C.B.I.");
        asignatura.setSubgrupo("1° Año");
        asignatura.setHorasReloj(64);
        asignatura.setHorasPresenciales(64);
        asignatura.setHorasNoPresenciales(0);
        asignatura.setCorrelativas("Sin correlativas previas");
        asignatura.setEsObligatoria(true);
        asignatura.setNumeroOrden(1);
    }

    @Test
    @DisplayName("deberiaRechazarAutocorrelatividadPorIdONombre")
    void deberiaRechazarAutocorrelatividadPorIdONombre() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));

        ActualizarAsignaturaRequest requestConMismoNombre = new ActualizarAsignaturaRequest(
                asigId,
                "AGR-101",
                "Física I",
                "C.B.I.",
                "1° Año",
                64,
                64,
                0,
                "Física I, Álgebra",
                true,
                1
        );

        DomainValidationException ex1 = assertThrows(
                DomainValidationException.class,
                () -> service.actualizarAsignatura(planId, asigId, requestConMismoNombre)
        );
        assertTrue(ex1.getMessage().contains("correlativa de sí misma"));

        ActualizarAsignaturaRequest requestConMismoCodigo = new ActualizarAsignaturaRequest(
                asigId,
                "AGR-101",
                "Física I",
                "C.B.I.",
                "1° Año",
                64,
                64,
                0,
                "AGR-101",
                true,
                1
        );

        DomainValidationException ex2 = assertThrows(
                DomainValidationException.class,
                () -> service.actualizarAsignatura(planId, asigId, requestConMismoCodigo)
        );
        assertTrue(ex2.getMessage().contains("correlativa de sí misma"));
    }

    @Test
    @DisplayName("deberiaActualizarCamposCorrectamenteYRecalcularHoras")
    void deberiaActualizarCamposCorrectamenteYRecalcularHoras() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findById(asigId)).thenReturn(Optional.of(asignatura));
        when(asignaturaRepository.save(any(ActividadCurricularPlanJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId))
                .thenReturn(List.of(asignatura));

        ActualizarAsignaturaRequest request = new ActualizarAsignaturaRequest(
                asigId,
                "AGR-101-MOD",
                "Física I (Plan Nuevo)",
                "C.B.I.",
                "1° Año",
                80,
                70,
                10,
                "Sin correlativas previas",
                true,
                1
        );

        ActividadCurricularPlanDto result = service.actualizarAsignatura(planId, asigId, request);

        assertNotNull(result);
        assertEquals("Física I (Plan Nuevo)", result.nombre());
        assertEquals(80, result.horasReloj());
        assertEquals(70, result.horasPresenciales());
        assertEquals(10, result.horasNoPresenciales());

        verify(planRepository).save(argThat(p ->
                p.getHorasPresenciales() == 70 &&
                p.getHorasNoPresenciales() == 10 &&
                p.getHorasTotales() == 80
        ));
    }
}
