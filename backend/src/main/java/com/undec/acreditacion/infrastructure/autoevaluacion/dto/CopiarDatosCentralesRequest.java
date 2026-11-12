package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.undec.acreditacion.application.ports.ModoClonacion;
import com.undec.acreditacion.application.ports.SeccionClonacion;

import java.util.Set;

public record CopiarDatosCentralesRequest(
        Set<SeccionClonacion> secciones,
        ModoClonacion modo
) {
    public CopiarDatosCentralesRequest {
        if (secciones == null || secciones.isEmpty()) {
            secciones = Set.of(SeccionClonacion.values());
        }
        if (modo == null) {
            modo = ModoClonacion.INICIALIZAR_O_AGREGAR_FALTANTES;
        }
    }
}
