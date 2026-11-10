import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
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

export type ActividadAmbitoTipo =
  | 'MAESTRO_RG006'
  | 'INTERNOS_FT008'
  | 'EXTERNOS_FT009'
  | 'DEMOSTRATIVAS_FT010'
  | 'CAM_FT011';

@Component({
  selector: 'app-ambitos-practica',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
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
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly ambitos = signal<AmbitoPractica[]>([]);
  readonly convenios = signal<ConvenioInstitucional[]>([]);
  readonly inmuebles = signal<Inmueble[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly autoevaluaciones = signal<AutoevaluacionCarreraAmbito[]>([]);

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Navegación de Actividades
  readonly actividadSeleccionada = signal<ActividadAmbitoTipo | null>(null);
  readonly procesoId = signal<string | null>(null);
  readonly filtroActividadesTexto = signal<string>('');

  // Tabs de categoría en el modo Maestro RG-006
  readonly tabCategoriaMaestro = signal<TipoCategoriaAmbito>('INTERNO');
  readonly searchQuery = signal<string>('');

  // Selector de carrera para las fichas específicas (OLA 2)
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
    especiesAnimalesCultivos: [''],
    numeroAnimales: [null as number | null],
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

  // Lista oficial de 5 actividades de la dimensión
  readonly listaActividadesDimension = computed(() => {
    const q = this.filtroActividadesTexto().toLowerCase().trim();
    const tieneAmbitos = this.ambitos().length > 0;
    const tieneEvaluaciones = this.autoevaluaciones().length > 0;

    const actividades = [
      {
        id: 'MAESTRO_RG006' as const,
        nombre: 'Gestionar información sobre ámbitos de formación práctica y unidades demostrativas',
        responsable: 'Comisión de Carrera / AEA / SGA',
        instrumento: 'Formulario CONEAU (RG-006)',
        ola: 'OLA 1 (Independiente)',
        completada: tieneAmbitos,
        estado: tieneAmbitos ? 'Completada' : 'Pendiente',
      },
      {
        id: 'INTERNOS_FT008' as const,
        nombre: 'Gestionar información de ámbitos de práctica internos de la carrera',
        responsable: 'Director de Carrera / Comisión Curricular',
        instrumento: 'Ficha FT-008: Registro de Laboratorios y Talleres',
        ola: 'OLA 2 (Dependiente)',
        dependencia: 'Requiere Ámbitos maestro (RG-006)',
        completada: tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO'),
        estado: tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO') ? 'Completada' : 'Pendiente',
      },
      {
        id: 'EXTERNOS_FT009' as const,
        nombre: 'Gestionar información de ámbitos de práctica externos de la carrera',
        responsable: 'Secretaría de Extensión / Carrera',
        instrumento: 'Ficha FT-009 y Convenios de Práctica (RG-007)',
        ola: 'OLA 2 (Dependiente)',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Convenios vigentes',
        completada: tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO'),
        estado: tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO') ? 'Completada' : 'Pendiente',
      },
      {
        id: 'DEMOSTRATIVAS_FT010' as const,
        nombre: 'Gestionar información de unidades demostrativas de la carrera',
        responsable: 'Área Académica / Dirección de Escuela',
        instrumento: 'Ficha FT-010: Campos Experimentales (Tilimuqui)',
        ola: 'OLA 2 (Dependiente)',
        dependencia: 'Requiere Ámbitos maestro (RG-006)',
        completada: tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA'),
        estado: tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA') ? 'Completada' : 'Pendiente',
      },
      {
        id: 'CAM_FT011' as const,
        nombre: 'Gestionar información de centros de atención médica (CAM) de la carrera',
        responsable: 'Dirección CAM / Escuela de Salud / Director Carrera',
        instrumento: 'Ficha FT-011: Habilitación Sanitaria y Convenios Médicos',
        ola: 'OLA 2 (Dependiente)',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Convenios hospitalarios',
        completada: tieneEvaluaciones && this.tieneVinculacionTipo('CAM'),
        estado: tieneEvaluaciones && this.tieneVinculacionTipo('CAM') ? 'Completada' : 'Pendiente',
      },
    ];

    if (!q) return actividades;
    return actividades.filter(
      (a) =>
        a.nombre.toLowerCase().includes(q) ||
        a.responsable.toLowerCase().includes(q) ||
        a.instrumento.toLowerCase().includes(q)
    );
  });

  // Lista filtrada en el maestro RG-006
  readonly ambitosMaestroFiltrados = computed(() => {
    let lista = this.ambitos().filter((a) => a.tipoCategoria === this.tabCategoriaMaestro());
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

  // Lista filtrada para la ficha de la carrera seleccionada
  readonly ambitosCarreraFiltrados = computed(() => {
    const act = this.actividadSeleccionada();
    let tipo: TipoCategoriaAmbito = 'INTERNO';
    if (act === 'EXTERNOS_FT009') tipo = 'EXTERNO';
    if (act === 'DEMOSTRATIVAS_FT010') tipo = 'UNIDAD_DEMOSTRATIVA';
    if (act === 'CAM_FT011') tipo = 'CAM';

    let lista = this.ambitos().filter((a) => a.tipoCategoria === tipo);
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      lista = lista.filter(
        (a) =>
          a.denominacion?.toLowerCase().includes(query) ||
          a.direccion?.toLowerCase().includes(query) ||
          a.convenioNombre?.toLowerCase().includes(query)
      );
    }
    return lista;
  });

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const pId = params.get('procesoId') || params.get('convocatoriaId');
      if (pId) {
        this.procesoId.set(pId);
      }
      const cId = params.get('carreraId');
      if (cId) {
        this.carreraSeleccionadaId.set(cId);
      }
      const act = params.get('actividad');
      if (
        act === 'MAESTRO_RG006' ||
        act === 'INTERNOS_FT008' ||
        act === 'EXTERNOS_FT009' ||
        act === 'DEMOSTRATIVAS_FT010' ||
        act === 'CAM_FT011'
      ) {
        this.actividadSeleccionada.set(act);
      }
    });

    this.cargarDatosIniciales();
  }

  cargarDatosIniciales(): void {
    this.loading.set(true);

    this.estructuraService.listarCarreras().subscribe({
      next: (data: CarreraResponse[]) => {
        this.carreras.set(data);
        if (data.length > 0 && !this.carreraSeleccionadaId()) {
          this.carreraSeleccionadaId.set(data[0].id);
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

    this.ambitoService.listarAutoevaluaciones(carreraId, this.procesoId() || undefined).subscribe({
      next: (data) => this.autoevaluaciones.set(data),
      error: () => {},
    });
  }

  tieneVinculacionTipo(tipo: TipoCategoriaAmbito): boolean {
    const idsDeTipo = this.ambitos()
      .filter((a) => a.tipoCategoria === tipo)
      .map((a) => a.id);
    return this.autoevaluaciones().some(
      (ae) => ae.utilizadoPorCarrera && idsDeTipo.includes(ae.ambitoPracticaId)
    );
  }

  seleccionarActividad(tipo: ActividadAmbitoTipo): void {
    this.actividadSeleccionada.set(tipo);
    this.searchQuery.set('');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  volverAlListado(): void {
    this.actividadSeleccionada.set(null);
    this.searchQuery.set('');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.carreraSeleccionadaId.set(select.value);
    this.cargarAutoevaluaciones();
  }

  seleccionarTabMaestro(tipo: TipoCategoriaAmbito): void {
    this.tabCategoriaMaestro.set(tipo);
    this.searchQuery.set('');
  }

  obtenerEvaluacion(ambitoId?: string): AutoevaluacionCarreraAmbito | undefined {
    if (!ambitoId) return undefined;
    return this.autoevaluaciones().find((ae) => ae.ambitoPracticaId === ambitoId);
  }

  abrirModalNuevo(): void {
    this.ambitoSeleccionadoId.set(null);
    this.form.reset({
      denominacion: '',
      tipoCategoria: this.tabCategoriaMaestro(),
      tipoAmbito: 'Laboratorio',
      descripcion: '',
      direccion: '',
      inmuebleId: '',
      convenioId: '',
      superficieM2: null,
      capacidadAlumnos: null,
      disponibilidadHoraria: '',
      actividadesDemostrativas: '',
      especiesAnimalesCultivos: '',
      numeroAnimales: null,
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
      especiesAnimalesCultivos: ambito.especiesAnimalesCultivos || '',
      numeroAnimales: ambito.numeroAnimales || null,
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
      especiesAnimalesCultivos: formVal.especiesAnimalesCultivos || undefined,
      numeroAnimales: formVal.numeroAnimales ? Number(formVal.numeroAnimales) : undefined,
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
          this.successMessage.set('Ámbito de práctica registrado en el padrón maestro.');
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
      procesoId: this.procesoId() || undefined,
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
