package com.undec.acreditacion.application.input;

public record BadgePlazoDto(
    String estado,
    int diasRestantes,
    String etiqueta,
    String cssClass
) {}
