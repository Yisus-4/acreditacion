export type NivelCarrera = 'GRADO';
export type TipoProceso = 'NUEVA' | 'EN_FUNCIONAMIENTO';
export type EstadoProceso = 'INICIADO' | 'EN_AUTOEVALUACION' | 'FINALIZADO' | 'CANCELADO';
export type TipoPlanTrabajo = 'PT_001_ACREDITACION' | 'PT_003_SEGUIMIENTO';
export type EstadoPlanTrabajo = 'ACTIVO' | 'FINALIZADO';
export type EstadoActividad = 'PENDIENTE' | 'EN_PROGRESO' | 'COMPLETADA' | 'CANCELADA';
export type TipoDocumento =
  | 'NORMATIVA_CONVOCATORIA'
  | 'ACTA_REUNION_COORDINACION'
  | 'DOCUMENTACION_INTERES'
  | 'VERIFICACION_INSTITUCIONAL'
  | 'COMPROBANTE_FORMALIZACION_CONEAU';

export interface ConvocatoriaFiltro {
  carreraId?: string;
  estado?: EstadoProceso;
  tipoProceso?: TipoProceso;
  anio?: number;
}

export interface IniciarConvocatoriaRequest {
  carreraId: string;
  nroResolucionConeau?: string;
  fechaInicio: string;
  fechaCierre: string;
  tipoProceso: TipoProceso;
  observaciones?: string;
}

export interface RegistroConvocatoria {
  id: string;
  nroResolucionConeau: string;
  fechaInicio: string;
  fechaCierre: string;
  observaciones?: string;
  rmEstandares?: string;
  areaDisciplinaria?: string;
  modalidadCarrera?: 'PRESENCIAL' | 'A_DISTANCIA' | string;
  estadoCarrera?: 'NUEVA' | 'EN_FUNCIONAMIENTO' | string;
  faseEvaluacion?: 'PRIMERA_FASE' | 'SEGUNDA_FASE' | 'NUEVO_CICLO' | 'SEGUIMIENTO';
  fechaLimitePresentacionConeau?: string;
  fechaFormalizacionElectronica?: string;
  creadoEn: string;
  actualizadoEn: string;
}

export type RegistroConvocatoriaDto = RegistroConvocatoria;

export interface PlanTrabajoResumen {
  id: string;
  codigo: string;
  tipo: TipoPlanTrabajo;
  estado: EstadoPlanTrabajo;
  actividadesCount: number;
}

export interface ConvocatoriaResumen {
  id: string;
  codigo: string;
  titulo: string;
  carreraId: string;
  carreraNombre: string;
  carreraSigla?: string;
  nivel: string;
  nroResolucionConeau: string;
  tipoProceso: TipoProceso;
  estado: EstadoProceso;
  fechaInicio: string;
  fechaCierre: string;
  planTrabajoCodigo?: string;
  creadoEn: string;
}

export interface ConvocatoriaDetalle {
  id: string;
  codigo: string;
  titulo: string;
  carreraId: string;
  carreraNombre: string;
  carreraSigla?: string;
  nivel: string;
  tipoProceso: TipoProceso;
  estado: EstadoProceso;
  creadoEn: string;
  creadoPor: string;
  registroConvocatoria?: RegistroConvocatoria;
  planTrabajo?: PlanTrabajoResumen;
}

export interface DocumentoAcreditacionDto {
  id: string;
  nombreOriginal: string;
  tipoDocumento: TipoDocumento;
  clasificacionOrigen?: string;
  descripcion?: string;
  mimeType: string;
  tamanioBytes: number;
  descargaUrl: string;
  subidoEn: string;
}

export type DocumentoInteresDto = DocumentoAcreditacionDto;

