package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CompletarActividadCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompletarActividadInformarProcesoServiceTest {

    private static final List<AreaInformadaProceso> AREAS = List.of(
            AreaInformadaProceso.DIRECCION_CARRERA,
            AreaInformadaProceso.DIRECCION_ESCUELA
    );
    private static final List<DocumentacionRemitidaProceso> DOCUMENTACION = List.of(
            DocumentacionRemitidaProceso.RESOLUCION_CONVOCATORIA_CONEAU,
            DocumentacionRemitidaProceso.RM_ESTANDARES
    );

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

    private CompletarActividadCommand comandoConAdjunto(UUID actividadId, Instant fecha, UUID usuario, byte[] pdf) {
        return new CompletarActividadCommand(
                actividadId,
                AREAS,
                DOCUMENTACION,
                fecha,
                null,
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                pdf,
                "Comunicacion.pdf",
                "application/pdf",
                usuario
        );
    }

    @Test
    @DisplayName("Debe completar la actividad 1 sin adjunto: no sube archivos ni crea documentos (adjunto opcional)")
    void debeCompletarSinAdjunto() {
        PlanTrabajo plan = PlanTrabajo.inicializar(UUID.randomUUID(), "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actividad = plan.getActividades().get(0);
        UUID usuarioAeaId = UUID.randomUUID();

        when(planTrabajoRepository.findActividadById(actividad.getId())).thenReturn(Optional.of(actividad));
        when(planTrabajoRepository.findById(actividad.getPlanTrabajoId())).thenReturn(Optional.of(plan));
        when(planTrabajoRepository.saveActividad(any(ActividadPlanTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant fechaReunion = Instant.now().minusSeconds(1800);
        CompletarActividadCommand command = new CompletarActividadCommand(
                actividad.getId(), AREAS, DOCUMENTACION, fechaReunion, null,
                null, null, null, null, usuarioAeaId
        );

        ActividadPlanTrabajo resultado = service.completar(command);

        assertEquals(EstadoActividad.COMPLETADA, resultado.getEstado());
        assertEquals(AREAS, resultado.getAreasInformadas());
        assertEquals(DOCUMENTACION, resultado.getDocumentacionRemitida());
        assertEquals(fechaReunion, resultado.getFechaReunionCoordinacion());
        assertNull(resultado.getObservaciones());
        verifyNoInteractions(documentStoragePort);
        verifyNoInteractions(documentoRepository);
        assertEquals(EstadoPlanTrabajo.ACTIVO, plan.getEstado());
    }

    @Test
    @DisplayName("Debe completar la actividad 1 con adjunto PDF opcional: resuelve procesoId (H-01) y lo guarda en MinIO")
    void debeCompletarActividadConAdjunto() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actividad = plan.getActividades().get(0);
        UUID actividadId = actividad.getId();
        UUID usuarioAeaId = UUID.randomUUID();

        when(planTrabajoRepository.findActividadById(actividadId)).thenReturn(Optional.of(actividad));
        when(planTrabajoRepository.findById(actividad.getPlanTrabajoId())).thenReturn(Optional.of(plan));
        when(documentStoragePort.uploadFile(any(), any(), eq("application/pdf")))
                .thenReturn("convocatorias/2026/acta_test.pdf");
        when(documentoRepository.save(any(DocumentoAcreditacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planTrabajoRepository.saveActividad(any(ActividadPlanTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] fakePdf = "%PDF-1.4 Fake PDF Content".getBytes();
        Instant fechaReunion = Instant.now().minusSeconds(1800);

        ActividadPlanTrabajo resultado = service.completar(comandoConAdjunto(actividadId, fechaReunion, usuarioAeaId, fakePdf));

        assertEquals(EstadoActividad.COMPLETADA, resultado.getEstado());
        assertEquals(usuarioAeaId, resultado.getCompletadaPor());
        verify(documentStoragePort).uploadFile(eq(fakePdf), anyString(), eq("application/pdf"));

        ArgumentCaptor<DocumentoAcreditacion> docCaptor = ArgumentCaptor.forClass(DocumentoAcreditacion.class);
        verify(documentoRepository).save(docCaptor.capture());
        DocumentoAcreditacion docGuardado = docCaptor.getValue();
        assertEquals(procesoId, docGuardado.getProcesoId());
        assertEquals(actividadId, docGuardado.getActividadId());
        assertEquals("Comunicacion.pdf", docGuardado.getNombreOriginal());
        assertEquals("convocatorias/2026/acta_test.pdf", docGuardado.getStoragePath());
        assertEquals(EstadoPlanTrabajo.ACTIVO, plan.getEstado());
    }

    @Test
    @DisplayName("Debe compensar borrando el archivo en MinIO si la persistencia en base de datos falla")
    void debeCompensarMinioSiFallaBD() {
        PlanTrabajo plan = PlanTrabajo.inicializar(UUID.randomUUID(), "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actividad = plan.getActividades().get(0);

        when(planTrabajoRepository.findActividadById(actividad.getId())).thenReturn(Optional.of(actividad));
        when(planTrabajoRepository.findById(actividad.getPlanTrabajoId())).thenReturn(Optional.of(plan));
        String storagePath = "convocatorias/2026/acta_test.pdf";
        when(documentStoragePort.uploadFile(any(), any(), eq("application/pdf"))).thenReturn(storagePath);
        when(documentoRepository.save(any(DocumentoAcreditacion.class)))
                .thenThrow(new RuntimeException("Error simulado de PostgreSQL"));

        CompletarActividadCommand command = comandoConAdjunto(actividad.getId(), Instant.now(), UUID.randomUUID(),
                "%PDF-1.4 Fake PDF Content".getBytes());

        assertThrows(RuntimeException.class, () -> service.completar(command));
        verify(documentStoragePort).deleteFile(storagePath);
    }

    @Test
    @DisplayName("Debe rechazar si la actividad no existe")
    void debeRechazarSiActividadNoExiste() {
        UUID actividadId = UUID.randomUUID();
        when(planTrabajoRepository.findActividadById(actividadId)).thenReturn(Optional.empty());

        CompletarActividadCommand command = comandoConAdjunto(actividadId, Instant.now(), UUID.randomUUID(),
                "%PDF-1.4".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> service.completar(command));
    }

    @Test
    @DisplayName("El comando debe exigir áreas informadas y documentación remitida, y rechazar adjuntos no PDF")
    void comandoDebeValidarCamposDelProcedimiento() {
        UUID id = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        Instant ahora = Instant.now();

        assertThrows(DomainValidationException.class, () -> new CompletarActividadCommand(
                id, List.of(), DOCUMENTACION, ahora, null, null, null, null, null, usuario));
        assertThrows(DomainValidationException.class, () -> new CompletarActividadCommand(
                id, AREAS, List.of(), ahora, null, null, null, null, null, usuario));
        assertThrows(DomainValidationException.class, () -> new CompletarActividadCommand(
                id, AREAS, DOCUMENTACION, null, null, null, null, null, null, usuario));
        assertThrows(DomainValidationException.class, () -> new CompletarActividadCommand(
                id, AREAS, DOCUMENTACION, ahora, null, TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                "abc".getBytes(), "doc.docx", "application/msword", usuario));
    }
}
