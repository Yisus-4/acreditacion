package com.undec.acreditacion.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MatrizNormativaTest {

    @Test
    @DisplayName("Carrera NUEVA exige exactamente los 5 documentos base")
    void testCarreraNueva_exige5DocumentosBase() {
        Set<TipoDocumentoNormativa> requeridos = MatrizNormativa.obtenerRequeridos(true, false);

        assertEquals(5, requeridos.size());
        assertTrue(requeridos.contains(TipoDocumentoNormativa.CREACION_CARRERA));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA));
        assertFalse(requeridos.contains(TipoDocumentoNormativa.ACTA_COMISION_CURRICULAR));
    }

    @Test
    @DisplayName("Carrera EN_FUNCIONAMIENTO sin antecedentes CONEAU exige los 7 documentos requeridos")
    void testCarreraEnFuncionamiento_sinHistorial_exige7Documentos() {
        Set<TipoDocumentoNormativa> requeridos = MatrizNormativa.obtenerRequeridos(false, false);
        assertEquals(7, requeridos.size());
        assertTrue(requeridos.contains(TipoDocumentoNormativa.RM_VALIDEZ_NACIONAL));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.APROBACION_PROGRAMAS_ANALITICOS));
    }

    @Test
    @DisplayName("Carrera EN_FUNCIONAMIENTO con historial CONEAU exige los 7 documentos de funcionamiento")
    void testCarreraEnFuncionamiento_conHistorial_exige7Documentos() {
        Set<TipoDocumentoNormativa> requeridos = MatrizNormativa.obtenerRequeridos(false, true);

        assertEquals(7, requeridos.size());
        assertTrue(requeridos.contains(TipoDocumentoNormativa.RM_VALIDEZ_NACIONAL));
        assertTrue(requeridos.contains(TipoDocumentoNormativa.APROBACION_PROGRAMAS_ANALITICOS));
        assertFalse(requeridos.contains(TipoDocumentoNormativa.ACTA_COMISION_CURRICULAR));
    }

    @Test
    @DisplayName("obtenerFaltantes calcula correctamente faltantes para carrera nueva incompleta")
    void testObtenerFaltantes_carreraNuevaIncompleta() {
        List<TipoDocumentoNormativa> cargados = List.of(
            TipoDocumentoNormativa.CREACION_CARRERA,
            TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA
        );

        List<TipoDocumentoNormativa> faltantes = MatrizNormativa.obtenerFaltantes(cargados, true, false);

        assertEquals(2, faltantes.size());
        assertTrue(faltantes.contains(TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA));
        assertTrue(faltantes.contains(TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR));
        assertFalse(MatrizNormativa.esCompleta(cargados, true, false));
    }

    @Test
    @DisplayName("esCompleta retorna true cuando están todos los obligatorios aunque no tenga ACTA_COMISION_CURRICULAR")
    void testEsCompleta_completaSinActas() {
        List<TipoDocumentoNormativa> cargados = List.of(
            TipoDocumentoNormativa.CREACION_CARRERA,
            TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA,
            TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR,
            TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA
        );

        assertTrue(MatrizNormativa.esCompleta(cargados, true, false));
        assertTrue(MatrizNormativa.obtenerFaltantes(cargados, true, false).isEmpty());
    }

    @Test
    @DisplayName("Lista de cargados nula o vacía devuelve todos los requeridos")
    void testObtenerFaltantes_listaVacia() {
        List<TipoDocumentoNormativa> faltantes = MatrizNormativa.obtenerFaltantes(Collections.emptyList(), true, false);
        assertEquals(5, faltantes.size());
    }
}
