package com.undec.acreditacion.domain.entities;

public enum TipoPermisoMatrizAtenea {
    LECTURA("Lectura"),
    LECTURA_MODIFICACION("Lectura y modificación");

    private final String descripcion;

    TipoPermisoMatrizAtenea(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
