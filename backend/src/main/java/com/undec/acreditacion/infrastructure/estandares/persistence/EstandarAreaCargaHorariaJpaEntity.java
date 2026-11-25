package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "estandar_area_carga_horaria")
public class EstandarAreaCargaHorariaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estandar_id", nullable = false)
    private EstandarJpaEntity estandar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_seccion", nullable = false, length = 30)
    private TipoSeccionCargaHoraria tipoSeccion = TipoSeccionCargaHoraria.CARGA_TOTAL_311;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "codigo", nullable = false, length = 60)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "etiqueta_practica", length = 220)
    private String etiquetaPractica;

    @Column(name = "habilitada", nullable = false)
    private Boolean habilitada = true;

    public EstandarAreaCargaHorariaJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EstandarJpaEntity getEstandar() {
        return estandar;
    }

    public void setEstandar(EstandarJpaEntity estandar) {
        this.estandar = estandar;
    }

    public TipoSeccionCargaHoraria getTipoSeccion() {
        return tipoSeccion;
    }

    public void setTipoSeccion(TipoSeccionCargaHoraria tipoSeccion) {
        this.tipoSeccion = tipoSeccion != null ? tipoSeccion : TipoSeccionCargaHoraria.CARGA_TOTAL_311;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEtiquetaPractica() {
        return etiquetaPractica;
    }

    public void setEtiquetaPractica(String etiquetaPractica) {
        this.etiquetaPractica = etiquetaPractica;
    }

    public Boolean getHabilitada() {
        return habilitada;
    }

    public void setHabilitada(Boolean habilitada) {
        this.habilitada = habilitada;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EstandarAreaCargaHorariaJpaEntity that = (EstandarAreaCargaHorariaJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
