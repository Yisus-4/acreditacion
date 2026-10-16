package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Carrera {

    public static final String NIVEL_GRADO = "GRADO";

    private final UUID id;
    private final UUID unidadAcademicaId;
    private String nombre;
    private String sigla;
    private final String nivel;
    private String areaDisciplinaria;
    private ModalidadCarrera modalidad;
    private EstadoCarrera estado;
    private String tituloOtorgado;
    private Integer duracionAnios;
    private String descripcion;
    private boolean activo;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<UUID> sedesIds = new ArrayList<>();

    public static Carrera registrar(UUID unidadAcademicaId,
                                    String nombre,
                                    String sigla,
                                    String areaDisciplinaria,
                                    ModalidadCarrera modalidad,
                                    EstadoCarrera estado,
                                    String tituloOtorgado,
                                    Integer duracionAnios,
                                    String descripcion,
                                    List<UUID> sedesIds) {
        Instant now = Instant.now();
        return new Carrera(
                UUID.randomUUID(),
                unidadAcademicaId,
                nombre,
                sigla,
                NIVEL_GRADO,
                areaDisciplinaria,
                modalidad,
                estado != null ? estado : EstadoCarrera.EN_FUNCIONAMIENTO,
                tituloOtorgado,
                duracionAnios,
                descripcion,
                true,
                now,
                now,
                sedesIds
        );
    }

    public static Carrera rehydrate(UUID id,
                                    UUID unidadAcademicaId,
                                    String nombre,
                                    String sigla,
                                    String nivel,
                                    String areaDisciplinaria,
                                    ModalidadCarrera modalidad,
                                    EstadoCarrera estado,
                                    String tituloOtorgado,
                                    Integer duracionAnios,
                                    String descripcion,
                                    boolean activo,
                                    Instant creadoEn,
                                    Instant actualizadoEn,
                                    List<UUID> sedesIds) {
        return new Carrera(
                id,
                unidadAcademicaId,
                nombre,
                sigla,
                nivel,
                areaDisciplinaria,
                modalidad,
                estado,
                tituloOtorgado,
                duracionAnios,
                descripcion,
                activo,
                creadoEn,
                actualizadoEn,
                sedesIds
        );
    }

    public Carrera(UUID id,
                   UUID unidadAcademicaId,
                   String nombre,
                   String sigla,
                   String nivel,
                   String areaDisciplinaria,
                   ModalidadCarrera modalidad,
                   EstadoCarrera estado,
                   String tituloOtorgado,
                   Integer duracionAnios,
                   String descripcion,
                   boolean activo,
                   Instant creadoEn,
                   Instant actualizadoEn,
                   List<UUID> sedesIds) {
        this.id = Objects.requireNonNull(id, "El id de la carrera es obligatorio");
        this.unidadAcademicaId = Objects.requireNonNull(unidadAcademicaId, "La unidad académica es obligatoria");
        this.nombre = requireNonBlank(nombre, "El nombre de la carrera es obligatorio");
        this.sigla = sigla != null && !sigla.isBlank() ? sigla.trim() : null;

        String nivelLimpio = requireNonBlank(nivel, "El nivel de la carrera es obligatorio");
        if (!NIVEL_GRADO.equalsIgnoreCase(nivelLimpio)) {
            throw new DomainValidationException("El nivel de la carrera debe ser estrictamente 'GRADO'");
        }
        this.nivel = NIVEL_GRADO;

        this.areaDisciplinaria = requireNonBlank(areaDisciplinaria, "El área disciplinaria es obligatoria");
        this.modalidad = Objects.requireNonNull(modalidad, "La modalidad de la carrera es obligatoria");
        this.estado = Objects.requireNonNull(estado, "El estado de la carrera es obligatorio");
        this.tituloOtorgado = tituloOtorgado != null && !tituloOtorgado.isBlank() ? tituloOtorgado.trim() : null;
        if (duracionAnios != null && duracionAnios <= 0) {
            throw new DomainValidationException("La duración en años debe ser un número positivo");
        }
        this.duracionAnios = duracionAnios;
        this.descripcion = descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null;
        this.activo = activo;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;

        if (sedesIds != null) {
            this.sedesIds.addAll(sedesIds);
        }
    }

    public void actualizar(String nuevoNombre,
                           String nuevaSigla,
                           String nuevaAreaDisciplinaria,
                           ModalidadCarrera nuevaModalidad,
                           EstadoCarrera nuevoEstado,
                           String nuevoTituloOtorgado,
                           Integer nuevaDuracionAnios,
                           String nuevaDescripcion,
                           List<UUID> nuevasSedesIds) {
        this.nombre = requireNonBlank(nuevoNombre, "El nombre de la carrera es obligatorio");
        this.sigla = nuevaSigla != null && !nuevaSigla.isBlank() ? nuevaSigla.trim() : null;
        this.areaDisciplinaria = requireNonBlank(nuevaAreaDisciplinaria, "El área disciplinaria es obligatoria");
        this.modalidad = Objects.requireNonNull(nuevaModalidad, "La modalidad de la carrera es obligatoria");
        this.estado = Objects.requireNonNull(nuevoEstado, "El estado de la carrera es obligatorio");
        this.tituloOtorgado = nuevoTituloOtorgado != null && !nuevoTituloOtorgado.isBlank() ? nuevoTituloOtorgado.trim() : null;
        if (nuevaDuracionAnios != null && nuevaDuracionAnios <= 0) {
            throw new DomainValidationException("La duración en años debe ser un número positivo");
        }
        this.duracionAnios = nuevaDuracionAnios;
        this.descripcion = nuevaDescripcion != null && !nuevaDescripcion.isBlank() ? nuevaDescripcion.trim() : null;

        this.sedesIds.clear();
        if (nuevasSedesIds != null) {
            this.sedesIds.addAll(nuevasSedesIds);
        }
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

    public UUID getUnidadAcademicaId() {
        return unidadAcademicaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNivel() {
        return nivel;
    }

    public String getAreaDisciplinaria() {
        return areaDisciplinaria;
    }

    public ModalidadCarrera getModalidad() {
        return modalidad;
    }

    public EstadoCarrera getEstado() {
        return estado;
    }

    public String getTituloOtorgado() {
        return tituloOtorgado;
    }

    public Integer getDuracionAnios() {
        return duracionAnios;
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

    public List<UUID> getSedesIds() {
        return Collections.unmodifiableList(sedesIds);
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException(message);
        }
        return value.trim();
    }
}
