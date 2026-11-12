package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "espacio_fisico_actividad_curricular")
public class EspacioFisicoActividadCurricularJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_fisico_id", nullable = false)
    private EspacioFisicoJpaEntity espacioFisico;

    @Column(name = "carrera_id", nullable = false)
    private UUID carreraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudioFt001JpaEntity planEstudio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @Column(name = "comisiones_asignadas", nullable = false)
    private Integer comisionesAsignadas = 1;

    @Column(name = "observaciones_uso", columnDefinition = "TEXT")
    private String observacionesUso;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public EspacioFisicoActividadCurricularJpaEntity() {
    }

    public EspacioFisicoActividadCurricularJpaEntity(UUID id, EspacioFisicoJpaEntity espacioFisico, UUID carreraId,
                                                    PlanEstudioFt001JpaEntity planEstudio,
                                                    ActividadCurricularPlanJpaEntity actividadCurricular,
                                                    Integer comisionesAsignadas, String observacionesUso) {
        this.id = id;
        this.espacioFisico = espacioFisico;
        this.carreraId = carreraId;
        this.planEstudio = planEstudio;
        this.actividadCurricular = actividadCurricular;
        this.comisionesAsignadas = comisionesAsignadas != null ? comisionesAsignadas : 1;
        this.observacionesUso = observacionesUso;
        this.creadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EspacioFisicoJpaEntity getEspacioFisico() {
        return espacioFisico;
    }

    public void setEspacioFisico(EspacioFisicoJpaEntity espacioFisico) {
        this.espacioFisico = espacioFisico;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public void setCarreraId(UUID carreraId) {
        this.carreraId = carreraId;
    }

    public PlanEstudioFt001JpaEntity getPlanEstudio() {
        return planEstudio;
    }

    public void setPlanEstudio(PlanEstudioFt001JpaEntity planEstudio) {
        this.planEstudio = planEstudio;
    }

    public ActividadCurricularPlanJpaEntity getActividadCurricular() {
        return actividadCurricular;
    }

    public void setActividadCurricular(ActividadCurricularPlanJpaEntity actividadCurricular) {
        this.actividadCurricular = actividadCurricular;
    }

    public Integer getComisionesAsignadas() {
        return comisionesAsignadas;
    }

    public void setComisionesAsignadas(Integer comisionesAsignadas) {
        this.comisionesAsignadas = comisionesAsignadas;
    }

    public String getObservacionesUso() {
        return observacionesUso;
    }

    public void setObservacionesUso(String observacionesUso) {
        this.observacionesUso = observacionesUso;
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
        EspacioFisicoActividadCurricularJpaEntity that = (EspacioFisicoActividadCurricularJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
