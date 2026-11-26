import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoVigenciaPlan = 'VIGENTE' | 'NO_VIGENTE' | 'HISTORICO';
export interface ResumenDatosCentrales {
  planesEstudioVigentes: number;
  cuerpoAcademicoActivo: number;
  espaciosFisicos: number;
  ambitosPractica: number;
}
export interface CrearAsignacionCatedraRequest {
  carreraId?: string;
  planEstudioId: string;
  actividadCurricularId: string;
  rolEnCatedra?: string;
  cargaHorariaSemanal?: number;
}

export interface AsignacionCatedra {
  id?: string;
  carreraId?: string;
  carreraNombre?: string;
  planEstudioId?: string;
  planEstudioNombre?: string;
  actividadCurricularId?: string;
  actividadCurricularNombre?: string;
  rolEnCatedra?: string;
  cargaHorariaSemanal?: number;
}

export interface PlanEstudioMaestro {
  id: string;
  carreraId: string;
  carreraNombre?: string;
  carreraSigla?: string;
  nombrePlan: string;
  estadoVigencia: EstadoVigenciaPlan;
  esMaestro: boolean;
  codigoResolucion?: string;
  anioAprobacion?: number;
  anioInicio?: number;
  duracionAnios?: number;
  horasTotales?: number;
  activo?: boolean;
  cantidadAsignaturas?: number;
  estandarId?: string;
  estandarNombre?: string;
  estandarResolucion?: string;
}

export interface CuerpoAcademicoMaestro {
  id: string;
  tipoDocumento?: string;
  dni: string;
  cuit?: string;
  legajo?: string | null;
  nroDesignacionMapuche?: string | null;
  departamentoAcademicoId?: string | null;
  departamentoAcademicoCodigo?: string | null;
  departamentoAcademicoNombre?: string | null;
  apellido: string;
  nombre: string;
  emailInstitucional?: string;
  emailPersonal?: string;
  telefono?: string;
  tituloGrado?: string;
  maximoTituloPosgrado?: string;
  categoriaDocente?: string;
  caracterDesignacion?: string;
  dedicacionDocente?: string;
  horasSemanales?: number;
  activo?: boolean;
  asignaciones?: AsignacionCatedra[];
}

export interface CrearPlanEstudioMaestroRequest {
  carreraId: string;
  nombrePlan: string;
  codigoResolucion?: string;
  anioAprobacion?: number;
  anioInicio?: number;
  duracionAnios?: number;
  horasTotales?: number;
}

export interface CrearCuerpoAcademicoMaestroRequest {
  tipoDocumento?: string;
  dni: string;
  cuit?: string;
  legajo?: string | null;
  nroDesignacionMapuche?: string | null;
  departamentoAcademicoId?: string | null;
  apellido: string;
  nombre: string;
  emailInstitucional?: string;
  emailPersonal?: string;
  telefono?: string;
  tituloGrado?: string;
  maximoTituloPosgrado?: string;
  categoriaDocente?: string;
  caracterDesignacion?: string;
  dedicacionDocente?: string;
  horasSemanales?: number;
  activo?: boolean;
}

export interface CambiarVigenciaPlanRequest {
  estadoVigencia: EstadoVigenciaPlan;
}

@Injectable({ providedIn: 'root' })
export class DatosCentralesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/academica';

  getResumen(): Observable<ResumenDatosCentrales> {
    return this.http.get<ResumenDatosCentrales>(`${this.baseUrl}/resumen`);
  }

  getPlanesEstudio(carreraId?: string, estadoVigencia?: string): Observable<PlanEstudioMaestro[]> {
    let params = new HttpParams();
    if (carreraId) params = params.set('carreraId', carreraId);
    if (estadoVigencia) params = params.set('estadoVigencia', estadoVigencia);
    return this.http.get<PlanEstudioMaestro[]>(`${this.baseUrl}/planes-estudio`, { params });
  }

  crearPlanEstudio(req: CrearPlanEstudioMaestroRequest): Observable<PlanEstudioMaestro> {
    return this.http.post<PlanEstudioMaestro>(`${this.baseUrl}/planes-estudio`, req);
  }

  actualizarPlanEstudio(id: string, req: CrearPlanEstudioMaestroRequest): Observable<PlanEstudioMaestro> {
    return this.http.put<PlanEstudioMaestro>(`${this.baseUrl}/planes-estudio/${id}`, req);
  }

  cambiarEstadoVigencia(id: string, req: CambiarVigenciaPlanRequest): Observable<PlanEstudioMaestro> {
    return this.http.patch<PlanEstudioMaestro>(`${this.baseUrl}/planes-estudio/${id}/estado-vigencia`, req);
  }

  getCuerpoAcademico(
    carreraId?: string,
    query?: string,
    departamentoAcademicoId?: string,
    incluirInactivos = false
  ): Observable<CuerpoAcademicoMaestro[]> {
    let params = new HttpParams();
    if (carreraId) params = params.set('carreraId', carreraId);
    if (query) params = params.set('query', query);
    if (departamentoAcademicoId) params = params.set('departamentoAcademicoId', departamentoAcademicoId);
    if (incluirInactivos) params = params.set('incluirInactivos', 'true');
    return this.http.get<CuerpoAcademicoMaestro[]>(`${this.baseUrl}/cuerpo-academico`, { params });
  }

  crearCuerpoAcademico(req: CrearCuerpoAcademicoMaestroRequest): Observable<CuerpoAcademicoMaestro> {
    return this.http.post<CuerpoAcademicoMaestro>(`${this.baseUrl}/cuerpo-academico`, req);
  }

  actualizarCuerpoAcademico(id: string, req: CrearCuerpoAcademicoMaestroRequest): Observable<CuerpoAcademicoMaestro> {
    return this.http.put<CuerpoAcademicoMaestro>(`${this.baseUrl}/cuerpo-academico/${id}`, req);
  }

  agregarAsignacionDocente(docenteId: string, req: CrearAsignacionCatedraRequest): Observable<CuerpoAcademicoMaestro> {
    return this.http.post<CuerpoAcademicoMaestro>(`${this.baseUrl}/cuerpo-academico/${docenteId}/asignaciones`, req);
  }

  eliminarAsignacionDocente(docenteId: string, asignacionId: string): Observable<CuerpoAcademicoMaestro> {
    return this.http.delete<CuerpoAcademicoMaestro>(`${this.baseUrl}/cuerpo-academico/${docenteId}/asignaciones/${asignacionId}`);
  }
}
