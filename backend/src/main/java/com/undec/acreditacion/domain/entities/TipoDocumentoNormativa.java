package com.undec.acreditacion.domain.entities;
public enum TipoDocumentoNormativa {
    RM_VALIDEZ_NACIONAL(CarpetaMinioDestino.NORMATIVA),
    CREACION_CARRERA(CarpetaMinioDestino.NORMATIVA),
    REGLAMENTO_CARRERA(CarpetaMinioDestino.NORMATIVA),
    ORDENANZA_PLAN_ESTUDIOS(CarpetaMinioDestino.NORMATIVA),
    PLAN_ESTUDIOS_VIGENTE(CarpetaMinioDestino.NORMATIVA),
    APROBACION_PROGRAMAS_ANALITICOS(CarpetaMinioDestino.NORMATIVA),
    RESOLUCION_DIRECTOR_CARRERA(CarpetaMinioDestino.NORMATIVA),
    RESOLUCION_COMISION_CURRICULAR(CarpetaMinioDestino.NORMATIVA),
    PLAN_DESARROLLO_CARRERA(CarpetaMinioDestino.NORMATIVA),
    PLAN_MEJORAS_PREVIO(CarpetaMinioDestino.EVALUACIONES_ANTERIORES),
    INFORME_CUMPLIMIENTO_PREVIO(CarpetaMinioDestino.EVALUACIONES_ANTERIORES),
    ACTA_COMISION_CURRICULAR(CarpetaMinioDestino.NORMATIVA_ACTAS),
    RM_ESTANDARES_CARRERA(CarpetaMinioDestino.NORMATIVA),
    RM_INCLUSION_ART_43(CarpetaMinioDestino.NORMATIVA),
    RESOLUCION_CONEAU_ANTERIOR(CarpetaMinioDestino.EVALUACIONES_ANTERIORES);

    private final CarpetaMinioDestino carpetaDestino;

    TipoDocumentoNormativa(CarpetaMinioDestino carpetaDestino) {
        this.carpetaDestino = carpetaDestino;
    }

    public CarpetaMinioDestino getCarpetaDestino() {
        return carpetaDestino;
    }
}
