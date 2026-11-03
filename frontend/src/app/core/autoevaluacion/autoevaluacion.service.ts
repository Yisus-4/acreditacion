import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Inmueble,
  RegistrarInmuebleRequest,
  TipoEspacioFisico,
  CrearTipoEspacioRequest,
  ExtenderPlazoRequest,
  VerificarInfraestructuraResponse,
  GuiaHigieneSeguridad,
  GuiaHigieneDocumento,
  ActualizarGuiaHigieneRequest,
  DocumentoHigieneItemRequest,
} from './autoevaluacion.models';

export * from './autoevaluacion.models';

@Injectable({
  providedIn: 'root',
})
export class AutoevaluacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/autoevaluacion';

  // ---------------------------------------------------------------------------
  // RG-005: Inmuebles
  // ---------------------------------------------------------------------------

  listarInmuebles(esMaestro?: boolean): Observable<Inmueble[]> {
    let params = new HttpParams();
    if (esMaestro !== undefined) {
      params = params.set('esMaestro', esMaestro);
    }
    return this.http.get<Inmueble[]>(`${this.baseUrl}/inmuebles`, { params });
  }

  listarInmueblesMaestros(): Observable<Inmueble[]> {
    return this.http.get<Inmueble[]>(`${this.baseUrl}/inmuebles/maestro`);
  }

  obtenerInmueblesPorProceso(procesoId: string): Observable<Inmueble[]> {
    return this.http.get<Inmueble[]>(`${this.baseUrl}/procesos/${procesoId}/inmuebles`);
  }

  regenerarSnapshotDesdeMaestro(procesoId: string): Observable<Inmueble[]> {
    return this.http.post<Inmueble[]>(`${this.baseUrl}/procesos/${procesoId}/inmuebles/copiar-maestro`, {});
  }

  extenderPlazoInfraestructura(procesoId: string, req: ExtenderPlazoRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/procesos/${procesoId}/infraestructura/extender-plazo`, req);
  }

  verificarYRemitirPorProceso(procesoId: string): Observable<VerificarInfraestructuraResponse> {
    return this.http.post<VerificarInfraestructuraResponse>(
      `${this.baseUrl}/procesos/${procesoId}/infraestructura/verificar-y-remitir`,
      {}
    );
  }

  obtenerInmueble(id: string): Observable<Inmueble> {
    return this.http.get<Inmueble>(`${this.baseUrl}/inmuebles/${id}`);
  }

  registrarInmueble(data: RegistrarInmuebleRequest): Observable<Inmueble> {
    return this.http.post<Inmueble>(`${this.baseUrl}/inmuebles`, data);
  }

  actualizarInmueble(id: string, data: RegistrarInmuebleRequest): Observable<Inmueble> {
    return this.http.put<Inmueble>(`${this.baseUrl}/inmuebles/${id}`, data);
  }

  verificarYRemitirADea(id: string): Observable<Inmueble> {
    return this.http.post<Inmueble>(`${this.baseUrl}/inmuebles/${id}/verificar`, {});
  }

  eliminarInmueble(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/inmuebles/${id}`);
  }

  listarTiposEspacio(): Observable<TipoEspacioFisico[]> {
    return this.http.get<TipoEspacioFisico[]>(`${this.baseUrl}/tipos-espacio`);
  }

  crearTipoEspacio(data: CrearTipoEspacioRequest): Observable<TipoEspacioFisico> {
    return this.http.post<TipoEspacioFisico>(`${this.baseUrl}/tipos-espacio`, data);
  }

  // ---------------------------------------------------------------------------
  // GT-003: Higiene y Seguridad
  // ---------------------------------------------------------------------------

  obtenerGuiaHigieneMaestra(): Observable<GuiaHigieneSeguridad> {
    return this.http.get<GuiaHigieneSeguridad>(`${this.baseUrl}/higiene-seguridad/maestro`);
  }

  obtenerGuiaHigienePorProceso(procesoId: string): Observable<GuiaHigieneSeguridad> {
    return this.http.get<GuiaHigieneSeguridad>(`${this.baseUrl}/higiene-seguridad/procesos/${procesoId}`);
  }

  actualizarGuiaHigiene(id: string, req: ActualizarGuiaHigieneRequest): Observable<GuiaHigieneSeguridad> {
    return this.http.put<GuiaHigieneSeguridad>(`${this.baseUrl}/higiene-seguridad/${id}`, req);
  }

  agregarDocumentoGuiaHigiene(id: string, req: DocumentoHigieneItemRequest): Observable<GuiaHigieneDocumento> {
    return this.http.post<GuiaHigieneDocumento>(`${this.baseUrl}/higiene-seguridad/${id}/documentos`, req);
  }

  extenderPlazoHigiene(procesoId: string, req: ExtenderPlazoRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/higiene-seguridad/procesos/${procesoId}/extender-plazo`, req);
  }

  verificarYRemitirHigienePorProceso(procesoId: string): Observable<VerificarInfraestructuraResponse> {
    return this.http.post<VerificarInfraestructuraResponse>(
      `${this.baseUrl}/higiene-seguridad/procesos/${procesoId}/verificar-y-remitir`,
      {}
    );
  }
}
