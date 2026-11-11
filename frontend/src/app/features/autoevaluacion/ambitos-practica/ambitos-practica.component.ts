import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
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
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import {
  ActividadCurricularPlan,
  DocenteResumen,
} from '../../../core/planes-estudio/plan-estudio.models';
import { AutoevaluacionContextService } from '../../../core/autoevaluacion/autoevaluacion-context.service';
import { NotificationState } from '../../../core/autoevaluacion/notification-state.helper';

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
  private readonly planService = inject(PlanEstudioService);
  readonly contextService = inject(AutoevaluacionContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private readonly notif = new NotificationState();

  readonly ambitos = signal<AmbitoPractica[]>([]);
  readonly convenios = signal<ConvenioInstitucional[]>([]);
  readonly inmuebles = signal<Inmueble[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly autoevaluaciones = signal<AutoevaluacionCarreraAmbito[]>([]);
  readonly asignaturasDelPlan = signal<ActividadCurricularPlan[]>([]);
  readonly docentesDisponibles = signal<DocenteResumen[]>([]);
  readonly completandoActividad = signal<boolean>(false);
  readonly actividadesCompletadas = signal<Set<string>>(new Set<string>());

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

  // Selector de carrera para las fichas específicas
  readonly carreraSeleccionadaId = signal<string>('');

  // Precedencia PRG-002: Plan debe tener asignaturas cargadas
  readonly tieneAsignaturas = computed(() => this.asignaturasDelPlan().length > 0);
  readonly puedeCompletarActividadesPractica = computed(() => this.tieneAsignaturas());

  // Modal Alta / Modificación Maestro
  readonly modalAbierto = signal<boolean>(false);
  readonly ambitoSeleccionadoId = signal<string | null>(null);
  readonly guardando = signal<boolean>(false);

  // Modal Vinculación Carrera (Snapshot)
  readonly modalVinculacionAbierto = signal<boolean>(false);
  readonly ambitoParaVincular = signal<AmbitoPractica | null>(null);

  // Modal Vinculación Chips & Filtros
  readonly asignaturasSeleccionadasChips = signal<string[]>([]);
  readonly filtroAsignaturasTexto = signal<string>('');
  readonly docentesSeleccionadosChips = signal<string[]>([]);
  readonly filtroDocentesTexto = signal<string>('');

  readonly asignaturasFiltradasParaModal = computed(() => {
    const q = this.filtroAsignaturasTexto().toLowerCase().trim();
    const asigs = this.asignaturasDelPlan();
    if (!q) return asigs;
    return asigs.filter(
      (a) =>
        a.nombre.toLowerCase().includes(q) ||
        (a.codigo && a.codigo.toLowerCase().includes(q))
    );
  });

  readonly docentesFiltradosParaModal = computed(() => {
    const q = this.filtroDocentesTexto().toLowerCase().trim();
    const docs = this.docentesDisponibles();
    if (!q) return docs.slice(0, 10);
    return docs.filter(
      (d) =>
        (d.nombreCompleto && d.nombreCompleto.toLowerCase().includes(q)) ||
        (d.username && d.username.toLowerCase().includes(q)) ||
        (d.email && d.email.toLowerCase().includes(q))
    );
  });

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
        dependencia: '',
        completada: tieneAmbitos,
        estado: tieneAmbitos ? 'Completada' : 'Pendiente',
      },
      {
        id: 'INTERNOS_FT008' as const,
        nombre: 'Gestionar información de ámbitos de práctica internos de la carrera',
        responsable: 'Director de Carrera / Comisión Curricular',
        instrumento: 'Ficha FT-008: Registro de Laboratorios y Talleres',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Plan de Estudios (8.3)',
        completada: this.actividadesCompletadas().has('INTERNOS_FT008') || (tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO')),
        estado: (this.actividadesCompletadas().has('INTERNOS_FT008') || (tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO'))) ? 'Completada' : 'Pendiente',
      },
      {
        id: 'EXTERNOS_FT009' as const,
        nombre: 'Gestionar información de ámbitos de práctica externos de la carrera',
        responsable: 'Secretaría de Extensión / Carrera',
        instrumento: 'Ficha FT-009 y Convenios de Práctica (RG-007)',
        dependencia: 'Requiere Ámbitos maestro (RG-006), Convenios vigentes y Plan (8.3)',
        completada: this.actividadesCompletadas().has('EXTERNOS_FT009') || (tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO')),
        estado: (this.actividadesCompletadas().has('EXTERNOS_FT009') || (tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO'))) ? 'Completada' : 'Pendiente',
      },
      {
        id: 'DEMOSTRATIVAS_FT010' as const,
        nombre: 'Gestionar información de unidades demostrativas de la carrera',
        responsable: 'Área Académica / Dirección de Escuela',
        instrumento: 'Ficha FT-010: Campos Experimentales (Tilimuqui)',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Plan (8.3)',
        completada: this.actividadesCompletadas().has('DEMOSTRATIVAS_FT010') || (tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA')),
        estado: (this.actividadesCompletadas().has('DEMOSTRATIVAS_FT010') || (tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA'))) ? 'Completada' : 'Pendiente',
      },
      {
        id: 'CAM_FT011' as const,
        nombre: 'Gestionar información de centros de atención médica (CAM) de la carrera',
        responsable: 'Dirección CAM / Escuela de Salud / Director Carrera',
        instrumento: 'Ficha FT-011: Habilitación Sanitaria y Convenios Médicos',
        dependencia: 'Requiere Ámbitos maestro (RG-006), Convenios hospitalarios y Plan (8.3)',
        completada: this.actividadesCompletadas().has('CAM_FT011') || (tieneEvaluaciones && this.tieneVinculacionTipo('CAM')),
        estado: (this.actividadesCompletadas().has('CAM_FT011') || (tieneEvaluaciones && this.tieneVinculacionTipo('CAM'))) ? 'Completada' : 'Pendiente',
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

  constructor() {
    effect(() => {
      const ctxProcesoId = this.contextService.procesoIdActivo();
      if (ctxProcesoId && ctxProcesoId !== this.procesoId()) {
        this.procesoId.set(ctxProcesoId);
      }
      const ctxCarreraId = this.contextService.carreraSeleccionadaId();
      if (ctxCarreraId && ctxCarreraId !== this.carreraSeleccionadaId()) {
        this.carreraSeleccionadaId.set(ctxCarreraId);
        this.cargarAutoevaluaciones();
        this.cargarAsignaturasDelPlan();
      }
    });
  }

  ngOnInit(): void {
    this.notif.registrarTeardown(this.destroyRef);

    this.route.queryParamMap.subscribe((params) => {
      const convIdParam = params.get('convocatoriaId');
      const procIdParam = params.get('procesoId');
      const validProcId =
        procIdParam && procIdParam !== convIdParam
          ? procIdParam
          : this.contextService.procesoIdActivo();
      if (validProcId) {
        this.procesoId.set(validProcId);
      }
      const cId = params.get('carreraId') || this.contextService.carreraSeleccionadaId();
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
      this.cargarAutoevaluaciones();
      this.cargarAsignaturasDelPlan();
    });

    this.cargarDatosIniciales();
  }

  cargarDatosIniciales(): void {
    this.loading.set(true);

    this.estructuraService.listarCarreras().subscribe({
      next: (data: CarreraResponse[]) => {
        this.carreras.set(data);
        if (data.length > 0 && !this.carreraSeleccionadaId()) {
          const fallbackId = this.contextService.carreraSeleccionadaId() || data[0].id;
          this.carreraSeleccionadaId.set(fallbackId);
        }
        this.cargarAutoevaluaciones();
        this.cargarAsignaturasDelPlan();
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

    this.cargarDocentes();
    this.cargarAmbitos();
  }

  cargarDocentes(): void {
    this.ambitoService.listarDocentes().subscribe({
      next: (data) => this.docentesDisponibles.set(data),
      error: () => {},
    });
  }

  cargarAsignaturasDelPlan(): void {
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    const procesoId = this.procesoId() || this.contextService.procesoIdActivo();
    if (!carreraId && !procesoId) {
      this.asignaturasDelPlan.set([]);
      return;
    }

    this.planService.listarPlanes(carreraId || undefined, procesoId || undefined).subscribe({
      next: (planes) => {
        if (planes && planes.length > 0 && planes[0].id) {
          this.planService.listarAsignaturas(planes[0].id).subscribe({
            next: (asigs) => this.asignaturasDelPlan.set(asigs),
            error: () => this.asignaturasDelPlan.set([]),
          });
        } else {
          this.asignaturasDelPlan.set([]);
        }
      },
      error: () => this.asignaturasDelPlan.set([]),
    });
  }

  cargarAmbitos(): void {
    this.ambitoService.listarAmbitos().subscribe({
      next: (data) => {
        this.ambitos.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'Error al cargar ámbitos de formación: ' + (err.error?.message || err.message), 6000);
        this.loading.set(false);
      },
    });
  }

  cargarAutoevaluaciones(): void {
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    if (!carreraId) return;
    const procesoId = this.procesoId() || this.contextService.procesoIdActivo();

    this.ambitoService.listarAutoevaluaciones(carreraId, procesoId || undefined).subscribe({
      next: (data) => this.autoevaluaciones.set(data),
      error: () => {},
    });
  }

  tieneVinculacionTipo(tipo: TipoCategoriaAmbito): boolean {
    const idsDeTipo = this.ambitos()
      .filter((a) => a.tipoCategoria === tipo)
      .map((a) => a.id);
    return this.autoevaluaciones().some(
      (ae) =>
        ae.utilizadoPorCarrera &&
        (idsDeTipo.includes(ae.ambitoPracticaId) || (ae.ambitoId && idsDeTipo.includes(ae.ambitoId)))
    );
  }

  seleccionarActividad(tipo: ActividadAmbitoTipo): void {
    this.actividadSeleccionada.set(tipo);
    this.searchQuery.set('');
    this.notif.limpiar(this.errorMessage);
    this.notif.limpiar(this.successMessage);
  }

  volverAlListado(): void {
    this.actividadSeleccionada.set(null);
    this.searchQuery.set('');
    this.notif.limpiar(this.errorMessage);
    this.notif.limpiar(this.successMessage);
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.carreraSeleccionadaId.set(select.value);
    this.contextService.seleccionarCarrera(select.value);
    this.cargarAutoevaluaciones();
    this.cargarAsignaturasDelPlan();
  }

  seleccionarTabMaestro(tipo: TipoCategoriaAmbito): void {
    this.tabCategoriaMaestro.set(tipo);
    this.searchQuery.set('');
  }

  obtenerEvaluacion(ambitoId?: string): AutoevaluacionCarreraAmbito | undefined {
    if (!ambitoId) return undefined;
    return this.autoevaluaciones().find(
      (ae) => ae.ambitoPracticaId === ambitoId || ae.ambitoId === ambitoId
    );
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
          this.notif.mostrar(this.successMessage, 'Ámbito de práctica actualizado.', 6000);
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarAmbitos();
        },
        error: (err) => {
          this.notif.mostrar(this.errorMessage, 'Error al actualizar: ' + (err.error?.message || err.message), 6000);
          this.guardando.set(false);
        },
      });
    } else {
      this.ambitoService.crearAmbito(dto).subscribe({
        next: () => {
          this.notif.mostrar(this.successMessage, 'Ámbito de práctica registrado en el padrón maestro.', 6000);
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarAmbitos();
        },
        error: (err) => {
          this.notif.mostrar(this.errorMessage, 'Error al registrar: ' + (err.error?.message || err.message), 6000);
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
        this.notif.mostrar(this.successMessage, 'Ámbito eliminado.', 6000);
        this.cargarAmbitos();
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'No se pudo eliminar el ámbito: ' + (err.error?.message || err.message), 6000);
      },
    });
  }

  // --- Snapshot Autoevaluación Carrera ---
  abrirModalVinculacion(ambito: AmbitoPractica): void {
    this.ambitoParaVincular.set(ambito);
    this.filtroAsignaturasTexto.set('');
    this.filtroDocentesTexto.set('');
    const evaluacionExistente = this.obtenerEvaluacion(ambito.id);

    if (evaluacionExistente) {
      this.formVinculacion.patchValue({
        utilizadoPorCarrera: evaluacionExistente.utilizadoPorCarrera,
        asignaturasVinculadas: evaluacionExistente.asignaturasVinculadas || '',
        actividadesPrevistas: evaluacionExistente.actividadesPrevistas || '',
        responsablePractica: evaluacionExistente.responsablePractica || '',
        observaciones: evaluacionExistente.observaciones || '',
      });

      const asigTokens = evaluacionExistente.asignaturasVinculadas
        ? evaluacionExistente.asignaturasVinculadas.split(/[,;]/).map((s) => s.trim()).filter((s) => s.length > 0)
        : [];
      this.asignaturasSeleccionadasChips.set(asigTokens);

      const docTokens = evaluacionExistente.responsablePractica
        ? evaluacionExistente.responsablePractica.split(/[,;]/).map((s) => s.trim()).filter((s) => s.length > 0)
        : [];
      this.docentesSeleccionadosChips.set(docTokens);
    } else {
      this.formVinculacion.reset({
        utilizadoPorCarrera: true,
        asignaturasVinculadas: '',
        actividadesPrevistas: '',
        responsablePractica: '',
        observaciones: '',
      });
      this.asignaturasSeleccionadasChips.set([]);
      this.docentesSeleccionadosChips.set([]);
    }
    this.modalVinculacionAbierto.set(true);
  }

  cerrarModalVinculacion(): void {
    this.modalVinculacionAbierto.set(false);
    this.ambitoParaVincular.set(null);
  }

  isAsignaturaSeleccionada(nombre: string): boolean {
    return this.asignaturasSeleccionadasChips().includes(nombre);
  }

  toggleAsignaturaVinculada(nombre: string): void {
    const current = this.asignaturasSeleccionadasChips();
    let updated: string[];
    if (current.includes(nombre)) {
      updated = current.filter((s) => s !== nombre);
    } else {
      updated = [...current, nombre];
    }
    this.asignaturasSeleccionadasChips.set(updated);
    this.formVinculacion.patchValue({ asignaturasVinculadas: updated.join('; ') });
  }

  removerAsignaturaChip(nombre: string): void {
    const updated = this.asignaturasSeleccionadasChips().filter((s) => s !== nombre);
    this.asignaturasSeleccionadasChips.set(updated);
    this.formVinculacion.patchValue({ asignaturasVinculadas: updated.join('; ') });
  }

  agregarAsignaturaManual(nombreInput: HTMLInputElement): void {
    const val = nombreInput.value.trim();
    if (!val) return;
    if (!this.asignaturasSeleccionadasChips().includes(val)) {
      const updated = [...this.asignaturasSeleccionadasChips(), val];
      this.asignaturasSeleccionadasChips.set(updated);
      this.formVinculacion.patchValue({ asignaturasVinculadas: updated.join('; ') });
    }
    nombreInput.value = '';
  }

  isDocenteSeleccionado(nombre: string): boolean {
    return this.docentesSeleccionadosChips().includes(nombre);
  }

  toggleDocenteResponsable(nombreCompleto: string): void {
    const current = this.docentesSeleccionadosChips();
    let updated: string[];
    if (current.includes(nombreCompleto)) {
      updated = current.filter((s) => s !== nombreCompleto);
    } else {
      updated = [...current, nombreCompleto];
    }
    this.docentesSeleccionadosChips.set(updated);
    this.formVinculacion.patchValue({ responsablePractica: updated.join('; ') });
  }

  removerDocenteChip(nombre: string): void {
    const updated = this.docentesSeleccionadosChips().filter((s) => s !== nombre);
    this.docentesSeleccionadosChips.set(updated);
    this.formVinculacion.patchValue({ responsablePractica: updated.join('; ') });
  }

  agregarDocenteManual(nombreInput: HTMLInputElement): void {
    const val = nombreInput.value.trim();
    if (!val) return;
    if (!this.docentesSeleccionadosChips().includes(val)) {
      const updated = [...this.docentesSeleccionadosChips(), val];
      this.docentesSeleccionadosChips.set(updated);
      this.formVinculacion.patchValue({ responsablePractica: updated.join('; ') });
    }
    nombreInput.value = '';
  }

  guardarVinculacion(): void {
    const ambito = this.ambitoParaVincular();
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    if (!ambito?.id || !carreraId) return;

    const val = this.formVinculacion.value;
    const asignaturasStr = val.asignaturasVinculadas || this.asignaturasSeleccionadasChips().join('; ');
    const responsablesStr = val.responsablePractica || this.docentesSeleccionadosChips().join('; ');

    const dto: AutoevaluacionCarreraAmbito = {
      carreraId: carreraId,
      procesoId: this.procesoId() || this.contextService.procesoIdActivo() || undefined,
      ambitoPracticaId: ambito.id,
      ambitoId: ambito.id,
      utilizadoPorCarrera: val.utilizadoPorCarrera ?? true,
      asignaturasVinculadas: asignaturasStr || undefined,
      actividadesPrevistas: val.actividadesPrevistas || undefined,
      responsablePractica: responsablesStr || undefined,
      observaciones: val.observaciones || undefined,
    };

    this.ambitoService.guardarAutoevaluacion(dto).subscribe({
      next: () => {
        this.notif.mostrar(this.successMessage, 'Vinculación de carrera guardada para CONEAU.', 6000);
        this.cerrarModalVinculacion();
        this.cargarAutoevaluaciones();
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'Error al guardar vinculación: ' + (err.error?.message || err.message), 6000);
      },
    });
  }

  isActividadCompletada(actId: string): boolean {
    return this.actividadesCompletadas().has(actId);
  }

  completarActividad(actividadCodigo?: string): void {
    const pId = this.procesoId() || this.contextService.procesoIdActivo();
    const act = actividadCodigo || this.actividadSeleccionada();
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId() || undefined;

    if (!pId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar un proceso o convocatoria de autoevaluación activa para completar la actividad.', 6000);
      return;
    }

    if (!act || act === 'MAESTRO_RG006') {
      return;
    }

    if (!this.tieneAsignaturas()) {
      this.notif.mostrar(
        this.errorMessage,
        'Precedencia requerida (CONEAU PRG-002): Para vincular y completar los ámbitos de práctica es necesario cargar primero las asignaturas del Plan de Estudios (Actividad 8.3 / FT-001).',
        6000
      );
      return;
    }

    this.completandoActividad.set(true);
    this.errorMessage.set(null);

    this.ambitoService.completarActividadAmbitos(pId, act, carreraId).subscribe({
      next: (res) => {
        this.completandoActividad.set(false);
        this.notif.mostrar(this.successMessage, res?.mensaje || 'Actividad completada y validada exitosamente ante CONEAU.', 6000);
        const comp = new Set(this.actividadesCompletadas());
        comp.add(act);
        this.actividadesCompletadas.set(comp);
        this.cargarAutoevaluaciones();
      },
      error: (err) => {
        this.completandoActividad.set(false);
        const msg = err.error?.message || err.message || 'Error al completar la actividad.';
        this.notif.mostrar(this.errorMessage, msg, 6000);
      },
    });
  }
}
