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

    @Test
    @DisplayName("Debe auto-agregar y persistir Actividad 1.3 si el plan consultado no la posee")
    void debeAutoAgregarActividadNormativaSiNoExiste() {
        UUID procesoId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        com.undec.acreditacion.domain.entities.ActividadPlanTrabajo act1 =
                com.undec.acreditacion.domain.entities.ActividadPlanTrabajo.crearActividadInicial(planId);
        PlanTrabajo planSinNormativa = PlanTrabajo.rehydrate(
                planId,
                procesoId,
                "PT-001-TEST",
                TipoPlanTrabajo.PT_001_ACREDITACION,
                com.undec.acreditacion.domain.entities.EstadoPlanTrabajo.ACTIVO,
                null,
                null,
                java.util.List.of(act1)
        );

        when(repository.findByProcesoId(procesoId)).thenReturn(Optional.of(planSinNormativa));
        when(repository.save(any(PlanTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanTrabajo res = service.obtenerPorProcesoId(procesoId);

        assertNotNull(res);
        boolean tiene13 = res.getActividades().stream()
                .anyMatch(a -> "1.3".equals(a.getCodigoActividad()));
        assertTrue(tiene13);
        verify(repository).save(any(PlanTrabajo.class));
    }
}
