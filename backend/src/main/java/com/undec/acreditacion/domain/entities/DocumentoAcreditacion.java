package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DocumentoAcreditacion {

    public static final long MAX_TAMANIO_BYTES = 20 * 1024 * 1024; // 20 MB = 20,971,520 bytes
    public static final String MIME_PDF = "application/pdf";

    private final UUID id;
    private final UUID procesoId;
    private final UUID actividadId;
    private final String nombreOriginal;
    private final TipoDocumentoAcreditacion tipoDocumento;
    private final String clasificacionOrigen;
    private final String descripcion;
    private final String mimeType;
    private final long tamanioBytes;
    private final String storagePath;
    private final UUID subidoPor;
    private final Instant creadoEn;

    public static DocumentoAcreditacion crear(UUID procesoId,
                                              UUID actividadId,
                                              String nombreOriginal,
                                              TipoDocumentoAcreditacion tipoDocumento,
                                              String mimeType,
                                              long tamanioBytes,
                                              String storagePath,
                                              UUID subidoPor) {
        return crear(
                procesoId,
                actividadId,
                nombreOriginal,
                tipoDocumento,
                null,
                null,
                mimeType,
                tamanioBytes,
                storagePath,
                subidoPor
        );
    }

    public static DocumentoAcreditacion crear(UUID procesoId,
                                              UUID actividadId,
                                              String nombreOriginal,
                                              TipoDocumentoAcreditacion tipoDocumento,
                                              String clasificacionOrigen,
                                              String descripcion,
                                              String mimeType,
                                              long tamanioBytes,
                                              String storagePath,
                                              UUID subidoPor) {
        return new DocumentoAcreditacion(
                UUID.randomUUID(),
                procesoId,
                actividadId,
                nombreOriginal,
                tipoDocumento,
                clasificacionOrigen,
                descripcion,
                mimeType,
                tamanioBytes,
                storagePath,
                subidoPor,
                Instant.now()
        );
    }

    public static DocumentoAcreditacion rehydrate(UUID id,
                                                  UUID procesoId,
                                                  UUID actividadId,
                                                  String nombreOriginal,
                                                  TipoDocumentoAcreditacion tipoDocumento,
                                                  String mimeType,
                                                  long tamanioBytes,
                                                  String storagePath,
                                                  UUID subidoPor,
                                                  Instant creadoEn) {
        return rehydrate(
                id,
                procesoId,
                actividadId,
                nombreOriginal,
                tipoDocumento,
                null,
                null,
                mimeType,
                tamanioBytes,
                storagePath,
                subidoPor,
                creadoEn
        );
    }

    public static DocumentoAcreditacion rehydrate(UUID id,
                                                  UUID procesoId,
                                                  UUID actividadId,
                                                  String nombreOriginal,
                                                  TipoDocumentoAcreditacion tipoDocumento,
                                                  String clasificacionOrigen,
                                                  String descripcion,
                                                  String mimeType,
                                                  long tamanioBytes,
                                                  String storagePath,
                                                  UUID subidoPor,
                                                  Instant creadoEn) {
        return new DocumentoAcreditacion(
                id,
                procesoId,
                actividadId,
                nombreOriginal,
                tipoDocumento,
                clasificacionOrigen,
                descripcion,
                mimeType,
                tamanioBytes,
                storagePath,
                subidoPor,
                creadoEn
        );
    }

    public DocumentoAcreditacion(UUID id,
                                 UUID procesoId,
                                 UUID actividadId,
                                 String nombreOriginal,
                                 TipoDocumentoAcreditacion tipoDocumento,
                                 String mimeType,
                                 long tamanioBytes,
                                 String storagePath,
                                 UUID subidoPor,
                                 Instant creadoEn) {
        this(
                id,
                procesoId,
                actividadId,
                nombreOriginal,
                tipoDocumento,
                null,
                null,
                mimeType,
                tamanioBytes,
                storagePath,
                subidoPor,
                creadoEn
        );
    }

    public DocumentoAcreditacion(UUID id,
                                 UUID procesoId,
                                 UUID actividadId,
                                 String nombreOriginal,
                                 TipoDocumentoAcreditacion tipoDocumento,
                                 String clasificacionOrigen,
                                 String descripcion,
                                 String mimeType,
                                 long tamanioBytes,
                                 String storagePath,
                                 UUID subidoPor,
                                 Instant creadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID del documento es obligatorio");
        }
        if (procesoId == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }
        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
            throw new DomainValidationException("El nombre original del documento es obligatorio");
        }
        if (tipoDocumento == null) {
            throw new DomainValidationException("El tipo de documento es obligatorio");
        }
        if (mimeType == null || !MIME_PDF.equalsIgnoreCase(mimeType.trim())) {
            throw new DomainValidationException("Solo se admiten documentos en formato PDF (application/pdf)");
        }
        if (tamanioBytes <= 0 || tamanioBytes > MAX_TAMANIO_BYTES) {
            throw new DomainValidationException("El tamaño del archivo debe ser mayor a 0 y menor o igual a 20 MB");
        }
        if (storagePath == null || storagePath.trim().isEmpty()) {
            throw new DomainValidationException("La ruta de almacenamiento (storagePath) es obligatoria");
        }
        if (subidoPor == null) {
            throw new DomainValidationException("El usuario que sube el documento es obligatorio");
        }

        this.id = id;
        this.procesoId = procesoId;
        this.actividadId = actividadId;
        this.nombreOriginal = nombreOriginal.trim();
        this.tipoDocumento = tipoDocumento;
        this.clasificacionOrigen = (clasificacionOrigen != null && !clasificacionOrigen.trim().isEmpty())
                ? clasificacionOrigen.trim()
                : null;
        this.descripcion = (descripcion != null && !descripcion.trim().isEmpty())
                ? descripcion.trim()
                : null;
        this.mimeType = MIME_PDF;
        this.tamanioBytes = tamanioBytes;
        this.storagePath = storagePath.trim();
        this.subidoPor = subidoPor;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
    }

    public String getClasificacionOrigen() {
        return clasificacionOrigen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public UUID getActividadId() {
        return actividadId;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public TipoDocumentoAcreditacion getTipoDocumento() {
        return tipoDocumento;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public UUID getSubidoPor() {
        return subidoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentoAcreditacion that = (DocumentoAcreditacion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
