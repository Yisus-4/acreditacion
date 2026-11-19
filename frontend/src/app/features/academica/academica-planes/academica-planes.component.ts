import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import { EstructuraService, CarreraResponse } from '../../../core/estructura/estructura.service';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import {
  ActividadCurricularPlan,
  ActualizarAsignaturaRequest,
  CrearAsignaturaRequest,
  EstadoDictadoActividad,
} from '../../../core/planes-estudio/plan-estudio.models';
import {
  DatosCentralesService,
  PlanEstudioMaestro,
} from '../../../core/services/datos-centrales.service';
import { SubnavAcademicaComponent } from '../subnav-academica/subnav-academica.component';

@Component({
  selector: 'app-academica-planes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SubnavAcademicaComponent],
  template: `
    <div class="acad-wrap">
      <app-subnav-academica seccionActual="Planes y Actividades Curriculares"></app-subnav-academica>

      @if (errorMessage()) {
        <div class="alert error"><span>{{ errorMessage() }}</span><button type="button" (click)="errorMessage.set(null)">✕</button></div>
      }
      @if (successMessage()) {
        <div class="alert success"><span>{{ successMessage() }}</span><button type="button" (click)="successMessage.set(null)">✕</button></div>
      }

      <section class="panel">
        <div class="bar">
          <div class="filters">
            <select [ngModel]="carreraId()" (ngModelChange)="carreraId.set($event); cargarPlanes()">
              <option value="">Todas las Carreras</option>
              @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
            </select>
            <select [ngModel]="vigencia()" (ngModelChange)="vigencia.set($event); cargarPlanes()">
              <option value="">Todos los Estados</option>
              <option value="VIGENTE">Vigente</option>
              <option value="NO_VIGENTE">No Vigente</option>
            </select>
          </div>
          @if (canEdit()) {
            <button type="button" class="btn-pri" (click)="abrirModalPlan()">+ Nuevo Plan de Estudio</button>
          }
        </div>

        <table class="tabla">
          <thead>
            <tr>
              <th>Carrera</th><th>Plan</th><th>Año</th><th>Actividades</th><th>Estado</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (p of planes(); track p.id) {
              <tr [class.sel]="planSeleccionado()?.id === p.id">
                <td>{{ p.carreraNombre || 'Carrera' }}</td>
                <td><strong>{{ p.nombrePlan }}</strong></td>
                <td>{{ p.anioAprobacion || '—' }}</td>
                <td>{{ p.cantidadAsignaturas ?? 0 }}</td>
                <td>
                  <span class="badge" [class.vigente]="p.estadoVigencia === 'VIGENTE'" [class.no-vigente]="p.estadoVigencia !== 'VIGENTE'">
                    {{ p.estadoVigencia === 'VIGENTE' ? 'VIGENTE' : 'NO VIGENTE' }}
                  </span>
                </td>
                <td class="acciones">
                  <button type="button" class="btn-sec" (click)="seleccionarPlan(p)">Ver Actividades Curriculares</button>
                  @if (isAea()) {
                    <button type="button" class="btn-sec" (click)="abrirModalPlan(p)">Editar Plan</button>
                  }
                  @if (canEdit()) {
                    <button type="button" class="btn-xs" (click)="cambiarVigencia(p, p.estadoVigencia === 'VIGENTE' ? 'NO_VIGENTE' : 'VIGENTE')">
                      {{ p.estadoVigencia === 'VIGENTE' ? 'Marcar No Vigente' : 'Marcar Vigente' }}
                    </button>
                  }
                </td>
              </tr>
            } @empty { <tr><td colspan="6" class="empty">Sin planes registrados.</td></tr> }
          </tbody>
        </table>
      </section>

      @if (planSeleccionado()) {
        <section class="panel" style="margin-top: 20px;">
          <div class="bar">
            <div>
              <h3>Actividades Curriculares — {{ planSeleccionado()?.nombrePlan }} ({{ planSeleccionado()?.carreraNombre }})</h3>
              <p class="sub">Total: {{ asignaturas().length }} · Se dictan: {{ countSeDicta() }}</p>
            </div>
            @if (canEdit()) {
              <button type="button" class="btn-pri" (click)="abrirModalAsignatura()">+ Nueva Actividad Curricular</button>
            }
          </div>

          <table class="tabla">
            <thead>
              <tr><th>Ord.</th><th>Código</th><th>Nombre</th><th>Ciclo / Año</th><th>Horas</th><th>Dictado</th>@if (canEdit()) { <th>Acciones</th> }</tr>
            </thead>
            <tbody>
              @for (a of asignaturas(); track a.id) {
                <tr>
                  <td>{{ a.numeroOrden }}</td>
                  <td>{{ a.codigo || '—' }}</td>
                  <td><strong>{{ a.nombre }}</strong></td>
                  <td>{{ a.grupo || '—' }} · {{ a.subgrupo || '—' }}</td>
                  <td>{{ a.horasReloj }}h</td>
                  <td>
                    <span class="badge" [class.vigente]="(a.estadoDictado || 'SE_DICTA') === 'SE_DICTA'" [class.no-vigente]="a.estadoDictado === 'NO_SE_DICTA'">
                      {{ a.estadoDictado === 'NO_SE_DICTA' ? 'NO SE DICTA' : 'SE DICTA' }}
                    </span>
                  </td>
                  @if (canEdit()) {
                    <td class="acciones">
                      <button type="button" class="btn-xs" (click)="toggleDictado(a)">
                        {{ a.estadoDictado === 'NO_SE_DICTA' ? 'Marcar Se Dicta' : 'Marcar No Se Dicta' }}
                      </button>
                      <button type="button" class="btn-sec" (click)="abrirModalAsignatura(a)">Editar</button>
                      <button type="button" class="btn-danger" (click)="eliminarAsignatura(a)">Eliminar</button>
                    </td>
                  }
                </tr>
              } @empty { <tr><td [attr.colspan]="canEdit() ? 7 : 6" class="empty">Sin actividades curriculares cargadas en este plan.</td></tr> }
            </tbody>
          </table>
        </section>
      }

      @if (modalPlan()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ planEditando() ? 'Editar Plan de Estudio' : 'Nuevo Plan de Estudio' }}</h3>
            @if (planError()) {
              <div class="alert error"><span>{{ planError() }}</span><button type="button" (click)="planError.set(null)">✕</button></div>
            }
            <form [formGroup]="planForm" (ngSubmit)="guardarPlan()">
              <label>Carrera *
                <select formControlName="carreraId">
                  <option value="">Seleccione carrera</option>
                  @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
                </select>
              </label>
              <label>Nombre Plan *<input type="text" formControlName="nombrePlan" placeholder="Plan 2026" /></label>
              <label>Año *<input type="number" formControlName="anioAprobacion" /></label>
              <div class="grid2">
                <label>Duración (años)<input type="number" formControlName="duracionAnios" /></label>
                <label>Horas Totales<input type="number" formControlName="horasTotales" /></label>
              </div>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalPlan.set(false)" [disabled]="guardandoPlan()">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="guardandoPlan()">
                  {{ guardandoPlan() ? 'Guardando...' : 'Guardar' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (modalAsignatura()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ asigEditando() ? 'Editar Actividad Curricular' : 'Nueva Actividad Curricular' }}</h3>
            <form [formGroup]="asigForm" (ngSubmit)="guardarAsignatura()">
              <div class="grid2">
                <label>Código<input type="text" formControlName="codigo" /></label>
                <label>Nombre *<input type="text" formControlName="nombre" /></label>
              </div>
              <div class="grid2">
                <label>Ciclo (Grupo) *<input type="text" formControlName="grupo" placeholder="C.B.I. / T.B. / T.A." /></label>
                <label>Año (Subgrupo) *<input type="text" formControlName="subgrupo" placeholder="1º / 2º / 3º" /></label>
              </div>
              <div class="grid2">
                <label>Horas Reloj *<input type="number" formControlName="horasReloj" /></label>
                <label>Estado de Dictado *
                  <select formControlName="estadoDictado">
                    <option value="SE_DICTA">Se dicta</option>
                    <option value="NO_SE_DICTA">No se dicta</option>
                  </select>
                </label>
              </div>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalAsignatura.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="asigForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .acad-wrap { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
    .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
    .filters { display: flex; gap: 10px; flex-wrap: wrap; }
    .filters select { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
    .sub { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
    h3 { margin: 0; font-size: 17px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla thead th { background: #f7f3f5; color: #5a4a53; font-weight: 600; font-size: 13px; border-bottom: 1px solid #e5dde1; padding: 10px 10px; text-align: left; }
    .tabla td { padding: 9px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; }
    .tabla tr.sel { background: #faf4f7; box-shadow: inset 3px 0 0 #6b4c5b; }
    .badge { padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
    .badge.vigente { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .badge.no-vigente { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .acciones { display: flex; gap: 6px; flex-wrap: wrap; }
    .btn-pri { background: #6b4c5b; color: #ffffff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; transition: background-color 0.15s ease; }
    .btn-pri:hover { background: #553c48; }
    .btn-sec { background: #ffffff; color: #5a4a53; border: 1px solid #d1d5db; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; transition: all 0.15s ease; }
    .btn-sec:hover { background: #fdfafb; border-color: #d9c5ce; color: #6b4c5b; }
    .btn-xs { background: #fdfafb; color: #6b4c5b; border: 1px solid #d9c5ce; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 600; transition: all 0.15s ease; }
    .btn-xs:hover { background: #f5eef1; border-color: #6b4c5b; }
    .btn-danger { background: #fff5f5; color: #991b1b; border: 1px solid #fecaca; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; transition: background-color 0.15s ease; }
    .btn-danger:hover { background: #fee2e2; }
    .empty { text-align: center; padding: 24px; color: #6b7280; }
    .alert { display: flex; justify-content: space-between; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px; }
    .alert.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .alert.success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 560px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
    input, select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
    .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicaPlanesComponent implements OnInit {
  private readonly datos = inject(DatosCentralesService);
  private readonly estructura = inject(EstructuraService);
  private readonly planService = inject(PlanEstudioService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly planes = signal<PlanEstudioMaestro[]>([]);
  readonly planSeleccionado = signal<PlanEstudioMaestro | null>(null);
  readonly asignaturas = signal<ActividadCurricularPlan[]>([]);
  readonly carreraId = signal('');
  readonly vigencia = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly modalPlan = signal(false);
  readonly planEditando = signal<PlanEstudioMaestro | null>(null);
  readonly planError = signal<string | null>(null);
  readonly guardandoPlan = signal(false);
  readonly modalAsignatura = signal(false);
  readonly asigEditando = signal<ActividadCurricularPlan | null>(null);

  readonly isAea = computed(() => this.auth.hasAnyRole('AEA'));
  readonly canEdit = computed(() => {
    const roles = this.auth.currentSession?.user?.roleCodes ?? [];
    return roles.includes('AEA');
  });

  readonly countSeDicta = computed(() => this.asignaturas().filter((a) => a.estadoDictado !== 'NO_SE_DICTA').length);

  readonly planForm = this.fb.group({
    carreraId: ['', Validators.required],
    nombrePlan: ['', [Validators.required, Validators.minLength(3)]],
    anioAprobacion: [new Date().getFullYear(), [Validators.required, Validators.min(1970)]],
    duracionAnios: [5, [Validators.required, Validators.min(1)]],
    horasTotales: [3000, [Validators.required, Validators.min(100)]],
  });

  readonly asigForm = this.fb.group({
    codigo: [''],
    nombre: ['', Validators.required],
    grupo: ['C.B.I.', Validators.required],
    subgrupo: ['1º', Validators.required],
    horasReloj: [64, [Validators.required, Validators.min(0)]],
    estadoDictado: ['SE_DICTA' as EstadoDictadoActividad, Validators.required],
  });

  ngOnInit(): void {
    this.estructura.listarCarreras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((cs) => this.carreras.set(cs));
    this.cargarPlanes();
  }

  cargarPlanes(): void {
    this.datos.getPlanesEstudio(this.carreraId() || undefined, this.vigencia() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((ps) => this.planes.set(ps));
  }

  cambiarVigencia(plan: PlanEstudioMaestro, estado: 'VIGENTE' | 'NO_VIGENTE'): void {
    this.datos.cambiarEstadoVigencia(plan.id, { estadoVigencia: estado })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set(`Plan "${plan.nombrePlan}" actualizado a ${estado}.`);
          this.cargarPlanes();
        },
        error: (err) => this.errorMessage.set('Error al cambiar vigencia: ' + (err.error?.message || err.message)),
      });
  }

  abrirModalPlan(plan?: PlanEstudioMaestro): void {
    this.planError.set(null);
    this.planEditando.set(plan || null);
    this.planForm.reset({
      carreraId: plan?.carreraId || this.carreraId() || '',
      nombrePlan: plan?.nombrePlan || '',
      anioAprobacion: plan?.anioAprobacion || new Date().getFullYear(),
      duracionAnios: plan?.duracionAnios || 5,
      horasTotales: plan?.horasTotales || 3000,
    });
    this.modalPlan.set(true);
  }

  guardarPlan(): void {
    this.planError.set(null);
    if (this.planForm.invalid) {
      this.planForm.markAllAsTouched();
      const c = this.planForm.controls;
      if (c.carreraId.invalid) {
        this.planError.set('Debe seleccionar una Carrera.');
      } else if (c.nombrePlan.invalid) {
        this.planError.set('El nombre del plan es obligatorio (mínimo 3 caracteres).');
      } else if (c.anioAprobacion.invalid) {
        this.planError.set('Ingrese un año válido (1970 o posterior).');
      } else if (c.duracionAnios.invalid) {
        this.planError.set('La duración debe ser al menos 1 año.');
      } else if (c.horasTotales.invalid) {
        this.planError.set('Las horas totales deben ser al menos 100.');
      } else {
        this.planError.set('Revise los campos obligatorios del formulario.');
      }
      return;
    }
    const v = this.planForm.value;
    const payload = {
      carreraId: v.carreraId!,
      nombrePlan: v.nombrePlan!,
      anioAprobacion: Number(v.anioAprobacion),
      duracionAnios: Number(v.duracionAnios),
      horasTotales: Number(v.horasTotales),
    };
    const editando = this.planEditando();
    this.guardandoPlan.set(true);
    const obs$ = editando
      ? this.datos.actualizarPlanEstudio(editando.id, payload)
      : this.datos.crearPlanEstudio(payload);

    obs$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.guardandoPlan.set(false);
        this.modalPlan.set(false);
        this.planEditando.set(null);
        this.planForm.reset({ carreraId: '', nombrePlan: '', anioAprobacion: new Date().getFullYear(), duracionAnios: 5, horasTotales: 3000 });
        this.successMessage.set(editando ? 'Plan de estudio actualizado.' : 'Plan de estudio creado.');
        this.cargarPlanes();
      },
      error: (err) => {
        this.guardandoPlan.set(false);
        const msg = err.error?.message || (err.status === 403 ? 'Solo usuarios con rol AEA pueden editar planes de estudio.' : err.message);
        this.planError.set('Error al guardar plan: ' + msg);
      },
    });
  }

  seleccionarPlan(p: PlanEstudioMaestro): void {
    this.planSeleccionado.set(p);
    this.cargarAsignaturas(p.id);
  }

  cargarAsignaturas(planId: string): void {
    this.planService.listarAsignaturas(planId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((as) => this.asignaturas.set(as));
  }

  abrirModalAsignatura(a?: ActividadCurricularPlan): void {
    this.asigEditando.set(a || null);
    this.asigForm.reset({
      codigo: a?.codigo || '',
      nombre: a?.nombre || '',
      grupo: a?.grupo || 'C.B.I.',
      subgrupo: a?.subgrupo || '1º',
      horasReloj: a?.horasReloj ?? 64,
      estadoDictado: a?.estadoDictado || 'SE_DICTA',
    });
    this.modalAsignatura.set(true);
  }

  guardarAsignatura(): void {
    const plan = this.planSeleccionado();
    if (!plan || this.asigForm.invalid) return;
    const v = this.asigForm.value;
    const edit = this.asigEditando();

    const obs = edit?.id
      ? this.planService.actualizarAsignatura(plan.id, edit.id, {
          id: edit.id,
          codigo: v.codigo || '',
          nombre: v.nombre!,
          grupo: v.grupo!,
          subgrupo: v.subgrupo!,
          horasReloj: Number(v.horasReloj),
          estadoDictado: v.estadoDictado as EstadoDictadoActividad,
        } as ActualizarAsignaturaRequest)
      : this.planService.crearAsignatura(plan.id, {
          codigo: v.codigo || '',
          nombre: v.nombre!,
          grupo: v.grupo!,
          subgrupo: v.subgrupo!,
          horasReloj: Number(v.horasReloj),
          estadoDictado: v.estadoDictado as EstadoDictadoActividad,
        } as CrearAsignaturaRequest);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalAsignatura.set(false);
        this.successMessage.set('Actividad curricular guardada.');
        this.cargarAsignaturas(plan.id);
        this.cargarPlanes();
      },
      error: (err) => this.errorMessage.set('Error al guardar actividad curricular: ' + (err.error?.message || err.message)),
    });
  }

  toggleDictado(a: ActividadCurricularPlan): void {
    const plan = this.planSeleccionado();
    if (!plan || !a.id) return;
    const nuevo: EstadoDictadoActividad = a.estadoDictado === 'NO_SE_DICTA' ? 'SE_DICTA' : 'NO_SE_DICTA';
    this.planService.cambiarEstadoDictado(plan.id, a.id, nuevo).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set(`Estado de "${a.nombre}" actualizado a ${nuevo === 'SE_DICTA' ? 'SE DICTA' : 'NO SE DICTA'}.`);
        this.cargarAsignaturas(plan.id);
      },
      error: (err) => this.errorMessage.set('Error al cambiar estado de dictado: ' + (err.error?.message || err.message)),
    });
  }

  eliminarAsignatura(a: ActividadCurricularPlan): void {
    const plan = this.planSeleccionado();
    if (!plan || !a.id || !confirm(`¿Eliminar la actividad curricular "${a.nombre}"?`)) return;
    this.planService.eliminarAsignatura(plan.id, a.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set('Actividad curricular eliminada.');
        this.cargarAsignaturas(plan.id);
        this.cargarPlanes();
      },
      error: (err) => this.errorMessage.set('Error al eliminar actividad curricular: ' + (err.error?.message || err.message)),
    });
  }
}
