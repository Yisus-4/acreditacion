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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';
import { AmbitoPracticaService } from '../../../core/ambitos-practica/ambito-practica.service';
import {
  ActividadVinculadaResumen,
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
import { Auth } from '../../../core/auth/auth';

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
  private readonly auth = inject(Auth);
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
  private lastLoadedKey = '';

  // Tabs de categoría en la gestión aislada RG-006
  readonly tabCategoriaMaestro = signal<TipoCategoriaAmbito>('INTERNO');
  readonly searchQuery = signal<string>('');

  // Selector de carrera para la autoevaluación de ámbitos
  readonly carreraSeleccionadaId = signal<string>('');

  readonly procesoIdEfectivo = computed<string | null>(() => {
    const cId = this.carreraSeleccionadaId();
    const detalle = this.contextService.convocatoriaDetalle();
    if (cId && detalle?.carrerasPropuestas?.length) {
      const match = detalle.carrerasPropuestas.find((cp) => cp.carreraId === cId);
      if (match?.procesoId) {
        return match.procesoId;
      }
    }
    return this.contextService.procesoIdActivo() || this.procesoId();
  });

  readonly nombreCarreraActiva = computed<string>(() => {
    const cId = this.carreraSeleccionadaId();
    return (
      this.carreras().find((c) => c.id === cId)?.nombre ||
      this.contextService.carreraActiva()?.nombre ||
      'Carrera seleccionada'
    );
  });

  // Precedencia PRG-002: Plan debe tener asignaturas cargadas
  readonly tieneAsignaturas = computed(() => this.asignaturasDelPlan().length > 0);
  readonly puedeCompletarActividadesPractica = computed(() => this.tieneAsignaturas());

  // Permisos y Roles (Strict Granular Roles PRG-002 / CONEAU)
  readonly esAdmin = computed(() => this.auth.hasAnyRole('AEA'));
  readonly puedeGestionarPadronMaestroRg006 = computed(() =>
    this.auth.hasAnyRole('AEA')
  );

  readonly perteneceACarreraActiva = computed(() => {
    if (this.esAdmin()) return true;
    const user = this.auth.currentSession?.user;
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    const carrera =
      this.carreras().find((c) => c.id === carreraId) ||
      this.contextService.carreraActiva();
    if (!user?.unidadAcademicaId || !carrera) return false;
    return carrera.unidadAcademicaId === user.unidadAcademicaId;
  });

  readonly puedeEditarFichasAmbitos = computed(() => {
    if (this.esAdmin()) return true;
    return (
      this.perteneceACarreraActiva() &&
      this.auth.hasAnyRole(
        'DIRECTOR_CARRERA',
        'DIRECTOR_ESCUELA',
        'COMISION_AUTOEVALUACION'
      )
    );
  });

  readonly puedeGestionarAmbitosCarrera = computed(() =>
    this.puedeGestionarPadronMaestroRg006() || this.puedeEditarFichasAmbitos()
  );

  readonly puedeCompletarActividadAmbito = computed(() => {
    if (!this.puedeEditarFichasAmbitos()) return false;
    if (!this.tieneAsignaturas()) return false;
    const act = this.actividadSeleccionada();
    if (!act || act === 'MAESTRO_RG006') return false;
    let tipo: TipoCategoriaAmbito = 'INTERNO';
    if (act === 'EXTERNOS_FT009') tipo = 'EXTERNO';
    if (act === 'DEMOSTRATIVAS_FT010') tipo = 'UNIDAD_DEMOSTRATIVA';
    if (act === 'CAM_FT011') tipo = 'CAM';
    return this.tieneVinculacionTipo(tipo);
  });

  // Modal Alta / Modificación en copia de la carrera
  readonly modalAbierto = signal<boolean>(false);
  readonly ambitoSeleccionadoId = signal<string | null>(null);
  readonly guardando = signal<boolean>(false);

  // Modal Copiar Datos Maestros (RG-006) para la carrera activa
  readonly modalCopiarAbierto = signal<boolean>(false);
  readonly copiandoMaestros = signal<boolean>(false);
  readonly modoCopiaMaestros = signal<'INICIALIZAR_O_AGREGAR_FALTANTES' | 'REEMPLAZAR_COPIA'>(
    'INICIALIZAR_O_AGREGAR_FALTANTES'
  );

  // Modal Vinculación Carrera (Snapshot)
  readonly modalVinculacionAbierto = signal<boolean>(false);
  readonly ambitoParaVincular = signal<AmbitoPractica | null>(null);

  // Modal Vinculación Chips & Filtros
  readonly actividadesSeleccionadasIds = signal<string[]>([]);
  readonly actividadesDetallePrevio = signal<ActividadVinculadaResumen[]>([]);
  readonly asignaturasManualesChips = signal<string[]>([]);
  readonly filtroAsignaturasTexto = signal<string>('');
  readonly docentesSeleccionadosChips = signal<string[]>([]);
  readonly filtroDocentesTexto = signal<string>('');

  readonly chipsAsignaturasSeleccionadas = computed(() => {
    const asigsPlan = this.asignaturasDelPlan();
    const detallePrevio = this.actividadesDetallePrevio();
    const chips: Array<{
      tipo: 'actividad' | 'manual';
      key: string;
      id?: string;
      manualValue?: string;
      label: string;
    }> = [];

    for (const id of this.actividadesSeleccionadasIds()) {
      const asig = asigsPlan.find((a) => a.id === id);
      const det = detallePrevio.find((d) => d.id === id);
      const nombre = asig?.nombre ?? det?.nombre ?? id;
      const codigo = (asig?.codigo ?? det?.codigo)?.trim();
      const label = codigo ? `${nombre} [${codigo}]` : nombre;
      chips.push({ tipo: 'actividad', key: `act:${id}`, id, label });
    }

    for (const manual of this.asignaturasManualesChips()) {
      chips.push({ tipo: 'manual', key: `man:${manual}`, manualValue: manual, label: manual });
    }

    return chips;
  });

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
        nombre: 'Gestionar ámbitos de formación práctica de la carrera (Copia RG-006)',
        responsable: 'Dirección de Carrera / Comisión de Autoevaluación / AEA',
        instrumento: 'Registro RG-006: Copia aislada desde maestro (Alta, Baja y Modificación)',
        dependencia: 'Copia los ámbitos maestros institucionales y los adapta a la carrera',
        completada: tieneAmbitos,
        estado: tieneAmbitos ? `Copiado (${this.ambitos().length})` : 'Sin copiar',
        tipoFicha: undefined as TipoCategoriaAmbito | undefined,
      },
      {
        id: 'INTERNOS_FT008' as const,
        nombre: 'Gestionar información de ámbitos de práctica internos de la carrera',
        responsable: 'Director de Carrera / Comisión Curricular',
        instrumento: 'Ficha FT-008: Registro de Laboratorios y Talleres',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Plan de Estudios (8.3)',
        completada: this.actividadesCompletadas().has('INTERNOS_FT008') || (tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO')),
        estado: (this.actividadesCompletadas().has('INTERNOS_FT008') || (tieneEvaluaciones && this.tieneVinculacionTipo('INTERNO'))) ? 'Completada' : 'Pendiente',
        tipoFicha: 'INTERNO' as TipoCategoriaAmbito,
      },
      {
        id: 'EXTERNOS_FT009' as const,
        nombre: 'Gestionar información de ámbitos de práctica externos de la carrera',
        responsable: 'Secretaría de Extensión / Carrera',
        instrumento: 'Ficha FT-009 y Convenios de Práctica (RG-007)',
        dependencia: 'Requiere Ámbitos maestro (RG-006), Convenios vigentes y Plan (8.3)',
        completada: this.actividadesCompletadas().has('EXTERNOS_FT009') || (tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO')),
        estado: (this.actividadesCompletadas().has('EXTERNOS_FT009') || (tieneEvaluaciones && this.tieneVinculacionTipo('EXTERNO'))) ? 'Completada' : 'Pendiente',
        tipoFicha: 'EXTERNO' as TipoCategoriaAmbito,
      },
      {
        id: 'DEMOSTRATIVAS_FT010' as const,
        nombre: 'Gestionar información de unidades demostrativas de la carrera',
        responsable: 'Área Académica / Dirección de Escuela',
        instrumento: 'Ficha FT-010: Campos Experimentales (Tilimuqui)',
        dependencia: 'Requiere Ámbitos maestro (RG-006) y Plan (8.3)',
        completada: this.actividadesCompletadas().has('DEMOSTRATIVAS_FT010') || (tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA')),
        estado: (this.actividadesCompletadas().has('DEMOSTRATIVAS_FT010') || (tieneEvaluaciones && this.tieneVinculacionTipo('UNIDAD_DEMOSTRATIVA'))) ? 'Completada' : 'Pendiente',
        tipoFicha: 'UNIDAD_DEMOSTRATIVA' as TipoCategoriaAmbito,
      },
      {
        id: 'CAM_FT011' as const,
        nombre: 'Gestionar información de centros de atención médica (CAM) de la carrera',
        responsable: 'Dirección CAM / Escuela de Salud / Director Carrera',
        instrumento: 'Ficha FT-011: Habilitación Sanitaria y Convenios Médicos',
        dependencia: 'Requiere Ámbitos maestro (RG-006), Convenios hospitalarios y Plan (8.3)',
        completada: this.actividadesCompletadas().has('CAM_FT011') || (tieneEvaluaciones && this.tieneVinculacionTipo('CAM')),
        estado: (this.actividadesCompletadas().has('CAM_FT011') || (tieneEvaluaciones && this.tieneVinculacionTipo('CAM'))) ? 'Completada' : 'Pendiente',
        tipoFicha: 'CAM' as TipoCategoriaAmbito,
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
      const disp = this.contextService.carrerasDisponibles();
      if (disp.length > 0) {
        this.carreras.set(disp);
        if (!this.carreraSeleccionadaId() || !disp.some((c) => c.id === this.carreraSeleccionadaId())) {
          const ctxCarId = this.contextService.carreraSeleccionadaId() || disp[0].id;
          this.carreraSeleccionadaId.set(ctxCarId);
        }
      }

      const ctxCarreraId = this.contextService.carreraSeleccionadaId();
      if (ctxCarreraId && !this.carreraSeleccionadaId()) {
        this.carreraSeleccionadaId.set(ctxCarreraId);
      }

      this.cargarDatosDeCarreraActiva();
    });
  }

  private cargarDatosDeCarreraActiva(force = false): void {
    const pId = this.procesoIdEfectivo();
    const cId = this.carreraSeleccionadaId();
    const loadKey = `${pId || ''}|${cId || ''}`;
    if (loadKey === '|') {
      return;
    }
    if (!force && loadKey === this.lastLoadedKey) {
      return;
    }
    this.lastLoadedKey = loadKey;

    if (pId !== this.procesoId()) {
      this.procesoId.set(pId);
    }
    this.actividadesCompletadas.set(new Set());
    this.cargarAmbitos();
    this.cargarAutoevaluaciones();
    this.cargarAsignaturasDelPlan();
    this.cargarInmueblesProceso();
  }

  ngOnInit(): void {
    this.notif.registrarTeardown(this.destroyRef);

    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
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
      this.cargarDatosDeCarreraActiva(true);
    });

    this.cargarDatosIniciales();
  }

  cargarDatosIniciales(): void {
    const convDisp = this.contextService.carrerasDisponibles();
    if (convDisp.length > 0) {
      this.carreras.set(convDisp);
      if (!this.carreraSeleccionadaId() || !convDisp.some((c) => c.id === this.carreraSeleccionadaId())) {
        const fallbackId = this.contextService.carreraSeleccionadaId() || convDisp[0].id;
        this.carreraSeleccionadaId.set(fallbackId);
      }
      this.cargarDatosDeCarreraActiva();
    } else {
      this.estructuraService.listarCarreras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (data: CarreraResponse[]) => {
          this.carreras.set(data);
          if (data.length > 0 && !this.carreraSeleccionadaId()) {
            const fallbackId = this.contextService.carreraSeleccionadaId() || data[0].id;
            this.carreraSeleccionadaId.set(fallbackId);
          }
          this.cargarDatosDeCarreraActiva();
        },
        error: () => {},
      });
    }

    this.convenioService.listarConvenios().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => this.convenios.set(data),
      error: () => {},
    });

    this.cargarDocentes();
    this.cargarDatosDeCarreraActiva();
  }

  cargarInmueblesProceso(): void {
    const pId = this.procesoIdEfectivo();
    if (!pId) {
      this.inmuebles.set([]);
      return;
    }
    this.autoevaluacionService
      .obtenerInmueblesPorProceso(pId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data: Inmueble[]) => this.inmuebles.set(data),
        error: () => this.inmuebles.set([]),
      });
  }

  cargarDocentes(): void {
    this.ambitoService.listarDocentes().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => this.docentesDisponibles.set(data),
      error: () => {},
    });
  }

  cargarAsignaturasDelPlan(): void {
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    const procesoId = this.procesoIdEfectivo();
    if (!carreraId && !procesoId) {
      this.asignaturasDelPlan.set([]);
      return;
    }

    this.planService.listarPlanes(carreraId || undefined, procesoId || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (planes) => {
        if (planes && planes.length > 0 && planes[0].id) {
          this.planService.listarAsignaturas(planes[0].id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (asigs) => this.asignaturasDelPlan.set(asigs.filter((a) => a.estadoDictado !== 'INACTIVA' && a.estadoDictado !== 'NO_SE_DICTA')),
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
    const pId = this.procesoIdEfectivo();
    if (!pId) {
      this.ambitos.set([]);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.ambitoService.listarAmbitos(undefined, undefined, pId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    const procesoId = this.procesoIdEfectivo();
    if (!carreraId && !procesoId) {
      this.autoevaluaciones.set([]);
      return;
    }

    this.ambitoService.listarAutoevaluaciones(carreraId || undefined, procesoId || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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

  puedeCompletarActividadTipo(tipo?: TipoCategoriaAmbito): boolean {
    if (!this.puedeEditarFichasAmbitos()) return false;
    if (!this.tieneAsignaturas()) return false;
    if (!tipo) return false;
    return this.tieneVinculacionTipo(tipo);
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
    this.cargarDatosDeCarreraActiva(true);
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

  abrirModalCopiarMaestros(): void {
    this.modoCopiaMaestros.set('INICIALIZAR_O_AGREGAR_FALTANTES');
    this.modalCopiarAbierto.set(true);
  }

  cerrarModalCopiarMaestros(): void {
    if (this.copiandoMaestros()) return;
    this.modalCopiarAbierto.set(false);
  }

  confirmarCopiarMaestros(): void {
    if (!this.puedeGestionarAmbitosCarrera()) {
      this.notif.mostrar(this.errorMessage, 'No posee permisos para copiar ámbitos maestros a esta carrera.', 6000);
      return;
    }
    const pId = this.procesoIdEfectivo();
    if (!pId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar una carrera con proceso de autoevaluación activo.', 6000);
      return;
    }

    this.copiandoMaestros.set(true);
    this.autoevaluacionService
      .copiarDatosCentrales(pId, {
        modo: this.modoCopiaMaestros(),
        secciones: ['AMBITOS_PRACTICA'],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.copiandoMaestros.set(false);
          this.modalCopiarAbierto.set(false);
          const cant = res?.ambitosPracticaClonados ?? 0;
          this.notif.mostrar(
            this.successMessage,
            `Copia completada para ${this.nombreCarreraActiva()}: ${cant} ámbitos de práctica copiados desde el Padrón Maestro (RG-006).`,
            7000
          );
          this.cargarAmbitos();
          this.cargarAutoevaluaciones();
        },
        error: (err) => {
          this.copiandoMaestros.set(false);
          this.notif.mostrar(
            this.errorMessage,
            'Error al copiar ámbitos maestros: ' + (err.error?.message || err.message),
            6000
          );
        },
      });
  }

  guardar(): void {
    if (!this.puedeGestionarAmbitosCarrera()) {
      this.notif.mostrar(this.errorMessage, 'No posee permisos para gestionar los ámbitos de práctica de esta carrera.', 6000);
      return;
    }

    const pId = this.procesoIdEfectivo();
    if (!pId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar una carrera con proceso de autoevaluación activo.', 6000);
      return;
    }

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
      esMaestro: false,
      procesoId: pId,
      activo: formVal.activo ?? true,
    };

    const id = this.ambitoSeleccionadoId();
    if (id) {
      this.ambitoService.actualizarAmbito(id, dto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.notif.mostrar(this.successMessage, 'Ámbito de práctica actualizado en la carrera.', 6000);
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
      this.ambitoService.crearAmbito(dto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.notif.mostrar(this.successMessage, `Ámbito de práctica registrado en la autoevaluación de ${this.nombreCarreraActiva()}.`, 6000);
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
    if (!this.puedeGestionarAmbitosCarrera()) {
      this.notif.mostrar(this.errorMessage, 'No posee permisos para eliminar ámbitos de práctica de esta carrera.', 6000);
      return;
    }
    if (!ambito.id) return;
    if (!confirm(`¿Eliminar el ámbito "${ambito.denominacion}" de la autoevaluación de ${this.nombreCarreraActiva()}?\n\nEsto no afectará el Padrón Maestro Institucional (RG-006) ni otras carreras.`)) return;

    this.ambitoService.eliminarAmbito(ambito.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notif.mostrar(this.successMessage, 'Ámbito eliminado de la copia de la carrera.', 6000);
        this.cargarAmbitos();
        this.cargarAutoevaluaciones();
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

      const idsRelacionados = (evaluacionExistente.actividadCurricularIds ?? []).filter(
        (id): id is string => !!id
      );
      this.actividadesDetallePrevio.set(evaluacionExistente.actividadesVinculadasDetalle ?? []);

      const rawManuales =
        evaluacionExistente.asignaturasManuales !== undefined
          ? evaluacionExistente.asignaturasManuales
          : evaluacionExistente.asignaturasVinculadas
            ? evaluacionExistente.asignaturasVinculadas
                .split(/[,;]/)
                .map((s) => s.trim())
                .filter((s) => s.length > 0)
            : [];

      const resolvedIds = [...idsRelacionados];
      const manualesRestantes: string[] = [];

      if (resolvedIds.length === 0 && rawManuales.length > 0) {
        const asigsPlan = this.asignaturasDelPlan();
        for (const token of rawManuales) {
          const matches = this.buscarCoincidenciasUnicasAsignatura(token, asigsPlan);
          if (matches.length === 1 && matches[0].id) {
            if (!resolvedIds.includes(matches[0].id)) {
              resolvedIds.push(matches[0].id);
            }
          } else {
            manualesRestantes.push(token);
          }
        }
      } else {
        manualesRestantes.push(...rawManuales);
      }

      this.actividadesSeleccionadasIds.set(resolvedIds);
      this.asignaturasManualesChips.set(manualesRestantes);
      this.sincronizarCampoAsignaturasVinculadas();

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
      this.actividadesSeleccionadasIds.set([]);
      this.actividadesDetallePrevio.set([]);
      this.asignaturasManualesChips.set([]);
      this.docentesSeleccionadosChips.set([]);
    }
    this.modalVinculacionAbierto.set(true);
  }

  private buscarCoincidenciasUnicasAsignatura(
    token: string,
    asigs: ActividadCurricularPlan[]
  ): ActividadCurricularPlan[] {
    const trimmed = token.trim();
    if (!trimmed) return [];
    const tLower = trimmed.toLowerCase();

    if (trimmed.endsWith(']')) {
      const openIdx = trimmed.lastIndexOf('[');
      if (openIdx > 0) {
        const bracketCode = trimmed.slice(openIdx + 1, -1).trim().toLowerCase();
        if (bracketCode) {
          return asigs.filter((a) => a.codigo && a.codigo.trim().toLowerCase() === bracketCode);
        }
      }
    }

    return asigs.filter(
      (a) =>
        (a.id && a.id.toLowerCase() === tLower) ||
        (a.codigo && a.codigo.trim().toLowerCase() === tLower) ||
        a.nombre.trim().toLowerCase() === tLower
    );
  }

  private sincronizarCampoAsignaturasVinculadas(): void {
    const labels = this.chipsAsignaturasSeleccionadas().map((c) => c.label);
    this.formVinculacion.patchValue({ asignaturasVinculadas: labels.join('; ') });
  }

  cerrarModalVinculacion(): void {
    this.modalVinculacionAbierto.set(false);
    this.ambitoParaVincular.set(null);
  }

  isAsignaturaSeleccionada(asigId?: string): boolean {
    return !!asigId && this.actividadesSeleccionadasIds().includes(asigId);
  }

  toggleAsignaturaVinculada(asigId?: string): void {
    if (!asigId) return;
    const current = this.actividadesSeleccionadasIds();
    const updated = current.includes(asigId)
      ? current.filter((id) => id !== asigId)
      : [...current, asigId];
    this.actividadesSeleccionadasIds.set(updated);
    this.sincronizarCampoAsignaturasVinculadas();
  }

  removerAsignaturaSeleccionada(chip: {
    tipo: 'actividad' | 'manual';
    id?: string;
    manualValue?: string;
  }): void {
    if (chip.tipo === 'actividad' && chip.id) {
      this.actividadesSeleccionadasIds.set(
        this.actividadesSeleccionadasIds().filter((id) => id !== chip.id)
      );
    } else if (chip.tipo === 'manual' && chip.manualValue) {
      this.asignaturasManualesChips.set(
        this.asignaturasManualesChips().filter((m) => m !== chip.manualValue)
      );
    }
    this.sincronizarCampoAsignaturasVinculadas();
  }

  agregarAsignaturaManual(nombreInput: HTMLInputElement): void {
    const val = nombreInput.value.trim();
    if (!val) return;
    if (!this.asignaturasManualesChips().includes(val)) {
      this.asignaturasManualesChips.set([...this.asignaturasManualesChips(), val]);
      this.sincronizarCampoAsignaturasVinculadas();
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
    if (!this.puedeEditarFichasAmbitos()) {
      this.notif.mostrar(this.errorMessage, 'No posee permisos para vincular ámbitos a esta carrera.', 6000);
      return;
    }
    const ambito = this.ambitoParaVincular();
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    const pId = this.procesoIdEfectivo();
    if (!ambito?.id || !carreraId || !pId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar una carrera con proceso de autoevaluación activo.', 6000);
      return;
    }

    const val = this.formVinculacion.value;
    const ids = this.actividadesSeleccionadasIds();
    const manuales = this.asignaturasManualesChips();
    const asignaturasStr =
      this.chipsAsignaturasSeleccionadas().map((c) => c.label).join('; ') ||
      val.asignaturasVinculadas ||
      '';
    const responsablesStr = val.responsablePractica || this.docentesSeleccionadosChips().join('; ');

    const dto: AutoevaluacionCarreraAmbito = {
      carreraId: carreraId,
      procesoId: pId,
      ambitoPracticaId: ambito.id,
      ambitoId: ambito.id,
      utilizadoPorCarrera: val.utilizadoPorCarrera ?? true,
      actividadCurricularIds: ids,
      asignaturasManuales: manuales,
      asignaturasVinculadas: asignaturasStr || undefined,
      actividadesPrevistas: val.actividadesPrevistas || undefined,
      responsablePractica: responsablesStr || undefined,
      observaciones: val.observaciones || undefined,
    };

    this.ambitoService.guardarAutoevaluacion(dto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    const pId = this.procesoIdEfectivo();
    const act = actividadCodigo || this.actividadSeleccionada();
    const carreraId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId() || undefined;

    if (!pId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar un proceso o convocatoria de autoevaluación activa para completar la actividad.', 6000);
      return;
    }

    if (!act || act === 'MAESTRO_RG006') {
      return;
    }

    if (!this.puedeEditarFichasAmbitos()) {
      this.notif.mostrar(this.errorMessage, 'No posee permisos para completar actividades de autoevaluación en esta carrera.', 6000);
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

    let tipo: TipoCategoriaAmbito = 'INTERNO';
    if (act === 'EXTERNOS_FT009') tipo = 'EXTERNO';
    if (act === 'DEMOSTRATIVAS_FT010') tipo = 'UNIDAD_DEMOSTRATIVA';
    if (act === 'CAM_FT011') tipo = 'CAM';

    if (!this.tieneVinculacionTipo(tipo)) {
      this.notif.mostrar(
        this.errorMessage,
        'No es posible completar la actividad: debe existir al menos un ámbito de práctica vinculado a la carrera.',
        6000
      );
      return;
    }

    this.completandoActividad.set(true);
    this.errorMessage.set(null);

    this.ambitoService.completarActividadAmbitos(pId, act, carreraId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
