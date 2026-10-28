package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorRegistroUsuariosAteneaPdfServiceTest {

    @Test
    @DisplayName("Debe generar PDF de RG-003 con cabeceras oficiales UNdeC, metadatos y matriz de permisos")
    void debeGenerarPdfRg003Correctamente() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.of(2026, 10, 26));

        registro.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Valeria", "Castro", "27-32111222-3",
                "vcastro@undec.edu.ar", "Área de Evaluación y Acreditación (AEA)",
                "Equipo Técnico AEA", TipoAccesoAtenea.ADMINISTRACION,
                "Formulario Integral ATENEA", false, true, true
        ));

        registro.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.SGI, "Carlos", "Perez", "20-25444333-1",
                "cperez@undec.edu.ar", "Secretaría de Gestión Institucional",
                "Secretario SGI", TipoAccesoAtenea.VISUALIZACION,
                "Dimensión Institucional", false, true, true
        ));

        registro.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.COMISION_AUTOEVALUACION, "Julio Luis", "Salerno", "20-08386710-9",
                "jsalerno@undec.edu.ar", "Comisión de Autoevaluación",
                "Director de Carrera", TipoAccesoAtenea.CARGA_Y_VISUALIZACION,
                "Formulario Carrera", false, true, true
        ));

        byte[] pdf = GeneradorRegistroUsuariosAteneaPdfService.generarPdf(
                registro, "Ingeniería Agronómica", "Grado - en funcionamiento - 1era Fase", "2026", "EX-2026-12345678-APN-CONEAU#ME"
        );

        assertNotNull(pdf);
        assertTrue(pdf.length > 50);

        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(contenido.contains("RG-003: Registro de permisos de usuarios habilitados"));
        assertTrue(contenido.contains("Versión N° 01"));
        assertTrue(contenido.contains("Área de Evaluación y Acreditación (AEA)"));
        assertTrue(contenido.contains("Ingeniería Agronómica"));
        assertTrue(contenido.contains("Grado - en funcionamiento - 1era Fase"));
        assertTrue(contenido.contains("2026"));
        assertTrue(contenido.contains("EX-2026-12345678-APN-CONEAU#ME"));
        assertTrue(contenido.contains("SECCION I: TABLA DE USUARIOS HABILITADOS EN CONEAU GLOBAL ATENEA"));
        assertTrue(contenido.contains("SECCION II: MATRIZ DE PERMISOS DEL SISTEMA CONEAU GLOBAL ATENEA"));
        assertTrue(contenido.contains("Infraestructura y equipamiento para enseñanza y aprendizaje"));
        assertTrue(contenido.contains("Estructura curricular, organización y gestión de la carrera"));
        assertTrue(contenido.contains("Valeria"));
        assertTrue(contenido.contains("Salerno"));
    }
}
