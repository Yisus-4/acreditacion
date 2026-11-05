package com.undec.acreditacion.infrastructure.estructura;

import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUnidadAcademicaConeauRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UnidadAcademicaConeauJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnidadAcademicaConeauControllerTest {

    @Mock
    private SpringDataUnidadAcademicaConeauRepository uaRepository;

    @Mock
    private SpringDataInstitucionRepository institucionRepository;

    @InjectMocks
    private UnidadAcademicaConeauController controller;

    private InstitucionJpaEntity institucion;
    private UnidadAcademicaConeauJpaEntity ua;

    @BeforeEach
    void setUp() {
        institucion = new InstitucionJpaEntity();
        institucion.setId(UUID.randomUUID());
        institucion.setNombre("Universidad Nacional de Chilecito");

        ua = new UnidadAcademicaConeauJpaEntity();
        ua.setId(UUID.randomUUID());
        ua.setInstitucion(institucion);
        ua.setDenominacion("Universidad Nacional de Chilecito (Undec)");
        ua.setActiva(true);
        ua.setCreadoEn(Instant.now());
        ua.setActualizadoEn(Instant.now());
    }

    @Test
    @DisplayName("Debe listar unidades académicas")
    void listarUnidadesAcademicas() {
        when(uaRepository.findAll()).thenReturn(List.of(ua));

        ResponseEntity<List<UnidadAcademicaConeauController.UnidadAcademicaConeauResponse>> resp =
                controller.listar(null, true);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals("Universidad Nacional de Chilecito (Undec)", resp.getBody().get(0).denominacion());
    }

    @Test
    @DisplayName("Debe crear una unidad académica válidamente")
    void crearUnidadAcademicaValida() {
        when(institucionRepository.findById(institucion.getId())).thenReturn(Optional.of(institucion));
        when(uaRepository.save(any(UnidadAcademicaConeauJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest req =
                new UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest(
                        institucion.getId(),
                        "Sede Los Sarmientos (Undec)",
                        true
                );

        ResponseEntity<UnidadAcademicaConeauController.UnidadAcademicaConeauResponse> resp = controller.crear(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Sede Los Sarmientos (Undec)", resp.getBody().denominacion());
        assertTrue(resp.getBody().activa());
    }

    @Test
    @DisplayName("Debe rechazar creación con denominación vacía")
    void rechazarCreacionConDenominacionVacia() {
        UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest req =
                new UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest(
                        institucion.getId(),
                        "   ",
                        true
                );

        assertThrows(ResponseStatusException.class, () -> controller.crear(req));
    }

    @Test
    @DisplayName("Debe actualizar denominación y estado activo")
    void actualizarUnidadAcademica() {
        when(uaRepository.findById(ua.getId())).thenReturn(Optional.of(ua));
        when(uaRepository.save(any(UnidadAcademicaConeauJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest req =
                new UnidadAcademicaConeauController.GuardarUnidadAcademicaConeauRequest(
                        institucion.getId(),
                        "UNdeC - Modificada",
                        false
                );

        ResponseEntity<UnidadAcademicaConeauController.UnidadAcademicaConeauResponse> resp =
                controller.actualizar(ua.getId(), req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("UNdeC - Modificada", resp.getBody().denominacion());
        assertFalse(resp.getBody().activa());
    }

    @Test
    @DisplayName("Debe eliminar físicamente si no tiene sedes vinculadas")
    void eliminarFisicamenteSinSedes() {
        ua.setSedes(Collections.emptyList());
        when(uaRepository.findById(ua.getId())).thenReturn(Optional.of(ua));

        ResponseEntity<Void> resp = controller.eliminar(ua.getId());

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(uaRepository).delete(ua);
    }
}
