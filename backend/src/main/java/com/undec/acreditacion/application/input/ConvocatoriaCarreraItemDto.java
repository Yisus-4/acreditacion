package com.undec.acreditacion.application.input;

import java.util.UUID;

public record ConvocatoriaCarreraItemDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String modalidad,
        String nivel,
        String areaDisciplinaria,
        boolean seleccionadaPorAea,
        boolean esObligatoria,
        boolean seleccionadaPorDe,
        UUID procesoId,
        String estadoProceso
) {
}
