package com.undec.acreditacion.infrastructure.estandares.dto;

import java.util.List;
import java.util.UUID;

public record FichaEstandarActividadDto(
        UUID planEstudioId,
        UUID actividadCurricularId,
        String asignaturaCodigo,
        String asignaturaNombre,
        Integer asignaturaHorasReloj,
        UUID docenteResponsableId,
        String docenteResponsableNombre,
        boolean puedeEditar,
        EstandarDetalleDto estandar,
        List<Respuesta22Dto> respuestas22,
        List<CargaAreaDto> cargasPorArea,
        ResumenCargaDto resumenCarga,
        boolean seccion22Completa,
        boolean seccion3Completa,
        boolean fichaCompleta
) {
    public record Respuesta22Dto(
            UUID estandarItemId,
            UUID escalaEvaluacionId
    ) {}

    public record CargaAreaDto(
            UUID areaCargaHorariaId,
            Integer horasTotalPresencial,
            Integer horasTotalDistancia,
            Integer horasPracticaPresencial,
            Integer horasPracticaDistancia
    ) {}

    public record ResumenCargaDto(
            String ambitosFormacionPractica,
            Integer horasSemanalTotalPresencial,
            Integer horasSemanalTotalDistancia,
            Integer horasSemanalPracticaPresencial,
            Integer horasSemanalPracticaDistancia
    ) {}
}
