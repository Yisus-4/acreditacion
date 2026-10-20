package com.undec.acreditacion.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorDiasHabilesTest {

    @Test
    @DisplayName("Inicio un jueves con 5 días hábiles debe resultar en el jueves siguiente (salta fin de semana)")
    void testCalcularFechaLimite_inicioJueves5DiasHabiles() {
        LocalDate jueves15 = LocalDate.of(2026, 10, 15);
        LocalDate fechaLimite = CalculadorDiasHabiles.calcularFechaLimite(jueves15, 5);

        assertEquals(LocalDate.of(2026, 10, 22), fechaLimite);
    }

    @Test
    @DisplayName("Inicio un viernes con 1 día hábil debe resultar en el lunes siguiente")
    void testCalcularFechaLimite_inicioViernes1Dia() {
        LocalDate viernes16 = LocalDate.of(2026, 10, 16);
        LocalDate fechaLimite = CalculadorDiasHabiles.calcularFechaLimite(viernes16, 1);

        assertEquals(LocalDate.of(2026, 10, 19), fechaLimite);
    }

    @Test
    @DisplayName("Días hábiles <= 0 debe arrojar IllegalArgumentException")
    void testCalcularFechaLimite_diasInvalidos() {
        LocalDate hoy = LocalDate.of(2026, 10, 15);
        assertThrows(IllegalArgumentException.class, () -> CalculadorDiasHabiles.calcularFechaLimite(hoy, 0));
        assertThrows(IllegalArgumentException.class, () -> CalculadorDiasHabiles.calcularFechaLimite(hoy, -2));
    }

    @Test
    @DisplayName("Fecha nula debe arrojar NullPointerException")
    void testCalcularFechaLimite_fechaNula() {
        assertThrows(NullPointerException.class, () -> CalculadorDiasHabiles.calcularFechaLimite(null, 5));
    }

    @Test
    @DisplayName("Calcular días hábiles restantes entre fecha actual y fecha límite")
    void testCalcularDiasHabilesRestantes() {
        LocalDate limite = LocalDate.of(2026, 10, 22);
        
        // El mismo día límite quedan 0 días restantes
        assertEquals(0, CalculadorDiasHabiles.calcularDiasHabilesRestantes(limite, limite));

        // El miércoles 21 queda 1 día hábil
        assertEquals(1, CalculadorDiasHabiles.calcularDiasHabilesRestantes(LocalDate.of(2026, 10, 21), limite));

        // El jueves 15 quedan 5 días hábiles
        assertEquals(5, CalculadorDiasHabiles.calcularDiasHabilesRestantes(LocalDate.of(2026, 10, 15), limite));

        // Si ya pasó el plazo (viernes 23) debe retornar negativo (-1)
        assertEquals(-1, CalculadorDiasHabiles.calcularDiasHabilesRestantes(LocalDate.of(2026, 10, 23), limite));
    }
}
