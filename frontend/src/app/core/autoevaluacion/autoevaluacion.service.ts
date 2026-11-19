import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Inmueble,
  RegistrarInmuebleRequest,
  TipoEspacioFisico,
  EspacioFisico,
  ConsultaEspaciosResponse,
  CrearTipoEspacioRequest,
  ExtenderPlazoRequest,
  VerificarInfraestructuraResponse,
  GuiaHigieneSeguridad,
  GuiaHigieneDocumento,
  ActualizarGuiaHigieneRequest,
  DocumentoHigieneItemRequest,
  EspacioFisicoActividadCurricularDto,
  AsignarActividadEspacioRequest,
  ResultadoSincronizacionDto,
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

  obtenerInmueblesPorProceso(procesoId: string, convocatoriaId?: string | null, carreraId?: string | null): Observable<Inmueble[]> {
    let params = new HttpParams();
    if (convocatoriaId) params = params.set('convocatoriaId', convocatoriaId);
    if (carreraId) params = params.set('carreraId', carreraId);
    return this.http.get<Inmueble[]>(`${this.baseUrl}/procesos/${procesoId}/inmuebles`, { params });
  }

  obtenerEstadoInfraestructuraCarrera(
    procesoId: string,
    convocatoriaId?: string | null,
    carreraId?: string | null
  ): Observable<{
    procesoId: string;
    verificado: boolean;
    rutaCarpeta: string;
    nombreArchivo: string;
    rutaCompleta: string;
    fechaVerificacion?: string;
  }> {
    let params = new HttpParams();
    if (convocatoriaId) params = params.set('convocatoriaId', convocatoriaId);
    if (carreraId) params = params.set('carreraId', carreraId);
    return this.http.get<any>(`${this.baseUrl}/procesos/${procesoId}/infraestructura/estado`, { params });
  }

  regenerarSnapshotDesdeMaestro(procesoId: string): Observable<Inmueble[]> {
    return this.http.post<Inmueble[]>(`${this.baseUrl}/procesos/${procesoId}/inmuebles/copiar-maestro`, {});
  }

  extenderPlazoInfraestructura(procesoId: string, req: ExtenderPlazoRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/procesos/${procesoId}/infraestructura/extender-plazo`, req);
  }

  verificarYRemitirPorProceso(
    procesoId: string,
    convocatoriaId?: string | null,
    carreraId?: string | null
  ): Observable<VerificarInfraestructuraResponse> {
    let params = new HttpParams();
    if (convocatoriaId) params = params.set('convocatoriaId', convocatoriaId);
    if (carreraId) params = params.set('carreraId', carreraId);
    return this.http.post<VerificarInfraestructuraResponse>(
      `${this.baseUrl}/procesos/${procesoId}/infraestructura/verificar-y-remitir`,
      {},
      { params }
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

  actualizarHigieneSeguridadEspacio(
    espacioId: string,
    data: string | { observacionHigieneSeguridad: string }
  ): Observable<EspacioFisico> {
    const observacionHigieneSeguridad = typeof data === 'string' ? data : data.observacionHigieneSeguridad;
    return this.http.patch<EspacioFisico>(`${this.baseUrl}/espacios/${espacioId}/higiene-seguridad`, {
      observacionHigieneSeguridad,
    });
  }

  consultarEspaciosPorM2(
    optionsOrTipoId?: string | { procesoId?: string; minM2?: number; m2Min?: number; maxM2?: number; m2Max?: number; tipoEspacioId?: string; sedeId?: string },
    m2Min?: number,
    m2Max?: number,
    sedeId?: string
  ): Observable<ConsultaEspaciosResponse> {
    let params = new HttpParams();
    if (typeof optionsOrTipoId === 'object' && optionsOrTipoId !== null) {
      if (optionsOrTipoId.procesoId) params = params.set('procesoId', optionsOrTipoId.procesoId);
      if (optionsOrTipoId.tipoEspacioId) params = params.set('tipoEspacioId', optionsOrTipoId.tipoEspacioId);
      const min = optionsOrTipoId.m2Min ?? optionsOrTipoId.minM2;
      if (min !== undefined && min !== null) params = params.set('m2Min', min.toString());
      const max = optionsOrTipoId.m2Max ?? optionsOrTipoId.maxM2;
      if (max !== undefined && max !== null) params = params.set('m2Max', max.toString());
      if (optionsOrTipoId.sedeId) params = params.set('sedeId', optionsOrTipoId.sedeId);
    } else {
      if (optionsOrTipoId) params = params.set('tipoEspacioId', optionsOrTipoId);
      if (m2Min !== undefined && m2Min !== null) params = params.set('m2Min', m2Min.toString());
      if (m2Max !== undefined && m2Max !== null) params = params.set('m2Max', m2Max.toString());
      if (sedeId) params = params.set('sedeId', sedeId);
    }
    return this.http.get<ConsultaEspaciosResponse>(`${this.baseUrl}/espacios/consulta`, { params });
  }

  extenderPlazo(procesoId: string, req: ExtenderPlazoRequest & { actividadCodigo?: string }): Observable<any> {
    if (req.actividadCodigo === '4.2') {
      return this.extenderPlazoHigiene(procesoId, req);
    }
    return this.extenderPlazoInfraestructura(procesoId, req);
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

  eliminarDocumentoGuiaHigiene(id: string, documentoId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/higiene-seguridad/${id}/documentos/${documentoId}`);
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

  // Multi-Plan y Sincronización
  obtenerActividadesPorEspacio(espacioId: string): Observable<EspacioFisicoActividadCurricularDto[]> {
    return this.http.get<EspacioFisicoActividadCurricularDto[]>(`${this.baseUrl}/espacios/${espacioId}/actividades-curriculares`);
  }

  guardarActividadesPorEspacio(
    espacioId: string,
    asignaciones: AsignarActividadEspacioRequest[]
  ): Observable<EspacioFisicoActividadCurricularDto[]> {
    return this.http.put<EspacioFisicoActividadCurricularDto[]>(
      `${this.baseUrl}/espacios/${espacioId}/actividades-curriculares`,
      asignaciones
    );
  }

  copiarDatosCentrales(procesoId: string, payload?: { modo?: string; secciones?: string[] }): Observable<ResultadoSincronizacionDto> {
    return this.http.post<ResultadoSincronizacionDto>(
      `${this.baseUrl}/procesos/${procesoId}/copiar-datos-centrales`,
      payload || {}
    );
  }
}
