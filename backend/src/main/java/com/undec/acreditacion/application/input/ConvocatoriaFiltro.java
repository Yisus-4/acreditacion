package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.EstadoProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;

import java.util.UUID;

public record ConvocatoriaFiltro(
        UUID carreraId,
        EstadoProcesoAcreditacion estado,
        TipoProcesoAcreditacion tipoProceso,
        Integer anio
) {
    public static ConvocatoriaFiltro vacio() {
        return new ConvocatoriaFiltro(null, null, null, null);
    }
}
