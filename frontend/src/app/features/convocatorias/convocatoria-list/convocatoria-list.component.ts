import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import { ConvocatoriaResumen, EstadoProceso } from '../../../core/convocatorias/models/convocatoria.model';
import { EstructuraService, CarreraResponse } from '../../../core/estructura/estructura.service';

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
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly filtroCarreraId = signal<string>('');
  readonly filtroEstado = signal<string>('');
  readonly filtroAnio = signal<string>('');

  readonly canCreate = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('ADMINISTRATOR') || session.user.roleCodes.includes('AEA');
  });

  ngOnInit(): void {
    this.cargarCarreras();
    this.cargarConvocatorias();
  }

  cargarCarreras(): void {
    this.estructuraService.listarCarreras().subscribe({
      next: (carreras) => this.carreras.set(carreras.filter((c) => c.activo)),
      error: () => {},
    });
  }

  cargarConvocatorias(): void {
    this.loading.set(true);
    this.error.set(null);

    const filtro: any = {};
    if (this.filtroCarreraId()) {
      filtro.carreraId = this.filtroCarreraId();
    }
    if (this.filtroEstado()) {
      filtro.estado = this.filtroEstado() as EstadoProceso;
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
    this.filtroCarreraId.set('');
    this.filtroEstado.set('');
    this.filtroAnio.set('');
    this.cargarConvocatorias();
  }
}
