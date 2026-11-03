package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "espacio_fisico")
public class EspacioFisicoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private InmuebleJpaEntity inmueble;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_espacio_id", nullable = false)
    private TipoEspacioFisicoJpaEntity tipoEspacio;

    @Column(name = "denominacion", nullable = false)
    private String denominacion;

    @Column(name = "descripcion_infraestructura", columnDefinition = "TEXT")
    private String descripcionInfraestructura;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "capacidad_personas_sentadas", nullable = false)
    private Integer capacidadPersonasSentadas = 0;

    @Column(name = "capacidad_estudiantes", nullable = false)
    private Integer capacidadEstudiantes = 0;

    @Column(name = "superficie_m2", precision = 10, scale = 2)
    private BigDecimal superficieM2;

    @Column(name = "equipamiento_disponible", columnDefinition = "TEXT")
    private String equipamientoDisponible;

    @Column(name = "estado_coneau", nullable = false, length = 50)
    private String estadoConeau = "CARGADO";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public EspacioFisicoJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InmuebleJpaEntity getInmueble() {
        return inmueble;
    }

    public void setInmueble(InmuebleJpaEntity inmueble) {
        this.inmueble = inmueble;
    }

    public TipoEspacioFisicoJpaEntity getTipoEspacio() {
        return tipoEspacio;
    }

    public void setTipoEspacio(TipoEspacioFisicoJpaEntity tipoEspacio) {
        this.tipoEspacio = tipoEspacio;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public String getDescripcionInfraestructura() {
        return descripcionInfraestructura;
    }

    public void setDescripcionInfraestructura(String descripcionInfraestructura) {
        this.descripcionInfraestructura = descripcionInfraestructura;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getCapacidadPersonasSentadas() {
        return capacidadPersonasSentadas;
    }

    public void setCapacidadPersonasSentadas(Integer capacidadPersonasSentadas) {
        this.capacidadPersonasSentadas = capacidadPersonasSentadas;
        this.capacidadEstudiantes = capacidadPersonasSentadas;
    }

    public Integer getCapacidadEstudiantes() {
        return capacidadEstudiantes;
    }

    public void setCapacidadEstudiantes(Integer capacidadEstudiantes) {
        this.capacidadEstudiantes = capacidadEstudiantes;
        if (this.capacidadPersonasSentadas == null || this.capacidadPersonasSentadas == 0) {
            this.capacidadPersonasSentadas = capacidadEstudiantes;
        }
    }

    public BigDecimal getSuperficieM2() {
        return superficieM2;
    }

    public void setSuperficieM2(BigDecimal superficieM2) {
        this.superficieM2 = superficieM2;
    }

    public String getEquipamientoDisponible() {
        return equipamientoDisponible;
    }

    public void setEquipamientoDisponible(String equipamientoDisponible) {
        this.equipamientoDisponible = equipamientoDisponible;
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
}
