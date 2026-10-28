export type GrupoUsuarioAtenea = 'SGI' | 'AEA' | 'OTRAS_AREAS' | 'COMISION_AUTOEVALUACION';

export type TipoAccesoAtenea = 'CARGA' | 'VISUALIZACION' | 'ADMINISTRACION' | 'CARGA_Y_VISUALIZACION';

export type EstadoRegistroUsuariosAtenea = 'EN_DEFINICION' | 'FINALIZADO';

export interface UsuarioPermisoAteneaDto {
  id?: string;
  grupoUsuario: GrupoUsuarioAtenea;
  nombre: string;
  apellido: string;
  nombreCompleto?: string;
  dniCuil: string;
  correo: string;
  areaDependencia?: string;
  cargoFuncion?: string;
  tipoAcceso: TipoAccesoAtenea;
  seccionAlcance?: string;
  requiereCuentaNuevaConeauGlobal: boolean;
  cuentaConeauGlobalCreada: boolean;
  habilitadoEnConeau: boolean;
}

export interface RegistroUsuariosAteneaDto {
  id: string;
  procesoId: string;
  estado: EstadoRegistroUsuariosAtenea;
  fechaLimite?: string;
  diasRestantes: number;
  semaforoTexto: string;
  generadoEn?: string;
  aprobadoEn?: string;
  creadoPor?: string;
  rutaDocumento?: string;
  finalizado: boolean;
  usuarios: UsuarioPermisoAteneaDto[];
}

export interface GuardarUsuariosAteneaRequest {
  usuarios: UsuarioPermisoAteneaDto[];
}
