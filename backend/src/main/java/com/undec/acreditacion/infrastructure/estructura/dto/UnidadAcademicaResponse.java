package com.undec.acreditacion.infrastructure.estructura.dto;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

import java.time.Instant;
import java.util.UUID;

public record UnidadAcademicaResponse(
        UUID id,
        UUID institucionId,
        String nombre,
        String sigla,
        String descripcion,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static UnidadAcademicaResponse fromDomain(UnidadAcademica domain) {
        if (domain == null) return null;
        return new UnidadAcademicaResponse(
                domain.getId(),
                domain.getInstitucionId(),
                domain.getNombre(),
                domain.getSigla(),
                domain.getDescripcion(),
                domain.isActivo(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );
    }
}
