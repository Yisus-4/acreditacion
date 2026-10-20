import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { NormativaService } from '../../../core/normativa/services/normativa.service';
import {
  RevisarNormativaRequest,
  SolicitarNormativaRequest,
  TipoDocumentoNormativa,
  formatearEstadoNormativa,
} from '../../../core/normativa/models/normativa.model';
import { BandejaCargaNormativaComponent } from '../bandeja-carga-normativa/bandeja-carga-normativa.component';
import { HojaInfoTabularComponent } from '../hoja-info-tabular/hoja-info-tabular.component';
import { ModalSolicitarNormativaComponent } from '../modal-solicitar-normativa/modal-solicitar-normativa.component';
import { ModalRevisarNormativaComponent } from '../modal-revisar-normativa/modal-revisar-normativa.component';

@Component({
  selector: 'app-normativa-detalle',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    BandejaCargaNormativaComponent,
    HojaInfoTabularComponent,
    ModalSolicitarNormativaComponent,
    ModalRevisarNormativaComponent,
  ],
  templateUrl: './normativa-detalle.component.html',
  styleUrls: ['./normativa-detalle.component.scss'],
})
export class NormativaDetalleComponent implements OnInit {
  private readonly normativaService = inject(NormativaService);
  private readonly auth = inject(Auth);
  private readonly route = inject(ActivatedRoute);

  @Input() procesoId = '';
  @Input() planTrabajoId = '';
  @Input() procesoCodigo = '';
  @Input() carreraNombre = '';
  @Input() embedded = false;

  readonly vistaActiva = signal<'BANDEJA' | 'HOJA_INFO'>('BANDEJA');
  readonly modalSolicitarAbierto = signal<boolean>(false);
  readonly modalRevisarAbierto = signal<boolean>(false);
  readonly alertMensaje = signal<{ tipo: 'success' | 'error'; texto: string } | null>(null);

  readonly detalle = this.normativaService.detalle;
  readonly loading = this.normativaService.loading;
  readonly actionLoading = this.normativaService.actionLoading;
  readonly porcentajeAvance = this.normativaService.porcentajeAvance;
  readonly puedeEnviar = this.normativaService.puedeEnviar;
  readonly esCompletada = this.normativaService.esCompletada;
  readonly esEnviadaRevision = this.normativaService.esEnviadaRevision;
  readonly esObservada = this.normativaService.esObservada;

  readonly esAeaOAdmin = computed(() => {
    const roles = this.auth.currentSession?.user?.roleCodes ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('AEA');
  });

  readonly puedeEditarBorradores = computed(() => {
    const d = this.detalle();
    if (!d) return false;
    const estadosEditables = ['SOLICITADA', 'EN_CARGA_BORRADOR', 'OBSERVADA'];
    return estadosEditables.includes(d.estado);
  });

  formatearEstado(estado?: string | null): string {
    return formatearEstadoNormativa(estado);
  }

  ngOnInit(): void {
    if (!this.procesoId) {
      this.procesoId = this.route.snapshot.paramMap.get('id') || '';
    }
    if (this.procesoId) {
      this.cargarDatos();
    }
  }

  cargarDatos(): void {
    this.normativaService.obtenerDetalle(this.procesoId).subscribe({
      error: (err) => {
        // Si no existe solicitud aún (404), queda en estado PENDIENTE_SOLICITUD
        if (err?.status !== 404) {
          this.mostrarAlerta('error', err?.error?.error || 'No se pudo cargar la información de normativa.');
        }
      },
    });
  }

  cambiarVista(vista: 'BANDEJA' | 'HOJA_INFO'): void {
    this.vistaActiva.set(vista);
  }

  abrirModalSolicitar(): void {
    this.modalSolicitarAbierto.set(true);
  }

  cerrarModalSolicitar(): void {
    this.modalSolicitarAbierto.set(false);
  }

  confirmarSolicitud(request: SolicitarNormativaRequest): void {
    this.normativaService.solicitarNormativa(this.procesoId, request).subscribe({
      next: () => {
        this.cerrarModalSolicitar();
        this.mostrarAlerta('success', 'Solicitud formal de normativa iniciada con éxito.');
      },
      error: (err) => {
        this.mostrarAlerta('error', err?.error?.error || 'Error al iniciar la solicitud de normativa.');
      },
    });
  }

  onSubirDocumento(event: { tipo: TipoDocumentoNormativa; archivo: File }): void {
    this.normativaService.cargarBorrador(this.procesoId, event.tipo, event.archivo).subscribe({
      next: () => {
        this.mostrarAlerta('success', `Archivo '${event.archivo.name}' cargado exitosamente.`);
      },
      error: (err) => {
        this.mostrarAlerta('error', err?.error?.error || 'Error al subir el archivo.');
      },
    });
  }

  onEliminarDocumento(documentoId: string): void {
    this.normativaService.eliminarBorrador(this.procesoId, documentoId).subscribe({
      next: () => {
        this.mostrarAlerta('success', 'Documento borrador eliminado con éxito.');
      },
      error: (err) => {
        this.mostrarAlerta('error', err?.error?.error || 'Error al eliminar el documento.');
      },
    });
  }

  onDescargarDocumento(event: { documentoId: string; nombreArchivo: string }): void {
    this.normativaService.descargarDocumento(this.procesoId, event.documentoId, event.nombreArchivo);
  }

  onEnviarDefinitiva(): void {
    if (!confirm('¿Confirma el envío definitivo de la documentación a revisión por el Área de Evaluación y Acreditación (AEA)? Una vez enviada, no podrá modificar los borradores.')) {
      return;
    }

    this.normativaService.enviarDefinitiva(this.procesoId).subscribe({
      next: () => {
        this.mostrarAlerta('success', 'Normativa enviada formalmente a revisión por AEA.');
      },
      error: (err) => {
        this.mostrarAlerta('error', err?.error?.error || 'Error al enviar la normativa definitiva.');
      },
    });
  }

  abrirModalRevisar(): void {
    this.modalRevisarAbierto.set(true);
  }

  cerrarModalRevisar(): void {
    this.modalRevisarAbierto.set(false);
  }

  confirmarRevision(request: RevisarNormativaRequest): void {
    this.normativaService.revisarNormativa(this.procesoId, request).subscribe({
      next: () => {
        this.cerrarModalRevisar();
        const msg =
          request.decision === 'APROBADO'
            ? '¡Normativa aprobada exitosamente! La Actividad 2 ha sido completada.'
            : 'Observaciones emitidas y notificadas a la Dirección de Carrera.';
        this.mostrarAlerta('success', msg);
      },
      error: (err) => {
        this.mostrarAlerta('error', err?.error?.error || 'Error al dictaminar sobre la normativa.');
      },
    });
  }

  onExportarExcel(planId: string): void {
    this.normativaService.exportarHojaInfoExcel(planId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Hoja_INFO_PlanTrabajo_${planId}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.mostrarAlerta('error', 'Error al generar la Hoja INFO en formato Excel.');
      },
    });
  }

  private mostrarAlerta(tipo: 'success' | 'error', texto: string): void {
    this.alertMensaje.set({ tipo, texto });
    setTimeout(() => {
      this.alertMensaje.set(null);
    }, 6000);
  }
}
