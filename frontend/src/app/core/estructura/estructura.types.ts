export interface UnidadAcademicaResponse {
  id: string;
  institucionId: string;
  nombre: string;
  sigla?: string;
  descripcion?: string;
  activo: boolean;
  creadoEn: string;
  actualizadoEn: string;
}

export interface RegistrarUnidadAcademicaData {
  institucionId: string;
  nombre: string;
  sigla?: string;
  descripcion?: string;
}

export interface ActualizarUnidadAcademicaData {
  nombre: string;
  sigla?: string;
  descripcion?: string;
}

export interface SedeLocalizacionResponse {
  id: string;
  institucionId: string;
  nombre: string;
  calle: string;
  numero: string;
  piso?: string;
  codigoPostal: string;
  provinciaId: number;
  departamentoId: number;
  localidadId: number;
  esPropia: boolean;
  activo: boolean;
  direccionCompleta: string;
  creadoEn: string;
  actualizadoEn: string;
}

export interface RegistrarSedeLocalizacionData {
  institucionId: string;
  nombre: string;
  calle: string;
  numero: string;
  piso?: string;
  codigoPostal: string;
  provinciaId: number;
  departamentoId: number;
  localidadId: number;
  esPropia?: boolean;
}

export interface ActualizarSedeLocalizacionData {
  nombre: string;
  calle: string;
  numero: string;
  piso?: string;
  codigoPostal: string;
  provinciaId: number;
  departamentoId: number;
  localidadId: number;
  esPropia?: boolean;
}

export type ModalidadCarrera = 'PRESENCIAL' | 'DISTANCIA';
export type EstadoCarrera = 'NUEVA' | 'EN_FUNCIONAMIENTO';

export interface CarreraResponse {
  id: string;
  unidadAcademicaId: string;
  nombre: string;
  sigla?: string;
  descripcion?: string;
  nivel: 'GRADO';
  areaDisciplinaria: string;
  modalidad: ModalidadCarrera;
  estado: EstadoCarrera;
  tituloOtorgado?: string;
  duracionAnios?: number;
  activo: boolean;
  sedesIds: string[];
  creadoEn: string;
  actualizadoEn: string;
}

export interface RegistrarCarreraData {
  unidadAcademicaId: string;
  nombre: string;
  sigla?: string;
  descripcion?: string;
  areaDisciplinaria: string;
  modalidad: ModalidadCarrera;
  estado: EstadoCarrera;
  tituloOtorgado?: string;
  duracionAnios?: number;
  sedesIds?: string[];
}

export interface ActualizarCarreraData {
  nombre: string;
  sigla?: string;
  descripcion?: string;
  areaDisciplinaria: string;
  modalidad: ModalidadCarrera;
  estado: EstadoCarrera;
  tituloOtorgado?: string;
  duracionAnios?: number;
  activo?: boolean;
  sedesIds?: string[];
}

export interface CambiarEstadoData {
  activo: boolean;
}