export interface FichaFormalizacionFt012Dto {
  id?: string;
  procesoId: string;
  modalidadDictado: 'PRESENCIAL' | 'A_DISTANCIA' | string;
  estado: 'PENDIENTE_SOLICITUD' | 'SOLICITADA_A_DC' | 'BORRADOR' | 'REMITIDA_POR_DC' | 'FORMALIZADA_EN_CONEAU' | string;
  institucionUniversitaria: string;
  autoridadApellido: string;
  autoridadPrimerNombre: string;
  autoridadSegundoNombre?: string;
  autoridadCorreo: string;
  autoridadTelefono?: string;
  unidadAcademica: string;
  sedeCalle: string;
  sedeNumero: string;
  sedePiso?: string;
  sedeDepartamento?: string;
  sedeCodigoPostal: string;
  sedeProvincia: string;
  sedeLocalidad: string;
  sedeDepartamentoPartido: string;
  nexoApellido?: string;
  nexoPrimerNombre?: string;
  nexoSegundoNombre?: string;
  nexoTelefono?: string;
  nexoCargo?: string;
  nexoCorreo?: string;
  carreraNombre: string;
  tituloAsimilado?: string;
  cargaHorariaNoPresencial?: string;
  anioInicioDictado?: number | null;
  responsableApellido?: string;
  responsablePrimerNombre?: string;
  responsableSegundoNombre?: string;
  responsableTelefono?: string;
  responsableCorreo?: string;
  responsableCargo?: string;
  coordinadorCaApellido?: string;
  coordinadorCaPrimerNombre?: string;
  coordinadorCaSegundoNombre?: string;
  coordinadorCaTelefono?: string;
  coordinadorCaCorreo?: string;
  comprobanteNombre?: string;
  comprobanteDocumentoId?: string;
  comprobanteStoragePath?: string;
  comprobanteTamanioBytes?: number;
  fechaFormalizacionElectronica?: string;
  observaciones?: string;
  solicitadoPor?: string;
  solicitadoEn?: string;
  remitidoPor?: string;
  remitidoEn?: string;
  formalizadoPor?: string;
  formalizadoEn?: string;
  updatedAt?: string;
}

export interface ActividadPlanTrabajoDto {
  id: string;
  planTrabajoId: string;
  numeroOrden: number;
  codigoActividad: string;
  nombre: string;
  estado: EstadoActividad;
  fechaReunionCoordinacion?: string;
  sintesisAcuerdos?: string;
  observaciones?: string;
  areasInformadas?: string[];
  documentacionRemitida?: string[];
  completadaEn?: string;
  completadaPor?: string;
  completadaPorNombre?: string;
  hoja?: HojaPlanTrabajo;
  fechaInicioEstimada?: string;
  fechaFinEstimada?: string;
  responsable?: string;
  prioridad?: PrioridadActividad;
  creadoEn: string;
  actualizadoEn: string;
  documentos: DocumentoAcreditacionDto[];
}

export interface OpcionCatalogoProceso {
  codigo: string;
  etiqueta: string;
  descripcion?: string;
}

export const AREAS_INFORMADAS_CATALOGO: OpcionCatalogoProceso[] = [
  { codigo: 'DIRECCION_CARRERA', etiqueta: 'Dirección de Carrera' },
  { codigo: 'DIRECCION_ESCUELA', etiqueta: 'Dirección de Escuela' },
  { codigo: 'DIRECCIONES_DEPARTAMENTOS', etiqueta: 'Direcciones de Departamentos' },
  { codigo: 'DIRECCIONES_INSTITUTOS', etiqueta: 'Direcciones de Institutos' },
  { codigo: 'SECRETARIAS', etiqueta: 'Secretarías' },
];

export const AREAS_INFORMADAS_LABELS: Record<string, string> = {
  DIRECCION_CARRERA: 'Dirección de Carrera',
  DIRECCION_ESCUELA: 'Dirección de Escuela',
  DIRECCIONES_DEPARTAMENTOS: 'Direcciones de Departamentos',
  DIRECCIONES_INSTITUTOS: 'Direcciones de Institutos',
  SECRETARIAS: 'Secretarías',
};

