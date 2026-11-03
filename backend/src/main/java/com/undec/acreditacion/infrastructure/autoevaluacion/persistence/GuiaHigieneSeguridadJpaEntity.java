package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guia_higiene_seguridad")
public class GuiaHigieneSeguridadJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_id", nullable = false)
    private InstitucionJpaEntity institucion;

    @Column(name = "es_maestro", nullable = false)
    private Boolean esMaestro = true;

    @Column(name = "edicion", nullable = false, length = 50)
    private String edicion = "01";

    @Column(name = "fecha_vigencia")
    private LocalDate fechaVigencia;

    @Column(name = "elaborado_por")
    private String elaboradoPor;

    @Column(name = "revisado_por")
    private String revisadoPor;

    @Column(name = "aprobado_por")
    private String aprobadoPor;

    @Column(name = "instancia_responsable_ua", columnDefinition = "TEXT")
    private String instanciaResponsableUa;

    @Column(name = "politica_unidades_apoyo", columnDefinition = "TEXT")
    private String politicaUnidadesApoyo;

    @Column(name = "aplica_unidades_apoyo", nullable = false)
    private Boolean aplicaUnidadesApoyo = false;

    @Column(name = "verificado", nullable = false)
    private Boolean verificado = false;

    @Column(name = "fecha_verificacion")
    private Instant fechaVerificacion;

    @Column(name = "fecha_solicitud")
    private Instant fechaSolicitud;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado = "PENDIENTE";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @OneToMany(mappedBy = "guia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GuiaHigieneDocumentoJpaEntity> documentos = new ArrayList<>();

    public GuiaHigieneSeguridadJpaEntity() {
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

    public InstitucionJpaEntity getInstitucion() {
        return institucion;
    }

    public void setInstitucion(InstitucionJpaEntity institucion) {
        this.institucion = institucion;
    }

    public Boolean getEsMaestro() {
        return esMaestro;
    }

    public void setEsMaestro(Boolean esMaestro) {
        this.esMaestro = esMaestro;
    }

    public String getEdicion() {
        return edicion;
    }

    public void setEdicion(String edicion) {
        this.edicion = edicion;
    }

    public LocalDate getFechaVigencia() {
        return fechaVigencia;
    }

    public void setFechaVigencia(LocalDate fechaVigencia) {
        this.fechaVigencia = fechaVigencia;
    }

    public String getElaboradoPor() {
        return elaboradoPor;
    }

    public void setElaboradoPor(String elaboradoPor) {
        this.elaboradoPor = elaboradoPor;
    }

    public String getRevisadoPor() {
        return revisadoPor;
    }

    public void setRevisadoPor(String revisadoPor) {
        this.revisadoPor = revisadoPor;
    }

    public String getAprobadoPor() {
        return aprobadoPor;
    }

    public void setAprobadoPor(String aprobadoPor) {
        this.aprobadoPor = aprobadoPor;
    }

    public String getInstanciaResponsableUa() {
        return instanciaResponsableUa;
    }

    public void setInstanciaResponsableUa(String instanciaResponsableUa) {
        this.instanciaResponsableUa = instanciaResponsableUa;
    }

    public String getPoliticaUnidadesApoyo() {
        return politicaUnidadesApoyo;
    }

    public void setPoliticaUnidadesApoyo(String politicaUnidadesApoyo) {
        this.politicaUnidadesApoyo = politicaUnidadesApoyo;
    }

    public Boolean getAplicaUnidadesApoyo() {
        return aplicaUnidadesApoyo;
    }

    public void setAplicaUnidadesApoyo(Boolean aplicaUnidadesApoyo) {
        this.aplicaUnidadesApoyo = aplicaUnidadesApoyo;
    }

    public Boolean getVerificado() {
        return verificado;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
    }

    public Instant getFechaVerificacion() {
        return fechaVerificacion;
    }

    public void setFechaVerificacion(Instant fechaVerificacion) {
        this.fechaVerificacion = fechaVerificacion;
    }

    public Instant getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Instant fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
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

    public List<GuiaHigieneDocumentoJpaEntity> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<GuiaHigieneDocumentoJpaEntity> documentos) {
        this.documentos = documentos;
    }
}
