package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actividad_curricular_carga_horaria_area")
public class ActividadCurricularCargaHorariaAreaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_carga_horaria_id", nullable = false)
    private EstandarAreaCargaHorariaJpaEntity areaCargaHoraria;

    @Column(name = "horas_total_presencial", nullable = false)
    private Integer horasTotalPresencial = 0;

    @Column(name = "horas_total_distancia", nullable = false)
    private Integer horasTotalDistancia = 0;

    @Column(name = "horas_practica_presencial", nullable = false)
    private Integer horasPracticaPresencial = 0;

    @Column(name = "horas_practica_distancia", nullable = false)
    private Integer horasPracticaDistancia = 0;

    public ActividadCurricularCargaHorariaAreaJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ActividadCurricularPlanJpaEntity getActividadCurricular() {
        return actividadCurricular;
    }

    public void setActividadCurricular(ActividadCurricularPlanJpaEntity actividadCurricular) {
        this.actividadCurricular = actividadCurricular;
    }

    public EstandarAreaCargaHorariaJpaEntity getAreaCargaHoraria() {
        return areaCargaHoraria;
    }

    public void setAreaCargaHoraria(EstandarAreaCargaHorariaJpaEntity areaCargaHoraria) {
        this.areaCargaHoraria = areaCargaHoraria;
    }

    public Integer getHorasTotalPresencial() {
        return horasTotalPresencial;
    }

    public void setHorasTotalPresencial(Integer horasTotalPresencial) {
        this.horasTotalPresencial = horasTotalPresencial;
    }

    public Integer getHorasTotalDistancia() {
        return horasTotalDistancia;
    }

    public void setHorasTotalDistancia(Integer horasTotalDistancia) {
        this.horasTotalDistancia = horasTotalDistancia;
    }

    public Integer getHorasPracticaPresencial() {
        return horasPracticaPresencial;
    }

    public void setHorasPracticaPresencial(Integer horasPracticaPresencial) {
        this.horasPracticaPresencial = horasPracticaPresencial;
    }

    public Integer getHorasPracticaDistancia() {
        return horasPracticaDistancia;
    }

    public void setHorasPracticaDistancia(Integer horasPracticaDistancia) {
        this.horasPracticaDistancia = horasPracticaDistancia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActividadCurricularCargaHorariaAreaJpaEntity that = (ActividadCurricularCargaHorariaAreaJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
