package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.NormativaDetalleDto;
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
import static org.mockito.Mockito.*;

class ObtenerNormativaDetalleServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private ObtenerNormativaDetalleService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);

        service = new ObtenerNormativaDetalleService(
            procesoRepository,
            planTrabajoRepository,
            solicitudRepository,
            documentoRepository
        );
    }

    @Test
    @DisplayName("Si no existe solicitud previa, retorna estado PENDIENTE_SOLICITUD")
    void testObtenerDetalle_sinSolicitudPrevia() {
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "ISI Acreditación", TipoProcesoAcreditacion.NUEVA, UUID.randomUUID());
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2));

        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.empty());

        NormativaDetalleDto dto = service.ejecutar(procesoId);

        assertNotNull(dto);
        assertEquals(EstadoSolicitudNormativa.PENDIENTE_SOLICITUD, dto.estado());
        assertTrue(dto.documentos().isEmpty());
        assertTrue(dto.carreraEsNueva());
    }

    @Test
    @DisplayName("Si existe solicitud, retorna detalle con documentos y badge calculado")
    void testObtenerDetalle_conSolicitud() {
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "ISI Acreditación", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID());
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2));

        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "plan.pdf", "path", "application/pdf", 1024, UUID.randomUUID());
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(List.of(doc));

        NormativaDetalleDto dto = service.ejecutar(procesoId);

        assertNotNull(dto);
        assertEquals(EstadoSolicitudNormativa.SOLICITADA, dto.estado());
        assertEquals(1, dto.documentos().size());
        assertFalse(dto.carreraEsNueva());
        assertTrue(dto.carreraTieneHistorial());
        assertNotNull(dto.badgePlazo());
    }
}
