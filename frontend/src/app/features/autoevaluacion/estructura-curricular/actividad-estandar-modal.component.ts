import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import {
  AreaCargaHorariaEstandar,
  FichaEstandarActividad,
  Grupo22Estandar,
  GuardarFichaEstandarRequest,
} from '../../../core/estandares/estandar.models';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';

interface CargaDraftRow {
  tp: number;
  td: number;
  pp: number;
  pd: number;
}

@Component({
  selector: 'app-actividad-estandar-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-bg" (click)="cerrar.emit()">
      <div class="modal-box modal-estandar" (click)="$event.stopPropagation()">
        <div class="modal-head">
          <div>
            <h3>Ficha del Estándar — Actividad Curricular</h3>
            @if (ficha(); as f) {
              <p class="sub">
                <strong>{{ f.asignaturaCodigo ? f.asignaturaCodigo + ' — ' : '' }}{{ f.asignaturaNombre }}</strong>
                · Carga horaria total: <strong>{{ f.asignaturaHorasReloj ?? 0 }} h reloj</strong>
                · Estándar: <strong>{{ f.estandar.nombre }}</strong>
                @if (f.docenteResponsableNombre) {
                  · Docente responsable: <strong>{{ f.docenteResponsableNombre }}</strong>
                }
              </p>
            }
          </div>
          <button type="button" class="btn-close" (click)="cerrar.emit()">✕</button>
        </div>

        @if (cargando()) {
          <div class="loading-box">Cargando configuración del estándar y respuestas...</div>
        } @else if (ficha(); as f) {
          <div class="modal-body-scroll">
            @if (!f.puedeEditar) {
              <div class="alert info">
                <span>Modo de solo lectura: solo el docente responsable asignado o la gestión académica puede editar esta ficha.</span>
              </div>
            }

            <!-- SECCIÓN 2.2 -->
            <section class="sec-card">
              <div class="sec-title-bar">
                <h4>2.2. {{ f.estandar.tipoSeccion22 === 'EJES' ? 'Ejes y enunciados multidimensionales y transversales' : f.estandar.tipoSeccion22 === 'BLOQUES' ? 'Bloques y criterios de evaluación' : f.estandar.tipoSeccion22 === 'NINGUNA' ? 'Criterios transversales (No aplica)' : 'Contenidos, competencias y habilidades' }}</h4>
                <span class="status-pill" [class.ok]="f.seccion22Completa" [class.pend]="!f.seccion22Completa">
                  {{ f.seccion22Completa ? 'Completa' : 'Pendiente' }}
                </span>
              </div>

              @if (f.estandar.tipoSeccion22 === 'NINGUNA' || f.estandar.tipoEvaluacion22 === 'NINGUNA') {
                <p class="muted">Este estándar no requiere evaluación en la Sección 2.2.</p>
              } @else {
                @if (f.estandar.instrucciones22) {
                  <p class="inst">{{ f.estandar.instrucciones22 }}</p>
                }

                @if (f.estandar.tipoEvaluacion22 === 'MATRIZ_MULTIPLE') {
                  <div class="ref-box">
                    <div class="ref-title">Valores de referencia:</div>
                    @for (esc of f.estandar.escalas; track esc.id) {
                      <div class="ref-item">
                        <strong>- {{ esc.etiqueta }}:</strong> {{ esc.descripcionReferencia || 'Nivel de logro' }}
                      </div>
                    }
                  </div>
                }

                @for (grp of gruposHabilitados(); track grp.id) {
                  <div class="grp-wrap">
                    <h5 class="grp-title">{{ grp.titulo }}</h5>
                    @if (grp.descripcion) {
                      <p class="muted">{{ grp.descripcion }}</p>
                    }

                    @if (f.estandar.tipoEvaluacion22 === 'MATRIZ_MULTIPLE') {
                      <table class="coneau-table">
                        <thead>
                          <tr>
                            <th>Contenidos y habilidades</th>
                            @for (esc of f.estandar.escalas; track esc.id) {
                              <th class="col-center">{{ esc.etiqueta }}</th>
                            }
                          </tr>
                        </thead>
                        <tbody>
                          @for (it of grp.items; track it.id; let idx = $index) {
                            <tr>
                              <td>{{ it.orden || idx + 1 }}. {{ it.enunciado }}</td>
                              @for (esc of f.estandar.escalas; track esc.id) {
                                <td class="col-center">
                                  <input
                                    type="checkbox"
                                    [disabled]="!f.puedeEditar"
                                    [checked]="isMatrixChecked(it.id!, esc.id!)"
                                    (change)="toggleMatrix(it.id!, esc.id!)"
                                  />
                                </td>
                              }
                            </tr>
                          }
                        </tbody>
                      </table>
                    } @else {
                      <table class="coneau-table">
                        <tbody>
                          @for (it of grp.items; track it.id; let idx = $index) {
                            <tr>
                              <td>{{ it.orden || idx + 1 }}. {{ it.enunciado }}</td>
                              <td style="width: 170px;">
                                <select
                                  class="scale-select"
                                  [disabled]="!f.puedeEditar"
                                  [ngModel]="getSingleScale(it.id!)"
                                  (ngModelChange)="setSingleScale(it.id!, $event)"
                                >
                                  @for (esc of f.estandar.escalas; track esc.id) {
                                    <option [value]="esc.id">{{ esc.etiqueta }}</option>
                                  }
                                </select>
                              </td>
                            </tr>
                          }
                        </tbody>
                      </table>
                    }
                  </div>
                }
              }
            </section>

            <!-- SECCIÓN 3 -->
            <section class="sec-card">
              <div class="sec-title-bar">
                <h4>3. Clasificación y carga horaria (en horas reloj)</h4>
                <span class="status-pill" [class.ok]="f.seccion3Completa" [class.pend]="!f.seccion3Completa">
                  {{ f.seccion3Completa ? 'Completa' : 'Pendiente' }}
                </span>
              </div>

              @if (filas311().length === 0 && filas312().length === 0) {
                <p class="muted">No hay filas temáticas o bloques habilitados en este estándar.</p>
              } @else {
                <!-- 3.1.1 -->
                <div class="subsec">
                  <h5>3.1.1. Clasificación de la carga horaria total de la asignatura según sus contenidos</h5>
                  <p class="muted">
                    La suma de cargas horarias parciales debe coincidir con la carga horaria total de la asignatura
                    (<strong>{{ f.asignaturaHorasReloj ?? 0 }} h reloj</strong>).
                  </p>
                  <table class="coneau-table">
                    <thead>
                      <tr>
                        <th rowspan="2">{{ f.estandar.tipoSeccion22 === 'EJES' ? 'Bloques' : 'Área temática / otra formación' }}</th>
                        <th colspan="2" class="col-center">Carga horaria</th>
                      </tr>
                      <tr>
                        <th class="col-center" style="width: 130px;">Presencial</th>
                        <th class="col-center" style="width: 130px;">A distancia</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (area of filas311(); track area.id) {
                        <tr>
                          <td>{{ area.nombre }}</td>
                          <td class="col-center">
                            <input
                              type="number"
                              min="0"
                              class="num-input"
                              [disabled]="!f.puedeEditar"
                              [ngModel]="getCarga(area.id!).tp"
                              (ngModelChange)="updateCarga(area.id!, 'tp', $event)"
                            />
                          </td>
                          <td class="col-center">
                            <input
                              type="number"
                              min="0"
                              class="num-input"
                              [disabled]="!f.puedeEditar"
                              [ngModel]="getCarga(area.id!).td"
                              (ngModelChange)="updateCarga(area.id!, 'td', $event)"
                            />
                          </td>
                        </tr>
                      }
                      <tr class="total-row">
                        <td>
                          <strong>Carga horaria total</strong> (Suma Presencial + Distancia: <strong>{{ total311Sum() }} h</strong>)
                          @if ((f.asignaturaHorasReloj ?? 0) > 0) {
                            @if (diferenciaHorasReloj() === 0) {
                              <span class="match-ok">· Coincide con {{ f.asignaturaHorasReloj }} h</span>
                            } @else {
                              <span class="match-bad">· Diferencia: {{ diferenciaHorasReloj() }} h respecto de {{ f.asignaturaHorasReloj }} h</span>
                            }
                          }
                        </td>
                        <td class="col-center"><strong>{{ total311Pres() }}</strong></td>
                        <td class="col-center"><strong>{{ total311Dist() }}</strong></td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- 3.1.2 -->
                @if (filas312().length > 0) {
                  <div class="subsec">
                    <h5>
                      3.1.2. Carga horaria total dedicada a actividades de formación práctica (Anexo III)
                    </h5>
                    <table class="coneau-table">
                      <thead>
                        <tr>
                          <th rowspan="2">{{ f.estandar.tipoSeccion22 === 'EJES' ? 'Modalidad de formación práctica' : 'Área temática' }}</th>
                          <th colspan="2" class="col-center">Carga horaria</th>
                        </tr>
                        <tr>
                          <th class="col-center" style="width: 130px;">Presencial</th>
                          <th class="col-center" style="width: 130px;">A distancia</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (area of filas312(); track area.id) {
                          <tr>
                            <td>{{ area.etiquetaPractica || area.nombre }}</td>
                            <td class="col-center">
                              <input
                                type="number"
                                min="0"
                                class="num-input"
                                [disabled]="!f.puedeEditar"
                                [ngModel]="getCarga(area.id!).pp"
                                (ngModelChange)="updateCarga(area.id!, 'pp', $event)"
                              />
                            </td>
                            <td class="col-center">
                              <input
                                type="number"
                                min="0"
                                class="num-input"
                                [disabled]="!f.puedeEditar"
                                [ngModel]="getCarga(area.id!).pd"
                                (ngModelChange)="updateCarga(area.id!, 'pd', $event)"
                              />
                            </td>
                          </tr>
                        }
                        <tr class="total-row">
                          <td><strong>Carga horaria total</strong></td>
                          <td class="col-center"><strong>{{ total312Pres() }}</strong></td>
                          <td class="col-center"><strong>{{ total312Dist() }}</strong></td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                }

                <!-- 3.1.3 -->
                <div class="subsec">
                  <p class="green-hint">
                    @if (total312Sum() === 0) {
                      Si no se incluyen actividades de formación práctica en el punto 3.1.2, no es obligatorio completar el punto 3.1.3.
                    } @else {
                      Se declararon {{ total312Sum() }} h de formación práctica en 3.1.2: es obligatorio especificar los ámbitos en 3.1.3.
                    }
                  </p>
                  <h5>
                    3.1.3. Especifique los ámbitos donde se desarrollan las actividades de formación práctica
                    @if (total312Sum() > 0) {
                      <span style="color:#991b1b;">*</span>
                    }
                  </h5>
                  <textarea
                    rows="4"
                    class="ambitos-box"
                    [disabled]="!f.puedeEditar"
                    [ngModel]="ambitos()"
                    (ngModelChange)="ambitos.set($event)"
                    placeholder="Ejemplo: laboratorio - unidad de enseñanza práctica, taller, aula..."
                  ></textarea>
                </div>

                <!-- 3.2 -->
                <div class="subsec">
                  <h5>3.2. Carga horaria semanal dedicada al dictado y a la formación práctica (horas reloj)</h5>
                  <table class="coneau-table">
                    <thead>
                      <tr>
                        <th>Concepto</th>
                        <th class="col-center" style="width: 130px;">Presencial</th>
                        <th class="col-center" style="width: 130px;">A distancia</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>Carga horaria semanal total *</td>
                        <td class="col-center">
                          <input type="number" min="0" class="num-input" [disabled]="!f.puedeEditar" [ngModel]="semTP()" (ngModelChange)="semTP.set(toInt($event))" />
                        </td>
                        <td class="col-center">
                          <input type="number" min="0" class="num-input" [disabled]="!f.puedeEditar" [ngModel]="semTD()" (ngModelChange)="semTD.set(toInt($event))" />
                        </td>
                      </tr>
                      <tr>
                        <td>Carga horaria semanal destinada a la formación práctica</td>
                        <td class="col-center">
                          <input type="number" min="0" class="num-input" [disabled]="!f.puedeEditar" [ngModel]="semPP()" (ngModelChange)="semPP.set(toInt($event))" />
                        </td>
                        <td class="col-center">
                          <input type="number" min="0" class="num-input" [disabled]="!f.puedeEditar" [ngModel]="semPD()" (ngModelChange)="semPD.set(toInt($event))" />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              }
            </section>
          </div>

          @if (errorMsg()) {
            <div class="alert error" style="margin-top: 10px;">
              <span>{{ errorMsg() }}</span>
              <button type="button" (click)="errorMsg.set(null)">✕</button>
            </div>
          }

          <div class="m-actions">
            <button type="button" class="btn-sec" (click)="cerrar.emit()">Cerrar</button>
            @if (f.puedeEditar) {
              <button type="button" class="btn-pri" (click)="guardar()" [disabled]="guardando()">
                {{ guardando() ? 'Guardando...' : 'Guardar Ficha del Estándar' }}
              </button>
            }
          </div>
        } @else if (errorMsg()) {
          <div class="alert error" style="margin-top: 12px;">
            <span>{{ errorMsg() }}</span>
          </div>
          <div class="m-actions">
            <button type="button" class="btn-sec" (click)="cerrar.emit()">Cerrar</button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 1200; padding: 16px; }
    .modal-estandar { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-width: 1000px; max-height: 92vh; display: flex; flex-direction: column; }
    .modal-head { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid #e5e7eb; padding-bottom: 10px; margin-bottom: 12px; }
    h3 { margin: 0; font-size: 18px; color: #1f1a1d; }
    .sub { margin: 4px 0 0; font-size: 13px; color: #5a4a53; }
    .btn-close { background: transparent; border: none; font-size: 18px; cursor: pointer; color: #6b7280; }
    .modal-body-scroll { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 16px; padding-right: 4px; }
    .loading-box { padding: 32px; text-align: center; color: #6b7280; font-size: 14px; }
    .sec-card { border: 1px solid #e5dde1; border-radius: 8px; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
    .sec-title-bar { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
    h4 { margin: 0; font-size: 15px; color: #6b4c5b; }
    h5 { margin: 0 0 6px; font-size: 13px; color: #1f1a1d; font-weight: 700; }
    .grp-title { font-size: 15px; font-weight: 600; color: #374151; margin: 4px 0 6px; }
    .inst { margin: 0; font-size: 13px; color: #374151; }
    .muted { margin: 0 0 6px; font-size: 12px; color: #6b7280; }
    .ref-box { background: #faf4f7; border: 1px solid #d9c5ce; border-radius: 6px; padding: 10px 12px; font-size: 12.5px; color: #5a4a53; display: flex; flex-direction: column; gap: 3px; }
    .ref-title { font-weight: 700; color: #6b4c5b; }
    .coneau-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 6px; }
    .coneau-table th { background: #f3ebef; color: #3d2c35; font-weight: 700; padding: 8px 10px; border: 1px solid #e5dde1; text-align: left; }
    .coneau-table td { background: #fcfafc; padding: 7px 10px; border: 1px solid #ece4e8; color: #1f1a1d; vertical-align: middle; }
    .coneau-table tr.total-row td { background: #f7f3f5; }
    .col-center { text-align: center !important; }
    .scale-select { width: 100%; padding: 5px 8px; border: 1px solid #d1d5db; border-radius: 5px; background: #fff; font-size: 13px; }
    .num-input { width: 90px; text-align: center; padding: 5px 6px; border: 1px solid #d1d5db; border-radius: 5px; background: #fff; font-size: 13px; }
    .ambitos-box { width: 100%; box-sizing: border-box; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13px; font-family: inherit; }
    .green-hint { margin: 0 0 4px; font-size: 12px; font-style: italic; color: #5a4a53; }
    .match-ok { color: #2d5a3d; font-weight: 700; margin-left: 6px; }
    .match-bad { color: #991b1b; font-weight: 700; margin-left: 6px; }
    .subsec { margin-top: 6px; }
    .status-pill { padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
    .status-pill.ok { background: #eef5f0; color: #2d5a3d; border: 1px solid #c6dfd0; }
    .status-pill.pend { background: #f7f3f5; color: #6b4c5b; border: 1px solid #d9c5ce; }
    .alert { display: flex; justify-content: space-between; align-items: center; padding: 9px 12px; border-radius: 6px; font-size: 13px; }
    .alert.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .alert.info { background: #faf4f7; color: #5a4a53; border: 1px solid #d9c5ce; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid #e5e7eb; padding-top: 12px; margin-top: 12px; }
    .btn-pri { background: #6b4c5b; color: #fff; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer; }
    .btn-pri:hover:not(:disabled) { background: #553c48; }
    .btn-pri:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-sec { background: #fff; color: #5a4a53; border: 1px solid #d1d5db; padding: 7px 14px; border-radius: 6px; font-weight: 500; cursor: pointer; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActividadEstandarModalComponent implements OnInit {
  private readonly planService = inject(PlanEstudioService);
  private readonly destroyRef = inject(DestroyRef);

  readonly planId = input.required<string>();
  readonly actividadId = input.required<string>();
  readonly cerrar = output<void>();
  readonly guardado = output<FichaEstandarActividad>();

  readonly ficha = signal<FichaEstandarActividad | null>(null);
  readonly cargando = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  readonly respuestasMap = signal<Record<string, string[]>>({});
  readonly cargasMap = signal<Record<string, CargaDraftRow>>({});
  readonly ambitos = signal<string>('');
  readonly semTP = signal<number>(0);
  readonly semTD = signal<number>(0);
  readonly semPP = signal<number>(0);
  readonly semPD = signal<number>(0);

  readonly gruposHabilitados = computed<Grupo22Estandar[]>(() =>
    (this.ficha()?.estandar.grupos22 || []).filter((g) => g.habilitado)
  );

  readonly filas311 = computed<AreaCargaHorariaEstandar[]>(() =>
    (this.ficha()?.estandar.areasCargaHoraria || []).filter(
      (a) => a.habilitada && (a.tipoSeccion || 'CARGA_TOTAL_311') === 'CARGA_TOTAL_311'
    )
  );

  readonly filas312 = computed<AreaCargaHorariaEstandar[]>(() =>
    (this.ficha()?.estandar.areasCargaHoraria || []).filter(
      (a) => a.habilitada && a.tipoSeccion === 'PRACTICA_312'
    )
  );

  readonly total311Pres = computed(() =>
    this.filas311().reduce((acc, a) => acc + this.getCarga(a.id!).tp, 0)
  );
  readonly total311Dist = computed(() =>
    this.filas311().reduce((acc, a) => acc + this.getCarga(a.id!).td, 0)
  );
  readonly total311Sum = computed(() => this.total311Pres() + this.total311Dist());

  readonly total312Pres = computed(() =>
    this.filas312().reduce((acc, a) => acc + this.getCarga(a.id!).pp, 0)
  );
  readonly total312Dist = computed(() =>
    this.filas312().reduce((acc, a) => acc + this.getCarga(a.id!).pd, 0)
  );
  readonly total312Sum = computed(() => this.total312Pres() + this.total312Dist());

  readonly diferenciaHorasReloj = computed(
    () => (this.ficha()?.asignaturaHorasReloj || 0) - this.total311Sum()
  );

  ngOnInit(): void {
    this.cargando.set(true);
    this.planService
      .obtenerFichaEstandar(this.planId(), this.actividadId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.inicializarEstado(res);
          this.ficha.set(res);
          this.cargando.set(false);
        },
        error: (err) => {
          this.cargando.set(false);
          this.errorMsg.set(err.error?.error || err.error?.mensaje || err.error?.message || 'No se pudo cargar la ficha del estándar.');
        },
      });
  }

  private inicializarEstado(f: FichaEstandarActividad): void {
    const rMap: Record<string, string[]> = {};
    for (const r of f.respuestas22 || []) {
      const prev = rMap[r.estandarItemId] || [];
      if (!prev.includes(r.escalaEvaluacionId)) {
        rMap[r.estandarItemId] = [...prev, r.escalaEvaluacionId];
      }
    }
    if (f.estandar.tipoEvaluacion22 === 'ESCALA_UNICA' && (f.estandar.escalas || []).length > 0) {
      const defEscala = f.estandar.escalas.find((e) => e.esValorPorDefecto) || f.estandar.escalas[0];
      if (defEscala?.id) {
        for (const grp of f.estandar.grupos22 || []) {
          if (!grp.habilitado) continue;
          for (const it of grp.items || []) {
            if (it.id && (!rMap[it.id] || rMap[it.id].length === 0)) {
              rMap[it.id] = [defEscala.id];
            }
          }
        }
      }
    }
    this.respuestasMap.set(rMap);

    const cMap: Record<string, CargaDraftRow> = {};
    for (const area of f.estandar.areasCargaHoraria || []) {
      if (!area.id || !area.habilitada) continue;
      const found = (f.cargasPorArea || []).find((c) => c.areaCargaHorariaId === area.id);
      cMap[area.id] = {
        tp: found?.horasTotalPresencial ?? 0,
        td: found?.horasTotalDistancia ?? 0,
        pp: found?.horasPracticaPresencial ?? 0,
        pd: found?.horasPracticaDistancia ?? 0,
      };
    }
    this.cargasMap.set(cMap);

    this.ambitos.set(f.resumenCarga?.ambitosFormacionPractica || '');
    this.semTP.set(f.resumenCarga?.horasSemanalTotalPresencial ?? 0);
    this.semTD.set(f.resumenCarga?.horasSemanalTotalDistancia ?? 0);
    this.semPP.set(f.resumenCarga?.horasSemanalPracticaPresencial ?? 0);
    this.semPD.set(f.resumenCarga?.horasSemanalPracticaDistancia ?? 0);
  }

  isMatrixChecked(itemId: string, escalaId: string): boolean {
    return (this.respuestasMap()[itemId] || []).includes(escalaId);
  }

  toggleMatrix(itemId: string, escalaId: string): void {
    const curr = { ...this.respuestasMap() };
    const existing = curr[itemId] || [];
    curr[itemId] = existing.includes(escalaId)
      ? existing.filter((x) => x !== escalaId)
      : [...existing, escalaId];
    this.respuestasMap.set(curr);
  }

  getSingleScale(itemId: string): string {
    return (this.respuestasMap()[itemId] || [])[0] || '';
  }

  setSingleScale(itemId: string, escalaId: string): void {
    this.respuestasMap.set({ ...this.respuestasMap(), [itemId]: escalaId ? [escalaId] : [] });
  }

  getCarga(areaId: string): CargaDraftRow {
    return this.cargasMap()[areaId] || { tp: 0, td: 0, pp: 0, pd: 0 };
  }

  updateCarga(areaId: string, key: keyof CargaDraftRow, rawVal: unknown): void {
    const val = this.toInt(rawVal);
    const curr = { ...this.cargasMap() };
    const row = { ...(curr[areaId] || { tp: 0, td: 0, pp: 0, pd: 0 }) };
    row[key] = val;
    curr[areaId] = row;
    this.cargasMap.set(curr);
  }

  toInt(val: unknown): number {
    const n = Number(val);
    return Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0;
  }

  guardar(): void {
    const f = this.ficha();
    if (!f) return;

    const respuestas22: Array<{ estandarItemId: string; escalaEvaluacionId: string }> = [];
    if (f.estandar.tipoSeccion22 !== 'NINGUNA' && f.estandar.tipoEvaluacion22 !== 'NINGUNA') {
      const rMap = this.respuestasMap();
      for (const grp of this.gruposHabilitados()) {
        for (const it of grp.items || []) {
          if (!it.id) continue;
          for (const escId of rMap[it.id] || []) {
            respuestas22.push({ estandarItemId: it.id, escalaEvaluacionId: escId });
          }
        }
      }
      if (f.estandar.tipoEvaluacion22 === 'MATRIZ_MULTIPLE' && respuestas22.length === 0) {
        this.errorMsg.set('Debe marcar al menos una competencia o habilidad en la Sección 2.2.');
        return;
      }
    }

    const horasReloj = f.asignaturaHorasReloj ?? 0;
    const f311 = this.filas311();
    const f312 = this.filas312();
    if (f311.length > 0 || f312.length > 0) {
      if (f311.length > 0 && horasReloj > 0 && this.total311Sum() !== horasReloj) {
        this.errorMsg.set(
          `La suma de cargas horarias del punto 3.1.1 (${this.total311Sum()} h) debe coincidir con la carga horaria total de la asignatura (${horasReloj} h).`
        );
        return;
      }
      if (f311.length > 0 && (this.total312Pres() > this.total311Pres() || this.total312Dist() > this.total311Dist())) {
        this.errorMsg.set(
          `Las horas prácticas del punto 3.1.2 (Presencial: ${this.total312Pres()} h, Distancia: ${this.total312Dist()} h) no pueden superar la carga horaria total del punto 3.1.1 (Presencial: ${this.total311Pres()} h, Distancia: ${this.total311Dist()} h).`
        );
        return;
      }
      const totalPorCodigo = new Map<string, { tp: number; td: number }>();
      for (const area of f311) {
        if (!area.codigo) continue;
        const cod = area.codigo.toUpperCase();
        const row = this.getCarga(area.id!);
        const prev = totalPorCodigo.get(cod) || { tp: 0, td: 0 };
        totalPorCodigo.set(cod, { tp: prev.tp + row.tp, td: prev.td + row.td });
      }
      for (const area of f312) {
        const cod = (area.codigo || '').toUpperCase();
        const maxArea = totalPorCodigo.get(cod);
        if (maxArea) {
          const row = this.getCarga(area.id!);
          if (row.pp > maxArea.tp || row.pd > maxArea.td) {
            this.errorMsg.set(
              `Las horas prácticas en "${area.etiquetaPractica || area.nombre}" no pueden superar su carga total declarada en 3.1.1.`
            );
            return;
          }
        }
      }
      if (this.total312Sum() > 0 && !this.ambitos().trim()) {
        this.errorMsg.set('Debe completar el punto 3.1.3 (ámbitos de formación práctica) al declarar horas prácticas en 3.1.2.');
        return;
      }
      if (horasReloj > 0 && this.semTP() + this.semTD() <= 0) {
        this.errorMsg.set('Debe ingresar la carga horaria semanal total (punto 3.2) mayor a 0.');
        return;
      }
      if (this.semPP() > this.semTP() || this.semPD() > this.semTD()) {
        this.errorMsg.set('La carga horaria semanal práctica no puede superar la carga horaria semanal total.');
        return;
      }
    }

    const body: GuardarFichaEstandarRequest = {
      respuestas22,
      cargasPorArea: [
        ...f311.map((area) => {
          const row = this.getCarga(area.id!);
          return {
            areaCargaHorariaId: area.id!,
            horasTotalPresencial: row.tp,
            horasTotalDistancia: row.td,
            horasPracticaPresencial: 0,
            horasPracticaDistancia: 0,
          };
        }),
        ...f312.map((area) => {
          const row = this.getCarga(area.id!);
          return {
            areaCargaHorariaId: area.id!,
            horasTotalPresencial: 0,
            horasTotalDistancia: 0,
            horasPracticaPresencial: row.pp,
            horasPracticaDistancia: row.pd,
          };
        }),
      ],
      resumenCarga: {
        ambitosFormacionPractica: this.ambitos().trim() || null,
        horasSemanalTotalPresencial: this.semTP(),
        horasSemanalTotalDistancia: this.semTD(),
        horasSemanalPracticaPresencial: this.semPP(),
        horasSemanalPracticaDistancia: this.semPD(),
      },
    };

    this.guardando.set(true);
    this.errorMsg.set(null);
    this.planService
      .guardarFichaEstandar(this.planId(), this.actividadId(), body)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.guardando.set(false);
          this.ficha.set(updated);
          this.guardado.emit(updated);
        },
        error: (err) => {
          this.guardando.set(false);
          this.errorMsg.set(err.error?.error || err.error?.mensaje || err.error?.message || 'No se pudo guardar la ficha del estándar.');
        },
      });
  }
}
