package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class GeneradorRegistroUsuariosAteneaPdfService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_ARGENTINA);

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
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("% =============================================================================\n");
        sb.append("% REGISTRO OFICIAL RG-003: Registro de permisos de usuarios habilitados\n");
        sb.append("% Dependencia: Área de Evaluación y Acreditación (AEA)\n");
        sb.append("% Título: Registro de permisos de usuarios habilitados\n");
        sb.append("% Versión: Versión N° 01\n");
        sb.append("% Código: RG-003\n");
        sb.append("% Universidad Nacional de Chilecito - UNdeC\n");
        sb.append("% =============================================================================\n");
        sb.append("% METADATOS DEL REGISTRO:\n");
        sb.append("% - Año académico: ").append(anioAcademico != null ? anioAcademico : "-").append("\n");
        sb.append("% - Carrera: ").append(nombreCarrera).append("\n");
        sb.append("% - Tipo de convocatoria: ").append(tipoConvocatoria != null ? tipoConvocatoria : "Grado - en funcionamiento - 1era Fase").append("\n");
        sb.append("% - Nro. de expediente TAD: ").append(nroExpedienteTad != null ? nroExpedienteTad : "S/N").append("\n");
        sb.append("% - Estado: ").append(registro.getEstado().name()).append("\n");
        if (registro.getAprobadoEn() != null) {
            sb.append("% - Fecha de finalización / emisión: ").append(FORMATTER.format(registro.getAprobadoEn())).append("\n");
        }
        sb.append("% -----------------------------------------------------------------------------\n");
        sb.append("% SECCION I: TABLA DE USUARIOS HABILITADOS EN CONEAU GLOBAL ATENEA\n");
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
                            .append(" | Dependencia: ").append(u.getAreaDependencia() != null ? u.getAreaDependencia() : "-")
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
        sb.append("% SECCION II: MATRIZ DE PERMISOS DEL SISTEMA CONEAU GLOBAL ATENEA\n");
        sb.append("% -----------------------------------------------------------------------------\n");
        sb.append("% Proceso/Actividades | Item | Sección | Tipo de permiso | Permisos SGI-AEA | Permisos CA\n");

        List<ItemMatrizPermisoAtenea> itemsMatriz = registro.getMatrizPermisos().stream()
                .sorted(Comparator.comparingInt(ItemMatrizPermisoAtenea::getOrden))
                .toList();

        String procesoActual = "";
        for (ItemMatrizPermisoAtenea m : itemsMatriz) {
            if (!m.getProcesoActividad().equals(procesoActual)) {
                procesoActual = m.getProcesoActividad();
                sb.append("% \n% [Proceso] ").append(procesoActual).append("\n");
            }
            sb.append("%   - Item: ").append(m.getItem())
                    .append(" | Sección: ").append(m.getSeccion() != null && !m.getSeccion().isBlank() ? m.getSeccion() : "Todos")
                    .append(" | Permiso: ").append(m.getTipoPermiso().getDescripcion())
                    .append(" | SGI-AEA: ").append(m.isPermisoSgiAea() ? "SI" : "NO")
                    .append(" | CA: ").append(m.isPermisoCa() ? "SI" : "NO")
                    .append("\n");
        }

        sb.append("% -----------------------------------------------------------------------------\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n");
        String streamContent = "BT /F1 12 Tf 50 720 Td (RG-003: Registro de permisos de usuarios habilitados - UNdeC AEA) Tj 0 -20 Td ("
                + nombreCarrera.replaceAll("[()]", "") + " - " + (anioAcademico != null ? anioAcademico : "") + ") Tj ET";
        sb.append("4 0 obj\n<< /Length ").append(streamContent.length()).append(" >>\nstream\n");
        sb.append(streamContent).append("\nendstream\nendobj\n");
        sb.append("xref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000200 00000 n \n");
        sb.append("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n300\n%%EOF\n");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
