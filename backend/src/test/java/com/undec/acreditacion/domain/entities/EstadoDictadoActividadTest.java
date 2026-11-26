package com.undec.acreditacion.domain.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoDictadoActividadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("RN09 / 3.7: Las tres situaciones de asignatura definen clases y exámenes finales correctamente")
    void verificaSemanticaDeTresEstados() {
        assertTrue(EstadoDictadoActividad.ACTIVA.desarrollaClases());
        assertTrue(EstadoDictadoActividad.ACTIVA.evaluaFinales());
        assertFalse(EstadoDictadoActividad.ACTIVA.esInactiva());

        assertFalse(EstadoDictadoActividad.SOLO_EXAMENES_FINALES.desarrollaClases());
        assertTrue(EstadoDictadoActividad.SOLO_EXAMENES_FINALES.evaluaFinales());
        assertFalse(EstadoDictadoActividad.SOLO_EXAMENES_FINALES.esInactiva());

        assertFalse(EstadoDictadoActividad.INACTIVA.desarrollaClases());
        assertFalse(EstadoDictadoActividad.INACTIVA.evaluaFinales());
        assertTrue(EstadoDictadoActividad.INACTIVA.esInactiva());
    }

    @Test
    @DisplayName("fromString y Jackson aceptan estados nuevos y alias legacy SE_DICTA / NO_SE_DICTA")
    void soportaAliasLegacyYDeserializacionJson() throws Exception {
        assertEquals(EstadoDictadoActividad.ACTIVA, EstadoDictadoActividad.fromString("ACTIVA"));
        assertEquals(EstadoDictadoActividad.ACTIVA, EstadoDictadoActividad.fromString("SE_DICTA"));
        assertEquals(EstadoDictadoActividad.ACTIVA, EstadoDictadoActividad.fromString("se_dicta"));
        assertEquals(EstadoDictadoActividad.SOLO_EXAMENES_FINALES, EstadoDictadoActividad.fromString("SOLO_EXAMENES_FINALES"));
        assertEquals(EstadoDictadoActividad.INACTIVA, EstadoDictadoActividad.fromString("INACTIVA"));
        assertEquals(EstadoDictadoActividad.INACTIVA, EstadoDictadoActividad.fromString("NO_SE_DICTA"));

        assertEquals(EstadoDictadoActividad.ACTIVA, objectMapper.readValue("\"SE_DICTA\"", EstadoDictadoActividad.class));
        assertEquals(EstadoDictadoActividad.SOLO_EXAMENES_FINALES, objectMapper.readValue("\"SOLO_EXAMENES_FINALES\"", EstadoDictadoActividad.class));
        assertEquals(EstadoDictadoActividad.INACTIVA, objectMapper.readValue("\"NO_SE_DICTA\"", EstadoDictadoActividad.class));
        assertEquals("\"SOLO_EXAMENES_FINALES\"", objectMapper.writeValueAsString(EstadoDictadoActividad.SOLO_EXAMENES_FINALES));

        assertThrows(IllegalArgumentException.class, () -> EstadoDictadoActividad.fromString("INVALIDO"));
    }
}
