package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Institucion {

    private final UUID id;
    private final String nombre;
    private final String sigla;
    private final String logoPath;
    private final boolean activo;
    private final Instant creadoEn;
    private final Instant actualizadoEn;
    private final List<AutoridadInstitucional> autoridades = new ArrayList<>();
    private final SedeCentral sedeCentral;

    public static Institucion registrar(String nombre,
                                       String sigla,
                                       String logoPath,
                                       AutoridadInstitucional maximaAutoridad,
                                       AutoridadInstitucional administradorInstitucional,
                                       SedeCentral sedeCentral) {
        if (maximaAutoridad == null) {
            throw new DomainValidationException("La máxima autoridad es obligatoria");
        }
        if (administradorInstitucional == null) {
            throw new DomainValidationException("El administrador institucional es obligatorio");
        }
        Instant now = Instant.now();
        List<AutoridadInstitucional> list = List.of(maximaAutoridad, administradorInstitucional);
        return new Institucion(
                UUID.randomUUID(),
                nombre,
                sigla,
                logoPath,
                true,
                now,
                now,
                list,
                sedeCentral
        );
    }

    public static Institucion rehydrate(UUID id,
                                       String nombre,
                                       String sigla,
                                       String logoPath,
                                       boolean activo,
                                       Instant creadoEn,
                                       Instant actualizadoEn,
                                       List<AutoridadInstitucional> autoridades,
                                       SedeCentral sedeCentral) {
        return new Institucion(
                id,
                nombre,
                sigla,
                logoPath,
                activo,
                creadoEn,
                actualizadoEn,
                autoridades,
                sedeCentral
        );
    }

    public Institucion(UUID id,
                       String nombre,
                       String sigla,
                       String logoPath,
                       boolean activo,
                       Instant creadoEn,
                       Instant actualizadoEn,
                       List<AutoridadInstitucional> autoridades,
                       SedeCentral sedeCentral) {
        this.id = Objects.requireNonNull(id, "El id de la institución es obligatorio");
        this.nombre = requireNonBlank(nombre, "El nombre de la institución es obligatorio");
        this.sigla = requireNonBlank(sigla, "La sigla de la institución es obligatoria");
        this.logoPath = requireNonBlank(logoPath, "La ruta del logotipo es obligatoria");
        this.activo = activo;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;

        if (autoridades != null) {
            this.autoridades.addAll(autoridades);
        }
        this.sedeCentral = sedeCentral;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSigla() {
        return sigla;
    }

    public String getLogoPath() {
        return logoPath;
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

    public List<AutoridadInstitucional> getAutoridades() {
        return Collections.unmodifiableList(autoridades);
    }

    public Optional<AutoridadInstitucional> getMaximaAutoridad() {
        return autoridades.stream()
                .filter(a -> a.getTipoAutoridad() == TipoAutoridad.MAXIMA_AUTORIDAD)
                .findFirst();
    }

    public Optional<AutoridadInstitucional> getAdministradorInstitucional() {
        return autoridades.stream()
                .filter(a -> a.getTipoAutoridad() == TipoAutoridad.ADMINISTRADOR_INSTITUCIONAL)
                .findFirst();
    }

    public SedeCentral getSedeCentral() {
        return sedeCentral;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException(message);
        }
        return value.trim();
    }
}
