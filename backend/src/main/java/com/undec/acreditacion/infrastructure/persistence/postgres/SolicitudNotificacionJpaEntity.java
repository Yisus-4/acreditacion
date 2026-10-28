package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitud_notificacion")
public class SolicitudNotificacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_payload", columnDefinition = "jsonb")
    private String datosPayload;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @Column(name = "carrera_nombre", length = 255)
    private String carreraNombre;

    @Column(name = "solicitado_por")
    private UUID solicitadoPor;

    @Column(name = "solicitado_por_nombre", length = 255)
    private String solicitadoPorNombre;

    @Column(name = "destinatario_rol", nullable = false, length = 50)
    private String destinatarioRol;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "resuelto_por")
    private UUID resueltoPor;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public SolicitudNotificacionJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getDatosPayload() {
        return datosPayload;
    }

    public void setDatosPayload(String datosPayload) {
        this.datosPayload = datosPayload;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public String getCarreraNombre() {
        return carreraNombre;
    }

    public void setCarreraNombre(String carreraNombre) {
        this.carreraNombre = carreraNombre;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(UUID solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public String getSolicitadoPorNombre() {
        return solicitadoPorNombre;
    }

    public void setSolicitadoPorNombre(String solicitadoPorNombre) {
        this.solicitadoPorNombre = solicitadoPorNombre;
    }

    public String getDestinatarioRol() {
        return destinatarioRol;
    }

    public void setDestinatarioRol(String destinatarioRol) {
        this.destinatarioRol = destinatarioRol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public UUID getResueltoPor() {
        return resueltoPor;
    }

    public void setResueltoPor(UUID resueltoPor) {
        this.resueltoPor = resueltoPor;
    }

    public Instant getResueltoEn() {
        return resueltoEn;
    }

    public void setResueltoEn(Instant resueltoEn) {
        this.resueltoEn = resueltoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
