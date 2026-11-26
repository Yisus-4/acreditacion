package com.undec.acreditacion.domain.entities;

public enum EstadoSolicitudDesignacion {
    BORRADOR,
    PRESENTADA_SGA,
    APROBADA,
    OBSERVADA;

    public boolean esEditablePorDepartamento() {
        return this == BORRADOR || this == OBSERVADA;
    }
}
