import { EstadoDictadoActividad } from '../planes-estudio/plan-estudio.models';
import { EstadoVigenciaPlan } from '../services/datos-centrales.service';

export type TipoSolicitudDesignacion = 'ANUAL' | 'EXTRAORDINARIA';
export type EstadoSolicitudDesignacion = 'BORRADOR' | 'PRESENTADA_SGA' | 'APROBADA' | 'OBSERVADA';
export type EstadoItemDesignacion = 'NUEVO' | 'REPLICADO' | 'MODIFICADO' | 'EXCLUIDO';
export type ModalidadParticipacionDocente = 'DESARROLLO_CLASES' | 'EVALUACION_FINALES';

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
}

export interface CrearSolicitudDesignacionRequest {
  departamentoAcademicoId: string | null;
  periodoAcademicoId: string;
  tipoSolicitud: TipoSolicitudDesignacion;
  observacionesDepartamento?: string | null;
}

export interface GuardarDesignacionDocenteRequest {
  docenteId: string;
  cargoDocenteId: string;
  dedicacionDocenteId: string;
  tipoDesignacionId: string;
  nroDesignacionMapuche?: string | null;
  observaciones?: string | null;
}

export interface GuardarDistribucionHorariaRequest {
  horasClases: number;
  horasInvestigacion: number;
  horasExtension: number;
  horasGestion: number;
  horasDireccionTesis: number;
  horasOtras: number;
}

export interface AgregarAsignacionCurricularRequest {
  carreraId: string;
  planEstudioId: string;
  actividadCurricularId: string;
  modalidadParticipacion: ModalidadParticipacionDocente;
  horasAsignadas?: number | null;
  observaciones?: string | null;
}
