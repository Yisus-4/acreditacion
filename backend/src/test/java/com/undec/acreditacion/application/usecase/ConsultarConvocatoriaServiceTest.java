package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
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

class ConsultarConvocatoriaServiceTest {

    private ProcesoAcreditacionRepositoryPort repository;
    private ConsultarConvocatoriaService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProcesoAcreditacionRepositoryPort.class);
        service = new ConsultarConvocatoriaService(repository);
    }

    @Test
    @DisplayName("Debe retornar proceso cuando existe")
    void debeRetornarProcesoCuandoExiste() {
        UUID id = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(repository.findById(id)).thenReturn(Optional.of(proceso));

        ProcesoAcreditacion res = service.obtenerPorId(id);
        assertNotNull(res);
        assertEquals("CONV-2026-ISI-01", res.getCodigo());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando no existe")
    void debeLanzarExcepcionCuandoNoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(id));
    }
}
