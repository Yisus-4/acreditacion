package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "registro_convocatoria")
public class RegistroConvocatoriaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false, unique = true)
    private UUID procesoId;

    @Column(name = "nro_resolucion_coneau", nullable = false, length = 100)
    private String nroResolucionConeau;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "rm_estandares", length = 100)
    private String rmEstandares;

    @Column(name = "fase_evaluacion", length = 50)
    private String faseEvaluacion;

    @Column(name = "fecha_limite_presentacion_coneau")
    private LocalDate fechaLimitePresentacionConeau;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public RegistroConvocatoriaJpaEntity() {
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

    public String getNroResolucionConeau() {
        return nroResolucionConeau;
    }

    public void setNroResolucionConeau(String nroResolucionConeau) {
        this.nroResolucionConeau = nroResolucionConeau;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getRmEstandares() {
        return rmEstandares;
    }

    public void setRmEstandares(String rmEstandares) {
        this.rmEstandares = rmEstandares;
    }

    public String getFaseEvaluacion() {
        return faseEvaluacion;
    }

    public void setFaseEvaluacion(String faseEvaluacion) {
        this.faseEvaluacion = faseEvaluacion;
    }

    public LocalDate getFechaLimitePresentacionConeau() {
        return fechaLimitePresentacionConeau;
    }

    public void setFechaLimitePresentacionConeau(LocalDate fechaLimitePresentacionConeau) {
        this.fechaLimitePresentacionConeau = fechaLimitePresentacionConeau;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistroConvocatoriaJpaEntity that = (RegistroConvocatoriaJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
