package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.util.List;
import java.util.UUID;

public record CrearCuerpoAcademicoMaestroRequest(
        String tipoDocumento,
        String dni,
        String cuit,
        String legajo,
        String nroDesignacionMapuche,
        UUID departamentoAcademicoId,
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
        List<CrearAsignacionCatedraItem> asignaciones
) {
    public CrearCuerpoAcademicoMaestroRequest(
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
            List<CrearAsignacionCatedraItem> asignaciones
    ) {
        this("DNI", dni, cuit, null, null, null, apellido, nombre, emailInstitucional,
                emailPersonal, telefono, tituloGrado, maximoTituloPosgrado, categoriaDocente,
                caracterDesignacion, dedicacionDocente, horasSemanales, true, asignaciones);
    }

    public record CrearAsignacionCatedraItem(
            UUID carreraId,
            UUID planEstudioId,
            UUID actividadCurricularId,
            String rolEnCatedra,
            Integer cargaHorariaSemanal
    ) {
    }
}
