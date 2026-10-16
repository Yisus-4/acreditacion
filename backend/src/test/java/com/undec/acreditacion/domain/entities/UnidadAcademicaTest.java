package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnidadAcademicaTest {

    @Test
    @DisplayName("Debe registrar una unidad académica válida")
    void registrarValida() {
        UUID instId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(
                instId,
                "Escuela de Ingeniería",
                "EI",
                "Descripción de la escuela"
        );

        assertNotNull(ua.getId());
        assertEquals(instId, ua.getInstitucionId());
        assertEquals("Escuela de Ingeniería", ua.getNombre());
        assertEquals("EI", ua.getSigla());
        assertEquals("Descripción de la escuela", ua.getDescripcion());
        assertTrue(ua.isActivo());
    }

    @Test
    @DisplayName("Debe fallar si el nombre es blanco")
    void rechazarNombreVacio() {
        assertThrows(DomainValidationException.class, () ->
                UnidadAcademica.registrar(UUID.randomUUID(), "  ", "EI", "Desc")
        );
    }
}
