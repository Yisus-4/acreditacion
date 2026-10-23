package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.*;
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

class ConfirmarConvocatoriaDirectorEscuelaServiceTest {

    private ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private CarreraRepositoryPort carreraRepository;
    private ConfirmarConvocatoriaDirectorEscuelaService service;

    @BeforeEach
    void setUp() {
        convocatoriaRepository = mock(ConvocatoriaEscuelaRepositoryPort.class);
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        service = new ConfirmarConvocatoriaDirectorEscuelaService(
                convocatoriaRepository, procesoRepository, registroRepository, planTrabajoRepository, carreraRepository);
    }

    @Test
    @DisplayName("Debe confirmar carreras y crear proceso, registro y plan de trabajo")
    void debeConfirmarYCrearProcesos() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID usuarioDe = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        Convocatoria conv = Convocatoria.crear(
                escuelaId,
                "CONV-2026-EI-01",
                "Convocatoria",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                UUID.randomUUID(),
                List.of(item1)
        );

        Carrera carrera = Carrera.registrar(escuelaId, "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Título", 5, "Desc", List.of());

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));
        when(procesoRepository.findActivoByCarreraId(c1)).thenReturn(Optional.empty());
        when(procesoRepository.countByAnio(anyInt())).thenReturn(0L);
        when(procesoRepository.save(any(ProcesoAcreditacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmarConvocatoriaDirectorEscuelaCommand cmd = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                convId,
                List.of(c1),
                usuarioDe
        );

        Convocatoria confirmada = service.confirmar(cmd);

        assertEquals(EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, confirmada.getEstado());
        assertEquals(usuarioDe, confirmada.getConfirmadoPorDe());
        assertNotNull(confirmada.getCarreras().get(0).getProcesoId());

        verify(procesoRepository).save(any(ProcesoAcreditacion.class));
        verify(registroRepository).save(any(RegistroConvocatoria.class));
        verify(planTrabajoRepository).save(any(PlanTrabajo.class));
        verify(convocatoriaRepository).save(any(Convocatoria.class));
    }
}
