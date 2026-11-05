export interface ConvenioInstitucional {
  id?: string;
  expediente: string;
  resolucionRectoral?: string;
  contraparte: string;
  fechaCelebracion: string; // YYYY-MM-DD
  duracion?: string;
  fechaCese?: string;
  objeto: string;
  vigente: boolean;
  estadoVigencia?: string;
  estadoEjecucion?: string;
  financiamiento?: string;
  responsableUndec?: string;
  objetivosGenerales?: string;
  objetivosEspecificos?: string;
  resultadosEsperados?: string;
  carrerasBeneficiadas?: string;
  tipoConvenio: string;
  archivoId?: string;
}

export type TipoConvenio = 'MARCO' | 'ESPECIFICO' | 'ASISTENCIAL' | 'PASANTIAS';
