package com.undec.acreditacion.infrastructure.excel;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.ExcelReportGeneratorPort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class ApachePoiHojaInfoExcelAdapter implements ExcelReportGeneratorPort {

    private static final byte[] COLOR_PRIMARY_RGB = new byte[]{(byte) 130, (byte) 97, (byte) 112}; // #826170
    private static final byte[] COLOR_ZEBRA_RGB = new byte[]{(byte) 249, (byte) 250, (byte) 251}; // #F9FAFB
    private static final byte[] COLOR_BORDER_RGB = new byte[]{(byte) 208, (byte) 208, (byte) 208}; // #D0D0D0

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] generarHojaInfoExcel(
        PlanTrabajo planTrabajo,
        ProcesoAcreditacion proceso,
        Carrera carrera,
        RegistroConvocatoria convocatoria,
        List<ActividadPlanTrabajo> actividades,
        Optional<SolicitudNormativaCarrera> solicitudNormativa
    ) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Hoja INFO - PT-001");
            sheet.setDisplayGridlines(true);

            // Estilos
            CreationHelper createHelper = workbook.getCreationHelper();
            short dateFormat = createHelper.createDataFormat().getFormat("dd/mm/yyyy");

            // Estilo Membrete Título
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(COLOR_PRIMARY_RGB, null));

            XSSFCellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Estilo Subtítulo
            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setBold(true);
            subtitleFont.setFontHeightInPoints((short) 11);

            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(subtitleFont);

            // Estilo Metadatos
            XSSFFont metaLabelFont = workbook.createFont();
            metaLabelFont.setBold(true);
            XSSFCellStyle metaLabelStyle = workbook.createCellStyle();
            metaLabelStyle.setFont(metaLabelFont);

            // Estilo Header de Tabla
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(COLOR_PRIMARY_RGB, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(headerStyle);

            // Estilos para datos
            XSSFCellStyle normalStyle = workbook.createCellStyle();
            aplicarBordes(normalStyle);

            XSSFCellStyle zebraStyle = workbook.createCellStyle();
            zebraStyle.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA_RGB, null));
            zebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            aplicarBordes(zebraStyle);

            XSSFCellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dateFormat);
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(dateStyle);

            XSSFCellStyle dateZebraStyle = workbook.createCellStyle();
            dateZebraStyle.setDataFormat(dateFormat);
            dateZebraStyle.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA_RGB, null));
            dateZebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dateZebraStyle.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(dateZebraStyle);

            // Estilos de Estado
            XSSFFont fontCompletada = workbook.createFont();
            fontCompletada.setBold(true);
            fontCompletada.setColor(IndexedColors.GREEN.getIndex());
            XSSFCellStyle styleCompletada = workbook.createCellStyle();
            styleCompletada.setFont(fontCompletada);
            styleCompletada.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(styleCompletada);

            XSSFFont fontProgreso = workbook.createFont();
            fontProgreso.setBold(true);
            fontProgreso.setColor(IndexedColors.BLUE.getIndex());
            XSSFCellStyle styleProgreso = workbook.createCellStyle();
            styleProgreso.setFont(fontProgreso);
            styleProgreso.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(styleProgreso);

            XSSFFont fontPendiente = workbook.createFont();
            fontPendiente.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            XSSFCellStyle stylePendiente = workbook.createCellStyle();
            stylePendiente.setFont(fontPendiente);
            stylePendiente.setAlignment(HorizontalAlignment.CENTER);
            aplicarBordes(stylePendiente);

            // Membrete
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("UNIVERSIDAD NACIONAL DE CHILECITO");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Secretaría Académica — Área de Evaluación y Acreditación (AEA)");
            c1.setCellStyle(subtitleStyle);

            Row r2 = sheet.createRow(2);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("Plan de Trabajo de Acreditación (PRG-001) — Hoja INFO");

            // Metadatos
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Carrera:");
            r4.getCell(0).setCellStyle(metaLabelStyle);
            r4.createCell(1).setCellValue(carrera != null ? carrera.getNombre() : "-");
            r4.createCell(3).setCellValue("Sigla:");
            r4.getCell(3).setCellStyle(metaLabelStyle);
            r4.createCell(4).setCellValue(carrera != null ? carrera.getSigla() : "-");

            Row r5 = sheet.createRow(5);
            r5.createCell(0).setCellValue("Convocatoria CONEAU:");
            r5.getCell(0).setCellStyle(metaLabelStyle);
            r5.createCell(1).setCellValue(convocatoria != null ? convocatoria.getNroResolucionConeau() : "-");

            Row r6 = sheet.createRow(6);
            r6.createCell(0).setCellValue("Plan de Trabajo:");
            r6.getCell(0).setCellStyle(metaLabelStyle);
            r6.createCell(1).setCellValue(planTrabajo != null ? planTrabajo.getCodigo() : "-");

            Row r7 = sheet.createRow(7);
            r7.createCell(0).setCellValue("Fecha de Emisión:");
            r7.getCell(0).setCellStyle(metaLabelStyle);
            r7.createCell(1).setCellValue(LocalDate.now().format(DATE_FORMATTER));

            // Fila 10 (Índice 9): Encabezados
            String[] headers = {
                "N°", "Código", "Descripción Actividad", "Responsable",
                "Inicio Estimado", "Fin Estimado", "Fecha Real Cierre", "Estado", "Evaluación Plazo"
            };

            Row headerRow = sheet.createRow(9);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowIndex = 10;
            if (actividades != null) {
                for (ActividadPlanTrabajo act : actividades) {
                    Row dataRow = sheet.createRow(rowIndex);
                    boolean isZebra = (rowIndex % 2 == 1);
                    XSSFCellStyle currentNormal = isZebra ? zebraStyle : normalStyle;
                    XSSFCellStyle currentDate = isZebra ? dateZebraStyle : dateStyle;

                    // Col 0: N°
                    Cell cellOrden = dataRow.createCell(0);
                    cellOrden.setCellValue(act.getNumeroOrden());
                    cellOrden.setCellStyle(currentNormal);

                    // Col 1: Código
                    Cell cellCod = dataRow.createCell(1);
                    cellCod.setCellValue(act.getCodigoActividad());
                    cellCod.setCellStyle(currentNormal);

                    // Col 2: Descripción
                    Cell cellNombre = dataRow.createCell(2);
                    cellNombre.setCellValue(act.getNombre());
                    cellNombre.setCellStyle(currentNormal);

                    // Col 3: Responsable
                    Cell cellResp = dataRow.createCell(3);
                    cellResp.setCellValue(act.getResponsable() != null ? act.getResponsable() : "AEA / DC");
                    cellResp.setCellStyle(currentNormal);

                    // Col 4: Inicio Estimado
                    Cell cellInicio = dataRow.createCell(4);
                    if (act.getFechaInicioEstimada() != null) {
                        cellInicio.setCellValue(Date.from(act.getFechaInicioEstimada().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        cellInicio.setCellStyle(currentDate);
                    } else {
                        cellInicio.setCellValue("-");
                        cellInicio.setCellStyle(currentNormal);
                    }

                    // Col 5: Fin Estimado
                    Cell cellFin = dataRow.createCell(5);
                    if (act.getFechaFinEstimada() != null) {
                        cellFin.setCellValue(Date.from(act.getFechaFinEstimada().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        cellFin.setCellStyle(currentDate);
                    } else {
                        cellFin.setCellValue("-");
                        cellFin.setCellStyle(currentNormal);
                    }

                    // Col 6: Fecha Real Cierre
                    Cell cellCierre = dataRow.createCell(6);
                    if (act.getCompletadaEn() != null) {
                        cellCierre.setCellValue(Date.from(act.getCompletadaEn()));
                        cellCierre.setCellStyle(currentDate);
                    } else {
                        cellCierre.setCellValue("-");
                        cellCierre.setCellStyle(currentNormal);
                    }

                    // Col 7: Estado
                    Cell cellEstado = dataRow.createCell(7);
                    cellEstado.setCellValue(act.getEstado().name());
                    if (act.getEstado() == EstadoActividad.COMPLETADA) {
                        cellEstado.setCellStyle(styleCompletada);
                    } else if (act.getEstado() == EstadoActividad.EN_PROGRESO) {
                        cellEstado.setCellStyle(styleProgreso);
                    } else {
                        cellEstado.setCellStyle(stylePendiente);
                    }

                    // Col 8: Evaluación Plazo
                    Cell cellPlazo = dataRow.createCell(8);
                    if (act.getNumeroOrden() == 2 || act.getCodigoActividad().contains("ACT-02")) {
                        if (solicitudNormativa.isPresent()) {
                            SolicitudNormativaCarrera sol = solicitudNormativa.get();
                            if (sol.getCumplimientoPlazo() == CumplimientoPlazo.EN_TERMINO) {
                                cellPlazo.setCellValue("En término");
                            } else if (sol.getCumplimientoPlazo() == CumplimientoPlazo.FUERA_DE_TERMINO) {
                                cellPlazo.setCellValue("Fuera de término");
                            } else {
                                cellPlazo.setCellValue("Pendiente");
                            }
                        } else {
                            cellPlazo.setCellValue("Pendiente");
                        }
                    } else {
                        cellPlazo.setCellValue("-");
                    }
                    cellPlazo.setCellStyle(currentNormal);

                    rowIndex++;
                }
            }

            // Firmas
            rowIndex += 3;
            Row signatureTitleRow = sheet.createRow(rowIndex);
            signatureTitleRow.createCell(0).setCellValue("CERTIFICACIÓN INSTITUCIONAL DE PLAN DE TRABAJO:");
            signatureTitleRow.getCell(0).setCellStyle(metaLabelStyle);

            rowIndex += 3;
            Row signRow = sheet.createRow(rowIndex);
            signRow.createCell(1).setCellValue("__________________________________");
            signRow.createCell(4).setCellValue("__________________________________");
            signRow.createCell(7).setCellValue("__________________________________");

            Row signLabelRow = sheet.createRow(rowIndex + 1);
            signLabelRow.createCell(1).setCellValue("Director/a de Carrera");
            signLabelRow.createCell(4).setCellValue("Responsable AEA");
            signLabelRow.createCell(7).setCellValue("Secretaría Académica");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3500));
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar reporte Excel Hoja INFO: " + e.getMessage(), e);
        }
    }

    private void aplicarBordes(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
