import { inject, Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  DocumentoNormativaDto,
  NormativaDetalleDto,
  RevisarNormativaRequest,
  SolicitarNormativaRequest,
  TipoDocumentoNormativa,
  CarpetaMinioDestino,
} from '../models/normativa.model';

export * from '../models/normativa.model';

@Injectable({
  providedIn: 'root',
})
export class NormativaService {
  private readonly http = inject(HttpClient);

  readonly detalle = signal<NormativaDetalleDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly actionLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly porcentajeAvance = computed(() => {
    return this.detalle()?.porcentajeAvance ?? 0;
  });

  readonly tieneDocumentosObservadosPendientes = computed(() =>
    (this.detalle()?.documentos ?? []).some((d) => d.observado)
  );

  readonly puedeEnviar = computed(() => {
    const d = this.detalle();
    if (!d) return false;
    const estadosPermitidos = ['SOLICITADA', 'EN_CARGA_BORRADOR', 'OBSERVADA'];
    return (
      estadosPermitidos.includes(d.estado) &&
      d.porcentajeAvance >= 100 &&
      (!d.documentosFaltantes || d.documentosFaltantes.length === 0) &&
      !this.tieneDocumentosObservadosPendientes()
    );
  });

  readonly esCompletada = computed(() => {
    return this.detalle()?.estado === 'COMPLETADA';
  });

  readonly esEnviadaRevision = computed(() => {
    return this.detalle()?.estado === 'ENVIADA_A_REVISION';
  });

  readonly esObservada = computed(() => {
    return this.detalle()?.estado === 'OBSERVADA';
  });

  readonly documentosPorCarpeta = computed(() => {
    const docs = this.detalle()?.documentos ?? [];
    return {
      normativa: docs.filter((d) => d.carpetaDestino === 'NORMATIVA'),
      actas: docs.filter((d) => d.carpetaDestino === 'NORMATIVA_ACTAS'),
      evaluaciones: docs.filter((d) => d.carpetaDestino === 'EVALUACIONES_ANTERIORES'),
    };
  });

  obtenerDetalle(procesoId: string): Observable<NormativaDetalleDto> {
    this.loading.set(true);
    this.error.set(null);
    return this.http
      .get<NormativaDetalleDto>(`/api/v1/procesos/${procesoId}/normativa`)
      .pipe(
        tap({
          next: (res) => {
            this.detalle.set(res);
            this.loading.set(false);
          },
          error: (err) => {
            this.error.set(err?.error?.error || 'No se pudo obtener el detalle de normativa.');
            this.loading.set(false);
          },
        })
      );
  }

  solicitarNormativa(
    procesoId: string,
    request?: SolicitarNormativaRequest
  ): Observable<NormativaDetalleDto> {
    this.actionLoading.set(true);
    return this.http
      .post<NormativaDetalleDto>(`/api/v1/procesos/${procesoId}/normativa`, request || {})
      .pipe(
        tap({
          next: (res) => {
            this.detalle.set(res);
            this.actionLoading.set(false);
          },
          error: (err) => {
            this.actionLoading.set(false);
            throw err;
          },
        })
      );
  }

  cargarBorrador(
    procesoId: string,
    tipoDocumento: TipoDocumentoNormativa,
    archivo: File
  ): Observable<DocumentoNormativaDto> {
    this.actionLoading.set(true);
    const formData = new FormData();
    formData.append('tipoDocumento', tipoDocumento);
    formData.append('archivo', archivo);

    return this.http
      .post<DocumentoNormativaDto>(
        `/api/v1/procesos/${procesoId}/normativa/documentos`,
        formData
      )
      .pipe(
        tap({
          next: () => {
            // Actualizar el detalle completo para sincronizar matriz y avance
            this.obtenerDetalle(procesoId).subscribe();
            this.actionLoading.set(false);
          },
          error: (err) => {
            this.actionLoading.set(false);
            throw err;
          },
        })
      );
  }

  eliminarBorrador(procesoId: string, documentoId: string): Observable<void> {
    this.actionLoading.set(true);
    return this.http
      .delete<void>(`/api/v1/procesos/${procesoId}/normativa/documentos/${documentoId}`)
      .pipe(
        tap({
          next: () => {
            this.obtenerDetalle(procesoId).subscribe();
            this.actionLoading.set(false);
          },
          error: (err) => {
            this.actionLoading.set(false);
            throw err;
          },
        })
      );
  }

  enviarDefinitiva(procesoId: string): Observable<NormativaDetalleDto> {
    this.actionLoading.set(true);
    return this.http
      .post<NormativaDetalleDto>(`/api/v1/procesos/${procesoId}/normativa/enviar`, {})
      .pipe(
        tap({
          next: (res) => {
            this.detalle.set(res);
            this.actionLoading.set(false);
          },
          error: (err) => {
            this.actionLoading.set(false);
            throw err;
          },
        })
      );
  }

  revisarNormativa(
    procesoId: string,
    request: RevisarNormativaRequest
  ): Observable<NormativaDetalleDto> {
    this.actionLoading.set(true);
    return this.http
      .post<NormativaDetalleDto>(`/api/v1/procesos/${procesoId}/normativa/revisar`, request)
      .pipe(
        tap({
          next: (res) => {
            this.detalle.set(res);
            this.actionLoading.set(false);
          },
          error: (err) => {
            this.actionLoading.set(false);
            throw err;
          },
        })
      );
  }

  exportarHojaInfoExcel(planTrabajoId: string): Observable<Blob> {
    return this.http.get(`/api/v1/planes-trabajo/${planTrabajoId}/hoja-info`, {
      responseType: 'blob',
    });
  }

  descargarDocumento(procesoId: string, documentoId: string, nombreArchivo: string): void {
    const url = `/api/v1/procesos/${procesoId}/normativa/documentos/${documentoId}/descargar`;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = objectUrl;
        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(objectUrl);
      },
      error: () => {
        alert('No se pudo descargar el archivo.');
      },
    });
  }
}
