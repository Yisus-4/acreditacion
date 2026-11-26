package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarDistribucionHorariaRequest(
        Integer horasClases,
        Integer horasInvestigacion,
        Integer horasExtension,
        Integer horasGestion,
        Integer horasDireccionTesis,
        Integer horasOtras
) {}
