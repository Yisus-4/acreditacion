package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SedeLocalizacion {

    private final UUID id;
    private final UUID institucionId;
    private String nombre;
    private String calle;
    private String numero;
    private String piso;
    private String codigoPostal;
    private Long provinciaId;
    private Long departamentoId;
    private Long localidadId;
    private boolean esPropia;
    private boolean activo;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static SedeLocalizacion registrar(UUID institucionId,
                                             String nombre,
                                             String calle,
                                             String numero,
                                             String piso,
                                             String codigoPostal,
                                             Long provinciaId,
                                             Long departamentoId,
                                             Long localidadId,
                                             boolean esPropia) {
        Instant now = Instant.now();
        return new SedeLocalizacion(
                UUID.randomUUID(),
                institucionId,
                nombre,
                calle,
                numero,
                piso,
                codigoPostal,
                provinciaId,
                departamentoId,
                localidadId,
                esPropia,
                true,
                now,
                now
        );
    }

    public static SedeLocalizacion rehydrate(UUID id,
                                             UUID institucionId,
                                             String nombre,
                                             String calle,
                                             String numero,
                                             String piso,
                                             String codigoPostal,
                                             Long provinciaId,
                                             Long departamentoId,
                                             Long localidadId,
                                             boolean esPropia,
                                             boolean activo,
                                             Instant creadoEn,
                                             Instant actualizadoEn) {
        return new SedeLocalizacion(
                id,
                institucionId,
                nombre,
                calle,
                numero,
                piso,
                codigoPostal,
                provinciaId,
                departamentoId,
                localidadId,
                esPropia,
                activo,
                creadoEn,
                actualizadoEn
        );
    }

    public SedeLocalizacion(UUID id,
                            UUID institucionId,
                            String nombre,
                            String calle,
                            String numero,
                            String piso,
                            String codigoPostal,
                            Long provinciaId,
                            Long departamentoId,
                            Long localidadId,
                            boolean esPropia,
                            boolean activo,
                            Instant creadoEn,
                            Instant actualizadoEn) {
        this.id = Objects.requireNonNull(id, "El id de la sede es obligatorio");
        this.institucionId = Objects.requireNonNull(institucionId, "El id de la institución es obligatorio");
        this.nombre = requireNonBlank(nombre, "El nombre de la sede es obligatorio");
        this.calle = requireNonBlank(calle, "La calle de la sede es obligatoria");
        this.numero = requireNonBlank(numero, "El número de la sede es obligatorio");
        this.piso = piso != null && !piso.isBlank() ? piso.trim() : null;
        this.codigoPostal = requireNonBlank(codigoPostal, "El código postal es obligatorio");
        this.provinciaId = Objects.requireNonNull(provinciaId, "La provincia es obligatoria");
        this.departamentoId = Objects.requireNonNull(departamentoId, "El departamento es obligatorio");
        this.localidadId = Objects.requireNonNull(localidadId, "La localidad es obligatoria");
        this.esPropia = esPropia;
        this.activo = activo;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void actualizar(String nuevoNombre,
                           String nuevaCalle,
                           String nuevoNumero,
                           String nuevoPiso,
                           String nuevoCodigoPostal,
                           Long nuevaProvinciaId,
                           Long nuevoDepartamentoId,
                           Long nuevaLocalidadId,
                           boolean nuevaEsPropia) {
        this.nombre = requireNonBlank(nuevoNombre, "El nombre de la sede es obligatorio");
        this.calle = requireNonBlank(nuevaCalle, "La calle de la sede es obligatoria");
        this.numero = requireNonBlank(nuevoNumero, "El número de la sede es obligatorio");
        this.piso = nuevoPiso != null && !nuevoPiso.isBlank() ? nuevoPiso.trim() : null;
        this.codigoPostal = requireNonBlank(nuevoCodigoPostal, "El código postal es obligatorio");
        this.provinciaId = Objects.requireNonNull(nuevaProvinciaId, "La provincia es obligatoria");
        this.departamentoId = Objects.requireNonNull(nuevoDepartamentoId, "El departamento es obligatorio");
        this.localidadId = Objects.requireNonNull(nuevaLocalidadId, "La localidad es obligatoria");
        this.esPropia = nuevaEsPropia;
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

    public String getCalle() {
        return calle;
    }

    public String getNumero() {
        return numero;
    }

    public String getPiso() {
        return piso;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public Long getProvinciaId() {
        return provinciaId;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public Long getLocalidadId() {
        return localidadId;
    }

    public boolean isEsPropia() {
        return esPropia;
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

    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder(calle).append(" ").append(numero);
        if (piso != null && !piso.isBlank()) {
            sb.append(", Piso ").append(piso);
        }
        return sb.toString();
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException(message);
        }
        return value.trim();
    }
}
