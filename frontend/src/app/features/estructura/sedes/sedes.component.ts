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
import { finalize, of, switchMap, tap } from 'rxjs';

import { EstructuraService, SedeLocalizacionResponse } from '../../../core/estructura/estructura.service';
import {
  Departamento,
  GeografiaService,
  Localidad,
  Provincia,
} from '../../../core/geografia/geografia.service';
import { InstitucionService } from '../../../core/instituciones/institucion.service';
import { InstitucionResponse } from '../../../core/instituciones/institucion.types';

@Component({
  selector: 'app-sedes',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './sedes.component.html',
  styleUrl: './sedes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SedesComponent implements OnInit {
  private readonly estructuraService = inject(EstructuraService);
  private readonly institucionService = inject(InstitucionService);
  private readonly geografiaService = inject(GeografiaService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly sedes = signal<SedeLocalizacionResponse[]>([]);
  readonly instituciones = signal<InstitucionResponse[]>([]);
  readonly provincias = signal<Provincia[]>([]);
  readonly departamentos = signal<Departamento[]>([]);
  readonly localidades = signal<Localidad[]>([]);

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
    calle: ['', [Validators.required, Validators.maxLength(255)]],
    numero: ['', [Validators.required, Validators.maxLength(50)]],
    piso: ['', [Validators.maxLength(20)]],
    codigoPostal: ['', [Validators.required, Validators.maxLength(20)]],
    provinciaId: [null as number | null, [Validators.required]],
    departamentoId: [{ value: null as number | null, disabled: true }, [Validators.required]],
    localidadId: [{ value: null as number | null, disabled: true }, [Validators.required]],
    esPropia: [true],
  });

  ngOnInit(): void {
    this.cargarDatos();
    this.setupGeografiaCascade();
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

    this.geografiaService
      .obtenerProvincias()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (provs) => this.provincias.set(provs),
        error: (err) => console.error('Error al cargar provincias:', err),
      });

    this.estructuraService
      .listarSedes()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (items) => this.sedes.set(items),
        error: (err) => {
          console.error('Error al listar sedes:', err);
          this.errorMessage.set('No se pudo cargar el listado de sedes y localizaciones.');
        },
      });
  }

  private setupGeografiaCascade(): void {
    const provControl = this.form.get('provinciaId');
    const deptoControl = this.form.get('departamentoId');
    const locControl = this.form.get('localidadId');

    provControl?.valueChanges
      .pipe(
        tap(() => {
          this.departamentos.set([]);
          this.localidades.set([]);
          deptoControl?.setValue(null, { emitEvent: false });
          locControl?.setValue(null, { emitEvent: false });
          deptoControl?.disable();
          locControl?.disable();
        }),
        switchMap((provId) => {
          if (!provId) return of([]);
          return this.geografiaService.obtenerDepartamentos(provId);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (deptos) => {
          this.departamentos.set(deptos);
          if (deptos.length > 0) {
            deptoControl?.enable();
          }
        },
        error: (err) => console.error('Error al cargar departamentos:', err),
      });

    deptoControl?.valueChanges
      .pipe(
        tap(() => {
          this.localidades.set([]);
          locControl?.setValue(null, { emitEvent: false });
          locControl?.disable();
        }),
        switchMap((deptoId) => {
          if (!deptoId) return of([]);
          return this.geografiaService.obtenerLocalidades(deptoId);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (locs) => {
          this.localidades.set(locs);
          if (locs.length > 0) {
            locControl?.enable();
          }
        },
        error: (err) => console.error('Error al cargar localidades:', err),
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
        calle: '',
        numero: '',
        piso: '',
        codigoPostal: '',
        provinciaId: null,
        departamentoId: null,
        localidadId: null,
        esPropia: true,
      });
      this.form.get('departamentoId')?.disable();
      this.form.get('localidadId')?.disable();
      this.showForm.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);
    }
  }

  iniciarEdicion(sede: SedeLocalizacionResponse): void {
    this.editingId.set(sede.id);
    this.showForm.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.geografiaService.obtenerDepartamentos(sede.provinciaId)
      .pipe(
        switchMap((deptos) => {
          this.departamentos.set(deptos);
          this.form.get('departamentoId')?.enable();
          return this.geografiaService.obtenerLocalidades(sede.departamentoId);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (locs) => {
          this.localidades.set(locs);
          this.form.get('localidadId')?.enable();
          this.form.patchValue({
            institucionId: sede.institucionId,
            nombre: sede.nombre,
            calle: sede.calle,
            numero: sede.numero,
            piso: sede.piso || '',
            codigoPostal: sede.codigoPostal,
            provinciaId: sede.provinciaId,
            departamentoId: sede.departamentoId,
            localidadId: sede.localidadId,
            esPropia: sede.esPropia,
          }, { emitEvent: false });
        },
        error: (err) => console.error('Error al cargar geografía para edición:', err),
      });
  }

  cancelarFormulario(): void {
    this.editingId.set(null);
    this.showForm.set(false);
    this.form.reset({
      institucionId: this.instituciones()[0]?.id || '',
      nombre: '',
      calle: '',
      numero: '',
      piso: '',
      codigoPostal: '',
      provinciaId: null,
      departamentoId: null,
      localidadId: null,
      esPropia: true,
    });
    this.form.get('departamentoId')?.disable();
    this.form.get('localidadId')?.disable();
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
        .actualizarSede(editId, {
          nombre: val.nombre!.trim(),
          calle: val.calle!.trim(),
          numero: val.numero!.trim(),
          piso: val.piso?.trim() || undefined,
          codigoPostal: val.codigoPostal!.trim(),
          provinciaId: Number(val.provinciaId),
          departamentoId: Number(val.departamentoId),
          localidadId: Number(val.localidadId),
          esPropia: val.esPropia ?? true,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (actualizada) => {
            this.sedes.update((list) =>
              list.map((item) => (item.id === actualizada.id ? actualizada : item))
            );
            this.successMessage.set(`Sede "${actualizada.nombre}" actualizada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al actualizar sede:', err);
            const msg = err.error?.error || 'No se pudo actualizar la sede. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    } else {
      this.estructuraService
        .crearSede({
          institucionId: val.institucionId!,
          nombre: val.nombre!.trim(),
          calle: val.calle!.trim(),
          numero: val.numero!.trim(),
          piso: val.piso?.trim() || undefined,
          codigoPostal: val.codigoPostal!.trim(),
          provinciaId: Number(val.provinciaId),
          departamentoId: Number(val.departamentoId),
          localidadId: Number(val.localidadId),
          esPropia: val.esPropia ?? true,
        })
        .pipe(
          finalize(() => this.submitting.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: (nueva) => {
            this.sedes.update((list) => [...list, nueva]);
            this.successMessage.set(`Sede "${nueva.nombre}" registrada exitosamente.`);
            this.cancelarFormulario();
          },
          error: (err) => {
            console.error('Error al registrar sede:', err);
            const msg = err.error?.error || 'No se pudo registrar la sede. Intente nuevamente.';
            this.errorMessage.set(msg);
          },
        });
    }
  }

  cambiarEstado(sede: SedeLocalizacionResponse): void {
    const nuevoEstado = !sede.activo;
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .cambiarEstadoSede(sede.id, nuevoEstado)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (actualizada) => {
          this.sedes.update((list) =>
            list.map((item) => (item.id === actualizada.id ? actualizada : item))
          );
          this.successMessage.set(
            `Estado de "${actualizada.nombre}" actualizado a ${actualizada.activo ? 'Activa' : 'Inactiva'}.`
          );
        },
        error: (err) => {
          console.error('Error al cambiar estado:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo cambiar el estado de la sede.');
        },
      });
  }

  eliminar(sede: SedeLocalizacionResponse): void {
    if (!confirm(`¿Está seguro de eliminar la sede "${sede.nombre}"? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.estructuraService
      .eliminarSede(sede.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.sedes.update((list) => list.filter((item) => item.id !== sede.id));
          this.successMessage.set(`Sede "${sede.nombre}" eliminada exitosamente.`);
          if (this.editingId() === sede.id) {
            this.cancelarFormulario();
          }
        },
        error: (err) => {
          console.error('Error al eliminar sede:', err);
          this.errorMessage.set(err.error?.error || 'No se pudo eliminar la sede.');
        },
      });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  filteredSedes(): SedeLocalizacionResponse[] {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) {
      return this.sedes();
    }
    return this.sedes().filter((s) => {
      const nom = s.nombre.toLowerCase();
      const dir = (s.direccionCompleta || '').toLowerCase();
      const cp = s.codigoPostal.toLowerCase();
      return nom.includes(q) || dir.includes(q) || cp.includes(q);
    });
  }

  isFieldInvalid(control: FormControl | null): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }
}
