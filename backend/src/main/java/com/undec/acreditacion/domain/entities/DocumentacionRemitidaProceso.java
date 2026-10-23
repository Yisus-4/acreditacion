package com.undec.acreditacion.domain.entities;

import java.util.List;

/**
 * Documentación que el AEA remite al informar sobre el proceso de acreditación
 * (Actividad 1 del procedimiento oficial, solo GRADO). Las opciones dependen del caso:
 * carrera nueva, carrera en funcionamiento (convocatoria 1era/2da Fase) o seguimiento
 * de avance de Planes de Mejora (PT-003).
 */
public enum DocumentacionRemitidaProceso {
    RESOLUCION_CONVOCATORIA_CONEAU("Resolución de convocatoria pública de CONEAU"),
    RM_ESTANDARES("Resolución Ministerial que fija los estándares de la carrera"),
    RM_INCLUSION_ART_43_LES("Resolución Ministerial de inclusión en el art. 43 de la LES"),
    ORDENANZAS_NOTAS_TECNICAS("Ordenanzas que fijan los procedimientos involucrados y notas técnicas"),
    RESOLUCION_ACREDITACION_COMPROMISOS("Resolución de acreditación (o dictamen) con compromisos y recomendaciones"),
    RESOLUCIONES_INFORME_SEGUIMIENTO("Resoluciones de Informe de Seguimiento de avance e Informe correspondiente"),
    PLANES_MEJORA_PRESENTADOS("Planes de mejora presentados ante CONEAU (última versión)"),
    // Fuentes analizadas en Actividad 4 (Identificar información y documentación de interés)
    DOC_PROPIA_AREA("Documentación propia del área"),
    PROCESOS_ACREDITACION_CARRERAS("Procesos de acreditación de carreras"),
    EVALUACION_FUNCIONES("Evaluación de funciones"),
    EVALUACION_INSTITUCIONAL("Evaluación institucional"),
    // Ítems verificados en Actividad 5 (Verificar y/o actualizar la información institucional precargada - IT-002 ap. 3.1)
    INFO_INSTITUCION_VERIFICADA("Administrar institución: verificados nombre oficial (UNdeC), máxima autoridad, administrador institucional y dirección de sede central"),
    OFERTA_ACADEMICA_RG004_VERIFICADA("Administrar carreras: verificadas carreras de grado y posgrado de la unidad académica según RG-004: Oferta académica UNdeC");

    private final String etiqueta;

    DocumentacionRemitidaProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static List<DocumentacionRemitidaProceso> opcionesPara(TipoProcesoAcreditacion tipoProceso, TipoPlanTrabajo tipoPlan) {
        if (tipoPlan == TipoPlanTrabajo.PT_003_SEGUIMIENTO) {
            return List.of(ORDENANZAS_NOTAS_TECNICAS, RESOLUCION_ACREDITACION_COMPROMISOS,
                    RESOLUCIONES_INFORME_SEGUIMIENTO, PLANES_MEJORA_PRESENTADOS);
        }
        if (tipoProceso == TipoProcesoAcreditacion.NUEVA) {
            return List.of(RM_ESTANDARES, RM_INCLUSION_ART_43_LES, ORDENANZAS_NOTAS_TECNICAS);
        }
        return List.of(RESOLUCION_CONVOCATORIA_CONEAU, RM_ESTANDARES, RM_INCLUSION_ART_43_LES, ORDENANZAS_NOTAS_TECNICAS);
    }
}
