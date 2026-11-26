package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarDistribucionHorariaRequest(
        Integer horasClases,
        Integer horasInvestigacion,
        Integer horasExtension,
        Integer horasGestion,
        Integer horasDireccionTesis,
        Integer horasOtras,
        String motivoCambio
) {
    public GuardarDistribucionHorariaRequest(
            Integer horasClases,
            Integer horasInvestigacion,
            Integer horasExtension,
            Integer horasGestion,
            Integer horasDireccionTesis,
            Integer horasOtras
    ) {
        this(horasClases, horasInvestigacion, horasExtension, horasGestion, horasDireccionTesis, horasOtras, null);
    }
}
