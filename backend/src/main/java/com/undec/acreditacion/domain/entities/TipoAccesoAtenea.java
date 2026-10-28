package com.undec.acreditacion.domain.entities;

public enum TipoAccesoAtenea {
    CARGA("Carga de información"),
    VISUALIZACION("Visualización de información"),
    ADMINISTRACION("Administración y gestión"),
    CARGA_Y_VISUALIZACION("Carga y visualización de información");

    private final String descripcion;

    TipoAccesoAtenea(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
