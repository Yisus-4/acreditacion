package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CuerpoAcademicoMaestroDto(
        UUID id,
        String tipoDocumento,
        String dni,
        String cuit,
        String legajo,
        String nroDesignacionMapuche,
        UUID departamentoAcademicoId,
        String departamentoAcademicoCodigo,
        String departamentoAcademicoNombre,
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
    public CuerpoAcademicoMaestroDto(
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
        this(id, "DNI", dni, cuit, null, null, null, null, null,
                apellido, nombre, emailInstitucional, emailPersonal, telefono,
                tituloGrado, maximoTituloPosgrado, categoriaDocente, caracterDesignacion,
                dedicacionDocente, horasSemanales, activo, asignaciones, creadoEn, actualizadoEn);
    }
}
