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
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import { ActividadCurricularPlan, PlanEstudioFt001 } from '../../../core/planes-estudio/plan-estudio.models';
import { ProgramaAnaliticoService } from '../../../core/programas-analiticos/programa-analitico.service';
import { ProgramaAnaliticoAsignatura } from '../../../core/programas-analiticos/programa-analitico.models';
import { EstructuraService } from '../../../core/estructura/estructura.service';
import { CarreraResponse } from '../../../core/estructura/estructura.types';

@Component({
  selector: 'app-estructura-curricular',
  standalone: true,
  imports: [
    CommonModule,
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
  private readonly fb = inject(FormBuilder);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly carreraSeleccionadaId = signal<string>('');
  readonly planes = signal<PlanEstudioFt001[]>([]);
  readonly planActivo = signal<PlanEstudioFt001 | null>(null);
  readonly asignaturas = signal<ActividadCurricularPlan[]>([]);
  readonly programas = signal<ProgramaAnaliticoAsignatura[]>([]);

  readonly tabPrincipal = signal<'PLAN_FT001' | 'PROGRAMAS_84'>('PLAN_FT001');
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Filtros
  readonly filtroAnio = signal<string>('TODOS');
  readonly filtroCiclo = signal<string>('TODOS');
  readonly searchQuery = signal<string>('');

  // Modal Subir Programa Analítico
  readonly modalSubirAbierto = signal<boolean>(false);
  readonly asignaturaSeleccionada = signal<ActividadCurricularPlan | null>(null);
  readonly archivoSeleccionado = signal<File | null>(null);
  readonly subiendo = signal<boolean>(false);

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

  obtenerPrograma(asignaturaId?: string): ProgramaAnaliticoAsignatura | undefined {
    if (!asignaturaId) return undefined;
    return this.programas().find((p) => p.asignaturaPlanId === asignaturaId);
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
