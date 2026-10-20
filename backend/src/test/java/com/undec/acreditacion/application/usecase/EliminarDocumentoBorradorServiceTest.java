package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarDocumentoBorradorCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EliminarDocumentoBorradorServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private DocumentStoragePort documentStoragePort;
    private EliminarDocumentoBorradorService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        documentStoragePort = mock(DocumentStoragePort.class);

        service = new EliminarDocumentoBorradorService(
            procesoRepository,
            planTrabajoRepository,
            solicitudRepository,
            documentoRepository,
            documentStoragePort
        );
    }

    @Test
    @DisplayName("Eliminar borrador en estado EN_CARGA_BORRADOR elimina de storage y repositorio")
    void testEliminarBorrador_exito() {
        UUID procesoId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(UUID.randomUUID(), "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, UUID.randomUUID());
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
            sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "plan.pdf", "storage/path/plan.pdf", "application/pdf", 1024, UUID.randomUUID()
        );
        when(documentoRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        EliminarDocumentoBorradorCommand cmd = new EliminarDocumentoBorradorCommand(procesoId, doc.getId(), UUID.randomUUID());
        service.ejecutar(cmd);

        verify(documentStoragePort).deleteFile("storage/path/plan.pdf");
        verify(documentoRepository).delete(doc);
    }

    @Test
    @DisplayName("Bloquear eliminación si el documento está congelado (esBorrador = false)")
    void testEliminarBorrador_bloqueaSiNoEsBorrador() {
        UUID procesoId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(UUID.randomUUID(), "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, UUID.randomUUID());
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
            sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "plan.pdf", "storage/path/plan.pdf", "application/pdf", 1024, UUID.randomUUID()
        );
        doc.marcarComoDefinitivo();
        when(documentoRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        EliminarDocumentoBorradorCommand cmd = new EliminarDocumentoBorradorCommand(procesoId, doc.getId(), UUID.randomUUID());
        assertThrows(EstadoInvalidoException.class, () -> service.ejecutar(cmd));
    }
}
