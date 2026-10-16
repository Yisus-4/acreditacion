package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SedeLocalizacionTest {

    @Test
    @DisplayName("Debe registrar una sede válida y formatear su dirección")
    void registrarValida() {
        UUID instId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                instId,
                "Campus Los Sarmientos",
                "Ruta Los Peregrinos",
                "s/n",
                "1",
                "5361",
                1L,
                2L,
                3L,
                true
        );

        assertNotNull(sede.getId());
        assertEquals(instId, sede.getInstitucionId());
        assertEquals("Campus Los Sarmientos", sede.getNombre());
        assertEquals("Ruta Los Peregrinos s/n, Piso 1", sede.getDireccionCompleta());
        assertTrue(sede.isEsPropia());
        assertTrue(sede.isActivo());
    }

    @Test
    @DisplayName("Debe fallar si falta la calle o el número")
    void rechazarDatosObligatoriosFaltantes() {
        assertThrows(DomainValidationException.class, () ->
                SedeLocalizacion.registrar(
                        UUID.randomUUID(),
                        "Campus",
                        "",
                        "123",
                        null,
                        "5360",
                        1L,
                        1L,
                        1L,
                        true
                )
        );
    }
}
