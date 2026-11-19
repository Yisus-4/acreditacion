package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record ProgramaAnaliticoAsignaturaDto(
        UUID id,
        UUID procesoId,
        UUID actividadCurricularId,
        UUID asignaturaPlanId,
        String asignaturaCodigo,
        String nombreAsignatura,
        String asignaturaNombre,
        String anioCursada,
        String resolucionAprobacion,
        Integer vigenciaDesde,
        Integer vigenciaHasta,
        UUID archivoId,
        String archivoNombre,
        String archivoRuta,
        String archivoUrl,
        Long tamanioBytes,
        String mimeType,
        String estado,
        String estadoCarga,
        String observaciones,
        UUID docenteDesignadoId,
        String docenteDesignadoNombre,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
