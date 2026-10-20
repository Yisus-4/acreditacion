package com.undec.acreditacion.domain.entities;

public enum CarpetaMinioDestino {
    NORMATIVA("01-Convocatoria y formalizacion CONEAU/Normativa/"),
    NORMATIVA_ACTAS("01-Convocatoria y formalizacion CONEAU/Normativa/Actas/"),
    EVALUACIONES_ANTERIORES("02-Evaluaciones anteriores CONEAU/");

    private final String rutaRelativa;

    CarpetaMinioDestino(String rutaRelativa) {
        this.rutaRelativa = rutaRelativa;
    }

    public String getRutaRelativa() {
        return rutaRelativa;
    }
}
