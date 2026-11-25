package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
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
