package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoActividadMaestroJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "proceso_cuerpo_academico_actividad")
public class ProcesoCuerpoAcademicoActividadJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private ProcesoCuerpoAcademicoJpaEntity docente;

    @Column(name = "carrera_id", nullable = false)
    private UUID carreraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudioFt001JpaEntity planEstudio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id", nullable = false)
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @Column(name = "rol_en_catedra", length = 50, nullable = false)
    private String rolEnCatedra = "PROFESOR_A_CARGO";

    @Column(name = "carga_horaria_semanal", nullable = false)
    private Integer cargaHorariaSemanal = 4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_maestro_id")
    private CuerpoAcademicoActividadMaestroJpaEntity origenMaestro;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public ProcesoCuerpoAcademicoActividadJpaEntity() {
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

    public ProcesoCuerpoAcademicoJpaEntity getDocente() {
        return docente;
    }

    public void setDocente(ProcesoCuerpoAcademicoJpaEntity docente) {
        this.docente = docente;
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

    public String getRolEnCatedra() {
        return rolEnCatedra;
    }

    public void setRolEnCatedra(String rolEnCatedra) {
        this.rolEnCatedra = rolEnCatedra;
    }

    public Integer getCargaHorariaSemanal() {
        return cargaHorariaSemanal;
    }

    public void setCargaHorariaSemanal(Integer cargaHorariaSemanal) {
        this.cargaHorariaSemanal = cargaHorariaSemanal;
    }

    public CuerpoAcademicoActividadMaestroJpaEntity getOrigenMaestro() {
        return origenMaestro;
    }

    public void setOrigenMaestro(CuerpoAcademicoActividadMaestroJpaEntity origenMaestro) {
        this.origenMaestro = origenMaestro;
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
        ProcesoCuerpoAcademicoActividadJpaEntity that = (ProcesoCuerpoAcademicoActividadJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
