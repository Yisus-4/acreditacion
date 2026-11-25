import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EstadoVigenciaEstandar,
  EstandarDetalle,
  EstandarResumen,
  GuardarEstandarRequest,
} from './estandar.models';

@Injectable({ providedIn: 'root' })
export class EstandarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/academica/estandares';

  listar(carreraId?: string, estadoVigencia?: EstadoVigenciaEstandar | ''): Observable<EstandarResumen[]> {
    let params = new HttpParams();
    if (carreraId) params = params.set('carreraId', carreraId);
    if (estadoVigencia) params = params.set('estadoVigencia', estadoVigencia);
    return this.http.get<EstandarResumen[]>(this.baseUrl, { params });
  }

  obtenerVigentePorCarrera(carreraId: string): Observable<EstandarDetalle> {
    return this.http.get<EstandarDetalle>(`${this.baseUrl}/vigente`, {
      params: { carreraId },
    });
  }

  obtenerDetalle(id: string): Observable<EstandarDetalle> {
    return this.http.get<EstandarDetalle>(`${this.baseUrl}/${id}`);
  }

  crear(req: GuardarEstandarRequest): Observable<EstandarDetalle> {
    return this.http.post<EstandarDetalle>(this.baseUrl, req);
  }

  actualizar(id: string, req: GuardarEstandarRequest): Observable<EstandarDetalle> {
    return this.http.put<EstandarDetalle>(`${this.baseUrl}/${id}`, req);
  }

  cambiarVigencia(id: string, estadoVigencia: EstadoVigenciaEstandar): Observable<EstandarDetalle> {
    return this.http.patch<EstandarDetalle>(`${this.baseUrl}/${id}/estado-vigencia`, { estadoVigencia });
  }

  crearNuevaVersion(id: string): Observable<EstandarDetalle> {
    return this.http.post<EstandarDetalle>(`${this.baseUrl}/${id}/nueva-version`, {});
  }
}
