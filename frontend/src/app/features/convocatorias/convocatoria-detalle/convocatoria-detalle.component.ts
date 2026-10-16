import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ActividadPlanTrabajoDto,
  ConvocatoriaDetalle,
  PlanTrabajoDto,
} from '../../../core/convocatorias/models/convocatoria.model';
import { CompletarActividadModalComponent } from '../completar-actividad-modal/completar-actividad-modal.component';

@Component({
  selector: 'app-convocatoria-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink, CompletarActividadModalComponent],
  templateUrl: './convocatoria-detalle.component.html',
  styleUrls: ['./convocatoria-detalle.component.scss'],
})
export class ConvocatoriaDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly auth = inject(Auth);

  readonly convocatoria = signal<ConvocatoriaDetalle | null>(null);
  readonly planTrabajo = signal<PlanTrabajoDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly actividadSeleccionada = signal<ActividadPlanTrabajoDto | null>(null);

  readonly canManage = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('ADMINISTRATOR') || session.user.roleCodes.includes('AEA');
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargarDatos(id);
    }
  }

  cargarDatos(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.convocatoriaService.obtenerConvocatoria(id).subscribe({
      next: (conv) => {
        this.convocatoria.set(conv);
        this.convocatoriaService.obtenerPlanTrabajo(id).subscribe({
          next: (plan) => {
            this.planTrabajo.set(plan);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No fue posible cargar el detalle de la convocatoria.');
        this.loading.set(false);
      },
    });
  }

  abrirModalCompletar(act: ActividadPlanTrabajoDto): void {
    this.actividadSeleccionada.set(act);
  }

  cerrarModal(completada: boolean): void {
    this.actividadSeleccionada.set(null);
    if (completada) {
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.cargarDatos(id);
      }
    }
  }

  descargar(documentoId: string, nombreArchivo: string): void {
    this.convocatoriaService.descargarDocumento(documentoId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('No fue posible descargar el documento probatorio.');
      },
    });
  }
}
