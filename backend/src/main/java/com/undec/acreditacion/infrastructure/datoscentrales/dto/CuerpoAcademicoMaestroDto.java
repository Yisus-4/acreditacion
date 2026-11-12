package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CuerpoAcademicoMaestroDto(
        UUID id,
        String dni,
        String cuit,
        String apellido,
        String nombre,
        String emailInstitucional,
        String emailPersonal,
        String telefono,
        String tituloGrado,
        String maximoTituloPosgrado,
        String categoriaDocente,
        String caracterDesignacion,
        String dedicacionDocente,
        Integer horasSemanales,
        Boolean activo,
        List<AsignacionCatedraDto> asignaciones,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
