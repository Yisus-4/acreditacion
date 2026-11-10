package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "programa_analitico_asignatura")
public class ProgramaAnaliticoAsignaturaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_curricular_id")
    private ActividadCurricularPlanJpaEntity actividadCurricular;

    @Column(name = "nombre_asignatura", nullable = false, length = 255)
    private String nombreAsignatura;

    @Column(name = "anio_cursada", length = 50)
    private String anioCursada;

    @Column(name = "resolucion_aprobacion", length = 150)
    private String resolucionAprobacion;

    @Column(name = "vigencia_desde")
    private Integer vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private Integer vigenciaHasta;

    @Column(name = "archivo_id")
    private UUID archivoId;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Column(name = "archivo_ruta", length = 500)
    private String archivoRuta;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado = "PENDIENTE"; // 'CARGADO', 'PENDIENTE', 'APROBADO'

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ProgramaAnaliticoAsignaturaJpaEntity() {
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

    public ActividadCurricularPlanJpaEntity getActividadCurricular() {
        return actividadCurricular;
    }

    public void setActividadCurricular(ActividadCurricularPlanJpaEntity actividadCurricular) {
        this.actividadCurricular = actividadCurricular;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public void setNombreAsignatura(String nombreAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
    }

    public String getAnioCursada() {
        return anioCursada;
    }

    public void setAnioCursada(String anioCursada) {
        this.anioCursada = anioCursada;
    }

    public String getResolucionAprobacion() {
        return resolucionAprobacion;
    }

    public void setResolucionAprobacion(String resolucionAprobacion) {
        this.resolucionAprobacion = resolucionAprobacion;
    }

    public Integer getVigenciaDesde() {
        return vigenciaDesde;
    }

    public void setVigenciaDesde(Integer vigenciaDesde) {
        this.vigenciaDesde = vigenciaDesde;
    }

    public Integer getVigenciaHasta() {
        return vigenciaHasta;
    }

    public void setVigenciaHasta(Integer vigenciaHasta) {
        this.vigenciaHasta = vigenciaHasta;
    }

    public UUID getArchivoId() {
        return archivoId;
    }

    public void setArchivoId(UUID archivoId) {
        this.archivoId = archivoId;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getArchivoRuta() {
        return archivoRuta;
    }

    public void setArchivoRuta(String archivoRuta) {
        this.archivoRuta = archivoRuta;
    }

    public Long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(Long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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
        ProgramaAnaliticoAsignaturaJpaEntity that = (ProgramaAnaliticoAsignaturaJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
