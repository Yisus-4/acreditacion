package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlanTrabajoTest {

    @Test
    @DisplayName("Debe inicializar un plan de trabajo incremental con la Actividad 1 en estado PENDIENTE")
    void debeInicializarPlanTrabajoConActividadInicial() {
        UUID procesoId = UUID.randomUUID();
        String codigo = "PT-001-ISI-2026";

        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, codigo, TipoPlanTrabajo.PT_001_ACREDITACION);

        assertNotNull(plan.getId());
        assertEquals(procesoId, plan.getProcesoId());
        assertEquals(codigo, plan.getCodigo());
        assertEquals(TipoPlanTrabajo.PT_001_ACREDITACION, plan.getTipo());
        assertEquals(EstadoPlanTrabajo.ACTIVO, plan.getEstado());
        assertNotNull(plan.getCreadoEn());
        assertNotNull(plan.getActualizadoEn());

        // Debe contener la actividad 1
        assertEquals(1, plan.getActividades().size());
        ActividadPlanTrabajo act1 = plan.getActividades().get(0);
        assertEquals(1, act1.getNumeroOrden());
        assertEquals(ActividadPlanTrabajo.CODIGO_ACT_01, act1.getCodigoActividad());
        assertEquals(ActividadPlanTrabajo.NOMBRE_ACT_01, act1.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, act1.getEstado());
        assertNull(act1.getFechaReunionCoordinacion());
        assertNull(act1.getSintesisAcuerdos());
        assertNull(act1.getCompletadaEn());
        assertNull(act1.getCompletadaPor());
    }

    @Test
    @DisplayName("Debe permitir anexar actividades futuras respetando número de orden único")
    void debeAnexarActividadFutura() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        plan.anexarActividad(2, "ACT-02-DESIGNAR-COMISION", "Conformación de Comisión de Autoevaluación");

        assertEquals(2, plan.getActividades().size());
        ActividadPlanTrabajo act2 = plan.getActividades().get(1);
        assertEquals(2, act2.getNumeroOrden());
        assertEquals("ACT-02-DESIGNAR-COMISION", act2.getCodigoActividad());
        assertEquals(EstadoActividad.PENDIENTE, act2.getEstado());
    }

    @Test
    @DisplayName("Debe rechazar anexar actividad con orden duplicado")
    void debeRechazarOrdenDuplicado() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        assertThrows(DomainValidationException.class, () ->
                plan.anexarActividad(1, "OTRA-ACT", "Otra actividad")
        );
    }

    @Test
    @DisplayName("Debe rechazar anexar actividades si el plan ya está finalizado")
    void debeRechazarAnexarSiPlanFinalizado() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        plan.finalizar();
        assertEquals(EstadoPlanTrabajo.FINALIZADO, plan.getEstado());

        assertThrows(DomainValidationException.class, () ->
                plan.anexarActividad(2, "ACT-02-DESIGNAR-COMISION", "Conformación de Comisión")
        );
    }
}
