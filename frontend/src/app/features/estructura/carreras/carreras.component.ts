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
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
  CarreraResponse,
  EstructuraService,
  ModalidadCarrera,
  EstadoCarrera,
  SedeLocalizacionResponse,
  UnidadAcademicaResponse,
} from '../../../core/estructura/estructura.service';

@Component({
  selector: 'app-carreras',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './carreras.component.html',
  styleUrl: './carreras.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CarrerasComponent implements OnInit {
  private readonly estructuraService = inject(EstructuraService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly unidades = signal<UnidadAcademicaResponse[]>([]);
  readonly sedes = signal<SedeLocalizacionResponse[]>([]);

  readonly loading = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly showForm = signal<boolean>(false);
  readonly editingId = signal<string | null>(null);
  readonly searchQuery = signal<string>('');
  readonly selectedUaFilter = signal<string>('');

  // Nivel es estrictamente "GRADO"
  readonly nivelFijo = 'GRADO';

  readonly form = this.fb.group({
    unidadAcademicaId: ['', [Validators.required]],
    nombre: ['', [Validators.required, Validators.maxLength(255)]],
    sigla: ['', [Validators.maxLength(50)]],
    areaDisciplinaria: ['', [Validators.required, Validators.maxLength(150)]],
    modalidad: ['PRESENCIAL' as ModalidadCarrera, [Validators.required]],
    estado: ['EN_FUNCIONAMIENTO' as EstadoCarrera, [Validators.required]],
    tituloOtorgado: ['', [Validators.maxLength(255)]],
    duracionAnios: [5 as number | null, [Validators.min(1), Validators.max(10)]],
    descripcion: [''],
    activo: [true],
    sedesSeleccionadas: this.fb.array<string>([]),
  });

  ngOnInit(): void {
    // Si viene queryParam de unidadAcademicaId, pre-filtrar
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const uaId = params.get('unidadAcademicaId');
      if (uaId) {
        this.selectedUaFilter.set(uaId);
        this.form.patchValue({ unidadAcademicaId: uaId });
      }
    });

    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.estructuraService
      .listarUnidadesAcademicas()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (uas) => {
          this.unidades.set(uas);
          if (uas.length > 0 && !this.form.value.unidadAcademicaId) {
            this.form.patchValue({ unidadAcademicaId: uas[0].id });
          }
        },
        error: (err) => console.error('Error al listar unidades académicas:', err),
      });

    this.estructuraService
      .listarSedes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sedesList) => {
          this.sedes.set(sedesList);
          if (sedesList.length > 0 && this.sedesSeleccionadasArray.length === 0 && !this.editingId()) {
            this.toggleSedeSeleccionada(sedesList[0].id, true);
          }
        },
        error: (err) => console.error('Error al listar sedes:', err),
      });

    this.estructuraService
      .listarCarreras()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (items) => this.carreras.set(items),
        error: (err) => {
          console.error('Error al listar carreras:', err);
          this.errorMessage.set('No se pudo cargar el listado de carreras de grado.');
        },
      });
  }

  get sedesSeleccionadasArray(): FormArray {
    return this.form.get('sedesSeleccionadas') as FormArray;
  }

  isSedeSelected(sedeId: string): boolean {
    return this.sedesSeleccionadasArray.value.includes(sedeId);
  }

  onSedeCheckboxChange(event: Event, sedeId: string): void {
    const input = event.target as HTMLInputElement;
    this.toggleSedeSeleccionada(sedeId, input.checked);
  }

  private toggleSedeSeleccionada(sedeId: string, checked: boolean): void {
    const arr = this.sedesSeleccionadasArray;
    if (checked) {
      if (!arr.value.includes(sedeId)) {
        arr.push(new FormControl(sedeId));
      }
    } else {
      const idx = arr.value.indexOf(sedeId);
      if (idx !== -1) {
        arr.removeAt(idx);
      }
    }
  }

  toggleForm(): void {
    if (this.showForm()) {
      this.cancelarFormulario();
    } else {
      this.editingId.set(null);
      this.resetFormToDefault();
      this.showForm.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);
    }
  }

  iniciarEdicion(carrera: CarreraResponse): void {
    this.editingId.set(carrera.id);
    this.showForm.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // Populate sedes array
    const arr = this.sedesSeleccionadasArray;
    while (arr.length > 0) {
      arr.removeAt(0);
    }
    if (carrera.sedesIds) {
      carrera.sedesIds.forEach((sid) => arr.push(new FormControl(sid)));
    }

    this.form.patchValue({
      unidadAcademicaId: carrera.unidadAcademicaId,
      nombre: carrera.nombre,
      sigla: carrera.sigla || '',
      areaDisciplinaria: carrera.areaDisciplinaria,
      modalidad: carrera.modalidad,
      estado: carrera.estado,
      tituloOtorgado: carrera.tituloOtorgado || '',
      duracionAnios: carrera.duracionAnios || 5,
      descripcion: carrera.descripcion || '',
      activo: carrera.activo,
    });
  }

  cancelarFormulario(): void {
    this.editingId.set(null);
    this.showForm.set(false);
    this.resetFormToDefault();
    this.errorMessage.set(null);
  }

  private resetFormToDefault(): void {
    const arr = this.sedesSeleccionadasArray;
    while (arr.length > 0) {
      arr.removeAt(0);
    }
    this.form.reset({
      unidadAcademicaId: this.unidades()[0]?.id || '',
      nombre: '',
      sigla: '',
      areaDisciplinaria: '',
      modalidad: 'PRESENCIAL',
      estado: 'EN_FUNCIONAMIENTO',
      tituloOtorgado: '',
      duracionAnios: 5,
      descripcion: '',
      activo: true,
      sedesSeleccionadas: [],
    });
    if (this.sedes().length > 0) {
      this.toggleSedeSeleccionada(this.sedes()[0].id, true);
    }
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
    const sedesIds = (val.sedesSeleccionadas || []) as string[];
    const editId = this.editingId();

    if (editId) {
      this.estructuraService
        .actualizarCarrera(editId, {
          nombre: val.nombre!.trim(),
          sigla: val.sigla?.trim() || undefined,
          descripcion: val.descripcion?.trim() || undefined,
          areaDisciplinaria: val.areaDisciplinaria!.trim(),
          modalidad: val.modalidad!,
          estado: val.estado!,
          tituloOtorgado: val.tituloOtorgado?.trim() || undefined,
          duracionAnios: val.duracionAnios ? Number(val.duracionAnios) : undefined,
          activo: val.activo ?? true,
          sedesIds: sedesIds.length > 0 ? sedesIds : undefined,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (actualizada) => {
            this.carreras.update((list) =>
              list.map((item) => (item.id === actualizada.id ? actualizada : item))
            );
            this.successMessage.set(`Carrera de grado "${actualizada.nombre}" actualizada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al actualizar carrera:', err);
            const msg = err.error?.error || 'No se pudo actualizar la carrera. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    } else {
      this.estructuraService
        .crearCarrera({
          unidadAcademicaId: val.unidadAcademicaId!,
          nombre: val.nombre!.trim(),
          sigla: val.sigla?.trim() || undefined,
          descripcion: val.descripcion?.trim() || undefined,
          areaDisciplinaria: val.areaDisciplinaria!.trim(),
          modalidad: val.modalidad!,
          estado: val.estado!,
          tituloOtorgado: val.tituloOtorgado?.trim() || undefined,
          duracionAnios: val.duracionAnios ? Number(val.duracionAnios) : undefined,
          sedesIds: sedesIds.length > 0 ? sedesIds : undefined,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (nueva) => {
            this.carreras.update((list) => [...list, nueva]);
            this.successMessage.set(`Carrera de grado "${nueva.nombre}" registrada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al registrar carrera:', err);
            const msg = err.error?.error || 'No se pudo registrar la carrera. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    }
  }

  cambiarEstado(carrera: CarreraResponse): void {
    const nuevoEstado = !carrera.activo;
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .cambiarEstadoCarrera(carrera.id, nuevoEstado)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (actualizada) => {
          this.carreras.update((list) =>
            list.map((item) => (item.id === actualizada.id ? actualizada : item))
          );
          this.successMessage.set(
            `Estado de "${actualizada.nombre}" actualizado a ${actualizada.activo ? 'Activa' : 'Inactiva'}.`
          );
        },
        error: (err) => {
          console.error('Error al cambiar estado:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo cambiar el estado de la carrera.');
        },
      });
  }

  eliminar(carrera: CarreraResponse): void {
    if (!confirm(`¿Está seguro de eliminar la carrera de grado "${carrera.nombre}"? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .eliminarCarrera(carrera.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.carreras.update((list) => list.filter((item) => item.id !== carrera.id));
          this.successMessage.set(`Carrera "${carrera.nombre}" eliminada exitosamente.`);
          if (this.editingId() === carrera.id) {
            this.cancelarFormulario();
          }
        },
        error: (err) => {
          console.error('Error al eliminar carrera:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo eliminar la carrera.');
        },
      });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  onFilterUaChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedUaFilter.set(select.value);
  }

  filteredCarreras(): CarreraResponse[] {
    const q = this.searchQuery().toLowerCase().trim();
    const uaFilter = this.selectedUaFilter();

    return this.carreras().filter((c) => {
      if (uaFilter && c.unidadAcademicaId !== uaFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const nom = c.nombre.toLowerCase();
      const sig = (c.sigla || '').toLowerCase();
      const area = (c.areaDisciplinaria || '').toLowerCase();
      const titulo = (c.tituloOtorgado || '').toLowerCase();
      return nom.includes(q) || sig.includes(q) || area.includes(q) || titulo.includes(q);
    });
  }

  getNombreUnidad(uaId: string): string {
    const ua = this.unidades().find((u) => u.id === uaId);
    return ua ? ua.nombre : 'Unidad Académica';
  }

  getNombreSede(sedeId: string): string {
    const sede = this.sedes().find((s) => s.id === sedeId);
    return sede ? sede.nombre : 'Sede';
  }

  isFieldInvalid(control: FormControl | null): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }
}
