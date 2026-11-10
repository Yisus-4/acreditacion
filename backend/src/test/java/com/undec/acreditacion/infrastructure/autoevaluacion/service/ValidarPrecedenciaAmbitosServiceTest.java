package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidarPrecedenciaAmbitosServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @InjectMocks
    private ValidarPrecedenciaAmbitosService service;

    private UUID procesoId;

    @BeforeEach
    void setUp() {
        procesoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("deberiaLanzarDomainValidationExceptionSiPlanNoTieneAsignaturas")
    void deberiaLanzarDomainValidationExceptionSiPlanNoTieneAsignaturas() {
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setProcesoId(procesoId);

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(List.of(plan));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(plan.getId())).thenReturn(List.of());

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> service.validarPrecedenciaParaCompletar(procesoId, "INTERNOS_FT008")
        );

        assertTrue(ex.getMessage().contains("Precedencia requerida"));
        assertTrue(ex.getMessage().contains("CONEAU PRG-002"));
    }

    @Test
    @DisplayName("deberiaPermitirCompletarSiPlanTieneAsignaturas")
    void deberiaPermitirCompletarSiPlanTieneAsignaturas() {
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setProcesoId(procesoId);

        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setNombre("Topografía");

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(List.of(plan));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(plan.getId())).thenReturn(List.of(asig));

        assertDoesNotThrow(() -> service.validarPrecedenciaParaCompletar(procesoId, "INTERNOS_FT008"));
    }
}
