import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AutoevaluacionService } from '../../../core/autoevaluacion/autoevaluacion.service';
import {
  AsignarActividadEspacioRequest,
  EspacioFisico,
  EspacioFisicoActividadCurricularDto,
  Inmueble,
  RegistrarInmuebleRequest,
  TipoEspacioFisico,
} from '../../../core/autoevaluacion/autoevaluacion.models';
import { EstructuraService, CarreraResponse, SedeLocalizacionResponse } from '../../../core/estructura/estructura.service';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import { ActividadCurricularPlan, PlanEstudioFt001 } from '../../../core/planes-estudio/plan-estudio.models';
import { SubnavGestionInstitucionalComponent } from '../subnav-gestion-institucional/subnav-gestion-institucional.component';

@Component({
  selector: 'app-admin-infraestructura',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SubnavGestionInstitucionalComponent],
  template: `
    <div class="vista-gestion-layout">
      <app-subnav-gestion-institucional seccionActual="Infraestructura"></app-subnav-gestion-institucional>

      @if (errorMessage()) {
        <div class="alert-banner-figma error">
          <span>{{ errorMessage() }}</span>
          <button type="button" class="btn-accion-icono" (click)="errorMessage.set(null)">✕</button>
        </div>
      }
      @if (successMessage()) {
        <div class="alert-banner-figma success">
          <span>{{ successMessage() }}</span>
          <button type="button" class="btn-accion-icono" (click)="successMessage.set(null)">✕</button>
        </div>
      }

      <div class="vista-toolbar">
        <div class="search-box-figma" style="flex: 1 1 320px;">
          <input type="text" placeholder="Buscar inmueble por nombre, domicilio o sede..." [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" />
        </div>
        <button type="button" class="btn-primary-figma" (click)="abrirModalNuevo()">+ Nuevo Inmueble Maestro</button>
      </div>

      @if (loading()) {
        <p class="estado-vacio">Cargando padrón maestro de infraestructura...</p>
      } @else {
        <div class="inmuebles-lista">
          @for (inm of inmueblesFiltrados(); track inm.id) {
            <article class="inmueble-card">
              <header class="inmueble-head">
                <div>
                  <h3>{{ inm.nombre }}</h3>
                  <p class="sub">{{ inm.domicilio }} @if (inm.sedeNombre) { · Sede: {{ inm.sedeNombre }} } · {{ inm.condicionTenencia }}</p>
                </div>
                <div class="acciones">
                  <button type="button" class="btn-sec" (click)="abrirModalEditar(inm)">Editar Inmueble / Espacios</button>
                  <button type="button" class="btn-danger" (click)="eliminarInmueble(inm)">Eliminar</button>
                </div>
              </header>

              <div class="espacios-wrap">
                <h4>Espacios Físicos ({{ inm.espacios.length }})</h4>
                @if (inm.espacios && inm.espacios.length > 0) {
                  <table class="tabla-compacta">
                    <thead>
                      <tr><th>Denominación</th><th>Tipo</th><th>Capacidad</th><th>Superficie</th><th>Vinculación Curricular</th></tr>
                    </thead>
                    <tbody>
                      @for (esp of inm.espacios; track esp.id || esp.denominacion) {
                        <tr>
                          <td><strong>{{ esp.denominacion }}</strong></td>
                          <td>{{ esp.tipoEspacioDenominacion || '—' }}</td>
                          <td>{{ esp.capacidadEstudiantes }} est.</td>
                          <td>{{ esp.superficieM2 ?? 0 }} m²</td>
                          <td>
                            @if (esp.id) {
                              <button type="button" class="btn-link" (click)="abrirModalVincular(esp)">Asociar Plan / Asignatura</button>
                            } @else {
                              <span class="muted">Guarde para vincular</span>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                } @else {
                  <p class="muted">Sin espacios físicos cargados en este inmueble.</p>
                }
              </div>
            </article>
          } @empty {
            <p class="estado-vacio">No hay inmuebles maestros registrados.</p>
          }
        </div>
      }

      @if (modalInmuebleAbierto()) {
        <div class="modal-backdrop">
          <div class="modal-card modal-lg">
            <h3>{{ inmuebleEditandoId() ? 'Editar Inmueble Maestro' : 'Nuevo Inmueble Maestro' }}</h3>
            <form [formGroup]="inmuebleForm" (ngSubmit)="guardarInmueble()">
              <div class="grid2">
                <label>Nombre / Denominación *<input type="text" formControlName="nombre" /></label>
                <label>Domicilio *<input type="text" formControlName="domicilio" /></label>
              </div>
              <div class="grid3">
                <label>Sede
                  <select formControlName="sedeId">
                    <option value="">Sin sede asignada</option>
                    @for (s of sedes(); track s.id) { <option [value]="s.id">{{ s.nombre }}</option> }
                  </select>
                </label>
                <label>Condición de tenencia *
                  <select formControlName="condicionTenencia">
                    <option value="PROPIO">Propio</option>
                    <option value="ALQUILADO">Alquilado</option>
                    <option value="COMODATO">Comodato</option>
                    <option value="CEDIDO">Cedido</option>
                  </select>
                </label>
                <label>Superficie Cubierta (m²)<input type="number" formControlName="superficieCubiertaM2" /></label>
              </div>

              <div class="espacios-editor">
                <div class="esp-head">
                  <h4>Espacios Físicos del Inmueble</h4>
                  <button type="button" class="btn-sec" (click)="agregarEspacioDraft()">+ Agregar Espacio</button>
                </div>
                @for (esp of espaciosDraft(); track $index) {
                  <div class="esp-row">
                    <input type="text" [(ngModel)]="esp.denominacion" [ngModelOptions]="{standalone: true}" placeholder="Ej. Aula 101 / Lab. Química" />
                    <select [(ngModel)]="esp.tipoEspacioId" [ngModelOptions]="{standalone: true}">
                      <option value="">Tipo...</option>
                      @for (t of tiposEspacio(); track t.id) { <option [value]="t.id">{{ t.denominacion }}</option> }
                    </select>
                    <input type="number" [(ngModel)]="esp.capacidadEstudiantes" [ngModelOptions]="{standalone: true}" placeholder="Cap." style="width: 85px;" />
                    <input type="number" [(ngModel)]="esp.superficieM2" [ngModelOptions]="{standalone: true}" placeholder="m²" style="width: 85px;" />
                    <button type="button" class="btn-danger-xs" (click)="quitarEspacioDraft($index)">✕</button>
                  </div>
                }
              </div>

              <div class="modal-actions">
                <button type="button" class="btn-sec" (click)="modalInmuebleAbierto.set(false)">Cancelar</button>
                <button type="submit" class="btn-primary-figma" [disabled]="inmuebleForm.invalid || guardando()">
                  {{ guardando() ? 'Guardando...' : 'Guardar Inmueble' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (espacioVinculando()) {
        <div class="modal-backdrop">
          <div class="modal-card modal-lg">
            <h3>Asociar Espacio "{{ espacioVinculando()?.denominacion }}" a Plan y Actividad Curricular</h3>
            <ul class="vinc-list">
              @for (v of vinculacionesActuales(); track v.id) {
                <li>
                  <span><strong>{{ v.carreraNombre }}</strong> · {{ v.planEstudioNombre }} · {{ v.actividadNombre }}</span>
                  <button type="button" class="btn-danger-xs" (click)="quitarVinculacion($index)">Quitar</button>
                </li>
              } @empty {
                <li class="muted">Sin vinculaciones a actividades curriculares aún.</li>
              }
            </ul>

            <div class="grid3 vinc-form">
              <label>Carrera
                <select [ngModel]="vincCarreraId()" (ngModelChange)="onVincCarreraChange($event)">
                  <option value="">Seleccione...</option>
                  @for (c of carreras(); track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
                </select>
              </label>
              <label>Plan de Estudio
                <select [ngModel]="vincPlanId()" (ngModelChange)="onVincPlanChange($event)">
                  <option value="">Seleccione...</option>
                  @for (p of vincPlanes(); track p.id) { <option [value]="p.id">{{ p.codigoResolucion || p.anioAprobacion }}</option> }
                </select>
              </label>
              <label>Actividad Curricular
                <select [ngModel]="vincActividadId()" (ngModelChange)="vincActividadId.set($event)">
                  <option value="">Seleccione...</option>
                  @for (a of vincActividades(); track a.id) { <option [value]="a.id">{{ a.nombre }}</option> }
                </select>
              </label>
            </div>
            <div style="margin-top: 10px;">
              <button type="button" class="btn-sec" (click)="agregarVinculacion()" [disabled]="!vincCarreraId() || !vincPlanId() || !vincActividadId()">+ Agregar Asociación</button>
            </div>

            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="espacioVinculando.set(null)">Cerrar</button>
              <button type="button" class="btn-primary-figma" (click)="guardarVinculaciones()">Guardar Asociaciones</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .vista-gestion-layout { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .vista-toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; }
    .search-box-figma input { width: 100%; padding: 9px 12px; border: 1px solid #d1d5db; border-radius: 6px; }
    .btn-primary-figma { background: #6b2442; color: #fff; border: none; padding: 9px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-sec { background: #f3f4f6; color: #1f2937; border: 1px solid #d1d5db; padding: 7px 12px; border-radius: 6px; cursor: pointer; }
    .btn-danger { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; padding: 7px 12px; border-radius: 6px; cursor: pointer; }
    .btn-danger-xs { background: #fee2e2; color: #991b1b; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; }
    .btn-link { background: none; border: none; color: #6b2442; text-decoration: underline; cursor: pointer; font-weight: 600; padding: 0; }
    .inmuebles-lista { display: flex; flex-direction: column; gap: 16px; }
    .inmueble-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
    .inmueble-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
    .inmueble-head h3 { margin: 0 0 4px; font-size: 18px; }
    .sub { margin: 0; color: #6b7280; font-size: 13px; }
    .acciones { display: flex; gap: 8px; }
    .espacios-wrap h4 { margin: 0 0 8px; font-size: 14px; color: #374151; }
    .tabla-compacta { width: 100%; border-collapse: collapse; font-size: 13px; }
    .tabla-compacta th, .tabla-compacta td { padding: 8px; border-bottom: 1px solid #f3f4f6; text-align: left; }
    .muted { color: #6b7280; font-size: 13px; }
    .estado-vacio { text-align: center; padding: 32px; color: #6b7280; }
    .alert-banner-figma { display: flex; justify-content: space-between; padding: 10px 14px; border-radius: 6px; margin-bottom: 14px; }
    .alert-banner-figma.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .alert-banner-figma.success { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-card { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 760px; max-height: 90vh; overflow-y: auto; }
    .grid2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
    .grid3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 12px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; }
    input, select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
    .espacios-editor { border-top: 1px solid #e5e7eb; padding-top: 12px; margin-top: 12px; }
    .esp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .esp-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-wrap: wrap; }
    .vinc-list { list-style: none; padding: 0; margin: 0 0 14px; display: flex; flex-direction: column; gap: 6px; }
    .vinc-list li { display: flex; justify-content: space-between; align-items: center; padding: 8px; background: #f9fafb; border-radius: 6px; font-size: 13px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminInfraestructuraComponent implements OnInit {
  private readonly autoevalService = inject(AutoevaluacionService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly planService = inject(PlanEstudioService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly guardando = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly busqueda = signal('');

  readonly inmuebles = signal<Inmueble[]>([]);
  readonly sedes = signal<SedeLocalizacionResponse[]>([]);
  readonly tiposEspacio = signal<TipoEspacioFisico[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);

  readonly modalInmuebleAbierto = signal(false);
  readonly inmuebleEditandoId = signal<string | null>(null);
  readonly espaciosDraft = signal<EspacioFisico[]>([]);

  readonly espacioVinculando = signal<EspacioFisico | null>(null);
  readonly vinculacionesActuales = signal<EspacioFisicoActividadCurricularDto[]>([]);
  readonly vincCarreraId = signal('');
  readonly vincPlanId = signal('');
  readonly vincActividadId = signal('');
  readonly vincPlanes = signal<PlanEstudioFt001[]>([]);
  readonly vincActividades = signal<ActividadCurricularPlan[]>([]);

  readonly inmueblesFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (!q) return this.inmuebles();
    return this.inmuebles().filter(
      (i) =>
        i.nombre.toLowerCase().includes(q) ||
        i.domicilio.toLowerCase().includes(q) ||
        (i.sedeNombre || '').toLowerCase().includes(q)
    );
  });

  readonly inmuebleForm = this.fb.group({
    nombre: ['', Validators.required],
    domicilio: ['', Validators.required],
    sedeId: [''],
    condicionTenencia: ['PROPIO', Validators.required],
    superficieCubiertaM2: [0],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.autoevalService.listarInmueblesMaestros().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.inmuebles.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Error al cargar infraestructura maestra: ' + (err.error?.message || err.message));
        this.loading.set(false);
      },
    });
    this.autoevalService.listarTiposEspacio().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((t) => this.tiposEspacio.set(t));
    this.estructuraService.listarSedes().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((s) => this.sedes.set(s));
    this.estructuraService.listarCarreras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((c) => this.carreras.set(c));
  }

  abrirModalNuevo(): void {
    this.inmuebleEditandoId.set(null);
    this.inmuebleForm.reset({ nombre: '', domicilio: '', sedeId: '', condicionTenencia: 'PROPIO', superficieCubiertaM2: 0 });
    this.espaciosDraft.set([]);
    this.modalInmuebleAbierto.set(true);
  }

  abrirModalEditar(inm: Inmueble): void {
    this.inmuebleEditandoId.set(inm.id);
    this.inmuebleForm.patchValue({
      nombre: inm.nombre,
      domicilio: inm.domicilio,
      sedeId: inm.sedeId || '',
      condicionTenencia: inm.condicionTenencia || 'PROPIO',
      superficieCubiertaM2: inm.superficieCubiertaM2 ?? 0,
    });
    this.espaciosDraft.set((inm.espacios || []).map((e) => ({ ...e })));
    this.modalInmuebleAbierto.set(true);
  }

  agregarEspacioDraft(): void {
    this.espaciosDraft.update((list) => [
      ...list,
      { denominacion: '', tipoEspacioId: this.tiposEspacio()[0]?.id || '', capacidadEstudiantes: 30, superficieM2: 40 },
    ]);
  }

  quitarEspacioDraft(idx: number): void {
    this.espaciosDraft.update((list) => list.filter((_, i) => i !== idx));
  }

  guardarInmueble(): void {
    if (this.inmuebleForm.invalid) return;
    const v = this.inmuebleForm.value;
    const payload: RegistrarInmuebleRequest = {
      nombre: v.nombre!,
      domicilio: v.domicilio!,
      sedeId: v.sedeId || undefined,
      propiedadInstitucion: v.condicionTenencia === 'PROPIO',
      condicionTenencia: v.condicionTenencia!,
      superficieCubiertaM2: Number(v.superficieCubiertaM2) || 0,
      esMaestro: true,
      vinculaTodasCarreras: true,
      carrerasIds: [],
      espacios: this.espaciosDraft()
        .filter((e) => e.denominacion?.trim())
        .map((e) => ({
          id: e.id,
          tipoEspacioId: e.tipoEspacioId || undefined,
          denominacion: e.denominacion.trim(),
          capacidadEstudiantes: Number(e.capacidadEstudiantes) || 0,
          superficieM2: Number(e.superficieM2) || 0,
        })),
    };

    this.guardando.set(true);
    const editId = this.inmuebleEditandoId();
    const obs = editId
      ? this.autoevalService.actualizarInmueble(editId, payload)
      : this.autoevalService.registrarInmueble(payload);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalInmuebleAbierto.set(false);
        this.successMessage.set('Inmueble maestro guardado correctamente.');
        this.cargarDatos();
      },
      error: (err: any) => {
        this.guardando.set(false);
        this.errorMessage.set('Error al guardar inmueble: ' + (err.error?.message || err.message));
      },
    });
  }

  eliminarInmueble(inm: Inmueble): void {
    if (!confirm(`¿Eliminar el inmueble maestro "${inm.nombre}"?`)) return;
    this.autoevalService.eliminarInmueble(inm.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set('Inmueble eliminado.');
        this.cargarDatos();
      },
      error: (err) => this.errorMessage.set('No se pudo eliminar: ' + (err.error?.message || err.message)),
    });
  }

  abrirModalVincular(esp: EspacioFisico): void {
    if (!esp.id) return;
    this.espacioVinculando.set(esp);
    this.vincCarreraId.set('');
    this.vincPlanId.set('');
    this.vincActividadId.set('');
    this.vincPlanes.set([]);
    this.vincActividades.set([]);
    this.autoevalService.obtenerActividadesPorEspacio(esp.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => this.vinculacionesActuales.set(list),
      error: () => this.vinculacionesActuales.set([]),
    });
  }

  onVincCarreraChange(carreraId: string): void {
    this.vincCarreraId.set(carreraId);
    this.vincPlanId.set('');
    this.vincActividadId.set('');
    this.vincActividades.set([]);
    if (!carreraId) {
      this.vincPlanes.set([]);
      return;
    }
    this.planService.listarPlanes(carreraId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ps) => this.vincPlanes.set(ps));
  }

  onVincPlanChange(planId: string): void {
    this.vincPlanId.set(planId);
    this.vincActividadId.set('');
    if (!planId) {
      this.vincActividades.set([]);
      return;
    }
    this.planService.listarAsignaturas(planId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((as) => this.vincActividades.set(as));
  }

  agregarVinculacion(): void {
    const c = this.carreras().find((x) => x.id === this.vincCarreraId());
    const p = this.vincPlanes().find((x) => x.id === this.vincPlanId());
    const a = this.vincActividades().find((x) => x.id === this.vincActividadId());
    const esp = this.espacioVinculando();
    if (!c || !p?.id || !a?.id || !esp?.id) return;

    if (this.vinculacionesActuales().some((v) => v.planEstudioId === p.id && v.actividadCurricularId === a.id)) return;

    this.vinculacionesActuales.update((list) => [
      ...list,
      {
        id: crypto.randomUUID(),
        espacioFisicoId: esp.id!,
        carreraId: c.id,
        carreraNombre: c.nombre,
        planEstudioId: p.id!,
        planEstudioNombre: p.codigoResolucion || `Plan ${p.anioAprobacion ?? ''}`,
        actividadCurricularId: a.id!,
        actividadNombre: a.nombre,
      },
    ]);
  }

  quitarVinculacion(idx: number): void {
    this.vinculacionesActuales.update((list) => list.filter((_, i) => i !== idx));
  }

  guardarVinculaciones(): void {
    const esp = this.espacioVinculando();
    if (!esp?.id) return;
    const req: AsignarActividadEspacioRequest[] = this.vinculacionesActuales().map((v) => ({
      carreraId: v.carreraId,
      planEstudioId: v.planEstudioId,
      actividadCurricularId: v.actividadCurricularId,
      comisionesAsignadas: v.comisionesAsignadas,
      observacionesUso: v.observacionesUso,
    }));
    this.autoevalService.guardarActividadesPorEspacio(esp.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.espacioVinculando.set(null);
        this.successMessage.set('Asociaciones curriculares del espacio físico guardadas.');
      },
      error: (err) => this.errorMessage.set('Error al guardar asociaciones: ' + (err.error?.message || err.message)),
    });
  }
}
