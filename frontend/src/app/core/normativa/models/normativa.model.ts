export type EstadoSolicitudNormativa =
  | 'PENDIENTE_SOLICITUD'
  | 'SOLICITADA'
  | 'EN_CARGA_BORRADOR'
  | 'ENVIADA_A_REVISION'
  | 'OBSERVADA'
  | 'COMPLETADA';

export type CumplimientoPlazo = 'EN_TERMINO' | 'FUERA_DE_TERMINO';

export type CarpetaMinioDestino =
  | 'NORMATIVA'
  | 'NORMATIVA_ACTAS'
  | 'EVALUACIONES_ANTERIORES';

export type TipoDocumentoNormativa =
  | 'RM_VALIDEZ_NACIONAL'
  | 'CREACION_CARRERA'
  | 'REGLAMENTO_CARRERA'
  | 'ORDENANZA_PLAN_ESTUDIOS'
  | 'PLAN_ESTUDIOS_VIGENTE'
  | 'APROBACION_PROGRAMAS_ANALITICOS'
  | 'RESOLUCION_DIRECTOR_CARRERA'
  | 'RESOLUCION_COMISION_CURRICULAR'
  | 'PLAN_DESARROLLO_CARRERA'
  | 'PLAN_MEJORAS_PREVIO'
  | 'INFORME_CUMPLIMIENTO_PREVIO'
  | 'ACTA_COMISION_CURRICULAR'
  | 'RM_ESTANDARES_CARRERA'
  | 'RM_INCLUSION_ART_43'
  | 'RESOLUCION_CONEAU_ANTERIOR';

export interface BadgePlazoDto {
  estado: string;
  diasRestantes: number;
  etiqueta: string;
  cssClass: string;
}

export interface DocumentoNormativaDto {
  id: string;
  solicitudNormativaId: string;
  procesoId: string;
  tipoDocumento: TipoDocumentoNormativa;
  carpetaDestino: CarpetaMinioDestino;
  nombreOriginal: string;
  storagePath: string;
  mimeType: string;
  tamanioBytes: number;
  esBorrador: boolean;
  observado: boolean;
  motivoObservacion?: string;
  esObligatorio: boolean;
  creadoEn: string;
  actualizadoEn: string;
}

export interface NormativaDetalleDto {
  id: string;
  procesoId: string;
  actividadId: string;
  estado: EstadoSolicitudNormativa;
  fechaSolicitud?: string;
  fechaLimiteEntrega?: string;
  diasHabilesPlazo: number;
  badgePlazo?: BadgePlazoDto;
  porcentajeAvance: number;
  documentosCargados: number;
  documentosRequeridos: number;
  observacionesSolicitud?: string;
  observacionesRevision?: string;
  documentos: DocumentoNormativaDto[];
  documentosFaltantes: TipoDocumentoNormativa[];
  carreraEsNueva: boolean;
  carreraTieneHistorial: boolean;
}

export interface SolicitarNormativaRequest {
  observaciones?: string;
  plazoDiasHabiles?: number;
}

export interface DocumentoObservadoDto {
  documentoId: string;
  motivo: string;
}

export interface RevisarNormativaRequest {
  decision: 'APROBADO' | 'REQUIERE_CORRECCIONES';
  observaciones?: string;
  documentosObservados?: DocumentoObservadoDto[];
}

export interface TipoDocumentoMeta {
  codigo: TipoDocumentoNormativa;
  titulo: string;
  descripcion: string;
  carpeta: CarpetaMinioDestino;
  carpetaLabel: string;
}

export function formatearEstadoNormativa(estado?: string | null): string {
  if (!estado) return '-';
  switch (estado) {
    case 'EN_CARGA_BORRADOR':
      return 'EN EJECUCIÓN';
    case 'PENDIENTE_SOLICITUD':
      return 'PENDIENTE DE SOLICITUD';
    case 'SOLICITADA':
      return 'SOLICITADA';
    case 'ENVIADA_A_REVISION':
      return 'EN REVISIÓN';
    case 'OBSERVADA':
      return 'OBSERVADA';
    case 'COMPLETADA':
      return 'APROBADA';
    default:
      return estado.replace(/_/g, ' ');
  }
}

