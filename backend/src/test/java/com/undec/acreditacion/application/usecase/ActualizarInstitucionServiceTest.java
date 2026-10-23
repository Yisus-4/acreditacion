package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.RegistrarInstitucionCommand;
import com.undec.acreditacion.domain.entities.AutoridadInstitucional;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.SedeCentral;
import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.InstitucionDuplicadaException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizarInstitucionServiceTest {

    private InstitucionRepositoryPort institucionRepository;
    private DocumentStoragePort documentStoragePort;
    private ActualizarInstitucionService service;

    @BeforeEach
    void setUp() {
        institucionRepository = mock(InstitucionRepositoryPort.class);
        documentStoragePort = mock(DocumentStoragePort.class);
        service = new ActualizarInstitucionService(institucionRepository, documentStoragePort);
    }

    private RegistrarInstitucionCommand createCommand(byte[] logoBytes, String logoFilename, String logoContentType) {
        return new RegistrarInstitucionCommand(
                "Universidad Nacional de La Rioja",
                "UNLaR",
                logoBytes,
                logoFilename,
                logoContentType,
                "Quiroga",
                "Daniel",
                null,
                "+543804123456",
                "rector@unlar.edu.ar",
                "Luna",
                "Carlos",
                null,
                "+543804654321",
                "admin@unlar.edu.ar",
                "Secretario General",
                "Rectorado",
                "Av. Laprida",
                "100",
                null,
                null,
                "5300",
                1L,
                1L,
                1L
        );
    }

    private Institucion sampleInstitucion(UUID id) {
        AutoridadInstitucional max = AutoridadInstitucional.maximaAutoridad("Pérez", "Juan", null, "123", "rector@undec.edu.ar");
        AutoridadInstitucional adm = AutoridadInstitucional.administradorInstitucional("Gómez", "María", null, "456", "admin@undec.edu.ar", "Cargo", "Ambito");
        SedeCentral sede = SedeCentral.crear("Calle", "123", null, null, "5360", 1L, 1L, 1L);
        return new Institucion(id, "UNdeC", "UNdeC", "logos/undec.png", true, null, null, java.util.List.of(max, adm), sede);
    }

    @Test
    @DisplayName("Actualización exitosa conservando logo previo")
    void actualizarExitosoSinNuevoLogo() {
        UUID id = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        RegistrarInstitucionCommand command = createCommand(null, null, null);

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));
        when(institucionRepository.findByNombre("Universidad Nacional de La Rioja")).thenReturn(Optional.empty());
        when(institucionRepository.findBySigla("UNLaR")).thenReturn(Optional.empty());
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(inv -> inv.getArgument(0));

        Institucion actualizada = service.actualizar(id, command);

        assertNotNull(actualizada);
        assertEquals("Universidad Nacional de La Rioja", actualizada.getNombre());
        assertEquals("UNLaR", actualizada.getSigla());
        assertEquals("logos/undec.png", actualizada.getLogoPath()); // conserva logo previo
        verify(documentStoragePort, never()).uploadFile(any(), any(), any());
        verify(institucionRepository).save(any(Institucion.class));
    }

    @Test
    @DisplayName("Actualización exitosa con nuevo logo")
    void actualizarExitosoConNuevoLogo() {
        UUID id = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        RegistrarInstitucionCommand command = createCommand(new byte[]{1, 2, 3}, "nuevo-logo.png", "image/png");

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));
        when(institucionRepository.findByNombre("Universidad Nacional de La Rioja")).thenReturn(Optional.empty());
        when(institucionRepository.findBySigla("UNLaR")).thenReturn(Optional.empty());
        when(documentStoragePort.uploadFile(eq(command.logoBytes()), eq("nuevo-logo.png"), eq("image/png")))
                .thenReturn("logos/nuevo-logo-path.png");
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(inv -> inv.getArgument(0));

        Institucion actualizada = service.actualizar(id, command);

        assertNotNull(actualizada);
        assertEquals("logos/nuevo-logo-path.png", actualizada.getLogoPath());
        verify(documentStoragePort).uploadFile(any(), any(), any());
        verify(institucionRepository).save(any(Institucion.class));
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la institución no existe")
    void actualizarInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        RegistrarInstitucionCommand command = createCommand(null, null, null);

        when(institucionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(id, command));
        verify(institucionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lanza InstitucionDuplicadaException si el nombre ya pertenece a otra institución")
    void actualizarNombreDuplicadoLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        UUID otroId = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        Institucion otra = sampleInstitucion(otroId);
        RegistrarInstitucionCommand command = createCommand(null, null, null);

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));
        when(institucionRepository.findByNombre("Universidad Nacional de La Rioja")).thenReturn(Optional.of(otra));

        assertThrows(InstitucionDuplicadaException.class, () -> service.actualizar(id, command));
        verify(institucionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lanza InstitucionDuplicadaException si la sigla ya pertenece a otra institución")
    void actualizarSiglaDuplicadaLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        UUID otroId = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        Institucion otra = sampleInstitucion(otroId);
        RegistrarInstitucionCommand command = createCommand(null, null, null);

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));
        when(institucionRepository.findByNombre("Universidad Nacional de La Rioja")).thenReturn(Optional.empty());
        when(institucionRepository.findBySigla("UNLaR")).thenReturn(Optional.of(otra));

        assertThrows(InstitucionDuplicadaException.class, () -> service.actualizar(id, command));
        verify(institucionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lanza ArchivoInvalidoException si el nuevo logo tiene extensión/formato inválido")
    void actualizarLogoInvalidoLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        RegistrarInstitucionCommand command = createCommand(new byte[]{1, 2, 3}, "malicioso.exe", "application/x-msdownload");

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));

        assertThrows(ArchivoInvalidoException.class, () -> service.actualizar(id, command));
        verify(documentStoragePort, never()).uploadFile(any(), any(), any());
    }

    @Test
    @DisplayName("Compensación: si falla el save tras subir nuevo logo, elimina el archivo de storage")
    void compensaArchivoSiFallaSave() {
        UUID id = UUID.randomUUID();
        Institucion existente = sampleInstitucion(id);
        RegistrarInstitucionCommand command = createCommand(new byte[]{1, 2, 3}, "logo.png", "image/png");

        when(institucionRepository.findById(id)).thenReturn(Optional.of(existente));
        when(institucionRepository.findByNombre("Universidad Nacional de La Rioja")).thenReturn(Optional.empty());
        when(institucionRepository.findBySigla("UNLaR")).thenReturn(Optional.empty());
        when(documentStoragePort.uploadFile(any(), any(), any())).thenReturn("logos/subido.png");
        when(institucionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> service.actualizar(id, command));
        verify(documentStoragePort).deleteFile("logos/subido.png");
    }
}
