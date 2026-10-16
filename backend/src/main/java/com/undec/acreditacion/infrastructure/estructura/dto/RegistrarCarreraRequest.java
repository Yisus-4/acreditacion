package com.undec.acreditacion.infrastructure.estructura.dto;

import java.util.List;
import java.util.UUID;

public record RegistrarCarreraRequest(
        UUID unidadAcademicaId,
        String nombre,
        String sigla,
        String areaDisciplinaria,
        String modalidad,
        String estado,
        String tituloOtorgado,
        Integer duracionAnios,
        String descripcion,
        List<UUID> sedesIds
) {
}
