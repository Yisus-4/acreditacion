package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;

import java.util.List;
import java.util.UUID;

public record RegistrarCarreraCommand(
        UUID unidadAcademicaId,
        String nombre,
        String sigla,
        String areaDisciplinaria,
        ModalidadCarrera modalidad,
        EstadoCarrera estado,
        String tituloOtorgado,
        Integer duracionAnios,
        String descripcion,
        List<UUID> sedesIds
) {
}
