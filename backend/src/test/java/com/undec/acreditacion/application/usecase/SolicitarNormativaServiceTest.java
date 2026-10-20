package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.application.input.SolicitarNormativaCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitarNormativaServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private CarreraRepositoryPort carreraRepository;
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private EmailNotificationPort emailNotificationPort;
    private SolicitarNormativaService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        registroConvocatoriaRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        emailNotificationPort = mock(EmailNotificationPort.class);

        service = new SolicitarNormativaService(
            procesoRepository,
            planTrabajoRepository,
            solicitudRepository,
            documentoRepository,
            carreraRepository,
            registroConvocatoriaRepository,
            emailNotificationPort
        );
    }

    @Test
    @DisplayName("Solicitar normativa con éxito cambia estado a SOLICITADA, calcula fecha límite y despacha correo")
    void testSolicitarNormativa_exito() {
        UUID carreraId = UUID.randomUUID();
        UUID solicitanteId = UUID.randomUUID();

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "ISI Acreditación", TipoProcesoAcreditacion.NUEVA, solicitanteId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(
            planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION,
            EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2)
        );

        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoRepository.findBySolicitudNormativaId(any())).thenReturn(List.of());

        Carrera carrera = Carrera.registrar(
            UUID.randomUUID(), "Ingeniería en Sistemas de Información", "ISI", "Ingeniería",
            ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero en Sistemas", 5, "Descripción", List.of()
        );
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        SolicitarNormativaCommand cmd = new SolicitarNormativaCommand(procesoId, solicitanteId, "Adjuntar ordenanzas vigentes.");
        NormativaDetalleDto result = service.ejecutar(cmd);

        assertNotNull(result);
        assertEquals(EstadoSolicitudNormativa.SOLICITADA, result.estado());
        assertNotNull(result.fechaLimiteEntrega());
        assertEquals("Adjuntar ordenanzas vigentes.", result.observacionesSolicitud());
        assertEquals(solicitanteId, result.solicitadoPor());

        verify(solicitudRepository).save(any(SolicitudNormativaCarrera.class));
        verify(emailNotificationPort).enviarSolicitudNormativa(
            eq(procesoId),
            anyString(),
            anyString(),
            eq("Ingeniería en Sistemas de Información"),
            anyString(),
            any(LocalDate.class),
            eq("Adjuntar ordenanzas vigentes.")
        );
    }
}
