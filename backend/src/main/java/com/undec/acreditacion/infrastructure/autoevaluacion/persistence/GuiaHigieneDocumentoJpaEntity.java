package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guia_higiene_seguridad_documento")
public class GuiaHigieneDocumentoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id", nullable = false)
    private GuiaHigieneSeguridadJpaEntity guia;

    @Column(name = "tipo_documento_clave", nullable = false, length = 100)
    private String tipoDocumentoClave;

    @Column(name = "tipo_documento_nombre", nullable = false, length = 200)
    private String tipoDocumentoNombre;

    @Column(name = "organismo_emisor")
    private String organismoEmisor;

    @Column(name = "numero_normativa", length = 100)
    private String numeroNormativa;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "url_minio", length = 500)
    private String urlMinio;

    @Column(name = "ruta_repositorio", nullable = false, length = 500)
    private String rutaRepositorio = "03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa";

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio = false;

    @Column(name = "cargado", nullable = false)
    private Boolean cargado = false;

    @Column(name = "subido_por_usuario_id")
    private UUID subidoPorUsuarioId;

    @Column(name = "fecha_subida")
    private Instant fechaSubida;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public GuiaHigieneDocumentoJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GuiaHigieneSeguridadJpaEntity getGuia() {
        return guia;
    }

    public void setGuia(GuiaHigieneSeguridadJpaEntity guia) {
        this.guia = guia;
    }

    public String getTipoDocumentoClave() {
        return tipoDocumentoClave;
    }

    public void setTipoDocumentoClave(String tipoDocumentoClave) {
        this.tipoDocumentoClave = tipoDocumentoClave;
    }

    public String getTipoDocumentoNombre() {
        return tipoDocumentoNombre;
    }

    public void setTipoDocumentoNombre(String tipoDocumentoNombre) {
        this.tipoDocumentoNombre = tipoDocumentoNombre;
    }

    public String getOrganismoEmisor() {
        return organismoEmisor;
    }

    public void setOrganismoEmisor(String organismoEmisor) {
        this.organismoEmisor = organismoEmisor;
    }

    public String getNumeroNormativa() {
        return numeroNormativa;
    }

    public void setNumeroNormativa(String numeroNormativa) {
        this.numeroNormativa = numeroNormativa;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUrlMinio() {
        return urlMinio;
    }

    public void setUrlMinio(String urlMinio) {
        this.urlMinio = urlMinio;
    }

    public String getRutaRepositorio() {
        return rutaRepositorio;
    }

    public void setRutaRepositorio(String rutaRepositorio) {
        this.rutaRepositorio = rutaRepositorio;
    }

    public Boolean getObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(Boolean obligatorio) {
        this.obligatorio = obligatorio;
    }

    public Boolean getCargado() {
        return cargado;
    }

    public void setCargado(Boolean cargado) {
        this.cargado = cargado;
    }

    public UUID getSubidoPorUsuarioId() {
        return subidoPorUsuarioId;
    }

    public void setSubidoPorUsuarioId(UUID subidoPorUsuarioId) {
        this.subidoPorUsuarioId = subidoPorUsuarioId;
    }

    public Instant getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(Instant fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
