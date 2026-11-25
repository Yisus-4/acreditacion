package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.estandares.persistence.EstandarJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "plan_estudio_ft001")
public class PlanEstudioFt001JpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private CarreraJpaEntity carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estandar_id")
    private EstandarJpaEntity estandar;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @Column(name = "nombre_plan", length = 150, nullable = false)
    private String nombrePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_vigencia", length = 30, nullable = false)
    private EstadoVigenciaPlan estadoVigencia = EstadoVigenciaPlan.VIGENTE;

    @Column(name = "es_maestro", nullable = false)
    private Boolean esMaestro = true;

    @Column(name = "origen_maestro_id")
    private UUID origenMaestroId;

    @Column(name = "codigo_resolucion", length = 150)
    private String codigoResolucion;

    @Column(name = "anio_aprobacion")
    private Integer anioAprobacion;

    @Column(name = "anio_inicio")
    private Integer anioInicio;

    @Column(name = "duracion_anios")
    private Integer duracionAnios;

    @Column(name = "orientaciones_json", columnDefinition = "TEXT")
    private String orientacionesJson;

    @Column(name = "fundamentacion_modificaciones", columnDefinition = "TEXT")
    private String fundamentacionModificaciones;

    @Column(name = "estructura_sintetica", columnDefinition = "TEXT")
    private String estructuraSintetica;

    @Column(name = "horas_presenciales")
    private Integer horasPresenciales;

    @Column(name = "horas_no_presenciales")
    private Integer horasNoPresenciales;

    @Column(name = "horas_totales")
    private Integer horasTotales;

    @Column(name = "horas_optativas_minimas")
    private Integer horasOptativasMinimas;

    @Column(name = "actividades_comunes_ciclo", columnDefinition = "TEXT")
    private String actividadesComunesCiclo;

    @Column(name = "titulo_intermedio_denominacion", length = 200)
    private String tituloIntermedioDenominacion;

    @Column(name = "titulo_intermedio_duracion", length = 100)
    private String tituloIntermedioDuracion;

    @Column(name = "titulo_intermedio_actividades", columnDefinition = "TEXT")
    private String tituloIntermedioActividades;

    @Column(name = "instancia_final_graduacion", columnDefinition = "TEXT")
    private String instanciaFinalGraduacion;

    @Column(name = "otra_informacion", columnDefinition = "TEXT")
    private String otraInformacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "planEstudio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroOrden ASC")
    private List<ActividadCurricularPlanJpaEntity> asignaturas = new ArrayList<>();

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public PlanEstudioFt001JpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CarreraJpaEntity getCarrera() {
        return carrera;
    }

    public void setCarrera(CarreraJpaEntity carrera) {
        this.carrera = carrera;
    }

    public EstandarJpaEntity getEstandar() {
        return estandar;
    }

    public void setEstandar(EstandarJpaEntity estandar) {
        this.estandar = estandar;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public String getNombrePlan() {
        return nombrePlan;
    }

    public void setNombrePlan(String nombrePlan) {
        this.nombrePlan = nombrePlan;
    }

    public EstadoVigenciaPlan getEstadoVigencia() {
        return estadoVigencia;
    }

    public void setEstadoVigencia(EstadoVigenciaPlan estadoVigencia) {
        this.estadoVigencia = estadoVigencia;
    }

    public Boolean getEsMaestro() {
        return esMaestro;
    }

    public void setEsMaestro(Boolean esMaestro) {
        this.esMaestro = esMaestro;
    }

    public UUID getOrigenMaestroId() {
        return origenMaestroId;
    }

    public void setOrigenMaestroId(UUID origenMaestroId) {
        this.origenMaestroId = origenMaestroId;
    }

    public String getCodigoResolucion() {
        return codigoResolucion;
    }

    public void setCodigoResolucion(String codigoResolucion) {
        this.codigoResolucion = codigoResolucion;
    }

    public Integer getAnioAprobacion() {
        return anioAprobacion;
    }

    public void setAnioAprobacion(Integer anioAprobacion) {
        this.anioAprobacion = anioAprobacion;
    }

    public Integer getAnioInicio() {
        return anioInicio;
    }

    public void setAnioInicio(Integer anioInicio) {
        this.anioInicio = anioInicio;
    }

    public Integer getDuracionAnios() {
        return duracionAnios;
    }

    public void setDuracionAnios(Integer duracionAnios) {
        this.duracionAnios = duracionAnios;
    }

    public String getOrientacionesJson() {
        return orientacionesJson;
    }

    public void setOrientacionesJson(String orientacionesJson) {
        this.orientacionesJson = orientacionesJson;
    }

    public String getFundamentacionModificaciones() {
        return fundamentacionModificaciones;
    }

    public void setFundamentacionModificaciones(String fundamentacionModificaciones) {
        this.fundamentacionModificaciones = fundamentacionModificaciones;
    }

    public String getEstructuraSintetica() {
        return estructuraSintetica;
    }

    public void setEstructuraSintetica(String estructuraSintetica) {
        this.estructuraSintetica = estructuraSintetica;
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

    public Integer getHorasTotales() {
        return horasTotales;
    }

    public void setHorasTotales(Integer horasTotales) {
        this.horasTotales = horasTotales;
    }

    public Integer getHorasOptativasMinimas() {
        return horasOptativasMinimas;
    }

    public void setHorasOptativasMinimas(Integer horasOptativasMinimas) {
        this.horasOptativasMinimas = horasOptativasMinimas;
    }

    public String getActividadesComunesCiclo() {
        return actividadesComunesCiclo;
    }

    public void setActividadesComunesCiclo(String actividadesComunesCiclo) {
        this.actividadesComunesCiclo = actividadesComunesCiclo;
    }

    public String getTituloIntermedioDenominacion() {
        return tituloIntermedioDenominacion;
    }

    public void setTituloIntermedioDenominacion(String tituloIntermedioDenominacion) {
        this.tituloIntermedioDenominacion = tituloIntermedioDenominacion;
    }

    public String getTituloIntermedioDuracion() {
        return tituloIntermedioDuracion;
    }

    public void setTituloIntermedioDuracion(String tituloIntermedioDuracion) {
        this.tituloIntermedioDuracion = tituloIntermedioDuracion;
    }

    public String getTituloIntermedioActividades() {
        return tituloIntermedioActividades;
    }

    public void setTituloIntermedioActividades(String tituloIntermedioActividades) {
        this.tituloIntermedioActividades = tituloIntermedioActividades;
    }

    public String getInstanciaFinalGraduacion() {
        return instanciaFinalGraduacion;
    }

    public void setInstanciaFinalGraduacion(String instanciaFinalGraduacion) {
        this.instanciaFinalGraduacion = instanciaFinalGraduacion;
    }

    public String getOtraInformacion() {
        return otraInformacion;
    }

    public void setOtraInformacion(String otraInformacion) {
        this.otraInformacion = otraInformacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<ActividadCurricularPlanJpaEntity> getAsignaturas() {
        return asignaturas;
    }

    public void setAsignaturas(List<ActividadCurricularPlanJpaEntity> asignaturas) {
        this.asignaturas = asignaturas;
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
        PlanEstudioFt001JpaEntity that = (PlanEstudioFt001JpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
