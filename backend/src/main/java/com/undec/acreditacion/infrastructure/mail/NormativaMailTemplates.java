package com.undec.acreditacion.infrastructure.mail;

import com.undec.acreditacion.domain.entities.CumplimientoPlazo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NormativaMailTemplates {

    private static final String COLOR_PRIMARY = "#826170"; // Granate UNdeC
    private static final String COLOR_SECONDARY = "#e5b115"; // Dorado UNdeC
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildSolicitudNormativaHtml(
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        LocalDate fechaLimite,
        String observaciones
    ) {
        String fechaStr = fechaLimite != null ? fechaLimite.format(DATE_FORMATTER) : "A coordinar";
        String obsHtml = (observaciones != null && !observaciones.isBlank())
            ? "<div style='margin-top:15px;padding:12px;background:#f9fafb;border-left:4px solid " + COLOR_SECONDARY + ";'>"
            + "<strong>Observaciones de AEA:</strong><p style='margin:5px 0 0;'>" + escape(observaciones) + "</p></div>"
            : "";

        return wrapTemplate(
            "Solicitud Formal de Normativa de Carrera",
            "<p>Estimado/a <strong>" + escape(destinatarioNombre) + "</strong>,</p>"
            + "<p>El Área de Evaluación y Acreditación (AEA) de la UNdeC ha iniciado formalmente la <strong>Actividad 2 del Plan de Trabajo PT-001</strong> para la carrera <strong>" + escape(carreraNombre) + "</strong> (Convocatoria: <em>" + escape(codigoConvocatoria) + "</em>).</p>"
            + "<p>Se solicita cargar la documentación y normativa correspondiente en el Sistema de Acreditación dentro del plazo reglamentario de <strong>5 días hábiles administrativos</strong>.</p>"
            + "<p style='font-size:16px;'><strong>Fecha límite de remisión:</strong> <span style='color:" + COLOR_PRIMARY + ";font-weight:bold;'>" + fechaStr + "</span></p>"
            + obsHtml
        );
    }

    public String buildRemisionRevisionHtml(
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        CumplimientoPlazo cumplimiento,
        int cantidadDocumentos
    ) {
        String estadoPlazo = cumplimiento == CumplimientoPlazo.EN_TERMINO
            ? "<span style='color:#166534;font-weight:bold;'>En Término</span>"
            : "<span style='color:#991b1b;font-weight:bold;'>Fuera de Término</span>";

        return wrapTemplate(
            "Remisión de Documentación Normativa a Revisión",
            "<p>Estimados integrantes del <strong>" + escape(destinatarioNombre) + "</strong>,</p>"
            + "<p>La Dirección de Carrera de <strong>" + escape(carreraNombre) + "</strong> ha formalizado la entrega definitiva de la normativa correspondiente a la convocatoria <em>" + escape(codigoConvocatoria) + "</em>.</p>"
            + "<p><strong>Cantidad de documentos adjuntos:</strong> " + cantidadDocumentos + "</p>"
            + "<p><strong>Evaluación de cumplimiento de plazo:</strong> " + estadoPlazo + "</p>"
            + "<p>El expediente se encuentra en estado <em>ENVIADA_A_REVISION</em> disponible para dictamen técnico de AEA.</p>"
        );
    }

    public String buildObservacionesHtml(
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        String observacionesRevision,
        List<String> documentosObservados
    ) {
        StringBuilder docsHtml = new StringBuilder();
        if (documentosObservados != null && !documentosObservados.isEmpty()) {
            docsHtml.append("<div style='margin-top:15px;'><strong>Documentos que requieren subsanación:</strong><ul>");
            for (String doc : documentosObservados) {
                docsHtml.append("<li>").append(escape(doc)).append("</li>");
            }
            docsHtml.append("</ul></div>");
        }

        return wrapTemplate(
            "Observaciones sobre la Documentación de Normativa",
            "<p>Estimado/a <strong>" + escape(destinatarioNombre) + "</strong>,</p>"
            + "<p>El Área de Evaluación y Acreditación (AEA) ha revisado la documentación remitida para <strong>" + escape(carreraNombre) + "</strong> (Convocatoria: <em>" + escape(codigoConvocatoria) + "</em>) y ha formulado las siguientes observaciones técnicas para su subsanación:</p>"
            + "<div style='margin-top:15px;padding:12px;background:#fee2e2;border-left:4px solid #991b1b;'>"
            + "<strong>Fundamentación del dictamen:</strong>"
            + "<p style='margin:5px 0 0;'>" + escape(observacionesRevision) + "</p>"
            + "</div>"
            + docsHtml
            + "<p style='margin-top:15px;'>La bandeja de carga se encuentra nuevamente habilitada para subir los archivos corregidos.</p>"
        );
    }

    public String buildAprobacionHtml(
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        String observaciones
    ) {
        String obsHtml = (observaciones != null && !observaciones.isBlank())
            ? "<div style='margin-top:15px;padding:12px;background:#f0fdf4;border-left:4px solid #166534;'>"
            + "<strong>Dictamen de aprobación:</strong><p style='margin:5px 0 0;'>" + escape(observaciones) + "</p></div>"
            : "";

        return wrapTemplate(
            "Aprobación de Normativa de Carrera - Actividad 2 Completada",
            "<p>Estimado/a <strong>" + escape(destinatarioNombre) + "</strong>,</p>"
            + "<p>Nos complace informar que el Área de Evaluación y Acreditación (AEA) ha <strong>aprobado satisfactoriamente</strong> la totalidad de la normativa presentada para <strong>" + escape(carreraNombre) + "</strong> (Convocatoria: <em>" + escape(codigoConvocatoria) + "</em>).</p>"
            + "<p>La <strong>Actividad 2 del Plan de Trabajo PT-001</strong> ha sido marcada como <strong>COMPLETADA</strong> en la Hoja INFO y el cronograma general del proceso.</p>"
            + obsHtml
        );
    }

    private String wrapTemplate(String titulo, String cuerpoHtml) {
        return "<!DOCTYPE html>"
            + "<html>"
            + "<head><meta charset='UTF-8'></head>"
            + "<body style='font-family:Arial,Helvetica,sans-serif;background-color:#f5f5f6;margin:0;padding:20px;color:#333333;'>"
            + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color:#ffffff;border:1px solid #dcdfe3;border-collapse:collapse;'>"
            + "<tr><td style='background-color:" + COLOR_PRIMARY + ";padding:25px;text-align:center;color:#ffffff;'>"
            + "<h1 style='margin:0;font-size:20px;letter-spacing:0.5px;'>UNIVERSIDAD NACIONAL DE CHILECITO</h1>"
            + "<p style='margin:5px 0 0;font-size:13px;opacity:0.9;'>Secretaría Académica — Área de Evaluación y Acreditación (AEA)</p>"
            + "</td></tr>"
            + "<tr><td style='padding:30px;'>"
            + "<h2 style='color:" + COLOR_PRIMARY + ";margin-top:0;font-size:18px;border-bottom:2px solid " + COLOR_SECONDARY + ";padding-bottom:8px;'>" + escape(titulo) + "</h2>"
            + cuerpoHtml
            + "<p style='margin-top:30px;font-size:13px;color:#666666;'>Este es un mensaje automático del Sistema de Gestión de Acreditaciones de Grado de la UNdeC.</p>"
            + "</td></tr>"
            + "<tr><td style='background-color:#f0f0f2;padding:15px;text-align:center;font-size:11px;color:#777777;border-top:1px solid #e0e0e0;'>"
            + "Área de Evaluación y Acreditación (AEA) — Universidad Nacional de Chilecito<br>La Rioja, República Argentina"
            + "</td></tr>"
            + "</table>"
            + "</body></html>";
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
