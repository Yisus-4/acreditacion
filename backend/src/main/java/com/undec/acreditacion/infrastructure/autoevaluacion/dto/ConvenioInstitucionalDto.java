package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConvenioInstitucionalDto(
        UUID id,
        String expediente,
        String resolucionRectoral,
        String contraparte,
        LocalDate fechaCelebracion,
        String duracion,
        LocalDate fechaCese,
        String objeto,
        Boolean vigente,
        String estadoVigencia,
        String estadoEjecucion,
        String financiamiento,
        String responsableUndec,
        String objetivosGenerales,
        String objetivosEspecificos,
        String resultadosEsperados,
        String carrerasBeneficiadas,
        String tipoConvenio,
        UUID archivoId,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
