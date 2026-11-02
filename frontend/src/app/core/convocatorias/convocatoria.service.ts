import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActividadPlanTrabajoDto,
  CompletarActividadPayload,
  ConvocatoriaDetalle,
  ConvocatoriaFiltro,
  ConvocatoriaResumen,
  ElaborarPlanTrabajoRequest,
  FichaFormalizacionFt012Dto,
  GuardarCronogramaPlanRequest,
  GuardarRg001Request,
  RegistroCaDto,
  GuardarRegistroCaBorradorRequest,
  CatalogoMiembroComisionDto,
  IniciarConvocatoriaRequest,
  PlanTrabajoDto,
  PlantillaPlanTrabajoDto,
  RegistroConvocatoria,
} from './models/convocatoria.model';

export * from './models/convocatoria.model';

@Injectable({
  providedIn: 'root',
})
export class ConvocatoriaService {
  private readonly http = inject(HttpClient);

  listarConvocatorias(filtro?: ConvocatoriaFiltro): Observable<ConvocatoriaResumen[]> {
    let params = new HttpParams();
    if (filtro) {
      if (filtro.unidadAcademicaId) {
        params = params.set('unidadAcademicaId', filtro.unidadAcademicaId);
      }
      if (filtro.carreraId) {
        params = params.set('carreraId', filtro.carreraId);
      }
      if (filtro.estado) {
        params = params.set('estado', filtro.estado);
      }
      if (filtro.tipoProceso) {
        params = params.set('tipoProceso', filtro.tipoProceso);
      }
      if (filtro.anio) {
        params = params.set('anio', filtro.anio.toString());
      }
    }
    return this.http.get<ConvocatoriaResumen[]>('/api/convocatorias', { params });
  }

  obtenerConvocatoria(id: string): Observable<ConvocatoriaDetalle> {
    return this.http.get<ConvocatoriaDetalle>(`/api/convocatorias/${id}`);
  }

  iniciarConvocatoria(request: IniciarConvocatoriaRequest): Observable<ConvocatoriaDetalle> {
    return this.http.post<ConvocatoriaDetalle>('/api/convocatorias', request);
  }

