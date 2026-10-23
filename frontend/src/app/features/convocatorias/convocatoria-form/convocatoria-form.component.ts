import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import { EstructuraService, CarreraResponse, UnidadAcademicaResponse } from '../../../core/estructura/estructura.service';
import { IniciarConvocatoriaRequest, ItemCarreraSeleccion, TipoProceso } from '../../../core/convocatorias/models/convocatoria.model';

export interface CarreraSeleccionState {
  carrera: CarreraResponse;
  seleccionada: boolean;
  esObligatoria: boolean;
}

@Component({
  selector: 'app-convocatoria-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './convocatoria-form.component.html',
  styleUrls: ['./convocatoria-form.component.scss'],
})
export class ConvocatoriaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly router = inject(Router);

  readonly today = new Date().toISOString().split('T')[0];

  form!: FormGroup;
  readonly escuelas = signal<UnidadAcademicaResponse[]>([]);
  readonly loadingEscuelas = signal<boolean>(false);
  readonly carrerasState = signal<CarreraSeleccionState[]>([]);
  readonly loadingCarreras = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly hasAtLeastOneCarreraSeleccionada = computed(() => {
    return this.carrerasState().some((c) => c.seleccionada);
  });

  readonly hasAtLeastOneObligatoria = computed(() => {
    return this.carrerasState().some((c) => c.seleccionada && c.esObligatoria);
  });

  get esCarreraEnFuncionamiento(): boolean {
    return this.form?.get('tipoProceso')?.value === 'EN_FUNCIONAMIENTO';
  }

  ngOnInit(): void {
    this.initForm();
    this.cargarEscuelas();
  }

  private initForm(): void {
    this.form = this.fb.group(
      {
        unidadAcademicaId: ['', Validators.required],
        nroResolucionConeau: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
        fechaInicio: [this.today, Validators.required],
        fechaCierre: [
          '',
          [
            Validators.required,
            (control: AbstractControl): ValidationErrors | null => {
              if (!control.value) {
                return null;
              }
              return control.value < this.today ? { fechaCierreAnteriorAHoy: true } : null;
            },
          ],
        ],
        tipoProceso: ['EN_FUNCIONAMIENTO' as TipoProceso, Validators.required],
        observaciones: [''],
      },
      { validators: this.fechasValidator }
    );

    this.form.get('tipoProceso')?.valueChanges.subscribe((tipo: TipoProceso) => {
      const ctrl = this.form.get('nroResolucionConeau');
      if (!ctrl) return;
      if (tipo === 'EN_FUNCIONAMIENTO') {
        ctrl.setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
      } else {
        ctrl.clearValidators();
      }
      ctrl.updateValueAndValidity();
    });

    this.form.get('unidadAcademicaId')?.valueChanges.subscribe((uaId: string) => {
      if (uaId) {
        this.cargarCarrerasDeEscuela(uaId);
      } else {
        this.carrerasState.set([]);
      }
    });
  }

  private fechasValidator(group: FormGroup): { [key: string]: boolean } | null {
    const inicio = group.get('fechaInicio')?.value;
    const cierre = group.get('fechaCierre')?.value;
    if (inicio && cierre && cierre < inicio) {
      return { fechaCierreAnterior: true };
    }
    return null;
  }

  private cargarEscuelas(): void {
    this.loadingEscuelas.set(true);
    this.estructuraService.listarUnidadesAcademicas().subscribe({
      next: (data) => {
        const activas = data.filter((u) => u.activo);
        this.escuelas.set(activas);
        this.loadingEscuelas.set(false);
      },
      error: () => {
        this.loadingEscuelas.set(false);
        this.errorMessage.set('No fue posible cargar las escuelas registradas.');
      },
    });
  }

  private cargarCarrerasDeEscuela(unidadAcademicaId: string): void {
    this.loadingCarreras.set(true);
    this.carrerasState.set([]);
    this.estructuraService.listarCarreras(unidadAcademicaId).subscribe({
      next: (data) => {
        const carrerasGrado = data.filter((c) => c.activo && (!c.nivel || c.nivel.toUpperCase() === 'GRADO'));
        // Inicializar estado: si hay al menos una carrera, la primera queda seleccionada y obligatoria por sugerencia inicial
        const estados: CarreraSeleccionState[] = carrerasGrado.map((c, index) => ({
          carrera: c,
          seleccionada: index === 0,
          esObligatoria: index === 0,
        }));
        this.carrerasState.set(estados);
        this.loadingCarreras.set(false);
      },
      error: () => {
        this.loadingCarreras.set(false);
        this.errorMessage.set('No fue posible cargar las carreras de la escuela seleccionada.');
      },
    });
  }

  toggleSeleccionCarrera(carreraId: string): void {
    this.carrerasState.update((prev) =>
      prev.map((item) => {
        if (item.carrera.id === carreraId) {
          const nuevaSeleccion = !item.seleccionada;
          return {
            ...item,
            seleccionada: nuevaSeleccion,
            // Si se deselecciona, también se apaga la obligatoriedad
            esObligatoria: nuevaSeleccion ? item.esObligatoria : false,
          };
        }
        return item;
      })
    );
  }

  toggleObligatoriaCarrera(carreraId: string, event: Event): void {
    event.stopPropagation();
    this.carrerasState.update((prev) =>
      prev.map((item) => {
        if (item.carrera.id === carreraId) {
          const nuevoValor = !item.esObligatoria;
          return {
            ...item,
            esObligatoria: nuevoValor,
            // Si se marca como obligatoria, automáticamente se selecciona
            seleccionada: nuevoValor ? true : item.seleccionada,
          };
        }
        return item;
      })
    );
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.hasAtLeastOneCarreraSeleccionada()) {
      this.errorMessage.set('Debe seleccionar al menos una carrera para la convocatoria.');
      return;
    }

    if (!this.hasAtLeastOneObligatoria()) {
      this.errorMessage.set('Al menos una de las carreras seleccionadas debe marcarse como obligatoria con el check correspondiente.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const formVal = this.form.value;
    const nroRes = (formVal.nroResolucionConeau || '').trim();

    const carrerasPayload: ItemCarreraSeleccion[] = this.carrerasState()
      .filter((c) => c.seleccionada)
      .map((c) => ({
        carreraId: c.carrera.id,
        seleccionada: true,
        esObligatoria: c.esObligatoria,
      }));

    const request: IniciarConvocatoriaRequest = {
      unidadAcademicaId: formVal.unidadAcademicaId,
      nroResolucionConeau: formVal.tipoProceso === 'EN_FUNCIONAMIENTO' ? nroRes : (nroRes || undefined),
      fechaInicio: formVal.fechaInicio,
      fechaCierre: formVal.fechaCierre,
      tipoProceso: formVal.tipoProceso,
      observaciones: formVal.observaciones?.trim() || undefined,
      carreras: carrerasPayload,
    };

    this.convocatoriaService.iniciarConvocatoria(request).subscribe({
      next: (convocatoria) => {
        this.submitting.set(false);
        void this.router.navigate(['/acreditacion/convocatorias', convocatoria.id]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.error || 'Ocurrió un error al enviar la convocatoria a la Dirección de Escuela.');
      },
    });
  }
}
