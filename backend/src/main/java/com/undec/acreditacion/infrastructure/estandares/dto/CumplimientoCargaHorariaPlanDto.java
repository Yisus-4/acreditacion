package com.undec.acreditacion.infrastructure.estandares.dto;

import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;

import java.util.List;
import java.util.UUID;

public record CumplimientoCargaHorariaPlanDto(
        UUID planEstudioId,
        String nombrePlan,
        UUID estandarId,
        String estandarNombre,
        int totalAsignaturasSeDicta,
        int asignaturasConSeccion3Cargada,
        boolean habilitar311,
        ModoSeccion311 modo311,
        int horasPresencialesTotal311,
        int horasDistanciaTotal311,
        int horasAcumuladasTotal311,
        Integer horasMinimasTotal311,
        Integer diferenciaTotal311,
        boolean tieneMinimoTotal311,
        boolean cumpleTotal311,
        List<FilaCumplimientoDto> filas311,
        boolean habilitar312,
        ModoSeccion312 modo312,
        int horasPresencialesTotal312,
        int horasDistanciaTotal312,
        int horasAcumuladasTotal312,
        Integer horasMinimasTotal312,
        Integer diferenciaTotal312,
        boolean tieneMinimoTotal312,
        boolean cumpleTotal312,
        List<FilaCumplimientoDto> filas312,
        boolean cumpleGlobal
) {
    public record FilaCumplimientoDto(
            UUID areaId,
            String codigo,
            String nombre,
            int horasPresenciales,
            int horasDistancia,
            int horasAcumuladas,
            Integer horasMinimasCarrera,
            Integer diferencia,
            boolean tieneMinimo,
            boolean cumple
    ) {}
}
