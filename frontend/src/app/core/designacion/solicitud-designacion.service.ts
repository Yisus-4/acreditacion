import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AgregarAsignacionCurricularRequest,
  AsignacionCurricularPeriodo,
  CambioSolicitudDesignacion,
  CrearSolicitudDesignacionRequest,
  DesignacionDocenteItem,
  GuardarDesignacionDocenteRequest,
  GuardarDistribucionHorariaRequest,
  MotivoCambioRequest,
  RevisionSgaRequest,
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

  listarHistorial(solicitudId: string): Observable<CambioSolicitudDesignacion[]> {
    return this.http.get<CambioSolicitudDesignacion[]>(`${this.baseUrl}/${solicitudId}/historial`);
  }

  crearSolicitud(req: CrearSolicitudDesignacionRequest): Observable<SolicitudDesignacionDetalle> {
    return this.http.post<SolicitudDesignacionDetalle>(this.baseUrl, req);
  }

  presentarSolicitud(solicitudId: string): Observable<SolicitudDesignacionDetalle> {
    return this.http.post<SolicitudDesignacionDetalle>(`${this.baseUrl}/${solicitudId}/presentar`, {});
  }

  aprobarSolicitud(solicitudId: string, req?: RevisionSgaRequest): Observable<SolicitudDesignacionDetalle> {
    return this.http.post<SolicitudDesignacionDetalle>(`${this.baseUrl}/${solicitudId}/aprobar`, req ?? {});
  }

  observarSolicitud(solicitudId: string, req: RevisionSgaRequest): Observable<SolicitudDesignacionDetalle> {
    return this.http.post<SolicitudDesignacionDetalle>(`${this.baseUrl}/${solicitudId}/observar`, req);
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

  excluirDesignacion(
    solicitudId: string,
    designacionId: string,
    req: MotivoCambioRequest
  ): Observable<DesignacionDocenteItem> {
    return this.http.post<DesignacionDocenteItem>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/excluir`,
      req
    );
  }

  reactivarDesignacion(
    solicitudId: string,
    designacionId: string,
    req?: MotivoCambioRequest
  ): Observable<DesignacionDocenteItem> {
    return this.http.post<DesignacionDocenteItem>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/reactivar`,
      req ?? {}
    );
  }

  eliminarDesignacion(solicitudId: string, designacionId: string, motivo?: string | null): Observable<void> {
    let params = new HttpParams();
    if (motivo) params = params.set('motivo', motivo);
    return this.http.delete<void>(`${this.baseUrl}/${solicitudId}/designaciones/${designacionId}`, { params });
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
    asignacionId: string,
    motivo?: string | null
  ): Observable<void> {
    let params = new HttpParams();
    if (motivo) params = params.set('motivo', motivo);
    return this.http.delete<void>(
      `${this.baseUrl}/${solicitudId}/designaciones/${designacionId}/asignaciones/${asignacionId}`,
      { params }
    );
  }
}
