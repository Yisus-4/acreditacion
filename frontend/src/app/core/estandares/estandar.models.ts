export type EstadoVigenciaEstandar = 'VIGENTE' | 'NO_VIGENTE';
export type TipoSeccionEstandar22 = 'COMPETENCIAS' | 'EJES' | 'BLOQUES' | 'NINGUNA';
export type TipoEvaluacionEstandar22 = 'ESCALA_UNICA' | 'MATRIZ_MULTIPLE' | 'NINGUNA';
export type TipoSeccionCargaHoraria = 'CARGA_TOTAL_311' | 'PRACTICA_312';

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
  }>;
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
