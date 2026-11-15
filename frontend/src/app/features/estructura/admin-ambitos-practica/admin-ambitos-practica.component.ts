import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AmbitoPracticaService } from '../../../core/ambitos-practica/ambito-practica.service';
import { AmbitoPractica, TipoCategoriaAmbito } from '../../../core/ambitos-practica/ambito-practica.models';
import { ConvenioService } from '../../../core/convenios/convenio.service';
import { ConvenioInstitucional } from '../../../core/convenios/convenio.models';
import { SubnavGestionInstitucionalComponent } from '../subnav-gestion-institucional/subnav-gestion-institucional.component';

@Component({
  selector: 'app-admin-ambitos-practica',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SubnavGestionInstitucionalComponent],
  template: `
    <div class="vista-gestion-layout">
      <app-subnav-gestion-institucional seccionActual="Ámbitos de práctica"></app-subnav-gestion-institucional>

      @if (errorMessage()) {
        <div class="alert-banner error">
          <span>{{ errorMessage() }}</span>
          <button type="button" (click)="errorMessage.set(null)">✕</button>
        </div>
      }
      @if (successMessage()) {
        <div class="alert-banner success">
          <span>{{ successMessage() }}</span>
          <button type="button" (click)="successMessage.set(null)">✕</button>
        </div>
      }

      <div class="tabs-row">
        <button type="button" [class.on]="subTab() === 'ambitos'" (click)="subTab.set('ambitos')">Ámbitos de Práctica (RG-006)</button>
        <button type="button" [class.on]="subTab() === 'convenios'" (click)="subTab.set('convenios')">Convenios Institucionales (RG-007)</button>
      </div>

      @if (subTab() === 'ambitos') {
        <div class="toolbar">
          <input type="text" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" (keyup.enter)="cargarAmbitos()" placeholder="Buscar ámbito por denominación o localidad..." />
          <button type="button" class="btn-pri" (click)="abrirModalAmbito()">+ Nuevo Ámbito Maestro</button>
        </div>
        <table class="tabla">
          <thead><tr><th>Denominación</th><th>Categoría</th><th>Localidad</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (a of ambitos(); track a.id) {
              <tr>
                <td><strong>{{ a.denominacion }}</strong></td>
                <td>{{ a.tipoCategoria }}</td>
                <td>{{ a.localidad || '—' }}</td>
                <td>{{ a.activo !== false ? 'Activo' : 'Inactivo' }}</td>
                <td class="acciones">
                  <button type="button" class="btn-sec" (click)="abrirModalAmbito(a)">Editar</button>
                  <button type="button" class="btn-danger" (click)="eliminarAmbito(a)">Eliminar</button>
                </td>
              </tr>
            } @empty { <tr><td colspan="5" class="empty">Sin ámbitos de práctica registrados.</td></tr> }
          </tbody>
        </table>
      } @else {
        <div class="toolbar">
          <input type="text" [ngModel]="busquedaConv()" (ngModelChange)="busquedaConv.set($event)" (keyup.enter)="cargarConvenios()" placeholder="Buscar convenio por contraparte u objeto..." />
          <button type="button" class="btn-pri" (click)="abrirModalConvenio()">+ Nuevo Convenio</button>
        </div>
        <table class="tabla">
          <thead><tr><th>Contraparte</th><th>Tipo</th><th>Vigencia</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (c of convenios(); track c.id) {
              <tr>
                <td><strong>{{ c.contraparte }}</strong></td>
                <td>{{ c.tipoConvenio || '—' }}</td>
                <td>{{ c.fechaCelebracion || '—' }} {{ c.fechaCese ? '→ ' + c.fechaCese : '' }}</td>
                <td>{{ c.vigente ? 'Vigente' : 'No vigente' }}</td>
                <td class="acciones">
                  <button type="button" class="btn-sec" (click)="abrirModalConvenio(c)">Editar</button>
                  <button type="button" class="btn-danger" (click)="eliminarConvenio(c)">Eliminar</button>
                </td>
              </tr>
            } @empty { <tr><td colspan="5" class="empty">Sin convenios registrados.</td></tr> }
          </tbody>
        </table>
      }

      @if (modalAmbito()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ ambitoEditId() ? 'Editar Ámbito de Práctica' : 'Nuevo Ámbito de Práctica' }}</h3>
            <form [formGroup]="ambitoForm" (ngSubmit)="guardarAmbito()">
              <label>Denominación *<input type="text" formControlName="denominacion" /></label>
              <label>Categoría *
                <select formControlName="tipoCategoria">
                  <option value="INTERNO">Interno</option>
                  <option value="EXTERNO">Externo</option>
                  <option value="UNIDAD_DEMOSTRATIVA">Unidad Demostrativa</option>
                  <option value="CAM">Campo / CAM</option>
                </select>
              </label>
              <div class="grid2">
                <label>Localidad<input type="text" formControlName="localidad" /></label>
                <label>Dirección<input type="text" formControlName="direccion" /></label>
              </div>
              <div class="modal-actions">
                <button type="button" class="btn-sec" (click)="modalAmbito.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="ambitoForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (modalConvenio()) {
        <div class="modal-bg">
          <div class="modal-box">
            <h3>{{ convenioEditId() ? 'Editar Convenio' : 'Nuevo Convenio Institucional' }}</h3>
            <form [formGroup]="convenioForm" (ngSubmit)="guardarConvenio()">
              <div class="grid2">
                <label>Expediente *<input type="text" formControlName="expediente" placeholder="EXP-001/2026" /></label>
                <label>Contraparte *<input type="text" formControlName="contraparte" /></label>
              </div>
              <div class="grid2">
                <label>Tipo de Convenio<input type="text" formControlName="tipoConvenio" placeholder="ESPECIFICO" /></label>
                <label>Fecha Celebración<input type="date" formControlName="fechaCelebracion" /></label>
              </div>
              <label>Objeto<input type="text" formControlName="objeto" /></label>
              <label class="chk"><input type="checkbox" formControlName="vigente" /> Convenio vigente</label>
              <div class="modal-actions">
                <button type="button" class="btn-sec" (click)="modalConvenio.set(false)">Cancelar</button>
                <button type="submit" class="btn-pri" [disabled]="convenioForm.invalid">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .vista-gestion-layout { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .tabs-row { display: flex; gap: 10px; margin-bottom: 16px; }
    .tabs-row button { padding: 8px 14px; border: 1px solid #d1d5db; background: #fff; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .tabs-row button.on { background: #6b2442; color: #fff; border-color: #6b2442; }
    .toolbar { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
    .toolbar input { flex: 1; padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 6px; }
    .tabla { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
    .tabla th, .tabla td { padding: 10px 12px; border-bottom: 1px solid #f3f4f6; text-align: left; font-size: 14px; }
    .acciones { display: flex; gap: 8px; }
    .btn-pri { background: #6b2442; color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-sec { background: #f3f4f6; color: #1f2937; border: 1px solid #d1d5db; padding: 6px 12px; border-radius: 6px; cursor: pointer; }
    .btn-danger { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; padding: 6px 12px; border-radius: 6px; cursor: pointer; }
    .empty { text-align: center; padding: 24px; color: #6b7280; }
    .alert-banner { display: flex; justify-content: space-between; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px; }
    .alert-banner.error { background: #fef2f2; color: #991b1b; }
    .alert-banner.success { background: #ecfdf5; color: #065f46; }
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 560px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
    label.chk { flex-direction: row; align-items: center; gap: 8px; }
    input, select { padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; }
    .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAmbitosPracticaComponent implements OnInit {
  private readonly ambitoService = inject(AmbitoPracticaService);
  private readonly convenioService = inject(ConvenioService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly subTab = signal<'ambitos' | 'convenios'>('ambitos');
  readonly busqueda = signal('');
  readonly busquedaConv = signal('');
  readonly ambitos = signal<AmbitoPractica[]>([]);
  readonly convenios = signal<ConvenioInstitucional[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalAmbito = signal(false);
  readonly ambitoEditId = signal<string | null>(null);
  readonly modalConvenio = signal(false);
  readonly convenioEditId = signal<string | null>(null);

  readonly ambitoForm = this.fb.group({
    denominacion: ['', Validators.required],
    tipoCategoria: ['INTERNO' as TipoCategoriaAmbito, Validators.required],
    localidad: [''],
    direccion: [''],
  });

  readonly convenioForm = this.fb.group({
    expediente: ['', Validators.required],
    contraparte: ['', Validators.required],
    tipoConvenio: ['ESPECIFICO'],
    fechaCelebracion: [new Date().toISOString().slice(0, 10)],
    objeto: [''],
    vigente: [true],
  });

  ngOnInit(): void {
    this.cargarAmbitos();
    this.cargarConvenios();
  }

  cargarAmbitos(): void {
    this.ambitoService.listarAmbitos(undefined, this.busqueda() || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => this.ambitos.set(list),
      error: (err) => this.errorMessage.set('Error al cargar ámbitos: ' + (err.error?.message || err.message)),
    });
  }

  cargarConvenios(): void {
    this.convenioService.listarConvenios(undefined, this.busquedaConv() || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => this.convenios.set(list),
      error: (err) => this.errorMessage.set('Error al cargar convenios: ' + (err.error?.message || err.message)),
    });
  }

  abrirModalAmbito(a?: AmbitoPractica): void {
    this.ambitoEditId.set(a?.id || null);
    this.ambitoForm.reset({
      denominacion: a?.denominacion || '',
      tipoCategoria: a?.tipoCategoria || 'INTERNO',
      localidad: a?.localidad || '',
      direccion: a?.direccion || '',
    });
    this.modalAmbito.set(true);
  }

  guardarAmbito(): void {
    if (this.ambitoForm.invalid) return;
    const v = this.ambitoForm.value;
    const dto: AmbitoPractica = {
      denominacion: v.denominacion!,
      tipoCategoria: v.tipoCategoria as TipoCategoriaAmbito,
      localidad: v.localidad || undefined,
      direccion: v.direccion || undefined,
      activo: true,
    };
    const id = this.ambitoEditId();
    const obs = id ? this.ambitoService.actualizarAmbito(id, dto) : this.ambitoService.crearAmbito(dto);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalAmbito.set(false);
        this.successMessage.set('Ámbito de práctica guardado.');
        this.cargarAmbitos();
      },
      error: (err) => this.errorMessage.set('Error al guardar ámbito: ' + (err.error?.message || err.message)),
    });
  }

  eliminarAmbito(a: AmbitoPractica): void {
    if (!a.id || !confirm(`¿Eliminar el ámbito "${a.denominacion}"?`)) return;
    this.ambitoService.eliminarAmbito(a.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set('Ámbito eliminado.');
        this.cargarAmbitos();
      },
      error: (err) => this.errorMessage.set('Error al eliminar ámbito: ' + (err.error?.message || err.message)),
    });
  }

  abrirModalConvenio(c?: ConvenioInstitucional): void {
    this.convenioEditId.set(c?.id || null);
    this.convenioForm.reset({
      expediente: c?.expediente || '',
      contraparte: c?.contraparte || '',
      tipoConvenio: c?.tipoConvenio || 'ESPECIFICO',
      fechaCelebracion: c?.fechaCelebracion || new Date().toISOString().slice(0, 10),
      objeto: c?.objeto || '',
      vigente: c?.vigente ?? true,
    });
    this.modalConvenio.set(true);
  }

  guardarConvenio(): void {
    if (this.convenioForm.invalid) return;
    const v = this.convenioForm.value;
    const dto: ConvenioInstitucional = {
      expediente: v.expediente || 'S/N',
      contraparte: v.contraparte!,
      tipoConvenio: v.tipoConvenio || 'ESPECIFICO',
      fechaCelebracion: v.fechaCelebracion || new Date().toISOString().slice(0, 10),
      objeto: v.objeto || '',
      vigente: v.vigente ?? true,
    };
    const id = this.convenioEditId();
    const obs = id ? this.convenioService.actualizarConvenio(id, dto) : this.convenioService.crearConvenio(dto);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.modalConvenio.set(false);
        this.successMessage.set('Convenio guardado.');
        this.cargarConvenios();
      },
      error: (err) => this.errorMessage.set('Error al guardar convenio: ' + (err.error?.message || err.message)),
    });
  }

  eliminarConvenio(c: ConvenioInstitucional): void {
    if (!c.id || !confirm(`¿Eliminar el convenio con "${c.contraparte}"?`)) return;
    this.convenioService.eliminarConvenio(c.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set('Convenio eliminado.');
        this.cargarConvenios();
      },
      error: (err) => this.errorMessage.set('Error al eliminar convenio: ' + (err.error?.message || err.message)),
    });
  }
}
