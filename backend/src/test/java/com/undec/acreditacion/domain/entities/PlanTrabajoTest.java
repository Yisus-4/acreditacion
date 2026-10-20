package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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

        // Debe contener la actividad 1 y la actividad 1.3
        assertEquals(2, plan.getActividades().size());
        ActividadPlanTrabajo act1 = plan.getActividades().get(0);
        assertEquals(1, act1.getNumeroOrden());
        assertEquals(ActividadPlanTrabajo.CODIGO_ACT_01, act1.getCodigoActividad());
        assertEquals(ActividadPlanTrabajo.NOMBRE_ACT_01, act1.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, act1.getEstado());
        assertNull(act1.getFechaReunionCoordinacion());
        assertNull(act1.getSintesisAcuerdos());
        assertNull(act1.getCompletadaEn());
        assertNull(act1.getCompletadaPor());

        ActividadPlanTrabajo actNormativa = plan.getActividades().get(1);
        assertEquals(3, actNormativa.getNumeroOrden());
        assertEquals(ActividadPlanTrabajo.CODIGO_ACT_03, actNormativa.getCodigoActividad());
        assertEquals(ActividadPlanTrabajo.NOMBRE_ACT_03, actNormativa.getNombre());
        assertEquals(EstadoActividad.PENDIENTE, actNormativa.getEstado());
    }

    @Test
    @DisplayName("Debe permitir anexar actividades futuras respetando número de orden único")
    void debeAnexarActividadFutura() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        plan.anexarActividad(2, "ACT-02-DESIGNAR-COMISION", "Conformación de Comisión de Autoevaluación");

        assertEquals(3, plan.getActividades().size());
        ActividadPlanTrabajo act2 = plan.getActividades().stream()
                .filter(a -> a.getNumeroOrden() == 2)
                .findFirst()
                .orElseThrow();
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

    @Test
    @DisplayName("Debe establecer cronograma tentativo completando Actividad 2 y organizando hojas INFO y CONEAU")
    void debeEstablecerCronogramaTentativoExitosamente() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        UUID usuarioAeaId = UUID.randomUUID();
        LocalDate fechaLimiteConeau = LocalDate.of(2026, 12, 1);

        ActividadPlanTrabajo actInfo = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(),
                3,
                "ACT-03-DOCENTES",
                "Relevamiento cuerpo docente",
                HojaPlanTrabajo.INFO,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                "Director",
                PrioridadActividad.ALTA
        );

        ActividadPlanTrabajo actConeau = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(),
                4,
                "ACT-04-CARGA-CONEAU",
                "Carga de fichas docentes",
                HojaPlanTrabajo.CONEAU,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                "Comisión",
                PrioridadActividad.MEDIA
        );

        plan.establecerCronogramaTentativo(List.of(actInfo, actConeau), fechaLimiteConeau, usuarioAeaId);

        // Debe contener ACT-01 (orden 1), ACT-02 (orden 2 completada), las 2 nuevas, y ACT-1.3 preservada
        assertEquals(5, plan.getActividades().size());

        ActividadPlanTrabajo act2 = plan.getActividades().stream()
                .filter(a -> ActividadPlanTrabajo.CODIGO_ACT_02.equals(a.getCodigoActividad()))
                .findFirst()
                .orElseThrow();
        assertEquals(EstadoActividad.COMPLETADA, act2.getEstado());
        assertEquals(usuarioAeaId, act2.getCompletadaPor());
        assertNotNull(act2.getCompletadaEn());

        assertEquals(4, plan.getActividadesInfo().size()); // ACT-01 + ACT-02 + ACT-03 + ACT-1.3 (todas en hoja INFO)
        assertEquals(1, plan.getActividadesConeau().size()); // ACT-04 (hoja CONEAU)
    }

    @Test
    @DisplayName("Debe preservar el ID y estado de la actividad de normativa si ya existía al armar cronograma")
    void debePreservarIdYEstadoDeActividadNormativaPrevia() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        UUID usuarioAeaId = UUID.randomUUID();

        ActividadPlanTrabajo normativaInicial = plan.getActividades().stream()
                .filter(a -> "1.3".equals(a.getCodigoActividad()))
                .findFirst()
                .orElseThrow();
        UUID idOriginal = normativaInicial.getId();
        normativaInicial.cambiarEstado(EstadoActividad.EN_PROGRESO);

        ActividadPlanTrabajo actNormativaCronograma = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(),
                3,
                "1.3",
                "Gestionar normativa de la carrera que se presenta a acreditación",
                HojaPlanTrabajo.INFO,
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 4, 15),
                "AEA",
                PrioridadActividad.ALTA
        );

        plan.establecerCronogramaTentativo(List.of(actNormativaCronograma), LocalDate.of(2026, 12, 1), usuarioAeaId);

        ActividadPlanTrabajo normativaResultante = plan.getActividades().stream()
                .filter(a -> "1.3".equals(a.getCodigoActividad()))
                .findFirst()
                .orElseThrow();

        assertEquals(idOriginal, normativaResultante.getId());
        assertEquals(EstadoActividad.EN_PROGRESO, normativaResultante.getEstado());
        assertEquals(LocalDate.of(2026, 3, 15), normativaResultante.getFechaInicioEstimada());
        assertEquals(LocalDate.of(2026, 4, 15), normativaResultante.getFechaFinEstimada());
    }

    @Test
    @DisplayName("Debe rechazar cronograma si alguna actividad supera la fecha límite de presentación CONEAU")
    void debeRechazarActividadesQueSuperenFechaLimiteConeau() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        UUID usuarioAeaId = UUID.randomUUID();
        LocalDate fechaLimiteConeau = LocalDate.of(2026, 11, 30);

        ActividadPlanTrabajo actExcedida = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(),
                3,
                "ACT-CONEAU-TARDIA",
                "Presentación formal tardía",
                HojaPlanTrabajo.CONEAU,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 5), // Posterior al límite
                "Coordinador",
                PrioridadActividad.ALTA
        );

        assertThrows(DomainValidationException.class, () ->
                plan.establecerCronogramaTentativo(List.of(actExcedida), fechaLimiteConeau, usuarioAeaId)
        );
    }
}
