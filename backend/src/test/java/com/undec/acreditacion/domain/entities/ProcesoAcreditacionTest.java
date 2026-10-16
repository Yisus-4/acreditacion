package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcesoAcreditacionTest {

    @Test
    @DisplayName("Debe crear un proceso de acreditación en estado inicial INICIADO")
    void debeCrearProcesoEnEstadoIniciado() {
        UUID carreraId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();
        String codigo = "CONV-2026-ISI-01";
        String titulo = "Convocatoria 2026 - Ingeniería en Sistemas de Información";

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId,
                codigo,
                titulo,
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                creadoPor
        );

        assertNotNull(proceso.getId());
        assertEquals(carreraId, proceso.getCarreraId());
        assertEquals(codigo, proceso.getCodigo());
        assertEquals(titulo, proceso.getTitulo());
        assertEquals(TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, proceso.getTipoProceso());
        assertEquals(EstadoProcesoAcreditacion.INICIADO, proceso.getEstado());
        assertEquals(creadoPor, proceso.getCreadoPor());
        assertNotNull(proceso.getCreadoEn());
        assertNotNull(proceso.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe rechazar creación si faltan campos obligatorios")
    void debeRechazarCreacionConCamposInvalidos() {
        UUID carreraId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();

        assertThrows(DomainValidationException.class, () ->
                ProcesoAcreditacion.iniciar(null, "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, creadoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                ProcesoAcreditacion.iniciar(carreraId, "   ", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, creadoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, creadoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "Titulo", null, creadoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, null)
        );
    }

    @Test
    @DisplayName("Debe permitir transiciones válidas de estado")
    void debePermitirTransicionesValidas() {
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(),
                "CONV-2026-ISI-01",
                "Titulo",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                UUID.randomUUID()
        );

        Instant fechaOriginal = proceso.getActualizadoEn();

        proceso.pasarAAutoevaluacion();
        assertEquals(EstadoProcesoAcreditacion.EN_AUTOEVALUACION, proceso.getEstado());
        assertTrue(!proceso.getActualizadoEn().isBefore(fechaOriginal));

        proceso.finalizar();
        assertEquals(EstadoProcesoAcreditacion.FINALIZADO, proceso.getEstado());
    }

    @Test
    @DisplayName("Debe rechazar transición inválida de estado")
    void debeRechazarTransicionInvalida() {
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(),
                "CONV-2026-ISI-01",
                "Titulo",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                UUID.randomUUID()
        );

        proceso.cancelar();
        assertEquals(EstadoProcesoAcreditacion.CANCELADO, proceso.getEstado());

        assertThrows(DomainValidationException.class, proceso::pasarAAutoevaluacion);
    }
}