export const DOCUMENTACION_REMITIDA_LABELS: Record<string, string> = {
  RESOLUCION_CONVOCATORIA_CONEAU: 'Resolución de convocatoria pública de CONEAU',
  RM_ESTANDARES: 'Resolución Ministerial que fija los estándares de la carrera',
  RM_ESTANDARES_CARRERA: 'Resolución Ministerial que fija los estándares de la carrera',
  RM_INCLUSION_ART_43_LES: 'Resolución Ministerial de inclusión en el art. 43 de la LES',
  ORDENANZAS_NOTAS_TECNICAS: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
  ORDENANZAS_Y_NOTAS_TECNICAS: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
  RESOLUCION_ACREDITACION_COMPROMISOS:
    'Resolución de acreditación (o dictamen) con compromisos y recomendaciones',
  RESOLUCIONES_INFORME_SEGUIMIENTO:
    'Resoluciones e informes de seguimiento de avance previos',
  RESOLUCIONES_INFORMES_SEGUIMIENTO:
    'Resoluciones e informes de seguimiento de avance previos',
  PLANES_MEJORA_PRESENTADOS: 'Planes de mejora presentados ante CONEAU (última versión)',
  PLANES_MEJORA_CONEAU: 'Planes de mejora presentados ante CONEAU (última versión)',
  DOC_PROPIA_AREA: 'Documentación propia del área',
  PROCESOS_ACREDITACION_CARRERAS: 'Procesos de acreditación de carreras',
  EVALUACION_FUNCIONES: 'Evaluación de funciones',
  EVALUACION_INSTITUCIONAL: 'Evaluación institucional',
  INFO_INSTITUCION_VERIFICADA:
    'Administrar institución: verificados nombre oficial (UNdeC), máxima autoridad, administrador institucional y dirección de sede central',
  OFERTA_ACADEMICA_RG004_VERIFICADA:
    'Administrar carreras: verificadas carreras de grado y posgrado de la unidad académica según RG-004: Oferta académica UNdeC',
};

export const FUENTES_DOCUMENTACION_INTERES_CATALOGO: OpcionCatalogoProceso[] = [
  {
    codigo: 'DOC_PROPIA_AREA',
    etiqueta: 'Documentación propia del área',
    descripcion: 'Normativa, resoluciones y antecedentes generados directamente por el Área de Evaluación y Acreditación (AEA).',
  },
  {
    codigo: 'PROCESOS_ACREDITACION_CARRERAS',
    etiqueta: 'Procesos de acreditación de carreras',
    descripcion: 'Dictámenes, resoluciones CONEAU, planes de mejora y memorias de acreditaciones previas de la carrera.',
  },
  {
    codigo: 'EVALUACION_FUNCIONES',
    etiqueta: 'Evaluación de funciones',
    descripcion: 'Informes y resoluciones vinculadas a evaluación de funciones sustantivas universitarias (docencia, investigación, extensión).',
  },
  {
    codigo: 'EVALUACION_INSTITUCIONAL',
    etiqueta: 'Evaluación institucional',
    descripcion: 'Informes de autoevaluación institucional y evaluaciones externas desarrolladas por la UNdeC.',
  },
];

export const ITEMS_VERIFICACION_INSTITUCIONAL_CATALOGO: OpcionCatalogoProceso[] = [
  {
    codigo: 'INFO_INSTITUCION_VERIFICADA',
    etiqueta: 'Administrar institución',
    descripcion: 'Verificados nombre oficial (Universidad Nacional de Chilecito), máxima autoridad, administrador institucional y dirección sede central.',
  },
  {
    codigo: 'OFERTA_ACADEMICA_RG004_VERIFICADA',
    etiqueta: 'Administrar carreras (RG-004)',
    descripcion: 'Verificadas carreras de grado y posgrado de la unidad académica según RG-004: Oferta académica UNdeC en CONEAU Global.',
  },
];

