import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import {
  AreaCargaHorariaEstandar,
  EscalaEstandar,
  EstadoVigenciaEstandar,
  EstandarDetalle,
  EstandarResumen,
  Grupo22Estandar,
  GuardarEstandarRequest,
  ModoSeccion311,
  ModoSeccion312,
  TipoEvaluacionEstandar22,
  TipoSeccionCargaHoraria,
  TipoSeccionEstandar22,
} from '../../../core/estandares/estandar.models';
import { EstandarService } from '../../../core/estandares/estandar.service';
import {
  CarreraResponse,
  EstructuraService,
} from '../../../core/estructura/estructura.service';
import { SubnavAcademicaComponent } from '../subnav-academica/subnav-academica.component';

interface EscalaDraft {
  codigo: string;
  etiqueta: string;
  descripcionReferencia: string;
  esValorPorDefecto: boolean;
}

interface GrupoDraft {
  codigo: string;
  titulo: string;
  descripcion: string;
  habilitado: boolean;
  items: { enunciado: string }[];
  pegadoMasivo: string;
  mostrarPegado: boolean;
}

interface AreaDraft {
  codigo: string;
  nombre: string;
  habilitada: boolean;
  horasMinimasCarrera: number | null;
}

@Component({
  selector: 'app-academica-estandares',
  standalone: true,
  imports: [CommonModule, FormsModule, SubnavAcademicaComponent],
  template: `
    <div class="acad-wrap">
      <app-subnav-academica seccionActual="Estándares"></app-subnav-academica>

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
          <div>
            <h3>Estándares Normativos por Carrera</h3>
            <p class="sub">
              Plantilla obligatoria de criterios de Sección 2.2 (competencias, ejes o bloques) y
              Sección 3 (carga horaria) que heredan los nuevos planes de estudio.
            </p>
          </div>
          @if (canManage()) {
            <button type="button" class="btn-pri" (click)="abrirNuevo()">+ Nuevo Estándar</button>
          }
        </div>

        <div class="filters" style="margin-bottom: 14px;">
          <select [ngModel]="filtroCarreraId()" (ngModelChange)="filtroCarreraId.set($event); cargarEstandares()">
            <option value="">Todas las carreras</option>
            @for (c of carreras(); track c.id) {
              <option [value]="c.id">{{ c.nombre }}</option>
            }
          </select>
          <select [ngModel]="filtroEstado()" (ngModelChange)="filtroEstado.set($event); cargarEstandares()">
            <option value="">Todos los estados</option>
            <option value="VIGENTE">Vigente</option>
            <option value="NO_VIGENTE">No Vigente</option>
          </select>
        </div>

        <table class="tabla">
          <thead>
            <tr>
              <th>Carrera</th>
              <th>Estándar / Resolución</th>
              <th>Año</th>
              <th>Vigencia</th>
              <th>Sección 2.2</th>
              <th>Sección 3</th>
              <th>Planes</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (e of estandares(); track e.id) {
              <tr>
                <td>
                  <strong>{{ e.carreraNombre || 'Carrera' }}</strong>
                  @if (e.carreraSigla) {
                    <div class="meta">{{ e.carreraSigla }}</div>
                  }
                </td>
                <td>
                  <strong>{{ e.nombre }}</strong>
                  @if (e.resolucionMinisterial) {
                    <div class="meta">{{ e.resolucionMinisterial }}</div>
                  }
                </td>
                <td>{{ e.anioVigencia || '—' }}</td>
                <td>
                  <span class="badge" [class.vigente]="e.estadoVigencia === 'VIGENTE'" [class.no-vigente]="e.estadoVigencia !== 'VIGENTE'">
                    {{ e.estadoVigencia === 'VIGENTE' ? 'Vigente' : 'No Vigente' }}
                  </span>
                </td>
                <td>
                  <span class="pill-tipo">{{ etiquetaTipo22(e.tipoSeccion22) }}</span>
                  <div class="meta">{{ etiquetaEval22(e.tipoEvaluacion22) }} · {{ e.cantidadGrupos22 }} gr. / {{ e.cantidadItems22 }} ítems</div>
                </td>
                <td>{{ e.cantidadAreas3 }} filas 3.1.1</td>
                <td>
                  <span class="meta">{{ e.planesAsociados }} plan(es)</span>
                </td>
                <td class="acciones">
                  <button type="button" class="btn-sec" (click)="abrirDetalle(e.id)">
                    {{ canManage() && e.editable ? 'Editar' : 'Ver detalle' }}
                  </button>
                  @if (canManage()) {
                    <button type="button" class="btn-xs" (click)="crearNuevaVersion(e)" [disabled]="procesando()">
                      Crear nueva versión
                    </button>
                    <button type="button" class="btn-sec" (click)="alternarVigencia(e)" [disabled]="procesando()">
                      {{ e.estadoVigencia === 'VIGENTE' ? 'Pasar a No Vigente' : 'Marcar Vigente' }}
                    </button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="8" class="empty">
                  @if (cargando()) {
                    Cargando estándares...
                  } @else {
                    No hay estándares configurados para los filtros seleccionados.
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </section>

      @if (modalAbierto()) {
        <div class="modal-bg">
          <div class="modal-box modal-xl">
            <div class="modal-head">
              <div>
                <h3>
                  @if (!editandoId()) {
                    Nuevo Estándar de Carrera
                  } @else if (soloLectura()) {
                    Detalle de Estándar (Solo lectura)
                  } @else {
                    Editar Estándar vigente
                  }
                </h3>
                <p class="sub">
                  Configuración de plantilla normativa para Sección 2.2 y Sección 3.
                </p>
              </div>
              <button type="button" class="btn-close" (click)="cerrarModal()">✕</button>
            </div>

            @if (soloLectura() && editandoId()) {
              <div class="alert info">
                <span>
                  Este estándar es de <strong>solo lectura</strong> (estado No Vigente o ya tiene planes asociados, o pertenece a otra gestión).
                  Para realizar cambios normativos use <strong>Crear nueva versión</strong>.
                </span>
              </div>
            }

            <div class="modal-body-scroll">
              <!-- Bloque 1: Datos Generales -->
              <fieldset class="card-block" [disabled]="soloLectura()">
                <legend>1. Datos Generales del Estándar</legend>
                <div class="grid2">
                  <label>
                    Carrera *
                    <select [(ngModel)]="draftCarreraId" [disabled]="!!editandoId() || soloLectura()">
                      <option value="">Seleccione carrera</option>
                      @for (c of carrerasGestionables(); track c.id) {
                        <option [value]="c.id">{{ c.nombre }}</option>
                      }
                    </select>
                  </label>
                  <label>
                    Nombre del Estándar *
                    <input type="text" [(ngModel)]="draftNombre" placeholder="Ej: Estándar CONEAU Ingeniería Agronómica" />
                  </label>
                </div>
                <div class="grid3">
                  <label>
                    Resolución Ministerial
                    <input type="text" [(ngModel)]="draftResolucion" placeholder="Ej: RM 1254/18" />
                  </label>
                  <label>
                    Año de Vigencia
                    <input type="number" [(ngModel)]="draftAnio" />
                  </label>
                  <label>
                    Estado inicial
                    <select [(ngModel)]="draftEstado" [disabled]="!!editandoId() || soloLectura()">
                      <option value="VIGENTE">Vigente (reemplaza al anterior vigente)</option>
                      <option value="NO_VIGENTE">No Vigente (borrador / histórico)</option>
                    </select>
                  </label>
                </div>
                <label>
                  Observaciones
                  <input type="text" [(ngModel)]="draftObservaciones" placeholder="Notas opcionales sobre la normativa" />
                </label>
              </fieldset>

              <!-- Bloque 2: Sección 2.2 -->
              <fieldset class="card-block" [disabled]="soloLectura()">
                <legend>2. Sección 2.2 — Criterios, Competencias o Ejes</legend>
                <label>
                  Tipo de Sección 2.2 *
                  <select [(ngModel)]="draftTipo22" (ngModelChange)="onTipo22Change($event)">
                    <option value="COMPETENCIAS">Competencias (Matriz de contenidos y habilidades)</option>
                    <option value="EJES">Ejes (Escala única por eje transversal)</option>
                    <option value="BLOQUES" disabled>Bloques temáticos (Próximamente — pendiente de definición)</option>
                    <option value="NINGUNA">Ninguna (Sin evaluación en Sección 2.2)</option>
                  </select>
                </label>

                @if (draftTipo22 !== 'NINGUNA') {
                  <label>
                    Instrucciones para el docente
                    <textarea rows="2" [(ngModel)]="draftInstrucciones22" placeholder="Indicaciones que verá el docente al completar la sección 2.2"></textarea>
                  </label>

                  <!-- Escala / Opciones de evaluación -->
                  <div class="subcard">
                    <div class="subcard-head">
                      <strong>Opciones / Niveles de evaluación ({{ draftEscalas.length }})</strong>
                      @if (!soloLectura()) {
                        <button type="button" class="btn-xs" (click)="agregarEscala()">+ Agregar opción</button>
                      }
                    </div>
                    @for (esc of draftEscalas; track $index; let idx = $index) {
                      <div class="escala-row">
                        <input type="text" [(ngModel)]="esc.etiqueta" placeholder="Etiqueta (ej: Aprende / Bajo)" />
                        <input type="text" [(ngModel)]="esc.descripcionReferencia" placeholder="Valor de referencia / descripción para el docente" />
                        <label class="chk-inline" title="Opción seleccionada por defecto en escala única">
                          <input type="checkbox" [(ngModel)]="esc.esValorPorDefecto" (ngModelChange)="marcarDefectoEscala(idx, $event)" />
                          Por defecto
                        </label>
                        @if (!soloLectura()) {
                          <button type="button" class="btn-danger-xs" (click)="eliminarEscala(idx)">✕</button>
                        }
                      </div>
                    }
                  </div>

                  <!-- Grupos / Formaciones Tildables -->
                  <div class="subcard">
                    <div class="subcard-head">
                      <strong>Formaciones / Bloques / Grupos de ítems (tildar los que aplican a la carrera)</strong>
                      @if (!soloLectura()) {
                        <button type="button" class="btn-xs" (click)="agregarGrupo()">+ Agregar grupo/formación</button>
                      }
                    </div>
                    @for (grp of draftGrupos; track $index; let gIdx = $index) {
                      <div class="grupo-card" [class.grupo-disabled]="!grp.habilitado">
                        <div class="grupo-head">
                          <label class="chk-inline">
                            <input type="checkbox" [(ngModel)]="grp.habilitado" />
                            <span>Habilitado para esta carrera</span>
                          </label>
                          <input type="text" class="grp-title" [(ngModel)]="grp.titulo" placeholder="Título (ej: Formación Básica)" />
                          @if (!soloLectura()) {
                            <button type="button" class="btn-xs" (click)="grp.mostrarPegado = !grp.mostrarPegado">
                              Pegar lista
                            </button>
                            <button type="button" class="btn-xs" (click)="agregarItem(gIdx)">+ Ítem</button>
                            <button type="button" class="btn-danger-xs" (click)="eliminarGrupo(gIdx)">Eliminar grupo</button>
                          }
                        </div>
                        <input type="text" class="grp-desc" [(ngModel)]="grp.descripcion" placeholder="Descripción opcional del bloque" />

                        @if (grp.mostrarPegado && !soloLectura()) {
                          <div class="pegado-box">
                            <textarea rows="3" [(ngModel)]="grp.pegadoMasivo" placeholder="Pegue una línea por cada pregunta / contenido / habilidad..."></textarea>
                            <div class="pegado-actions">
                              <button type="button" class="btn-xs" (click)="aplicarPegadoMasivo(gIdx)">Agregar líneas como ítems</button>
                            </div>
                          </div>
                        }

                        @if (grp.habilitado) {
                          <div class="items-list">
                            @for (it of grp.items; track $index; let iIdx = $index) {
                              <div class="item-row">
                                <span class="item-num">{{ iIdx + 1 }}.</span>
                                <textarea rows="2" [(ngModel)]="it.enunciado" placeholder="Enunciado del contenido, habilidad o eje..."></textarea>
                                @if (!soloLectura()) {
                                  <button type="button" class="btn-danger-xs" (click)="eliminarItem(gIdx, iIdx)">✕</button>
                                }
                              </div>
                            } @empty {
                              <div class="meta" style="padding: 6px 0;">Sin ítems aún. Use "+ Ítem" o "Pegar lista".</div>
                            }
                          </div>
                        } @else {
                          <div class="meta">Grupo destildado: no se solicitará completar este bloque al docente.</div>
                        }
                      </div>
                    }
                  </div>
                }
              </fieldset>

              <!-- Bloque 3: Sección 3 Carga Horaria -->
              <fieldset class="card-block" [disabled]="soloLectura()">
                <legend>3. Sección 3 — Clasificación y Carga Horaria</legend>
                <p class="meta">
                  Active o desactive de forma independiente <strong>3.1.1</strong> y <strong>3.1.2</strong>, elija si trabaja por
                  <strong>Áreas temáticas</strong> o <strong>Bloques / Modalidades</strong>, y configure las
                  <strong>horas mínimas globales de la carrera</strong> (se verifican sumando todas las asignaturas activas del plan, nunca por materia individual).
                </p>

                <!-- 3.1.1 -->
                <div class="subcard" [class.grupo-disabled]="!draftHabilitar311">
                  <div class="subcard-head">
                    <label class="chk-inline">
                      <input type="checkbox" [(ngModel)]="draftHabilitar311" />
                      <strong>3.1.1 — Carga horaria total</strong>
                    </label>
                    @if (draftHabilitar311) {
                      <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                        <select [(ngModel)]="draftModo311" style="padding:4px 8px; font-size:12px;">
                          <option value="AREAS_TEMATICAS">Áreas temáticas / otra formación</option>
                          <option value="BLOQUES_CURRICULARES">Bloques curriculares</option>
                        </select>
                        @if (!soloLectura()) {
                          <button type="button" class="btn-xs" (click)="cargarPlantilla311()">Cargar plantilla</button>
                          <button type="button" class="btn-xs" (click)="agregarArea311()">+ Agregar fila a 3.1.1</button>
                        }
                      </div>
                    }
                  </div>

                  @if (draftHabilitar311) {
                    <div class="min-global-bar">
                      <label class="min-global-label">
                        <span>Mínimo global Total Carrera 3.1.1 (hs reloj, opcional):</span>
                        <input
                          type="number"
                          min="0"
                          class="num-min-input"
                          [(ngModel)]="draftHorasMinimasTotal311"
                          placeholder="Ej: 3600"
                        />
                      </label>
                    </div>
                    @for (area of draftAreas311; track $index; let aIdx = $index) {
                      <div class="area-row-min">
                        <label class="chk-inline">
                          <input type="checkbox" [(ngModel)]="area.habilitada" />
                          <span>Habilitada</span>
                        </label>
                        <input type="text" [(ngModel)]="area.nombre" placeholder="Nombre del bloque o área temática" />
                        <div class="min-cell" title="Horas mínimas globales de carrera (suma de todas las asignaturas SE DICTA)">
                          <span class="min-tag">Mín. carrera (h)</span>
                          <input
                            type="number"
                            min="0"
                            class="num-min-input"
                            [disabled]="!area.habilitada || soloLectura()"
                            [(ngModel)]="area.horasMinimasCarrera"
                            placeholder="Opcional"
                          />
                        </div>
                        @if (!soloLectura()) {
                          <button type="button" class="btn-danger-xs" (click)="eliminarArea311(aIdx)">✕</button>
                        }
                      </div>
                    }
                  } @else {
                    <div class="meta">Subsección 3.1.1 desactivada: no se solicitará clasificación de carga horaria total en las asignaturas.</div>
                  }
                </div>

                <!-- 3.1.2 -->
                <div class="subcard" [class.grupo-disabled]="!draftHabilitar312">
                  <div class="subcard-head">
                    <label class="chk-inline">
                      <input type="checkbox" [(ngModel)]="draftHabilitar312" />
                      <strong>3.1.2 — Formación práctica (Anexo III)</strong>
                    </label>
                    @if (draftHabilitar312) {
                      <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                        <select [(ngModel)]="draftModo312" style="padding:4px 8px; font-size:12px;">
                          <option value="AREAS_PRACTICAS">Áreas de formación práctica</option>
                          <option value="MODALIDADES_PRACTICAS">Modalidades de formación práctica</option>
                        </select>
                        @if (!soloLectura()) {
                          <button type="button" class="btn-xs" (click)="cargarPlantilla312()">Cargar plantilla</button>
                          <button type="button" class="btn-xs" (click)="agregarArea312()">+ Agregar fila a 3.1.2</button>
                        }
                      </div>
                    }
                  </div>

                  @if (draftHabilitar312) {
                    <div class="min-global-bar">
                      <label class="min-global-label">
                        <span>Mínimo global Total Formación Práctica 3.1.2 (hs reloj, opcional):</span>
                        <input
                          type="number"
                          min="0"
                          class="num-min-input"
                          [(ngModel)]="draftHorasMinimasTotal312"
                          placeholder="Ej: 750"
                        />
                      </label>
                    </div>
                    @for (area of draftAreas312; track $index; let aIdx = $index) {
                      <div class="area-row-min">
                        <label class="chk-inline">
                          <input type="checkbox" [(ngModel)]="area.habilitada" />
                          <span>Habilitada</span>
                        </label>
                        <input type="text" [(ngModel)]="area.nombre" placeholder="Nombre de la modalidad o área práctica" />
                        <div class="min-cell" title="Horas mínimas globales de carrera (suma de todas las asignaturas SE DICTA)">
                          <span class="min-tag">Mín. carrera (h)</span>
                          <input
                            type="number"
                            min="0"
                            class="num-min-input"
                            [disabled]="!area.habilitada || soloLectura()"
                            [(ngModel)]="area.horasMinimasCarrera"
                            placeholder="Opcional"
                          />
                        </div>
                        @if (!soloLectura()) {
                          <button type="button" class="btn-danger-xs" (click)="eliminarArea312(aIdx)">✕</button>
                        }
                      </div>
                    }
                  } @else {
                    <div class="meta">Subsección 3.1.2 desactivada: no se solicitarán modalidades/áreas de formación práctica ni ámbitos (3.1.3).</div>
                  }
                </div>
              </fieldset>
            </div>

            <div class="m-actions">
              <button type="button" class="btn-sec" (click)="cerrarModal()">Cerrar</button>
              @if (!soloLectura()) {
                <button type="button" class="btn-pri" (click)="guardar()" [disabled]="procesando()">
                  {{ procesando() ? 'Guardando...' : 'Guardar Estándar' }}
                </button>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .acad-wrap { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; }
    .bar { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
    .filters { display: flex; gap: 10px; flex-wrap: wrap; }
    .filters select { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; }
    h3 { margin: 0; font-size: 18px; color: #1f1a1d; }
    .sub { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
    .meta { color: #6b7280; font-size: 12px; margin-top: 2px; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 14px; }
    .tabla thead th { background: #f7f3f5; color: #5a4a53; font-weight: 600; font-size: 13px; border-bottom: 1px solid #e5dde1; padding: 10px; text-align: left; }
    .tabla td { padding: 10px; border-bottom: 1px solid #f3f4f6; text-align: left; vertical-align: top; }
    .badge { padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 700; display: inline-block; }
    .badge.vigente { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .badge.no-vigente { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .pill-tipo { background: #f5eef1; color: #6b4c5b; border: 1px solid #d9c5ce; padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; }
    .acciones { display: flex; gap: 6px; flex-wrap: wrap; }
    .btn-pri { background: #6b4c5b; color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-pri:hover:not(:disabled) { background: #553c48; }
    .btn-pri:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-sec { background: #fff; color: #5a4a53; border: 1px solid #d1d5db; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; }
    .btn-sec:hover:not(:disabled) { background: #fdfafb; border-color: #d9c5ce; color: #6b4c5b; }
    .btn-xs { background: #fdfafb; color: #6b4c5b; border: 1px solid #d9c5ce; padding: 4px 9px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 600; }
    .btn-xs:hover:not(:disabled) { background: #f5eef1; border-color: #6b4c5b; }
    .btn-danger-xs { background: #fff5f5; color: #991b1b; border: 1px solid #fecaca; padding: 4px 8px; border-radius: 6px; cursor: pointer; font-size: 12px; }
    .btn-close { background: transparent; border: none; font-size: 18px; cursor: pointer; color: #6b7280; }
    .empty { text-align: center; padding: 24px; color: #6b7280; }
    .alert { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
    .alert.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .alert.success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .alert.info { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
    .modal-bg { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal-box { background: #fff; border-radius: 10px; padding: 20px; width: 100%; max-height: 92vh; display: flex; flex-direction: column; }
    .modal-xl { max-width: 960px; }
    .modal-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
    .modal-body-scroll { overflow-y: auto; flex: 1; padding-right: 4px; display: flex; flex-direction: column; gap: 14px; }
    .card-block { border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px; margin: 0; display: flex; flex-direction: column; gap: 10px; }
    .card-block legend { font-weight: 700; font-size: 14px; color: #6b4c5b; padding: 0 6px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; font-weight: 600; color: #374151; }
    input[type='text'], input[type='number'], select, textarea { padding: 7px 9px; border: 1px solid #d1d5db; border-radius: 6px; font-weight: 400; font-size: 13px; font-family: inherit; }
    .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .grid3 { display: grid; grid-template-columns: 1.3fr 0.7fr 1fr; gap: 10px; }
    .subcard { border: 1px solid #f0e8ec; border-radius: 6px; padding: 10px; background: #fcfafc; display: flex; flex-direction: column; gap: 8px; }
    .subcard-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; }
    .escala-row { display: grid; grid-template-columns: 1fr 2fr auto auto; gap: 8px; align-items: center; }
    .chk-inline { display: flex; flex-direction: row; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; white-space: nowrap; }
    .grupo-card { border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px; background: #fff; display: flex; flex-direction: column; gap: 8px; }
    .grupo-card.grupo-disabled { background: #f9fafb; opacity: 0.75; }
    .grupo-head { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .grp-title { flex: 1; min-width: 200px; font-weight: 600 !important; }
    .grp-desc { width: 100%; box-sizing: border-box; }
    .items-list { display: flex; flex-direction: column; gap: 6px; }
    .item-row { display: grid; grid-template-columns: 24px 1fr auto; gap: 8px; align-items: flex-start; }
    .item-num { font-size: 12px; font-weight: 600; color: #6b7280; padding-top: 8px; }
    .pegado-box { display: flex; flex-direction: column; gap: 6px; background: #f9fafb; padding: 8px; border-radius: 6px; border: 1px dashed #d1d5db; }
    .pegado-actions { display: flex; justify-content: flex-end; }
    .area-row { display: grid; grid-template-columns: auto 1fr auto; gap: 8px; align-items: center; }
    .area-row-min { display: grid; grid-template-columns: auto 1fr auto auto; gap: 8px; align-items: center; }
    .min-global-bar { background: #f7f3f5; border: 1px dashed #d9c5ce; border-radius: 6px; padding: 8px 10px; }
    .min-global-label { display: flex; flex-direction: row; align-items: center; justify-content: space-between; gap: 10px; font-size: 12px; color: #5a4a53; }
    .min-cell { display: flex; align-items: center; gap: 5px; }
    .min-tag { font-size: 11px; color: #6b7280; white-space: nowrap; }
    .num-min-input { width: 92px; text-align: center; padding: 5px 6px !important; }
    .m-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; border-top: 1px solid #f3f4f6; padding-top: 12px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicaEstandaresComponent implements OnInit {
  private readonly estandarService = inject(EstandarService);
  private readonly estructura = inject(EstructuraService);
  private readonly auth = inject(Auth);
  private readonly destroyRef = inject(DestroyRef);

  readonly carreras = signal<CarreraResponse[]>([]);
  readonly estandares = signal<EstandarResumen[]>([]);
  readonly filtroCarreraId = signal('');
  readonly filtroEstado = signal<EstadoVigenciaEstandar | ''>('');
  readonly cargando = signal(false);
  readonly procesando = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalAbierto = signal(false);
  readonly editandoId = signal<string | null>(null);
  readonly soloLectura = signal(false);

  readonly canManage = computed(() =>
    this.auth.hasAnyRole('AEA', 'DIRECTOR_ESCUELA')
  );

  readonly carrerasGestionables = computed(() => {
    const all = this.carreras();
    if (this.auth.hasRole('AEA')) return all;
    const uaId = this.auth.currentSession?.user?.unidadAcademicaId;
    if (!uaId) return all;
    const filtradas = all.filter((c) => c.unidadAcademicaId === uaId);
    return filtradas.length > 0 ? filtradas : all;
  });

  // Editor draft fields
  draftCarreraId = '';
  draftNombre = '';
  draftResolucion = '';
  draftAnio: number | null = new Date().getFullYear();
  draftEstado: EstadoVigenciaEstandar = 'VIGENTE';
  draftTipo22: TipoSeccionEstandar22 = 'COMPETENCIAS';
  draftEval22: TipoEvaluacionEstandar22 = 'MATRIZ_MULTIPLE';
  draftInstrucciones22 = '';
  draftObservaciones = '';
  draftHabilitar311 = true;
  draftModo311: ModoSeccion311 = 'AREAS_TEMATICAS';
  draftHorasMinimasTotal311: number | null = null;
  draftHabilitar312 = true;
  draftModo312: ModoSeccion312 = 'AREAS_PRACTICAS';
  draftHorasMinimasTotal312: number | null = null;
  draftEscalas: EscalaDraft[] = [];
  draftGrupos: GrupoDraft[] = [];
  draftAreas311: AreaDraft[] = [];
  draftAreas312: AreaDraft[] = [];

  ngOnInit(): void {
    this.estructura
      .listarCarreras()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cs) => this.carreras.set(cs || []),
        error: () => this.errorMessage.set('No se pudo cargar la lista de carreras.'),
      });
    this.cargarEstandares();
  }

  cargarEstandares(): void {
    this.cargando.set(true);
    this.estandarService
      .listar(this.filtroCarreraId() || undefined, this.filtroEstado() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.estandares.set(res || []);
          this.cargando.set(false);
        },
        error: (err) => {
          this.cargando.set(false);
          this.errorMessage.set(err.error?.message || 'Error al cargar los estándares.');
        },
      });
  }

  abrirNuevo(): void {
    const gestionables = this.carrerasGestionables();
    const carreraPre = this.filtroCarreraId() || (gestionables.length === 1 ? gestionables[0].id : '');
    this.editandoId.set(null);
    this.soloLectura.set(false);
    this.draftCarreraId = carreraPre;
    this.draftNombre = '';
    this.draftResolucion = '';
    this.draftAnio = new Date().getFullYear();
    this.draftEstado = 'VIGENTE';
    this.draftObservaciones = '';
    this.cargarPresetCompetencias();
    this.modalAbierto.set(true);
  }

  abrirDetalle(id: string): void {
    this.procesando.set(true);
    this.estandarService
      .obtenerDetalle(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (det) => {
          this.procesando.set(false);
          this.cargarDraftDesdeDetalle(det);
          this.modalAbierto.set(true);
        },
        error: (err) => {
          this.procesando.set(false);
          this.errorMessage.set(err.error?.message || 'No se pudo cargar el detalle del estándar.');
        },
      });
  }

  private cargarDraftDesdeDetalle(det: EstandarDetalle): void {
    this.editandoId.set(det.id);
    this.soloLectura.set(!this.canManage() || !det.editable);
    this.draftCarreraId = det.carreraId;
    this.draftNombre = det.nombre;
    this.draftResolucion = det.resolucionMinisterial || '';
    this.draftAnio = det.anioVigencia;
    this.draftEstado = det.estadoVigencia;
    this.draftTipo22 = det.tipoSeccion22;
    this.draftEval22 = det.tipoEvaluacion22;
    this.draftInstrucciones22 = det.instrucciones22 || '';
    this.draftObservaciones = det.observaciones || '';
    this.draftHabilitar311 = det.habilitar311 !== false;
    this.draftModo311 = det.modo311 || (det.tipoSeccion22 === 'EJES' ? 'BLOQUES_CURRICULARES' : 'AREAS_TEMATICAS');
    this.draftHorasMinimasTotal311 = det.horasMinimasTotal311 ?? null;
    this.draftHabilitar312 = det.habilitar312 !== false;
    this.draftModo312 = det.modo312 || (det.tipoSeccion22 === 'EJES' ? 'MODALIDADES_PRACTICAS' : 'AREAS_PRACTICAS');
    this.draftHorasMinimasTotal312 = det.horasMinimasTotal312 ?? null;
    this.draftEscalas = (det.escalas || []).map((e: EscalaEstandar) => ({
      codigo: e.codigo || '',
      etiqueta: e.etiqueta,
      descripcionReferencia: e.descripcionReferencia || '',
      esValorPorDefecto: !!e.esValorPorDefecto,
    }));
    this.draftGrupos = (det.grupos22 || []).map((g: Grupo22Estandar) => ({
      codigo: g.codigo || '',
      titulo: g.titulo,
      descripcion: g.descripcion || '',
      habilitado: !!g.habilitado,
      items: (g.items || []).map((i) => ({ enunciado: i.enunciado })),
      pegadoMasivo: '',
      mostrarPegado: false,
    }));
    const todasAreas = det.areasCargaHoraria || [];
    this.draftAreas311 = todasAreas
      .filter((a: AreaCargaHorariaEstandar) => (a.tipoSeccion || 'CARGA_TOTAL_311') === 'CARGA_TOTAL_311')
      .map((a: AreaCargaHorariaEstandar) => ({
        codigo: a.codigo || '',
        nombre: a.nombre,
        habilitada: !!a.habilitada,
        horasMinimasCarrera: a.horasMinimasCarrera ?? null,
      }));
    this.draftAreas312 = todasAreas
      .filter((a: AreaCargaHorariaEstandar) => a.tipoSeccion === 'PRACTICA_312')
      .map((a: AreaCargaHorariaEstandar) => ({
        codigo: a.codigo || '',
        nombre: a.etiquetaPractica || a.nombre,
        habilitada: !!a.habilitada,
        horasMinimasCarrera: a.horasMinimasCarrera ?? null,
      }));
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.editandoId.set(null);
  }

  onTipo22Change(tipo: TipoSeccionEstandar22): void {
    if (tipo === 'COMPETENCIAS') {
      this.cargarPresetCompetencias();
    } else if (tipo === 'EJES') {
      this.cargarPresetEjes();
    } else if (tipo === 'NINGUNA') {
      this.draftEval22 = 'NINGUNA';
      this.draftInstrucciones22 = '';
      this.draftEscalas = [];
      this.draftGrupos = [];
    } else if (this.draftEval22 === 'NINGUNA') {
      this.draftEval22 = 'MATRIZ_MULTIPLE';
    }
  }

  cargarPresetCompetencias(): void {
    this.draftTipo22 = 'COMPETENCIAS';
    this.draftEval22 = 'MATRIZ_MULTIPLE';
    this.draftInstrucciones22 =
      'Seleccione para cada contenido o habilidad los niveles alcanzados por el alumno (Aprende, Observa, Resuelve, Ejecuta).';
    this.draftEscalas = [
      {
        codigo: 'APRENDE',
        etiqueta: 'Aprende',
        descripcionReferencia: 'El alumno aprende los fundamentos teóricos relacionados con el contenido/habilidad',
        esValorPorDefecto: false,
      },
      {
        codigo: 'OBSERVA',
        etiqueta: 'Observa',
        descripcionReferencia: 'El alumno observa actividades demostrativas relacionadas con el contenido/habilidad',
        esValorPorDefecto: false,
      },
      {
        codigo: 'RESUELVE',
        etiqueta: 'Resuelve',
        descripcionReferencia: 'El alumno resuelve problemas teórico-prácticos relacionados con el contenido/habilidad',
        esValorPorDefecto: false,
      },
      {
        codigo: 'EJECUTA',
        etiqueta: 'Ejecuta',
        descripcionReferencia: 'El alumno ejecuta, al menos una vez, las acciones relacionadas con el contenido/habilidad',
        esValorPorDefecto: false,
      },
    ];
    this.draftGrupos = [
      { codigo: 'FORMACION_BASICA', titulo: 'Formación Básica', descripcion: '', habilitado: true, items: [{ enunciado: '' }], pegadoMasivo: '', mostrarPegado: false },
      { codigo: 'FORMACION_APLICADA', titulo: 'Formación Aplicada', descripcion: '', habilitado: true, items: [{ enunciado: '' }], pegadoMasivo: '', mostrarPegado: false },
      { codigo: 'FORMACION_PROFESIONAL', titulo: 'Formación Profesional', descripcion: '', habilitado: true, items: [{ enunciado: '' }], pegadoMasivo: '', mostrarPegado: false },
      { codigo: 'FORMACION_COMPLEMENTARIA', titulo: 'Formación Complementaria', descripcion: '', habilitado: false, items: [], pegadoMasivo: '', mostrarPegado: false },
    ];
    this.draftHabilitar311 = true;
    this.draftModo311 = 'AREAS_TEMATICAS';
    this.draftHorasMinimasTotal311 = null;
    this.draftHabilitar312 = true;
    this.draftModo312 = 'AREAS_PRACTICAS';
    this.draftHorasMinimasTotal312 = null;
    this.cargarPlantilla311();
    this.cargarPlantilla312();
  }

  cargarPresetEjes(): void {
    this.draftTipo22 = 'EJES';
    this.draftEval22 = 'ESCALA_UNICA';
    this.draftInstrucciones22 =
      'Si la actividad curricular prepara al alumno para su adquisición como fija la Resolución Ministerial, seleccione el grado de profundidad. Si no prepara al alumno, deje el cuadro en "Ninguna".';
    this.draftEscalas = [
      { codigo: 'NINGUNA', etiqueta: '(Ninguna)', descripcionReferencia: 'La actividad curricular no aborda este eje.', esValorPorDefecto: true },
      { codigo: 'BAJO', etiqueta: 'Bajo', descripcionReferencia: 'Nivel introductorio o inicial.', esValorPorDefecto: false },
      { codigo: 'MEDIO', etiqueta: 'Medio', descripcionReferencia: 'Nivel intermedio de aplicación.', esValorPorDefecto: false },
      { codigo: 'ALTO', etiqueta: 'Alto', descripcionReferencia: 'Nivel avanzado o integrador.', esValorPorDefecto: false },
    ];
    this.draftGrupos = [
      {
        codigo: 'EJES_TRANSVERSALES',
        titulo: 'Ejes y enunciados multidimensionales y transversales',
        descripcion: 'Ejes definidos en la Resolución Ministerial de la carrera.',
        habilitado: true,
        items: [{ enunciado: '' }],
        pegadoMasivo: '',
        mostrarPegado: false,
      },
    ];
    this.draftHabilitar311 = true;
    this.draftModo311 = 'BLOQUES_CURRICULARES';
    this.draftHorasMinimasTotal311 = null;
    this.draftHabilitar312 = true;
    this.draftModo312 = 'MODALIDADES_PRACTICAS';
    this.draftHorasMinimasTotal312 = null;
    this.cargarPlantilla311();
    this.cargarPlantilla312();
  }

  cargarPlantilla311(): void {
    if (this.draftModo311 === 'BLOQUES_CURRICULARES') {
      this.draftAreas311 = [
        { codigo: 'CIENCIAS_BASICAS_INGENIERIA', nombre: 'Ciencias Básicas de la Ingeniería', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'TECNOLOGIAS_BASICAS', nombre: 'Tecnologías Básicas', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'TECNOLOGIAS_APLICADAS', nombre: 'Tecnologías Aplicadas', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'CIENCIAS_TECNOLOGIAS_COMPLEMENTARIAS', nombre: 'Ciencias y Tecnologías Complementarias', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'OTROS_CONTENIDOS', nombre: 'Otros contenidos', habilitada: true, horasMinimasCarrera: null },
      ];
    } else {
      this.draftAreas311 = [
        { codigo: 'FORMACION_BASICA', nombre: 'Formación Básica', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_APLICADA', nombre: 'Formación Aplicada', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_PROFESIONAL', nombre: 'Formación Profesional', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_COMPLEMENTARIA', nombre: 'Formación Complementaria', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'OTROS_CONTENIDOS', nombre: 'Otros contenidos', habilitada: true, horasMinimasCarrera: null },
      ];
    }
  }

  cargarPlantilla312(): void {
    if (this.draftModo312 === 'MODALIDADES_PRACTICAS') {
      this.draftAreas312 = [
        {
          codigo: 'INSTANCIAS_SUPERVISADAS_PRACTICA',
          nombre: 'Instancias supervisadas de formación práctica (prácticas en diferentes ámbitos tales como aula, laboratorios, campo u otros)',
          habilitada: true,
          horasMinimasCarrera: null,
        },
        { codigo: 'PROYECTO_INTEGRADOR', nombre: 'Proyecto Integrador', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'PRACTICA_PROFESIONAL_SUPERVISADA', nombre: 'Práctica profesional supervisada', habilitada: true, horasMinimasCarrera: null },
      ];
    } else {
      this.draftAreas312 = [
        { codigo: 'FORMACION_BASICA', nombre: 'Formación Básica', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_APLICADA', nombre: 'Formación Aplicada', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_PROFESIONAL', nombre: 'Formación Profesional', habilitada: true, horasMinimasCarrera: null },
        { codigo: 'FORMACION_COMPLEMENTARIA', nombre: 'Carga horaria asignada a formación complementaria', habilitada: true, horasMinimasCarrera: null },
      ];
    }
  }

  agregarEscala(): void {
    this.draftEscalas = [...this.draftEscalas, { codigo: '', etiqueta: '', descripcionReferencia: '', esValorPorDefecto: false }];
  }

  eliminarEscala(idx: number): void {
    this.draftEscalas = this.draftEscalas.filter((_, i) => i !== idx);
  }

  marcarDefectoEscala(idx: number, val: boolean): void {
    this.draftEscalas.forEach((e, i) => {
      e.esValorPorDefecto = i === idx ? val : false;
    });
  }

  agregarGrupo(): void {
    this.draftGrupos = [
      ...this.draftGrupos,
      { codigo: '', titulo: '', descripcion: '', habilitado: true, items: [{ enunciado: '' }], pegadoMasivo: '', mostrarPegado: false },
    ];
  }

  eliminarGrupo(idx: number): void {
    this.draftGrupos = this.draftGrupos.filter((_, i) => i !== idx);
  }

  agregarItem(gIdx: number): void {
    this.draftGrupos[gIdx].items = [...this.draftGrupos[gIdx].items, { enunciado: '' }];
  }

  eliminarItem(gIdx: number, iIdx: number): void {
    this.draftGrupos[gIdx].items = this.draftGrupos[gIdx].items.filter((_, i) => i !== iIdx);
  }

  aplicarPegadoMasivo(gIdx: number): void {
    const grp = this.draftGrupos[gIdx];
    const lines = (grp.pegadoMasivo || '')
      .split(/\r?\n/)
      .map((l) => l.trim())
      .filter((l) => l.length > 0);
    if (lines.length === 0) return;
    const existentes = grp.items.filter((i) => i.enunciado.trim().length > 0);
    grp.items = [...existentes, ...lines.map((enunciado) => ({ enunciado }))];
    grp.pegadoMasivo = '';
    grp.mostrarPegado = false;
  }

  agregarArea311(): void {
    this.draftAreas311 = [...this.draftAreas311, { codigo: '', nombre: '', habilitada: true, horasMinimasCarrera: null }];
  }

  eliminarArea311(idx: number): void {
    this.draftAreas311 = this.draftAreas311.filter((_, i) => i !== idx);
  }

  agregarArea312(): void {
    this.draftAreas312 = [...this.draftAreas312, { codigo: '', nombre: '', habilitada: true, horasMinimasCarrera: null }];
  }

  eliminarArea312(idx: number): void {
    this.draftAreas312 = this.draftAreas312.filter((_, i) => i !== idx);
  }

  private normMin(val: number | null | undefined): number | null {
    if (val === null || val === undefined || val === ('' as unknown)) return null;
    const n = Number(val);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : null;
  }

  guardar(): void {
    if (!this.draftCarreraId || !this.draftNombre.trim()) {
      this.errorMessage.set('Debe seleccionar la carrera e ingresar el nombre del estándar.');
      return;
    }
    if (this.draftHabilitar311) {
      const areas311Validas = this.draftAreas311.filter((a) => a.nombre.trim().length > 0 && a.habilitada);
      if (areas311Validas.length === 0) {
        this.errorMessage.set('La Sección 3.1.1 está habilitada y debe tener al menos un bloque o área activa.');
        return;
      }
    }
    if (this.draftHabilitar312) {
      const areas312Validas = this.draftAreas312.filter((a) => a.nombre.trim().length > 0 && a.habilitada);
      if (areas312Validas.length === 0) {
        this.errorMessage.set('La Sección 3.1.2 está habilitada y debe tener al menos una modalidad o área práctica activa.');
        return;
      }
    }

    const mapAreas = (lista: AreaDraft[], tipoSeccion: TipoSeccionCargaHoraria) =>
      lista
        .filter((a) => a.nombre.trim().length > 0)
        .map((a, idx) => ({
          tipoSeccion,
          orden: idx + 1,
          codigo: a.codigo.trim() || undefined,
          nombre: a.nombre.trim(),
          etiquetaPractica: a.nombre.trim(),
          habilitada: !!a.habilitada,
          horasMinimasCarrera: a.habilitada ? this.normMin(a.horasMinimasCarrera) : null,
        }));

    const payload: GuardarEstandarRequest = {
      carreraId: this.draftCarreraId,
      nombre: this.draftNombre.trim(),
      resolucionMinisterial: this.draftResolucion.trim() || null,
      anioVigencia: this.draftAnio,
      estadoVigencia: this.draftEstado,
      tipoSeccion22: this.draftTipo22,
      tipoEvaluacion22: this.draftTipo22 === 'NINGUNA' ? 'NINGUNA' : this.draftEval22,
      instrucciones22: this.draftInstrucciones22.trim() || null,
      observaciones: this.draftObservaciones.trim() || null,
      habilitar311: !!this.draftHabilitar311,
      modo311: this.draftModo311,
      horasMinimasTotal311: this.draftHabilitar311 ? this.normMin(this.draftHorasMinimasTotal311) : null,
      habilitar312: !!this.draftHabilitar312,
      modo312: this.draftModo312,
      horasMinimasTotal312: this.draftHabilitar312 ? this.normMin(this.draftHorasMinimasTotal312) : null,
      escalas: this.draftEscalas
        .filter((e) => e.etiqueta.trim().length > 0)
        .map((e, idx) => ({
          orden: idx + 1,
          codigo: e.codigo.trim() || undefined,
          etiqueta: e.etiqueta.trim(),
          descripcionReferencia: e.descripcionReferencia.trim() || null,
          esValorPorDefecto: !!e.esValorPorDefecto,
        })),
      grupos22: this.draftGrupos
        .filter((g) => g.titulo.trim().length > 0)
        .map((g, gIdx) => ({
          orden: gIdx + 1,
          codigo: g.codigo.trim() || undefined,
          titulo: g.titulo.trim(),
          descripcion: g.descripcion.trim() || null,
          habilitado: !!g.habilitado,
          items: g.items
            .filter((i) => i.enunciado.trim().length > 0)
            .map((i, iIdx) => ({ orden: iIdx + 1, enunciado: i.enunciado.trim() })),
        })),
      areasCargaHoraria: [
        ...mapAreas(this.draftAreas311, 'CARGA_TOTAL_311'),
        ...mapAreas(this.draftAreas312, 'PRACTICA_312'),
      ],
    };

    this.procesando.set(true);
    this.errorMessage.set(null);
    const id = this.editandoId();
    const obs = id ? this.estandarService.actualizar(id, payload) : this.estandarService.crear(payload);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.procesando.set(false);
        this.modalAbierto.set(false);
        this.successMessage.set(id ? 'Estándar actualizado correctamente.' : 'Estándar creado correctamente.');
        this.cargarEstandares();
      },
      error: (err) => {
        this.procesando.set(false);
        this.errorMessage.set(err.error?.message || 'No se pudo guardar el estándar.');
      },
    });
  }

  crearNuevaVersion(e: EstandarResumen): void {
    const ok = window.confirm(
      `Se creará una nueva versión VIGENTE copiando la configuración de "${e.nombre}", y la versión actual pasará a NO VIGENTE. Los planes existentes conservarán el estándar anterior sin perder datos. ¿Continuar?`
    );
    if (!ok) return;
    this.procesando.set(true);
    this.estandarService
      .crearNuevaVersion(e.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (nueva) => {
          this.procesando.set(false);
          this.successMessage.set(`Nueva versión "${nueva.nombre}" creada y marcada como VIGENTE.`);
          this.cargarEstandares();
          this.cargarDraftDesdeDetalle(nueva);
          this.modalAbierto.set(true);
        },
        error: (err) => {
          this.procesando.set(false);
          this.errorMessage.set(err.error?.message || 'No se pudo crear la nueva versión.');
        },
      });
  }

  alternarVigencia(e: EstandarResumen): void {
    const nuevo: EstadoVigenciaEstandar = e.estadoVigencia === 'VIGENTE' ? 'NO_VIGENTE' : 'VIGENTE';
    const msg =
      nuevo === 'VIGENTE'
        ? `¿Marcar "${e.nombre}" como VIGENTE? El estándar vigente actual de la carrera pasará a NO VIGENTE.`
        : `¿Pasar "${e.nombre}" a NO VIGENTE?`;
    if (!window.confirm(msg)) return;
    this.procesando.set(true);
    this.estandarService
      .cambiarVigencia(e.id, nuevo)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.procesando.set(false);
          this.successMessage.set('Estado de vigencia actualizado.');
          this.cargarEstandares();
        },
        error: (err) => {
          this.procesando.set(false);
          this.errorMessage.set(err.error?.message || 'No se pudo cambiar el estado de vigencia.');
        },
      });
  }

  etiquetaTipo22(t: TipoSeccionEstandar22): string {
    switch (t) {
      case 'COMPETENCIAS': return 'Competencias';
      case 'EJES': return 'Ejes';
      case 'BLOQUES': return 'Bloques';
      case 'NINGUNA': return 'Sin Sec. 2.2';
    }
  }

  etiquetaEval22(ev: TipoEvaluacionEstandar22): string {
    switch (ev) {
      case 'MATRIZ_MULTIPLE': return 'Matriz';
      case 'ESCALA_UNICA': return 'Escala única';
      case 'NINGUNA': return 'Ninguna';
    }
  }
}
