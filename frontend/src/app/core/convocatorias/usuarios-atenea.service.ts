import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GuardarUsuariosAteneaRequest, RegistroUsuariosAteneaDto } from './models/usuarios-atenea.model';

@Injectable({
  providedIn: 'root'
})
export class UsuariosAteneaService {
  private readonly http = inject(HttpClient);

  consultar(procesoId: string): Observable<RegistroUsuariosAteneaDto> {
    return this.http.get<RegistroUsuariosAteneaDto>(`/api/procesos/${procesoId}/usuarios-atenea`);
  }

  guardar(procesoId: string, req: GuardarUsuariosAteneaRequest): Observable<RegistroUsuariosAteneaDto> {
    return this.http.put<RegistroUsuariosAteneaDto>(`/api/procesos/${procesoId}/usuarios-atenea`, req);
  }

  finalizar(procesoId: string): Observable<RegistroUsuariosAteneaDto> {
    return this.http.post<RegistroUsuariosAteneaDto>(`/api/procesos/${procesoId}/usuarios-atenea/finalizar`, {});
  }

  actualizarPlazo(procesoId: string, diasHabiles?: number, nuevaFechaLimite?: string): Observable<RegistroUsuariosAteneaDto> {
    const query = nuevaFechaLimite
      ? `nuevaFechaLimite=${encodeURIComponent(nuevaFechaLimite)}`
      : `diasHabiles=${diasHabiles ?? 2}`;
    return this.http.post<RegistroUsuariosAteneaDto>(
      `/api/procesos/${procesoId}/usuarios-atenea/plazo?${query}`,
      {}
    );
  }

  descargarPdf(procesoId: string): Observable<Blob> {
    return this.http.get(`/api/procesos/${procesoId}/usuarios-atenea/descargar`, {
      responseType: 'blob'
    });
  }
}
