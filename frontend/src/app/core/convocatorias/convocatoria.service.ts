import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActividadPlanTrabajoDto,
  CompletarActividadPayload,
  ConvocatoriaDetalle,
  ConvocatoriaFiltro,
  ConvocatoriaResumen,
  DocumentoInteresDto,
  ElaborarPlanTrabajoRequest,
  FichaFormalizacionFt012Dto,
  IniciarConvocatoriaRequest,
  PlanTrabajoDto,
  PlantillaPlanTrabajoDto,
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

  obtenerPlanTrabajo(procesoId: string): Observable<PlanTrabajoDto> {
    return this.http.get<PlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo`);
  }

  getPlantillaPlanTrabajo(procesoId: string): Observable<PlantillaPlanTrabajoDto> {
    return this.http.get<PlantillaPlanTrabajoDto>(`/api/convocatorias/${procesoId}/plan-trabajo/plantilla`);
  }

  obtenerPlantillaPlan(procesoId: string): Observable<PlantillaPlanTrabajoDto> {
    return this.getPlantillaPlanTrabajo(procesoId);
  }

  elaborarPlanTrabajo(procesoId: string, request: ElaborarPlanTrabajoRequest): Observable<ConvocatoriaDetalle> {
    const payload = {
      ...request,
      fechaLimiteConeau: request.fechaLimiteConeau ?? request.fechaLimitePresentacionConeau,
      fechaLimitePresentacionConeau: request.fechaLimitePresentacionConeau ?? request.fechaLimiteConeau,
    };
    return this.http.post<ConvocatoriaDetalle>(`/api/convocatorias/${procesoId}/plan-trabajo/elaborar`, payload);
  }

  elaborarPlan(procesoId: string, request: ElaborarPlanTrabajoRequest): Observable<ConvocatoriaDetalle> {
    return this.elaborarPlanTrabajo(procesoId, request);
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

  descargarDocumento(documentoId: string): Observable<Blob> {
    return this.http.get(`/api/documentos/${documentoId}/descargar`, {
      responseType: 'blob',
    });
  }

  eliminarConvocatoria(id: string): Observable<void> {
    return this.http.delete<void>(`/api/convocatorias/${id}`);
  }

  // Actividad 4: Identificar información y documentación de interés
  listarDocumentosActividad(procesoId: string, actividadId: string): Observable<DocumentoInteresDto[]> {
    return this.http.get<DocumentoInteresDto[]>(
      `/api/convocatorias/${procesoId}/actividades/${actividadId}/documentos`
    );
  }

  subirDocumentoInteres(
    procesoId: string,
    actividadId: string,
    archivo: File,
    clasificacionOrigen: string,
    descripcion?: string
  ): Observable<DocumentoInteresDto> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('clasificacionOrigen', clasificacionOrigen);
    if (descripcion && descripcion.trim()) {
      formData.append('descripcion', descripcion.trim());
    }
    return this.http.post<DocumentoInteresDto>(
      `/api/convocatorias/${procesoId}/actividades/${actividadId}/documentos`,
      formData
    );
  }

  eliminarDocumentoInteres(procesoId: string, actividadId: string, documentoId: string): Observable<void> {
    return this.http.delete<void>(
      `/api/convocatorias/${procesoId}/actividades/${actividadId}/documentos/${documentoId}`
    );
  }

  descargarDocumentoInteres(procesoId: string, actividadId: string, documentoId: string): Observable<Blob> {
    return this.http.get(
      `/api/convocatorias/${procesoId}/actividades/${actividadId}/documentos/${documentoId}/descargar`,
      { responseType: 'blob' }
    );
  }

  completarDocumentacionInteres(
    procesoId: string,
    actividadId: string,
    payload: { fuentesSeleccionadas: string[]; observaciones?: string }
  ): Observable<ActividadPlanTrabajoDto> {
    return this.http.post<ActividadPlanTrabajoDto>(
      `/api/convocatorias/${procesoId}/actividades/${actividadId}/completar-documentacion-interes`,
      payload
    );
  }

  // Actividad 5: Formalización electrónica de participación en convocatoria (FT-012 + RG-001)
  obtenerFormalizacion(procesoId: string): Observable<FichaFormalizacionFt012Dto> {
    return this.http.get<FichaFormalizacionFt012Dto>(`/api/convocatorias/${procesoId}/formalizacion`);
  }

  solicitarFormalizacion(procesoId: string): Observable<FichaFormalizacionFt012Dto> {
    return this.http.post<FichaFormalizacionFt012Dto>(
      `/api/convocatorias/${procesoId}/formalizacion/solicitar`,
      {}
    );
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

  formalizarEnConeau(
    procesoId: string,
    payload: { archivo: File; fechaFormalizacionElectronica: string; observaciones?: string }
  ): Observable<FichaFormalizacionFt012Dto> {
    const formData = new FormData();
    formData.append('archivo', payload.archivo);
    formData.append('fechaFormalizacionElectronica', payload.fechaFormalizacionElectronica);
    if (payload.observaciones && payload.observaciones.trim()) {
      formData.append('observaciones', payload.observaciones.trim());
    }
    return this.http.post<FichaFormalizacionFt012Dto>(
      `/api/convocatorias/${procesoId}/formalizacion/formalizar`,
      formData
    );
  }

  descargarComprobanteFormalizacion(procesoId: string): Observable<Blob> {
    return this.http.get(`/api/convocatorias/${procesoId}/formalizacion/comprobante`, {
      responseType: 'blob',
    });
  }
}

