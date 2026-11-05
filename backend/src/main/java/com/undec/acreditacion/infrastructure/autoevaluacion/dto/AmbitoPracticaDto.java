package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AmbitoPracticaDto(
        UUID id,
        Integer numeroOrden,
        String denominacion,
        String tipoCategoria,
        String tipoAmbito,
        String tipoEstablecimiento,
        UUID inmuebleId,
        String inmuebleNombre,
        UUID convenioId,
        String convenioNombre,
        String descripcion,
        Integer anioConstruccion,
        BigDecimal superficieM2,
        Integer capacidadAlumnos,
        String disponibilidadHoraria,
        String direccion,
        String localidad,
        String provincia,
        String departamento,
        String codigoPostal,
        String tipoDependencia,
        String sector,
        String actividadAlumnoCam,
        String especiesAnimalesCultivos,
        Integer numeroAnimales,
        String actividadesDemostrativas,
        String carrerasQueLoUsan,
        Boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
