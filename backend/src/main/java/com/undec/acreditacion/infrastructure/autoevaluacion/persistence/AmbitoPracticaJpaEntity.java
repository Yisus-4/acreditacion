package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ambito_practica")
public class AmbitoPracticaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "numero_orden")
    private Integer numeroOrden;

    @Column(name = "denominacion", nullable = false, length = 255)
    private String denominacion;

    @Column(name = "tipo_categoria", nullable = false, length = 50)
    private String tipoCategoria; // 'INTERNO', 'EXTERNO', 'UNIDAD_DEMOSTRATIVA', 'CAM'

    @Column(name = "tipo_ambito", length = 100)
    private String tipoAmbito;

    @Column(name = "tipo_establecimiento", length = 100)
    private String tipoEstablecimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id")
    private InmuebleJpaEntity inmueble;

    @Column(name = "inmueble_nombre_referencia", length = 255)
    private String inmuebleNombreReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convenio_id")
    private ConvenioInstitucionalJpaEntity convenio;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "anio_construccion")
    private Integer anioConstruccion;

    @Column(name = "superficie_m2", precision = 12, scale = 2)
    private BigDecimal superficieM2;

    @Column(name = "capacidad_alumnos")
    private Integer capacidadAlumnos;

    @Column(name = "disponibilidad_horaria", length = 100)
    private String disponibilidadHoraria;

    // Campos CAM
    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "localidad", length = 100)
    private String localidad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "departamento", length = 100)
    private String departamento;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(name = "tipo_dependencia", length = 100)
    private String tipoDependencia;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "actividad_alumno_cam", columnDefinition = "TEXT")
    private String actividadAlumnoCam;

    // Campos Unidades Demostrativas
    @Column(name = "especies_animales_cultivos", columnDefinition = "TEXT")
    private String especiesAnimalesCultivos;

    @Column(name = "numero_animales")
    private Integer numeroAnimales;

    @Column(name = "actividades_demostrativas", columnDefinition = "TEXT")
    private String actividadesDemostrativas;

    @Column(name = "carreras_que_lo_usan", columnDefinition = "TEXT")
    private String carrerasQueLoUsan;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public AmbitoPracticaJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(Integer numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public String getTipoCategoria() {
        return tipoCategoria;
    }

    public void setTipoCategoria(String tipoCategoria) {
        this.tipoCategoria = tipoCategoria;
    }

    public String getTipoAmbito() {
        return tipoAmbito;
    }

    public void setTipoAmbito(String tipoAmbito) {
        this.tipoAmbito = tipoAmbito;
    }

    public String getTipoEstablecimiento() {
        return tipoEstablecimiento;
    }

    public void setTipoEstablecimiento(String tipoEstablecimiento) {
        this.tipoEstablecimiento = tipoEstablecimiento;
    }

    public InmuebleJpaEntity getInmueble() {
        return inmueble;
    }

    public void setInmueble(InmuebleJpaEntity inmueble) {
        this.inmueble = inmueble;
    }

    public String getInmuebleNombreReferencia() {
        return inmuebleNombreReferencia;
    }

    public void setInmuebleNombreReferencia(String inmuebleNombreReferencia) {
        this.inmuebleNombreReferencia = inmuebleNombreReferencia;
    }

    public ConvenioInstitucionalJpaEntity getConvenio() {
        return convenio;
    }

    public void setConvenio(ConvenioInstitucionalJpaEntity convenio) {
        this.convenio = convenio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getAnioConstruccion() {
        return anioConstruccion;
    }

    public void setAnioConstruccion(Integer anioConstruccion) {
        this.anioConstruccion = anioConstruccion;
    }

    public BigDecimal getSuperficieM2() {
        return superficieM2;
    }

    public void setSuperficieM2(BigDecimal superficieM2) {
        this.superficieM2 = superficieM2;
    }

    public Integer getCapacidadAlumnos() {
        return capacidadAlumnos;
    }

    public void setCapacidadAlumnos(Integer capacidadAlumnos) {
        this.capacidadAlumnos = capacidadAlumnos;
    }

    public String getDisponibilidadHoraria() {
        return disponibilidadHoraria;
    }

    public void setDisponibilidadHoraria(String disponibilidadHoraria) {
        this.disponibilidadHoraria = disponibilidadHoraria;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getTipoDependencia() {
        return tipoDependencia;
    }

    public void setTipoDependencia(String tipoDependencia) {
        this.tipoDependencia = tipoDependencia;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getActividadAlumnoCam() {
        return actividadAlumnoCam;
    }

    public void setActividadAlumnoCam(String actividadAlumnoCam) {
        this.actividadAlumnoCam = actividadAlumnoCam;
    }

    public String getEspeciesAnimalesCultivos() {
        return especiesAnimalesCultivos;
    }

    public void setEspeciesAnimalesCultivos(String especiesAnimalesCultivos) {
        this.especiesAnimalesCultivos = especiesAnimalesCultivos;
    }

    public Integer getNumeroAnimales() {
        return numeroAnimales;
    }

    public void setNumeroAnimales(Integer numeroAnimales) {
        this.numeroAnimales = numeroAnimales;
    }

    public String getActividadesDemostrativas() {
        return actividadesDemostrativas;
    }

    public void setActividadesDemostrativas(String actividadesDemostrativas) {
        this.actividadesDemostrativas = actividadesDemostrativas;
    }

    public String getCarrerasQueLoUsan() {
        return carrerasQueLoUsan;
    }

    public void setCarrerasQueLoUsan(String carrerasQueLoUsan) {
        this.carrerasQueLoUsan = carrerasQueLoUsan;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AmbitoPracticaJpaEntity that = (AmbitoPracticaJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