export const METADATOS_DOCUMENTOS_NORMATIVA: Partial<Record<TipoDocumentoNormativa, TipoDocumentoMeta>> = {
  RM_VALIDEZ_NACIONAL: {
    codigo: 'RM_VALIDEZ_NACIONAL',
    titulo: 'Resolución de reconocimiento oficial y validez nacional de título',
    descripcion: 'Resolución Ministerial de reconocimiento oficial y validez nacional del título universitario (sólo carreras en funcionamiento).',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  CREACION_CARRERA: {
    codigo: 'CREACION_CARRERA',
    titulo: 'Acto administrativo de creación de la carrera',
    descripcion: 'Resolución u Ordenanza del Consejo Superior que crea formalmente la carrera en la universidad.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  REGLAMENTO_CARRERA: {
    codigo: 'REGLAMENTO_CARRERA',
    titulo: 'Acto administrativo del reglamento de la carrera',
    descripcion: 'Reglamento académico y operativo (Aplica exclusivamente a Posgrado. No requerido para carreras de Grado).',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  ORDENANZA_PLAN_ESTUDIOS: {
    codigo: 'ORDENANZA_PLAN_ESTUDIOS',
    titulo: 'Acto administrativo que establece el plan de estudios vigente y modificatorias',
    descripcion: 'Ordenanza o Resolución del Consejo Superior con el texto ordenado del plan de estudios y sus modificaciones.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  PLAN_ESTUDIOS_VIGENTE: {
    codigo: 'PLAN_ESTUDIOS_VIGENTE',
    titulo: 'Acto administrativo que establece el plan de estudios vigente y modificatorias',
    descripcion: 'Texto ordenado del plan de estudios vigente aprobado por el Consejo Superior.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  APROBACION_PROGRAMAS_ANALITICOS: {
    codigo: 'APROBACION_PROGRAMAS_ANALITICOS',
    titulo: 'Acto administrativo que aprueba los programas analíticos',
    descripcion: 'Resolución formal de aprobación de los programas analíticos de las asignaturas (sólo carreras en funcionamiento).',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  RESOLUCION_DIRECTOR_CARRERA: {
    codigo: 'RESOLUCION_DIRECTOR_CARRERA',
    titulo: 'Acto administrativo de designación del Director/a de la carrera',
    descripcion: 'Acto formal de designación de la máxima autoridad unipersonal de gestión académica de la carrera.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  RESOLUCION_COMISION_CURRICULAR: {
    codigo: 'RESOLUCION_COMISION_CURRICULAR',
    titulo: 'Acto administrativo que conforma la Comisión Curricular',
    descripcion: 'Acto administrativo que conforma la Comisión Curricular (carreras de grado) o Comité Académico (posgrado).',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  PLAN_DESARROLLO_CARRERA: {
    codigo: 'PLAN_DESARROLLO_CARRERA',
    titulo: 'Acto administrativo de aprobación del Plan de Desarrollo de la carrera',
    descripcion: 'Resolución de aprobación del plan estratégico de desarrollo institucional y académico de la titulación.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  ACTA_COMISION_CURRICULAR: {
    codigo: 'ACTA_COMISION_CURRICULAR',
    titulo: 'Actas de reuniones de Comisión Curricular o Comité Académico',
    descripcion: 'Recomendación de AEA a Dirección de Carrera: tener disponibles las actas desde la última instancia de acreditación.',
    carpeta: 'NORMATIVA_ACTAS',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/Actas/'
  },
  RM_ESTANDARES_CARRERA: {
    codigo: 'RM_ESTANDARES_CARRERA',
    titulo: 'Resolución Ministerial de Estándares (Referencia curricular)',
    descripcion: 'Resolución del Ministerio de Educación que fija estándares de acreditación para la titulación.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  RM_INCLUSION_ART_43: {
    codigo: 'RM_INCLUSION_ART_43',
    titulo: 'Resolución Ministerial de Inclusión en Art. 43 LES',
    descripcion: 'Normativa ministerial que declara de interés público la titulación.',
    carpeta: 'NORMATIVA',
    carpetaLabel: '01-Convocatoria y formalizacion CONEAU/Normativa/'
  },
  RESOLUCION_CONEAU_ANTERIOR: {
    codigo: 'RESOLUCION_CONEAU_ANTERIOR',
    titulo: 'Resolución CONEAU de Acreditación Previa',
    descripcion: 'Resolución CONEAU del ciclo anterior para verificación de antecedentes.',
    carpeta: 'EVALUACIONES_ANTERIORES',
    carpetaLabel: '02-Evaluaciones anteriores CONEAU/'
  }
};
