package com.undec.acreditacion.infrastructure.estructura.dto;

import com.undec.acreditacion.domain.entities.Carrera;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CarreraResponse(
        UUID id,
        UUID unidadAcademicaId,
        String nombre,
        String sigla,
        String descripcion,
        String nivel,
        String areaDisciplinaria,
        String modalidad,
        String estado,
        String tituloOtorgado,
        Integer duracionAnios,
        boolean activo,
        List<UUID> sedesIds,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static CarreraResponse fromDomain(Carrera domain) {
        if (domain == null) return null;
        return new CarreraResponse(
                domain.getId(),
                domain.getUnidadAcademicaId(),
                domain.getNombre(),
                domain.getSigla(),
                domain.getDescripcion(),
                domain.getNivel(),
                domain.getAreaDisciplinaria(),
                domain.getModalidad().name(),
                domain.getEstado().name(),
                domain.getTituloOtorgado(),
                domain.getDuracionAnios(),
                domain.isActivo(),
                domain.getSedesIds(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );
    }
}
