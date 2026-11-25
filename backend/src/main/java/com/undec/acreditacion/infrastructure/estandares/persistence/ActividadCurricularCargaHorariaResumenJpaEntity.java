package com.undec.acreditacion.infrastructure.estandares.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actividad_curricular_carga_horaria_resumen")
public class ActividadCurricularCargaHorariaResumenJpaEntity {

    @Id
    @Column(name = "actividad_curricular_id", nullable = false)
    private UUID actividadCurricularId;

    @Column(name = "ambitos_formacion_practica", columnDefinition = "TEXT")
    private String ambitosFormacionPractica;

    @Column(name = "horas_semanal_total_presencial", nullable = false)
    private Integer horasSemanalTotalPresencial = 0;

    @Column(name = "horas_semanal_total_distancia", nullable = false)
    private Integer horasSemanalTotalDistancia = 0;

    @Column(name = "horas_semanal_practica_presencial", nullable = false)
    private Integer horasSemanalPracticaPresencial = 0;

    @Column(name = "horas_semanal_practica_distancia", nullable = false)
    private Integer horasSemanalPracticaDistancia = 0;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public ActividadCurricularCargaHorariaResumenJpaEntity() {
    }

    public UUID getActividadCurricularId() {
        return actividadCurricularId;
    }

    public void setActividadCurricularId(UUID actividadCurricularId) {
        this.actividadCurricularId = actividadCurricularId;
    }

    public String getAmbitosFormacionPractica() {
        return ambitosFormacionPractica;
    }

    public void setAmbitosFormacionPractica(String ambitosFormacionPractica) {
        this.ambitosFormacionPractica = ambitosFormacionPractica;
    }

    public Integer getHorasSemanalTotalPresencial() {
        return horasSemanalTotalPresencial;
    }

    public void setHorasSemanalTotalPresencial(Integer horasSemanalTotalPresencial) {
        this.horasSemanalTotalPresencial = horasSemanalTotalPresencial;
    }

    public Integer getHorasSemanalTotalDistancia() {
        return horasSemanalTotalDistancia;
    }

    public void setHorasSemanalTotalDistancia(Integer horasSemanalTotalDistancia) {
        this.horasSemanalTotalDistancia = horasSemanalTotalDistancia;
    }

    public Integer getHorasSemanalPracticaPresencial() {
        return horasSemanalPracticaPresencial;
    }

    public void setHorasSemanalPracticaPresencial(Integer horasSemanalPracticaPresencial) {
        this.horasSemanalPracticaPresencial = horasSemanalPracticaPresencial;
    }

    public Integer getHorasSemanalPracticaDistancia() {
        return horasSemanalPracticaDistancia;
    }

    public void setHorasSemanalPracticaDistancia(Integer horasSemanalPracticaDistancia) {
        this.horasSemanalPracticaDistancia = horasSemanalPracticaDistancia;
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
        ActividadCurricularCargaHorariaResumenJpaEntity that = (ActividadCurricularCargaHorariaResumenJpaEntity) o;
        return Objects.equals(actividadCurricularId, that.actividadCurricularId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actividadCurricularId);
    }
}
