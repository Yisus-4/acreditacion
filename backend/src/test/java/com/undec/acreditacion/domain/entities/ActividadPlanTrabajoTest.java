package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActividadPlanTrabajoTest {

    @Test
    @DisplayName("Debe crear la actividad inicial con código y nombre oficial en estado PENDIENTE")
    void debeCrearActividadInicial() {
        UUID planId = UUID.randomUUID();

        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(planId);

        assertNotNull(actividad.getId());
        assertEquals(planId, actividad.getPlanTrabajoId());
        assertEquals(1, actividad.getNumeroOrden());
        assertEquals(ActividadPlanTrabajo.CODIGO_ACT_01, actividad.getCodigoActividad());
        assertEquals(ActividadPlanTrabajo.NOMBRE_ACT_01, actividad.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, actividad.getEstado());
        assertEquals(HojaPlanTrabajo.INFO, actividad.getHoja());
        assertEquals(PrioridadActividad.MEDIA, actividad.getPrioridad());
        assertNotNull(actividad.getCreadoEn());
        assertNotNull(actividad.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe definir y crear la actividad 2 Elaborar Plan de Trabajo")
    void debeCrearActividadElaborarPlan() {
        UUID planId = UUID.randomUUID();

        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);

        assertNotNull(actividad.getId());
        assertEquals(planId, actividad.getPlanTrabajoId());
        assertEquals(2, actividad.getNumeroOrden());
        assertEquals(ActividadPlanTrabajo.CODIGO_ACT_02, actividad.getCodigoActividad());
        assertEquals("ACT-02-ELABORAR-PLAN-TRABAJO", ActividadPlanTrabajo.CODIGO_ACT_02);
        assertEquals(ActividadPlanTrabajo.NOMBRE_ACT_02, actividad.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, actividad.getEstado());
        assertEquals(HojaPlanTrabajo.INFO, actividad.getHoja());
        assertEquals(PrioridadActividad.ALTA, actividad.getPrioridad());
    }

    @Test
    @DisplayName("Debe completar la actividad inicial exitosamente con fecha, síntesis (>=10 chars) y usuario AEA")
    void debeCompletarActividadExitosamente() {
        UUID planId = UUID.randomUUID();
        UUID usuarioAeaId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(planId);

        Instant fechaReunion = Instant.now().minusSeconds(3600);
        String sintesis = "Reunión de coordinación formal con la Dirección de Carrera para acordar plazos.";

        actividad.registrarReunionYCompletar(fechaReunion, sintesis, usuarioAeaId);

        assertEquals(EstadoActividad.COMPLETADA, actividad.getEstado());
        assertEquals(fechaReunion, actividad.getFechaReunionCoordinacion());
        assertEquals(sintesis, actividad.getSintesisAcuerdos());
        assertEquals(usuarioAeaId, actividad.getCompletadaPor());
        assertNotNull(actividad.getCompletadaEn());
        assertNotNull(actividad.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe completar actividad sin reunión (como ACT-02) auditando usuario AEA")
    void debeCompletarActividadConAuditoria() {
        UUID planId = UUID.randomUUID();
        UUID usuarioAeaId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);

        actividad.completar(usuarioAeaId);

        assertEquals(EstadoActividad.COMPLETADA, actividad.getEstado());
        assertEquals(usuarioAeaId, actividad.getCompletadaPor());
        assertNotNull(actividad.getCompletadaEn());
    }

    @Test
    @DisplayName("Debe rechazar completar si la actividad ya fue completada")
    void debeRechazarRecompletar() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());
        actividad.registrarReunionYCompletar(Instant.now(), "Síntesis válida con más de 10 caracteres.", UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(Instant.now(), "Otra síntesis válida.", UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Debe rechazar si la fecha de reunión es nula")
    void debeRechazarFechaReunionNula() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(null, "Síntesis válida de acuerdos.", UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Debe rechazar si la síntesis de acuerdos es nula, vacía o menor a 10 caracteres")
    void debeRechazarSintesisInvalida() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(Instant.now(), null, UUID.randomUUID())
        );

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(Instant.now(), "   ", UUID.randomUUID())
        );

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(Instant.now(), "123456789", UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Debe rechazar si el usuario AEA responsable es nulo")
    void debeRechazarUsuarioAeaNulo() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarReunionYCompletar(Instant.now(), "Síntesis válida con más de 10 caracteres.", null)
        );
    }

    @Test
    @DisplayName("Debe crear actividad de cronograma con hoja, fechas estimadas, responsable y prioridad")
    void debeCrearActividadCronogramaValida() {
        UUID planId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 4, 1);
        LocalDate fin = LocalDate.of(2026, 5, 15);

        ActividadPlanTrabajo act = ActividadPlanTrabajo.crearActividadCronograma(
                planId,
                3,
                "ACT-03-DOCENTES",
                "Relevamiento cuerpo docente y CVs",
                HojaPlanTrabajo.INFO,
                inicio,
                fin,
                "Director de Carrera",
                PrioridadActividad.ALTA
        );

        assertEquals(HojaPlanTrabajo.INFO, act.getHoja());
        assertEquals(inicio, act.getFechaInicioEstimada());
        assertEquals(fin, act.getFechaFinEstimada());
        assertEquals("Director de Carrera", act.getResponsable());
        assertEquals(PrioridadActividad.ALTA, act.getPrioridad());
    }

    @Test
    @DisplayName("Debe rechazar actividad si fecha fin estimada es anterior a fecha inicio estimada")
    void debeRechazarFechasEstimadasIncoherentes() {
        UUID planId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 5, 1);
        LocalDate finInvalido = LocalDate.of(2026, 4, 30);

        assertThrows(DomainValidationException.class, () ->
                ActividadPlanTrabajo.crearActividadCronograma(
                        planId,
                        3,
                        "ACT-03-DOCENTES",
                        "Relevamiento cuerpo docente y CVs",
                        HojaPlanTrabajo.INFO,
                        inicio,
                        finInvalido,
                        "Director de Carrera",
                        PrioridadActividad.ALTA
                )
        );
    }
}
