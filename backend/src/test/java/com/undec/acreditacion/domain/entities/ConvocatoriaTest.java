package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConvocatoriaTest {

    @Test
    @DisplayName("Debe fallar si no se selecciona ninguna carrera")
    void debeFallarSinCarreras() {
        UUID escuelaId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();

        assertThrows(DomainValidationException.class, () ->
                Convocatoria.crear(
                        escuelaId,
                        "CONV-2026-EI-01",
                        "Convocatoria EI",
                        TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                        "RES-123",
                        LocalDate.now(),
                        LocalDate.now().plusDays(10),
                        "Obs",
                        creadoPor,
                        List.of()
                ));
    }

    @Test
    @DisplayName("Debe fallar si ninguna carrera seleccionada es obligatoria")
    void debeFallarSinCarreraObligatoria() {
        UUID escuelaId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConvocatoriaCarrera optativa = ConvocatoriaCarrera.crear(convId, carreraId, true, false);

        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                Convocatoria.crear(
                        escuelaId,
                        "CONV-2026-EI-01",
                        "Convocatoria EI",
                        TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                        "RES-123",
                        LocalDate.now(),
                        LocalDate.now().plusDays(10),
                        "Obs",
                        creadoPor,
                        List.of(optativa)
                ));

        assertTrue(ex.getMessage().contains("obligatoria"));
    }

    @Test
    @DisplayName("Debe crear convocatoria exitosamente en estado PENDIENTE_DIRECTOR_ESCUELA")
    void debeCrearConvocatoriaValida() {
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConvocatoriaCarrera obligatoria = ConvocatoriaCarrera.crear(convId, c1, true, true);
        ConvocatoriaCarrera optativa = ConvocatoriaCarrera.crear(convId, c2, true, false);

        Convocatoria conv = Convocatoria.crear(
                escuelaId,
                "CONV-2026-EI-01",
                "Convocatoria EI",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                UUID.randomUUID(),
                List.of(obligatoria, optativa)
        );

        assertNotNull(conv.getId());
        assertEquals(EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA, conv.getEstado());
        assertEquals(2, conv.getCarreras().size());
    }

    @Test
    @DisplayName("Director de Escuela no puede omitir carreras obligatorias")
    void directorEscuelaNoPuedeOmitirObligatoria() {
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConvocatoriaCarrera obligatoria = ConvocatoriaCarrera.crear(convId, c1, true, true);
        ConvocatoriaCarrera optativa = ConvocatoriaCarrera.crear(convId, c2, true, false);

        Convocatoria conv = Convocatoria.crear(
                escuelaId,
                "CONV-2026-EI-01",
                "Convocatoria EI",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                UUID.randomUUID(),
                List.of(obligatoria, optativa)
        );

        // DE intenta mandar solo c2 (optativa) omitiendo c1 (obligatoria)
        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                conv.confirmarPorDirectorEscuela(List.of(c2), UUID.randomUUID()));

        assertTrue(ex.getMessage().contains("obligatorias"));
    }

    @Test
    @DisplayName("Director de Escuela confirma exitosamente incluyendo la obligatoria")
    void directorEscuelaConfirmaExitosamente() {
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConvocatoriaCarrera obligatoria = ConvocatoriaCarrera.crear(convId, c1, true, true);
        ConvocatoriaCarrera optativa = ConvocatoriaCarrera.crear(convId, c2, true, false);

        Convocatoria conv = Convocatoria.crear(
                escuelaId,
                "CONV-2026-EI-01",
                "Convocatoria EI",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                UUID.randomUUID(),
                List.of(obligatoria, optativa)
        );

        UUID usuarioDe = UUID.randomUUID();
        conv.confirmarPorDirectorEscuela(List.of(c1), usuarioDe);

        assertEquals(EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, conv.getEstado());
        assertEquals(usuarioDe, conv.getConfirmadoPorDe());
        assertTrue(conv.getCarreras().stream().filter(c -> c.getCarreraId().equals(c1)).findFirst().get().isSeleccionadaPorDe());
        assertFalse(conv.getCarreras().stream().filter(c -> c.getCarreraId().equals(c2)).findFirst().get().isSeleccionadaPorDe());
    }
}
