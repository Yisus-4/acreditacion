import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubnavGestionInstitucionalComponent } from '../subnav-gestion-institucional/subnav-gestion-institucional.component';
import { EstructuraService, UnidadAcademicaConeauResponse } from '../../../core/estructura/estructura.service';
import { InstitucionService } from '../../../core/instituciones/institucion.service';
import { InstitucionResponse } from '../../../core/instituciones/institucion.types';

@Component({
  selector: 'app-unidades-academicas-coneau',
  standalone: true,
  imports: [CommonModule, FormsModule, SubnavGestionInstitucionalComponent],
  templateUrl: './unidades-academicas-coneau.component.html',
  styleUrls: ['./unidades-academicas-coneau.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnidadesAcademicasConeauComponent implements OnInit {
  private readonly estructuraService = inject(EstructuraService);
  private readonly institucionService = inject(InstitucionService);

  readonly instituciones = signal<InstitucionResponse[]>([]);
  readonly selectedInstitucionId = signal<string>('');
  readonly unidades = signal<UnidadAcademicaConeauResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Filtros de búsqueda
  readonly searchQuery = signal<string>('');
  readonly mostrarInactivas = signal<boolean>(true);

  // Drawer de Alta/Edición
  readonly drawerAbierto = signal<boolean>(false);
  readonly editingId = signal<string | null>(null);
  readonly formInstitucionId = signal<string>('');
  readonly formDenominacion = signal<string>('');
  readonly formActiva = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);

  readonly filteredUnidades = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const incluirInactivas = this.mostrarInactivas();

    return this.unidades().filter((ua) => {
      if (!incluirInactivas && !ua.activa) {
        return false;
      }
      if (!query) {
        return true;
      }
      const matchDenom = ua.denominacion?.toLowerCase().includes(query);
      const matchInst = ua.institucionNombre?.toLowerCase().includes(query);
      return matchDenom || matchInst;
    });
  });

  readonly institucionActualNombre = computed(() => {
    const id = this.selectedInstitucionId();
    const inst = this.instituciones().find((i) => i.id === id);
    return inst ? `${inst.nombre} (${inst.sigla})` : 'Universidad Nacional de Chilecito';
  });

  ngOnInit(): void {
    this.cargarInstituciones();
  }

  cargarInstituciones(): void {
    this.institucionService.listarInstituciones().subscribe({
      next: (insts: InstitucionResponse[]) => {
        this.instituciones.set(insts);
        if (insts.length > 0 && !this.selectedInstitucionId()) {
          this.selectedInstitucionId.set(insts[0].id);
        }
        this.cargarUnidades();
      },
      error: () => this.cargarUnidades(),
    });
  }

  onInstitucionFilterChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.selectedInstitucionId.set(target.value);
    this.cargarUnidades();
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery.set(target.value);
  }

  cargarUnidades(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.estructuraService
      .listarUnidadesAcademicasConeau(this.selectedInstitucionId() || undefined, true)
      .subscribe({
        next: (data) => {
          this.unidades.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set('Error al cargar las unidades académicas: ' + (err.error?.message || err.message));
          this.loading.set(false);
        },
      });
  }

  abrirDrawerNuevo(): void {
    this.editingId.set(null);
    this.formInstitucionId.set(this.selectedInstitucionId() || (this.instituciones()[0]?.id ?? ''));
    this.formDenominacion.set('');
    this.formActiva.set(true);
    this.drawerAbierto.set(true);
  }

  iniciarEdicion(ua: UnidadAcademicaConeauResponse): void {
    this.editingId.set(ua.id);
    this.formInstitucionId.set(ua.institucionId || this.selectedInstitucionId());
    this.formDenominacion.set(ua.denominacion);
    this.formActiva.set(ua.activa);
    this.drawerAbierto.set(true);
  }

  cerrarDrawer(): void {
    this.drawerAbierto.set(false);
  }

  guardar(): void {
    const denom = this.formDenominacion().trim();
    if (!denom) {
      this.errorMessage.set('La denominación de la unidad académica es obligatoria.');
      return;
    }

    this.guardando.set(true);
    this.errorMessage.set(null);

    const payload = {
      institucionId: this.formInstitucionId() || this.selectedInstitucionId(),
      denominacion: denom,
      activa: this.formActiva(),
    };

    if (this.editingId()) {
      this.estructuraService
        .actualizarUnidadAcademicaConeau(this.editingId()!, payload)
        .subscribe({
          next: () => {
            this.guardando.set(false);
            this.cerrarDrawer();
            this.successMessage.set('Unidad académica actualizada exitosamente.');
            this.cargarUnidades();
          },
          error: (err) => {
            this.guardando.set(false);
            this.errorMessage.set('Error al actualizar unidad académica: ' + (err.error?.message || err.message));
          },
        });
    } else {
      this.estructuraService
        .crearUnidadAcademicaConeau(payload)
        .subscribe({
          next: () => {
            this.guardando.set(false);
            this.cerrarDrawer();
            this.successMessage.set('Unidad académica registrada exitosamente.');
            this.cargarUnidades();
          },
          error: (err) => {
            this.guardando.set(false);
            this.errorMessage.set('Error al registrar unidad académica: ' + (err.error?.message || err.message));
          },
        });
    }
  }

  toggleActiva(ua: UnidadAcademicaConeauResponse): void {
    const nuevoEstado = !ua.activa;
    const accion = nuevoEstado ? 'activar' : 'desactivar';

    if (!confirm(`¿Está seguro de que desea ${accion} la unidad académica "${ua.denominacion}"?`)) {
      return;
    }

    this.estructuraService
      .actualizarUnidadAcademicaConeau(ua.id, {
        institucionId: ua.institucionId,
        denominacion: ua.denominacion,
        activa: nuevoEstado,
      })
      .subscribe({
        next: () => {
          this.successMessage.set(`Unidad académica "${ua.denominacion}" ${nuevoEstado ? 'activada' : 'desactivada'} correctamente.`);
          this.cargarUnidades();
        },
        error: (err) => {
          this.errorMessage.set(`Error al cambiar el estado: ` + (err.error?.message || err.message));
        },
      });
  }

  eliminar(ua: UnidadAcademicaConeauResponse): void {
    const confirmar = confirm(
      `¿Desea eliminar la unidad académica "${ua.denominacion}"?\n\nSi tiene sedes vinculadas o referencias activas, el sistema la desactivará para mantener la integridad histórica.`
    );
    if (!confirmar) return;

    this.estructuraService.eliminarUnidadAcademicaConeau(ua.id).subscribe({
      next: () => {
        this.successMessage.set(`Unidad académica "${ua.denominacion}" eliminada o desactivada.`);
        this.cargarUnidades();
      },
      error: (err) => {
        this.errorMessage.set('Error al eliminar: ' + (err.error?.message || err.message));
      },
    });
  }
}
