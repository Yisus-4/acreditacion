package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Convocatoria {

    private final UUID id;
    private final UUID unidadAcademicaId;
    private final String codigo;
    private final String titulo;
    private final TipoProcesoAcreditacion tipoProceso;
    private final String nroResolucionConeau;
    private final LocalDate fechaInicio;
    private final LocalDate fechaCierre;
    private EstadoConvocatoria estado;
    private final String observaciones;
    private final UUID creadoPor;
    private UUID confirmadoPorDe;
    private Instant confirmadoPorDeEn;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<ConvocatoriaCarrera> carreras = new ArrayList<>();

    public static Convocatoria crear(UUID unidadAcademicaId,
                                     String codigo,
                                     String titulo,
                                     TipoProcesoAcreditacion tipoProceso,
                                     String nroResolucionConeau,
                                     LocalDate fechaInicio,
                                     LocalDate fechaCierre,
                                     String observaciones,
                                     UUID creadoPor,
                                     List<ConvocatoriaCarrera> itemsCarreras) {
        if (itemsCarreras == null || itemsCarreras.isEmpty()) {
            throw new DomainValidationException("Debe seleccionar al menos una carrera para la convocatoria");
        }
        boolean tieneObligatoria = itemsCarreras.stream().anyMatch(ConvocatoriaCarrera::isEsObligatoria);
        if (!tieneObligatoria) {
            throw new DomainValidationException("Al menos una carrera debe seleccionarse como obligatoria para la acreditación");
        }

        Instant now = Instant.now();
        Convocatoria c = new Convocatoria(
                UUID.randomUUID(),
                unidadAcademicaId,
                codigo,
                titulo,
                tipoProceso,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA,
                observaciones,
                creadoPor,
                null,
                null,
                now,
                now,
                itemsCarreras
        );
        return c;
    }

    public static Convocatoria rehydrate(UUID id,
                                         UUID unidadAcademicaId,
                                         String codigo,
                                         String titulo,
                                         TipoProcesoAcreditacion tipoProceso,
                                         String nroResolucionConeau,
                                         LocalDate fechaInicio,
                                         LocalDate fechaCierre,
                                         EstadoConvocatoria estado,
                                         String observaciones,
                                         UUID creadoPor,
                                         UUID confirmadoPorDe,
                                         Instant confirmadoPorDeEn,
                                         Instant creadoEn,
                                         Instant actualizadoEn,
                                         List<ConvocatoriaCarrera> carreras) {
        return new Convocatoria(
                id,
                unidadAcademicaId,
                codigo,
                titulo,
                tipoProceso,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                estado,
                observaciones,
                creadoPor,
                confirmadoPorDe,
                confirmadoPorDeEn,
                creadoEn,
                actualizadoEn,
                carreras
        );
    }

    public Convocatoria(UUID id,
                        UUID unidadAcademicaId,
                        String codigo,
                        String titulo,
                        TipoProcesoAcreditacion tipoProceso,
                        String nroResolucionConeau,
                        LocalDate fechaInicio,
                        LocalDate fechaCierre,
                        EstadoConvocatoria estado,
                        String observaciones,
                        UUID creadoPor,
                        UUID confirmadoPorDe,
                        Instant confirmadoPorDeEn,
                        Instant creadoEn,
                        Instant actualizadoEn,
                        List<ConvocatoriaCarrera> carreras) {
        if (id == null) {
            throw new DomainValidationException("El ID de la convocatoria es obligatorio");
        }
        if (unidadAcademicaId == null) {
            throw new DomainValidationException("La escuela / unidad académica es obligatoria");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DomainValidationException("El código de la convocatoria es obligatorio");
        }
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DomainValidationException("El título de la convocatoria es obligatorio");
        }
        if (tipoProceso == null) {
            throw new DomainValidationException("El tipo de proceso es obligatorio");
        }
        if (fechaInicio == null) {
            throw new DomainValidationException("La fecha de inicio es requerida");
        }
        if (fechaCierre == null) {
            throw new DomainValidationException("La fecha de cierre es requerida");
        }
        if (fechaCierre.isBefore(fechaInicio)) {
            throw new DomainValidationException("La fecha de cierre no puede ser anterior a la fecha de inicio");
        }
        if (estado == null) {
            throw new DomainValidationException("El estado de la convocatoria es obligatorio");
        }
        if (creadoPor == null) {
            throw new DomainValidationException("El usuario creador es obligatorio");
        }

        this.id = id;
        this.unidadAcademicaId = unidadAcademicaId;
        this.codigo = codigo.trim();
        this.titulo = titulo.trim();
        this.tipoProceso = tipoProceso;
        this.nroResolucionConeau = nroResolucionConeau != null ? nroResolucionConeau.trim() : null;
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
        this.estado = estado;
        this.observaciones = observaciones;
        this.creadoPor = creadoPor;
        this.confirmadoPorDe = confirmadoPorDe;
        this.confirmadoPorDeEn = confirmadoPorDeEn;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;

        if (carreras != null) {
            this.carreras.addAll(carreras);
        }
    }

    public void confirmarPorDirectorEscuela(List<UUID> carrerasConfirmadas, UUID usuarioDeId) {
        if (this.estado != EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA) {
            throw new DomainValidationException("La convocatoria ya se encuentra confirmada o procesada");
        }
        if (carrerasConfirmadas == null || carrerasConfirmadas.isEmpty()) {
            throw new DomainValidationException("El Director de Escuela debe postular al menos una carrera para acreditar");
        }

        // Regla: Debe mandar sí o sí todas las carreras marcadas como obligatorias por AEA
        for (ConvocatoriaCarrera item : this.carreras) {
            if (item.isEsObligatoria() && !carrerasConfirmadas.contains(item.getCarreraId())) {
                throw new DomainValidationException("No se pueden omitir las carreras obligatorias definidas por AEA");
            }
        }

        // Actualizar flags de selección por DE
        for (ConvocatoriaCarrera item : this.carreras) {
            boolean elegida = carrerasConfirmadas.contains(item.getCarreraId());
            item.confirmarPorDirectorEscuela(elegida);
        }

        this.estado = EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA;
        this.confirmadoPorDe = Objects.requireNonNull(usuarioDeId, "El usuario Director de Escuela es requerido");
        this.confirmadoPorDeEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadAcademicaId() {
        return unidadAcademicaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public TipoProcesoAcreditacion getTipoProceso() {
        return tipoProceso;
    }

    public String getNroResolucionConeau() {
        return nroResolucionConeau;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public EstadoConvocatoria getEstado() {
        return estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public UUID getCreadoPor() {
        return creadoPor;
    }

    public UUID getConfirmadoPorDe() {
        return confirmadoPorDe;
    }

    public Instant getConfirmadoPorDeEn() {
        return confirmadoPorDeEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<ConvocatoriaCarrera> getCarreras() {
        return Collections.unmodifiableList(carreras);
    }
}
