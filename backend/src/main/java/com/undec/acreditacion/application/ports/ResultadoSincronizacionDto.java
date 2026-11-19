package com.undec.acreditacion.application.ports;

import java.time.Instant;
import java.util.UUID;

public record ResultadoSincronizacionDto(
        UUID procesoId,
        int planesClonados,
        int actividadesCurricularesClonadas,
        int docentesClonados,
        int asignacionesDocentesClonadas,
        int inmueblesClonados,
        int espaciosClonados,
        int vinculacionesEspacioActividadClonadas,
        int ambitosPracticaClonados,
        ModoClonacion modoEjecutado,
        Instant timestamp
) {
    public static ResultadoSincronizacionDto vacio(UUID procesoId, ModoClonacion modo) {
        return new ResultadoSincronizacionDto(
                procesoId,
                0, 0, 0, 0, 0, 0, 0, 0,
                modo,
                Instant.now()
        );
    }
}
