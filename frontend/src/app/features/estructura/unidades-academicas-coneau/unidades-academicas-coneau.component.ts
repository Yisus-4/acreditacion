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

  readonly mostrarInactivos = signal<boolean>(false);
  readonly selectedUnidadId = signal<string | null>(null);

  // Modal
  readonly modalAbierto = signal<boolean>(false);
  readonly modoEdicion = signal<boolean>(false);
  readonly formDenominacion = signal<string>('');
  readonly formActiva = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);

  readonly selectedUnidad = computed(() => {
    const id = this.selectedUnidadId();
    return id ? this.unidades().find((u) => u.id === id) || null : null;
  });

  readonly unidadesFiltradas = computed(() => {
    const inactivos = this.mostrarInactivos();
    return this.unidades().filter((u) => inactivos || u.activa);
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
          this.cargarUnidades();
        }
      },
      error: () => this.cargarUnidades(),
    });
  }

  onInstitucionChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.selectedInstitucionId.set(target.value);
    this.selectedUnidadId.set(null);
    this.cargarUnidades();
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
          // Si no hay seleccionada y hay datos, seleccionar la primera
          if (!this.selectedUnidadId() && data.length > 0) {
            this.selectedUnidadId.set(data[0].id);
          }
        },
        error: (err) => {
          this.errorMessage.set('Error al cargar las unidades académicas: ' + (err.error?.message || err.message));
          this.loading.set(false);
        },
      });
  }

  seleccionarUnidad(id: string): void {
    this.selectedUnidadId.set(id);
  }

  abrirModalAgregar(): void {
    this.modoEdicion.set(false);
    this.formDenominacion.set('');
    this.formActiva.set(true);
    this.modalAbierto.set(true);
  }

  abrirModalModificar(): void {
    const sel = this.selectedUnidad();
    if (!sel) return;
    this.modoEdicion.set(true);
    this.formDenominacion.set(sel.denominacion);
    this.formActiva.set(sel.activa);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardar(): void {
    const den = this.formDenominacion().trim();
    if (!den) {
      alert('La denominación es obligatoria');
      return;
    }

    this.guardando.set(true);
    if (this.modoEdicion()) {
      const id = this.selectedUnidadId()!;
      this.estructuraService
        .actualizarUnidadAcademicaConeau(id, {
          institucionId: this.selectedInstitucionId(),
          denominacion: den,
          activa: this.formActiva(),
        })
        .subscribe({
          next: () => {
            this.guardando.set(false);
            this.cerrarModal();
            this.successMessage.set('Unidad académica modificada correctamente');
            this.cargarUnidades();
          },
          error: (err) => {
            this.guardando.set(false);
            alert('Error al actualizar: ' + (err.error?.message || err.message));
          },
        });
    } else {
      this.estructuraService
        .crearUnidadAcademicaConeau({
          institucionId: this.selectedInstitucionId(),
          denominacion: den,
          activa: this.formActiva(),
        })
        .subscribe({
          next: (creada) => {
            this.guardando.set(false);
            this.cerrarModal();
            this.successMessage.set('Unidad académica registrada exitosamente');
            this.selectedUnidadId.set(creada.id);
            this.cargarUnidades();
          },
          error: (err) => {
            this.guardando.set(false);
            alert('Error al registrar: ' + (err.error?.message || err.message));
          },
        });
    }
  }

  eliminar(): void {
    const sel = this.selectedUnidad();
    if (!sel) return;

    const confirmar = confirm(
      `¿Desea eliminar la unidad académica "${sel.denominacion}"?\n\nNota: Si ya se encuentra vinculada a sedes u otros sistemas, el sistema la desactivará automáticamente para preservar la integridad histórica.`
    );
    if (!confirmar) return;

    this.estructuraService.eliminarUnidadAcademicaConeau(sel.id).subscribe({
      next: () => {
        this.successMessage.set('Unidad académica procesada');
        this.selectedUnidadId.set(null);
        this.cargarUnidades();
      },
      error: (err) => {
        alert('Error al eliminar: ' + (err.error?.message || err.message));
      },
    });
  }
}
