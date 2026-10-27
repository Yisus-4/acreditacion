package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalogo_miembro_comision")
public class CatalogoMiembroComisionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "documento", nullable = false, length = 30)
    private String documento;

    @Column(name = "cuit_pasaporte", nullable = false, length = 50)
    private String cuitPasaporte;

    @Column(name = "correo_electronico", nullable = false, length = 150)
    private String correoElectronico;

    @Column(name = "correo_gmail", nullable = false, length = 150)
    private String correoGmail;

    @Column(name = "cargo_funcion", nullable = false, length = 150)
    private String cargoFuncion;

    @Column(name = "es_docente", nullable = false)
    private boolean esDocente;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public CatalogoMiembroComisionJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getCuitPasaporte() {
        return cuitPasaporte;
    }

    public void setCuitPasaporte(String cuitPasaporte) {
        this.cuitPasaporte = cuitPasaporte;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
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

    public boolean isEsDocente() {
        return esDocente;
    }

    public void setEsDocente(boolean esDocente) {
        this.esDocente = esDocente;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
