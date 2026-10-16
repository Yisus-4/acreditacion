package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;

import java.util.List;
import java.util.UUID;

public record ActualizarCarreraCommand(
        UUID id,
        String nombre,
        String sigla,
        String areaDisciplinaria,
        ModalidadCarrera modalidad,
        EstadoCarrera estado,
        String tituloOtorgado,
        Integer duracionAnios,
        String descripcion,
        Boolean activo,
        List<UUID> sedesIds
) {
}
