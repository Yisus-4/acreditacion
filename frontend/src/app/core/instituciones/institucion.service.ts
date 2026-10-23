import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  InstitucionResponse,
  RegistrarInstitucionData,
} from './institucion.types';

export * from './institucion.types';

@Injectable({
  providedIn: 'root',
})
export class InstitucionService {
  private readonly http = inject(HttpClient);

  registrarInstitucion(formData: FormData): Observable<InstitucionResponse> {
    return this.http.post<InstitucionResponse>('/api/instituciones', formData);
  }

  listarInstituciones(): Observable<InstitucionResponse[]> {
    return this.http.get<InstitucionResponse[]>('/api/instituciones');
  }

  obtenerInstitucionPorId(id: string): Observable<InstitucionResponse> {
    return this.http.get<InstitucionResponse>(`/api/instituciones/${id}`);
  }

  actualizarInstitucion(id: string, formData: FormData): Observable<InstitucionResponse> {
    return this.http.put<InstitucionResponse>(`/api/instituciones/${id}`, formData);
  }

  actualizarInstitucionJson(id: string, datos: RegistrarInstitucionData): Observable<InstitucionResponse> {
    return this.http.put<InstitucionResponse>(`/api/instituciones/${id}`, datos);
  }

  cambiarEstado(id: string, activo: boolean): Observable<InstitucionResponse> {
    return this.http.patch<InstitucionResponse>(`/api/instituciones/${id}/estado?activo=${activo}`, {});
  }

  eliminarInstitucion(id: string): Observable<void> {
    return this.http.delete<void>(`/api/instituciones/${id}`);
  }

  crearRegistroFormData(datos: RegistrarInstitucionData, logotipo?: File | null): FormData {
    const formData = new FormData();
    formData.append('datos', JSON.stringify(datos));
    if (logotipo) {
      formData.append('logotipo', logotipo, logotipo.name);
    }
    return formData;
  }
}