  confirmarConvocatoriaDe(id: string, carrerasSeleccionadas: string[]): Observable<ConvocatoriaDetalle> {
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${id}/confirmar-de`, { carrerasSeleccionadas });
  }

  reiniciarConvocatoriaTest(convocatoriaId: string): Observable<ConvocatoriaDetalle> {
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${convocatoriaId}/reiniciar-test`, {});
  }

  obtenerPlanTrabajo(procesoId: string): Observable<PlanTrabajoDto> {
    return this.http.get<PlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo`);
  }

  getPlantillaPlanTrabajo(procesoId: string): Observable<PlantillaPlanTrabajoDto> {
    return this.http.get<PlantillaPlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo/plantilla`);
  }

  elaborarPlanTrabajo(procesoId: string, request: ElaborarPlanTrabajoRequest): Observable<ConvocatoriaDetalle> {
    const payload = {
      ...request,
      fechaLimiteConeau: request.fechaLimiteConeau ?? request.fechaLimitePresentacionConeau,
      fechaLimitePresentacionConeau: request.fechaLimitePresentacionConeau ?? request.fechaLimiteConeau,
    };
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${procesoId}/plan-trabajo/elaborar`, payload);
  }

  completarActividad(
    actividadId: string,
    payload: FormData | CompletarActividadPayload
  ): Observable<ActividadPlanTrabajoDto> {
    let formData: FormData;
    if (payload instanceof FormData) {
      formData = payload;
    } else {
      formData = new FormData();
      formData.append('fechaReunion', payload.fechaReunion);
      for (const area of payload.areasInformadas ?? []) {
        formData.append('areasInformadas', area);
      }
      for (const doc of payload.documentacionRemitida ?? []) {
        formData.append('documentacionRemitida', doc);
      }
      const textoObs = (payload.observaciones ?? payload.sintesisAcuerdos ?? '').trim();
      if (textoObs) {
        formData.append('observaciones', textoObs);
        formData.append('sintesisAcuerdos', textoObs);
      }
      if (payload.archivo) {
        formData.append('archivo', payload.archivo);
        formData.append('tipoDocumento', payload.tipoDocumento ?? 'ACTA_REUNION_COORDINACION');
      }
    }
    return this.http.post<ActividadPlanTrabajoDto>(`/api/actividades/${actividadId}/completar`, formData);
  }

  extenderPlazoActividad(actividadId: string, nuevaFechaLimite: string): Observable<ActividadPlanTrabajoDto> {
    const params = new HttpParams().set('nuevaFechaLimite', nuevaFechaLimite);
    return this.http.post<ActividadPlanTrabajoDto>(`/api/actividades/${actividadId}/plazo`, {}, { params });
  }

  completarActividadSimple(actividadId: string): Observable<ActividadPlanTrabajoDto> {
    return this.http.post<ActividadPlanTrabajoDto>(`/api/actividades/${actividadId}/completar-simple`, {});
  }

  descargarDocumento(documentoId: string): Observable<Blob> {
    return this.http.get(`/api/documentos/${documentoId}/descargar`, {
      responseType: 'blob',
    });
  }

  eliminarConvocatoria(id: string): Observable<void> {
    return this.http.delete<void>(`/api/convocatorias/${id}`);
  }

  // Formalización electrónica FT-012
  obtenerFormalizacion(procesoId: string): Observable<FichaFormalizacionFt012Dto> {
    return this.http.get<FichaFormalizacionFt012Dto>(`/api/convocatorias/${procesoId}/formalizacion`);
  }

  guardarFichaFormalizacion(
    procesoId: string,
    payload: Partial<FichaFormalizacionFt012Dto> & { enviar: boolean }
  ): Observable<FichaFormalizacionFt012Dto> {
    return this.http.put<FichaFormalizacionFt012Dto>(
      `/api/convocatorias/${procesoId}/formalizacion/ficha`,
      payload
    );
  }

  descargarComprobanteFormalizacion(procesoId: string): Observable<Blob> {
    return this.http.get(`/api/convocatorias/${procesoId}/formalizacion/comprobante`, {
      responseType: 'blob',
    });
  }

  // Actividad 2 / Nueva Estructura: Formulario RG-001 y Plan de Trabajo independientes
  guardarRg001Borrador(procesoId: string, request: GuardarRg001Request): Observable<RegistroConvocatoria> {
    return this.http.post<RegistroConvocatoria>(`/api/convocatorias/${procesoId}/rg001/borrador`, request);
  }

  enviarRg001(procesoId: string, request: GuardarRg001Request): Observable<RegistroConvocatoria> {
    return this.http.post<RegistroConvocatoria>(`/api/convocatorias/${procesoId}/rg001/enviar`, request);
  }

  guardarPlanTrabajoBorrador(procesoId: string, request: GuardarCronogramaPlanRequest): Observable<PlanTrabajoDto> {
    return this.http.post<PlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo/borrador`, request);
  }

  enviarPlanTrabajo(procesoId: string, request: GuardarCronogramaPlanRequest): Observable<PlanTrabajoDto> {
    return this.http.post<PlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo/enviar`, request);
  }

  finalizarActividadPlanTrabajo(procesoId: string): Observable<ConvocatoriaDetalle> {
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${procesoId}/plan-trabajo/finalizar-actividad`, {});
  }

  // ==========================================
  // Comisión de Autoevaluación (CA · RG-002)
  // ==========================================

  obtenerRegistroCa(procesoId: string): Observable<RegistroCaDto> {
    return this.http.get<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion`);
  }

  solicitarConformacionCa(procesoId: string, diasPlazo?: number, nuevaFechaLimite?: string): Observable<RegistroCaDto> {
    let params = new HttpParams();
    if (nuevaFechaLimite) {
      params = params.set('nuevaFechaLimite', nuevaFechaLimite);
    } else if (diasPlazo && diasPlazo > 0) {
      params = params.set('diasPlazo', String(diasPlazo));
    }
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/solicitar`, {}, { params });
  }

  adjuntarResolucionCa(procesoId: string, archivo: File): Observable<RegistroCaDto> {
    const formData = new FormData();
    formData.append('resolucion', archivo);
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/resolucion`, formData);
  }

  descargarInstructivoIt001(procesoId: string): Observable<Blob> {
    return this.http.get(`/api/convocatorias/${procesoId}/comision-autoevaluacion/instructivo-it001`, {
      responseType: 'blob',
    });
  }

  guardarRegistroCaBorrador(procesoId: string, request: GuardarRegistroCaBorradorRequest): Observable<RegistroCaDto> {
    return this.http.put<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/borrador`, request);
  }

  remitirRegistroCa(procesoId: string, formData: FormData): Observable<RegistroCaDto> {
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/remitir`, formData);
  }

  aprobarRegistroCa(procesoId: string): Observable<RegistroCaDto> {
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/aprobar`, {});
  }

  observarRegistroCa(procesoId: string, observaciones: string): Observable<RegistroCaDto> {
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/observar`, {
      observaciones,
    });
  }

  descargarResolucionCa(procesoId: string): Observable<Blob> {
    return this.http.get(`/api/convocatorias/${procesoId}/comision-autoevaluacion/resolucion/descargar`, {
      responseType: 'blob',
    });
  }

  consultarCatalogoMiembrosCa(busqueda?: string): Observable<CatalogoMiembroComisionDto[]> {
    let params = new HttpParams();
    if (busqueda && busqueda.trim()) {
      params = params.set('busqueda', busqueda.trim());
    }
    return this.http.get<CatalogoMiembroComisionDto[]>('/api/comision-autoevaluacion/catalogo', { params });
  }

  confirmarRegistroMiembroCa(procesoId: string, miembroId: string): Observable<RegistroCaDto> {
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/miembros/${miembroId}/confirmar-registro`, {});
  }

  cancelarRegistroMiembroCa(procesoId: string, miembroId: string): Observable<RegistroCaDto> {
    return this.http.post<RegistroCaDto>(`/api/convocatorias/${procesoId}/comision-autoevaluacion/miembros/${miembroId}/cancelar-registro`, {});
  }
}
