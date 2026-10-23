package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.EstadoConvocatoria;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConvocatoriaEscuelaDetalleDto(
        UUID id,
        UUID unidadAcademicaId,
        String unidadAcademicaNombre,
        String unidadAcademicaSigla,
        String codigo,
        String titulo,
        TipoProcesoAcreditacion tipoProceso,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        EstadoConvocatoria estado,
        String observaciones,
        UUID creadoPor,
        UUID confirmadoPorDe,
        Instant confirmadoPorDeEn,
        Instant creadoEn,
        Instant actualizadoEn,
        List<ConvocatoriaCarreraItemDto> carreras
) {
}
