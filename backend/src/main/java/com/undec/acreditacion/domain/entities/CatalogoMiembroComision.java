package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class CatalogoMiembroComision {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private UUID id;
    private String apellido;
    private String nombreCompleto;
    private String documento;
    private String cuitPasaporte;
    private String correoElectronico;
    private String correoGmail;
    private String cargoFuncion;
    private Instant creadoEn;

    public CatalogoMiembroComision(
            UUID id,
            String apellido,
            String nombreCompleto,
            String documento,
            String cuitPasaporte,
            String correoElectronico,
            String correoGmail,
            String cargoFuncion,
            Instant creadoEn
    ) {
        validarCampos(apellido, nombreCompleto, documento, cuitPasaporte, correoElectronico, correoGmail, cargoFuncion);
        this.id = id != null ? id : UUID.randomUUID();
        this.apellido = apellido.trim();
        this.nombreCompleto = nombreCompleto.trim();
        this.documento = documento.trim();
        this.cuitPasaporte = cuitPasaporte.trim();
        this.correoElectronico = correoElectronico.trim().toLowerCase();
        this.correoGmail = correoGmail.trim().toLowerCase();
        this.cargoFuncion = cargoFuncion.trim();
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
    }

    private void validarCampos(
            String apellido,
            String nombreCompleto,
            String documento,
            String cuitPasaporte,
            String correoElectronico,
            String correoGmail,
            String cargoFuncion
    ) {
        if (apellido == null || apellido.isBlank()) {
            throw new DomainValidationException("El apellido del miembro es obligatorio");
        }
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new DomainValidationException("El nombre completo del miembro es obligatorio");
        }
        if (documento == null || documento.isBlank()) {
            throw new DomainValidationException("El documento del miembro es obligatorio");
        }
        if (cuitPasaporte == null || cuitPasaporte.isBlank()) {
            throw new DomainValidationException("El CUIT/Pasaporte del miembro es obligatorio");
        }
        if (correoElectronico == null || correoElectronico.isBlank() || !EMAIL_PATTERN.matcher(correoElectronico.trim()).matches()) {
            throw new DomainValidationException("El correo electrónico del miembro es inválido o no fue ingresado");
        }
        if (correoGmail == null || correoGmail.isBlank() || !EMAIL_PATTERN.matcher(correoGmail.trim()).matches()) {
            throw new DomainValidationException("El correo Gmail del miembro es inválido o no fue ingresado");
        }
        if (cargoFuncion == null || cargoFuncion.isBlank()) {
            throw new DomainValidationException("El cargo o función en la carrera es obligatorio");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getApellido() {
        return apellido;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDocumento() {
        return documento;
    }

    public String getCuitPasaporte() {
        return cuitPasaporte;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public String getCorreoGmail() {
        return correoGmail;
    }

    public String getCargoFuncion() {
        return cargoFuncion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogoMiembroComision that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
