import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EstructuraService, UnidadAcademicaResponse } from '../../../core/estructura/estructura.service';
import { InstitucionService } from '../../../core/instituciones/institucion.service';
import { InstitucionResponse } from '../../../core/instituciones/institucion.types';

@Component({
  selector: 'app-unidades-academicas',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './unidades-academicas.component.html',
  styleUrl: './unidades-academicas.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnidadesAcademicasComponent implements OnInit {
  private readonly estructuraService = inject(EstructuraService);
  private readonly institucionService = inject(InstitucionService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly unidades = signal<UnidadAcademicaResponse[]>([]);
  readonly instituciones = signal<InstitucionResponse[]>([]);
  readonly loading = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly showForm = signal<boolean>(false);
  readonly editingId = signal<string | null>(null);
  readonly searchQuery = signal<string>('');

  readonly form = this.fb.group({
    institucionId: ['', [Validators.required]],
    nombre: ['', [Validators.required, Validators.maxLength(255)]],
    sigla: ['', [Validators.maxLength(50)]],
    descripcion: [''],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.institucionService
      .listarInstituciones()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (insts) => {
          this.instituciones.set(insts);
          if (insts.length > 0 && !this.form.value.institucionId) {
            this.form.patchValue({ institucionId: insts[0].id });
          }
        },
        error: (err) => console.error('Error al listar instituciones:', err),
      });

    this.estructuraService
      .listarUnidadesAcademicas()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (items) => this.unidades.set(items),
        error: (err) => {
          console.error('Error al listar unidades académicas:', err);
          this.errorMessage.set('No se pudo cargar el listado de unidades académicas.');
        },
      });
  }

  toggleForm(): void {
    if (this.showForm()) {
      this.cancelarFormulario();
    } else {
      this.editingId.set(null);
      this.form.reset({
        institucionId: this.instituciones()[0]?.id || '',
        nombre: '',
        sigla: '',
        descripcion: '',
      });
      this.showForm.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);
    }
  }

  iniciarEdicion(ua: UnidadAcademicaResponse): void {
    this.editingId.set(ua.id);
    this.showForm.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.form.patchValue({
      institucionId: ua.institucionId,
      nombre: ua.nombre,
      sigla: ua.sigla || '',
      descripcion: ua.descripcion || '',
    });
  }

  cancelarFormulario(): void {
    this.editingId.set(null);
    this.showForm.set(false);
    this.form.reset({
      institucionId: this.instituciones()[0]?.id || '',
      nombre: '',
      sigla: '',
      descripcion: '',
    });
    this.errorMessage.set(null);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const val = this.form.getRawValue();
    const editId = this.editingId();

    if (editId) {
      this.estructuraService
        .actualizarUnidadAcademica(editId, {
          nombre: val.nombre!.trim(),
          sigla: val.sigla?.trim() || undefined,
          descripcion: val.descripcion?.trim() || undefined,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (actualizada) => {
            this.unidades.update((list) =>
              list.map((item) => (item.id === actualizada.id ? actualizada : item))
            );
            this.successMessage.set(`Unidad académica "${actualizada.nombre}" actualizada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al actualizar unidad académica:', err);
            const msg = err.error?.error || 'No se pudo actualizar la unidad académica. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    } else {
      this.estructuraService
        .crearUnidadAcademica({
          institucionId: val.institucionId!,
          nombre: val.nombre!.trim(),
          sigla: val.sigla?.trim() || undefined,
          descripcion: val.descripcion?.trim() || undefined,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (nueva) => {
            this.unidades.update((list) => [...list, nueva]);
            this.successMessage.set(`Unidad académica "${nueva.nombre}" registrada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al registrar unidad académica:', err);
            const msg = err.error?.error || 'No se pudo registrar la unidad académica. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    }
  }

  cambiarEstado(ua: UnidadAcademicaResponse): void {
    const nuevoEstado = !ua.activo;
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .cambiarEstadoUnidadAcademica(ua.id, nuevoEstado)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (actualizada) => {
          this.unidades.update((list) =>
            list.map((item) => (item.id === actualizada.id ? actualizada : item))
          );
          this.successMessage.set(
            `Estado de "${actualizada.nombre}" actualizado a ${actualizada.activo ? 'Activa' : 'Inactiva'}.`
          );
        },
        error: (err) => {
          console.error('Error al cambiar estado:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo cambiar el estado de la unidad académica.');
        },
      });
  }

  eliminar(ua: UnidadAcademicaResponse): void {
    if (!confirm(`¿Está seguro de eliminar la unidad académica "${ua.nombre}"? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .eliminarUnidadAcademica(ua.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.unidades.update((list) => list.filter((item) => item.id !== ua.id));
          this.successMessage.set(`Unidad académica "${ua.nombre}" eliminada exitosamente.`);
          if (this.editingId() === ua.id) {
            this.cancelarFormulario();
          }
        },
        error: (err) => {
          console.error('Error al eliminar unidad académica:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo eliminar la unidad académica.');
        },
      });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  filteredUnidades(): UnidadAcademicaResponse[] {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) {
      return this.unidades();
    }
    return this.unidades().filter((u) => {
      const nom = u.nombre.toLowerCase();
      const sig = (u.sigla || '').toLowerCase();
      const desc = (u.descripcion || '').toLowerCase();
      return nom.includes(q) || sig.includes(q) || desc.includes(q);
    });
  }

  isFieldInvalid(control: FormControl | null): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }
}
