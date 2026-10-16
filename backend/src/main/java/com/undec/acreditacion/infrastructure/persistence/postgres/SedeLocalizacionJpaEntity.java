package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sede_localizacion")
public class SedeLocalizacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_id", nullable = false)
    private InstitucionJpaEntity institucion;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "calle", nullable = false)
    private String calle;

    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "piso", length = 20)
    private String piso;

    @Column(name = "codigo_postal", nullable = false, length = 20)
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id", nullable = false)
    private ProvinciaJpaEntity provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private DepartamentoJpaEntity departamentoGeografico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localidad_id", nullable = false)
    private LocalidadJpaEntity localidad;

    @Column(name = "es_propia", nullable = false)
    private boolean esPropia;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public SedeLocalizacionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InstitucionJpaEntity getInstitucion() {
        return institucion;
    }

    public void setInstitucion(InstitucionJpaEntity institucion) {
        this.institucion = institucion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = piso;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public ProvinciaJpaEntity getProvincia() {
        return provincia;
    }

    public void setProvincia(ProvinciaJpaEntity provincia) {
        this.provincia = provincia;
    }

    public DepartamentoJpaEntity getDepartamentoGeografico() {
        return departamentoGeografico;
    }

    public void setDepartamentoGeografico(DepartamentoJpaEntity departamentoGeografico) {
        this.departamentoGeografico = departamentoGeografico;
    }

    public LocalidadJpaEntity getLocalidad() {
        return localidad;
    }

    public void setLocalidad(LocalidadJpaEntity localidad) {
        this.localidad = localidad;
    }

    public boolean isEsPropia() {
        return esPropia;
    }

    public void setEsPropia(boolean esPropia) {
        this.esPropia = esPropia;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
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
        SedeLocalizacionJpaEntity that = (SedeLocalizacionJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
