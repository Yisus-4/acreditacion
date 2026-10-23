package com.undec.acreditacion.domain.entities;

/**
 * Áreas a las que el Área de Evaluación y Acreditación (AEA) informa sobre el proceso
 * de acreditación dispuesto por CONEAU (Actividad 1 del procedimiento oficial).
 */
public enum AreaInformadaProceso {
    DIRECCION_CARRERA("Dirección de Carrera"),
    DIRECCION_ESCUELA("Dirección de Escuela"),
    DIRECCIONES_DEPARTAMENTOS("Direcciones de Departamentos"),
    DIRECCIONES_INSTITUTOS("Direcciones de Institutos"),
    SECRETARIAS("Secretarías");

    private final String etiqueta;

    AreaInformadaProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
