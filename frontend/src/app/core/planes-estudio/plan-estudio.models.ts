export type EstadoDictadoActividad =
  | 'ACTIVA'
  | 'SOLO_EXAMENES_FINALES'
  | 'INACTIVA'
  | 'SE_DICTA'
  | 'NO_SE_DICTA';

export function esAsignaturaInactiva(estado?: EstadoDictadoActividad | null): boolean {
  return estado === 'INACTIVA' || estado === 'NO_SE_DICTA';
}

export function normalizarEstadoDictado(
  estado?: EstadoDictadoActividad | null
): 'ACTIVA' | 'SOLO_EXAMENES_FINALES' | 'INACTIVA' {
  if (estado === 'INACTIVA' || estado === 'NO_SE_DICTA') return 'INACTIVA';
  if (estado === 'SOLO_EXAMENES_FINALES') return 'SOLO_EXAMENES_FINALES';
  return 'ACTIVA';
}

export interface ActividadCurricularPlan {
  id?: string;
  planEstudioId?: string;
  numeroOrden?: number;
  codigo: string;
  nombre: string;
  grupo?: string; // C.B.I., T.B., T.A., C.T.C.
  subgrupo?: string; // 1º, 2º, 3º, 4º, 5º
  horasReloj: number;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  horasSemanales?: number | null;
  correlativas?: string;
  esObligatoria?: boolean;
  estadoDictado?: EstadoDictadoActividad;
  docenteResponsableId?: string | null;
  docenteResponsableNombre?: string | null;
  seccion22Completa?: boolean;
  seccion3Completa?: boolean;
}

export interface CrearAsignaturaRequest {
  codigo: string;
  nombre: string;
  grupo?: string;
  subgrupo?: string;
  horasReloj: number;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  horasSemanales?: number | null;
  correlativas?: string;
  esObligatoria?: boolean;
  numeroOrden?: number;
  estadoDictado?: EstadoDictadoActividad;
}

export interface ActualizarAsignaturaRequest {
  id?: string;
  codigo?: string;
  nombre?: string;
  grupo?: string;
  subgrupo?: string;
  horasReloj?: number;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  horasSemanales?: number | null;
  correlativas?: string;
  esObligatoria?: boolean;
  numeroOrden?: number;
  estadoDictado?: EstadoDictadoActividad;
}

export interface DocenteResumen {
  id: string;
  username: string;
  nombreCompleto: string;
  email?: string;
  unidadAcademicaId?: string;
  unidadAcademicaNombre?: string;
}

export interface PlanEstudioVigenteDto {
  id: string;
  carreraId: string;
  carreraNombre?: string;
  nombrePlan: string;
  codigoResolucion: string;
  anioAprobacion?: number;
  estadoVigencia?: string;
  esMaestro?: boolean;
}

export interface PlanEstudioFt001 {
  id?: string;
  carreraId: string;
  carreraNombre?: string;
  procesoId?: string;
  nombrePlan?: string;
  codigoResolucion: string;
  anioAprobacion?: number;
  anioInicio?: number;
  duracionAnios?: number;
  orientacionesJson?: string;
  fundamentacionModificaciones?: string;
  estructuraSintetica?: string;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  horasTotales?: number;
  horasOptativasMinimas?: number;
  actividadesComunesCiclo?: string;
  tituloIntermedioDenominacion?: string;
  tituloIntermedioDuracion?: string;
  tituloIntermedioActividades?: string;
  instanciaFinalGraduacion?: string;
  otraInformacion?: string;
  activo?: boolean;
  asignaturas?: ActividadCurricularPlan[];
  estandarId?: string;
  estandarNombre?: string;
  estandarResolucion?: string;
}
