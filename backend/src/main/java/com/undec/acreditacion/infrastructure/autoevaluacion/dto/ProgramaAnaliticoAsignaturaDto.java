package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record ProgramaAnaliticoAsignaturaDto(
        UUID id,
        UUID procesoId,
        UUID actividadCurricularId,
        String nombreAsignatura,
        String anioCursada,
        String resolucionAprobacion,
        UUID archivoId,
        String archivoNombre,
        String archivoRuta,
        Long tamanioBytes,
        String mimeType,
        String estado,
        String observaciones,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
