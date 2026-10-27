package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class RegistroCaMapper {

    private RegistroCaMapper() {}

    public static RegistroCaDto toDto(RegistroConformacionCa r) {
        if (r == null) return null;

        Long diasRestantes = null;
        String semaforoTexto = null;

        if (r.getFechaLimiteDc() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), r.getFechaLimiteDc());
            diasRestantes = dias;
            if (dias > 1) {
                semaforoTexto = "Faltan " + dias + " días";
            } else if (dias == 1) {
                semaforoTexto = "Falta 1 día";
            } else if (dias == 0) {
                semaforoTexto = "Vence hoy";
            } else {
                semaforoTexto = "Plazo vencido (" + Math.abs(dias) + " d)";
            }
        }

        List<RegistroCaDto.MiembroCaDto> miembrosDto = r.getMiembros().stream()
                .map(m -> new RegistroCaDto.MiembroCaDto(
                        m.getId(),
                        m.getRegistroCaId(),
                        m.getApellido(),
                        m.getPrimerNombre(),
                        m.getSegundoNombre(),
                        m.getDniCuil(),
                        m.getCorreo(),
                        m.getTelefono(),
                        m.getClaustroRol(),
                        m.isEsDocente(),
                        m.isCuentaConeauGlobalCreada(),
                        m.isCuentaCvarVinculada(),
                        m.getNumeroOrden()
                ))
                .toList();

        return new RegistroCaDto(
                r.getId(),
                r.getProcesoId(),
                r.getEstado(),
                r.isCoincideComisionCurricular(),
                r.getSolicitadoEn(),
                r.getSolicitadoPor(),
                r.getFechaLimiteDc(),
                diasRestantes,
                semaforoTexto,
                r.getRemitidoEn(),
                r.getRemitidoPor(),
                r.getAprobadoEn(),
                r.getAprobadoPor(),
                r.getObservacionesRevision(),
                r.getResolucionStoragePath(),
                r.getResolucionNombreOriginal(),
                r.getResolucionTamanioBytes(),
                r.getRegistroGeneradoStoragePath(),
                r.getCreadoEn(),
                r.getActualizadoEn(),
                miembrosDto
        );
    }

    public static List<MiembroComisionAutoevaluacion> toDomainList(List<MiembroCaInputDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .map(d -> new MiembroComisionAutoevaluacion(
                        d.id(),
                        null,
                        d.apellido(),
                        d.primerNombre(),
                        d.segundoNombre(),
                        d.dniCuil(),
                        d.correo(),
                        d.telefono(),
                        d.claustroRol(),
                        d.esDocente(),
                        d.cuentaConeauGlobalCreada(),
                        d.cuentaCvarVinculada(),
                        d.numeroOrden()
                ))
                .toList();
    }
}
