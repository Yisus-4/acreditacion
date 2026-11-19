package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "autoevaluacion_ambito_actividad")
public class AutoevaluacionAmbitoActividadJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autoevaluacion_ambito_id", nullable = false)
    private AutoevaluacionCarreraAmbitoJpaEntity autoevaluacionAmbito;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public AutoevaluacionAmbitoActividadJpaEntity() {
    }

    public AutoevaluacionAmbitoActividadJpaEntity(
            UUID id,
            AutoevaluacionCarreraAmbitoJpaEntity autoevaluacionAmbito,
            ActividadCurricularPlanJpaEntity actividadCurricular
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.autoevaluacionAmbito = autoevaluacionAmbito;
        this.actividadCurricular = actividadCurricular;
        this.creadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AutoevaluacionCarreraAmbitoJpaEntity getAutoevaluacionAmbito() {
        return autoevaluacionAmbito;
    }

    public void setAutoevaluacionAmbito(AutoevaluacionCarreraAmbitoJpaEntity autoevaluacionAmbito) {
        this.autoevaluacionAmbito = autoevaluacionAmbito;
    }

    public ActividadCurricularPlanJpaEntity getActividadCurricular() {
        return actividadCurricular;
    }

    public void setActividadCurricular(ActividadCurricularPlanJpaEntity actividadCurricular) {
        this.actividadCurricular = actividadCurricular;
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
        AutoevaluacionAmbitoActividadJpaEntity that = (AutoevaluacionAmbitoActividadJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
