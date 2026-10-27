package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class GeneradorRegistroCaPdfService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_ARGENTINA);

    private GeneradorRegistroCaPdfService() {}

    public static byte[] generarPdf(RegistroConformacionCa ca, String nombreCarrera, String anioConvocatoria) {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("% Registro Oficial RG-002: Registro de Conformación de la Comisión de Autoevaluación (CA)\n");
        sb.append("% Universidad Nacional de Chilecito - Área de Evaluación y Acreditación (AEA)\n");
        sb.append("% Carrera: ").append(nombreCarrera).append("\n");
        sb.append("% Convocatoria: ").append(anioConvocatoria).append("\n");
        sb.append("% Estado: ").append(ca.getEstado().name()).append("\n");
        if (ca.getAprobadoEn() != null) {
            sb.append("% Aprobado el: ").append(FORMATTER.format(ca.getAprobadoEn())).append("\n");
        }
        sb.append("% Coincide con Comisión Curricular: ").append(ca.isCoincideComisionCurricular() ? "SI" : "NO").append("\n");
        sb.append("% --- NOMINA DE MIEMBROS DE LA COMISION DE AUTOEVALUACION ---\n");
        for (MiembroComisionAutoevaluacion m : ca.getMiembros()) {
            sb.append("% ").append(m.getNumeroOrden()).append(". ")
                    .append(m.getApellido()).append(", ").append(m.getPrimerNombre())
                    .append(" | DNI/CUIL: ").append(m.getDniCuil())
                    .append(" | Rol: ").append(m.getClaustroRol().name())
                    .append(" | Correo: ").append(m.getCorreo())
                    .append(" | Docente: ").append(m.isEsDocente() ? "SI" : "NO")
                    .append(" | CVar Vinculada: ").append(m.isCuentaCvarVinculada() ? "SI" : "NO")
                    .append(" | CONEAU Global: ").append(m.isCuentaConeauGlobalCreada() ? "SI" : "NO")
                    .append("\n");
        }
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n");
        String streamContent = "BT /F1 12 Tf 50 700 Td (RG-002: Registro de Conformacion de CA - UNdeC) Tj ET";
        sb.append("4 0 obj\n<< /Length ").append(streamContent.length()).append(" >>\nstream\n");
        sb.append(streamContent).append("\nendstream\nendobj\n");
        sb.append("xref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000200 00000 n \n");
        sb.append("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n300\n%%EOF\n");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
