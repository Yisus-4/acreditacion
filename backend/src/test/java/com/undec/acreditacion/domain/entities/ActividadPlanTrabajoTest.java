package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
        assertEquals("ACT-01-INFORMAR-PROCESO", actividad.getCodigoActividad());
        assertEquals("Informar sobre el proceso de acreditación", actividad.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, actividad.getEstado());
        assertNotNull(actividad.getCreadoEn());
        assertNotNull(actividad.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe completar la actividad exitosamente con fecha, síntesis (>=10 chars) y usuario AEA")
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
}
