import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActividadCurricularPlan,
  ActualizarAsignaturaRequest,
  CrearAsignaturaRequest,
  PlanEstudioFt001,
  PlanEstudioVigenteDto,
} from './plan-estudio.models';

@Injectable({
  providedIn: 'root',
})
export class PlanEstudioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/planes-estudio';

  listarPlanes(carreraId?: string, procesoId?: string): Observable<PlanEstudioFt001[]> {
    let params = new HttpParams();
    if (carreraId) params = params.set('carreraId', carreraId);
    if (procesoId) params = params.set('procesoId', procesoId);
    return this.http.get<PlanEstudioFt001[]>(this.baseUrl, { params });
  }

  listarPlanesVigentesPorCarrera(carreraId: string): Observable<PlanEstudioVigenteDto[]> {
    return this.http.get<PlanEstudioVigenteDto[]>(`${this.baseUrl}/carrera/${carreraId}/vigentes`);
  }

  obtenerPlan(id: string): Observable<PlanEstudioFt001> {
    return this.http.get<PlanEstudioFt001>(`${this.baseUrl}/${id}`);
  }

  listarAsignaturas(planId: string): Observable<ActividadCurricularPlan[]> {
    return this.http.get<ActividadCurricularPlan[]>(`${this.baseUrl}/${planId}/asignaturas`);
  }

  crearPlan(dto: PlanEstudioFt001): Observable<PlanEstudioFt001> {
    return this.http.post<PlanEstudioFt001>(this.baseUrl, dto);
  }

  actualizarPlan(id: string, dto: PlanEstudioFt001): Observable<PlanEstudioFt001> {
    return this.http.put<PlanEstudioFt001>(`${this.baseUrl}/${id}`, dto);
  }

  crearAsignatura(planId: string, req: CrearAsignaturaRequest): Observable<ActividadCurricularPlan> {
    return this.http.post<ActividadCurricularPlan>(`${this.baseUrl}/${planId}/asignaturas`, req);
  }

  actualizarAsignatura(planId: string, asigId: string, req: ActualizarAsignaturaRequest): Observable<ActividadCurricularPlan> {
    return this.http.put<ActividadCurricularPlan>(`${this.baseUrl}/${planId}/asignaturas/${asigId}`, req);
  }

  eliminarAsignatura(planId: string, asigId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${planId}/asignaturas/${asigId}`);
  }

  cambiarEstadoDictado(
    planId: string,
    asigId: string,
    estadoDictado: 'SE_DICTA' | 'NO_SE_DICTA'
  ): Observable<ActividadCurricularPlan> {
    return this.http.patch<ActividadCurricularPlan>(
      `${this.baseUrl}/${planId}/asignaturas/${asigId}/estado-dictado`,
      { estadoDictado }
    );
  }
}
