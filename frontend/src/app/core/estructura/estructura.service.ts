import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ActualizarCarreraData,
  ActualizarSedeLocalizacionData,
  ActualizarUnidadAcademicaData,
  CarreraResponse,
  RegistrarCarreraData,
  RegistrarSedeLocalizacionData,
  RegistrarUnidadAcademicaData,
  SedeLocalizacionResponse,
  UnidadAcademicaResponse,
} from './estructura.types';

export * from './estructura.types';

@Injectable({
  providedIn: 'root',
})
export class EstructuraService {
  private readonly http = inject(HttpClient);

  // ──────────────────────────── Unidades Académicas ────────────────────────────
  listarUnidadesAcademicas(institucionId?: string): Observable<UnidadAcademicaResponse[]> {
    let params = new HttpParams();
    if (institucionId) {
      params = params.set('institucionId', institucionId);
    }
    return this.http.get<UnidadAcademicaResponse[]>('/api/unidades-academicas', { params });
  }

  crearUnidadAcademica(data: RegistrarUnidadAcademicaData): Observable<UnidadAcademicaResponse> {
    return this.http.post<UnidadAcademicaResponse>('/api/unidades-academicas', data);
  }

  actualizarUnidadAcademica(id: string, data: ActualizarUnidadAcademicaData): Observable<UnidadAcademicaResponse> {
    return this.http.put<UnidadAcademicaResponse>(`/api/unidades-academicas/${id}`, data);
  }

  cambiarEstadoUnidadAcademica(id: string, activo: boolean): Observable<UnidadAcademicaResponse> {
    return this.http.patch<UnidadAcademicaResponse>(`/api/unidades-academicas/${id}/estado`, { activo });
  }

  eliminarUnidadAcademica(id: string): Observable<void> {
    return this.http.delete<void>(`/api/unidades-academicas/${id}`);
  }

  // ──────────────────────────── Sedes y Localizaciones ────────────────────────────
  listarSedes(institucionId?: string): Observable<SedeLocalizacionResponse[]> {
    let params = new HttpParams();
    if (institucionId) {
      params = params.set('institucionId', institucionId);
    }
    return this.http.get<SedeLocalizacionResponse[]>('/api/sedes-localizaciones', { params });
  }

  crearSede(data: RegistrarSedeLocalizacionData): Observable<SedeLocalizacionResponse> {
    return this.http.post<SedeLocalizacionResponse>('/api/sedes-localizaciones', data);
  }

  actualizarSede(id: string, data: ActualizarSedeLocalizacionData): Observable<SedeLocalizacionResponse> {
    return this.http.put<SedeLocalizacionResponse>(`/api/sedes-localizaciones/${id}`, data);
  }

  cambiarEstadoSede(id: string, activo: boolean): Observable<SedeLocalizacionResponse> {
    return this.http.patch<SedeLocalizacionResponse>(`/api/sedes-localizaciones/${id}/estado`, { activo });
  }

  eliminarSede(id: string): Observable<void> {
    return this.http.delete<void>(`/api/sedes-localizaciones/${id}`);
  }

  // ──────────────────────────── Carreras de Grado ────────────────────────────
  listarCarreras(unidadAcademicaId?: string): Observable<CarreraResponse[]> {
    let params = new HttpParams();
    if (unidadAcademicaId) {
      params = params.set('unidadAcademicaId', unidadAcademicaId);
    }
    return this.http.get<CarreraResponse[]>('/api/carreras', { params });
  }

  crearCarrera(data: RegistrarCarreraData): Observable<CarreraResponse> {
    return this.http.post<CarreraResponse>('/api/carreras', data);
  }

  actualizarCarrera(id: string, data: ActualizarCarreraData): Observable<CarreraResponse> {
    return this.http.put<CarreraResponse>(`/api/carreras/${id}`, data);
  }

  cambiarEstadoCarrera(id: string, activo: boolean): Observable<CarreraResponse> {
    return this.http.patch<CarreraResponse>(`/api/carreras/${id}/estado`, { activo });
  }

  eliminarCarrera(id: string): Observable<void> {
    return this.http.delete<void>(`/api/carreras/${id}`);
  }
}