export function obtenerOpcionesDocumentacionRemitida(
  tipoProceso?: TipoProceso,
  tipoPlan?: TipoPlanTrabajo
): OpcionCatalogoProceso[] {
  if (tipoPlan === 'PT_003_SEGUIMIENTO') {
    return [
      {
        codigo: 'ORDENANZAS_NOTAS_TECNICAS',
        etiqueta: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
      },
      {
        codigo: 'RESOLUCION_ACREDITACION_COMPROMISOS',
        etiqueta: 'Resolución de acreditación (o dictamen) con compromisos y recomendaciones',
      },
      {
        codigo: 'RESOLUCIONES_INFORME_SEGUIMIENTO',
        etiqueta: 'Resoluciones e informes de seguimiento de avance previos',
      },
      {
        codigo: 'PLANES_MEJORA_PRESENTADOS',
        etiqueta: 'Planes de mejora presentados ante CONEAU (última versión)',
      },
    ];
  }

  if (tipoProceso === 'NUEVA') {
    return [
      {
        codigo: 'RM_ESTANDARES',
        etiqueta: 'Resolución Ministerial que fija los estándares de la carrera',
      },
      {
        codigo: 'RM_INCLUSION_ART_43_LES',
        etiqueta: 'Resolución Ministerial de inclusión en el art. 43 de la LES',
      },
      {
        codigo: 'ORDENANZAS_NOTAS_TECNICAS',
        etiqueta: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
      },
    ];
  }

  // Carrera en funcionamiento (1era o 2da Fase) por defecto
  return [
    {
      codigo: 'RESOLUCION_CONVOCATORIA_CONEAU',
      etiqueta: 'Resolución de convocatoria pública de CONEAU',
    },
    {
      codigo: 'RM_ESTANDARES',
      etiqueta: 'Resolución Ministerial que fija los estándares de la carrera',
    },
    {
      codigo: 'RM_INCLUSION_ART_43_LES',
      etiqueta: 'Resolución Ministerial de inclusión en el art. 43 de la LES',
    },
    {
      codigo: 'ORDENANZAS_NOTAS_TECNICAS',
      etiqueta: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
    },
  ];
}

export interface CompletarActividadPayload {
  fechaReunion: string;
  areasInformadas: string[];
  documentacionRemitida: string[];
  observaciones?: string;
  sintesisAcuerdos?: string;
  archivo?: File | null;
  tipoDocumento?: TipoDocumento;
}

export interface PlanTrabajoDto {
  id: string;
  procesoId: string;
  codigo: string;
  tipo: TipoPlanTrabajo;
  estado: EstadoPlanTrabajo;
  actividades: ActividadPlanTrabajoDto[];
}

export type HojaPlanTrabajo = 'INFO' | 'CONEAU';
export type FaseEvaluacion = 'PRIMERA_FASE' | 'SEGUNDA_FASE' | 'NUEVO_CICLO' | 'SEGUIMIENTO';
export type PrioridadActividad = 'ALTA' | 'MEDIA' | 'BAJA';

export interface PlantillaActividadDto {
  numeroOrden?: number;
  ordenSugerido?: number;
  codigoActividad: string;
  nombre: string;
  hoja: HojaPlanTrabajo;
  responsableSugerido?: string;
  prioridadSugerida: PrioridadActividad;
}

export interface PlantillaPlanTrabajoDto {
  tipoPlan: string;
  actividades?: PlantillaActividadDto[];
  actividadesInfo?: PlantillaActividadDto[];
  actividadesConeau?: PlantillaActividadDto[];
}

export interface ItemActividadPlanRequest {
  numeroOrden: number;
  codigoActividad: string;
  nombre: string;
  hoja: HojaPlanTrabajo;
  fechaInicioEstimada: string;
  fechaFinEstimada: string;
  responsable: string;
  prioridad: PrioridadActividad;
}

export interface ElaborarPlanTrabajoRequest {
  fechaInicio?: string;
  fechaCierre?: string;
  estadoCarrera?: 'NUEVA' | 'EN_FUNCIONAMIENTO' | string;
  nroResolucionConeau?: string;
  rmEstandares: string;
  areaDisciplinaria?: string;
  modalidadCarrera?: 'PRESENCIAL' | 'A_DISTANCIA' | string;
  faseEvaluacion: FaseEvaluacion;
  fechaLimitePresentacionConeau: string;
  fechaLimiteConeau?: string;
  actividades: ItemActividadPlanRequest[];
}
