package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegistrarInmuebleRequest(
        Integer numeroOrden,
        String nombre,
        String domicilio,
        String calle,
        String numero,
        UUID sedeId,
        Long provinciaId,
        Long departamentoId,
        Long localidadId,
        Integer anioConstruccion,
        Integer anioInicioInstitucion,
        Boolean propiedadInstitucion,
        String condicionTenencia,
        String situacion,
        Boolean vinculaTodasCarreras,
        Boolean esMaestro,
        UUID procesoId,
        BigDecimal superficieCubiertaM2,
        BigDecimal superficieTotalM2,
        String observaciones,
        String estadoConeau,
        List<UUID> carrerasIds,
        List<EspacioFisicoRequest> espacios,
        List<DocumentoRespaldoRequest> documentos
) {
    public record DocumentoRespaldoRequest(
            String nombreArchivo,
            String urlMinio,
            String tipoAnexo,
            String descripcion
    ) {}
}
