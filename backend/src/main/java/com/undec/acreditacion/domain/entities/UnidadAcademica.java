package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class UnidadAcademica {

    private final UUID id;
    private final UUID institucionId;
    private String nombre;
    private String sigla;
    private String descripcion;
    private boolean activo;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static UnidadAcademica registrar(UUID institucionId,
                                            String nombre,
                                            String sigla,
                                            String descripcion) {
        Instant now = Instant.now();
        return new UnidadAcademica(
                UUID.randomUUID(),
                institucionId,
                nombre,
                sigla,
                descripcion,
                true,
                now,
                now
        );
    }

    public static UnidadAcademica rehydrate(UUID id,
                                            UUID institucionId,
                                            String nombre,
                                            String sigla,
                                            String descripcion,
                                            boolean activo,
                                            Instant creadoEn,
                                            Instant actualizadoEn) {
        return new UnidadAcademica(
                id,
                institucionId,
                nombre,
                sigla,
                descripcion,
                activo,
                creadoEn,
                actualizadoEn
        );
    }

    public UnidadAcademica(UUID id,
                           UUID institucionId,
                           String nombre,
                           String sigla,
                           String descripcion,
                           boolean activo,
                           Instant creadoEn,
                           Instant actualizadoEn) {
        this.id = Objects.requireNonNull(id, "El id de la unidad académica es obligatorio");
        this.institucionId = Objects.requireNonNull(institucionId, "El id de la institución es obligatorio");
        this.nombre = requireNonBlank(nombre, "El nombre de la unidad académica es obligatorio");
        this.sigla = sigla != null && !sigla.isBlank() ? sigla.trim() : null;
        this.descripcion = descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null;
        this.activo = activo;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void actualizar(String nuevoNombre, String nuevaSigla, String nuevaDescripcion) {
        this.nombre = requireNonBlank(nuevoNombre, "El nombre de la unidad académica es obligatorio");
        this.sigla = nuevaSigla != null && !nuevaSigla.isBlank() ? nuevaSigla.trim() : null;
        this.descripcion = nuevaDescripcion != null && !nuevaDescripcion.isBlank() ? nuevaDescripcion.trim() : null;
        this.actualizadoEn = Instant.now();
    }

    public void activar() {
        this.activo = true;
        this.actualizadoEn = Instant.now();
    }

    public void desactivar() {
        this.activo = false;
        this.actualizadoEn = Instant.now();
    }

    public void cambiarEstado(boolean activo) {
        this.activo = activo;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitucionId() {
        return institucionId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException(message);
        }
        return value.trim();
    }
}
