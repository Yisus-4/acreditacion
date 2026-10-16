package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListarConvocatoriasServiceTest {

    private ProcesoAcreditacionRepositoryPort repository;
    private ListarConvocatoriasService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProcesoAcreditacionRepositoryPort.class);
        service = new ListarConvocatoriasService(repository);
    }

    @Test
    @DisplayName("Debe listar convocatorias aplicando el filtro")
    void debeListarConvocatorias() {
        ConvocatoriaFiltro filtro = new ConvocatoriaFiltro(null, EstadoProcesoAcreditacion.INICIADO, null, 2026);
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(repository.findAllByFiltro(filtro)).thenReturn(List.of(proceso));

        List<ProcesoAcreditacion> lista = service.listar(filtro);
        assertEquals(1, lista.size());
        assertEquals("CONV-2026-ISI-01", lista.get(0).getCodigo());
        verify(repository).findAllByFiltro(filtro);
    }
}
