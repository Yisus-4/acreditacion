package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EliminarConvocatoriaServiceTest {

    private ProcesoAcreditacionRepositoryPort repository;
    private EliminarConvocatoriaService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProcesoAcreditacionRepositoryPort.class);
        service = new EliminarConvocatoriaService(repository);
    }

    @Test
    @DisplayName("Debe eliminar proceso cuando existe")
    void debeEliminarProcesoCuandoExiste() {
        UUID id = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(repository.findById(id)).thenReturn(Optional.of(proceso));

        service.ejecutar(id);

        verify(repository).findById(id);
        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando no existe")
    void debeLanzarExcepcionCuandoNoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.ejecutar(id));
        verify(repository).findById(id);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException cuando el ID es nulo")
    void debeLanzarExcepcionCuandoIdEsNulo() {
        assertThrows(NullPointerException.class, () -> service.ejecutar(null));
        verify(repository, never()).findById(any());
        verify(repository, never()).deleteById(any());
    }
}
