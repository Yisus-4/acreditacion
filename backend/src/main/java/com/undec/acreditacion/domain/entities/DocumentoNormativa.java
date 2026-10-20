package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.NormativaValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DocumentoNormativa {

    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB = 20971520 bytes
    public static final String MIME_PDF = "application/pdf";

    private final UUID id;
    private final UUID solicitudNormativaId;
    private final UUID procesoId;
    private final TipoDocumentoNormativa tipoDocumento;
    private final CarpetaMinioDestino carpetaDestino;
    private final String nombreOriginal;
    private final String storagePath;
    private final String mimeType;
    private final long tamanioBytes;
    private boolean esBorrador;
    private boolean observado;
    private String motivoObservacion;
    private final UUID subidoPor;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public DocumentoNormativa(
        UUID id,
        UUID solicitudNormativaId,
        UUID procesoId,
        TipoDocumentoNormativa tipoDocumento,
        CarpetaMinioDestino carpetaDestino,
        String nombreOriginal,
        String storagePath,
        String mimeType,
        long tamanioBytes,
        boolean esBorrador,
        boolean observado,
        String motivoObservacion,
        UUID subidoPor,
        Instant creadoEn,
        Instant actualizadoEn
    ) {
        this.id = Objects.requireNonNull(id, "El ID del documento es obligatorio");
        this.solicitudNormativaId = Objects.requireNonNull(solicitudNormativaId, "El ID de la solicitud es obligatorio");
        this.procesoId = Objects.requireNonNull(procesoId, "El ID del proceso es obligatorio");
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "El tipo de documento es obligatorio");
        this.carpetaDestino = Objects.requireNonNull(carpetaDestino, "La carpeta de destino es obligatoria");
        
        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
            throw new NormativaValidationException("El nombre original del archivo es obligatorio");
        }
        this.nombreOriginal = nombreOriginal.trim();

        if (storagePath == null || storagePath.trim().isEmpty()) {
            throw new NormativaValidationException("La ruta de almacenamiento (storage path) es obligatoria");
        }
        this.storagePath = storagePath.trim();

        if (mimeType == null || !MIME_PDF.equalsIgnoreCase(mimeType.trim())) {
            throw new NormativaValidationException("El archivo debe ser de tipo application/pdf");
        }
        this.mimeType = MIME_PDF;

        if (tamanioBytes <= 0 || tamanioBytes > MAX_FILE_SIZE_BYTES) {
            throw new NormativaValidationException("El tamaño del archivo debe ser mayor a 0 y menor o igual a 20 MB (20971520 bytes)");
        }
        this.tamanioBytes = tamanioBytes;

        this.esBorrador = esBorrador;
        this.observado = observado;
        this.motivoObservacion = motivoObservacion;
        this.subidoPor = Objects.requireNonNull(subidoPor, "El usuario que sube el archivo es obligatorio");
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public static DocumentoNormativa crearBorrador(
        UUID solicitudNormativaId,
        UUID procesoId,
        TipoDocumentoNormativa tipoDocumento,
        String nombreOriginal,
        String storagePath,
        String mimeType,
        long tamanioBytes,
        UUID subidoPor
    ) {
        Objects.requireNonNull(tipoDocumento, "El tipo de documento es obligatorio");
        Instant now = Instant.now();
        return new DocumentoNormativa(
            UUID.randomUUID(),
            solicitudNormativaId,
            procesoId,
            tipoDocumento,
            tipoDocumento.getCarpetaDestino(),
            nombreOriginal,
            storagePath,
            mimeType,
            tamanioBytes,
            true,
            false,
            null,
            subidoPor,
            now,
            now
        );
    }

    public void marcarComoDefinitivo() {
        this.esBorrador = false;
        this.actualizadoEn = Instant.now();
    }

    public void marcarObservado(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new NormativaValidationException("El motivo de observación del documento es obligatorio");
        }
        this.observado = true;
        this.motivoObservacion = motivo.trim();
        this.actualizadoEn = Instant.now();
    }

    public void limpiarObservacion() {
        this.observado = false;
        this.motivoObservacion = null;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSolicitudNormativaId() {
        return solicitudNormativaId;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public TipoDocumentoNormativa getTipoDocumento() {
        return tipoDocumento;
    }

    public CarpetaMinioDestino getCarpetaDestino() {
        return carpetaDestino;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public boolean isEsBorrador() {
        return esBorrador;
    }

    public boolean isObservado() {
        return observado;
    }

    public String getMotivoObservacion() {
        return motivoObservacion;
    }

    public UUID getSubidoPor() {
        return subidoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
