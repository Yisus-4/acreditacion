package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.DepartamentoJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.LocalidadJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProvinciaJpaEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "inmueble")
public class InmuebleJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_id", nullable = false)
    private InstitucionJpaEntity institucion;

    @Column(name = "numero_orden")
    private Integer numeroOrden;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "domicilio", nullable = false)
    private String domicilio;

    @Column(name = "calle")
    private String calle;

    @Column(name = "numero")
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id", nullable = false)
    private ProvinciaJpaEntity provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private DepartamentoJpaEntity departamentoGeografico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localidad_id", nullable = false)
    private LocalidadJpaEntity localidad;

    @Column(name = "anio_construccion")
    private Integer anioConstruccion;

    @Column(name = "anio_inicio_institucion")
    private Integer anioInicioInstitucion;

    @Column(name = "propiedad_institucion", nullable = false)
    private Boolean propiedadInstitucion = true;

    @Column(name = "condicion_tenencia", nullable = false, length = 50)
    private String condicionTenencia = "PROPIO";

    @Column(name = "situacion", length = 100)
    private String situacion = "Propiedad";

    @Column(name = "vincula_todas_carreras", nullable = false)
    private Boolean vinculaTodasCarreras = false;

    @Column(name = "es_maestro", nullable = false)
    private Boolean esMaestro = true;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @Column(name = "superficie_cubierta_m2", precision = 10, scale = 2)
    private BigDecimal superficieCubiertaM2;

    @Column(name = "superficie_total_m2", precision = 10, scale = 2)
    private BigDecimal superficieTotalM2;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "verificado", nullable = false)
    private Boolean verificado = false;

    @Column(name = "fecha_verificacion")
    private Instant fechaVerificacion;

    @Column(name = "estado_coneau", nullable = false, length = 50)
    private String estadoConeau = "CARGADO";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inmueble_carreras",
            joinColumns = @JoinColumn(name = "inmueble_id"),
            inverseJoinColumns = @JoinColumn(name = "carrera_id")
    )
    private Set<CarreraJpaEntity> carreras = new HashSet<>();

    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EspacioFisicoJpaEntity> espacios = new ArrayList<>();

    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InmuebleDocumentacionRespaldoJpaEntity> documentos = new ArrayList<>();

    public InmuebleJpaEntity() {
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

    public Integer getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(Integer numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
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

    public Integer getAnioConstruccion() {
        return anioConstruccion;
    }

    public void setAnioConstruccion(Integer anioConstruccion) {
        this.anioConstruccion = anioConstruccion;
    }

    public Integer getAnioInicioInstitucion() {
        return anioInicioInstitucion;
    }

    public void setAnioInicioInstitucion(Integer anioInicioInstitucion) {
        this.anioInicioInstitucion = anioInicioInstitucion;
    }

    public Boolean getPropiedadInstitucion() {
        return propiedadInstitucion;
    }

    public void setPropiedadInstitucion(Boolean propiedadInstitucion) {
        this.propiedadInstitucion = propiedadInstitucion;
    }

    public String getCondicionTenencia() {
        return condicionTenencia;
    }

    public void setCondicionTenencia(String condicionTenencia) {
        this.condicionTenencia = condicionTenencia;
    }

    public String getSituacion() {
        return situacion;
    }

    public void setSituacion(String situacion) {
        this.situacion = situacion;
    }

    public Boolean getVinculaTodasCarreras() {
        return vinculaTodasCarreras;
    }

    public void setVinculaTodasCarreras(Boolean vinculaTodasCarreras) {
        this.vinculaTodasCarreras = vinculaTodasCarreras;
    }

    public Boolean getEsMaestro() {
        return esMaestro;
    }

    public void setEsMaestro(Boolean esMaestro) {
        this.esMaestro = esMaestro;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public BigDecimal getSuperficieCubiertaM2() {
        return superficieCubiertaM2;
    }

    public void setSuperficieCubiertaM2(BigDecimal superficieCubiertaM2) {
        this.superficieCubiertaM2 = superficieCubiertaM2;
    }

    public BigDecimal getSuperficieTotalM2() {
        return superficieTotalM2;
    }

    public void setSuperficieTotalM2(BigDecimal superficieTotalM2) {
        this.superficieTotalM2 = superficieTotalM2;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

    public String getEstadoConeau() {
        return estadoConeau;
    }

    public void setEstadoConeau(String estadoConeau) {
        this.estadoConeau = estadoConeau;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
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

    public Set<CarreraJpaEntity> getCarreras() {
        return carreras;
    }

    public void setCarreras(Set<CarreraJpaEntity> carreras) {
        this.carreras = carreras;
    }

    public List<EspacioFisicoJpaEntity> getEspacios() {
        return espacios;
    }

    public void setEspacios(List<EspacioFisicoJpaEntity> espacios) {
        this.espacios = espacios;
    }

    public List<InmuebleDocumentacionRespaldoJpaEntity> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<InmuebleDocumentacionRespaldoJpaEntity> documentos) {
        this.documentos = documentos;
    }
}
