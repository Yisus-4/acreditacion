export type EstadoVigenciaEstandar = 'VIGENTE' | 'NO_VIGENTE';
export type TipoSeccionEstandar22 = 'COMPETENCIAS' | 'EJES' | 'BLOQUES' | 'NINGUNA';
export type TipoEvaluacionEstandar22 = 'ESCALA_UNICA' | 'MATRIZ_MULTIPLE' | 'NINGUNA';
export type TipoSeccionCargaHoraria = 'CARGA_TOTAL_311' | 'PRACTICA_312';
export type ModoSeccion311 = 'AREAS_TEMATICAS' | 'BLOQUES_CURRICULARES';
export type ModoSeccion312 = 'AREAS_PRACTICAS' | 'MODALIDADES_PRACTICAS';

export interface EstandarResumen {
  id: string;
  carreraId: string;
  carreraNombre: string | null;
  carreraSigla: string | null;
  nombre: string;
  resolucionMinisterial: string | null;
  anioVigencia: number | null;
  estadoVigencia: EstadoVigenciaEstandar;
  tipoSeccion22: TipoSeccionEstandar22;
  tipoEvaluacion22: TipoEvaluacionEstandar22;
  cantidadGrupos22: number;
  cantidadItems22: number;
  cantidadAreas3: number;
  planesAsociados: number;
  editable: boolean;
  creadoEn: string;
  actualizadoEn: string;
}

export interface EscalaEstandar {
  id?: string;
  orden: number;
  codigo?: string;
  etiqueta: string;
  descripcionReferencia?: string | null;
  esValorPorDefecto?: boolean;
}

export interface Item22Estandar {
  id?: string;
  orden: number;
  enunciado: string;
}

export interface Grupo22Estandar {
  id?: string;
  orden: number;
  codigo?: string;
  titulo: string;
  descripcion?: string | null;
  habilitado: boolean;
  items: Item22Estandar[];
}

export interface AreaCargaHorariaEstandar {
  id?: string;
  tipoSeccion?: TipoSeccionCargaHoraria;
  orden: number;
  codigo?: string;
  nombre: string;
  etiquetaPractica?: string | null;
  habilitada: boolean;
  horasMinimasCarrera?: number | null;
}

export interface EstandarDetalle {
  id: string;
  carreraId: string;
  carreraNombre: string | null;
  carreraSigla: string | null;
  nombre: string;
  resolucionMinisterial: string | null;
  anioVigencia: number | null;
  estadoVigencia: EstadoVigenciaEstandar;
  tipoSeccion22: TipoSeccionEstandar22;
  tipoEvaluacion22: TipoEvaluacionEstandar22;
  instrucciones22: string | null;
  observaciones: string | null;
  habilitar311: boolean;
  modo311: ModoSeccion311;
  horasMinimasTotal311: number | null;
  habilitar312: boolean;
  modo312: ModoSeccion312;
  horasMinimasTotal312: number | null;
  planesAsociados: number;
  editable: boolean;
  escalas: EscalaEstandar[];
  grupos22: Grupo22Estandar[];
  areasCargaHoraria: AreaCargaHorariaEstandar[];
  creadoEn: string;
  actualizadoEn: string;
}

export interface GuardarEstandarRequest {
  carreraId: string;
  nombre: string;
  resolucionMinisterial?: string | null;
  anioVigencia?: number | null;
  estadoVigencia?: EstadoVigenciaEstandar;
  tipoSeccion22: TipoSeccionEstandar22;
  tipoEvaluacion22: TipoEvaluacionEstandar22;
  instrucciones22?: string | null;
  observaciones?: string | null;
  habilitar311: boolean;
  modo311: ModoSeccion311;
  horasMinimasTotal311?: number | null;
  habilitar312: boolean;
  modo312: ModoSeccion312;
  horasMinimasTotal312?: number | null;
  escalas: Array<{
    orden: number;
    codigo?: string;
    etiqueta: string;
    descripcionReferencia?: string | null;
    esValorPorDefecto?: boolean;
  }>;
  grupos22: Array<{
    orden: number;
    codigo?: string;
    titulo: string;
    descripcion?: string | null;
    habilitado: boolean;
    items: Array<{
      orden: number;
      enunciado: string;
    }>;
  }>;
  areasCargaHoraria: Array<{
    tipoSeccion?: TipoSeccionCargaHoraria;
    orden: number;
    codigo?: string;
    nombre: string;
    etiquetaPractica?: string | null;
    habilitada: boolean;
    horasMinimasCarrera?: number | null;
  }>;
}

export interface FilaCumplimientoHorario {
  areaId: string;
  codigo: string;
  nombre: string;
  horasPresenciales: number;
  horasDistancia: number;
  horasAcumuladas: number;
  horasMinimasCarrera: number | null;
  diferencia: number | null;
  tieneMinimo: boolean;
  cumple: boolean;
}

export interface CumplimientoCargaHorariaPlan {
  planEstudioId: string;
  nombrePlan: string;
  estandarId: string;
  estandarNombre: string;
  totalAsignaturasSeDicta: number;
  asignaturasConSeccion3Cargada: number;
  habilitar311: boolean;
  modo311: ModoSeccion311;
  horasPresencialesTotal311: number;
  horasDistanciaTotal311: number;
  horasAcumuladasTotal311: number;
  horasMinimasTotal311: number | null;
  diferenciaTotal311: number | null;
  tieneMinimoTotal311: boolean;
  cumpleTotal311: boolean;
  filas311: FilaCumplimientoHorario[];
  habilitar312: boolean;
  modo312: ModoSeccion312;
  horasPresencialesTotal312: number;
  horasDistanciaTotal312: number;
  horasAcumuladasTotal312: number;
  horasMinimasTotal312: number | null;
  diferenciaTotal312: number | null;
  tieneMinimoTotal312: boolean;
  cumpleTotal312: boolean;
  filas312: FilaCumplimientoHorario[];
  cumpleGlobal: boolean;
}

export interface Respuesta22Dto {
  estandarItemId: string;
  escalaEvaluacionId: string;
}

export interface CargaAreaDto {
  areaCargaHorariaId: string;
  horasTotalPresencial: number;
  horasTotalDistancia: number;
  horasPracticaPresencial: number;
  horasPracticaDistancia: number;
}

export interface ResumenCargaDto {
  ambitosFormacionPractica: string | null;
  horasSemanalTotalPresencial: number;
  horasSemanalTotalDistancia: number;
  horasSemanalPracticaPresencial: number;
  horasSemanalPracticaDistancia: number;
}

export interface FichaEstandarActividad {
  planEstudioId: string;
  actividadCurricularId: string;
  asignaturaCodigo: string | null;
  asignaturaNombre: string;
  asignaturaHorasReloj: number | null;
  docenteResponsableId: string | null;
  docenteResponsableNombre: string | null;
  puedeEditar: boolean;
  estandar: EstandarDetalle;
  respuestas22: Respuesta22Dto[];
  cargasPorArea: CargaAreaDto[];
  resumenCarga: ResumenCargaDto;
  seccion22Completa: boolean;
  seccion3Completa: boolean;
  fichaCompleta: boolean;
}

export interface GuardarFichaEstandarRequest {
  respuestas22: Respuesta22Dto[];
  cargasPorArea: CargaAreaDto[];
  resumenCarga: ResumenCargaDto;
}
