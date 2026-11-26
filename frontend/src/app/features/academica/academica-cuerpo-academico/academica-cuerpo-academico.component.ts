import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import {
  CargoDocente,
  DedicacionDocente,
  DepartamentoAcademico,
  TipoDesignacionDocente,
} from '../../../core/designacion/designacion-parametros.models';
import { DesignacionParametrosService } from '../../../core/designacion/designacion-parametros.service';
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
            <select
              [ngModel]="departamentoFiltroId()"
              [disabled]="esDirectorDeptoRestringido()"
              (ngModelChange)="departamentoFiltroId.set($event); cargarDocentes()"
              aria-label="Filtrar por Departamento Académico"
            >
              @if (!esDirectorDeptoRestringido()) {
                <option value="">Todos los Departamentos</option>
              }
              @for (dep of departamentos(); track dep.id) {
                <option [value]="dep.id">{{ dep.denominacion }} ({{ dep.codigo }})</option>
              }
            </select>

            <select [ngModel]="carreraId()" (ngModelChange)="carreraId.set($event); cargarDocentes()">
              <option value="">Todas las Carreras (Padrón Troncal)</option>
              @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
            </select>

            <input
              type="text"
              [ngModel]="busqueda()"
              (ngModelChange)="busqueda.set($event)"
              (keyup.enter)="cargarDocentes()"
              placeholder="Buscar DNI, Legajo, Mapuche o Apellido..."
            />

            <label class="chk-inline">
              <input
                type="checkbox"
                [ngModel]="incluirInactivos()"
                (ngModelChange)="incluirInactivos.set($event); cargarDocentes()"
              />
              <span>Inactivos</span>
            </label>

            <button type="button" class="btn-sec" (click)="cargarDocentes()">Filtrar</button>
          </div>
          @if (canEdit()) {
            <button type="button" class="btn-pri" (click)="abrirModalDocente()">+ Registrar Docente</button>
          }
        </div>

        <table class="tabla">
          <thead>
            <tr>
              <th>Documento / Legajo</th>
              <th>Apellido y Nombre</th>
              <th>Departamento Académico</th>
              <th>Cargo / Tipo</th>
              <th>Dedicación</th>
              <th>Estado</th>
              <th>Asignaciones</th>
              @if (canEdit()) { <th>Acciones</th> }
            </tr>
          </thead>
          <tbody>
            @for (d of docentes(); track d.id) {
              <tr [class.row-inactiva]="d.activo === false">
                <td>
                  <div><strong>{{ d.tipoDocumento || 'DNI' }} {{ d.dni }}</strong></div>
                  <div class="sub-tags">
                    @if (d.legajo) {
                      <span class="tag-chip">Leg.: {{ d.legajo }}</span>
                    }
                    @if (d.nroDesignacionMapuche) {
                      <span class="tag-chip mapuche">Mapuche: {{ d.nroDesignacionMapuche }}</span>
                    }
                  </div>
                </td>
                <td>
                  <strong>{{ d.apellido }}, {{ d.nombre }}</strong>
                  @if (d.emailInstitucional) {
                    <div class="sub">{{ d.emailInstitucional }}</div>
                  }
                </td>
                <td>
                  <span
                    class="badge"
                    [class.warn]="d.departamentoAcademicoCodigo === 'SIN_ASIGNAR'"
                    [class.neutral]="d.departamentoAcademicoCodigo !== 'SIN_ASIGNAR'"
                  >
                    {{ d.departamentoAcademicoNombre || 'Sin asignar' }}
                  </span>
                </td>
                <td>
                  <div>{{ nombreCargo(d.categoriaDocente) }}</div>
                  <div class="sub">{{ nombreTipoDesignacion(d.caracterDesignacion) }}</div>
                </td>
                <td>{{ nombreDedicacion(d.dedicacionDocente) }} ({{ d.horasSemanales ?? 0 }}h)</td>
                <td>
                  <span class="badge" [class.vigente]="d.activo !== false" [class.no-vigente]="d.activo === false">
                    {{ d.activo !== false ? 'ACTIVO' : 'INACTIVO' }}
                  </span>
                </td>
                <td>
                  <button type="button" class="btn-link" (click)="abrirModalAsignaciones(d)">
                    Gestionar ({{ d.asignaciones?.length ?? 0 }})
                  </button>
                </td>
                @if (canEdit()) {
                  <td class="acciones">
                    <button type="button" class="btn-sec" (click)="abrirModalDocente(d)">Editar</button>
                  </td>
                }
              </tr>
            } @empty {
              <tr><td [attr.colspan]="canEdit() ? 8 : 7" class="empty">Sin docentes registrados para los filtros seleccionados.</td></tr>
            }
          </tbody>
        </table>
      </section>

      @if (modalDocente()) {
        <div class="modal-bg">
          <div class="modal-box modal-lg">
            <h3>{{ docenteEditando() ? 'Editar Ficha de Docente' : 'Registrar Docente en Padrón Institucional' }}</h3>
            <form [formGroup]="docenteForm" (ngSubmit)="guardarDocente()">
              <label>Departamento Académico de pertenencia (RN01) *
                <select formControlName="departamentoAcademicoId">
                  <option value="">Seleccione Departamento Académico...</option>
                  @for (dep of departamentos(); track dep.id) {
                    <option [value]="dep.id">{{ dep.denominacion }} ({{ dep.codigo }})</option>
                  }
                </select>
              </label>

              <div class="grid3">
                <label>Tipo Documento *
                  <select formControlName="tipoDocumento">
                    <option value="DNI">DNI</option>
                    <option value="PASAPORTE">Pasaporte</option>
                    <option value="LE">Libreta de Enrolamiento</option>
                    <option value="LC">Libreta Cívica</option>
                  </select>
                </label>
                <label>N° Documento *<input type="text" formControlName="dni" /></label>
                <label>CUIL / CUIT<input type="text" formControlName="cuit" placeholder="20-XXXXXXXX-X" /></label>
              </div>

              <div class="grid2">
                <label>Legajo Institucional (opcional, único)
                  <input type="text" formControlName="legajo" placeholder="Ej: LEG-1042" />
                </label>
                <label>N° Designación / Cargo SIU-Mapuche (RF01.6)
                  <input type="text" formControlName="nroDesignacionMapuche" placeholder="Referencia manual Mapuche" />
                </label>
              </div>

              <div class="grid2">
                <label>Apellido *<input type="text" formControlName="apellido" /></label>
                <label>Nombre *<input type="text" formControlName="nombre" /></label>
              </div>

              <div class="grid2">
                <label>Email Institucional<input type="email" formControlName="emailInstitucional" /></label>
                <label>Teléfono<input type="text" formControlName="telefono" /></label>
              </div>

              <div class="grid2">
                <label>Título de Grado<input type="text" formControlName="tituloGrado" /></label>
                <label>Máximo Título de Posgrado<input type="text" formControlName="maximoTituloPosgrado" /></label>
              </div>

              <div class="grid2">
                <label>Cargo Docente Base (RN03) *
                  <select formControlName="categoriaDocente">
                    @for (c of cargos(); track c.id) {
                      <option [value]="c.codigo">{{ c.nombre }}</option>
                    }
                  </select>
                </label>
                <label>Tipo de Designación Base (RF10.3) *
                  <select formControlName="caracterDesignacion">
                    @for (t of tiposDesignacion(); track t.id) {
                      <option [value]="t.codigo">{{ t.nombre }}</option>
                    }
                  </select>
                </label>
              </div>

              <div class="grid2">
                <label>Dedicación Horaria Base (RN04) *
                  <select formControlName="dedicacionDocente" (change)="onDedicacionChange($any($event.target).value)">
                    @for (ded of dedicaciones(); track ded.id) {
                      <option [value]="ded.codigo">{{ ded.nombre }} ({{ ded.horasSemanales }} hs)</option>
                    }
                  </select>
                </label>
                <label>Horas Semanales *<input type="number" formControlName="horasSemanales" min="1" max="50" /></label>
              </div>

              <label class="chk-inline" style="margin-top: 4px;">
                <input type="checkbox" formControlName="activo" />
                <span>Docente activo en el padrón institucional</span>
              </label>

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
                      @for (p of asigPlanes(); track p.id) { <option [value]="p.id">{{ p.nombrePlan || p.codigoResolucion || ('Plan ' + p.anioAprobacion) }}</option> }
                    </select>
                  </label>
                  <label>Actividad Curricular
                    <select [ngModel]="asigActId()" (ngModelChange)="asigActId.set($event)">
                      <option value="">Seleccione...</option>
                      @for (a of asigActividades(); track a.id) {
                        <option [value]="a.id">{{ a.nombre }} @if (a.estadoDictado === 'SOLO_EXAMENES_FINALES') { (Solo Finales) }</option>
                      }
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
    .acad-wrap { max-width: 1240px; margin: 0 auto; padding: 24px; }
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
    .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
    .filters { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
    .filters select, .filters input[type="text"] { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla th, .tabla td { padding: 9px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; vertical-align: middle; }
    .row-inactiva { opacity: 0.68; }
    .sub { font-size: 12px; color: #6b7280; margin-top: 2px; }
    .sub-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 3px; }
    .tag-chip { font-size: 11px; background: #f3f4f6; color: #374151; padding: 1px 6px; border-radius: 4px; }
    .tag-chip.mapuche { background: #eff6ff; color: #1d4ed8; }
    .badge { display: inline-block; padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
    .badge.vigente { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .badge.no-vigente { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .badge.warn { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
    .badge.neutral { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }
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
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 640px; max-height: 90vh; overflow-y: auto; }
    .modal-box.modal-lg { max-width: 760px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
    label.chk-inline { flex-direction: row; align-items: center; gap: 6px; margin-bottom: 0; cursor: pointer; font-weight: 500; }
    input[type="text"], input[type="email"], input[type="number"], select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
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
  private readonly parametrosService = inject(DesignacionParametrosService);
  private readonly estructura = inject(EstructuraService);
  private readonly planService = inject(PlanEstudioService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly departamentos = signal<DepartamentoAcademico[]>([]);
  readonly cargos = signal<CargoDocente[]>([]);
  readonly dedicaciones = signal<DedicacionDocente[]>([]);
  readonly tiposDesignacion = signal<TipoDesignacionDocente[]>([]);

  readonly docentes = signal<CuerpoAcademicoMaestro[]>([]);
  readonly departamentoFiltroId = signal('');
  readonly carreraId = signal('');
  readonly busqueda = signal('');
  readonly incluirInactivos = signal(false);
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

  readonly canEdit = computed(() =>
    this.auth.hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')
  );

  readonly esDirectorDeptoRestringido = computed(() => {
    const roles = this.auth.currentSession?.user?.roleCodes ?? [];
    const esInstitucional = roles.some((r) =>
      ['AEA', 'SECRETARIA_ACADEMICA', 'CONSULTA_INSTITUCIONAL', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA'].includes(r)
    );
    return !esInstitucional && roles.includes('DIRECTOR_DEPARTAMENTO');
  });

  readonly docenteForm = this.fb.group({
    departamentoAcademicoId: ['', Validators.required],
    tipoDocumento: ['DNI', Validators.required],
    dni: ['', Validators.required],
    cuit: [''],
    legajo: [''],
    nroDesignacionMapuche: [''],
    apellido: ['', Validators.required],
    nombre: ['', Validators.required],
    emailInstitucional: [''],
    telefono: [''],
    tituloGrado: [''],
    maximoTituloPosgrado: [''],
    categoriaDocente: ['ADJUNTO', Validators.required],
    caracterDesignacion: ['INTERINO_RENTADO', Validators.required],
    dedicacionDocente: ['SIMPLE', Validators.required],
    horasSemanales: [10, [Validators.required, Validators.min(1), Validators.max(50)]],
    activo: [true],
  });

  ngOnInit(): void {
    const userDepto = this.auth.currentSession?.user?.departamentoAcademicoId;
    if (this.esDirectorDeptoRestringido() && userDepto) {
      this.departamentoFiltroId.set(userDepto);
    }

    this.parametrosService
      .obtenerCatalogos(false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((cat) => {
        this.departamentos.set(cat.departamentos);
        this.cargos.set(cat.cargos);
        this.dedicaciones.set(cat.dedicaciones);
        this.tiposDesignacion.set(cat.tiposDesignacion);
      });

    this.estructura
      .listarCarreras()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((cs) => this.carreras.set(cs));

    this.cargarDocentes();
  }

  cargarDocentes(): void {
    this.datos
      .getCuerpoAcademico(
        this.carreraId() || undefined,
        this.busqueda() || undefined,
        this.departamentoFiltroId() || undefined,
        this.incluirInactivos()
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ds) => this.docentes.set(ds),
        error: (err) => this.errorMessage.set(err.error?.error || err.error?.message || 'No se pudo cargar el padrón docente.'),
      });
  }

  nombreCargo(codigo?: string): string {
    if (!codigo) return '—';
    return this.cargos().find((c) => c.codigo === codigo)?.nombre || codigo;
  }

  nombreDedicacion(codigo?: string): string {
    if (!codigo) return '—';
    return this.dedicaciones().find((d) => d.codigo === codigo)?.nombre || codigo;
  }

  nombreTipoDesignacion(codigo?: string): string {
    if (!codigo) return '—';
    return this.tiposDesignacion().find((t) => t.codigo === codigo)?.nombre || codigo;
  }

  onDedicacionChange(codigo: string): void {
    const found = this.dedicaciones().find((d) => d.codigo === codigo);
    if (found) {
      this.docenteForm.controls.horasSemanales.setValue(found.horasSemanales);
    }
  }

  abrirModalDocente(d?: CuerpoAcademicoMaestro): void {
    this.docenteEditando.set(d || null);
    const userDepto = this.auth.currentSession?.user?.departamentoAcademicoId || '';
    const defaultDepto =
      d?.departamentoAcademicoId ||
      (this.esDirectorDeptoRestringido() ? userDepto : this.departamentoFiltroId()) ||
      this.departamentos().find((dep) => dep.codigo !== 'SIN_ASIGNAR')?.id ||
      this.departamentos()[0]?.id ||
      '';

    const defaultCargo = d?.categoriaDocente || this.cargos()[0]?.codigo || 'ADJUNTO';
    const defaultTipo = d?.caracterDesignacion || this.tiposDesignacion()[0]?.codigo || 'INTERINO_RENTADO';
    const defaultDed = d?.dedicacionDocente || this.dedicaciones()[2]?.codigo || this.dedicaciones()[0]?.codigo || 'SIMPLE';
    const dedHoras = this.dedicaciones().find((x) => x.codigo === defaultDed)?.horasSemanales ?? 10;

    this.docenteForm.reset({
      departamentoAcademicoId: defaultDepto,
      tipoDocumento: d?.tipoDocumento || 'DNI',
      dni: d?.dni || '',
      cuit: d?.cuit || '',
      legajo: d?.legajo || '',
      nroDesignacionMapuche: d?.nroDesignacionMapuche || '',
      apellido: d?.apellido || '',
      nombre: d?.nombre || '',
      emailInstitucional: d?.emailInstitucional || '',
      telefono: d?.telefono || '',
      tituloGrado: d?.tituloGrado || '',
      maximoTituloPosgrado: d?.maximoTituloPosgrado || '',
      categoriaDocente: defaultCargo,
      caracterDesignacion: defaultTipo,
      dedicacionDocente: defaultDed,
      horasSemanales: d?.horasSemanales ?? dedHoras,
      activo: d ? d.activo !== false : true,
    });

    if (this.esDirectorDeptoRestringido()) {
      this.docenteForm.controls.departamentoAcademicoId.disable();
    } else {
      this.docenteForm.controls.departamentoAcademicoId.enable();
    }

    this.modalDocente.set(true);
  }

  guardarDocente(): void {
    if (this.docenteForm.invalid) return;
    const v = this.docenteForm.getRawValue();
    const req = {
      departamentoAcademicoId: v.departamentoAcademicoId || null,
      tipoDocumento: v.tipoDocumento || 'DNI',
      dni: v.dni!,
      cuit: v.cuit || undefined,
      legajo: v.legajo ?? '',
      nroDesignacionMapuche: v.nroDesignacionMapuche ?? '',
      apellido: v.apellido!,
      nombre: v.nombre!,
      emailInstitucional: v.emailInstitucional || undefined,
      telefono: v.telefono || undefined,
      tituloGrado: v.tituloGrado || undefined,
      maximoTituloPosgrado: v.maximoTituloPosgrado || undefined,
      categoriaDocente: v.categoriaDocente!,
      caracterDesignacion: v.caracterDesignacion!,
      dedicacionDocente: v.dedicacionDocente!,
      horasSemanales: Number(v.horasSemanales) || 10,
      activo: v.activo !== false,
    };
    const edit = this.docenteEditando();
    const obs = edit
      ? this.datos.actualizarCuerpoAcademico(edit.id, req)
      : this.datos.crearCuerpoAcademico(req);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalDocente.set(false);
        this.successMessage.set('Ficha docente guardada correctamente.');
        this.cargarDocentes();
      },
      error: (err) => this.errorMessage.set('Error al guardar docente: ' + (err.error?.error || err.error?.message || err.message)),
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
    this.planService
      .listarPlanes(carreraId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((ps) => this.asigPlanes.set(ps.filter((p) => !p.procesoId)));
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
      .subscribe((as) => this.asigActividades.set(as.filter((a) => a.estadoDictado !== 'INACTIVA' && a.estadoDictado !== 'NO_SE_DICTA')));
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
      error: (err) => this.errorMessage.set('Error al asignar cátedra: ' + (err.error?.error || err.error?.message || err.message)),
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
      error: (err) => this.errorMessage.set('Error al eliminar asignación: ' + (err.error?.error || err.error?.message || err.message)),
    });
  }
}
