package com.undec.acreditacion.domain.entities;

public enum GrupoUsuarioAtenea {
    SGI("Personal de SGI"),
    AEA("Personal de AEA (Área de Evaluación y Acreditación)"),
    OTRAS_AREAS("Personal de Otras Áreas Institucionales"),
    COMISION_AUTOEVALUACION("Miembros de la Comisión de Autoevaluación");

    private final String descripcion;

    GrupoUsuarioAtenea(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
