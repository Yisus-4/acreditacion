package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidarPrecedenciaAmbitosServiceTest {

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @Mock
    private SpringDataProcesoAcreditacionRepository procesoRepository;

    @InjectMocks
    private ValidarPrecedenciaAmbitosService service;

    private UUID procesoId;
    private UUID carreraId;

    @BeforeEach
    void setUp() {
        procesoId = UUID.randomUUID();
        carreraId = UUID.randomUUID();
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

    @Test
    @DisplayName("deberiaPermitirCompletarConFallbackACarreraIdCuandoProcesoNoTienePlan")
    void deberiaPermitirCompletarConFallbackACarreraIdCuandoProcesoNoTienePlan() {
        PlanEstudioFt001JpaEntity planCarrera = new PlanEstudioFt001JpaEntity();
        planCarrera.setId(UUID.randomUUID());

        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setNombre("Geodesia I");

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());
        when(planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId)).thenReturn(List.of(planCarrera));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planCarrera.getId())).thenReturn(List.of(asig));

        assertTrue(service.puedeCompletar(procesoId, carreraId));
        assertDoesNotThrow(() -> service.validar(procesoId, carreraId, "INTERNOS_FT008"));
    }

    @Test
    @DisplayName("deberiaPermitirCompletarConFallbackACarreraBuscandoProcesoRepositoryCuandoCarreraIdEsNull")
    void deberiaPermitirCompletarConFallbackACarreraBuscandoProcesoRepositoryCuandoCarreraIdEsNull() {
        ProcesoAcreditacionJpaEntity proceso = new ProcesoAcreditacionJpaEntity();
        proceso.setId(procesoId);
        proceso.setCarreraId(carreraId);

        PlanEstudioFt001JpaEntity planCarrera = new PlanEstudioFt001JpaEntity();
        planCarrera.setId(UUID.randomUUID());

        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setNombre("Geodesia I");

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId)).thenReturn(List.of(planCarrera));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planCarrera.getId())).thenReturn(List.of(asig));

        assertTrue(service.puedeCompletar(procesoId));
        assertDoesNotThrow(() -> service.validar(procesoId, "EXTERNOS_FT009"));
    }

    @Test
    @DisplayName("deberiaRechazarSiPlanCarreraTampocoTieneAsignaturas")
    void deberiaRechazarSiPlanCarreraTampocoTieneAsignaturas() {
        PlanEstudioFt001JpaEntity planCarrera = new PlanEstudioFt001JpaEntity();
        planCarrera.setId(UUID.randomUUID());

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());
        when(planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId)).thenReturn(List.of(planCarrera));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planCarrera.getId())).thenReturn(Collections.emptyList());

        assertFalse(service.puedeCompletar(procesoId, carreraId));
        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> service.validar(procesoId, carreraId, "DEMOSTRATIVAS_FT010")
        );
        assertTrue(ex.getMessage().contains("Precedencia requerida"));
    }
}
