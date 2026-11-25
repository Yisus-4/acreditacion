package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actividad_curricular_evaluacion_22")
public class ActividadCurricularEvaluacion22JpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estandar_item_id", nullable = false)
    private EstandarItem22JpaEntity estandarItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "escala_evaluacion_id", nullable = false)
    private EstandarEscalaEvaluacionJpaEntity escalaEvaluacion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public ActividadCurricularEvaluacion22JpaEntity() {
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

    public EstandarItem22JpaEntity getEstandarItem() {
        return estandarItem;
    }

    public void setEstandarItem(EstandarItem22JpaEntity estandarItem) {
        this.estandarItem = estandarItem;
    }

    public EstandarEscalaEvaluacionJpaEntity getEscalaEvaluacion() {
        return escalaEvaluacion;
    }

    public void setEscalaEvaluacion(EstandarEscalaEvaluacionJpaEntity escalaEvaluacion) {
        this.escalaEvaluacion = escalaEvaluacion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActividadCurricularEvaluacion22JpaEntity that = (ActividadCurricularEvaluacion22JpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
