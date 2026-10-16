package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actividad_plan_trabajo")
public class ActividadPlanTrabajoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "plan_trabajo_id", nullable = false)
    private UUID planTrabajoId;

    @Column(name = "numero_orden", nullable = false)
    private int numeroOrden;

    @Column(name = "codigo_actividad", nullable = false, length = 50)
    private String codigoActividad;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "fecha_reunion_coordinacion")
    private Instant fechaReunionCoordinacion;

    @Column(name = "sintesis_acuerdos", columnDefinition = "TEXT")
    private String sintesisAcuerdos;

    @Column(name = "completada_en")
    private Instant completadaEn;

    @Column(name = "completada_por")
    private UUID completadaPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ActividadPlanTrabajoJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlanTrabajoId() {
        return planTrabajoId;
    }

    public void setPlanTrabajoId(UUID planTrabajoId) {
        this.planTrabajoId = planTrabajoId;
    }

    public int getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(int numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getCodigoActividad() {
        return codigoActividad;
    }

    public void setCodigoActividad(String codigoActividad) {
        this.codigoActividad = codigoActividad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getFechaReunionCoordinacion() {
        return fechaReunionCoordinacion;
    }

    public void setFechaReunionCoordinacion(Instant fechaReunionCoordinacion) {
        this.fechaReunionCoordinacion = fechaReunionCoordinacion;
    }

    public String getSintesisAcuerdos() {
        return sintesisAcuerdos;
    }

    public void setSintesisAcuerdos(String sintesisAcuerdos) {
        this.sintesisAcuerdos = sintesisAcuerdos;
    }

    public Instant getCompletadaEn() {
        return completadaEn;
    }

    public void setCompletadaEn(Instant completadaEn) {
        this.completadaEn = completadaEn;
    }

    public UUID getCompletadaPor() {
        return completadaPor;
    }

    public void setCompletadaPor(UUID completadaPor) {
        this.completadaPor = completadaPor;
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
        ActividadPlanTrabajoJpaEntity that = (ActividadPlanTrabajoJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
