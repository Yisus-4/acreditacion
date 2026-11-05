export interface ActividadCurricularPlan {
  id?: string;
  planEstudioId?: string;
  numeroOrden?: number;
  codigo: string;
  nombre: string;
  grupo?: string; // C.B.I., T.B., T.A., C.T.C.
  subgrupo?: string; // 1º, 2º, 3º, 4º, 5º
  horasReloj: number;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  correlativas?: string;
  esObligatoria?: boolean;
}

export interface PlanEstudioFt001 {
  id?: string;
  carreraId: string;
  carreraNombre?: string;
  procesoId?: string;
  codigoResolucion: string;
  anioAprobacion?: number;
  anioInicio?: number;
  duracionAnios?: number;
  orientacionesJson?: string;
  fundamentacionModificaciones?: string;
  estructuraSintetica?: string;
  horasPresenciales?: number;
  horasNoPresenciales?: number;
  horasTotales?: number;
  horasOptativasMinimas?: number;
  actividadesComunesCiclo?: string;
  tituloIntermedioDenominacion?: string;
  tituloIntermedioDuracion?: string;
  tituloIntermedioActividades?: string;
  instanciaFinalGraduacion?: string;
  otraInformacion?: string;
  activo?: boolean;
  asignaturas?: ActividadCurricularPlan[];
}
