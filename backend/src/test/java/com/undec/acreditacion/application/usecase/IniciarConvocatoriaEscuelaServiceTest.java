package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.IniciarConvocatoriaEscuelaCommand;
import com.undec.acreditacion.application.input.ItemCarreraSeleccionCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;
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

class IniciarConvocatoriaEscuelaServiceTest {

    private ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private CarreraRepositoryPort carreraRepository;
    private IniciarConvocatoriaEscuelaService service;

    @BeforeEach
    void setUp() {
        convocatoriaRepository = mock(ConvocatoriaEscuelaRepositoryPort.class);
        unidadAcademicaRepository = mock(UnidadAcademicaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        service = new IniciarConvocatoriaEscuelaService(convocatoriaRepository, unidadAcademicaRepository, carreraRepository);
    }

    @Test
    @DisplayName("Debe rechazar si no se marca ninguna carrera como obligatoria")
    void debeRechazarSinObligatoria() {
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();

        UnidadAcademica escuela = UnidadAcademica.registrar(UUID.randomUUID(), "Escuela de Ingeniería", "EI", "Desc");
        when(unidadAcademicaRepository.findById(escuelaId)).thenReturn(Optional.of(escuela));

        IniciarConvocatoriaEscuelaCommand cmd = new IniciarConvocatoriaEscuelaCommand(
                escuelaId,
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                List.of(new ItemCarreraSeleccionCommand(c1, true, false)),
                UUID.randomUUID()
        );

        assertThrows(DomainValidationException.class, () -> service.iniciar(cmd));
    }

    @Test
    @DisplayName("Debe iniciar convocatoria exitosamente en estado PENDIENTE_DIRECTOR_ESCUELA")
    void debeIniciarConvocatoriaExitosamente() {
        UUID institucionId = UUID.randomUUID();
        UnidadAcademica escuela = UnidadAcademica.registrar(institucionId, "Escuela de Ingeniería", "EI", "Desc");
        UUID escuelaId = escuela.getId();
        UUID c1 = UUID.randomUUID();

        Carrera carrera = Carrera.registrar(escuelaId, "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Título", 5, "Desc", List.of());

        when(unidadAcademicaRepository.findById(escuelaId)).thenReturn(Optional.of(escuela));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));
        when(convocatoriaRepository.countByAnio(anyInt())).thenReturn(0L);
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IniciarConvocatoriaEscuelaCommand cmd = new IniciarConvocatoriaEscuelaCommand(
                escuelaId,
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                List.of(new ItemCarreraSeleccionCommand(c1, true, true)),
                UUID.randomUUID()
        );

        Convocatoria resultado = service.iniciar(cmd);

        assertNotNull(resultado);
        assertEquals(EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA, resultado.getEstado());
        assertEquals(1, resultado.getCarreras().size());
        assertTrue(resultado.getCarreras().get(0).isEsObligatoria());
        verify(convocatoriaRepository).save(any(Convocatoria.class));
    }
}
