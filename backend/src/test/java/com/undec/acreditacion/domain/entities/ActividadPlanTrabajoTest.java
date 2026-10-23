package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActividadPlanTrabajoTest {

    private static final List<AreaInformadaProceso> AREAS = List.of(
            AreaInformadaProceso.DIRECCION_CARRERA,
            AreaInformadaProceso.SECRETARIAS
    );
    private static final List<DocumentacionRemitidaProceso> DOCUMENTACION = List.of(
            DocumentacionRemitidaProceso.RM_ESTANDARES,
            DocumentacionRemitidaProceso.ORDENANZAS_NOTAS_TECNICAS
    );

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
        assertTrue(actividad.getAreasInformadas().isEmpty());
        assertTrue(actividad.getDocumentacionRemitida().isEmpty());
        assertNotNull(actividad.getCreadoEn());
        assertNotNull(actividad.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe definir y crear la actividad Elaborar Plan de Trabajo")
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
    @DisplayName("Actividad 1: debe completar registrando áreas informadas, documentación remitida y fecha de reunión con la DC, sin exigir síntesis")
    void debeCompletarActividadInformarSinSintesis() {
        UUID usuarioAeaId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());
        Instant fechaReunion = Instant.now().minusSeconds(3600);

        actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, fechaReunion, null, usuarioAeaId);

        assertEquals(EstadoActividad.COMPLETADA, actividad.getEstado());
        assertEquals(fechaReunion, actividad.getFechaReunionCoordinacion());
        assertEquals(AREAS, actividad.getAreasInformadas());
        assertEquals(DOCUMENTACION, actividad.getDocumentacionRemitida());
        assertNull(actividad.getObservaciones());
        assertEquals(usuarioAeaId, actividad.getCompletadaPor());
        assertNotNull(actividad.getCompletadaEn());
    }

    @Test
    @DisplayName("Actividad 1: las observaciones son opcionales y se guardan recortadas; una observación corta es válida")
    void debeGuardarObservacionesOpcionales() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, Instant.now(), "  Ok  ", UUID.randomUUID());

        assertEquals("Ok", actividad.getObservaciones());
        assertEquals("Ok", actividad.getSintesisAcuerdos());
    }

    @Test
    @DisplayName("Actividad 1: observaciones en blanco se normalizan a null")
    void debeNormalizarObservacionesEnBlanco() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, Instant.now(), "   ", UUID.randomUUID());

        assertNull(actividad.getObservaciones());
    }

    @Test
    @DisplayName("Actividad 1: debe exigir al menos un área informada")
    void debeExigirAreasInformadas() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(List.of(), DOCUMENTACION, Instant.now(), null, UUID.randomUUID()));
        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(null, DOCUMENTACION, Instant.now(), null, UUID.randomUUID()));
        assertEquals(EstadoActividad.PENDIENTE, actividad.getEstado());
    }

    @Test
    @DisplayName("Actividad 1: debe exigir al menos un documento remitido")
    void debeExigirDocumentacionRemitida() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(AREAS, List.of(), Instant.now(), null, UUID.randomUUID()));
        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(AREAS, null, Instant.now(), null, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Actividad 1: debe eliminar duplicados preservando el orden")
    void debeEliminarDuplicados() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        actividad.registrarComunicacionYCompletar(
                List.of(AreaInformadaProceso.SECRETARIAS, AreaInformadaProceso.SECRETARIAS, AreaInformadaProceso.DIRECCION_CARRERA),
                List.of(DocumentacionRemitidaProceso.RM_ESTANDARES, DocumentacionRemitidaProceso.RM_ESTANDARES),
                Instant.now(), null, UUID.randomUUID());

        assertEquals(List.of(AreaInformadaProceso.SECRETARIAS, AreaInformadaProceso.DIRECCION_CARRERA), actividad.getAreasInformadas());
        assertEquals(List.of(DocumentacionRemitidaProceso.RM_ESTANDARES), actividad.getDocumentacionRemitida());
    }

    @Test
    @DisplayName("Debe completar actividad sin reunión (como Elaborar Plan) auditando usuario AEA")
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
        actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, Instant.now(), null, UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, Instant.now(), null, UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Debe rechazar si la fecha de reunión con la DC es nula")
    void debeRechazarFechaReunionNula() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, null, null, UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Debe rechazar si el usuario AEA responsable es nulo")
    void debeRechazarUsuarioAeaNulo() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        assertThrows(DomainValidationException.class, () ->
                actividad.registrarComunicacionYCompletar(AREAS, DOCUMENTACION, Instant.now(), null, null)
        );
    }

    @Test
    @DisplayName("Debe restaurar áreas y documentación persistidas al rehidratar")
    void debeRestaurarRegistroComunicacion() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());

        actividad.restaurarRegistroComunicacion(AREAS, DOCUMENTACION);

        assertEquals(AREAS, actividad.getAreasInformadas());
        assertEquals(DOCUMENTACION, actividad.getDocumentacionRemitida());
    }

    @Test
    @DisplayName("Documentación remitida: opciones según el caso del proceso (solo GRADO)")
    void debeResolverOpcionesDeDocumentacionSegunCaso() {
        assertEquals(List.of(
                DocumentacionRemitidaProceso.RM_ESTANDARES,
                DocumentacionRemitidaProceso.RM_INCLUSION_ART_43_LES,
                DocumentacionRemitidaProceso.ORDENANZAS_NOTAS_TECNICAS
        ), DocumentacionRemitidaProceso.opcionesPara(TipoProcesoAcreditacion.NUEVA, TipoPlanTrabajo.PT_001_ACREDITACION));

        assertEquals(List.of(
                DocumentacionRemitidaProceso.RESOLUCION_CONVOCATORIA_CONEAU,
                DocumentacionRemitidaProceso.RM_ESTANDARES,
                DocumentacionRemitidaProceso.RM_INCLUSION_ART_43_LES,
                DocumentacionRemitidaProceso.ORDENANZAS_NOTAS_TECNICAS
        ), DocumentacionRemitidaProceso.opcionesPara(TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, TipoPlanTrabajo.PT_001_ACREDITACION));

        assertEquals(List.of(
                DocumentacionRemitidaProceso.ORDENANZAS_NOTAS_TECNICAS,
                DocumentacionRemitidaProceso.RESOLUCION_ACREDITACION_COMPROMISOS,
                DocumentacionRemitidaProceso.RESOLUCIONES_INFORME_SEGUIMIENTO,
                DocumentacionRemitidaProceso.PLANES_MEJORA_PRESENTADOS
        ), DocumentacionRemitidaProceso.opcionesPara(TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, TipoPlanTrabajo.PT_003_SEGUIMIENTO));
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
