package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExportarHojaInfoExcelServiceTest {

    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private CarreraRepositoryPort carreraRepository;
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private ExcelReportGeneratorPort excelReportGeneratorPort;
    private ExportarHojaInfoExcelService service;

    @BeforeEach
    void setUp() {
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        registroConvocatoriaRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        excelReportGeneratorPort = mock(ExcelReportGeneratorPort.class);

        service = new ExportarHojaInfoExcelService(
            planTrabajoRepository,
            procesoRepository,
            carreraRepository,
            registroConvocatoriaRepository,
            solicitudRepository,
            excelReportGeneratorPort
        );
    }

    @Test
    @DisplayName("Recupera entidades asociadas y delega generación binaria en ExcelReportGeneratorPort")
    void testExportarHojaInfoExcel_exito() {
        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        UUID carreraId = carrera.getId();
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, UUID.randomUUID());
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2));
        when(planTrabajoRepository.findById(planId)).thenReturn(Optional.of(plan));

        RegistroConvocatoria conv = RegistroConvocatoria.crear(procesoId, "RES-1234-2026", LocalDate.now(), LocalDate.now().plusMonths(3), "obs");
        when(registroConvocatoriaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(conv));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        byte[] fakeExcelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP/OOXML magic bytes
        when(excelReportGeneratorPort.generarHojaInfoExcel(eq(plan), eq(proceso), eq(carrera), eq(conv), anyList(), any())).thenReturn(fakeExcelBytes);

        byte[] resultado = service.ejecutar(planId);

        assertNotNull(resultado);
        assertArrayEquals(fakeExcelBytes, resultado);
        verify(excelReportGeneratorPort).generarHojaInfoExcel(eq(plan), eq(proceso), eq(carrera), eq(conv), anyList(), any());
    }
}
