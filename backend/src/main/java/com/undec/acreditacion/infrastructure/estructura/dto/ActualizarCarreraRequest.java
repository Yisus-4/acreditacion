package com.undec.acreditacion.infrastructure.estructura.dto;

import java.util.List;
import java.util.UUID;

public record ActualizarCarreraRequest(
        String nombre,
        String sigla,
        String areaDisciplinaria,
        String modalidad,
        String estado,
        String tituloOtorgado,
        Integer duracionAnios,
        String descripcion,
        Boolean activo,
        List<UUID> sedesIds
) {
}
