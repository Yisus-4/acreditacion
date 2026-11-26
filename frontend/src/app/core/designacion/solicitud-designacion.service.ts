import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AgregarAsignacionCurricularRequest,
  AsignacionCurricularPeriodo,
  CrearSolicitudDesignacionRequest,
  DesignacionDocenteItem,
  GuardarDesignacionDocenteRequest,
  GuardarDistribucionHorariaRequest,
  SolicitudDesignacionDetalle,
  SolicitudDesignacionResumen,
} from './solicitud-designacion.models';

@Injectable({ providedIn: 'root' })
export class SolicitudDesignacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/academica/solicitudes-designacion';

  listarSolicitudes(periodoId?: string | null, departamentoId?: string | null): Observable<SolicitudDesignacionResumen[]> {
    let params = new HttpParams();
    if (periodoId) params = params.set('periodoId', periodoId);
    if (departamentoId) params = params.set('departamentoId', departamentoId);
    return this.http.get<SolicitudDesignacionResumen[]>(this.baseUrl, { params });
  }

  obtenerDetalle(solicitudId: string): Observable<SolicitudDesignacionDetalle> {
    return this.http.get<SolicitudDesignacionDetalle>(`${this.baseUrl}/${solicitudId}`);
  }

  crearSolicitud(req: CrearSolicitudDesignacionRequest): Observable<SolicitudDesignacionDetalle> {
    return this.http.post<SolicitudDesignacionDetalle>(this.baseUrl, req);
  }

  agregarDesignacion(
    solicitudId: string,
    req: GuardarDesignacionDocenteRequest
  ): Observable<DesignacionDocenteItem> {
    return this.http.post<DesignacionDocenteItem>(`${this.baseUrl}/${solicitudId}/designaciones`, req);
  }

  actualizarDesignacion(
    solicitudId: string,
    designacionId: string,
    req: GuardarDesignacionDocenteRequest
  ): Observable<DesignacionDocenteItem> {
    return this.http.put<DesignacionDocenteItem>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}`,
      req
    );
  }

  eliminarDesignacion(solicitudId: string, designacionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${solicitudId}/designaciones/${designacionId}`);
  }

  actualizarDistribucion(
    solicitudId: string,
    designacionId: string,
    req: GuardarDistribucionHorariaRequest
  ): Observable<DesignacionDocenteItem> {
    return this.http.put<DesignacionDocenteItem>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/distribucion`,
      req
    );
  }

  agregarAsignacion(
    solicitudId: string,
    designacionId: string,
    req: AgregarAsignacionCurricularRequest
  ): Observable<AsignacionCurricularPeriodo> {
    return this.http.post<AsignacionCurricularPeriodo>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/asignaciones`,
      req
    );
  }

  eliminarAsignacion(
    solicitudId: string,
    designacionId: string,
    asignacionId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/asignaciones/${asignacionId}`
    );
  }
}
