package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class RegistroConvocatoria {

    private final UUID id;
    private final UUID procesoId;
    private String nroResolucionConeau;
    private LocalDate fechaInicio;
    private LocalDate fechaCierre;
    private final String observaciones;
    private String rmEstandares;
    private String areaDisciplinaria;
    private String modalidadCarrera;
    private String estadoCarrera;
    private FaseEvaluacion faseEvaluacion;
    private LocalDate fechaLimitePresentacionConeau;
    private LocalDate fechaFormalizacionElectronica;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static RegistroConvocatoria crear(UUID procesoId,
                                             String nroResolucionConeau,
                                             LocalDate fechaInicio,
                                             LocalDate fechaCierre,
                                             String observaciones) {
        Instant now = Instant.now();
        return new RegistroConvocatoria(
                UUID.randomUUID(),
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                null,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                FaseEvaluacion.PRIMERA_FASE,
                null,
                now,
                now
        );
    }

    public static RegistroConvocatoria crear(UUID procesoId,
                                             String nroResolucionConeau,
                                             LocalDate fechaInicio,
                                             LocalDate fechaCierre,
                                             String observaciones,
                                             String rmEstandares,
                                             FaseEvaluacion faseEvaluacion,
                                             LocalDate fechaLimitePresentacionConeau) {
        Instant now = Instant.now();
        return new RegistroConvocatoria(
                UUID.randomUUID(),
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                now,
                now
        );
    }

    public static RegistroConvocatoria crear(UUID procesoId,
                                             String nroResolucionConeau,
                                             LocalDate fechaInicio,
                                             LocalDate fechaCierre,
                                             String observaciones,
                                             String rmEstandares,
                                             String areaDisciplinaria,
                                             String modalidadCarrera,
                                             String estadoCarrera,
                                             FaseEvaluacion faseEvaluacion,
                                             LocalDate fechaLimitePresentacionConeau) {
        Instant now = Instant.now();
        return new RegistroConvocatoria(
                UUID.randomUUID(),
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                areaDisciplinaria,
                modalidadCarrera,
                estadoCarrera,
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                now,
                now
        );
    }

    public static RegistroConvocatoria rehydrate(UUID id,
                                                 UUID procesoId,
                                                 String nroResolucionConeau,
                                                 LocalDate fechaInicio,
                                                 LocalDate fechaCierre,
                                                 String observaciones,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new RegistroConvocatoria(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                null,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                FaseEvaluacion.PRIMERA_FASE,
                null,
                creadoEn,
                actualizadoEn
        );
    }

    public static RegistroConvocatoria rehydrate(UUID id,
                                                 UUID procesoId,
                                                 String nroResolucionConeau,
                                                 LocalDate fechaInicio,
                                                 LocalDate fechaCierre,
                                                 String observaciones,
                                                 String rmEstandares,
                                                 FaseEvaluacion faseEvaluacion,
                                                 LocalDate fechaLimitePresentacionConeau,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new RegistroConvocatoria(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                creadoEn,
                actualizadoEn
        );
    }

    public static RegistroConvocatoria rehydrate(UUID id,
                                                 UUID procesoId,
                                                 String nroResolucionConeau,
                                                 LocalDate fechaInicio,
                                                 LocalDate fechaCierre,
                                                 String observaciones,
                                                 String rmEstandares,
                                                 String areaDisciplinaria,
                                                 String modalidadCarrera,
                                                 String estadoCarrera,
                                                 FaseEvaluacion faseEvaluacion,
                                                 LocalDate fechaLimitePresentacionConeau,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new RegistroConvocatoria(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                areaDisciplinaria,
                modalidadCarrera,
                estadoCarrera,
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                creadoEn,
                actualizadoEn
        );
    }

    public RegistroConvocatoria(UUID id,
                                UUID procesoId,
                                String nroResolucionConeau,
                                LocalDate fechaInicio,
                                LocalDate fechaCierre,
                                String observaciones,
                                String rmEstandares,
                                FaseEvaluacion faseEvaluacion,
                                LocalDate fechaLimitePresentacionConeau,
                                Instant creadoEn,
                                Instant actualizadoEn) {
        this(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                creadoEn,
                actualizadoEn
        );
    }

    public RegistroConvocatoria(UUID id,
                                UUID procesoId,
                                String nroResolucionConeau,
                                LocalDate fechaInicio,
                                LocalDate fechaCierre,
                                String observaciones,
                                String rmEstandares,
                                String areaDisciplinaria,
                                String modalidadCarrera,
                                String estadoCarrera,
                                FaseEvaluacion faseEvaluacion,
                                LocalDate fechaLimitePresentacionConeau,
                                Instant creadoEn,
                                Instant actualizadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID del registro de convocatoria es obligatorio");
        }
        if (procesoId == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }

        String estadoNormalizado = normalizarEstadoCarrera(estadoCarrera);
        if ("EN_FUNCIONAMIENTO".equalsIgnoreCase(estadoNormalizado)
                && (nroResolucionConeau == null || nroResolucionConeau.trim().isEmpty())) {
            throw new DomainValidationException("El número de resolución CONEAU es obligatorio");
        }

        if (fechaInicio == null) {
            fechaInicio = LocalDate.now();
        }
        if (fechaCierre == null) {
            throw new DomainValidationException("La fecha de cierre de la convocatoria es obligatoria");
        }
        if (fechaCierre.isBefore(fechaInicio)) {
            throw new DomainValidationException("La fecha de cierre CONEAU no puede ser anterior a la fecha de inicio");
        }
        if (fechaLimitePresentacionConeau != null && fechaLimitePresentacionConeau.isBefore(fechaCierre)) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU no puede ser anterior a la fecha de cierre de la convocatoria");
        }

        this.id = id;
        this.procesoId = procesoId;
        this.nroResolucionConeau = nroResolucionConeau != null && !nroResolucionConeau.trim().isEmpty()
                ? nroResolucionConeau.trim()
                : null;
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
        this.observaciones = observaciones != null ? observaciones.trim() : null;
        this.rmEstandares = rmEstandares != null && !rmEstandares.trim().isEmpty() ? rmEstandares.trim() : null;
        this.areaDisciplinaria = areaDisciplinaria != null && !areaDisciplinaria.trim().isEmpty()
                ? areaDisciplinaria.trim()
                : null;
        this.modalidadCarrera = normalizarModalidad(modalidadCarrera);
        this.estadoCarrera = estadoNormalizado;
        this.faseEvaluacion = faseEvaluacion != null ? faseEvaluacion : FaseEvaluacion.PRIMERA_FASE;
        this.fechaLimitePresentacionConeau = fechaLimitePresentacionConeau;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void completarDatosConeau(String rmEstandares, FaseEvaluacion faseEvaluacion, LocalDate fechaLimiteConeau) {
        completarDatosRg001(
                null,
                null,
                null,
                null,
                rmEstandares,
                null,
                null,
                faseEvaluacion,
                fechaLimiteConeau
        );
    }

    public void completarDatosRg001(LocalDate nuevaFechaInicio,
                                    LocalDate nuevaFechaCierre,
                                    String nuevoEstadoCarrera,
                                    String nuevoNroResolucionConeau,
                                    String nuevoRmEstandares,
                                    String nuevaAreaDisciplinaria,
                                    String nuevaModalidadCarrera,
                                    FaseEvaluacion nuevaFaseEvaluacion,
                                    LocalDate fechaLimiteConeau) {
        if (nuevaFaseEvaluacion == null) {
            throw new DomainValidationException("La fase de evaluación es obligatoria");
        }
        if (fechaLimiteConeau == null) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU es obligatoria al completar los datos CONEAU");
        }

        LocalDate inicioEfectivo = nuevaFechaInicio != null ? nuevaFechaInicio : this.fechaInicio;
        LocalDate cierreEfectivo = nuevaFechaCierre != null ? nuevaFechaCierre : this.fechaCierre;

        if (cierreEfectivo.isBefore(inicioEfectivo)) {
            throw new DomainValidationException("La fecha de cierre CONEAU no puede ser anterior a la fecha de inicio");
        }
        if (fechaLimiteConeau.isBefore(cierreEfectivo)) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU no puede ser anterior a la fecha de cierre de la convocatoria");
        }

        String estadoEfectivo = (nuevoEstadoCarrera != null && !nuevoEstadoCarrera.trim().isEmpty())
                ? normalizarEstadoCarrera(nuevoEstadoCarrera)
                : this.estadoCarrera;

        String resolucionEfectiva;
        if ("NUEVA".equalsIgnoreCase(estadoEfectivo)) {
            if (nuevoNroResolucionConeau != null) {
                resolucionEfectiva = !nuevoNroResolucionConeau.trim().isEmpty() ? nuevoNroResolucionConeau.trim() : null;
            } else {
                resolucionEfectiva = this.nroResolucionConeau;
            }
        } else {
            resolucionEfectiva = (nuevoNroResolucionConeau != null)
                    ? (!nuevoNroResolucionConeau.trim().isEmpty() ? nuevoNroResolucionConeau.trim() : null)
                    : this.nroResolucionConeau;
            if (resolucionEfectiva == null || resolucionEfectiva.isBlank()) {
                throw new DomainValidationException("El número de resolución CONEAU es obligatorio para carreras en funcionamiento");
            }
        }

        this.fechaInicio = inicioEfectivo;
        this.fechaCierre = cierreEfectivo;
        this.estadoCarrera = estadoEfectivo;
        this.nroResolucionConeau = resolucionEfectiva;
        this.rmEstandares = nuevoRmEstandares != null && !nuevoRmEstandares.trim().isEmpty()
                ? nuevoRmEstandares.trim()
                : null;
        if (nuevaAreaDisciplinaria != null && !nuevaAreaDisciplinaria.trim().isEmpty()) {
            this.areaDisciplinaria = nuevaAreaDisciplinaria.trim();
        }
        if (nuevaModalidadCarrera != null && !nuevaModalidadCarrera.trim().isEmpty()) {
            this.modalidadCarrera = normalizarModalidad(nuevaModalidadCarrera);
        }
        this.faseEvaluacion = nuevaFaseEvaluacion;
        this.fechaLimitePresentacionConeau = fechaLimiteConeau;
        this.actualizadoEn = Instant.now();
    }

    private static String normalizarEstadoCarrera(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "EN_FUNCIONAMIENTO";
        }
        String upper = valor.trim().toUpperCase();
        if ("NUEVA".equals(upper)) {
            return "NUEVA";
        }
        return "EN_FUNCIONAMIENTO";
    }

    private static String normalizarModalidad(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "PRESENCIAL";
        }
        String upper = valor.trim().toUpperCase();
        if ("A_DISTANCIA".equals(upper) || "DISTANCIA".equals(upper) || "A DISTANCIA".equals(upper)) {
            return "A_DISTANCIA";
        }
        return "PRESENCIAL";
    }

    public void registrarFechaFormalizacionElectronica(LocalDate fecha) {
        this.fechaFormalizacionElectronica = fecha;
        this.actualizadoEn = Instant.now();
    }

    public LocalDate getFechaFormalizacionElectronica() {
        return fechaFormalizacionElectronica;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
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

    public String getObservaciones() {
        return observaciones;
    }

    public String getRmEstandares() {
        return rmEstandares;
    }

    public String getAreaDisciplinaria() {
        return areaDisciplinaria;
    }

    public String getModalidadCarrera() {
        return modalidadCarrera;
    }

    public String getEstadoCarrera() {
        return estadoCarrera;
    }

    public FaseEvaluacion getFaseEvaluacion() {
        return faseEvaluacion;
    }

    public LocalDate getFechaLimitePresentacionConeau() {
        return fechaLimitePresentacionConeau;
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
        if (o == null || getClass() != o.getClass()) return false;
        RegistroConvocatoria that = (RegistroConvocatoria) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
