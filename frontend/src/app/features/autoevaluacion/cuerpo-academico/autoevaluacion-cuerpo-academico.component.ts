import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { AutoevaluacionContextService } from '../../../core/autoevaluacion/autoevaluacion-context.service';
import { AutoevaluacionService } from '../../../core/autoevaluacion/autoevaluacion.service';
import {
  ActividadCurricularPlan,
  PlanEstudioFt001,
} from '../../../core/planes-estudio/plan-estudio.models';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import {
  CrearAsignacionCatedraRequest,
  CrearCuerpoAcademicoMaestroRequest,
  CuerpoAcademicoMaestro,
} from '../../../core/services/datos-centrales.service';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';

@Component({
  selector: 'app-autoevaluacion-cuerpo-academico',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, SubnavAutoevaluacionComponent],
  template: `
    <div class="acad-wrap">
      <div style="margin-bottom: 12px;">
        <a
          routerLink="/dashboard"
          style="display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; font-size: 13px; font-weight: 500; color: #4b5563; background: #fff; border: 1px solid #d1d5db; border-radius: 6px; text-decoration: none;"
        >
          &larr; Volver al Inicio / Tablero
        </a>
      </div>

      <app-subnav-autoevaluacion seccionActual="Cuerpo académico"></app-subnav-autoevaluacion>

      <div class="info-banner">
        <strong>Copia aislada de Autoevaluación (RG-010):</strong> Los docentes, designaciones y asignaciones a cátedras que agregue, edite o elimine aquí corresponden únicamente a este proceso de autoevaluación y no alteran el padrón troncal de Gestión Académica.
      </div>

      @if (errorMessage()) {
        <div class="alert error"><span>{{ errorMessage() }}</span><button type="button" (click)="errorMessage.set(null)">✕</button></div>
      }
      @if (successMessage()) {
        <div class="alert success"><span>{{ successMessage() }}</span><button type="button" (click)="successMessage.set(null)">✕</button></div>
      }

      <section class="panel">
        <div class="bar">
          <div class="filters">
            <label class="inline-label">Carrera Convocada:
              <select [ngModel]="carreraId()" (ngModelChange)="onCarreraSelect($event)">
                @for (c of carrerasConvocadas(); track c.id) {
                  <option [value]="c.id">{{ c.nombre }}</option>
                }
              </select>
            </label>
            <input
              type="text"
              [ngModel]="busqueda()"
              (ngModelChange)="busqueda.set($event)"
              (keyup.enter)="cargarDocentes()"
              placeholder="Buscar DNI o Apellido..."
            />
            <button type="button" class="btn-sec" (click)="cargarDocentes()">Filtrar</button>
          </div>

          <div class="acciones">
            @if (canEdit() && procesoId()) {
              <button type="button" class="btn-copy" [disabled]="copiando()" (click)="copiarDesdeAcademica()">
                {{ copiando() ? 'Copiando...' : 'Copiar Cuerpo Académico desde Académica' }}
              </button>
              <button type="button" class="btn-pri" (click)="abrirModalDocente()">
                + Nuevo Docente (en esta Autoevaluación)
              </button>
            }
          </div>
        </div>

        @if (!procesoId()) {
          <div class="empty">Seleccione una convocatoria y carrera con proceso activo para gestionar su cuerpo académico.</div>
        } @else {
          <table class="tabla">
            <thead>
              <tr>
                <th>DNI</th>
                <th>Apellido y Nombre</th>
                <th>Categoría</th>
                <th>Carácter</th>
                <th>Dedicación</th>
                <th>Asignaciones</th>
                @if (canEdit()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (d of docentes(); track d.id) {
                <tr>
                  <td>{{ d.dni }}</td>
                  <td><strong>{{ d.apellido }}, {{ d.nombre }}</strong></td>
                  <td>{{ d.categoriaDocente || '—' }}</td>
                  <td>{{ d.caracterDesignacion || '—' }}</td>
                  <td>{{ d.dedicacionDocente || '—' }} ({{ d.horasSemanales ?? 0 }}h)</td>
                  <td>
                    <button type="button" class="btn-link" (click)="abrirModalAsignaciones(d)">
                      Gestionar ({{ d.asignaciones?.length ?? 0 }})
                    </button>
                  </td>
                  @if (canEdit()) {
                    <td class="acciones">
                      <button type="button" class="btn-sec" (click)="abrirModalDocente(d)">Editar</button>
                      <button type="button" class="btn-danger-xs" (click)="eliminarDocente(d)">Eliminar</button>
                    </td>
                  }
                </tr>
              } @empty {
                <tr>
                  <td [attr.colspan]="canEdit() ? 7 : 6" class="empty">
                    No hay docentes copiados en esta autoevaluación. Use <strong>Copiar Cuerpo Académico desde Académica</strong> para inicializar la copia aislada.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>

      @if (modalDocente()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ docenteEditando() ? 'Editar Docente (Copia Autoevaluación)' : 'Nuevo Docente en esta Autoevaluación' }}</h3>
            <form [formGroup]="docenteForm" (ngSubmit)="guardarDocente()">
              <div class="grid2">
                <label>DNI *<input type="text" formControlName="dni" /></label>
                <label>CUIT<input type="text" formControlName="cuit" /></label>
              </div>
              <div class="grid2">
                <label>Apellido *<input type="text" formControlName="apellido" /></label>
                <label>Nombre *<input type="text" formControlName="nombre" /></label>
              </div>
              <div class="grid2">
                <label>Email Institucional<input type="email" formControlName="emailInstitucional" /></label>
                <label>Título de Grado<input type="text" formControlName="tituloGrado" /></label>
              </div>
              <div class="grid2">
                <label>Categoría Docente *
                  <select formControlName="categoriaDocente">
                    <option value="TITULAR">Titular</option>
                    <option value="ASOCIADO">Asociado</option>
                    <option value="ADJUNTO">Adjunto</option>
                    <option value="JTP">Jefe de Trabajos Prácticos</option>
                    <option value="AYUDANTE_PRIMERA">Ayudante de Primera</option>
                  </select>
                </label>
                <label>Carácter *
                  <select formControlName="caracterDesignacion">
                    <option value="REGULAR">Regular / Concursado</option>
                    <option value="INTERINO">Interino</option>
                    <option value="CONTRATADO">Contratado</option>
                  </select>
                </label>
              </div>
              <div class="grid2">
                <label>Dedicación *
                  <select formControlName="dedicacionDocente">
                    <option value="EXCLUSIVA">Exclusiva</option>
                    <option value="SEMIEXCLUSIVA">Semiexclusiva</option>
                    <option value="SIMPLE">Simple</option>
                  </select>
                </label>
                <label>Horas Semanales<input type="number" formControlName="horasSemanales" /></label>
              </div>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalDocente.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="docenteForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (docenteAsignando(); as doc) {
        <div class="modal-bg">
          <div class="modal-box modal-lg">
            <h3>Asignaciones a Cátedras — {{ doc.apellido }}, {{ doc.nombre }}</h3>
            <ul class="asig-list">
              @for (a of doc.asignaciones ?? []; track a.id) {
                <li>
                  <span>
                    <strong>{{ a.actividadCurricularNombre || 'Actividad' }}</strong>
                    ({{ a.carreraNombre || 'Carrera' }} · {{ a.planEstudioNombre || 'Plan' }}) —
                    {{ a.rolEnCatedra || 'Docente' }} ({{ a.cargaHorariaSemanal ?? 0 }}h/sem)
                  </span>
                  @if (canEdit() && a.id) {
                    <button type="button" class="btn-danger-xs" (click)="quitarAsignacion(a.id!)">Quitar</button>
                  }
                </li>
              } @empty { <li class="empty">Sin asignaciones registradas en esta autoevaluación.</li> }
            </ul>

            @if (canEdit()) {
              <div class="asig-form">
                <h4>Agregar Asignación (sobre Plan copiado del proceso)</h4>
                <div class="grid2">
                  <label>Plan de Estudio
                    <select [ngModel]="asigPlanId()" (ngModelChange)="onAsigPlanChange($event)">
                      <option value="">Seleccione...</option>
                      @for (p of asigPlanes(); track p.id) {
                        <option [value]="p.id">{{ p.nombrePlan || p.codigoResolucion || ('Plan ' + p.anioAprobacion) }}</option>
                      }
                    </select>
                  </label>
                  <label>Actividad Curricular
                    <select [ngModel]="asigActId()" (ngModelChange)="asigActId.set($event)">
                      <option value="">Seleccione...</option>
                      @for (a of asigActividades(); track a.id) { <option [value]="a.id">{{ a.nombre }}</option> }
                    </select>
                  </label>
                </div>
                <div class="grid2">
                  <label>Rol en Cátedra
                    <select [ngModel]="asigRol()" (ngModelChange)="asigRol.set($event)">
                      <option value="PROFESOR_A_CARGO">Profesor a Cargo</option>
                      <option value="ADJUNTO">Profesor Adjunto</option>
                      <option value="JTP">Jefe de Trabajos Prácticos</option>
                      <option value="AUXILIAR">Auxiliar Docente</option>
                    </select>
                  </label>
                  <label>Carga Horaria Semanal (h)
                    <input type="number" [ngModel]="asigHoras()" (ngModelChange)="asigHoras.set($event)" />
                  </label>
                </div>
                <button type="button" class="btn-pri" (click)="agregarAsignacion()" [disabled]="!asigPlanId() || !asigActId()">+ Asignar Cátedra</button>
              </div>
            }

            <div class="m-actions">
              <button type="button" class="btn-sec" (click)="docenteAsignando.set(null)">Cerrar</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .acad-wrap { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .info-banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 12px 16px; border-radius: 8px; margin-bottom: 14px; font-size: 13.5px; }
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
    .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
    .filters { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
    .inline-label { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; margin: 0; flex-direction: row; }
    .filters select, .filters input { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13.5px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla th, .tabla td { padding: 9px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; }
    .acciones { display: flex; gap: 6px; flex-wrap: wrap; }
    .btn-pri { background: #6b2442; color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 13.5px; }
    .btn-copy { background: #fdf2f8; color: #9d174d; border: 1px solid #fbcfe8; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 13.5px; }
    .btn-sec { background: #f3f4f6; color: #1f2937; border: 1px solid #d1d5db; padding: 6px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .btn-link { background: none; border: none; color: #6b2442; text-decoration: underline; cursor: pointer; font-weight: 600; padding: 0; }
    .btn-danger-xs { background: #fee2e2; color: #991b1b; border: none; padding: 5px 9px; border-radius: 4px; cursor: pointer; font-size: 12.5px; }
    .empty { text-align: center; padding: 24px; color: #6b7280; }
    .alert { display: flex; justify-content: space-between; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px; }
    .alert.error { background: #fef2f2; color: #991b1b; }
    .alert.success { background: #ecfdf5; color: #065f46; }
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 620px; max-height: 90vh; overflow-y: auto; }
    .modal-box.modal-lg { max-width: 760px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
    input, select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
    .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .asig-list { list-style: none; padding: 0; margin: 0 0 14px; display: flex; flex-direction: column; gap: 6px; }
    .asig-list li { display: flex; justify-content: space-between; align-items: center; padding: 8px; background: #f9fafb; border-radius: 6px; font-size: 13px; }
    .asig-form { border-top: 1px solid #e5e7eb; padding-top: 12px; margin-top: 12px; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AutoevaluacionCuerpoAcademicoComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly contextService = inject(AutoevaluacionContextService);
  private readonly autoevalService = inject(AutoevaluacionService);
  private readonly planService = inject(PlanEstudioService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly carrerasConvocadas = computed(() => this.contextService.carrerasDisponibles());
  readonly carreraId = signal<string>('');
  readonly procesoId = computed<string | null>(() => {
    const carId = this.carreraId();
    const detalle = this.contextService.convocatoriaDetalle();
    const match = detalle?.carrerasPropuestas?.find((cp) => cp.carreraId === carId);
    return match?.procesoId || this.contextService.procesoIdActivo();
  });

  readonly docentes = signal<CuerpoAcademicoMaestro[]>([]);
  readonly busqueda = signal('');
  readonly copiando = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalDocente = signal(false);
  readonly docenteEditando = signal<CuerpoAcademicoMaestro | null>(null);
  readonly docenteAsignando = signal<CuerpoAcademicoMaestro | null>(null);

  readonly asigPlanId = signal('');
  readonly asigActId = signal('');
  readonly asigRol = signal('PROFESOR_A_CARGO');
  readonly asigHoras = signal(4);
  readonly asigPlanes = signal<PlanEstudioFt001[]>([]);
  readonly asigActividades = signal<ActividadCurricularPlan[]>([]);

  readonly canEdit = computed(() =>
    this.auth.hasAnyRole(
      'AEA',
      'DIRECTOR_ESCUELA',
      'DIRECTOR_CARRERA',
      'CAREER_DIRECTOR',
      'COMISION_AUTOEVALUACION'
    )
  );

  readonly docenteForm = this.fb.group({
    dni: ['', Validators.required],
    cuit: [''],
    apellido: ['', Validators.required],
    nombre: ['', Validators.required],
    emailInstitucional: [''],
    tituloGrado: [''],
    categoriaDocente: ['ADJUNTO', Validators.required],
    caracterDesignacion: ['REGULAR', Validators.required],
    dedicacionDocente: ['SIMPLE', Validators.required],
    horasSemanales: [10, [Validators.required, Validators.min(1)]],
  });

  private ultimoProcesoCargado = '';

  constructor() {
    effect(() => {
      const disp = this.carrerasConvocadas();
      if (disp.length > 0 && (!this.carreraId() || !disp.some((c) => c.id === this.carreraId()))) {
        this.carreraId.set(this.contextService.carreraSeleccionadaId() || disp[0].id);
      }
      const pId = this.procesoId();
      if (pId && pId !== this.ultimoProcesoCargado) {
        this.ultimoProcesoCargado = pId;
        this.cargarDocentes();
      }
    });
  }

  ngOnInit(): void {
    const pId = this.procesoId();
    if (pId) {
      this.ultimoProcesoCargado = pId;
      this.cargarDocentes();
    }
  }

  onCarreraSelect(carId: string): void {
    this.carreraId.set(carId);
    this.contextService.seleccionarCarrera(carId);
  }

  cargarDocentes(): void {
    const pId = this.procesoId();
    if (!pId) {
      this.docentes.set([]);
      return;
    }
    let params = new HttpParams();
    if (this.busqueda().trim()) {
      params = params.set('query', this.busqueda().trim());
    }
    this.http
      .get<CuerpoAcademicoMaestro[]>(`/api/autoevaluacion/procesos/${pId}/cuerpo-academico`, { params })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ds) => this.docentes.set(ds || []),
        error: (err) => this.errorMessage.set('Error al cargar cuerpo académico del proceso: ' + (err.error?.message || err.message)),
      });
  }

  copiarDesdeAcademica(): void {
    const pId = this.procesoId();
    if (!pId) return;
    this.copiando.set(true);
    this.autoevalService
      .copiarDatosCentrales(pId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.copiando.set(false);
          this.successMessage.set(
            `Copia sincronizada (${res.cuerpoAcademicoClonado} docente(s) y ${res.planesClonados} plan(es) clonados en forma aislada).`
          );
          this.cargarDocentes();
        },
        error: (err) => {
          this.copiando.set(false);
          this.errorMessage.set('Error al copiar desde Académica: ' + (err.error?.message || err.message));
        },
      });
  }

  abrirModalDocente(d?: CuerpoAcademicoMaestro): void {
    this.docenteEditando.set(d || null);
    this.docenteForm.reset({
      dni: d?.dni || '',
      cuit: d?.cuit || '',
      apellido: d?.apellido || '',
      nombre: d?.nombre || '',
      emailInstitucional: d?.emailInstitucional || '',
      tituloGrado: d?.tituloGrado || '',
      categoriaDocente: d?.categoriaDocente || 'ADJUNTO',
      caracterDesignacion: d?.caracterDesignacion || 'REGULAR',
      dedicacionDocente: d?.dedicacionDocente || 'SIMPLE',
      horasSemanales: d?.horasSemanales ?? 10,
    });
    this.modalDocente.set(true);
  }

  guardarDocente(): void {
    const pId = this.procesoId();
    if (this.docenteForm.invalid || !pId) return;
    const v = this.docenteForm.value;
    const req: CrearCuerpoAcademicoMaestroRequest = {
      dni: v.dni!,
      cuit: v.cuit || undefined,
      apellido: v.apellido!,
      nombre: v.nombre!,
      emailInstitucional: v.emailInstitucional || undefined,
      tituloGrado: v.tituloGrado || undefined,
      categoriaDocente: v.categoriaDocente!,
      caracterDesignacion: v.caracterDesignacion!,
      dedicacionDocente: v.dedicacionDocente!,
      horasSemanales: Number(v.horasSemanales) || 10,
    };
    const edit = this.docenteEditando();
    const url = `/api/autoevaluacion/procesos/${pId}/cuerpo-academico`;
    const obs = edit
      ? this.http.put<CuerpoAcademicoMaestro>(`${url}/${edit.id}`, req)
      : this.http.post<CuerpoAcademicoMaestro>(url, req);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalDocente.set(false);
        this.successMessage.set('Docente guardado en la copia de autoevaluación.');
        this.cargarDocentes();
      },
      error: (err) => this.errorMessage.set('Error al guardar docente: ' + (err.error?.message || err.message)),
    });
  }

  eliminarDocente(d: CuerpoAcademicoMaestro): void {
    const pId = this.procesoId();
    if (!pId || !confirm(`¿Quitar a "${d.apellido}, ${d.nombre}" de esta autoevaluación? (No afecta el padrón troncal)`)) return;
    this.http
      .delete<void>(`/api/autoevaluacion/procesos/${pId}/cuerpo-academico/${d.id}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set('Docente quitado de la autoevaluación.');
          this.cargarDocentes();
        },
        error: (err) => this.errorMessage.set('Error al eliminar docente: ' + (err.error?.message || err.message)),
      });
  }

  abrirModalAsignaciones(d: CuerpoAcademicoMaestro): void {
    this.docenteAsignando.set(d);
    this.asigPlanId.set('');
    this.asigActId.set('');
    this.asigActividades.set([]);
    const carId = this.carreraId();
    const pId = this.procesoId() || undefined;
    if (carId && pId) {
      this.planService
        .listarPlanes(carId, pId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (ps) => this.asigPlanes.set(ps || []),
          error: () => this.asigPlanes.set([]),
        });
    } else {
      this.asigPlanes.set([]);
    }
  }

  onAsigPlanChange(planId: string): void {
    this.asigPlanId.set(planId);
    this.asigActId.set('');
    if (!planId) {
      this.asigActividades.set([]);
      return;
    }
    this.planService
      .listarAsignaturas(planId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (as) => this.asigActividades.set(as || []),
        error: () => this.asigActividades.set([]),
      });
  }

  agregarAsignacion(): void {
    const pId = this.procesoId();
    const d = this.docenteAsignando();
    if (!pId || !d || !this.asigPlanId() || !this.asigActId()) return;
    const req: CrearAsignacionCatedraRequest = {
      carreraId: this.carreraId() || undefined,
      planEstudioId: this.asigPlanId(),
      actividadCurricularId: this.asigActId(),
      rolEnCatedra: this.asigRol(),
      cargaHorariaSemanal: Number(this.asigHoras()) || 4,
    };
    this.http
      .post<CuerpoAcademicoMaestro>(`/api/autoevaluacion/procesos/${pId}/cuerpo-academico/${d.id}/asignaciones`, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (upd) => {
          this.docenteAsignando.set(upd);
          this.successMessage.set('Asignación agregada en la autoevaluación.');
          this.cargarDocentes();
        },
        error: (err) => this.errorMessage.set('Error al asignar cátedra: ' + (err.error?.message || err.message)),
      });
  }

  quitarAsignacion(asignacionId: string): void {
    const pId = this.procesoId();
    const d = this.docenteAsignando();
    if (!pId || !d) return;
    this.http
      .delete<CuerpoAcademicoMaestro>(`/api/autoevaluacion/procesos/${pId}/cuerpo-academico/${d.id}/asignaciones/${asignacionId}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (upd) => {
          this.docenteAsignando.set(upd);
          this.successMessage.set('Asignación eliminada de la autoevaluación.');
          this.cargarDocentes();
        },
        error: (err) => this.errorMessage.set('Error al quitar asignación: ' + (err.error?.message || err.message)),
      });
  }
}
