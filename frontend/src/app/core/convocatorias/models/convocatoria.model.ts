export type NivelCarrera = 'GRADO';
export type TipoProceso = 'NUEVA' | 'EN_FUNCIONAMIENTO';
export type EstadoProceso = 'INICIADO' | 'EN_AUTOEVALUACION' | 'FINALIZADO' | 'CANCELADO';
export type TipoPlanTrabajo = 'PT_001_ACREDITACION' | 'PT_003_SEGUIMIENTO';
export type EstadoPlanTrabajo = 'ACTIVO' | 'FINALIZADO';
export type EstadoActividad = 'PENDIENTE' | 'EN_PROGRESO' | 'COMPLETADA' | 'CANCELADA';
export type TipoDocumento = 'NORMATIVA_CONVOCATORIA' | 'ACTA_REUNION_COORDINACION';

export interface ConvocatoriaFiltro {
  carreraId?: string;
  estado?: EstadoProceso;
  tipoProceso?: TipoProceso;
  anio?: number;
}

export interface IniciarConvocatoriaRequest {
  carreraId: string;
  nroResolucionConeau: string;
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
  creadoEn: string;
  actualizadoEn: string;
}

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
  mimeType: string;
  tamanioBytes: number;
  descargaUrl: string;
  subidoEn: string;
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
  completadaEn?: string;
  completadaPor?: string;
  completadaPorNombre?: string;
  creadoEn: string;
  actualizadoEn: string;
  documentos: DocumentoAcreditacionDto[];
}

export interface PlanTrabajoDto {
  id: string;
  procesoId: string;
  codigo: string;
  tipo: TipoPlanTrabajo;
  estado: EstadoPlanTrabajo;
  actividades: ActividadPlanTrabajoDto[];
}
