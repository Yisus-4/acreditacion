package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actividad_curricular_plan")
public class ActividadCurricularPlanJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudioFt001JpaEntity planEstudio;

    @Column(name = "numero_orden", nullable = false)
    private Integer numeroOrden;

    @Column(name = "codigo", length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "grupo", length = 100)
    private String grupo; // 'C.B.I.', 'T.B.', 'T.A.', 'C.T.C.'

    @Column(name = "subgrupo", length = 100)
    private String subgrupo; // '1º', '2º', '3º', '4º', '5º'

    @Column(name = "horas_reloj")
    private Integer horasReloj;

    @Column(name = "horas_presenciales")
    private Integer horasPresenciales;

    @Column(name = "horas_no_presenciales")
    private Integer horasNoPresenciales;

    @Column(name = "correlativas", columnDefinition = "TEXT")
    private String correlativas;

    @Column(name = "es_obligatoria", nullable = false)
    private Boolean esObligatoria = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_dictado", nullable = false, length = 20)
    private EstadoDictadoActividad estadoDictado = EstadoDictadoActividad.SE_DICTA;

    @Column(name = "origen_maestro_id")
    private UUID origenMaestroId;

    @Column(name = "docente_responsable_id")
    private UUID docenteResponsableId;

    @Column(name = "docente_responsable_nombre", length = 200)
    private String docenteResponsableNombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public ActividadCurricularPlanJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PlanEstudioFt001JpaEntity getPlanEstudio() {
        return planEstudio;
    }

    public void setPlanEstudio(PlanEstudioFt001JpaEntity planEstudio) {
        this.planEstudio = planEstudio;
    }

    public Integer getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(Integer numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getSubgrupo() {
        return subgrupo;
    }

    public void setSubgrupo(String subgrupo) {
        this.subgrupo = subgrupo;
    }

    public Integer getHorasReloj() {
        return horasReloj;
    }

    public void setHorasReloj(Integer horasReloj) {
        this.horasReloj = horasReloj;
    }

    public Integer getHorasPresenciales() {
        return horasPresenciales;
    }

    public void setHorasPresenciales(Integer horasPresenciales) {
        this.horasPresenciales = horasPresenciales;
    }

    public Integer getHorasNoPresenciales() {
        return horasNoPresenciales;
    }

    public void setHorasNoPresenciales(Integer horasNoPresenciales) {
        this.horasNoPresenciales = horasNoPresenciales;
    }

    public String getCorrelativas() {
        return correlativas;
    }

    public void setCorrelativas(String correlativas) {
        this.correlativas = correlativas;
    }

    public Boolean getEsObligatoria() {
        return esObligatoria;
    }

    public void setEsObligatoria(Boolean esObligatoria) {
        this.esObligatoria = esObligatoria;
    }

    public UUID getOrigenMaestroId() {
        return origenMaestroId;
    }

    public EstadoDictadoActividad getEstadoDictado() {
        return estadoDictado != null ? estadoDictado : EstadoDictadoActividad.SE_DICTA;
    }

    public void setEstadoDictado(EstadoDictadoActividad estadoDictado) {
        this.estadoDictado = estadoDictado != null ? estadoDictado : EstadoDictadoActividad.SE_DICTA;
    }

    public void setOrigenMaestroId(UUID origenMaestroId) {
        this.origenMaestroId = origenMaestroId;
    }

    public UUID getDocenteResponsableId() {
        return docenteResponsableId;
    }

    public void setDocenteResponsableId(UUID docenteResponsableId) {
        this.docenteResponsableId = docenteResponsableId;
    }

    public String getDocenteResponsableNombre() {
        return docenteResponsableNombre;
    }

    public void setDocenteResponsableNombre(String docenteResponsableNombre) {
        this.docenteResponsableNombre = docenteResponsableNombre;
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
        ActividadCurricularPlanJpaEntity that = (ActividadCurricularPlanJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
