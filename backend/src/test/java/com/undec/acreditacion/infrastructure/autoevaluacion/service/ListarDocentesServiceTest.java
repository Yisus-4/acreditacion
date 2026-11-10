package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.infrastructure.autoevaluacion.dto.DocenteResumenResponse;
import com.undec.acreditacion.infrastructure.persistence.postgres.RoleJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUnidadAcademicaConeauRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUserRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UnidadAcademicaConeauJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListarDocentesServiceTest {

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataUnidadAcademicaConeauRepository unidadAcademicaRepository;

    @InjectMocks
    private ListarDocentesService service;

    private RoleJpaEntity roleDocente;

    @BeforeEach
    void setUp() {
        roleDocente = new RoleJpaEntity(
                UUID.randomUUID(),
                "DOCENTE",
                "Docente",
                "Docente universitario",
                true,
                true,
                Instant.now()
        );
    }

    @Test
    @DisplayName("deberiaRetornarSoloDocentesActivos")
    void deberiaRetornarSoloDocentesActivos() {
        UserJpaEntity docente = new UserJpaEntity(
                UUID.randomUUID(),
                "carlos.gomez",
                "cgomez@undec.edu.ar",
                "hash",
                true,
                false,
                Instant.now()
        );
        docente.setRoles(Set.of(roleDocente));

        when(userRepository.findDocentesActivos(null)).thenReturn(List.of(docente));

        List<DocenteResumenResponse> response = service.listarDocentes(null);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(docente.getId(), response.get(0).id());
        assertEquals("carlos.gomez", response.get(0).username());
        assertEquals("cgomez@undec.edu.ar", response.get(0).email());
    }

    @Test
    @DisplayName("deberiaFormatearNombreCompletoLegible")
    void deberiaFormatearNombreCompletoLegible() {
        UUID uaId = UUID.randomUUID();
        UserJpaEntity docenteConPunto = new UserJpaEntity(
                UUID.randomUUID(),
                "laura.martino",
                "lmartino@undec.edu.ar",
                "hash",
                true,
                false,
                Instant.now()
        );
        docenteConPunto.setUnidadAcademicaId(uaId);
        docenteConPunto.setRoles(Set.of(roleDocente));

        UnidadAcademicaConeauJpaEntity ua = new UnidadAcademicaConeauJpaEntity();
        ua.setId(uaId);
        ua.setDenominacion("Departamento de Ciencias Básicas");

        when(userRepository.findDocentesActivos("martino")).thenReturn(List.of(docenteConPunto));
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));

        List<DocenteResumenResponse> response = service.listarDocentes("martino");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Laura Martino", response.get(0).nombreCompleto());
        assertEquals("Departamento de Ciencias Básicas", response.get(0).unidadAcademicaNombre());
    }
}
