import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocenteDisponibleDto, ProgramaAnaliticoAsignatura } from './programa-analitico.models';

@Injectable({
  providedIn: 'root',
})
export class ProgramaAnaliticoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/programas-analiticos';

  listarProgramas(procesoId?: string, planId?: string): Observable<ProgramaAnaliticoAsignatura[]> {
    let params = new HttpParams();
    if (procesoId) params = params.set('procesoId', procesoId);
    if (planId) params = params.set('planId', planId);
    return this.http.get<ProgramaAnaliticoAsignatura[]>(this.baseUrl, { params });
  }

  subirPrograma(
    archivo: File,
    asignaturaPlanId: string,
    procesoId?: string,
    resolucionAprobacion?: string,
    vigenciaDesde?: number,
    vigenciaHasta?: number,
    observaciones?: string
  ): Observable<ProgramaAnaliticoAsignatura> {
    const formData = new FormData();
    formData.append('archivo', archivo, archivo.name);
    formData.append('asignaturaPlanId', asignaturaPlanId);
    if (procesoId) formData.append('procesoId', procesoId);
    if (resolucionAprobacion) formData.append('resolucionAprobacion', resolucionAprobacion);
    if (vigenciaDesde) formData.append('vigenciaDesde', vigenciaDesde.toString());
    if (vigenciaHasta) formData.append('vigenciaHasta', vigenciaHasta.toString());
    if (observaciones) formData.append('observaciones', observaciones);

    return this.http.post<ProgramaAnaliticoAsignatura>(`${this.baseUrl}/upload`, formData);
  }

  descargarUrl(id: string): string {
    return `${this.baseUrl}/${id}/descargar`;
  }

  listarDocentesDisponibles(): Observable<DocenteDisponibleDto[]> {
    return this.http.get<DocenteDisponibleDto[]>(`${this.baseUrl}/docentes-disponibles`);
  }

  designarDocente(
    asignaturaPlanId: string,
    docenteUsuarioId: string | null,
    docenteNombre?: string
  ): Observable<ProgramaAnaliticoAsignatura> {
    return this.http.patch<ProgramaAnaliticoAsignatura>(
      `${this.baseUrl}/asignatura/${asignaturaPlanId}/designar-docente`,
      { docenteUsuarioId: docenteUsuarioId || '', docenteNombre: docenteNombre || '' }
    );
  }

  validarYCompletarActividad84(procesoId?: string, planId?: string): Observable<{ success: boolean; mensaje: string; programasCargados: number; totalAsignaturas: number }> {
    let params = new HttpParams();
    if (procesoId) params = params.set('procesoId', procesoId);
    if (planId) params = params.set('planId', planId);
    return this.http.post<{ success: boolean; mensaje: string; programasCargados: number; totalAsignaturas: number }>(
      `${this.baseUrl}/validar-completar-8-4`,
      {},
      { params }
    );
  }
}
