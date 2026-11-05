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
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import { ActividadCurricularPlan, PlanEstudioFt001 } from '../../../core/planes-estudio/plan-estudio.models';
import { ProgramaAnaliticoService } from '../../../core/programas-analiticos/programa-analitico.service';
import { ProgramaAnaliticoAsignatura } from '../../../core/programas-analiticos/programa-analitico.models';
import { EstructuraService } from '../../../core/estructura/estructura.service';
import { CarreraResponse } from '../../../core/estructura/estructura.types';

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
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

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
        ola: 'OLA 1 (Independiente)',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'GT_001' as const,
        nombre: 'Gestionar información sobre organización y gestión de la carrera',
        responsable: 'Dirección de Carrera',
        instrumento: 'Manual de Procedimientos (GT-001)',
        ola: 'OLA 1 (Independiente)',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'PLAN_FT001' as const,
        nombre: 'Gestionar información de planes de estudio',
        responsable: 'Secretaría Académica / Dirección de Carrera',
        instrumento: 'Resolución Ministerial / Ficha (FT-001)',
        ola: 'OLA 1 (Independiente)',
        completada: tienePlan,
        estado: tienePlan ? 'Completada' : 'Pendiente',
      },
      {
        id: 'PROGRAMAS_84' as const,
        nombre: 'Gestionar programas analíticos de las asignaturas',
        responsable: 'Docentes Titulares / Carrera / Comisión Curricular',
        instrumento: 'Programas Vigentes en PDF',
        ola: 'OLA 1 (Crítico - Insumo para Biblioteca)',
        completada: programasOk,
        estado: programasOk ? 'Completada' : `${this.programasCargadosCount()}/${this.asignaturas().length} cargados`,
      },
      {
        id: 'FT_003' as const,
        nombre: 'Gestionar información de las asignaturas',
        responsable: 'Secretaría Académica / Docentes',
        instrumento: 'Fichas FT-003',
        ola: 'OLA 3 (Cuello de Botella)',
        dependencia: 'Requiere Biblioteca RG-014 y Cuadros SGA',
        completada: false,
        estado: 'Pendiente',
      },
      {
        id: 'GT_006' as const,
        nombre: 'Gestionar información sobre comisiones de planificación y seguimiento de la carrera',
        responsable: 'Comisión de Seguimiento Curricular',
        instrumento: 'Resolución de Designación (GT-006)',
        ola: 'OLA 1 (Independiente)',
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

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const pId = params.get('procesoId') || params.get('convocatoriaId');
      if (pId) {
        this.procesoId.set(pId);
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
          const agri = data.find((c: CarreraResponse) => c.nombre.toLowerCase().includes('agrimensura'));
          const id = agri ? agri.id : data[0].id;
          this.carreraSeleccionadaId.set(id);
          this.cargarPlan(id);
        } else {
          this.loading.set(false);
        }
      },
      error: (err: any) => {
        this.errorMessage.set('Error al cargar carreras: ' + (err.error?.message || err.message));
        this.loading.set(false);
      },
    });
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.carreraSeleccionadaId.set(select.value);
    this.cargarPlan(select.value);
  }

  cargarPlan(carreraId: string): void {
    this.loading.set(true);
    this.planService.listarPlanes(carreraId).subscribe({
      next: (planes) => {
        this.planes.set(planes);
        if (planes.length > 0) {
          const p = planes[0];
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
        } else {
          this.planActivo.set(null);
          this.asignaturas.set([]);
          this.programas.set([]);
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.errorMessage.set('Error al cargar planes de estudio: ' + (err.error?.message || err.message));
        this.loading.set(false);
      },
    });
  }

  cargarAsignaturasYProgramas(planId: string): void {
    this.planService.listarAsignaturas(planId).subscribe({
      next: (asigs) => {
        this.asignaturas.set(asigs);
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
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  volverAlListado(): void {
    this.actividadSeleccionada.set(null);
    this.searchQuery.set('');
    this.errorMessage.set(null);
    this.successMessage.set(null);
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
        this.successMessage.set('Ficha de Plan de Estudios (FT-001) actualizada correctamente.');
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (err) => {
        this.errorMessage.set('Error al actualizar plan: ' + (err.error?.message || err.message));
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
      this.errorMessage.set('Debe seleccionar un archivo PDF para el programa analítico.');
      return;
    }

    if (this.formSubida.invalid) {
      this.formSubida.markAllAsTouched();
      return;
    }

    this.subiendo.set(true);
    const formVal = this.formSubida.value;
    const plan = this.planActivo();

    this.programaService
      .subirPrograma(
        archivo,
        asig.id,
        plan?.procesoId,
        formVal.resolucionAprobacion || undefined,
        formVal.vigenciaDesde || undefined,
        formVal.vigenciaHasta || undefined,
        formVal.observaciones || undefined
      )
      .subscribe({
        next: () => {
          this.successMessage.set(`Programa analítico de "${asig.nombre}" subido correctamente.`);
          this.subiendo.set(false);
          this.cerrarModalSubir();
          if (plan?.id) {
            this.cargarAsignaturasYProgramas(plan.id);
          }
          setTimeout(() => this.successMessage.set(null), 3000);
        },
        error: (err) => {
          this.errorMessage.set('Error al subir programa: ' + (err.error?.message || err.message));
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
    if (!plan?.id) return;

    this.programaService.validarYCompletarActividad84(plan.procesoId, plan.id).subscribe({
      next: (resp) => {
        this.successMessage.set(resp.mensaje);
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.mensaje || 'Error al validar actividad 8.4');
      },
    });
  }
}
