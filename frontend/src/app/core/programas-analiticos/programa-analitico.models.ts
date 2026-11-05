export interface ProgramaAnaliticoAsignatura {
  id?: string;
  asignaturaPlanId: string;
  asignaturaCodigo?: string;
  asignaturaNombre?: string;
  procesoId?: string;
  archivoUrl?: string;
  archivoNombre?: string;
  resolucionAprobacion?: string;
  vigenciaDesde?: number;
  vigenciaHasta?: number;
  subidoPorUsuarioNombre?: string;
  fechaSubida?: string;
  observaciones?: string;
  estadoCarga?: 'CARGADO' | 'PENDIENTE';
}
