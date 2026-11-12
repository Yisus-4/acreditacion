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
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import {
  ActividadCurricularPlan,
  ActualizarAsignaturaRequest,
  CrearAsignaturaRequest,
  PlanEstudioFt001,
} from '../../../core/planes-estudio/plan-estudio.models';
import { ProgramaAnaliticoService } from '../../../core/programas-analiticos/programa-analitico.service';
import { ProgramaAnaliticoAsignatura } from '../../../core/programas-analiticos/programa-analitico.models';
import { EstructuraService } from '../../../core/estructura/estructura.service';
import { CarreraResponse } from '../../../core/estructura/estructura.types';
import { AutoevaluacionContextService } from '../../../core/autoevaluacion/autoevaluacion-context.service';
import { NotificationState } from '../../../core/autoevaluacion/notification-state.helper';

export type ActividadCurricularTipo =
  | 'GT_007A'
  | 'GT_001'
  | 'PLAN_FT001'
  | 'PROGRAMAS_84'
  | 'FT_003'
  | 'GT_006';

@Component({
  selector: 'app-estructura-curricular',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    SubnavAutoevaluacionComponent,
  ],
  templateUrl: './estructura-curricular.component.html',
  styleUrls: ['../infraestructura/infraestructura.component.scss', './estructura-curricular.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstructuraCurricularComponent implements OnInit {
  private readonly planService = inject(PlanEstudioService);
  private readonly programaService = inject(ProgramaAnaliticoService);
  private readonly estructuraService = inject(EstructuraService);
  readonly contextService = inject(AutoevaluacionContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private readonly notif = new NotificationState();

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly carreraSeleccionadaId = signal<string>('');
  readonly planes = signal<PlanEstudioFt001[]>([]);
  readonly planActivo = signal<PlanEstudioFt001 | null>(null);
  readonly asignaturas = signal<ActividadCurricularPlan[]>([]);
  readonly programas = signal<ProgramaAnaliticoAsignatura[]>([]);

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Navegación de Actividades
  readonly actividadSeleccionada = signal<ActividadCurricularTipo | null>(null);
  readonly procesoId = signal<string | null>(null);
  readonly filtroActividadesTexto = signal<string>('');

  // Filtros de asignaturas
  readonly filtroAnio = signal<string>('TODOS');
  readonly filtroCiclo = signal<string>('TODOS');
  readonly searchQuery = signal<string>('');

  // Modal Subir Programa Analítico
  readonly modalSubirAbierto = signal<boolean>(false);
  readonly asignaturaSeleccionada = signal<ActividadCurricularPlan | null>(null);
  readonly archivoSeleccionado = signal<File | null>(null);
  readonly subiendo = signal<boolean>(false);

  // Formulario de edición rápida de FT-001
  readonly mostrarEditorPlan = signal<boolean>(false);
  readonly guardandoPlan = signal<boolean>(false);
  readonly inicializandoPlan = signal<boolean>(false);

  readonly formPlan = this.fb.group({
    codigoResolucion: ['', [Validators.required]],
    anioAprobacion: [2024],
    duracionAnios: [5],
    horasTotales: [3650],
    horasPresenciales: [3650],
    horasNoPresenciales: [0],
    estructuraSintetica: [''],
    instanciaFinalGraduacion: [''],
    otraInformacion: [''],
  });

  readonly formSubida = this.fb.group({
    resolucionAprobacion: ['', [Validators.required]],
    vigenciaDesde: [2024, [Validators.required]],
    vigenciaHasta: [null as number | null],
    observaciones: [''],
  });

  // Modal Gestión de Asignaturas (8.3 / FT-001)
  readonly modalAsignaturaAbierto = signal<boolean>(false);
  readonly asignaturaEnEdicion = signal<ActividadCurricularPlan | null>(null);
  readonly guardandoAsignatura = signal<boolean>(false);
  readonly deletingAsignaturaId = signal<string | null>(null);
  readonly correlativasSeleccionadas = signal<string[]>([]);

  readonly formAsignatura = this.fb.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    numeroOrden: [null as number | null],
    grupo: ['C.B.I.', [Validators.required]],
    subgrupo: ['1º', [Validators.required]],
    horasReloj: [64, [Validators.required, Validators.min(1)]],
    horasPresenciales: [64, [Validators.min(0)]],
    horasNoPresenciales: [0, [Validators.min(0)]],
    esObligatoria: [true],
  });

  readonly asignaturasDisponiblesCorrelativas = computed(() => {
    const editando = this.asignaturaEnEdicion();
    return this.asignaturas().filter((a) => {
      if (editando?.id && a.id === editando.id) return false;
      if (editando?.nombre && a.nombre.toLowerCase().trim() === editando.nombre.toLowerCase().trim()) return false;
      return true;
    });
  });

  // Métricas de horas por ciclo
  readonly horasCbi = computed(() =>
    this.asignaturas()
      .filter((a) => a.grupo === 'C.B.I.')
      .reduce((acc, curr) => acc + (curr.horasReloj || 0), 0)
  );
  readonly horasTb = computed(() =>
    this.asignaturas()
      .filter((a) => a.grupo === 'T.B.')
      .reduce((acc, curr) => acc + (curr.horasReloj || 0), 0)
  );
  readonly horasTa = computed(() =>
    this.asignaturas()
      .filter((a) => a.grupo === 'T.A.')
      .reduce((acc, curr) => acc + (curr.horasReloj || 0), 0)
  );
  readonly horasCtc = computed(() =>
    this.asignaturas()
      .filter((a) => a.grupo === 'C.T.C.')
      .reduce((acc, curr) => acc + (curr.horasReloj || 0), 0)
  );

  // Métricas de Programas analíticos
  readonly programasCargadosCount = computed(() => {
    return this.asignaturas().filter((a) => this.obtenerPrograma(a.id)?.estadoCarga === 'CARGADO').length;
  });

  readonly porcentajeProgramas = computed(() => {
    const total = this.asignaturas().length;
    if (total === 0) return 0;
    return Math.round((this.programasCargadosCount() / total) * 100);
  });

  // Lista oficial de 6 actividades de la dimensión
  readonly listaActividadesDimension = computed(() => {
    const q = this.filtroActividadesTexto().toLowerCase().trim();
    const tienePlan = !!this.planActivo();
    const programasOk = this.programasCargadosCount() > 0 && this.programasCargadosCount() === this.asignaturas().length;

    const actividades = [
      {
        id: 'GT_007A' as const,
        nombre: 'Gestionar información sobre mecanismos o instancias de gestión y seguimiento relacionadas a la carrera',
        responsable: 'Director de Carrera / Comisión Curricular',
        instrumento: 'Actas de Seguimiento (GT-007A)',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'GT_001' as const,
        nombre: 'Gestionar información sobre organización y gestión de la carrera',
        responsable: 'Dirección de Carrera',
        instrumento: 'Manual de Procedimientos (GT-001)',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'PLAN_FT001' as const,
        nombre: 'Gestionar información de planes de estudio',
        responsable: 'Secretaría Académica / Dirección de Carrera',
        instrumento: 'Resolución Ministerial / Ficha (FT-001)',
        completada: tienePlan,
        estado: tienePlan ? 'Completada' : 'Pendiente',
      },
      {
        id: 'PROGRAMAS_84' as const,
        nombre: 'Gestionar programas analíticos de las asignaturas',
        responsable: 'Docentes Titulares / Carrera / Comisión Curricular',
        instrumento: 'Programas Vigentes en PDF',
        completada: programasOk,
        estado: programasOk ? 'Completada' : `${this.programasCargadosCount()}/${this.asignaturas().length} cargados`,
      },
      {
        id: 'FT_003' as const,
        nombre: 'Gestionar información de las asignaturas',
        responsable: 'Secretaría Académica / Docentes',
        instrumento: 'Fichas FT-003',
        dependencia: 'Requiere Biblioteca RG-014 y Cuadros SGA',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'GT_006' as const,
        nombre: 'Gestionar información sobre comisiones de planificación y seguimiento de la carrera',
        responsable: 'Comisión de Seguimiento Curricular',
        instrumento: 'Resolución de Designación (GT-006)',
        completada: false,
        estado: 'Pendiente',
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

  // Lista filtrada de asignaturas
  readonly asignaturasFiltradas = computed(() => {
    let lista = this.asignaturas();
    const anio = this.filtroAnio();
    if (anio !== 'TODOS') {
      lista = lista.filter((a) => a.subgrupo === anio);
    }
    const ciclo = this.filtroCiclo();
    if (ciclo !== 'TODOS') {
      lista = lista.filter((a) => a.grupo === ciclo);
    }
    const q = this.searchQuery().toLowerCase().trim();
    if (q) {
      lista = lista.filter(
        (a) =>
          a.nombre?.toLowerCase().includes(q) ||
          a.codigo?.toLowerCase().includes(q)
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
        this.cargarPlan(ctxCarreraId);
      }
      const planActivoId = this.contextService.planConvocadoActivoId();
      if (planActivoId && planActivoId !== this.planActivo()?.id) {
        this.cargarPlanPorId(planActivoId);
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
      if (cId && cId !== this.carreraSeleccionadaId()) {
        this.carreraSeleccionadaId.set(cId);
        this.cargarPlan(cId);
      }
      const act = params.get('actividad');
      if (
        act === 'PLAN_FT001' ||
        act === 'PROGRAMAS_84' ||
        act === 'GT_007A' ||
        act === 'GT_001' ||
        act === 'FT_003' ||
        act === 'GT_006'
      ) {
        this.actividadSeleccionada.set(act as ActividadCurricularTipo);
      }
    });

    this.cargarCarreras();
  }

  cargarCarreras(): void {
    this.loading.set(true);
    this.estructuraService.listarCarreras().subscribe({
      next: (data: CarreraResponse[]) => {
        this.carreras.set(data);
        if (data.length > 0) {
          let id = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
          if (!id || !data.some((c: CarreraResponse) => c.id === id)) {
            id = data[0].id;
          }
          if (id !== this.carreraSeleccionadaId() || !this.planActivo()) {
            this.carreraSeleccionadaId.set(id);
            this.cargarPlan(id);
          } else {
            this.loading.set(false);
          }
        } else {
          this.loading.set(false);
        }
      },
      error: (err: any) => {
        this.notif.mostrar(this.errorMessage, 'Error al cargar carreras: ' + (err.error?.message || err.message), 6000);
        this.loading.set(false);
      },
    });
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.carreraSeleccionadaId.set(select.value);
    this.contextService.seleccionarCarrera(select.value);
    this.cargarPlan(select.value);
  }

  cargarPlanPorId(planId: string): void {
    const cached = this.planes().find((p) => p.id === planId);
    if (cached) {
      this.aplicarPlanActivo(cached);
      return;
    }
    this.loading.set(true);
    this.planService.obtenerPlan(planId).subscribe({
      next: (plan) => {
        if (plan) {
          this.aplicarPlanActivo(plan);
        } else {
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'Error al cargar plan de estudios: ' + (err.error?.message || err.message), 6000);
        this.loading.set(false);
      },
    });
  }

  private aplicarPlanActivo(p: PlanEstudioFt001): void {
    this.planActivo.set(p);
    this.formPlan.patchValue({
      codigoResolucion: p.codigoResolucion,
      anioAprobacion: p.anioAprobacion || 2024,
      duracionAnios: p.duracionAnios || 5,
      horasTotales: p.horasTotales || 3650,
      horasPresenciales: p.horasPresenciales || 3650,
      horasNoPresenciales: p.horasNoPresenciales || 0,
      estructuraSintetica: p.estructuraSintetica || '',
      instanciaFinalGraduacion: p.instanciaFinalGraduacion || '',
      otraInformacion: p.otraInformacion || '',
    });
    if (p.id) {
      this.cargarAsignaturasYProgramas(p.id);
    } else {
      this.loading.set(false);
    }
  }

  cargarPlan(carreraId: string): void {
    this.loading.set(true);
    const procId = this.procesoId() || this.contextService.procesoIdActivo() || undefined;
    this.planService.listarPlanes(carreraId, procId).subscribe({
      next: (planes) => {
        this.planes.set(planes);
        if (planes.length > 0) {
          const activePlanId = this.contextService.planConvocadoActivoId();
          const targetPlan = (activePlanId && planes.find((p) => p.id === activePlanId)) || planes[0];
          this.aplicarPlanActivo(targetPlan);
        } else {
          this.planActivo.set(null);
          this.asignaturas.set([]);
          this.programas.set([]);
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'Error al cargar planes de estudio: ' + (err.error?.message || err.message), 6000);
        this.loading.set(false);
      },
    });
  }

  inicializarPlanFt001(): void {
    const cId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
    if (!cId) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar una carrera de grado para inicializar el plan.', 6000);
      return;
    }
    const pId = this.procesoId() || this.contextService.procesoIdActivo() || undefined;

    this.inicializandoPlan.set(true);
    const dto: PlanEstudioFt001 = {
      carreraId: cId,
      procesoId: pId,
      codigoResolucion: 'RES-CS-INICIAL/2026',
      anioAprobacion: 2026,
      anioInicio: 2026,
      duracionAnios: 5,
      activo: true,
      horasPresenciales: 0,
      horasNoPresenciales: 0,
      horasTotales: 0,
    };

    this.planService.crearPlan(dto).subscribe({
      next: () => {
        this.inicializandoPlan.set(false);
        this.notif.mostrar(this.successMessage, 'Ficha base de Plan de Estudios (FT-001) creada exitosamente.', 6000);
        this.cargarPlan(cId);
      },
      error: (err) => {
        this.inicializandoPlan.set(false);
        this.notif.mostrar(this.errorMessage, 'Error al inicializar plan de estudios: ' + (err.error?.message || err.message), 6000);
      },
    });
  }

  cargarAsignaturasYProgramas(planId: string): void {
    this.planService.listarAsignaturas(planId).subscribe({
      next: (asigs) => {
        this.asignaturas.set(asigs);
        this.planService.obtenerPlan(planId).subscribe({
          next: (p) => {
            if (p) this.planActivo.set(p);
          },
          error: () => {},
        });
        this.programaService.listarProgramas(undefined, planId).subscribe({
          next: (progs) => {
            this.programas.set(progs);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
      error: () => this.loading.set(false),
    });
  }

  seleccionarActividad(tipo: ActividadCurricularTipo): void {
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

  obtenerPrograma(asignaturaId?: string): ProgramaAnaliticoAsignatura | undefined {
    if (!asignaturaId) return undefined;
    return this.programas().find((p) => p.asignaturaPlanId === asignaturaId);
  }

  guardarDatosPlan(): void {
    const plan = this.planActivo();
    if (!plan?.id) return;

    this.guardandoPlan.set(true);
    const val = this.formPlan.value;
    const dto: PlanEstudioFt001 = {
      ...plan,
      codigoResolucion: val.codigoResolucion || plan.codigoResolucion,
      anioAprobacion: val.anioAprobacion || plan.anioAprobacion,
      duracionAnios: val.duracionAnios || plan.duracionAnios,
      horasTotales: val.horasTotales || plan.horasTotales,
      horasPresenciales: val.horasPresenciales || plan.horasPresenciales,
      horasNoPresenciales: val.horasNoPresenciales || plan.horasNoPresenciales,
      estructuraSintetica: val.estructuraSintetica || plan.estructuraSintetica,
      instanciaFinalGraduacion: val.instanciaFinalGraduacion || plan.instanciaFinalGraduacion,
      otraInformacion: val.otraInformacion || plan.otraInformacion,
    };

    this.planService.actualizarPlan(plan.id, dto).subscribe({
      next: (actualizado) => {
        this.planActivo.set(actualizado);
        this.guardandoPlan.set(false);
        this.mostrarEditorPlan.set(false);
        this.notif.mostrar(this.successMessage, 'Ficha de Plan de Estudios (FT-001) actualizada correctamente.', 6000);
      },
      error: (err) => {
        this.notif.mostrar(this.errorMessage, 'Error al actualizar plan: ' + (err.error?.message || err.message), 6000);
        this.guardandoPlan.set(false);
      },
    });
  }

  // --- Modal Subida de Programa PDF ---
  abrirModalSubir(asig: ActividadCurricularPlan): void {
    this.asignaturaSeleccionada.set(asig);
    this.archivoSeleccionado.set(null);
    const prog = this.obtenerPrograma(asig.id);
    if (prog) {
      this.formSubida.patchValue({
        resolucionAprobacion: prog.resolucionAprobacion || 'Resol. HCD 045/24',
        vigenciaDesde: prog.vigenciaDesde || 2024,
        vigenciaHasta: prog.vigenciaHasta || null,
        observaciones: prog.observaciones || '',
      });
    } else {
      this.formSubida.reset({
        resolucionAprobacion: 'Resol. HCD 045/24',
        vigenciaDesde: 2024,
        vigenciaHasta: null,
        observaciones: '',
      });
    }
    this.modalSubirAbierto.set(true);
  }

  cerrarModalSubir(): void {
    this.modalSubirAbierto.set(false);
    this.asignaturaSeleccionada.set(null);
    this.archivoSeleccionado.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.archivoSeleccionado.set(input.files[0]);
    }
  }

  guardarPrograma(): void {
    const asig = this.asignaturaSeleccionada();
    const archivo = this.archivoSeleccionado();

    if (!asig?.id) return;
    if (!archivo) {
      this.notif.mostrar(this.errorMessage, 'Debe seleccionar un archivo PDF para el programa analítico.', 6000);
      return;
    }

    if (this.formSubida.invalid) {
      this.formSubida.markAllAsTouched();
      return;
    }

    this.subiendo.set(true);
    const formVal = this.formSubida.value;
    const plan = this.planActivo();
    const effectiveProcesoId = this.procesoId() || this.contextService.procesoIdActivo() || plan?.procesoId || undefined;

    this.programaService
      .subirPrograma(
        archivo,
        asig.id,
        effectiveProcesoId,
        formVal.resolucionAprobacion || undefined,
        formVal.vigenciaDesde || undefined,
        formVal.vigenciaHasta || undefined,
        formVal.observaciones || undefined
      )
      .subscribe({
        next: () => {
          this.notif.mostrar(this.successMessage, `Programa analítico de "${asig.nombre}" subido correctamente.`, 6000);
          this.subiendo.set(false);
          this.cerrarModalSubir();
          if (plan?.id) {
            this.cargarAsignaturasYProgramas(plan.id);
          }
        },
        error: (err) => {
          const msg = err.error?.error || err.error?.mensaje || err.error?.message || err.message;
          this.notif.mostrar(this.errorMessage, 'Error al subir programa: ' + msg, 6000);
          this.subiendo.set(false);
        },
      });
  }

  descargarPdf(programa: ProgramaAnaliticoAsignatura): void {
    if (!programa.id) return;
    window.open(this.programaService.descargarUrl(programa.id), '_blank');
  }

  validarYCompletar84(): void {
    const plan = this.planActivo();
    if (!plan?.id || this.asignaturas().length === 0) {
      this.notif.mostrar(
        this.errorMessage,
        'Primero debe cargar las asignaturas del plan de estudios en "Gestionar información de planes de estudio" (FT-001).',
        6000
      );
      return;
    }

    if (this.programasCargadosCount() < this.asignaturas().length) {
      this.notif.mostrar(
        this.errorMessage,
        `No se puede completar la actividad 8.4: faltan programas analíticos por cargar (${this.programasCargadosCount()} de ${this.asignaturas().length} cargados).`,
        6000
      );
      return;
    }

    const effectiveProcesoId = this.contextService.procesoIdActivo() || this.procesoId() || plan.procesoId || undefined;

    this.programaService.validarYCompletarActividad84(effectiveProcesoId, plan.id).subscribe({
      next: (resp) => {
        this.notif.mostrar(this.successMessage, resp.mensaje, 6000);
      },
      error: (err) => {
        const msg = err.error?.error || err.error?.mensaje || err.error?.message || 'Error al validar actividad 8.4';
        this.notif.mostrar(this.errorMessage, msg, 6000);
      },
    });
  }

  // --- Modal Gestión de Asignaturas (8.3 / FT-001) ---
  abrirModalNuevaAsignatura(): void {
    this.asignaturaEnEdicion.set(null);
    this.correlativasSeleccionadas.set([]);
    const total = this.asignaturas().length;
    this.formAsignatura.reset({
      codigo: `ASIG-${String(total + 1).padStart(2, '0')}`,
      nombre: '',
      numeroOrden: total + 1,
      grupo: 'C.B.I.',
      subgrupo: '1º',
      horasReloj: 64,
      horasPresenciales: 64,
      horasNoPresenciales: 0,
      esObligatoria: true,
    });
    this.modalAsignaturaAbierto.set(true);
  }

  abrirModalEditarAsignatura(asig: ActividadCurricularPlan): void {
    this.asignaturaEnEdicion.set(asig);
    const corrArray = (asig.correlativas || '')
      .split(/[,;]/)
      .map((s) => s.trim())
      .filter((s) => s && s.toLowerCase() !== 'sin correlativas previas' && s.toLowerCase() !== 'sin correlatividades previas');
    this.correlativasSeleccionadas.set(corrArray);

    this.formAsignatura.patchValue({
      codigo: asig.codigo || '',
      nombre: asig.nombre || '',
      numeroOrden: asig.numeroOrden ?? null,
      grupo: asig.grupo || 'C.B.I.',
      subgrupo: asig.subgrupo || '1º',
      horasReloj: asig.horasReloj || 64,
      horasPresenciales: asig.horasPresenciales || asig.horasReloj || 64,
      horasNoPresenciales: asig.horasNoPresenciales || 0,
      esObligatoria: asig.esObligatoria ?? true,
    });
    this.modalAsignaturaAbierto.set(true);
  }

  cerrarModalAsignatura(): void {
    this.modalAsignaturaAbierto.set(false);
    this.asignaturaEnEdicion.set(null);
    this.correlativasSeleccionadas.set([]);
    this.formAsignatura.reset();
  }

  toggleCorrelativa(nombre: string): void {
    const current = this.correlativasSeleccionadas();
    if (current.includes(nombre)) {
      this.correlativasSeleccionadas.set(current.filter((c) => c !== nombre));
    } else {
      this.correlativasSeleccionadas.set([...current, nombre]);
    }
  }

  guardarAsignatura(): void {
    if (this.formAsignatura.invalid) {
      this.formAsignatura.markAllAsTouched();
      return;
    }

    this.guardandoAsignatura.set(true);
    const val = this.formAsignatura.value;
    const correlativasStr =
      this.asignaturasDisponiblesCorrelativas().length > 0 && this.correlativasSeleccionadas().length > 0
        ? this.correlativasSeleccionadas().join(', ')
        : 'Sin correlativas previas';

    const editando = this.asignaturaEnEdicion();
    const plan = this.planActivo();

    if (!plan?.id) {
      const cId = this.carreraSeleccionadaId() || this.contextService.carreraSeleccionadaId();
      if (!cId) {
        this.guardandoAsignatura.set(false);
        this.notif.mostrar(this.errorMessage, 'Debe seleccionar una carrera de grado para registrar la asignatura.', 6000);
        return;
      }
      const pId = this.procesoId() || this.contextService.procesoIdActivo() || undefined;

      const nuevoPlanDto: PlanEstudioFt001 = {
        carreraId: cId,
        procesoId: pId,
        codigoResolucion: 'RES-CS-INICIAL/2026',
        anioAprobacion: 2026,
        anioInicio: 2026,
        duracionAnios: 5,
        activo: true,
        horasPresenciales: 0,
        horasNoPresenciales: 0,
        horasTotales: 0,
      };

      this.planService.crearPlan(nuevoPlanDto).subscribe({
        next: (nuevoPlan) => {
          this.planActivo.set(nuevoPlan);
          this.ejecutarGuardadoAsignatura(nuevoPlan.id!, editando, val, correlativasStr);
        },
        error: (err) => {
          this.guardandoAsignatura.set(false);
          this.notif.mostrar(
            this.errorMessage,
            'Error al auto-inicializar plan de estudios: ' + (err.error?.message || err.message),
            6000
          );
        },
      });
      return;
    }

    this.ejecutarGuardadoAsignatura(plan.id, editando, val, correlativasStr);
  }

  private ejecutarGuardadoAsignatura(
    planId: string,
    editando: ActividadCurricularPlan | null,
    val: any,
    correlativasStr: string
  ): void {
    if (editando?.id) {
      const req: ActualizarAsignaturaRequest = {
        id: editando.id,
        codigo: val.codigo || '',
        nombre: val.nombre || '',
        numeroOrden: val.numeroOrden ?? editando.numeroOrden,
        grupo: val.grupo || 'C.B.I.',
        subgrupo: val.subgrupo || '1º',
        horasReloj: Number(val.horasReloj) || 0,
        horasPresenciales: Number(val.horasPresenciales) || 0,
        horasNoPresenciales: Number(val.horasNoPresenciales) || 0,
        correlativas: correlativasStr,
        esObligatoria: val.esObligatoria ?? true,
      };

      this.planService.actualizarAsignatura(planId, editando.id, req).subscribe({
        next: () => {
          this.guardandoAsignatura.set(false);
          this.cerrarModalAsignatura();
          this.notif.mostrar(this.successMessage, `Asignatura "${req.nombre}" actualizada exitosamente.`, 6000);
          this.cargarAsignaturasYProgramas(planId);
        },
        error: (err) => {
          this.guardandoAsignatura.set(false);
          this.notif.mostrar(
            this.errorMessage,
            'Error al actualizar asignatura: ' + (err.error?.message || err.error?.error || err.message),
            6000
          );
        },
      });
    } else {
      const req: CrearAsignaturaRequest = {
        codigo: val.codigo || '',
        nombre: val.nombre || '',
        numeroOrden: val.numeroOrden ?? undefined,
        grupo: val.grupo || 'C.B.I.',
        subgrupo: val.subgrupo || '1º',
        horasReloj: Number(val.horasReloj) || 0,
        horasPresenciales: Number(val.horasPresenciales) || 0,
        horasNoPresenciales: Number(val.horasNoPresenciales) || 0,
        correlativas: correlativasStr,
        esObligatoria: val.esObligatoria ?? true,
      };

      this.planService.crearAsignatura(planId, req).subscribe({
        next: () => {
          this.guardandoAsignatura.set(false);
          this.cerrarModalAsignatura();
          this.notif.mostrar(this.successMessage, `Asignatura "${req.nombre}" creada exitosamente.`, 6000);
          this.cargarAsignaturasYProgramas(planId);
        },
        error: (err) => {
          this.guardandoAsignatura.set(false);
          this.notif.mostrar(
            this.errorMessage,
            'Error al crear asignatura: ' + (err.error?.message || err.error?.error || err.message),
            6000
          );
        },
      });
    }
  }

  eliminarAsignatura(asig: ActividadCurricularPlan): void {
    if (!asig.id) return;
    const plan = this.planActivo();
    if (!plan?.id) return;

    const confirmado = window.confirm(`¿Está seguro de eliminar la asignatura "${asig.nombre}"?`);
    if (!confirmado) return;

    this.deletingAsignaturaId.set(asig.id);
    this.planService.eliminarAsignatura(plan.id, asig.id).subscribe({
      next: () => {
        this.deletingAsignaturaId.set(null);
        this.notif.mostrar(this.successMessage, `Asignatura "${asig.nombre}" eliminada exitosamente.`, 6000);
        this.cargarAsignaturasYProgramas(plan.id!);
      },
      error: (err) => {
        this.deletingAsignaturaId.set(null);
        const msg =
          err.error?.error ||
          err.error?.message ||
          'No se puede eliminar la asignatura porque está vinculada a ámbitos de práctica o es correlativa de otra.';
        this.notif.mostrar(this.errorMessage, msg, 6000);
      },
    });
  }
}
