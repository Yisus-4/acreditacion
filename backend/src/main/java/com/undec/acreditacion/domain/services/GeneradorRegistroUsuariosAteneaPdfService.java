package com.undec.acreditacion.domain.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.TipoAccesoAtenea;
import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class GeneradorRegistroUsuariosAteneaPdfService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_ARGENTINA);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONA_ARGENTINA);

    // Paleta institucional UNdeC
    private static final Color COLOR_PRIMARIO = new Color(107, 76, 91);      // #6b4c5b
    private static final Color COLOR_PRIMARIO_OSCURO = new Color(61, 44, 53); // #3d2c35
    private static final Color COLOR_FONDO_SUAVE = new Color(247, 243, 245); // #f7f3f5
    private static final Color COLOR_FONDO_ALT = new Color(252, 250, 252);   // #fcfafc
    private static final Color COLOR_BORDE = new Color(217, 197, 206);       // #d9c5ce
    private static final Color COLOR_TEXTO = new Color(31, 26, 29);          // #1f1a1d
    private static final Color COLOR_MUTED = new Color(90, 74, 83);          // #5a4a53
    private static final Color COLOR_SI = new Color(45, 90, 61);             // #2d5a3d
    private static final Color COLOR_NO = new Color(138, 122, 130);          // #8a7a82

    private static final Font FONT_INST_BIG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PRIMARIO_OSCURO);
    private static final Font FONT_INST_SUB = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);
    private static final Font FONT_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARIO_OSCURO);
    private static final Font FONT_DOC_SUB = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_MUTED);
    private static final Font FONT_CODE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARIO);
    private static final Font FONT_SEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_GRP_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_PRIMARIO_OSCURO);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_MUTED);
    private static final Font FONT_VAL = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXTO);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 7.3f, COLOR_TEXTO);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.3f, COLOR_TEXTO);
    private static final Font FONT_TD_SI = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.3f, COLOR_SI);
    private static final Font FONT_TD_NO = FontFactory.getFont(FontFactory.HELVETICA, 7.3f, COLOR_NO);
    private static final Font FONT_EMPTY = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, COLOR_MUTED);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_MUTED);

    private GeneradorRegistroUsuariosAteneaPdfService() {}

    public static byte[] generarPdf(RegistroUsuariosAtenea registro, String nombreCarrera, String cicloFase, String anioConvocatoria) {
        String tipoConvocatoria = "Grado - en funcionamiento - " + (cicloFase != null ? cicloFase : "1era Fase");
        return generarPdf(registro, nombreCarrera, tipoConvocatoria, anioConvocatoria, "S/N");
    }

    public static byte[] generarPdf(
            RegistroUsuariosAtenea registro,
            String nombreCarrera,
            String tipoConvocatoria,
            String anioAcademico,
            String nroExpedienteTad
    ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new FooterPageEvent());
            document.open();

            String carreraSafe = safe(nombreCarrera, "Carrera");
            String tipoSafe = safe(tipoConvocatoria, "Grado - en funcionamiento - 1era Fase");
            String anioSafe = safe(anioAcademico, "-");
            String tadSafe = safe(nroExpedienteTad, "S/N");
            Instant fechaRef = registro.getAprobadoEn() != null ? registro.getAprobadoEn() : Instant.now();

            // 1. Membrete institucional UNdeC / AEA
            document.add(crearMembreteOficial(fechaRef));
            document.add(espacio(8));

            // 2. Metadatos del registro
            document.add(crearTablaMetadatos(carreraSafe, anioSafe, tipoSafe, tadSafe, registro, fechaRef));
            document.add(espacio(10));

            // 3. Sección I: Tabla de Usuarios Habilitados en CONEAU Global ATENEA
            document.add(crearBannerSeccion("SECCIÓN I — TABLA DE USUARIOS HABILITADOS EN CONEAU GLOBAL ATENEA"));
            document.add(espacio(4));
            agregarTablaUsuariosPorGrupo(document, registro.getUsuarios());
            document.add(espacio(10));

            // 4. Sección II: Matriz de Permisos del Sistema CONEAU Global ATENEA
            document.newPage();
            document.add(crearMembreteOficial(fechaRef));
            document.add(espacio(8));
            document.add(crearBannerSeccion("SECCIÓN II — MATRIZ DE PERMISOS DEL SISTEMA CONEAU GLOBAL ATENEA"));
            document.add(espacio(4));
            agregarTablaMatrizPermisos(document, registro.getMatrizPermisos());

            document.close();
            return baos.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Error generando el documento PDF oficial RG-003", ex);
        }
    }

    private static PdfPTable crearMembreteOficial(Instant fechaEmision) {
        PdfPTable header = new PdfPTable(new float[]{28f, 50f, 22f});
        header.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorderColor(COLOR_BORDE);
        left.setBackgroundColor(COLOR_FONDO_SUAVE);
        left.setPadding(8);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pLeft = new Paragraph();
        pLeft.add(new Chunk("UNIVERSIDAD NACIONAL DE CHILECITO\n", FONT_INST_BIG));
        pLeft.add(new Chunk("Área de Evaluación y Acreditación (AEA)", FONT_INST_SUB));
        left.addElement(pLeft);
        header.addCell(left);

        PdfPCell center = new PdfPCell();
        center.setBorderColor(COLOR_BORDE);
        center.setBackgroundColor(Color.WHITE);
        center.setPadding(8);
        center.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pTitle = new Paragraph("RG-003: Registro de permisos de usuarios habilitados", FONT_DOC_TITLE);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        Paragraph pSub = new Paragraph("Plataforma CONEAU Global — Sistema ATENEA", FONT_DOC_SUB);
        pSub.setAlignment(Element.ALIGN_CENTER);
        center.addElement(pTitle);
        center.addElement(pSub);
        header.addCell(center);

        PdfPCell right = new PdfPCell();
        right.setBorderColor(COLOR_BORDE);
        right.setBackgroundColor(COLOR_FONDO_SUAVE);
        right.setPadding(8);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pRight = new Paragraph();
        pRight.add(new Chunk("Código: RG-003\n", FONT_CODE));
        pRight.add(new Chunk("Versión N° 01\n", FONT_INST_SUB));
        pRight.add(new Chunk("Fecha: " + DATE_ONLY.format(fechaEmision), FONT_INST_SUB));
        right.addElement(pRight);
        header.addCell(right);

        return header;
    }

    private static PdfPTable crearTablaMetadatos(
            String nombreCarrera,
            String anioAcademico,
            String tipoConvocatoria,
            String nroExpedienteTad,
            RegistroUsuariosAtenea registro,
            Instant fechaRef
    ) {
        PdfPTable table = new PdfPTable(new float[]{16f, 34f, 18f, 32f});
        table.setWidthPercentage(100);

        addMetaRow(table, "Carrera:", nombreCarrera, "Año académico:", anioAcademico);
        addMetaRow(table, "Tipo de convocatoria:", tipoConvocatoria, "Nro. de expediente TAD:", nroExpedienteTad);
        String estadoTexto = registro.getEstado() != null ? registro.getEstado().name() : "EN_DEFINICION";
        String fechaTexto = FORMATTER.format(fechaRef);
        addMetaRow(table, "Estado del registro:", estadoTexto, "Fecha de emisión:", fechaTexto);

        return table;
    }

    private static void addMetaRow(PdfPTable table, String l1, String v1, String l2, String v2) {
        table.addCell(metaLabelCell(l1));
        table.addCell(metaValueCell(v1));
        table.addCell(metaLabelCell(l2));
        table.addCell(metaValueCell(v2));
    }

    private static PdfPCell metaLabelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_LABEL));
        c.setBackgroundColor(COLOR_FONDO_SUAVE);
        c.setBorderColor(COLOR_BORDE);
        c.setPadding(5);
        return c;
    }

    private static PdfPCell metaValueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_VAL));
        c.setBackgroundColor(Color.WHITE);
        c.setBorderColor(COLOR_BORDE);
        c.setPadding(5);
        return c;
    }

    private static PdfPTable crearBannerSeccion(String titulo) {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(titulo, FONT_SEC_TITLE));
        cell.setBackgroundColor(COLOR_PRIMARIO);
        cell.setBorderColor(COLOR_PRIMARIO);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(8);
        banner.addCell(cell);
        return banner;
    }

    private static void agregarTablaUsuariosPorGrupo(Document document, List<UsuarioPermisoAtenea> todosUsuarios) {
        PdfPTable table = new PdfPTable(new float[]{3.5f, 16f, 10.5f, 15.5f, 15.5f, 11f, 12f, 5.3f, 5.3f, 5.4f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        String[] headers = {
                "N°", "Apellido y Nombre", "DNI / CUIL", "Correo electrónico",
                "Dependencia / Cargo", "Tipo de Acceso", "Sección / Alcance",
                "Req. Cta.", "Creada", "Habilit."
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell th = new PdfPCell(new Phrase(headers[i], FONT_TH));
            th.setBackgroundColor(COLOR_PRIMARIO_OSCURO);
            th.setBorderColor(COLOR_BORDE);
            th.setPadding(5);
            th.setHorizontalAlignment(i == 0 || i >= 7 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
            th.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(th);
        }

        List<UsuarioPermisoAtenea> lista = todosUsuarios != null ? todosUsuarios : List.of();
        for (GrupoUsuarioAtenea grupo : GrupoUsuarioAtenea.values()) {
            List<UsuarioPermisoAtenea> delGrupo = lista.stream()
                    .filter(u -> u.getGrupoUsuario() == grupo)
                    .toList();

            PdfPCell grpCell = new PdfPCell(new Phrase(grupo.name() + " — " + grupo.getDescripcion(), FONT_GRP_TITLE));
            grpCell.setColspan(headers.length);
            grpCell.setBackgroundColor(COLOR_FONDO_SUAVE);
            grpCell.setBorderColor(COLOR_BORDE);
            grpCell.setPaddingTop(5);
            grpCell.setPaddingBottom(5);
            grpCell.setPaddingLeft(6);
            table.addCell(grpCell);

            if (delGrupo.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Sin usuarios registrados en este grupo.", FONT_EMPTY));
                empty.setColspan(headers.length);
                empty.setBorderColor(COLOR_BORDE);
                empty.setPadding(5);
                table.addCell(empty);
                continue;
            }

            int idx = 1;
            for (UsuarioPermisoAtenea u : delGrupo) {
                Color rowBg = (idx % 2 == 0) ? COLOR_FONDO_ALT : Color.WHITE;
                String depCargo = combinarDependenciaCargo(u.getAreaDependencia(), u.getCargoFuncion());

                table.addCell(tdCenter(String.valueOf(idx), FONT_TD, rowBg));
                table.addCell(tdLeft(safe(u.getApellido(), "-") + ", " + safe(u.getNombre(), "-"), FONT_TD_BOLD, rowBg));
                table.addCell(tdLeft(safe(u.getDniCuil(), "-"), FONT_TD, rowBg));
                table.addCell(tdLeft(safe(u.getCorreo(), "-"), FONT_TD, rowBg));
                table.addCell(tdLeft(depCargo, FONT_TD, rowBg));
                table.addCell(tdLeft(formatearAcceso(u.getTipoAcceso()), FONT_TD, rowBg));
                table.addCell(tdLeft(safe(u.getSeccionAlcance(), "-"), FONT_TD, rowBg));
                table.addCell(tdBool(u.isRequiereCuentaNuevaConeauGlobal(), rowBg));
                table.addCell(tdBool(u.isCuentaConeauGlobalCreada(), rowBg));
                table.addCell(tdBool(u.isHabilitadoEnConeau(), rowBg));
                idx++;
            }
        }

        document.add(table);
    }

    private static void agregarTablaMatrizPermisos(Document document, List<ItemMatrizPermisoAtenea> matriz) {
        PdfPTable table = new PdfPTable(new float[]{28f, 34f, 12f, 10f, 8f, 8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        String[] headers = {
                "Proceso / Actividad", "Ítem", "Sección", "Tipo de Permiso", "SGI / AEA", "Comisión (CA)"
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell th = new PdfPCell(new Phrase(headers[i], FONT_TH));
            th.setBackgroundColor(COLOR_PRIMARIO_OSCURO);
            th.setBorderColor(COLOR_BORDE);
            th.setPadding(5);
            th.setHorizontalAlignment(i >= 3 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
            th.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(th);
        }

        List<ItemMatrizPermisoAtenea> items = (matriz != null ? matriz : List.<ItemMatrizPermisoAtenea>of()).stream()
                .sorted(Comparator.comparingInt(ItemMatrizPermisoAtenea::getOrden)
                        .thenComparing(m -> m.getTipoPermiso() != null ? m.getTipoPermiso().ordinal() : 0))
                .toList();

        if (items.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No se registraron ítems en la matriz de permisos.", FONT_EMPTY));
            empty.setColspan(headers.length);
            empty.setBorderColor(COLOR_BORDE);
            empty.setPadding(6);
            table.addCell(empty);
        } else {
            int rowIdx = 0;
            for (ItemMatrizPermisoAtenea m : items) {
                Color rowBg = (rowIdx % 2 == 1) ? COLOR_FONDO_ALT : Color.WHITE;
                table.addCell(tdLeft(safe(m.getProcesoActividad(), "-"), FONT_TD, rowBg));
                table.addCell(tdLeft(safe(m.getItem(), "-"), FONT_TD, rowBg));
                table.addCell(tdLeft(safe(m.getSeccion(), "-"), FONT_TD, rowBg));
                table.addCell(tdCenter(formatearTipoPermiso(m.getTipoPermiso()), FONT_TD_BOLD, rowBg));
                table.addCell(tdBool(m.isPermisoSgiAea(), rowBg));
                table.addCell(tdBool(m.isPermisoCa(), rowBg));
                rowIdx++;
            }
        }

        document.add(table);
    }

    private static PdfPCell tdLeft(String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(COLOR_BORDE);
        c.setPadding(4);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private static PdfPCell tdCenter(String text, Font font, Color bg) {
        PdfPCell c = tdLeft(text, font, bg);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private static PdfPCell tdBool(boolean value, Color bg) {
        return tdCenter(value ? "SÍ" : "NO", value ? FONT_TD_SI : FONT_TD_NO, bg);
    }

    private static Paragraph espacio(float pts) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(pts);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        return p;
    }

    private static String safe(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val.trim();
    }

    private static String combinarDependenciaCargo(String dep, String cargo) {
        String d = dep != null ? dep.trim() : "";
        String c = cargo != null ? cargo.trim() : "";
        if (!d.isEmpty() && !c.isEmpty()) return d + " — " + c;
        if (!d.isEmpty()) return d;
        if (!c.isEmpty()) return c;
        return "-";
    }

    private static String formatearAcceso(TipoAccesoAtenea acceso) {
        if (acceso == null) return "-";
        return switch (acceso) {
            case ADMINISTRACION -> "Administración";
            case VISUALIZACION -> "Visualización";
            case CARGA -> "Carga";
            case CARGA_Y_VISUALIZACION -> "Carga y Visualización";
        };
    }

    private static String formatearTipoPermiso(TipoPermisoMatrizAtenea tipo) {
        if (tipo == null) return "-";
        return tipo.getDescripcion();
    }

    private static final class FooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(new float[]{75f, 25f});
            footer.setTotalWidth(document.right() - document.left());

            PdfPCell left = new PdfPCell(new Phrase(
                    "Universidad Nacional de Chilecito (UNdeC) — Área de Evaluación y Acreditación (AEA) · RG-003",
                    FONT_FOOTER));
            left.setBorder(Rectangle.TOP);
            left.setBorderColor(COLOR_BORDE);
            left.setPaddingTop(4);
            footer.addCell(left);

            PdfPCell right = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), FONT_FOOTER));
            right.setBorder(Rectangle.TOP);
            right.setBorderColor(COLOR_BORDE);
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            right.setPaddingTop(4);
            footer.addCell(right);

            footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 8, writer.getDirectContent());
        }
    }
}
