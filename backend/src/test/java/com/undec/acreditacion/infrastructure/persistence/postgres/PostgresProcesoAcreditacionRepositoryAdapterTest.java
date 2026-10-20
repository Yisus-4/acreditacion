package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostgresProcesoAcreditacionRepositoryAdapterTest {

    private SpringDataProcesoAcreditacionRepository springDataRepository;
    private ConvocatoriaPersistenceMapper mapper;
    private PostgresProcesoAcreditacionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataProcesoAcreditacionRepository.class);
        mapper = new ConvocatoriaPersistenceMapper();
        adapter = new PostgresProcesoAcreditacionRepositoryAdapter(springDataRepository, mapper);
    }

    @Test
    @DisplayName("Debe guardar y mapear ProcesoAcreditacion")
    void debeGuardarProceso() {
        ProcesoAcreditacion domain = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );

        when(springDataRepository.save(any(ProcesoAcreditacionJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcesoAcreditacion guardado = adapter.save(domain);

        assertNotNull(guardado);
        assertEquals(domain.getId(), guardado.getId());
        assertEquals("CONV-2026-ISI-01", guardado.getCodigo());
        verify(springDataRepository).save(any(ProcesoAcreditacionJpaEntity.class));
    }

    @Test
    @DisplayName("Debe buscar proceso por ID")
    void debeBuscarPorId() {
        UUID id = UUID.randomUUID();
        ProcesoAcreditacionJpaEntity entity = new ProcesoAcreditacionJpaEntity();
        entity.setId(id);
        entity.setCarreraId(UUID.randomUUID());
        entity.setCodigo("CONV-2026-ISI-01");
        entity.setTitulo("Titulo");
        entity.setTipoProceso("EN_FUNCIONAMIENTO");
        entity.setEstado("INICIADO");
        entity.setCreadoPor(UUID.randomUUID());
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<ProcesoAcreditacion> res = adapter.findById(id);

        assertTrue(res.isPresent());
        assertEquals("CONV-2026-ISI-01", res.get().getCodigo());
    }

    @Test
    @DisplayName("Debe buscar proceso activo por carrera ID")
    void debeBuscarActivoPorCarreraId() {
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacionJpaEntity entity = new ProcesoAcreditacionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCarreraId(carreraId);
        entity.setCodigo("CONV-2026-ISI-01");
        entity.setTitulo("Titulo");
        entity.setTipoProceso("EN_FUNCIONAMIENTO");
        entity.setEstado("INICIADO");
        entity.setCreadoPor(UUID.randomUUID());
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        when(springDataRepository.findByCarreraIdAndEstadoIn(eq(carreraId), anyList()))
                .thenReturn(Optional.of(entity));

        Optional<ProcesoAcreditacion> res = adapter.findActivoByCarreraId(carreraId);

        assertTrue(res.isPresent());
        assertEquals(carreraId, res.get().getCarreraId());
    }

    @Test
    @DisplayName("Debe eliminar proceso por ID")
    void debeEliminarProcesoPorId() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);
        verify(springDataRepository).deleteById(id);
    }
}
