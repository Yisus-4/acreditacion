import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import {
  CargoDocente,
  DedicacionDocente,
  DepartamentoAcademico,
  PeriodoAcademico,
  TipoDesignacionDocente,
} from '../../../core/designacion/designacion-parametros.models';
import { DesignacionParametrosService } from '../../../core/designacion/designacion-parametros.service';
import { SubnavAcademicaComponent } from '../subnav-academica/subnav-academica.component';

type TabParametro = 'DEPARTAMENTOS' | 'CARGOS' | 'DEDICACIONES' | 'TIPOS' | 'PERIODOS';

@Component({
  selector: 'app-academica-departamentos-parametros',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SubnavAcademicaComponent],
  template: `
    <div class="acad-wrap">
      <app-subnav-academica seccionActual="Departamentos y Parámetros"></app-subnav-academica>

      @if (errorMessage()) {
        <div class="alert error">
          <span>{{ errorMessage() }}</span>
          <button type="button" (click)="errorMessage.set(null)">✕</button>
        </div>
      }
      @if (successMessage()) {
        <div class="alert success">
          <span>{{ successMessage() }}</span>
          <button type="button" (click)="successMessage.set(null)">✕</button>
        </div>
      }

      <section class="panel">
        <div class="bar">
          <div class="inner-tabs" role="tablist">
            <button type="button" class="itab" [class.on]="tab() === 'DEPARTAMENTOS'" (click)="tab.set('DEPARTAMENTOS')">
              Departamentos Académicos ({{ departamentos().length }})
            </button>
            <button type="button" class="itab" [class.on]="tab() === 'CARGOS'" (click)="tab.set('CARGOS')">
              Cargos Docentes ({{ cargos().length }})
            </button>
            <button type="button" class="itab" [class.on]="tab() === 'DEDICACIONES'" (click)="tab.set('DEDICACIONES')">
              Dedicaciones ({{ dedicaciones().length }})
            </button>
            <button type="button" class="itab" [class.on]="tab() === 'TIPOS'" (click)="tab.set('TIPOS')">
              Tipos de Designación ({{ tiposDesignacion().length }})
            </button>
            <button type="button" class="itab" [class.on]="tab() === 'PERIODOS'" (click)="tab.set('PERIODOS')">
              Períodos Académicos ({{ periodos().length }})
            </button>
          </div>

          <div class="right-controls">
            <label class="chk-inline">
              <input type="checkbox" [ngModel]="incluirInactivos()" (ngModelChange)="incluirInactivos.set($event); cargarTodo()" />
              <span>Mostrar inactivos</span>
            </label>
            @if (canManage()) {
              <button type="button" class="btn-pri" (click)="abrirNuevoSegunTab()">
                + {{ botonNuevoLabel() }}
              </button>
            }
          </div>
        </div>

        @if (tab() === 'DEPARTAMENTOS') {
          <p class="sub-desc">
            Unidades organizacionales académicas responsables de administrar docentes y elaborar las solicitudes anuales y extraordinarias de designación y asignación docente (RF02.1, RN01).
          </p>
          <table class="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Denominación</th>
                <th>Descripción</th>
                <th>Estado</th>
                @if (canManage()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (d of departamentos(); track d.id) {
                <tr>
                  <td>
                    <code>{{ d.codigo }}</code>
                    @if (d.esSistema) {
                      <span class="badge neutral" style="margin-left: 6px;">SISTEMA</span>
                    }
                  </td>
                  <td><strong>{{ d.denominacion }}</strong></td>
                  <td>{{ d.descripcion || '—' }}</td>
                  <td>
                    <span class="badge" [class.vigente]="d.activo" [class.no-vigente]="!d.activo">
                      {{ d.activo ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  @if (canManage()) {
                    <td>
                      <button type="button" class="btn-sec" (click)="abrirModalDepartamento(d)">Editar</button>
                    </td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canManage() ? 5 : 4" class="empty">Sin departamentos registrados.</td></tr>
              }
            </tbody>
          </table>
        }

        @if (tab() === 'CARGOS') {
          <p class="sub-desc">
            Jerarquías parametrizables para designación docente (RN03, RNF06).
          </p>
          <table class="tabla">
            <thead>
              <tr>
                <th>Orden</th><th>Código</th><th>Nombre del Cargo</th><th>Estado</th>@if (canManage()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (c of cargos(); track c.id) {
                <tr>
                  <td>{{ c.orden }}</td>
                  <td><code>{{ c.codigo }}</code></td>
                  <td><strong>{{ c.nombre }}</strong></td>
                  <td>
                    <span class="badge" [class.vigente]="c.activo" [class.no-vigente]="!c.activo">
                      {{ c.activo ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  @if (canManage()) {
                    <td><button type="button" class="btn-sec" (click)="abrirModalCargo(c)">Editar</button></td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canManage() ? 5 : 4" class="empty">Sin cargos parametrizados.</td></tr>
              }
            </tbody>
          </table>
        }

        @if (tab() === 'DEDICACIONES') {
          <p class="sub-desc">
            Dedicaciones horarias semanales parametrizables (RN04: Exclusiva 40 hs, Parcial 20 hs, Simple 10 hs; RNF06).
          </p>
          <table class="tabla">
            <thead>
              <tr>
                <th>Orden</th><th>Código</th><th>Dedicación</th><th>Carga Semanal</th><th>Estado</th>@if (canManage()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (ded of dedicaciones(); track ded.id) {
                <tr>
                  <td>{{ ded.orden }}</td>
                  <td><code>{{ ded.codigo }}</code></td>
                  <td><strong>{{ ded.nombre }}</strong></td>
                  <td><strong>{{ ded.horasSemanales }} hs/sem</strong></td>
                  <td>
                    <span class="badge" [class.vigente]="ded.activo" [class.no-vigente]="!ded.activo">
                      {{ ded.activo ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  @if (canManage()) {
                    <td><button type="button" class="btn-sec" (click)="abrirModalDedicacion(ded)">Editar</button></td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canManage() ? 6 : 5" class="empty">Sin dedicaciones parametrizadas.</td></tr>
              }
            </tbody>
          </table>
        }

        @if (tab() === 'TIPOS') {
          <p class="sub-desc">
            Tipos de designación docente contemplados en la emisión oficial de solicitudes (RF10.3, RNF06).
          </p>
          <table class="tabla">
            <thead>
              <tr>
                <th>Orden</th><th>Código</th><th>Tipo de Designación</th><th>Régimen</th><th>Estado</th>@if (canManage()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (t of tiposDesignacion(); track t.id) {
                <tr>
                  <td>{{ t.orden }}</td>
                  <td><code>{{ t.codigo }}</code></td>
                  <td><strong>{{ t.nombre }}</strong></td>
                  <td>{{ t.esRentado ? 'Rentado' : 'Ad honorem' }}</td>
                  <td>
                    <span class="badge" [class.vigente]="t.activo" [class.no-vigente]="!t.activo">
                      {{ t.activo ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  @if (canManage()) {
                    <td><button type="button" class="btn-sec" (click)="abrirModalTipo(t)">Editar</button></td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canManage() ? 6 : 5" class="empty">Sin tipos de designación parametrizados.</td></tr>
              }
            </tbody>
          </table>
        }

        @if (tab() === 'PERIODOS') {
          <p class="sub-desc">
            Períodos académicos habilitados para solicitudes anuales y extraordinarias de designación docente.
          </p>
          <table class="tabla">
            <thead>
              <tr>
                <th>Código</th><th>Denominación</th><th>Año</th><th>Vigencia</th><th>Actual</th><th>Estado</th>@if (canManage()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (p of periodos(); track p.id) {
                <tr>
                  <td><code>{{ p.codigo }}</code></td>
                  <td><strong>{{ p.denominacion }}</strong></td>
                  <td>{{ p.anio }}</td>
                  <td>{{ p.fechaInicio || '—' }} al {{ p.fechaFin || '—' }}</td>
                  <td>
                    @if (p.esActual) {
                      <span class="badge vigente">PERÍODO ACTUAL</span>
                    } @else {
                      <span class="sub">—</span>
                    }
                  </td>
                  <td>
                    <span class="badge" [class.vigente]="p.activo" [class.no-vigente]="!p.activo">
                      {{ p.activo ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  @if (canManage()) {
                    <td><button type="button" class="btn-sec" (click)="abrirModalPeriodo(p)">Editar</button></td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canManage() ? 7 : 6" class="empty">Sin períodos académicos registrados.</td></tr>
              }
            </tbody>
          </table>
        }
      </section>

      <!-- Modal Departamento -->
      @if (modalDepto()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ editandoDepto() ? 'Editar Departamento Académico' : 'Nuevo Departamento Académico' }}</h3>
            <form [formGroup]="deptoForm" (ngSubmit)="guardarDepartamento()">
              <div class="grid2">
                <label>Código *
                  <input type="text" formControlName="codigo" [readonly]="!!editandoDepto()?.esSistema" placeholder="Ej: CBASICAS" />
                </label>
                <label>Denominación *
                  <input type="text" formControlName="denominacion" placeholder="Ej: Departamento de Ciencias Básicas" />
                </label>
              </div>
              <label>Descripción
                <input type="text" formControlName="descripcion" placeholder="Observaciones u alcance disciplinar..." />
              </label>
              @if (!editandoDepto()?.esSistema) {
                <label class="chk-inline">
                  <input type="checkbox" formControlName="activo" />
                  <span>Departamento activo</span>
                </label>
              }
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalDepto.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="deptoForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Modal Cargo -->
      @if (modalCargo()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ editandoCargo() ? 'Editar Cargo Docente' : 'Nuevo Cargo Docente' }}</h3>
            <form [formGroup]="cargoForm" (ngSubmit)="guardarCargo()">
              <div class="grid2">
                <label>Código *<input type="text" formControlName="codigo" placeholder="Ej: TITULAR" /></label>
                <label>Orden *<input type="number" formControlName="orden" min="1" /></label>
              </div>
              <label>Nombre del Cargo *<input type="text" formControlName="nombre" placeholder="Ej: Profesor Titular" /></label>
              <label class="chk-inline">
                <input type="checkbox" formControlName="activo" />
                <span>Cargo activo</span>
              </label>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalCargo.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="cargoForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Modal Dedicación -->
      @if (modalDedicacion()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ editandoDedicacion() ? 'Editar Dedicación Docente' : 'Nueva Dedicación Docente' }}</h3>
            <form [formGroup]="dedicacionForm" (ngSubmit)="guardarDedicacion()">
              <div class="grid2">
                <label>Código *<input type="text" formControlName="codigo" placeholder="Ej: EXCLUSIVA" /></label>
                <label>Horas Semanales (1-50) *<input type="number" formControlName="horasSemanales" min="1" max="50" /></label>
              </div>
              <div class="grid2">
                <label>Nombre *<input type="text" formControlName="nombre" placeholder="Ej: Exclusiva" /></label>
                <label>Orden *<input type="number" formControlName="orden" min="1" /></label>
              </div>
              <label class="chk-inline">
                <input type="checkbox" formControlName="activo" />
                <span>Dedicación activa</span>
              </label>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalDedicacion.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="dedicacionForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Modal Tipo Designación -->
      @if (modalTipo()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ editandoTipo() ? 'Editar Tipo de Designación' : 'Nuevo Tipo de Designación' }}</h3>
            <form [formGroup]="tipoForm" (ngSubmit)="guardarTipo()">
              <div class="grid2">
                <label>Código *<input type="text" formControlName="codigo" placeholder="Ej: INTERINO_RENTADO" /></label>
                <label>Orden *<input type="number" formControlName="orden" min="1" /></label>
              </div>
              <label>Nombre *<input type="text" formControlName="nombre" placeholder="Ej: Interino Rentado" /></label>
              <div class="grid2" style="margin-top: 6px;">
                <label class="chk-inline">
                  <input type="checkbox" formControlName="esRentado" />
                  <span>Cargo rentado (desmarcar si es ad honorem)</span>
                </label>
                <label class="chk-inline">
                  <input type="checkbox" formControlName="activo" />
                  <span>Activo</span>
                </label>
              </div>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalTipo.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="tipoForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Modal Período -->
      @if (modalPeriodo()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ editandoPeriodo() ? 'Editar Período Académico' : 'Nuevo Período Académico' }}</h3>
            <form [formGroup]="periodoForm" (ngSubmit)="guardarPeriodo()">
              <div class="grid2">
                <label>Código *<input type="text" formControlName="codigo" placeholder="Ej: 2026" /></label>
                <label>Año *<input type="number" formControlName="anio" min="1990" max="2100" /></label>
              </div>
              <label>Denominación *<input type="text" formControlName="denominacion" placeholder="Ej: Ciclo Lectivo 2026" /></label>
              <div class="grid2">
                <label>Fecha Inicio<input type="date" formControlName="fechaInicio" /></label>
                <label>Fecha Fin<input type="date" formControlName="fechaFin" /></label>
              </div>
              <div class="grid2" style="margin-top: 6px;">
                <label class="chk-inline">
                  <input type="checkbox" formControlName="esActual" />
                  <span>Marcar como período actual</span>
                </label>
                <label class="chk-inline">
                  <input type="checkbox" formControlName="activo" />
                  <span>Activo</span>
                </label>
              </div>
              <div class="m-actions">
                <button type="button" class="btn-sec" (click)="modalPeriodo.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="periodoForm.invalid">Guardar</button>
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
    .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
    .inner-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
    .itab { background: #f9fafb; border: 1px solid #e5e7eb; color: #5a4a53; padding: 7px 12px; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; }
    .itab.on { background: #6b4c5b; color: #fff; border-color: #6b4c5b; }
    .right-controls { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
    .sub-desc { margin: 0 0 12px; font-size: 13px; color: #6b7280; }
    .sub { color: #6b7280; font-size: 12px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla thead th { background: #f7f3f5; color: #5a4a53; font-weight: 600; font-size: 13px; border-bottom: 1px solid #e5dde1; padding: 10px; text-align: left; }
    .tabla td { padding: 9px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; }
    code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
    .badge { padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
    .badge.vigente { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .badge.no-vigente { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .badge.neutral { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }
    .btn-pri { background: #6b4c5b; color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-pri:hover { background: #553c48; }
    .btn-sec { background: #fff; color: #5a4a53; border: 1px solid #d1d5db; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .empty { text-align: center; padding: 24px; color: #6b7280; }
    .alert { display: flex; justify-content: space-between; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px; }
    .alert.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .alert.success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 560px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
    label.chk-inline { flex-direction: row; align-items: center; gap: 8px; margin-bottom: 0; cursor: pointer; font-weight: 500; }
    input[type="text"], input[type="number"], input[type="date"], select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
    .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicaDepartamentosParametrosComponent implements OnInit {
  private readonly service = inject(DesignacionParametrosService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly tab = signal<TabParametro>('DEPARTAMENTOS');
  readonly incluirInactivos = signal(false);
  readonly departamentos = signal<DepartamentoAcademico[]>([]);
  readonly cargos = signal<CargoDocente[]>([]);
  readonly dedicaciones = signal<DedicacionDocente[]>([]);
  readonly tiposDesignacion = signal<TipoDesignacionDocente[]>([]);
  readonly periodos = signal<PeriodoAcademico[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalDepto = signal(false);
  readonly editandoDepto = signal<DepartamentoAcademico | null>(null);

  readonly modalCargo = signal(false);
  readonly editandoCargo = signal<CargoDocente | null>(null);

  readonly modalDedicacion = signal(false);
  readonly editandoDedicacion = signal<DedicacionDocente | null>(null);

  readonly modalTipo = signal(false);
  readonly editandoTipo = signal<TipoDesignacionDocente | null>(null);

  readonly modalPeriodo = signal(false);
  readonly editandoPeriodo = signal<PeriodoAcademico | null>(null);

  readonly canManage = computed(() =>
    this.auth.hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')
  );

  readonly botonNuevoLabel = computed(() => {
    switch (this.tab()) {
      case 'DEPARTAMENTOS':
        return 'Nuevo Departamento';
      case 'CARGOS':
        return 'Nuevo Cargo';
      case 'DEDICACIONES':
        return 'Nueva Dedicación';
      case 'TIPOS':
        return 'Nuevo Tipo';
      case 'PERIODOS':
        return 'Nuevo Período';
    }
  });

  readonly deptoForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    denominacion: ['', Validators.required],
    descripcion: [''],
    activo: [true],
  });

  readonly cargoForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nombre: ['', Validators.required],
    orden: [1, [Validators.required, Validators.min(1)]],
    activo: [true],
  });

  readonly dedicacionForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nombre: ['', Validators.required],
    horasSemanales: [10, [Validators.required, Validators.min(1), Validators.max(50)]],
    orden: [1, [Validators.required, Validators.min(1)]],
    activo: [true],
  });

  readonly tipoForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nombre: ['', Validators.required],
    esRentado: [true],
    orden: [1, [Validators.required, Validators.min(1)]],
    activo: [true],
  });

  readonly periodoForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    denominacion: ['', Validators.required],
    anio: [new Date().getFullYear(), [Validators.required, Validators.min(1990), Validators.max(2100)]],
    fechaInicio: [''],
    fechaFin: [''],
    esActual: [false],
    activo: [true],
  });

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.service
      .obtenerCatalogos(this.incluirInactivos())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cat) => {
          this.departamentos.set(cat.departamentos);
          this.cargos.set(cat.cargos);
          this.dedicaciones.set(cat.dedicaciones);
          this.tiposDesignacion.set(cat.tiposDesignacion);
          this.periodos.set(cat.periodos);
        },
        error: (err) => this.errorMessage.set('No se pudieron cargar los catálogos: ' + (err.error?.error || err.message)),
      });
  }

  abrirNuevoSegunTab(): void {
    switch (this.tab()) {
      case 'DEPARTAMENTOS':
        this.abrirModalDepartamento();
        break;
      case 'CARGOS':
        this.abrirModalCargo();
        break;
      case 'DEDICACIONES':
        this.abrirModalDedicacion();
        break;
      case 'TIPOS':
        this.abrirModalTipo();
        break;
      case 'PERIODOS':
        this.abrirModalPeriodo();
        break;
    }
  }

  abrirModalDepartamento(d?: DepartamentoAcademico): void {
    this.editandoDepto.set(d || null);
    this.deptoForm.reset({
      codigo: d?.codigo || '',
      denominacion: d?.denominacion || '',
      descripcion: d?.descripcion || '',
      activo: d ? d.activo : true,
    });
    this.modalDepto.set(true);
  }

  guardarDepartamento(): void {
    if (this.deptoForm.invalid) return;
    const v = this.deptoForm.getRawValue();
    const edit = this.editandoDepto();
    const obs = edit
      ? this.service.actualizarDepartamento(edit.id, v)
      : this.service.crearDepartamento(v);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalDepto.set(false);
        this.successMessage.set('Departamento académico guardado correctamente.');
        this.cargarTodo();
      },
      error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'Error al guardar departamento.'),
    });
  }

  abrirModalCargo(c?: CargoDocente): void {
    this.editandoCargo.set(c || null);
    this.cargoForm.reset({
      codigo: c?.codigo || '',
      nombre: c?.nombre || '',
      orden: c?.orden ?? (this.cargos().length + 1),
      activo: c ? c.activo : true,
    });
    this.modalCargo.set(true);
  }

  guardarCargo(): void {
    if (this.cargoForm.invalid) return;
    const v = this.cargoForm.getRawValue();
    const edit = this.editandoCargo();
    const obs = edit
      ? this.service.actualizarCargo(edit.id, v)
      : this.service.crearCargo(v);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalCargo.set(false);
        this.successMessage.set('Cargo docente guardado correctamente.');
        this.cargarTodo();
      },
      error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'Error al guardar cargo.'),
    });
  }

  abrirModalDedicacion(d?: DedicacionDocente): void {
    this.editandoDedicacion.set(d || null);
    this.dedicacionForm.reset({
      codigo: d?.codigo || '',
      nombre: d?.nombre || '',
      horasSemanales: d?.horasSemanales ?? 20,
      orden: d?.orden ?? (this.dedicaciones().length + 1),
      activo: d ? d.activo : true,
    });
    this.modalDedicacion.set(true);
  }

  guardarDedicacion(): void {
    if (this.dedicacionForm.invalid) return;
    const v = this.dedicacionForm.getRawValue();
    const edit = this.editandoDedicacion();
    const obs = edit
      ? this.service.actualizarDedicacion(edit.id, v)
      : this.service.crearDedicacion(v);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalDedicacion.set(false);
        this.successMessage.set('Dedicación docente guardada correctamente.');
        this.cargarTodo();
      },
      error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'Error al guardar dedicación.'),
    });
  }

  abrirModalTipo(t?: TipoDesignacionDocente): void {
    this.editandoTipo.set(t || null);
    this.tipoForm.reset({
      codigo: t?.codigo || '',
      nombre: t?.nombre || '',
      esRentado: t ? t.esRentado : true,
      orden: t?.orden ?? (this.tiposDesignacion().length + 1),
      activo: t ? t.activo : true,
    });
    this.modalTipo.set(true);
  }

  guardarTipo(): void {
    if (this.tipoForm.invalid) return;
    const v = this.tipoForm.getRawValue();
    const edit = this.editandoTipo();
    const obs = edit
      ? this.service.actualizarTipoDesignacion(edit.id, v)
      : this.service.crearTipoDesignacion(v);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalTipo.set(false);
        this.successMessage.set('Tipo de designación guardado correctamente.');
        this.cargarTodo();
      },
      error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'Error al guardar tipo de designación.'),
    });
  }

  abrirModalPeriodo(p?: PeriodoAcademico): void {
    this.editandoPeriodo.set(p || null);
    const anio = p?.anio ?? new Date().getFullYear();
    this.periodoForm.reset({
      codigo: p?.codigo || String(anio),
      denominacion: p?.denominacion || `Ciclo Lectivo ${anio}`,
      anio,
      fechaInicio: p?.fechaInicio || `${anio}-03-01`,
      fechaFin: p?.fechaFin || `${anio}-12-31`,
      esActual: p ? p.esActual : false,
      activo: p ? p.activo : true,
    });
    this.modalPeriodo.set(true);
  }

  guardarPeriodo(): void {
    if (this.periodoForm.invalid) return;
    const v = this.periodoForm.getRawValue();
    const payload = {
      codigo: v.codigo,
      denominacion: v.denominacion,
      anio: Number(v.anio),
      fechaInicio: v.fechaInicio || null,
      fechaFin: v.fechaFin || null,
      esActual: v.esActual,
      activo: v.activo,
    };
    const edit = this.editandoPeriodo();
    const obs = edit
      ? this.service.actualizarPeriodo(edit.id, payload)
      : this.service.crearPeriodo(payload);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalPeriodo.set(false);
        this.successMessage.set('Período académico guardado correctamente.');
        this.cargarTodo();
      },
      error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'Error al guardar período académico.'),
    });
  }
}
