package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class MiembroComisionAutoevaluacion {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private UUID id;
    private UUID registroCaId;
    private String apellido;
    private String primerNombre;
    private String segundoNombre;
    private String dniCuil;
    private String cuitPasaporte;
    private String correo;
    private String correoGmail;
    private String cargoFuncion;
    private String telefono;
    private RolClaustroCa claustroRol;
    private boolean pendienteAltaSistema;
    private int numeroOrden;

    public MiembroComisionAutoevaluacion(
            UUID id,
            UUID registroCaId,
            String apellido,
            String primerNombre,
            String segundoNombre,
            String dniCuil,
            String cuitPasaporte,
            String correo,
            String correoGmail,
            String cargoFuncion,
            String telefono,
            RolClaustroCa claustroRol,
            boolean pendienteAltaSistema,
            int numeroOrden
    ) {
        validarCampos(apellido, primerNombre, dniCuil, correo, claustroRol);
        this.id = id != null ? id : UUID.randomUUID();
        this.registroCaId = registroCaId;
        this.apellido = apellido.trim();
        this.primerNombre = primerNombre.trim();
        this.segundoNombre = segundoNombre != null && !segundoNombre.isBlank() ? segundoNombre.trim() : null;
        this.dniCuil = dniCuil.trim();
        this.cuitPasaporte = cuitPasaporte != null && !cuitPasaporte.isBlank() ? cuitPasaporte.trim() : null;
        this.correo = correo.trim().toLowerCase();
        this.correoGmail = correoGmail != null && !correoGmail.isBlank() ? correoGmail.trim().toLowerCase() : null;
        this.cargoFuncion = cargoFuncion != null && !cargoFuncion.isBlank() ? cargoFuncion.trim() : null;
        this.telefono = telefono != null && !telefono.isBlank() ? telefono.trim() : null;
        this.claustroRol = claustroRol;
        this.pendienteAltaSistema = pendienteAltaSistema;
        this.numeroOrden = numeroOrden;
    }

    public MiembroComisionAutoevaluacion(
            UUID id,
            UUID registroCaId,
            String apellido,
            String primerNombre,
            String segundoNombre,
            String dniCuil,
            String correo,
            String telefono,
            RolClaustroCa claustroRol,
            int numeroOrden
    ) {
        this(
                id,
                registroCaId,
                apellido,
                primerNombre,
                segundoNombre,
                dniCuil,
                null,
                correo,
                null,
                null,
                telefono,
                claustroRol,
                false,
                numeroOrden
        );
    }

    private void validarCampos(String apellido, String primerNombre, String dniCuil, String correo, RolClaustroCa claustroRol) {
        if (apellido == null || apellido.isBlank()) {
            throw new DomainValidationException("El apellido del miembro es obligatorio");
        }
        if (primerNombre == null || primerNombre.isBlank()) {
            throw new DomainValidationException("El primer nombre del miembro es obligatorio");
        }
        if (dniCuil == null || dniCuil.isBlank()) {
            throw new DomainValidationException("El DNI/CUIL del miembro es obligatorio");
        }
        if (correo == null || correo.isBlank() || !EMAIL_PATTERN.matcher(correo.trim()).matches()) {
            throw new DomainValidationException("El correo electrónico del miembro es inválido o no fue ingresado");
        }
        if (claustroRol == null) {
            throw new DomainValidationException("El rol o claustro del miembro es obligatorio");
        }
    }

    public void validarRequisitosParaRemision() {
        // Restricción de CVar obligatoria eliminada según requerimiento
    }

    public String getNombreCompleto() {
        if (segundoNombre != null && !segundoNombre.isBlank()) {
            return primerNombre + " " + segundoNombre;
        }
        return primerNombre;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRegistroCaId() {
        return registroCaId;
    }

    public void setRegistroCaId(UUID registroCaId) {
        this.registroCaId = registroCaId;
    }

    public String getApellido() {
        return apellido;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public String getDniCuil() {
        return dniCuil;
    }

    public String getCuitPasaporte() {
        return cuitPasaporte;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCorreoGmail() {
        return correoGmail;
    }

    public String getCargoFuncion() {
        return cargoFuncion;
    }

    public String getTelefono() {
        return telefono;
    }

    public RolClaustroCa getClaustroRol() {
        return claustroRol;
    }

    public boolean isPendienteAltaSistema() {
        return pendienteAltaSistema;
    }

    public void setPendienteAltaSistema(boolean pendienteAltaSistema) {
        this.pendienteAltaSistema = pendienteAltaSistema;
    }

    public int getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(int numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiembroComisionAutoevaluacion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
