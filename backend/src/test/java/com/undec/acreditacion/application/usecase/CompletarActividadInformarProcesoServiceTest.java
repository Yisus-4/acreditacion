package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CompletarActividadCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompletarActividadInformarProcesoServiceTest {

    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private DocumentoAcreditacionRepositoryPort documentoRepository;
    private DocumentStoragePort documentStoragePort;
    private CompletarActividadInformarProcesoService service;

    @BeforeEach
    void setUp() {
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        documentoRepository = mock(DocumentoAcreditacionRepositoryPort.class);
        documentStoragePort = mock(DocumentStoragePort.class);

        service = new CompletarActividadInformarProcesoService(
                planTrabajoRepository,
                documentoRepository,
                documentStoragePort
        );
    }

    @Test
    @DisplayName("Debe completar la actividad 1 resolviendo procesoId (H-01), subiendo PDF a MinIO y manteniendo proceso y plan abiertos")
    void debeCompletarActividadExitosamente() {
        UUID procesoId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actividad = plan.getActividades().get(0);
        UUID actividadId = actividad.getId();
        UUID usuarioAeaId = UUID.randomUUID();

        when(planTrabajoRepository.findActividadById(actividadId)).thenReturn(Optional.of(actividad));
        // H-01: Resuelve procesoId navegando hacia el Plan de Trabajo
        when(planTrabajoRepository.findById(actividad.getPlanTrabajoId())).thenReturn(Optional.of(plan));

        when(documentStoragePort.uploadFile(any(), any(), eq("application/pdf")))
                .thenReturn("convocatorias/2026/acta_test.pdf");
        when(documentoRepository.save(any(DocumentoAcreditacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(planTrabajoRepository.saveActividad(any(ActividadPlanTrabajo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        byte[] fakePdf = "%PDF-1.4 Fake PDF Content".getBytes();
        Instant fechaReunion = Instant.now().minusSeconds(1800);
        String sintesis = "Reunión de coordinación formal realizada con la Dirección de Carrera.";

        CompletarActividadCommand command = new CompletarActividadCommand(
                actividadId,
                fechaReunion,
                sintesis,
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                fakePdf,
                "Acta_Reunion.pdf",
                "application/pdf",
                usuarioAeaId
        );

        ActividadPlanTrabajo resultado = service.completar(command);

        assertNotNull(resultado);
        assertEquals(EstadoActividad.COMPLETADA, resultado.getEstado());
        assertEquals(fechaReunion, resultado.getFechaReunionCoordinacion());
        assertEquals(sintesis, resultado.getSintesisAcuerdos());
        assertEquals(usuarioAeaId, resultado.getCompletadaPor());
        assertNotNull(resultado.getCompletadaEn());

        // Verificar que MinIO subió el archivo
        verify(documentStoragePort).uploadFile(eq(fakePdf), anyString(), eq("application/pdf"));

        // Verificar que DocumentoAcreditacion se guardó con el procesoId resuelto del plan
        ArgumentCaptor<DocumentoAcreditacion> docCaptor = ArgumentCaptor.forClass(DocumentoAcreditacion.class);
        verify(documentoRepository).save(docCaptor.capture());
        DocumentoAcreditacion docGuardado = docCaptor.getValue();
        assertEquals(procesoId, docGuardado.getProcesoId());
        assertEquals(actividadId, docGuardado.getActividadId());
        assertEquals("Acta_Reunion.pdf", docGuardado.getNombreOriginal());
        assertEquals("convocatorias/2026/acta_test.pdf", docGuardado.getStoragePath());
        assertEquals(usuarioAeaId, docGuardado.getSubidoPor());

        // REGLA CRÍTICA: Ciclo de Vida Continuo. El plan permanece ACTIVO
        assertEquals(EstadoPlanTrabajo.ACTIVO, plan.getEstado());
    }

    @Test
    @DisplayName("Debe compensar borrando el archivo en MinIO si la persistencia en base de datos falla")
    void debeCompensarMinioSiFallaBD() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actividad = plan.getActividades().get(0);
        UUID actividadId = actividad.getId();
        UUID usuarioAeaId = UUID.randomUUID();

        when(planTrabajoRepository.findActividadById(actividadId)).thenReturn(Optional.of(actividad));
        when(planTrabajoRepository.findById(actividad.getPlanTrabajoId())).thenReturn(Optional.of(plan));

        String storagePath = "convocatorias/2026/acta_test.pdf";
        when(documentStoragePort.uploadFile(any(), any(), eq("application/pdf"))).thenReturn(storagePath);

        // Simulamos fallo de BD al persistir DocumentoAcreditacion
        when(documentoRepository.save(any(DocumentoAcreditacion.class)))
                .thenThrow(new RuntimeException("Error simulado de PostgreSQL"));

        byte[] fakePdf = "%PDF-1.4 Fake PDF Content".getBytes();
        CompletarActividadCommand command = new CompletarActividadCommand(
                actividadId,
                Instant.now(),
                "Síntesis de acuerdos mayor a 10 caracteres.",
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                fakePdf,
                "Acta.pdf",
                "application/pdf",
                usuarioAeaId
        );

        assertThrows(RuntimeException.class, () -> service.completar(command));

        // Debe haberse ejecutado la compensación en MinIO
        verify(documentStoragePort).deleteFile(storagePath);
    }

    @Test
    @DisplayName("Debe rechazar si la actividad no existe")
    void debeRechazarSiActividadNoExiste() {
        UUID actividadId = UUID.randomUUID();
        when(planTrabajoRepository.findActividadById(actividadId)).thenReturn(Optional.empty());

        byte[] fakePdf = "%PDF-1.4".getBytes();
        CompletarActividadCommand command = new CompletarActividadCommand(
                actividadId,
                Instant.now(),
                "Síntesis de prueba válida.",
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                fakePdf,
                "Acta.pdf",
                "application/pdf",
                UUID.randomUUID()
        );

        assertThrows(ResourceNotFoundException.class, () -> service.completar(command));
    }
}
