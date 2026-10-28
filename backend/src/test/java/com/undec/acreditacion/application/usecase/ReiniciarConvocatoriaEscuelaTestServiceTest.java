package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReiniciarConvocatoriaEscuelaTestServiceTest {

    private ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private ReiniciarConvocatoriaEscuelaTestService service;

    @BeforeEach
    void setUp() {
        convocatoriaRepository = mock(ConvocatoriaEscuelaRepositoryPort.class);
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        service = new ReiniciarConvocatoriaEscuelaTestService(convocatoriaRepository, procesoRepository);
    }

    @Test
    @DisplayName("Debe fallar si el ID de convocatoria es nulo")
    void debeFallarSiIdEsNulo() {
        assertThrows(DomainValidationException.class, () -> service.reiniciar(null));
    }

    @Test
    @DisplayName("Debe fallar si la convocatoria no existe")
    void debeFallarSiConvocatoriaNoExiste() {
        UUID id = UUID.randomUUID();
        when(convocatoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.reiniciar(id));
    }

    @Test
    @DisplayName("Debe reiniciar convocatoria, desvincular carreras, borrar procesos y guardar convocatoria")
    void debeReiniciarYBorrarProcesosExitosamente() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID proceso1 = UUID.randomUUID();
        UUID proceso2 = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        ConvocatoriaCarrera item2 = ConvocatoriaCarrera.crear(convId, c2, true, false);

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
                List.of(item1, item2)
        );

        UUID usuarioDe = UUID.randomUUID();
        conv.confirmarPorDirectorEscuela(List.of(c1, c2), usuarioDe);
        item1.vincularProceso(proceso1);
        item2.vincularProceso(proceso2);

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Convocatoria resultado = service.reiniciar(convId);

        assertEquals(EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA, resultado.getEstado());
        assertNull(resultado.getConfirmadoPorDe());
        assertNull(resultado.getConfirmadoPorDeEn());
        assertNull(item1.getProcesoId());
        assertNull(item2.getProcesoId());
        assertFalse(item1.isSeleccionadaPorDe());
        assertFalse(item2.isSeleccionadaPorDe());

        verify(convocatoriaRepository, atLeastOnce()).save(conv);
        verify(procesoRepository).deleteById(proceso1);
        verify(procesoRepository).deleteById(proceso2);
    }
}
