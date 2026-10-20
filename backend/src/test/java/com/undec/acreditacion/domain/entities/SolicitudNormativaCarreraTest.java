package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DocumentosFaltantesException;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.NormativaValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudNormativaCarreraTest {

    @Test
    @DisplayName("Iniciar solicitud la coloca en PENDIENTE_SOLICITUD")
    void testIniciarSolicitud() {
        UUID procesoId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, actividadId);

        assertNotNull(sol.getId());
        assertEquals(procesoId, sol.getProcesoId());
        assertEquals(actividadId, sol.getActividadId());
        assertEquals(EstadoSolicitudNormativa.PENDIENTE_SOLICITUD, sol.getEstado());
        assertEquals(5, sol.getDiasHabilesPlazo());
        assertNull(sol.getFechaLimiteEntrega());
    }

    @Test
    @DisplayName("Solicitar pasa a SOLICITADA y calcula fecha límite con 5 días hábiles")
    void testSolicitar() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        UUID usuarioAea = UUID.randomUUID();
        LocalDate jueves15 = LocalDate.of(2026, 10, 15);

        sol.solicitar(usuarioAea, "Adjuntar ordenanza y designaciones.", 5, jueves15);

        assertEquals(EstadoSolicitudNormativa.SOLICITADA, sol.getEstado());
        assertEquals(usuarioAea, sol.getSolicitadoPor());
        assertNotNull(sol.getFechaSolicitud());
        assertEquals(LocalDate.of(2026, 10, 22), sol.getFechaLimiteEntrega());
        assertEquals("Adjuntar ordenanza y designaciones.", sol.getObservacionesSolicitud());
    }

    @Test
    @DisplayName("Iniciar carga borrador transiciona desde SOLICITADA u OBSERVADA")
    void testIniciarCargaBorrador() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));

        sol.iniciarCargaBorrador();
        assertEquals(EstadoSolicitudNormativa.EN_CARGA_BORRADOR, sol.getEstado());
    }

    @Test
    @DisplayName("Enviar definitivo bloquea con DocumentosFaltantesException si la matriz está incompleta")
    void testEnviarDefinitivo_bloqueaSiIncompleta() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));
        sol.iniciarCargaBorrador();

        // Sin documentos cargados
        assertThrows(DocumentosFaltantesException.class, () -> sol.enviarDefinitivo(
            UUID.randomUUID(), LocalDate.of(2026, 10, 18), Collections.emptyList(), true, false
        ));
    }

    @Test
    @DisplayName("Enviar definitivo exitoso congela documentos y evalúa EN_TERMINO")
    void testEnviarDefinitivo_exitosoEnTermino() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));
        sol.iniciarCargaBorrador();

        DocumentoNormativa d1 = DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, UUID.randomUUID());
        DocumentoNormativa d2 = DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, UUID.randomUUID());
        DocumentoNormativa d3 = DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, UUID.randomUUID());
        DocumentoNormativa d4 = DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, UUID.randomUUID());
        DocumentoNormativa d5 = DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, UUID.randomUUID());

        List<DocumentoNormativa> docs = List.of(d1, d2, d3, d4, d5);
        LocalDate fechaEnvio = LocalDate.of(2026, 10, 20); // Antes del 22

        sol.enviarDefinitivo(UUID.randomUUID(), fechaEnvio, docs, true, false);

        assertEquals(EstadoSolicitudNormativa.ENVIADA_A_REVISION, sol.getEstado());
        assertEquals(CumplimientoPlazo.EN_TERMINO, sol.getCumplimientoPlazo());
        assertFalse(d1.isEsBorrador());
        assertFalse(d5.isEsBorrador());
    }

    @Test
    @DisplayName("Enviar definitivo después de la fecha límite evalúa FUERA_DE_TERMINO")
    void testEnviarDefinitivo_fueraDeTermino() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));
        sol.iniciarCargaBorrador();

        List<DocumentoNormativa> docs = List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, UUID.randomUUID())
        );

        LocalDate fechaEnvioTardio = LocalDate.of(2026, 10, 26); // Límite era 22
        sol.enviarDefinitivo(UUID.randomUUID(), fechaEnvioTardio, docs, true, false);

        assertEquals(EstadoSolicitudNormativa.ENVIADA_A_REVISION, sol.getEstado());
        assertEquals(CumplimientoPlazo.FUERA_DE_TERMINO, sol.getCumplimientoPlazo());
    }

    @Test
    @DisplayName("Observar exige fundamentación >= 10 caracteres y transiciona a OBSERVADA")
    void testObservar() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));
        sol.iniciarCargaBorrador();
        List<DocumentoNormativa> docs = List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, UUID.randomUUID())
        );
        sol.enviarDefinitivo(UUID.randomUUID(), LocalDate.of(2026, 10, 20), docs, true, false);

        assertThrows(NormativaValidationException.class, () -> sol.observar(UUID.randomUUID(), "Corta"));

        sol.observar(UUID.randomUUID(), "El archivo de ordenanza no incluye los anexos de correlatividades.");
        assertEquals(EstadoSolicitudNormativa.OBSERVADA, sol.getEstado());
        assertEquals("El archivo de ordenanza no incluye los anexos de correlatividades.", sol.getObservacionesRevision());
    }

    @Test
    @DisplayName("Aprobar pasa a COMPLETADA")
    void testAprobar() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.of(2026, 10, 15));
        sol.iniciarCargaBorrador();
        List<DocumentoNormativa> docs = List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), sol.getProcesoId(), TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, UUID.randomUUID())
        );
        sol.enviarDefinitivo(UUID.randomUUID(), LocalDate.of(2026, 10, 20), docs, true, false);

        sol.aprobar(UUID.randomUUID(), "Documentación validada conforme a normativa CONEAU.");
        assertEquals(EstadoSolicitudNormativa.COMPLETADA, sol.getEstado());
        assertNotNull(sol.getFechaRevision());
    }

    @Test
    @DisplayName("Modificar una solicitud ya completada o en revisión lanza EstadoInvalidoException")
    void testTransicionesInvalidas() {
        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(EstadoInvalidoException.class, sol::iniciarCargaBorrador);
    }
}
