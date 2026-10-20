package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "solicitud_normativa_carrera")
public class SolicitudNormativaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @Column(name = "actividad_id", nullable = false, unique = true)
    private UUID actividadId;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "fecha_solicitud")
    private Instant fechaSolicitud;

    @Column(name = "solicitado_por")
    private UUID solicitadoPor;

    @Column(name = "fecha_limite_entrega")
    private LocalDate fechaLimiteEntrega;

    @Column(name = "dias_habiles_plazo", nullable = false)
    private int diasHabilesPlazo;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    @Column(name = "enviado_por")
    private UUID enviadoPor;

    @Column(name = "cumplimiento_plazo", length = 30)
    private String cumplimientoPlazo;

    @Column(name = "observaciones_solicitud", columnDefinition = "TEXT")
    private String observacionesSolicitud;

    @Column(name = "observaciones_revision", columnDefinition = "TEXT")
    private String observacionesRevision;

    @Column(name = "fecha_revision")
    private Instant fechaRevision;

    @Column(name = "revisado_por")
    private UUID revisadoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public SolicitudNormativaJpaEntity() {
    }

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

    public UUID getActividadId() {
        return actividadId;
    }

    public void setActividadId(UUID actividadId) {
        this.actividadId = actividadId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Instant fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(UUID solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public LocalDate getFechaLimiteEntrega() {
        return fechaLimiteEntrega;
    }

    public void setFechaLimiteEntrega(LocalDate fechaLimiteEntrega) {
        this.fechaLimiteEntrega = fechaLimiteEntrega;
    }

    public int getDiasHabilesPlazo() {
        return diasHabilesPlazo;
    }

    public void setDiasHabilesPlazo(int diasHabilesPlazo) {
        this.diasHabilesPlazo = diasHabilesPlazo;
    }

    public Instant getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Instant fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public UUID getEnviadoPor() {
        return enviadoPor;
    }

    public void setEnviadoPor(UUID enviadoPor) {
        this.enviadoPor = enviadoPor;
    }

    public String getCumplimientoPlazo() {
        return cumplimientoPlazo;
    }

    public void setCumplimientoPlazo(String cumplimientoPlazo) {
        this.cumplimientoPlazo = cumplimientoPlazo;
    }

    public String getObservacionesSolicitud() {
        return observacionesSolicitud;
    }

    public void setObservacionesSolicitud(String observacionesSolicitud) {
        this.observacionesSolicitud = observacionesSolicitud;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public void setObservacionesRevision(String observacionesRevision) {
        this.observacionesRevision = observacionesRevision;
    }

    public Instant getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(Instant fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public UUID getRevisadoPor() {
        return revisadoPor;
    }

    public void setRevisadoPor(UUID revisadoPor) {
        this.revisadoPor = revisadoPor;
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
