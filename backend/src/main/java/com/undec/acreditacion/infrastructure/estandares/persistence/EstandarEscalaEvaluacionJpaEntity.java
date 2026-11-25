package com.undec.acreditacion.infrastructure.estandares.persistence;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "estandar_escala_evaluacion")
public class EstandarEscalaEvaluacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estandar_id", nullable = false)
    private EstandarJpaEntity estandar;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "codigo", nullable = false, length = 60)
    private String codigo;

    @Column(name = "etiqueta", nullable = false, length = 120)
    private String etiqueta;

    @Column(name = "descripcion_referencia", columnDefinition = "TEXT")
    private String descripcionReferencia;

    @Column(name = "es_valor_por_defecto", nullable = false)
    private Boolean esValorPorDefecto = false;

    public EstandarEscalaEvaluacionJpaEntity() {
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

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getDescripcionReferencia() {
        return descripcionReferencia;
    }

    public void setDescripcionReferencia(String descripcionReferencia) {
        this.descripcionReferencia = descripcionReferencia;
    }

    public Boolean getEsValorPorDefecto() {
        return esValorPorDefecto;
    }

    public void setEsValorPorDefecto(Boolean esValorPorDefecto) {
        this.esValorPorDefecto = esValorPorDefecto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EstandarEscalaEvaluacionJpaEntity that = (EstandarEscalaEvaluacionJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
