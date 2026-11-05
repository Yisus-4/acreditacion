package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record AutoevaluacionCarreraAmbitoDto(
        UUID id,
        UUID procesoId,
        UUID ambitoId,
        AmbitoPracticaDto ambito,
        Boolean utilizadoPorCarrera,
        String asignaturasVinculadas,
        String responsablePractica,
        String horariosUsoAnioAnterior,
        String tipoPracticaEstudiantes,
        String observacionesCarrera,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
