import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConvenioInstitucional } from './convenio.models';

@Injectable({
  providedIn: 'root',
})
export class ConvenioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/convenios';

  listarConvenios(vigente?: boolean, buscar?: string): Observable<ConvenioInstitucional[]> {
    let params = new HttpParams();
    if (vigente !== undefined && vigente !== null) {
      params = params.set('vigente', vigente.toString());
    }
    if (buscar && buscar.trim()) {
      params = params.set('q', buscar.trim());
    }
    return this.http.get<ConvenioInstitucional[]>(this.baseUrl, { params });
  }

  obtenerConvenio(id: string): Observable<ConvenioInstitucional> {
    return this.http.get<ConvenioInstitucional>(`${this.baseUrl}/${id}`);
  }

  crearConvenio(dto: ConvenioInstitucional): Observable<ConvenioInstitucional> {
    return this.http.post<ConvenioInstitucional>(this.baseUrl, dto);
  }

  actualizarConvenio(id: string, dto: ConvenioInstitucional): Observable<ConvenioInstitucional> {
    return this.http.put<ConvenioInstitucional>(`${this.baseUrl}/${id}`, dto);
  }

  eliminarConvenio(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
