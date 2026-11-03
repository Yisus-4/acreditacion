package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.RolClaustroCa;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "miembro_comision_autoevaluacion")
public class MiembroComisionAutoevaluacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_ca_id", nullable = false)
    private RegistroConformacionCaJpaEntity registroCa;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

    @Column(name = "dni_cuil", nullable = false, length = 30)
    private String dniCuil;

    @Column(name = "cuit_pasaporte", length = 50)
    private String cuitPasaporte;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "correo_gmail", length = 150)
    private String correoGmail;

    @Column(name = "cargo_funcion", length = 150)
    private String cargoFuncion;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "claustro_rol", nullable = false, length = 50)
    private RolClaustroCa claustroRol;

    @Column(name = "pendiente_alta_sistema", nullable = false)
    private boolean pendienteAltaSistema;

    @Column(name = "numero_orden", nullable = false)
    private int numeroOrden;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public MiembroComisionAutoevaluacionJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RegistroConformacionCaJpaEntity getRegistroCa() {
        return registroCa;
    }

    public void setRegistroCa(RegistroConformacionCaJpaEntity registroCa) {
        this.registroCa = registroCa;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getDniCuil() {
        return dniCuil;
    }

    public void setDniCuil(String dniCuil) {
        this.dniCuil = dniCuil;
    }

    public String getCuitPasaporte() {
        return cuitPasaporte;
    }

    public void setCuitPasaporte(String cuitPasaporte) {
        this.cuitPasaporte = cuitPasaporte;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCorreoGmail() {
        return correoGmail;
    }

    public void setCorreoGmail(String correoGmail) {
        this.correoGmail = correoGmail;
    }

    public String getCargoFuncion() {
        return cargoFuncion;
    }

    public void setCargoFuncion(String cargoFuncion) {
        this.cargoFuncion = cargoFuncion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public RolClaustroCa getClaustroRol() {
        return claustroRol;
    }

    public void setClaustroRol(RolClaustroCa claustroRol) {
        this.claustroRol = claustroRol;
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

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
