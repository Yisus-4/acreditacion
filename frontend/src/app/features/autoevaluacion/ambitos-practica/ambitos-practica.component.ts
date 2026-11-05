import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';
import { AmbitoPracticaService } from '../../../core/ambitos-practica/ambito-practica.service';
import {
  AmbitoPractica,
  AutoevaluacionCarreraAmbito,
  TipoCategoriaAmbito,
} from '../../../core/ambitos-practica/ambito-practica.models';
import { ConvenioService } from '../../../core/convenios/convenio.service';
import { ConvenioInstitucional } from '../../../core/convenios/convenio.models';
import { EstructuraService } from '../../../core/estructura/estructura.service';
import { CarreraResponse } from '../../../core/estructura/estructura.types';
import { AutoevaluacionService } from '../../../core/autoevaluacion/autoevaluacion.service';
import { Inmueble } from '../../../core/autoevaluacion/autoevaluacion.models';

@Component({
  selector: 'app-ambitos-practica',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SubnavAutoevaluacionComponent,
  ],
  templateUrl: './ambitos-practica.component.html',
  styleUrls: ['../infraestructura/infraestructura.component.scss', './ambitos-practica.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AmbitosPracticaComponent implements OnInit {
  private readonly ambitoService = inject(AmbitoPracticaService);
  private readonly convenioService = inject(ConvenioService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly autoevaluacionService = inject(AutoevaluacionService);
  private readonly fb = inject(FormBuilder);

  readonly ambitos = signal<AmbitoPractica[]>([]);
  readonly convenios = signal<ConvenioInstitucional[]>([]);
  readonly inmuebles = signal<Inmueble[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly autoevaluaciones = signal<AutoevaluacionCarreraAmbito[]>([]);

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Tabs de categoría
  readonly tabActivo = signal<TipoCategoriaAmbito>('INTERNO');
  readonly searchQuery = signal<string>('');

  // Modo: Maestro Institucional vs Snapshot de Carrera
  readonly modoSnapshot = signal<boolean>(false);
  readonly carreraSeleccionadaId = signal<string>('');

  // Modal Alta / Modificación Maestro
  readonly modalAbierto = signal<boolean>(false);
  readonly ambitoSeleccionadoId = signal<string | null>(null);
  readonly guardando = signal<boolean>(false);

  // Modal Vinculación Carrera (Snapshot)
  readonly modalVinculacionAbierto = signal<boolean>(false);
  readonly ambitoParaVincular = signal<AmbitoPractica | null>(null);

  readonly form = this.fb.group({
    denominacion: ['', [Validators.required, Validators.maxLength(250)]],
    tipoCategoria: ['INTERNO' as TipoCategoriaAmbito, [Validators.required]],
    tipoAmbito: ['Laboratorio'],
    descripcion: [''],
    direccion: [''],
    inmuebleId: [''],
    convenioId: [''],
    superficieM2: [null as number | null],
    capacidadAlumnos: [null as number | null],
    disponibilidadHoraria: [''],
    actividadesDemostrativas: [''],
    actividadAlumnoCam: [''],
    activo: [true],
  });

  readonly formVinculacion = this.fb.group({
    utilizadoPorCarrera: [true],
    asignaturasVinculadas: ['', [Validators.required]],
    actividadesPrevistas: [''],
    responsablePractica: [''],
    observaciones: [''],
  });

  readonly ambitosFiltrados = computed(() => {
    let lista = this.ambitos().filter((a) => a.tipoCategoria === this.tabActivo());
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      lista = lista.filter(
        (a) =>
          a.denominacion?.toLowerCase().includes(query) ||
          a.direccion?.toLowerCase().includes(query) ||
          a.convenioNombre?.toLowerCase().includes(query) ||
          a.tipoAmbito?.toLowerCase().includes(query)
      );
    }
    return lista;
  });

  ngOnInit(): void {
    this.cargarDatosIniciales();
  }

  cargarDatosIniciales(): void {
    this.loading.set(true);

    this.estructuraService.listarCarreras().subscribe({
      next: (data: CarreraResponse[]) => {
        this.carreras.set(data);
        if (data.length > 0 && !this.carreraSeleccionadaId()) {
          const agri = data.find((c: CarreraResponse) => c.nombre.toLowerCase().includes('agrimensura'));
          this.carreraSeleccionadaId.set(agri ? agri.id : data[0].id);
        }
        this.cargarAutoevaluaciones();
      },
      error: () => {},
    });

    this.convenioService.listarConvenios().subscribe({
      next: (data) => this.convenios.set(data),
      error: () => {},
    });

    this.autoevaluacionService.listarInmueblesMaestros().subscribe({
      next: (data: Inmueble[]) => this.inmuebles.set(data),
      error: () => {},
    });

    this.cargarAmbitos();
  }

  cargarAmbitos(): void {
    this.ambitoService.listarAmbitos().subscribe({
      next: (data) => {
        this.ambitos.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Error al cargar ámbitos de formación: ' + (err.error?.message || err.message));
        this.loading.set(false);
      },
    });
  }

  cargarAutoevaluaciones(): void {
    const carreraId = this.carreraSeleccionadaId();
    if (!carreraId) return;

    this.ambitoService.listarAutoevaluaciones(carreraId).subscribe({
      next: (data) => this.autoevaluaciones.set(data),
      error: () => {},
    });
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.carreraSeleccionadaId.set(select.value);
    this.cargarAutoevaluaciones();
  }

  seleccionarTab(tipo: TipoCategoriaAmbito): void {
    this.tabActivo.set(tipo);
  }

  obtenerEvaluacion(ambitoId?: string): AutoevaluacionCarreraAmbito | undefined {
    if (!ambitoId) return undefined;
    return this.autoevaluaciones().find((ae) => ae.ambitoPracticaId === ambitoId);
  }

  abrirModalNuevo(): void {
    this.ambitoSeleccionadoId.set(null);
    this.form.reset({
      denominacion: '',
      tipoCategoria: this.tabActivo(),
      tipoAmbito: 'Laboratorio',
      descripcion: '',
      direccion: '',
      inmuebleId: '',
      convenioId: '',
      superficieM2: null,
      capacidadAlumnos: null,
      disponibilidadHoraria: '',
      actividadesDemostrativas: '',
      actividadAlumnoCam: '',
      activo: true,
    });
    this.modalAbierto.set(true);
  }

  abrirModalEditar(ambito: AmbitoPractica): void {
    this.ambitoSeleccionadoId.set(ambito.id || null);
    this.form.patchValue({
      denominacion: ambito.denominacion,
      tipoCategoria: ambito.tipoCategoria,
      tipoAmbito: ambito.tipoAmbito || 'Laboratorio',
      descripcion: ambito.descripcion || '',
      direccion: ambito.direccion || '',
      inmuebleId: ambito.inmuebleId || '',
      convenioId: ambito.convenioId || '',
      superficieM2: ambito.superficieM2 || null,
      capacidadAlumnos: ambito.capacidadAlumnos || null,
      disponibilidadHoraria: ambito.disponibilidadHoraria || '',
      actividadesDemostrativas: ambito.actividadesDemostrativas || '',
      actividadAlumnoCam: ambito.actividadAlumnoCam || '',
      activo: ambito.activo ?? true,
    });
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.ambitoSeleccionadoId.set(null);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const formVal = this.form.value;
    const dto: AmbitoPractica = {
      denominacion: formVal.denominacion!,
      tipoCategoria: formVal.tipoCategoria as TipoCategoriaAmbito,
      tipoAmbito: formVal.tipoAmbito || undefined,
      descripcion: formVal.descripcion || undefined,
      direccion: formVal.direccion || undefined,
      inmuebleId: formVal.inmuebleId || undefined,
      convenioId: formVal.convenioId || undefined,
      superficieM2: formVal.superficieM2 ? Number(formVal.superficieM2) : undefined,
      capacidadAlumnos: formVal.capacidadAlumnos ? Number(formVal.capacidadAlumnos) : undefined,
      disponibilidadHoraria: formVal.disponibilidadHoraria || undefined,
      actividadesDemostrativas: formVal.actividadesDemostrativas || undefined,
      actividadAlumnoCam: formVal.actividadAlumnoCam || undefined,
      activo: formVal.activo ?? true,
    };

    const id = this.ambitoSeleccionadoId();
    if (id) {
      this.ambitoService.actualizarAmbito(id, dto).subscribe({
        next: () => {
          this.successMessage.set('Ámbito de práctica actualizado.');
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarAmbitos();
          setTimeout(() => this.successMessage.set(null), 3000);
        },
        error: (err) => {
          this.errorMessage.set('Error al actualizar: ' + (err.error?.message || err.message));
          this.guardando.set(false);
        },
      });
    } else {
      this.ambitoService.crearAmbito(dto).subscribe({
        next: () => {
          this.successMessage.set('Ámbito de práctica registrado en el padrón.');
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarAmbitos();
          setTimeout(() => this.successMessage.set(null), 3000);
        },
        error: (err) => {
          this.errorMessage.set('Error al registrar: ' + (err.error?.message || err.message));
          this.guardando.set(false);
        },
      });
    }
  }

  eliminar(ambito: AmbitoPractica): void {
    if (!ambito.id) return;
    if (!confirm(`¿Eliminar el ámbito "${ambito.denominacion}"?`)) return;

    this.ambitoService.eliminarAmbito(ambito.id).subscribe({
      next: () => {
        this.successMessage.set('Ámbito eliminado.');
        this.cargarAmbitos();
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (err) => {
        this.errorMessage.set('No se pudo eliminar el ámbito: ' + (err.error?.message || err.message));
      },
    });
  }

  // --- Snapshot Autoevaluación Carrera ---
  abrirModalVinculacion(ambito: AmbitoPractica): void {
    this.ambitoParaVincular.set(ambito);
    const evaluacionExistente = this.obtenerEvaluacion(ambito.id);

    if (evaluacionExistente) {
      this.formVinculacion.patchValue({
        utilizadoPorCarrera: evaluacionExistente.utilizadoPorCarrera,
        asignaturasVinculadas: evaluacionExistente.asignaturasVinculadas || '',
        actividadesPrevistas: evaluacionExistente.actividadesPrevistas || '',
        responsablePractica: evaluacionExistente.responsablePractica || '',
        observaciones: evaluacionExistente.observaciones || '',
      });
    } else {
      this.formVinculacion.reset({
        utilizadoPorCarrera: true,
        asignaturasVinculadas: '',
        actividadesPrevistas: '',
        responsablePractica: '',
        observaciones: '',
      });
    }
    this.modalVinculacionAbierto.set(true);
  }

  cerrarModalVinculacion(): void {
    this.modalVinculacionAbierto.set(false);
    this.ambitoParaVincular.set(null);
  }

  guardarVinculacion(): void {
    const ambito = this.ambitoParaVincular();
    const carreraId = this.carreraSeleccionadaId();
    if (!ambito?.id || !carreraId) return;

    const val = this.formVinculacion.value;
    const dto: AutoevaluacionCarreraAmbito = {
      carreraId: carreraId,
      ambitoPracticaId: ambito.id,
      utilizadoPorCarrera: val.utilizadoPorCarrera ?? true,
      asignaturasVinculadas: val.asignaturasVinculadas || undefined,
      actividadesPrevistas: val.actividadesPrevistas || undefined,
      responsablePractica: val.responsablePractica || undefined,
      observaciones: val.observaciones || undefined,
    };

    this.ambitoService.guardarAutoevaluacion(dto).subscribe({
      next: () => {
        this.successMessage.set('Vinculación de carrera guardada para CONEAU.');
        this.cerrarModalVinculacion();
        this.cargarAutoevaluaciones();
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (err) => {
        this.errorMessage.set('Error al guardar vinculación: ' + (err.error?.message || err.message));
      },
    });
  }
}
