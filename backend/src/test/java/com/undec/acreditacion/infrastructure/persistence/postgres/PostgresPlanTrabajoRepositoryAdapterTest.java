package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostgresPlanTrabajoRepositoryAdapterTest {

    private SpringDataPlanTrabajoRepository planRepository;
    private SpringDataActividadPlanTrabajoRepository actividadRepository;
    private PlanTrabajoPersistenceMapper mapper;
    private PostgresPlanTrabajoRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        planRepository = mock(SpringDataPlanTrabajoRepository.class);
        actividadRepository = mock(SpringDataActividadPlanTrabajoRepository.class);
        mapper = new PlanTrabajoPersistenceMapper();
        adapter = new PostgresPlanTrabajoRepositoryAdapter(planRepository, actividadRepository, mapper);
    }

    @Test
    @DisplayName("Debe guardar PlanTrabajo y sus actividades asociadas")
    void debeGuardarPlanTrabajo() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo domain = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        when(planRepository.save(any(PlanTrabajoJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(actividadRepository.save(any(ActividadPlanTrabajoJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActividadPlanTrabajoJpaEntity actJpa = new ActividadPlanTrabajoJpaEntity();
        actJpa.setId(domain.getActividades().get(0).getId());
        actJpa.setPlanTrabajoId(domain.getId());
        actJpa.setNumeroOrden(1);
        actJpa.setCodigoActividad(ActividadPlanTrabajo.CODIGO_ACT_01);
        actJpa.setNombre(ActividadPlanTrabajo.NOMBRE_ACT_01);
        actJpa.setEstado("PENDIENTE");
        actJpa.setCreadoEn(Instant.now());
        actJpa.setActualizadoEn(Instant.now());

        when(actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(domain.getId()))
                .thenReturn(List.of(actJpa));

        PlanTrabajo guardado = adapter.save(domain);

        assertNotNull(guardado);
        assertEquals(domain.getId(), guardado.getId());
        assertEquals(1, guardado.getActividades().size());
        verify(planRepository).save(any(PlanTrabajoJpaEntity.class));
        verify(actividadRepository).save(any(ActividadPlanTrabajoJpaEntity.class));
    }

    @Test
    @DisplayName("Debe buscar PlanTrabajo por ID (H-01)")
    void debeBuscarPlanPorId() {
        UUID planId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();

        PlanTrabajoJpaEntity planJpa = new PlanTrabajoJpaEntity();
        planJpa.setId(planId);
        planJpa.setProcesoId(procesoId);
        planJpa.setCodigo("PT-001-ISI-2026");
        planJpa.setTipo("PT_001_ACREDITACION");
        planJpa.setEstado("ACTIVO");
        planJpa.setCreadoEn(Instant.now());
        planJpa.setActualizadoEn(Instant.now());

        when(planRepository.findById(planId)).thenReturn(Optional.of(planJpa));
        when(actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(planId)).thenReturn(Collections.emptyList());

        Optional<PlanTrabajo> optPlan = adapter.findById(planId);

        assertTrue(optPlan.isPresent());
        assertEquals(planId, optPlan.get().getId());
        assertEquals(procesoId, optPlan.get().getProcesoId());
    }

    @Test
    @DisplayName("Debe buscar actividad por ID")
    void debeBuscarActividadPorId() {
        UUID actId = UUID.randomUUID();
        ActividadPlanTrabajoJpaEntity actJpa = new ActividadPlanTrabajoJpaEntity();
        actJpa.setId(actId);
        actJpa.setPlanTrabajoId(UUID.randomUUID());
        actJpa.setNumeroOrden(1);
        actJpa.setCodigoActividad("ACT-01-INFORMAR-PROCESO");
        actJpa.setNombre("Informar sobre el proceso de acreditación");
        actJpa.setEstado("PENDIENTE");
        actJpa.setCreadoEn(Instant.now());
        actJpa.setActualizadoEn(Instant.now());

        when(actividadRepository.findById(actId)).thenReturn(Optional.of(actJpa));

        Optional<ActividadPlanTrabajo> optAct = adapter.findActividadById(actId);

        assertTrue(optAct.isPresent());
        assertEquals("ACT-01-INFORMAR-PROCESO", optAct.get().getCodigoActividad());
    }
}
