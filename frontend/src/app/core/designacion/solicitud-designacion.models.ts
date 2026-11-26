import { EstadoDictadoActividad } from '../planes-estudio/plan-estudio.models';
import { EstadoVigenciaPlan } from '../services/datos-centrales.service';

export type TipoSolicitudDesignacion = 'ANUAL' | 'EXTRAORDINARIA';
export type EstadoSolicitudDesignacion = 'BORRADOR' | 'PRESENTADA_SGA' | 'APROBADA' | 'OBSERVADA';
export type EstadoItemDesignacion = 'NUEVO' | 'REPLICADO' | 'MODIFICADO' | 'EXCLUIDO';
export type ModalidadParticipacionDocente = 'DESARROLLO_CLASES' | 'EVALUACION_FINALES';
export type TipoEventoSolicitudDesignacion =
  | 'CREACION'
  | 'CLONACION_PERIODO_ANTERIOR'
  | 'ALTA_DESIGNACION'
  | 'MODIFICACION_DESIGNACION'
  | 'MODIFICACION_DISTRIBUCION'
  | 'EXCLUSION_DESIGNACION'
  | 'REACTIVACION_DESIGNACION'
  | 'ALTA_ASIGNACION'
  | 'BAJA_ASIGNACION'
  | 'PRESENTACION_SGA'
  | 'APROBACION_SGA'
  | 'OBSERVACION_SGA';

export interface CambioSolicitudDesignacion {
  id: string;
  solicitudId: string;
  designacionId: string | null;
  tipoEvento: TipoEventoSolicitudDesignacion;
  campoModificado: string | null;
  valorAnterior: string | null;
  valorNuevo: string | null;
  motivo: string | null;
  usuarioId: string | null;
  usuarioNombre: string | null;
  fechaCambio: string;
}

export interface SolicitudDesignacionResumen {
  id: string;
  departamentoAcademicoId: string;
  departamentoCodigo: string;
  departamentoDenominacion: string;
  periodoAcademicoId: string;
  periodoCodigo: string;
  periodoDenominacion: string;
  periodoAnio: number;
  tipoSolicitud: TipoSolicitudDesignacion;
  estado: EstadoSolicitudDesignacion;
  editable: boolean;
  cantidadDesignaciones: number;
  cantidadConAlertaHoras: number;
  observacionesDepartamento: string | null;
  observacionesSga: string | null;
  fechaEmision: string | null;
  creadoEn: string;
  actualizadoEn: string;
}

export interface AsignacionCurricularPeriodo {
  id: string;
  carreraId: string;
  carreraNombre: string;
  carreraSigla: string | null;
  planEstudioId: string;
  planEstudioNombre: string;
  planEstadoVigencia: EstadoVigenciaPlan;
  actividadCurricularId: string;
  actividadCodigo: string | null;
  actividadNombre: string;
  actividadEstadoDictado: EstadoDictadoActividad;
  actividadHorasSemanales: number | null;
  modalidadParticipacion: ModalidadParticipacionDocente;
  horasAsignadas: number | null;
  observaciones: string | null;
}

export interface DesignacionDocenteItem {
  id: string;
  solicitudId: string;
  designacionOrigenId: string | null;
  docenteId: string;
  docenteDni: string;
  docenteLegajo: string | null;
  docenteApellido: string;
  docenteNombre: string;
  cargoDocenteId: string;
  cargoCodigo: string;
  cargoNombre: string;
  dedicacionDocenteId: string;
  dedicacionCodigo: string;
  dedicacionNombre: string;
  horasDedicacion: number;
  tipoDesignacionId: string;
  tipoDesignacionCodigo: string;
  tipoDesignacionNombre: string;
  nroDesignacionMapuche: string | null;
  estadoItem: EstadoItemDesignacion;
  horasClases: number;
  horasInvestigacion: number;
  horasExtension: number;
  horasGestion: number;
  horasDireccionTesis: number;
  horasOtras: number;
  totalHorasDistribuidas: number;
  horasSinDistribuir: number;
  tieneAlertaHorasSinDistribuir: boolean;
  totalHorasDocentePeriodo: number;
  observaciones: string | null;
  asignaciones: AsignacionCurricularPeriodo[];
}

export interface SolicitudDesignacionDetalle {
  id: string;
  solicitudOrigenId: string | null;
  departamentoAcademicoId: string;
  departamentoCodigo: string;
  departamentoDenominacion: string;
  periodoAcademicoId: string;
  periodoCodigo: string;
  periodoDenominacion: string;
  periodoAnio: number;
  tipoSolicitud: TipoSolicitudDesignacion;
  estado: EstadoSolicitudDesignacion;
  editable: boolean;
  observacionesDepartamento: string | null;
  observacionesSga: string | null;
  fechaEmision: string | null;
  creadoEn: string;
  actualizadoEn: string;
  designaciones: DesignacionDocenteItem[];
  historialCambios: CambioSolicitudDesignacion[];
}

export interface CrearSolicitudDesignacionRequest {
  departamentoAcademicoId: string | null;
  periodoAcademicoId: string;
  tipoSolicitud: TipoSolicitudDesignacion;
  observacionesDepartamento?: string | null;
  clonarPeriodoAnterior?: boolean;
  solicitudOrigenId?: string | null;
}

export interface GuardarDesignacionDocenteRequest {
  docenteId: string;
  cargoDocenteId: string;
  dedicacionDocenteId: string;
  tipoDesignacionId: string;
  nroDesignacionMapuche?: string | null;
  observaciones?: string | null;
  motivoCambio?: string | null;
}

export interface GuardarDistribucionHorariaRequest {
  horasClases: number;
  horasInvestigacion: number;
  horasExtension: number;
  horasGestion: number;
  horasDireccionTesis: number;
  horasOtras: number;
  motivoCambio?: string | null;
}

export interface AgregarAsignacionCurricularRequest {
  carreraId: string;
  planEstudioId: string;
  actividadCurricularId: string;
  modalidadParticipacion: ModalidadParticipacionDocente;
  horasAsignadas?: number | null;
  observaciones?: string | null;
  motivoCambio?: string | null;
}

export interface MotivoCambioRequest {
  motivo?: string | null;
}

export interface RevisionSgaRequest {
  observacionesSga?: string | null;
}
