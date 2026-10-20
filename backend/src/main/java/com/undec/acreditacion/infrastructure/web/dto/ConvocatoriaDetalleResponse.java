package com.undec.acreditacion.infrastructure.web.dto;

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
        List<ActividadDetalleResponse> actividades
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
             registroConvocatoria, planTrabajo, Collections.emptyList());
    }
}
