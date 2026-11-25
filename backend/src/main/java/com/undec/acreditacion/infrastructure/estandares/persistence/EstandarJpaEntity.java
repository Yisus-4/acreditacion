package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "estandar")
public class EstandarJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private CarreraJpaEntity carrera;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "resolucion_ministerial", length = 180)
    private String resolucionMinisterial;

    @Column(name = "anio_vigencia")
    private Integer anioVigencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_vigencia", nullable = false, length = 30)
    private EstadoVigenciaEstandar estadoVigencia = EstadoVigenciaEstandar.VIGENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_seccion_22", nullable = false, length = 30)
    private TipoSeccionEstandar22 tipoSeccion22 = TipoSeccionEstandar22.COMPETENCIAS;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evaluacion_22", nullable = false, length = 30)
    private TipoEvaluacionEstandar22 tipoEvaluacion22 = TipoEvaluacionEstandar22.MATRIZ_MULTIPLE;

    @Column(name = "instrucciones_22", columnDefinition = "TEXT")
    private String instrucciones22;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "habilitar_311", nullable = false)
    private Boolean habilitar311 = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_311", nullable = false, length = 30)
    private ModoSeccion311 modo311 = ModoSeccion311.AREAS_TEMATICAS;

    @Column(name = "horas_minimas_total_311")
    private Integer horasMinimasTotal311;

    @Column(name = "habilitar_312", nullable = false)
    private Boolean habilitar312 = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_312", nullable = false, length = 30)
    private ModoSeccion312 modo312 = ModoSeccion312.AREAS_PRACTICAS;

    @Column(name = "horas_minimas_total_312")
    private Integer horasMinimasTotal312;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public EstandarJpaEntity() {
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getResolucionMinisterial() {
        return resolucionMinisterial;
    }

    public void setResolucionMinisterial(String resolucionMinisterial) {
        this.resolucionMinisterial = resolucionMinisterial;
    }

    public Integer getAnioVigencia() {
        return anioVigencia;
    }

    public void setAnioVigencia(Integer anioVigencia) {
        this.anioVigencia = anioVigencia;
    }

    public EstadoVigenciaEstandar getEstadoVigencia() {
        return estadoVigencia;
    }

    public void setEstadoVigencia(EstadoVigenciaEstandar estadoVigencia) {
        this.estadoVigencia = estadoVigencia;
    }

    public TipoSeccionEstandar22 getTipoSeccion22() {
        return tipoSeccion22;
    }

    public void setTipoSeccion22(TipoSeccionEstandar22 tipoSeccion22) {
        this.tipoSeccion22 = tipoSeccion22;
    }

    public TipoEvaluacionEstandar22 getTipoEvaluacion22() {
        return tipoEvaluacion22;
    }

    public void setTipoEvaluacion22(TipoEvaluacionEstandar22 tipoEvaluacion22) {
        this.tipoEvaluacion22 = tipoEvaluacion22;
    }

    public String getInstrucciones22() {
        return instrucciones22;
    }

    public void setInstrucciones22(String instrucciones22) {
        this.instrucciones22 = instrucciones22;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Boolean getHabilitar311() {
        return habilitar311;
    }

    public void setHabilitar311(Boolean habilitar311) {
        this.habilitar311 = habilitar311 != null ? habilitar311 : true;
    }

    public ModoSeccion311 getModo311() {
        return modo311;
    }

    public void setModo311(ModoSeccion311 modo311) {
        this.modo311 = modo311 != null ? modo311 : ModoSeccion311.AREAS_TEMATICAS;
    }

    public Integer getHorasMinimasTotal311() {
        return horasMinimasTotal311;
    }

    public void setHorasMinimasTotal311(Integer horasMinimasTotal311) {
        this.horasMinimasTotal311 = horasMinimasTotal311;
    }

    public Boolean getHabilitar312() {
        return habilitar312;
    }

    public void setHabilitar312(Boolean habilitar312) {
        this.habilitar312 = habilitar312 != null ? habilitar312 : true;
    }

    public ModoSeccion312 getModo312() {
        return modo312;
    }

    public void setModo312(ModoSeccion312 modo312) {
        this.modo312 = modo312 != null ? modo312 : ModoSeccion312.AREAS_PRACTICAS;
    }

    public Integer getHorasMinimasTotal312() {
        return horasMinimasTotal312;
    }

    public void setHorasMinimasTotal312(Integer horasMinimasTotal312) {
        this.horasMinimasTotal312 = horasMinimasTotal312;
    }

    public UUID getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(UUID creadoPor) {
        this.creadoPor = creadoPor;
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
        EstandarJpaEntity that = (EstandarJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
