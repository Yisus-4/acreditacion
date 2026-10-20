package com.undec.acreditacion.domain.entities;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public final class CalculadorDiasHabiles {

    private CalculadorDiasHabiles() {
        // Utility class
    }

    public static LocalDate calcularFechaLimite(LocalDate fechaInicio, int diasHabiles) {
        Objects.requireNonNull(fechaInicio, "La fecha de inicio es obligatoria");
        if (diasHabiles <= 0) {
            throw new IllegalArgumentException("Los días hábiles deben ser mayores a cero");
        }
        LocalDate cursor = fechaInicio;
        int contados = 0;
        while (contados < diasHabiles) {
            cursor = cursor.plusDays(1);
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                contados++;
            }
        }
        return cursor;
    }

    public static int calcularDiasHabilesRestantes(LocalDate fechaActual, LocalDate fechaLimite) {
        Objects.requireNonNull(fechaActual, "La fecha actual es obligatoria");
        Objects.requireNonNull(fechaLimite, "La fecha límite es obligatoria");

        if (fechaActual.isEqual(fechaLimite)) {
            return 0;
        }

        if (fechaActual.isBefore(fechaLimite)) {
            LocalDate cursor = fechaActual;
            int dias = 0;
            while (cursor.isBefore(fechaLimite)) {
                cursor = cursor.plusDays(1);
                if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    dias++;
                }
            }
            return dias;
        } else {
            LocalDate cursor = fechaLimite;
            int dias = 0;
            while (cursor.isBefore(fechaActual)) {
                cursor = cursor.plusDays(1);
                if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    dias++;
                }
            }
            return -dias;
        }
    }
}
