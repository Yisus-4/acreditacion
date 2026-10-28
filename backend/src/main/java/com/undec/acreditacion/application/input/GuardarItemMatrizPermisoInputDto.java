package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;

import java.util.UUID;

public record GuardarItemMatrizPermisoInputDto(
        UUID id,
        String procesoActividad,
        String item,
        String seccion,
        TipoPermisoMatrizAtenea tipoPermiso,
        boolean permisoSgiAea,
        boolean permisoCa,
        int orden
) {}
