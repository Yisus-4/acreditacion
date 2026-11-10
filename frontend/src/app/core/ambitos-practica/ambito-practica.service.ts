import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AmbitoPractica, AutoevaluacionCarreraAmbito, TipoCategoriaAmbito } from './ambito-practica.models';
import { DocenteResumen } from '../planes-estudio/plan-estudio.models';

@Injectable({
  providedIn: 'root',
})
export class AmbitoPracticaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/ambitos-practica';

  listarAmbitos(categoria?: TipoCategoriaAmbito, buscar?: string): Observable<AmbitoPractica[]> {
    let params = new HttpParams();
    if (categoria) {
      params = params.set('categoria', categoria);
    }
    if (buscar && buscar.trim()) {
      params = params.set('q', buscar.trim());
    }
    return this.http.get<AmbitoPractica[]>(this.baseUrl, { params });
  }

  obtenerAmbito(id: string): Observable<AmbitoPractica> {
    return this.http.get<AmbitoPractica>(`${this.baseUrl}/${id}`);
  }

  crearAmbito(dto: AmbitoPractica): Observable<AmbitoPractica> {
    return this.http.post<AmbitoPractica>(this.baseUrl, dto);
  }

  actualizarAmbito(id: string, dto: AmbitoPractica): Observable<AmbitoPractica> {
    return this.http.put<AmbitoPractica>(`${this.baseUrl}/${id}`, dto);
  }

  eliminarAmbito(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listarAutoevaluaciones(carreraId?: string, procesoId?: string): Observable<AutoevaluacionCarreraAmbito[]> {
    let params = new HttpParams();
    if (carreraId) params = params.set('carreraId', carreraId);
    if (procesoId) params = params.set('procesoId', procesoId);
    return this.http.get<AutoevaluacionCarreraAmbito[]>(`${this.baseUrl}/autoevaluacion`, { params });
  }

  guardarAutoevaluacion(dto: AutoevaluacionCarreraAmbito): Observable<AutoevaluacionCarreraAmbito> {
    return this.http.post<AutoevaluacionCarreraAmbito>(`${this.baseUrl}/autoevaluacion`, dto);
  }

  completarActividadAmbitos(
    procesoId: string,
    actividad: string,
    carreraId?: string
  ): Observable<{ completado: boolean; mensaje: string }> {
    let params = new HttpParams().set('actividad', actividad);
    if (carreraId) {
      params = params.set('carreraId', carreraId);
    }
    return this.http.post<{ completado: boolean; mensaje: string }>(
      `${this.baseUrl}/carrera/${procesoId}/completar`,
      null,
      { params }
    );
  }

  listarDocentes(query?: string): Observable<DocenteResumen[]> {
    let params = new HttpParams();
    if (query && query.trim()) {
      params = params.set('query', query.trim());
    }
    return this.http.get<DocenteResumen[]>('/api/autoevaluacion/docentes', { params });
  }
}
