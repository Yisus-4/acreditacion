export type TipoCategoriaAmbito = 'INTERNO' | 'EXTERNO' | 'UNIDAD_DEMOSTRATIVA' | 'CAM';

export interface AmbitoPractica {
  id?: string;
  numeroOrden?: number;
  denominacion: string;
  tipoCategoria: TipoCategoriaAmbito;
  tipoAmbito?: string;
  tipoEstablecimiento?: string;
  inmuebleId?: string;
  inmuebleNombre?: string;
  convenioId?: string;
  convenioNombre?: string;
  descripcion?: string;
  anioConstruccion?: number;
  superficieM2?: number;
  capacidadAlumnos?: number;
  disponibilidadHoraria?: string;
  direccion?: string;
  localidad?: string;
  provincia?: string;
  departamento?: string;
  codigoPostal?: string;
  tipoDependencia?: string;
  sector?: string;
  actividadAlumnoCam?: string;
  especiesAnimalesCultivos?: string;
  numeroAnimales?: number;
  actividadesDemostrativas?: string;
  carrerasQueLoUsan?: string;
  activo?: boolean;
}

export interface AutoevaluacionCarreraAmbito {
  id?: string;
  carreraId: string;
  procesoId?: string;
  ambitoPracticaId: string;
  utilizadoPorCarrera: boolean;
  asignaturasVinculadas?: string;
  actividadesPrevistas?: string;
  responsablePractica?: string;
  observaciones?: string;
}
