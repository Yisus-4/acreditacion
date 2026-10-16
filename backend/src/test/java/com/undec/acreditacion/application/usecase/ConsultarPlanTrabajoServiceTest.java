package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.TipoPlanTrabajo;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarPlanTrabajoServiceTest {

    private PlanTrabajoRepositoryPort repository;
    private ConsultarPlanTrabajoService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlanTrabajoRepositoryPort.class);
        service = new ConsultarPlanTrabajoService(repository);
    }

    @Test
    @DisplayName("Debe retornar plan de trabajo por procesoId")
    void debeRetornarPlanPorProcesoId() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        when(repository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        PlanTrabajo res = service.obtenerPorProcesoId(procesoId);
        assertNotNull(res);
        assertEquals(procesoId, res.getProcesoId());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el plan no existe")
    void debeLanzarExcepcionSiNoExiste() {
        UUID procesoId = UUID.randomUUID();
        when(repository.findByProcesoId(procesoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorProcesoId(procesoId));
    }
}
