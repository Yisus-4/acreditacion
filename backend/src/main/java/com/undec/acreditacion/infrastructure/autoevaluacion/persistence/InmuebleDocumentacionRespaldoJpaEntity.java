package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inmueble_documentacion_respaldo")
public class InmuebleDocumentacionRespaldoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private InmuebleJpaEntity inmueble;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "url_minio", nullable = false, length = 500)
    private String urlMinio;

    @Column(name = "ruta_repositorio", nullable = false, length = 500)
    private String rutaRepositorio = "03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa";

    @Column(name = "tipo_anexo", nullable = false, length = 100)
    private String tipoAnexo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subido_por_usuario_id")
    private UserJpaEntity subidoPorUsuario;

    @Column(name = "fecha_subida", nullable = false)
    private Instant fechaSubida = Instant.now();

    public InmuebleDocumentacionRespaldoJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InmuebleJpaEntity getInmueble() {
        return inmueble;
    }

    public void setInmueble(InmuebleJpaEntity inmueble) {
        this.inmueble = inmueble;
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

    public String getTipoAnexo() {
        return tipoAnexo;
    }

    public void setTipoAnexo(String tipoAnexo) {
        this.tipoAnexo = tipoAnexo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public UserJpaEntity getSubidoPorUsuario() {
        return subidoPorUsuario;
    }

    public void setSubidoPorUsuario(UserJpaEntity subidoPorUsuario) {
        this.subidoPorUsuario = subidoPorUsuario;
    }

    public Instant getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(Instant fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}
