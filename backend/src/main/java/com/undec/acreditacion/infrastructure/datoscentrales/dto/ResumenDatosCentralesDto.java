package com.undec.acreditacion.infrastructure.datoscentrales.dto;

public record ResumenDatosCentralesDto(
        long planesEstudioVigentes,
        long cuerpoAcademicoActivo,
        long espaciosFisicos,
        long ambitosPractica
) {
}
