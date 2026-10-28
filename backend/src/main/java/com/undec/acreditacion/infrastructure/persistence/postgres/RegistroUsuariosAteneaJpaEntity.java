package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.EstadoRegistroUsuariosAtenea;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "registro_usuarios_atenea")
public class RegistroUsuariosAteneaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false, unique = true)
    private UUID procesoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoRegistroUsuariosAtenea estado;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "generado_en")
    private Instant generadoEn;

    @Column(name = "aprobado_en")
    private Instant aprobadoEn;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "ruta_documento", length = 500)
    private String rutaDocumento;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "registro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UsuarioPermisoAteneaJpaEntity> usuarios = new ArrayList<>();

    public RegistroUsuariosAteneaJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public EstadoRegistroUsuariosAtenea getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistroUsuariosAtenea estado) {
        this.estado = estado;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public Instant getGeneradoEn() {
        return generadoEn;
    }

    public void setGeneradoEn(Instant generadoEn) {
        this.generadoEn = generadoEn;
    }

    public Instant getAprobadoEn() {
        return aprobadoEn;
    }

    public void setAprobadoEn(Instant aprobadoEn) {
        this.aprobadoEn = aprobadoEn;
    }

    public UUID getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(UUID creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getRutaDocumento() {
        return rutaDocumento;
    }

    public void setRutaDocumento(String rutaDocumento) {
        this.rutaDocumento = rutaDocumento;
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

    public List<UsuarioPermisoAteneaJpaEntity> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<UsuarioPermisoAteneaJpaEntity> usuarios) {
        this.usuarios = usuarios;
    }
}
