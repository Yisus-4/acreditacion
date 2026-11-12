package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.util.List;
import java.util.UUID;

public record CrearCuerpoAcademicoMaestroRequest(
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
    public record CrearAsignacionCatedraItem(
            UUID carreraId,
            UUID planEstudioId,
            UUID actividadCurricularId,
            String rolEnCatedra,
            Integer cargaHorariaSemanal
    ) {
    }
}
