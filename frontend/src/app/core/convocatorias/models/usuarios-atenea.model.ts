export type GrupoUsuarioAtenea = 'SGI' | 'AEA' | 'OTRAS_AREAS' | 'COMISION_AUTOEVALUACION';

export type TipoAccesoAtenea = 'CARGA' | 'VISUALIZACION' | 'ADMINISTRACION' | 'CARGA_Y_VISUALIZACION';

export type EstadoRegistroUsuariosAtenea = 'EN_DEFINICION' | 'FINALIZADO';

export type TipoPermisoMatrizAtenea = 'LECTURA' | 'LECTURA_MODIFICACION';

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

export interface ItemMatrizPermisoAteneaDto {
  id?: string;
  procesoActividad: string;
  item: string;
  seccion?: string;
  tipoPermiso: TipoPermisoMatrizAtenea;
  tipoPermisoDescripcion?: string;
  permisoSgiAea: boolean;
  permisoCa: boolean;
  orden: number;
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
  matrizPermisos: ItemMatrizPermisoAteneaDto[];
}

export interface GuardarUsuariosAteneaRequest {
  usuarios: UsuarioPermisoAteneaDto[];
  matrizPermisos?: ItemMatrizPermisoAteneaDto[];
}
