package com.undec.acreditacion.domain.services;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.undec.acreditacion.domain.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorRegistroUsuariosAteneaPdfServiceTest {

    @Test
    @DisplayName("Debe generar PDF oficial RG-003 con membrete UNdeC, metadatos, usuarios y matriz de permisos visibles")
    void debeGenerarPdfRg003Correctamente() throws Exception {
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
        assertTrue(pdf.length > 1000);

        StringBuilder textoExtraido = new StringBuilder();
        try (PdfReader reader = new PdfReader(pdf)) {
            assertTrue(reader.getNumberOfPages() >= 2, "Debe incluir al menos 2 páginas (Usuarios + Matriz de Permisos)");
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                textoExtraido.append(extractor.getTextFromPage(page)).append("\n");
            }
        }

        String contenido = textoExtraido.toString();
        assertTrue(contenido.contains("RG-003: Registro de permisos de usuarios habilitados"));
        assertTrue(contenido.contains("Versión N° 01"));
        assertTrue(contenido.contains("Área de Evaluación y Acreditación (AEA)"));
        assertTrue(contenido.contains("Ingeniería Agronómica"));
        assertTrue(contenido.contains("Grado - en funcionamiento - 1era Fase"));
        assertTrue(contenido.contains("2026"));
        assertTrue(contenido.contains("EX-2026-12345678-APN-CONEAU#ME"));
        assertTrue(contenido.contains("SECCIÓN I"));
        assertTrue(contenido.contains("SECCIÓN II"));
        assertTrue(contenido.contains("Valeria"));
        assertTrue(contenido.contains("Salerno"));
        assertTrue(contenido.contains("Infraestructura y equipamiento"));
    }
}
