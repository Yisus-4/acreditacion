package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matriz_permisos_atenea")
public class ItemMatrizPermisoAteneaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_id", nullable = false)
    private RegistroUsuariosAteneaJpaEntity registro;

    @Column(name = "proceso_actividad", nullable = false, length = 255)
    private String procesoActividad;

    @Column(name = "item", nullable = false, length = 255)
    private String item;

    @Column(name = "seccion", length = 100)
    private String seccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_permiso", nullable = false, length = 50)
    private TipoPermisoMatrizAtenea tipoPermiso;

    @Column(name = "permiso_sgi_aea", nullable = false)
    private boolean permisoSgiAea;

    @Column(name = "permiso_ca", nullable = false)
    private boolean permisoCa;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ItemMatrizPermisoAteneaJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RegistroUsuariosAteneaJpaEntity getRegistro() {
        return registro;
    }

    public void setRegistro(RegistroUsuariosAteneaJpaEntity registro) {
        this.registro = registro;
    }

    public String getProcesoActividad() {
        return procesoActividad;
    }

    public void setProcesoActividad(String procesoActividad) {
        this.procesoActividad = procesoActividad;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public TipoPermisoMatrizAtenea getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(TipoPermisoMatrizAtenea tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public boolean isPermisoSgiAea() {
        return permisoSgiAea;
    }

    public void setPermisoSgiAea(boolean permisoSgiAea) {
        this.permisoSgiAea = permisoSgiAea;
    }

    public boolean isPermisoCa() {
        return permisoCa;
    }

    public void setPermisoCa(boolean permisoCa) {
        this.permisoCa = permisoCa;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
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
}
