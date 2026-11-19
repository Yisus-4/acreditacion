import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import { ConvocatoriaResumen } from '../../../core/convocatorias/models/convocatoria.model';
import { EstructuraService, UnidadAcademicaResponse } from '../../../core/estructura/estructura.service';

@Component({
  selector: 'app-convocatoria-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './convocatoria-list.component.html',
  styleUrls: ['./convocatoria-list.component.scss'],
})
export class ConvocatoriaListComponent implements OnInit {
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly auth = inject(Auth);

  readonly convocatorias = signal<ConvocatoriaResumen[]>([]);
  readonly escuelas = signal<UnidadAcademicaResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly filtroEscuelaId = signal<string>('');
  readonly filtroEstado = signal<string>('');
  readonly filtroAnio = signal<string>('');
  readonly eliminandoId = signal<string | null>(null);

  readonly canCreate = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('AEA');
  });

  readonly canDeleteDev = computed(() => {
    return this.auth.hasAnyRole('AEA', 'AC_COORDINATOR', 'DEV', 'DEVELOPER');
  });

  ngOnInit(): void {
    this.cargarEscuelas();
    this.cargarConvocatorias();
  }

  cargarEscuelas(): void {
    this.estructuraService.listarUnidadesAcademicas().subscribe({
      next: (escuelas) => this.escuelas.set(escuelas.filter((e) => e.activo)),
      error: () => {},
    });
  }

  cargarConvocatorias(): void {
    this.loading.set(true);
    this.error.set(null);

    const filtro: any = {};
    if (this.filtroEscuelaId()) {
      filtro.unidadAcademicaId = this.filtroEscuelaId();
    }
    if (this.filtroEstado()) {
      filtro.estado = this.filtroEstado();
    }
    if (this.filtroAnio()) {
      const parsedAnio = parseInt(this.filtroAnio(), 10);
      if (!isNaN(parsedAnio)) {
        filtro.anio = parsedAnio;
      }
    }

    this.convocatoriaService.listarConvocatorias(filtro).subscribe({
      next: (data) => {
        this.convocatorias.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No fue posible cargar las convocatorias.');
        this.loading.set(false);
      },
    });
  }

  limpiarFiltros(): void {
    this.filtroEscuelaId.set('');
    this.filtroEstado.set('');
    this.filtroAnio.set('');
    this.cargarConvocatorias();
  }

  eliminarConvocatoriaDev(c: ConvocatoriaResumen, event: Event): void {
    event.stopPropagation();
    if (!this.canDeleteDev()) {
      alert('Acción restringida: solo usuarios de AEA o con rol de desarrollo pueden eliminar convocatorias.');
      return;
    }
    const ok = window.confirm(
      `¿Está seguro de que desea eliminar la convocatoria "${c.codigo}" (Modo Desarrollo)?\n\n` +
        `Esta acción eliminará la convocatoria y todos sus procesos de carrera vinculados en cascada.`
    );
    if (!ok) return;

    this.eliminandoId.set(c.id);
    this.convocatoriaService.eliminarConvocatoria(c.id).subscribe({
      next: () => {
        this.eliminandoId.set(null);
        this.cargarConvocatorias();
      },
      error: (err) => {
        this.eliminandoId.set(null);
        this.error.set(err?.error?.error || err?.error?.message || 'No fue posible eliminar la convocatoria.');
      },
    });
  }
}
