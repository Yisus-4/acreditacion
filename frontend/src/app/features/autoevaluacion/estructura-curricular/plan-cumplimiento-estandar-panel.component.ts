import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CumplimientoCargaHorariaPlan } from '../../../core/estandares/estandar.models';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';

@Component({
  selector: 'app-plan-cumplimiento-estandar-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (cargando()) {
      <div class="cump-panel muted">Calculando cumplimiento horario del plan respecto al estándar...</div>
    } @else if (datos(); as d) {
      @if (d.habilitar311 || d.habilitar312) {
        <section class="cump-panel">
          <div class="cump-head">
            <div>
              <h4>Verificación de Carga Horaria del Plan vs. Estándar (Sección 3)</h4>
              <p class="sub">
                Estándar: <strong>{{ d.estandarNombre }}</strong> ·
                Cobertura: <strong>{{ d.asignaturasConSeccion3Cargada }} / {{ d.totalAsignaturasSeDicta }}</strong>
                actividades curriculares (<em>SE DICTA</em>) con Sección 3 completa.
              </p>
            </div>
            <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
              <span class="semaforo" [class.ok]="d.cumpleGlobal" [class.warn]="!d.cumpleGlobal">
                {{ d.cumpleGlobal ? '✓ Cumple mínimos horarios del estándar' : '⚠ Existen faltantes respecto al estándar' }}
              </span>
              <button type="button" class="btn-toggle" (click)="expandido.set(!expandido())">
                {{ expandido() ? 'Ocultar detalle' : 'Ver detalle' }}
              </button>
            </div>
          </div>

          @if (expandido()) {
            <div class="cump-grid">
              @if (d.habilitar311) {
                <div class="cump-block">
                  <h5>
                    3.1.1 — {{ d.modo311 === 'BLOQUES_CURRICULARES' ? 'Bloques curriculares' : 'Áreas temáticas / otra formación' }}
                  </h5>
                  <table class="cump-table">
                    <thead>
                      <tr>
                        <th>{{ d.modo311 === 'BLOQUES_CURRICULARES' ? 'Bloque' : 'Área' }}</th>
                        <th class="num">Pres.</th>
                        <th class="num">Dist.</th>
                        <th class="num">Acumulado</th>
                        <th class="num">Mín. Carrera</th>
                        <th class="estado">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (f of d.filas311; track f.areaId) {
                        <tr>
                          <td>{{ f.nombre }}</td>
                          <td class="num">{{ f.horasPresenciales }} h</td>
                          <td class="num">{{ f.horasDistancia }} h</td>
                          <td class="num"><strong>{{ f.horasAcumuladas }} h</strong></td>
                          <td class="num">{{ f.tieneMinimo ? (f.horasMinimasCarrera + ' h') : '—' }}</td>
                          <td class="estado">
                            @if (!f.tieneMinimo) {
                              <span class="pill neutral">Sin mínimo</span>
                            } @else if (f.cumple) {
                              <span class="pill ok">Cumple (+{{ f.diferencia }} h)</span>
                            } @else {
                              <span class="pill bad">Faltan {{ -(f.diferencia || 0) }} h</span>
                            }
                          </td>
                        </tr>
                      }
                      <tr class="total-row">
                        <td><strong>Total Global Carrera (3.1.1)</strong></td>
                        <td class="num"><strong>{{ d.horasPresencialesTotal311 }} h</strong></td>
                        <td class="num"><strong>{{ d.horasDistanciaTotal311 }} h</strong></td>
                        <td class="num"><strong>{{ d.horasAcumuladasTotal311 }} h</strong></td>
                        <td class="num"><strong>{{ d.tieneMinimoTotal311 ? (d.horasMinimasTotal311 + ' h') : '—' }}</strong></td>
                        <td class="estado">
                          @if (!d.tieneMinimoTotal311) {
                            <span class="pill neutral">Sin mínimo</span>
                          } @else if (d.cumpleTotal311) {
                            <span class="pill ok">Cumple (+{{ d.diferenciaTotal311 }} h)</span>
                          } @else {
                            <span class="pill bad">Faltan {{ -(d.diferenciaTotal311 || 0) }} h</span>
                          }
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              }

              @if (d.habilitar312) {
                <div class="cump-block">
                  <h5>
                    3.1.2 — {{ d.modo312 === 'MODALIDADES_PRACTICAS' ? 'Modalidades de formación práctica' : 'Áreas de formación práctica' }}
                  </h5>
                  <table class="cump-table">
                    <thead>
                      <tr>
                        <th>{{ d.modo312 === 'MODALIDADES_PRACTICAS' ? 'Modalidad' : 'Área práctica' }}</th>
                        <th class="num">Pres.</th>
                        <th class="num">Dist.</th>
                        <th class="num">Acumulado</th>
                        <th class="num">Mín. Carrera</th>
                        <th class="estado">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (f of d.filas312; track f.areaId) {
                        <tr>
                          <td>{{ f.nombre }}</td>
                          <td class="num">{{ f.horasPresenciales }} h</td>
                          <td class="num">{{ f.horasDistancia }} h</td>
                          <td class="num"><strong>{{ f.horasAcumuladas }} h</strong></td>
                          <td class="num">{{ f.tieneMinimo ? (f.horasMinimasCarrera + ' h') : '—' }}</td>
                          <td class="estado">
                            @if (!f.tieneMinimo) {
                              <span class="pill neutral">Sin mínimo</span>
                            } @else if (f.cumple) {
                              <span class="pill ok">Cumple (+{{ f.diferencia }} h)</span>
                            } @else {
                              <span class="pill bad">Faltan {{ -(f.diferencia || 0) }} h</span>
                            }
                          </td>
                        </tr>
                      }
                      <tr class="total-row">
                        <td><strong>Total Global Práctica (3.1.2)</strong></td>
                        <td class="num"><strong>{{ d.horasPresencialesTotal312 }} h</strong></td>
                        <td class="num"><strong>{{ d.horasDistanciaTotal312 }} h</strong></td>
                        <td class="num"><strong>{{ d.horasAcumuladasTotal312 }} h</strong></td>
                        <td class="num"><strong>{{ d.tieneMinimoTotal312 ? (d.horasMinimasTotal312 + ' h') : '—' }}</strong></td>
                        <td class="estado">
                          @if (!d.tieneMinimoTotal312) {
                            <span class="pill neutral">Sin mínimo</span>
                          } @else if (d.cumpleTotal312) {
                            <span class="pill ok">Cumple (+{{ d.diferenciaTotal312 }} h)</span>
                          } @else {
                            <span class="pill bad">Faltan {{ -(d.diferenciaTotal312 || 0) }} h</span>
                          }
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              }
            </div>
          }
        </section>
      }
    }
  `,
  styles: [`
    .cump-panel { background: #fdfafb; border: 1px solid #e5dde1; border-radius: 8px; padding: 14px 16px; margin: 14px 0; }
    .cump-panel.muted { font-size: 13px; color: #6b7280; }
    .cump-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
    h4 { margin: 0; font-size: 15px; color: #3d2c35; }
    h5 { margin: 0 0 8px; font-size: 13px; color: #6b4c5b; font-weight: 700; }
    .sub { margin: 4px 0 0; font-size: 12.5px; color: #5a4a53; }
    .semaforo { padding: 4px 11px; border-radius: 999px; font-size: 12px; font-weight: 700; }
    .semaforo.ok { background: #eef5f0; color: #2d5a3d; border: 1px solid #c6dfd0; }
    .semaforo.warn { background: #fff7ed; color: #9a3412; border: 1px solid #fed7aa; }
    .btn-toggle { background: #fff; color: #5a4a53; border: 1px solid #d1d5db; padding: 4px 10px; border-radius: 6px; font-size: 12px; cursor: pointer; font-weight: 600; }
    .cump-grid { display: flex; flex-direction: column; gap: 14px; margin-top: 12px; }
    .cump-table { width: 100%; border-collapse: collapse; font-size: 12.5px; background: #fff; }
    .cump-table th { background: #f3ebef; color: #3d2c35; font-weight: 700; padding: 7px 9px; border: 1px solid #e5dde1; text-align: left; }
    .cump-table td { padding: 6px 9px; border: 1px solid #ece4e8; color: #1f1a1d; }
    .cump-table th.num, .cump-table td.num { text-align: right; white-space: nowrap; }
    .cump-table th.estado, .cump-table td.estado { text-align: center; white-space: nowrap; }
    .cump-table tr.total-row td { background: #f7f3f5; }
    .pill { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
    .pill.ok { background: #eef5f0; color: #2d5a3d; border: 1px solid #c6dfd0; }
    .pill.bad { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .pill.neutral { background: #f3f4f6; color: #6b7280; border: 1px solid #e5e7eb; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanCumplimientoEstandarPanelComponent {
  private readonly planService = inject(PlanEstudioService);
  private readonly destroyRef = inject(DestroyRef);

  readonly planId = input<string | null | undefined>(null);
  readonly refreshKey = input<number>(0);

  readonly datos = signal<CumplimientoCargaHorariaPlan | null>(null);
  readonly cargando = signal<boolean>(false);
  readonly expandido = signal<boolean>(true);

  constructor() {
    effect(() => {
      const id = this.planId();
      this.refreshKey(); // subscribe to refresh ticks
      if (!id) {
        this.datos.set(null);
        return;
      }
      this.cargar(id);
    });
  }

  private cargar(planId: string): void {
    this.cargando.set(true);
    this.planService
      .obtenerCumplimientoEstandar(planId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.datos.set(res);
          this.cargando.set(false);
        },
        error: () => {
          this.datos.set(null);
          this.cargando.set(false);
        },
      });
  }
}
