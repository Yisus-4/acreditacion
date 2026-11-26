package com.undec.acreditacion.domain.entities;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Situación curricular de una asociación Asignatura–Plan (Sección 3.7 / RN09):
 * <ul>
 *   <li>{@link #ACTIVA}: Desarrolla clases (Sí) y evalúa exámenes finales (Sí).</li>
 *   <li>{@link #SOLO_EXAMENES_FINALES}: Desarrolla clases (No) y evalúa exámenes finales (Sí).</li>
 *   <li>{@link #INACTIVA}: Desarrolla clases (No) y evalúa exámenes finales (No).</li>
 * </ul>
 */
public enum EstadoDictadoActividad {
    ACTIVA(true, true),
    SOLO_EXAMENES_FINALES(false, true),
    INACTIVA(false, false);

    private final boolean desarrollaClases;
    private final boolean evaluaFinales;

    EstadoDictadoActividad(boolean desarrollaClases, boolean evaluaFinales) {
        this.desarrollaClases = desarrollaClases;
        this.evaluaFinales = evaluaFinales;
    }

    public boolean desarrollaClases() {
        return desarrollaClases;
    }

    public boolean evaluaFinales() {
        return evaluaFinales;
    }

    public boolean esInactiva() {
        return this == INACTIVA;
    }

    @JsonCreator
    public static EstadoDictadoActividad fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El estado de dictado de la asignatura no puede estar vacío.");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVA", "SE_DICTA" -> ACTIVA;
            case "SOLO_EXAMENES_FINALES" -> SOLO_EXAMENES_FINALES;
            case "INACTIVA", "NO_SE_DICTA" -> INACTIVA;
            default -> throw new IllegalArgumentException("Estado de dictado inválido: " + raw);
        };
    }
}
