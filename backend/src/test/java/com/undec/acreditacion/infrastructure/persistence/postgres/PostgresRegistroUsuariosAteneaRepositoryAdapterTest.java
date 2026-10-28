package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostgresRegistroUsuariosAteneaRepositoryAdapterTest {

    @Test
    @DisplayName("Debe mapear entre domain y entity ida y vuelta correctamente")
    void debeMapearDomainYEntity() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea domain = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.of(2026, 10, 26));

        domain.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Valeria", "Castro", "27-32111222-3",
                "vcastro@undec.edu.ar", "AEA", "Coordinadora", TipoAccesoAtenea.ADMINISTRACION,
                "Formulario Integral", false, true, true
        ));

        RegistroUsuariosAteneaJpaEntity entity = RegistroUsuariosAteneaPersistenceMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(procesoId, entity.getProcesoId());
        assertEquals(1, entity.getUsuarios().size());
        assertEquals(146, entity.getMatrizPermisos().size());

        RegistroUsuariosAtenea reconstruido = RegistroUsuariosAteneaPersistenceMapper.toDomain(entity);
        assertNotNull(reconstruido);
        assertEquals(domain.getId(), reconstruido.getId());
        assertEquals(1, reconstruido.getUsuarios().size());
        assertEquals(146, reconstruido.getMatrizPermisos().size());
        assertEquals("Valeria", reconstruido.getUsuarios().get(0).getNombre());
    }

    @Test
    @DisplayName("Debe interactuar correctamente con SpringData repository")
    void debeInteractuarConSpringDataRepository() {
        SpringDataRegistroUsuariosAteneaRepository repo = mock(SpringDataRegistroUsuariosAteneaRepository.class);
        PostgresRegistroUsuariosAteneaRepositoryAdapter adapter = new PostgresRegistroUsuariosAteneaRepositoryAdapter(repo);

        UUID procesoId = UUID.randomUUID();
        RegistroUsuariosAtenea domain = RegistroUsuariosAtenea.iniciar(procesoId, UUID.randomUUID(), LocalDate.now());
        RegistroUsuariosAteneaJpaEntity entity = RegistroUsuariosAteneaPersistenceMapper.toEntity(domain);

        when(repo.save(any(RegistroUsuariosAteneaJpaEntity.class))).thenReturn(entity);
        when(repo.findByProcesoId(procesoId)).thenReturn(Optional.of(entity));

        RegistroUsuariosAtenea guardado = adapter.save(domain);
        assertNotNull(guardado);
        assertEquals(domain.getId(), guardado.getId());

        Optional<RegistroUsuariosAtenea> encontrado = adapter.findByProcesoId(procesoId);
        assertTrue(encontrado.isPresent());
        assertEquals(domain.getId(), encontrado.get().getId());

        adapter.deleteById(domain.getId());
        verify(repo).deleteById(domain.getId());
    }
}
