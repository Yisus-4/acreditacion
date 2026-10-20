package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento_normativa")
public class DocumentoNormativaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "solicitud_normativa_id", nullable = false)
    private UUID solicitudNormativaId;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @Column(name = "tipo_documento", nullable = false, length = 100)
    private String tipoDocumento;

    @Column(name = "carpeta_destino", nullable = false, length = 255)
    private String carpetaDestino;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @Column(name = "es_borrador", nullable = false)
    private boolean esBorrador;

    @Column(name = "observado", nullable = false)
    private boolean observado;

    @Column(name = "motivo_observacion", columnDefinition = "TEXT")
    private String motivoObservacion;

    @Column(name = "subido_por", nullable = false)
    private UUID subidoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public DocumentoNormativaJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSolicitudNormativaId() {
        return solicitudNormativaId;
    }

    public void setSolicitudNormativaId(UUID solicitudNormativaId) {
        this.solicitudNormativaId = solicitudNormativaId;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getCarpetaDestino() {
        return carpetaDestino;
    }

    public void setCarpetaDestino(String carpetaDestino) {
        this.carpetaDestino = carpetaDestino;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public boolean isEsBorrador() {
        return esBorrador;
    }

    public void setEsBorrador(boolean esBorrador) {
        this.esBorrador = esBorrador;
    }

    public boolean isObservado() {
        return observado;
    }

    public void setObservado(boolean observado) {
        this.observado = observado;
    }

    public String getMotivoObservacion() {
        return motivoObservacion;
    }

    public void setMotivoObservacion(String motivoObservacion) {
        this.motivoObservacion = motivoObservacion;
    }

    public UUID getSubidoPor() {
        return subidoPor;
    }

    public void setSubidoPor(UUID subidoPor) {
        this.subidoPor = subidoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
