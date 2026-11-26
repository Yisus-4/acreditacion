package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.ModalidadParticipacionDocente;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitud_asignacion_curricular")
public class SolicitudAsignacionCurricularJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designacion_id", nullable = false)
    private SolicitudDocenteDesignacionJpaEntity designacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private CarreraJpaEntity carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudioFt001JpaEntity planEstudio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_participacion", nullable = false, length = 30)
    private ModalidadParticipacionDocente modalidadParticipacion = ModalidadParticipacionDocente.DESARROLLO_CLASES;

    @Column(name = "horas_asignadas")
    private Integer horasAsignadas;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public SolicitudAsignacionCurricularJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SolicitudDocenteDesignacionJpaEntity getDesignacion() { return designacion; }
    public void setDesignacion(SolicitudDocenteDesignacionJpaEntity designacion) { this.designacion = designacion; }

    public CarreraJpaEntity getCarrera() { return carrera; }
    public void setCarrera(CarreraJpaEntity carrera) { this.carrera = carrera; }

    public PlanEstudioFt001JpaEntity getPlanEstudio() { return planEstudio; }
    public void setPlanEstudio(PlanEstudioFt001JpaEntity planEstudio) { this.planEstudio = planEstudio; }

    public ActividadCurricularPlanJpaEntity getActividadCurricular() { return actividadCurricular; }
    public void setActividadCurricular(ActividadCurricularPlanJpaEntity actividadCurricular) { this.actividadCurricular = actividadCurricular; }

    public ModalidadParticipacionDocente getModalidadParticipacion() { return modalidadParticipacion; }
    public void setModalidadParticipacion(ModalidadParticipacionDocente modalidadParticipacion) { this.modalidadParticipacion = modalidadParticipacion; }

    public Integer getHorasAsignadas() { return horasAsignadas; }
    public void setHorasAsignadas(Integer horasAsignadas) { this.horasAsignadas = horasAsignadas; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitudAsignacionCurricularJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
