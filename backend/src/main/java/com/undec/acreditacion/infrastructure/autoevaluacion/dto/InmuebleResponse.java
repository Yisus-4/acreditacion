package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InmuebleResponse(
        UUID id,
        UUID institucionId,
        Integer numeroOrden,
        String nombre,
        String domicilio,
        String calle,
        String numero,
        UUID sedeId,
        String sedeNombre,
        String unidadAcademicaDenominacion,
        Long provinciaId,
        String provinciaNombre,
        Long departamentoId,
        String departamentoNombre,
        Long localidadId,
        String localidadNombre,
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
        Boolean verificado,
        Instant fechaVerificacion,
        String estadoConeau,
        Boolean activo,
        List<CarreraVinculadaResponse> carreras,
        List<EspacioFisicoResponse> espacios,
        List<DocumentacionRespaldoResponse> documentos,
        int cantidadEspacios,
        boolean tieneDocumentacionRespaldo
) {
    public record CarreraVinculadaResponse(
            UUID id,
            String nombre,
            String sigla
    ) {}
}
