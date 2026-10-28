package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class GeneradorRegistroUsuariosAteneaPdfService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_ARGENTINA);

    private GeneradorRegistroUsuariosAteneaPdfService() {}

    public static byte[] generarPdf(RegistroUsuariosAtenea registro, String nombreCarrera, String cicloFase, String anioConvocatoria) {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("% =============================================================================\n");
        sb.append("% Registro Oficial RG-003: Registro de usuarios de ATENEA\n");
        sb.append("% Universidad Nacional de Chilecito - Área de Evaluación y Acreditación (AEA)\n");
        sb.append("% =============================================================================\n");
        sb.append("% Carrera: ").append(nombreCarrera).append("\n");
        sb.append("% Ciclo / Fase: ").append(cicloFase != null ? cicloFase : "1raFase").append("\n");
        sb.append("% Convocatoria / Año: ").append(anioConvocatoria).append("\n");
        sb.append("% Estado: ").append(registro.getEstado().name()).append("\n");
        if (registro.getAprobadoEn() != null) {
            sb.append("% Fecha de finalización / registro: ").append(FORMATTER.format(registro.getAprobadoEn())).append("\n");
        }
        sb.append("% -----------------------------------------------------------------------------\n");
        sb.append("% DEFINICION DE USUARIOS Y PERMISOS CONEAU GLOBAL ATENEA\n");
        sb.append("% -----------------------------------------------------------------------------\n");

        for (GrupoUsuarioAtenea grupo : GrupoUsuarioAtenea.values()) {
            List<UsuarioPermisoAtenea> usuariosGrupo = registro.getUsuarios().stream()
                    .filter(u -> u.getGrupoUsuario() == grupo)
                    .toList();

            sb.append("% \n");
            sb.append("% >>> ").append(grupo.name()).append(" - ").append(grupo.getDescripcion()).append(" <<<\n");
            if (usuariosGrupo.isEmpty()) {
                sb.append("% (Sin usuarios registrados en este grupo)\n");
            } else {
                int idx = 1;
                for (UsuarioPermisoAtenea u : usuariosGrupo) {
                    sb.append("% ").append(idx++).append(". ")
                            .append(u.getApellido()).append(", ").append(u.getNombre())
                            .append(" | DNI/CUIL: ").append(u.getDniCuil())
                            .append(" | Correo: ").append(u.getCorreo())
                            .append(" | Área: ").append(u.getAreaDependencia() != null ? u.getAreaDependencia() : "-")
                            .append(" | Cargo: ").append(u.getCargoFuncion() != null ? u.getCargoFuncion() : "-")
                            .append(" | Acceso: ").append(u.getTipoAcceso() != null ? u.getTipoAcceso().name() : "-")
                            .append(" | Alcance: ").append(u.getSeccionAlcance() != null ? u.getSeccionAlcance() : "-")
                            .append(" | Req. Nueva Cuenta CONEAU: ").append(u.isRequiereCuentaNuevaConeauGlobal() ? "SI" : "NO")
                            .append(" | Cuenta Creada: ").append(u.isCuentaConeauGlobalCreada() ? "SI" : "NO")
                            .append(" | Habilitado en CONEAU: ").append(u.isHabilitadoEnConeau() ? "SI" : "NO")
                            .append("\n");
                }
            }
        }

        sb.append("% -----------------------------------------------------------------------------\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n");
        String streamContent = "BT /F1 12 Tf 50 720 Td (RG-003: Registro de usuarios de ATENEA - UNdeC) Tj 0 -20 Td ("
                + nombreCarrera.replaceAll("[()]", "") + " - " + anioConvocatoria + ") Tj ET";
        sb.append("4 0 obj\n<< /Length ").append(streamContent.length()).append(" >>\nstream\n");
        sb.append(streamContent).append("\nendstream\nendobj\n");
        sb.append("xref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000200 00000 n \n");
        sb.append("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n300\n%%EOF\n");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
