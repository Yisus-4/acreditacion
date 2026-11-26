export interface DepartamentoAcademico {
  id: string;
  institucionId?: string | null;
  codigo: string;
  denominacion: string;
  descripcion?: string | null;
  activo: boolean;
  esSistema: boolean;
  creadoEn?: string;
  actualizadoEn?: string;
}

export interface GuardarDepartamentoAcademicoRequest {
  codigo: string;
  denominacion: string;
  descripcion?: string | null;
  activo?: boolean;
}

export interface CargoDocente {
  id: string;
  codigo: string;
  nombre: string;
  orden: number;
  activo: boolean;
}

export interface GuardarCargoDocenteRequest {
  codigo: string;
  nombre: string;
  orden?: number;
  activo?: boolean;
}

export interface DedicacionDocente {
  id: string;
  codigo: string;
  nombre: string;
  horasSemanales: number;
  orden: number;
  activo: boolean;
}

export interface GuardarDedicacionDocenteRequest {
  codigo: string;
  nombre: string;
  horasSemanales: number;
  orden?: number;
  activo?: boolean;
}

export interface TipoDesignacionDocente {
  id: string;
  codigo: string;
  nombre: string;
  esRentado: boolean;
  orden: number;
  activo: boolean;
}

export interface GuardarTipoDesignacionRequest {
  codigo: string;
  nombre: string;
  esRentado?: boolean;
  orden?: number;
  activo?: boolean;
}

export interface PeriodoAcademico {
  id: string;
  codigo: string;
  denominacion: string;
  anio: number;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  esActual: boolean;
  activo: boolean;
}

export interface GuardarPeriodoAcademicoRequest {
  codigo: string;
  denominacion: string;
  anio: number;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  esActual?: boolean;
  activo?: boolean;
}

export interface CatalogosDocentes {
  departamentos: DepartamentoAcademico[];
  cargos: CargoDocente[];
  dedicaciones: DedicacionDocente[];
  tiposDesignacion: TipoDesignacionDocente[];
  periodos: PeriodoAcademico[];
}
