import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActividadPlanTrabajoDto,
  ConvocatoriaDetalle,
  ConvocatoriaFiltro,
  ConvocatoriaResumen,
  ElaborarPlanTrabajoRequest,
  IniciarConvocatoriaRequest,
  PlanTrabajoDto,
  PlantillaPlanTrabajoDto,
} from './models/convocatoria.model';

export * from './models/convocatoria.model';

@Injectable({
  providedIn: 'root',
})
export class ConvocatoriaService {
  private readonly http = inject(HttpClient);

  listarConvocatorias(filtro?: ConvocatoriaFiltro): Observable<ConvocatoriaResumen[]> {
    let params = new HttpParams();
    if (filtro) {
      if (filtro.carreraId) {
        params = params.set('carreraId', filtro.carreraId);
      }
      if (filtro.estado) {
        params = params.set('estado', filtro.estado);
      }
      if (filtro.tipoProceso) {
        params = params.set('tipoProceso', filtro.tipoProceso);
      }
      if (filtro.anio) {
        params = params.set('anio', filtro.anio.toString());
      }
    }
    return this.http.get<ConvocatoriaResumen[]>('/api/convocatorias', { params });
  }

  obtenerConvocatoria(id: string): Observable<ConvocatoriaDetalle> {
    return this.http.get<ConvocatoriaDetalle>(`/api/convocatorias/${id}`);
  }

  iniciarConvocatoria(request: IniciarConvocatoriaRequest): Observable<ConvocatoriaDetalle> {
    return this.http.post<ConvocatoriaDetalle>('/api/convocatorias', request);
  }

  obtenerPlanTrabajo(procesoId: string): Observable<PlanTrabajoDto> {
    return this.http.get<PlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo`);
  }

  getPlantillaPlanTrabajo(procesoId: string): Observable<PlantillaPlanTrabajoDto> {
    return this.http.get<PlantillaPlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo/plantilla`);
  }

  obtenerPlantillaPlan(procesoId: string): Observable<PlantillaPlanTrabajoDto> {
    return this.getPlantillaPlanTrabajo(procesoId);
  }

  elaborarPlanTrabajo(procesoId: string, request: ElaborarPlanTrabajoRequest): Observable<ConvocatoriaDetalle> {
    const payload = {
      ...request,
      fechaLimiteConeau: request.fechaLimiteConeau ?? request.fechaLimitePresentacionConeau,
      fechaLimitePresentacionConeau: request.fechaLimitePresentacionConeau ?? request.fechaLimiteConeau,
    };
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${procesoId}/plan-trabajo/elaborar`, payload);
  }

  elaborarPlan(procesoId: string, request: ElaborarPlanTrabajoRequest): Observable<ConvocatoriaDetalle> {
    return this.elaborarPlanTrabajo(procesoId, request);
  }

  completarActividad(actividadId: string, formData: FormData): Observable<ActividadPlanTrabajoDto> {
    return this.http.post<ActividadPlanTrabajoDto>(`/api/actividades/${actividadId}/completar`, formData);
  }

  descargarDocumento(documentoId: string): Observable<Blob> {
    return this.http.get(`/api/documentos/${documentoId}/descargar`, {
      responseType: 'blob',
    });
  }

  eliminarConvocatoria(id: string): Observable<void> {
    return this.http.delete<void>(`/api/convocatorias/${id}`);
  }
}

