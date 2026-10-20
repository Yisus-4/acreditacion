package com.undec.acreditacion.infrastructure.excel;

import com.undec.acreditacion.domain.entities.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApachePoiHojaInfoExcelAdapterTest {

    @Test
    @DisplayName("Genera libro Excel OOXML válido con hoja oficial, membrete, cabecera granate y actividades")
    void testGenerarHojaInfoExcel_valido() throws Exception {
        ApachePoiHojaInfoExcelAdapter adapter = new ApachePoiHojaInfoExcelAdapter();

        UUID planId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        Carrera carrera = Carrera.registrar(
            carreraId, "Ingeniería en Sistemas de Información", "ISI", "Ingeniería",
            ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of()
        );

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "Acreditación ISI", TipoProcesoAcreditacion.NUEVA, UUID.randomUUID());
        RegistroConvocatoria conv = RegistroConvocatoria.crear(procesoId, "RES-2026-CONEAU", LocalDate.now(), LocalDate.now().plusMonths(4), "Sin observaciones");

        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        act2.completar(UUID.randomUUID());
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        sol.enviarDefinitivo(UUID.randomUUID(), LocalDate.now(), List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, UUID.randomUUID()),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, UUID.randomUUID())
        ), true, false);
        sol.aprobar(UUID.randomUUID(), "Aprobado");

        byte[] excelBytes = adapter.generarHojaInfoExcel(
            plan, proceso, carrera, conv, List.of(act1, act2), Optional.of(sol)
        );

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Hoja INFO - PT-001");
            assertNotNull(sheet, "Debe existir la hoja 'Hoja INFO - PT-001'");

            // Membrete en fila 0
            Row r0 = sheet.getRow(0);
            assertNotNull(r0);
            assertEquals("UNIVERSIDAD NACIONAL DE CHILECITO", r0.getCell(0).getStringCellValue().trim());

            // Fila de encabezados (fila 9)
            Row headerRow = sheet.getRow(9);
            assertNotNull(headerRow, "Debe existir la fila 10 de encabezados (índice 9)");
            assertEquals("N°", headerRow.getCell(0).getStringCellValue());
            assertEquals("Código", headerRow.getCell(1).getStringCellValue());
            assertEquals("Descripción Actividad", headerRow.getCell(2).getStringCellValue());
            assertEquals("Evaluación Plazo", headerRow.getCell(8).getStringCellValue());

            // Fila de Actividad 2 (fila 11, índice 11)
            Row act2Row = sheet.getRow(11);
            assertNotNull(act2Row);
            assertEquals("En término", act2Row.getCell(8).getStringCellValue());
            assertEquals("COMPLETADA", act2Row.getCell(7).getStringCellValue());
        }
    }
}
