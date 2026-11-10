package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "autoevaluacion_carrera_ambito", uniqueConstraints = {
        @UniqueConstraint(name = "uq_proceso_ambito", columnNames = {"proceso_id", "ambito_id"})
})
public class AutoevaluacionCarreraAmbitoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ambito_id", nullable = false)
    private AmbitoPracticaJpaEntity ambito;

    @Column(name = "utilizado_por_carrera", nullable = false)
    private Boolean utilizadoPorCarrera = true;

    @Column(name = "asignaturas_vinculadas", columnDefinition = "TEXT")
    private String asignaturasVinculadas;

    @Column(name = "responsable_practica", length = 500)
    private String responsablePractica;

    @Column(name = "horarios_uso_anio_anterior", length = 100)
    private String horariosUsoAnioAnterior;

    @Column(name = "tipo_practica_estudiantes", columnDefinition = "TEXT")
    private String tipoPracticaEstudiantes;

    @Column(name = "observaciones_carrera", columnDefinition = "TEXT")
    private String observacionesCarrera;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public AutoevaluacionCarreraAmbitoJpaEntity() {
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

    public AmbitoPracticaJpaEntity getAmbito() {
        return ambito;
    }

    public void setAmbito(AmbitoPracticaJpaEntity ambito) {
        this.ambito = ambito;
    }

    public Boolean getUtilizadoPorCarrera() {
        return utilizadoPorCarrera;
    }

    public void setUtilizadoPorCarrera(Boolean utilizadoPorCarrera) {
        this.utilizadoPorCarrera = utilizadoPorCarrera;
    }

    public String getAsignaturasVinculadas() {
        return asignaturasVinculadas;
    }

    public void setAsignaturasVinculadas(String asignaturasVinculadas) {
        this.asignaturasVinculadas = asignaturasVinculadas;
    }

    public String getResponsablePractica() {
        return responsablePractica;
    }

    public void setResponsablePractica(String responsablePractica) {
        this.responsablePractica = responsablePractica;
    }

    public String getHorariosUsoAnioAnterior() {
        return horariosUsoAnioAnterior;
    }

    public void setHorariosUsoAnioAnterior(String horariosUsoAnioAnterior) {
        this.horariosUsoAnioAnterior = horariosUsoAnioAnterior;
    }

    public String getTipoPracticaEstudiantes() {
        return tipoPracticaEstudiantes;
    }

    public void setTipoPracticaEstudiantes(String tipoPracticaEstudiantes) {
        this.tipoPracticaEstudiantes = tipoPracticaEstudiantes;
    }

    public String getObservacionesCarrera() {
        return observacionesCarrera;
    }

    public void setObservacionesCarrera(String observacionesCarrera) {
        this.observacionesCarrera = observacionesCarrera;
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
        AutoevaluacionCarreraAmbitoJpaEntity that = (AutoevaluacionCarreraAmbitoJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
