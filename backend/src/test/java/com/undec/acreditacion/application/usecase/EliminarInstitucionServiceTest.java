package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.AutoridadInstitucional;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.SedeCentral;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarInstitucionServiceTest {

    private InstitucionRepositoryPort institucionRepository;
    private EliminarInstitucionService service;

    @BeforeEach
    void setUp() {
        institucionRepository = mock(InstitucionRepositoryPort.class);
        service = new EliminarInstitucionService(institucionRepository);
    }

    private Institucion sampleInstitucion(UUID id) {
        AutoridadInstitucional max = AutoridadInstitucional.maximaAutoridad("Pérez", "Juan", null, "123", "rector@undec.edu.ar");
        AutoridadInstitucional adm = AutoridadInstitucional.administradorInstitucional("Gómez", "María", null, "456", "admin@undec.edu.ar", "Cargo", "Ambito");
        SedeCentral sede = SedeCentral.crear("Calle", "123", null, null, "5360", 1L, 1L, 1L);
        return new Institucion(id, "UNdeC", "UNdeC", "logos/undec.png", true, null, null, java.util.List.of(max, adm), sede);
    }

    @Test
    @DisplayName("Eliminar institución existente llama a deleteById")
    void eliminarExistente() {
        UUID id = UUID.randomUUID();
        Institucion institucion = sampleInstitucion(id);

        when(institucionRepository.findById(id)).thenReturn(Optional.of(institucion));

        service.eliminar(id);

        verify(institucionRepository).findById(id);
        verify(institucionRepository).deleteById(id);
    }

    @Test
    @DisplayName("Eliminar institución inexistente lanza ResourceNotFoundException y no llama a deleteById")
    void eliminarInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(institucionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(id));
        verify(institucionRepository).findById(id);
        verify(institucionRepository, never()).deleteById(id);
    }

    @Test
    @DisplayName("Eliminar con id nulo lanza ResourceNotFoundException")
    void eliminarConIdNulo() {
        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(null));
        verify(institucionRepository, never()).deleteById(any());
    }

    private UUID any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
