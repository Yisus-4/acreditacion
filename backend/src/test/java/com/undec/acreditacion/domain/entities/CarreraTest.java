package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarreraTest {

    @Test
    @DisplayName("Debe registrar una carrera de grado válida correctamente")
    void registrarCarreraValida() {
        UUID uaId = UUID.randomUUID();
        UUID sedeId = UUID.randomUUID();

        Carrera carrera = Carrera.registrar(
                uaId,
                "Ingeniería en Sistemas de Información",
                "ISI",
                "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero/a en Sistemas",
                5,
                "Carrera de grado orientada al desarrollo de software y sistemas de información.",
                List.of(sedeId)
        );

        assertNotNull(carrera.getId());
        assertEquals("Ingeniería en Sistemas de Información", carrera.getNombre());
        assertEquals("ISI", carrera.getSigla());
        assertEquals(Carrera.NIVEL_GRADO, carrera.getNivel());
        assertEquals("Ciencias Aplicadas", carrera.getAreaDisciplinaria());
        assertEquals(ModalidadCarrera.PRESENCIAL, carrera.getModalidad());
        assertEquals(EstadoCarrera.EN_FUNCIONAMIENTO, carrera.getEstado());
        assertEquals("Ingeniero/a en Sistemas", carrera.getTituloOtorgado());
        assertEquals(5, carrera.getDuracionAnios());
        assertEquals("Carrera de grado orientada al desarrollo de software y sistemas de información.", carrera.getDescripcion());
        assertTrue(carrera.isActivo());
        assertEquals(1, carrera.getSedesIds().size());
        assertEquals(sedeId, carrera.getSedesIds().get(0));
    }

    @Test
    @DisplayName("Debe actualizar carrera y permitir cambiar descripción y estado")
    void actualizarCarreraConDescripcion() {
        UUID uaId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                uaId,
                "Ingeniería en Sistemas",
                "IS",
                "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero",
                5,
                "Descripción original",
                List.of()
        );

        carrera.actualizar(
                "Ingeniería en Sistemas de Información",
                "ISI",
                "Ciencias Aplicadas y Tecnológicas",
                ModalidadCarrera.DISTANCIA,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero/a en Sistemas",
                5,
                "Nueva descripción actualizada",
                List.of()
        );

        assertEquals("Ingeniería en Sistemas de Información", carrera.getNombre());
        assertEquals("Nueva descripción actualizada", carrera.getDescripcion());
        assertEquals(ModalidadCarrera.DISTANCIA, carrera.getModalidad());

        carrera.cambiarEstado(false);
        assertFalse(carrera.isActivo());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nivel no es GRADO")
    void rechazarNivelNoGrado() {
        UUID id = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();

        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                Carrera.rehydrate(
                        id,
                        uaId,
                        "Maestría en Sistemas",
                        "MSI",
                        "POSGRADO",
                        "Ciencias Aplicadas",
                        ModalidadCarrera.PRESENCIAL,
                        EstadoCarrera.EN_FUNCIONAMIENTO,
                        "Magíster",
                        2,
                        "Posgrado",
                        true,
                        null,
                        null,
                        List.of()
                )
        );

        assertTrue(ex.getMessage().contains("estrictamente 'GRADO'"));
    }

    @Test
    @DisplayName("Debe fallar si el nombre de la carrera está vacío")
    void rechazarNombreVacio() {
        assertThrows(DomainValidationException.class, () ->
                Carrera.registrar(
                        UUID.randomUUID(),
                        "",
                        "ISI",
                        "Ciencias Aplicadas",
                        ModalidadCarrera.PRESENCIAL,
                        EstadoCarrera.EN_FUNCIONAMIENTO,
                        "Ingeniero",
                        5,
                        "Descripción",
                        List.of()
                )
        );
    }

    @Test
    @DisplayName("Debe fallar si el área disciplinaria está vacía")
    void rechazarAreaDisciplinariaVacia() {
        assertThrows(DomainValidationException.class, () ->
                Carrera.registrar(
                        UUID.randomUUID(),
                        "Ingeniería",
                        "ING",
                        "",
                        ModalidadCarrera.PRESENCIAL,
                        EstadoCarrera.EN_FUNCIONAMIENTO,
                        "Ingeniero",
                        5,
                        "Descripción",
                        List.of()
                )
        );
    }
}
