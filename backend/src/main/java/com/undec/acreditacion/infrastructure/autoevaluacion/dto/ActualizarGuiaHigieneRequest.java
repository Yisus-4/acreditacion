package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.util.List;

public record ActualizarGuiaHigieneRequest(
        String instanciaResponsableUa,
        String politicaUnidadesApoyo,
        Boolean aplicaUnidadesApoyo,
        String observaciones,
        List<DocumentoHigieneItemRequest> documentos
) {
}
