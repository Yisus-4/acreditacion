package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "convocatoria")
public class ConvocatoriaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "unidad_academica_id", nullable = false)
    private UUID unidadAcademicaId;

    @Column(nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "tipo_proceso", nullable = false, length = 50)
    private String tipoProceso;

    @Column(name = "nro_resolucion_coneau", length = 100)
    private String nroResolucionConeau;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Column(nullable = false, length = 50)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_por", nullable = false)
    private UUID creadoPor;

    @Column(name = "confirmado_por_de")
    private UUID confirmadoPorDe;

    @Column(name = "confirmado_por_de_en")
    private Instant confirmadoPorDeEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "convocatoria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ConvocatoriaCarreraJpaEntity> carreras = new ArrayList<>();

    public ConvocatoriaJpaEntity() {
    }

    public ConvocatoriaJpaEntity(UUID id, UUID unidadAcademicaId, String codigo, String titulo,
                                 String tipoProceso, String nroResolucionConeau, LocalDate fechaInicio,
                                 LocalDate fechaCierre, String estado, String observaciones,
                                 UUID creadoPor, UUID confirmadoPorDe, Instant confirmadoPorDeEn,
                                 Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.unidadAcademicaId = unidadAcademicaId;
        this.codigo = codigo;
        this.titulo = titulo;
        this.tipoProceso = tipoProceso;
        this.nroResolucionConeau = nroResolucionConeau;
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
        this.estado = estado;
        this.observaciones = observaciones;
        this.creadoPor = creadoPor;
        this.confirmadoPorDe = confirmadoPorDe;
        this.confirmadoPorDeEn = confirmadoPorDeEn;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUnidadAcademicaId() {
        return unidadAcademicaId;
    }

    public void setUnidadAcademicaId(UUID unidadAcademicaId) {
        this.unidadAcademicaId = unidadAcademicaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipoProceso() {
        return tipoProceso;
    }

    public void setTipoProceso(String tipoProceso) {
        this.tipoProceso = tipoProceso;
    }

    public String getNroResolucionConeau() {
        return nroResolucionConeau;
    }

    public void setNroResolucionConeau(String nroResolucionConeau) {
        this.nroResolucionConeau = nroResolucionConeau;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
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

    public UUID getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(UUID creadoPor) {
        this.creadoPor = creadoPor;
    }

    public UUID getConfirmadoPorDe() {
        return confirmadoPorDe;
    }

    public void setConfirmadoPorDe(UUID confirmadoPorDe) {
        this.confirmadoPorDe = confirmadoPorDe;
    }

    public Instant getConfirmadoPorDeEn() {
        return confirmadoPorDeEn;
    }

    public void setConfirmadoPorDeEn(Instant confirmadoPorDeEn) {
        this.confirmadoPorDeEn = confirmadoPorDeEn;
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

    public List<ConvocatoriaCarreraJpaEntity> getCarreras() {
        return carreras;
    }

    public void setCarreras(List<ConvocatoriaCarreraJpaEntity> carreras) {
        this.carreras = carreras;
    }

    public void addCarrera(ConvocatoriaCarreraJpaEntity carrera) {
        this.carreras.add(carrera);
        carrera.setConvocatoria(this);
    }
}
