package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.application.input.ConvocatoriaCarreraItemDto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ConvocatoriaDetalleResponse(
        UUID id,
        String codigo,
        String titulo,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String nivel,
        String tipoProceso,
        String estado,
        Instant creadoEn,
        UUID creadoPor,
        RegistroConvocatoriaResponse registroConvocatoria,
        PlanTrabajoResumenResponse planTrabajo,
        List<ActividadDetalleResponse> actividades,
        UUID unidadAcademicaId,
        String unidadAcademicaNombre,
        String unidadAcademicaSigla,
        UUID confirmadoPorDe,
        Instant confirmadoPorDeEn,
        List<ConvocatoriaCarreraItemDto> carrerasPropuestas
) {
    public ConvocatoriaDetalleResponse(UUID id,
                                       String codigo,
                                       String titulo,
                                       UUID carreraId,
                                       String carreraNombre,
                                       String carreraSigla,
                                       String nivel,
                                       String tipoProceso,
                                       String estado,
                                       Instant creadoEn,
                                       UUID creadoPor,
                                       RegistroConvocatoriaResponse registroConvocatoria,
                                       PlanTrabajoResumenResponse planTrabajo) {
        this(id, codigo, titulo, carreraId, carreraNombre, carreraSigla, nivel, tipoProceso, estado, creadoEn, creadoPor,
             registroConvocatoria, planTrabajo, Collections.emptyList(), null, null, null, null, null, Collections.emptyList());
    }

    public ConvocatoriaDetalleResponse(UUID id,
                                       String codigo,
                                       String titulo,
                                       UUID carreraId,
                                       String carreraNombre,
                                       String carreraSigla,
                                       String nivel,
                                       String tipoProceso,
                                       String estado,
                                       Instant creadoEn,
                                       UUID creadoPor,
                                       RegistroConvocatoriaResponse registroConvocatoria,
                                       PlanTrabajoResumenResponse planTrabajo,
                                       List<ActividadDetalleResponse> actividades) {
        this(id, codigo, titulo, carreraId, carreraNombre, carreraSigla, nivel, tipoProceso, estado, creadoEn, creadoPor,
             registroConvocatoria, planTrabajo, actividades, null, null, null, null, null, Collections.emptyList());
    }
}
