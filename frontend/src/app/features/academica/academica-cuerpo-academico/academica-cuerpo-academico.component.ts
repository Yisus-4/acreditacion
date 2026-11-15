import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import { EstructuraService, CarreraResponse } from '../../../core/estructura/estructura.service';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import { ActividadCurricularPlan, PlanEstudioFt001 } from '../../../core/planes-estudio/plan-estudio.models';
import {
  CuerpoAcademicoMaestro,
  DatosCentralesService,
} from '../../../core/services/datos-centrales.service';
import { SubnavAcademicaComponent } from '../subnav-academica/subnav-academica.component';

@Component({
  selector: 'app-academica-cuerpo-academico',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SubnavAcademicaComponent],
  template: `
    <div class="acad-wrap">
      <app-subnav-academica seccionActual="Cuerpo Académico Troncal"></app-subnav-academica>

      @if (errorMessage()) {
        <div class="alert error"><span>{{ errorMessage() }}</span><button type="button" (click)="errorMessage.set(null)">✕</button></div>
      }
      @if (successMessage()) {
        <div class="alert success"><span>{{ successMessage() }}</span><button type="button" (click)="successMessage.set(null)">✕</button></div>
      }

      <section class="panel">
        <div class="bar">
          <div class="filters">
            <select [ngModel]="carreraId()" (ngModelChange)="carreraId.set($event); cargarDocentes()">
              <option value="">Todas las Carreras (Padrón Troncal)</option>
              @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
            </select>
            <input type="text" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" (keyup.enter)="cargarDocentes()" placeholder="Buscar DNI o Apellido..." />
            <button type="button" class="btn-sec" (click)="cargarDocentes()">Filtrar</button>
          </div>
          @if (canEdit()) {
            <button type="button" class="btn-pri" (click)="abrirModalDocente()">+ Nueva Designación Docente</button>
          }
        </div>

        <table class="tabla">
          <thead>
            <tr>
              <th>DNI</th><th>Apellido y Nombre</th><th>Categoría</th><th>Carácter</th><th>Dedicación</th><th>Asignaciones</th>@if (canEdit()) { <th>Acciones</th> }
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
                    <button type="button" class="btn-sec" (click)="abrirModalDocente(d)">Editar Designación</button>
                  </td>
                }
              </tr>
            } @empty { <tr><td [attr.colspan]="canEdit() ? 7 : 6" class="empty">Sin docentes registrados.</td></tr> }
          </tbody>
        </table>
      </section>

      @if (modalDocente()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ docenteEditando() ? 'Editar Designación Docente' : 'Nueva Designación Docente (Troncal)' }}</h3>
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
                    <option value="AYUDANTE">Ayudante</option>
                  </select>
                </label>
                <label>Carácter Designación *
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

      @if (docenteAsignando()) {
        <div class="modal-bg">
          <div class="modal-box modal-lg">
            <h3>Asignaciones a Cátedras — {{ docenteAsignando()?.apellido }}, {{ docenteAsignando()?.nombre }}</h3>
            <ul class="asig-list">
              @for (a of docenteAsignando()?.asignaciones; track a.id) {
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
              } @empty { <li class="empty">Sin asignaciones registradas.</li> }
            </ul>

            @if (canEdit()) {
              <div class="asig-form">
                <h4>Agregar Asignación</h4>
                <div class="grid3">
                  <label>Carrera
                    <select [ngModel]="asigCarreraId()" (ngModelChange)="onAsigCarreraChange($event)">
                      <option value="">Seleccione...</option>
                      @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
                    </select>
                  </label>
                  <label>Plan de Estudio
                    <select [ngModel]="asigPlanId()" (ngModelChange)="onAsigPlanChange($event)">
                      <option value="">Seleccione...</option>
                      @for (p of asigPlanes(); track p.id) { <option [value]="p.id">{{ p.codigoResolucion || p.anioAprobacion }}</option> }
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
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
    .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
    .filters { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
    .filters select, .filters input { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla th, .tabla td { padding: 9px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; }
    .acciones { display: flex; gap: 6px; }
    .btn-pri { background: #6b2442; color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-sec { background: #f3f4f6; color: #1f2937; border: 1px solid #d1d5db; padding: 6px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .btn-link { background: none; border: none; color: #6b2442; text-decoration: underline; cursor: pointer; font-weight: 600; padding: 0; }
    .btn-danger-xs { background: #fee2e2; color: #991b1b; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; }
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
    .grid3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
    .asig-list { list-style: none; padding: 0; margin: 0 0 14px; display: flex; flex-direction: column; gap: 6px; }
    .asig-list li { display: flex; justify-content: space-between; align-items: center; padding: 8px; background: #f9fafb; border-radius: 6px; font-size: 13px; }
    .asig-form { border-top: 1px solid #e5e7eb; padding-top: 12px; margin-top: 12px; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicaCuerpoAcademicoComponent implements OnInit {
  private readonly datos = inject(DatosCentralesService);
  private readonly estructura = inject(EstructuraService);
  private readonly planService = inject(PlanEstudioService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly docentes = signal<CuerpoAcademicoMaestro[]>([]);
  readonly carreraId = signal('');
  readonly busqueda = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalDocente = signal(false);
  readonly docenteEditando = signal<CuerpoAcademicoMaestro | null>(null);
  readonly docenteAsignando = signal<CuerpoAcademicoMaestro | null>(null);

  readonly asigCarreraId = signal('');
  readonly asigPlanId = signal('');
  readonly asigActId = signal('');
  readonly asigRol = signal('PROFESOR_A_CARGO');
  readonly asigHoras = signal(4);
  readonly asigPlanes = signal<PlanEstudioFt001[]>([]);
  readonly asigActividades = signal<ActividadCurricularPlan[]>([]);

  readonly canEdit = computed(() => {
    const roles = this.auth.currentSession?.user?.roleCodes ?? [];
    return roles.some((r) => r === 'ADMIN' || r === 'ADMINISTRATOR' || r === 'AEA');
  });

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

  ngOnInit(): void {
    this.estructura.listarCarreras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((cs) => this.carreras.set(cs));
    this.cargarDocentes();
  }

  cargarDocentes(): void {
    this.datos.getCuerpoAcademico(this.carreraId() || undefined, this.busqueda() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((ds) => this.docentes.set(ds));
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
    if (this.docenteForm.invalid) return;
    const v = this.docenteForm.value;
    const req = {
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
    const obs = edit
      ? this.datos.actualizarCuerpoAcademico(edit.id, req)
      : this.datos.crearCuerpoAcademico(req);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalDocente.set(false);
        this.successMessage.set('Designación docente guardada.');
        this.cargarDocentes();
      },
      error: (err) => this.errorMessage.set('Error al guardar docente: ' + (err.error?.message || err.message)),
    });
  }

  abrirModalAsignaciones(d: CuerpoAcademicoMaestro): void {
    this.docenteAsignando.set(d);
    this.asigCarreraId.set('');
    this.asigPlanId.set('');
    this.asigActId.set('');
    this.asigPlanes.set([]);
    this.asigActividades.set([]);
  }

  onAsigCarreraChange(carreraId: string): void {
    this.asigCarreraId.set(carreraId);
    this.asigPlanId.set('');
    this.asigActId.set('');
    this.asigActividades.set([]);
    if (!carreraId) {
      this.asigPlanes.set([]);
      return;
    }
    this.planService.listarPlanes(carreraId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ps) => this.asigPlanes.set(ps));
  }

  onAsigPlanChange(planId: string): void {
    this.asigPlanId.set(planId);
    this.asigActId.set('');
    if (!planId) {
      this.asigActividades.set([]);
      return;
    }
    this.planService.listarAsignaturas(planId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((as) => this.asigActividades.set(as));
  }

  agregarAsignacion(): void {
    const d = this.docenteAsignando();
    if (!d || !this.asigPlanId() || !this.asigActId()) return;
    this.datos.agregarAsignacionDocente(d.id, {
      carreraId: this.asigCarreraId() || undefined,
      planEstudioId: this.asigPlanId(),
      actividadCurricularId: this.asigActId(),
      rolEnCatedra: this.asigRol(),
      cargaHorariaSemanal: Number(this.asigHoras()) || 4,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (actualizado) => {
        this.docenteAsignando.set(actualizado);
        this.successMessage.set('Asignación a cátedra agregada.');
        this.cargarDocentes();
      },
      error: (err) => this.errorMessage.set('Error al asignar cátedra: ' + (err.error?.message || err.message)),
    });
  }

  quitarAsignacion(asignacionId: string): void {
    const d = this.docenteAsignando();
    if (!d) return;
    this.datos.eliminarAsignacionDocente(d.id, asignacionId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (actualizado) => {
        this.docenteAsignando.set(actualizado);
        this.successMessage.set('Asignación eliminada.');
        this.cargarDocentes();
      },
      error: (err) => this.errorMessage.set('Error al eliminar asignación: ' + (err.error?.message || err.message)),
    });
  }
}
