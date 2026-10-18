package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistroConvocatoriaTest {

    @Test
    @DisplayName("Debe crear un RegistroConvocatoria RG-001 con fechas y resolución válidas")
    void debeCrearRegistroConvocatoriaValido() {
        UUID procesoId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        LocalDate cierre = LocalDate.of(2026, 11, 30);
        String resolucion = "RESFC-2026-145-APN-CONEAU#ME";
        String obs = "Convocatoria formal ciclo 2026.";

        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoId,
                resolucion,
                inicio,
                cierre,
                obs
        );

        assertNotNull(registro.getId());
        assertEquals(procesoId, registro.getProcesoId());
        assertEquals(resolucion, registro.getNroResolucionConeau());
        assertEquals(inicio, registro.getFechaInicio());
        assertEquals(cierre, registro.getFechaCierre());
        assertEquals(obs, registro.getObservaciones());
        // H-05: Auditoría Flyway V5
        assertNotNull(registro.getCreadoEn());
        assertNotNull(registro.getActualizadoEn());
    }

    @Test
    @DisplayName("Debe rechazar resolución CONEAU nula o en blanco")
    void debeRechazarResolucionConeauInvalida() {
        UUID procesoId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        LocalDate cierre = LocalDate.of(2026, 11, 30);

        assertThrows(DomainValidationException.class, () ->
                RegistroConvocatoria.crear(procesoId, null, inicio, cierre, null)
        );

        assertThrows(DomainValidationException.class, () ->
                RegistroConvocatoria.crear(procesoId, "   ", inicio, cierre, null)
        );
    }

    @Test
    @DisplayName("Debe rechazar fecha de cierre anterior a fecha de inicio")
    void debeRechazarFechaCierreAnteriorAInicio() {
        UUID procesoId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        LocalDate cierreInvalido = LocalDate.of(2026, 2, 28);

        assertThrows(DomainValidationException.class, () ->
                RegistroConvocatoria.crear(procesoId, "RESFC-2026-145", inicio, cierreInvalido, null)
        );
    }

    @Test
    @DisplayName("Debe aceptar fecha de cierre igual a fecha de inicio")
    void debeAceptarFechaCierreIgualAInicio() {
        UUID procesoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 1);

        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoId,
                "RESFC-2026-145",
                fecha,
                fecha,
                null
        );

        assertNotNull(registro);
        assertEquals(fecha, registro.getFechaInicio());
        assertEquals(fecha, registro.getFechaCierre());
    }

    @Test
    @DisplayName("Debe asignar fecha actual si la fecha de inicio es nula")
    void debeAsignarFechaActualSiFechaInicioEsNula() {
        UUID procesoId = UUID.randomUUID();
        LocalDate cierre = LocalDate.now().plusMonths(6);

        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoId,
                "RESFC-2026-145",
                null,
                cierre,
                null
        );

        assertNotNull(registro);
        assertEquals(LocalDate.now(), registro.getFechaInicio());
        assertEquals(cierre, registro.getFechaCierre());
    }
}
