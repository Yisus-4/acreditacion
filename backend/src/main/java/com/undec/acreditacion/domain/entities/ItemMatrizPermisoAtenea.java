package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ItemMatrizPermisoAtenea {

    private UUID id;
    private UUID registroId;
    private String procesoActividad;
    private String item;
    private String seccion;
    private TipoPermisoMatrizAtenea tipoPermiso;
    private boolean permisoSgiAea;
    private boolean permisoCa;
    private int orden;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ItemMatrizPermisoAtenea() {
        this.id = UUID.randomUUID();
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public static ItemMatrizPermisoAtenea crear(
            String procesoActividad,
            String item,
            String seccion,
            TipoPermisoMatrizAtenea tipoPermiso,
            boolean permisoSgiAea,
            boolean permisoCa,
            int orden
    ) {
        if (procesoActividad == null || procesoActividad.isBlank()) {
            throw new DomainValidationException("El proceso/actividad es obligatorio");
        }
        if (item == null || item.isBlank()) {
            throw new DomainValidationException("El item es obligatorio");
        }
        if (tipoPermiso == null) {
            throw new DomainValidationException("El tipo de permiso es obligatorio");
        }

        ItemMatrizPermisoAtenea i = new ItemMatrizPermisoAtenea();
        i.id = UUID.randomUUID();
        i.procesoActividad = procesoActividad.trim();
        i.item = item.trim();
        i.seccion = seccion != null ? seccion.trim() : null;
        i.tipoPermiso = tipoPermiso;
        i.permisoSgiAea = permisoSgiAea;
        i.permisoCa = permisoCa;
        i.orden = orden;
        i.creadoEn = Instant.now();
        i.actualizadoEn = Instant.now();
        return i;
    }

    public static ItemMatrizPermisoAtenea rehydrate(
            UUID id,
            UUID registroId,
            String procesoActividad,
            String item,
            String seccion,
            TipoPermisoMatrizAtenea tipoPermiso,
            boolean permisoSgiAea,
            boolean permisoCa,
            int orden,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        ItemMatrizPermisoAtenea i = new ItemMatrizPermisoAtenea();
        i.id = id;
        i.registroId = registroId;
        i.procesoActividad = procesoActividad;
        i.item = item;
        i.seccion = seccion;
        i.tipoPermiso = tipoPermiso;
        i.permisoSgiAea = permisoSgiAea;
        i.permisoCa = permisoCa;
        i.orden = orden;
        i.creadoEn = creadoEn;
        i.actualizadoEn = actualizadoEn;
        return i;
    }

    public void actualizarPermisos(boolean permisoSgiAea, boolean permisoCa) {
        this.permisoSgiAea = permisoSgiAea;
        this.permisoCa = permisoCa;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRegistroId() {
        return registroId;
    }

    public void setRegistroId(UUID registroId) {
        this.registroId = registroId;
    }

    public String getProcesoActividad() {
        return procesoActividad;
    }

    public String getItem() {
        return item;
    }

    public String getSeccion() {
        return seccion;
    }

    public TipoPermisoMatrizAtenea getTipoPermiso() {
        return tipoPermiso;
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

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemMatrizPermisoAtenea that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
