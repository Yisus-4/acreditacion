import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../core/auth/auth';
import {
  CargoDocente,
  DedicacionDocente,
  DepartamentoAcademico,
  PeriodoAcademico,
  TipoDesignacionDocente,
} from '../../../core/designacion/designacion-parametros.models';
import { DesignacionParametrosService } from '../../../core/designacion/designacion-parametros.service';
import {
  DesignacionDocenteItem,
  ModalidadParticipacionDocente,
  SolicitudDesignacionDetalle,
  SolicitudDesignacionResumen,
  TipoSolicitudDesignacion,
} from '../../../core/designacion/solicitud-designacion.models';
import { SolicitudDesignacionService } from '../../../core/designacion/solicitud-designacion.service';
import { CarreraResponse, EstructuraService } from '../../../core/estructura/estructura.service';
import { ActividadCurricularPlan } from '../../../core/planes-estudio/plan-estudio.models';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import {
  CuerpoAcademicoMaestro,
  DatosCentralesService,
  PlanEstudioMaestro,
} from '../../../core/services/datos-centrales.service';
import { SubnavAcademicaComponent } from '../subnav-academica/subnav-academica.component';

type TabDetalle = 'DESIGNACIONES' | 'HISTORIAL';

@Component({
  selector: 'app-academica-solicitudes-designacion',
  standalone: true,
  imports: [CommonModule, FormsModule, SubnavAcademicaComponent],
  template: `
    <div class="acad-wrap">
      <app-subnav-academica seccionActual="Solicitudes de Designación"></app-subnav-academica>

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

      <!-- Barra de filtros y creación -->
      <section class="panel">
        <div class="filters-bar">
          <div class="filter-group">
            <label for="filtro-periodo">Período Académico</label>
            <select
              id="filtro-periodo"
              [ngModel]="filtroPeriodoId()"
              (ngModelChange)="filtroPeriodoId.set($event); cargarSolicitudes()"
            >
              <option value="">Todos los períodos</option>
              @for (p of periodos(); track p.id) {
                <option [value]="p.id">{{ p.denominacion }} ({{ p.codigo }})</option>
              }
            </select>
          </div>

          <div class="filter-group">
            <label for="filtro-depto">Departamento Académico</label>
            <select
              id="filtro-depto"
              [ngModel]="filtroDepartamentoId()"
              (ngModelChange)="filtroDepartamentoId.set($event); cargarSolicitudes()"
              [disabled]="esSoloDirectorDepto()"
            >
              @if (!esSoloDirectorDepto()) {
                <option value="">Todos los departamentos</option>
              }
              @for (d of departamentos(); track d.id) {
                <option [value]="d.id">{{ d.denominacion }} ({{ d.codigo }})</option>
              }
            </select>
          </div>

          <div class="filter-actions">
            @if (canManage()) {
              <button type="button" class="btn-pri" (click)="abrirModalNuevaSolicitud()">
                + Nueva Solicitud
              </button>
            }
          </div>
        </div>

        <!-- Listado de solicitudes -->
        @if (cargandoLista()) {
          <p class="muted">Cargando solicitudes de designación...</p>
        } @else if (solicitudes().length === 0) {
          <div class="empty-box">
            <p>No hay solicitudes de designación registradas para los filtros seleccionados.</p>
            @if (canManage()) {
              <button type="button" class="btn-pri" (click)="abrirModalNuevaSolicitud()">
                Crear primera solicitud
              </button>
            }
          </div>
        } @else {
          <div class="solicitudes-grid">
            @for (sol of solicitudes(); track sol.id) {
              <button
                type="button"
                class="sol-card"
                [class.selected]="solicitudSeleccionada()?.id === sol.id"
                (click)="seleccionarSolicitud(sol.id)"
              >
                <div class="sol-card__top">
                  <span class="badge badge--tipo">{{ sol.tipoSolicitud }}</span>
                  <span class="badge" [class]="'badge--estado-' + sol.estado.toLowerCase()">
                    {{ etiquetaEstado(sol.estado) }}
                  </span>
                </div>
                <strong class="sol-card__depto">{{ sol.departamentoDenominacion }}</strong>
                <span class="sol-card__periodo">{{ sol.periodoDenominacion }} · {{ sol.periodoAnio }}</span>
                <div class="sol-card__meta">
                  <span>{{ sol.cantidadDesignaciones }} designación(es) activa(s)</span>
                  @if (sol.cantidadConAlertaHoras > 0) {
                    <span class="badge badge--warn">
                      ⚠ {{ sol.cantidadConAlertaHoras }} con hs sin distribuir
                    </span>
                  }
                </div>
              </button>
            }
          </div>
        }
      </section>

      <!-- Detalle de solicitud seleccionada -->
      @if (solicitudSeleccionada(); as detalle) {
        <section class="panel detail-panel">
          <header class="detail-header">
            <div>
              <div class="detail-badges">
                <span class="badge badge--tipo">{{ detalle.tipoSolicitud }}</span>
                <span class="badge" [class]="'badge--estado-' + detalle.estado.toLowerCase()">
                  {{ etiquetaEstado(detalle.estado) }}
                </span>
                @if (detalle.solicitudOrigenId) {
                  <span class="badge badge--origen" title="Generada o vinculada a solicitud anterior (RF07 / RF09)">
                    ⇄ Con solicitud base vinculada
                  </span>
                }
                @if (!detalle.editable) {
                  <span class="badge badge--muted">Solo lectura</span>
                }
              </div>
              <h2 class="detail-title">
                {{ detalle.departamentoDenominacion }} — {{ detalle.periodoDenominacion }}
              </h2>
              @if (detalle.observacionesDepartamento) {
                <p class="sub-desc">{{ detalle.observacionesDepartamento }}</p>
              }
            </div>

            <div class="detail-actions">
              @if (canManage() && detalle.editable) {
                <button type="button" class="btn-sec" (click)="abrirModalDesignacion(null)">
                  + Agregar Cargo / Docente
                </button>
                <button
                  type="button"
                  class="btn-pri"
                  [disabled]="guardando()"
                  (click)="presentarASga(detalle)"
                >
                  Presentar a SGA →
                </button>
              }
              @if (canReviewSga() && detalle.estado === 'PRESENTADA_SGA') {
                <button
                  type="button"
                  class="btn-ok"
                  [disabled]="guardando()"
                  (click)="abrirModalRevisionSga('APROBAR')"
                >
                  ✓ Aprobar (SGA)
                </button>
                <button
                  type="button"
                  class="btn-warn-act"
                  [disabled]="guardando()"
                  (click)="abrirModalRevisionSga('OBSERVAR')"
                >
                  ↩ Observar / Devolver
                </button>
              }
            </div>
          </header>

          @if (detalle.observacionesSga) {
            <div class="sga-banner" [class.sga-banner--obs]="detalle.estado === 'OBSERVADA'">
              <strong>Dictamen / Observaciones SGA:</strong> {{ detalle.observacionesSga }}
            </div>
          }

          <!-- Sub-pestañas de Detalle: Designaciones vs. Historial de Auditoría -->
          <div class="inner-tabs">
            <button
              type="button"
              class="itab"
              [class.on]="tabDetalle() === 'DESIGNACIONES'"
              (click)="tabDetalle.set('DESIGNACIONES')"
            >
              Designaciones y Carga Horaria ({{ detalle.designaciones.length }})
            </button>
            <button
              type="button"
              class="itab"
              [class.on]="tabDetalle() === 'HISTORIAL'"
              (click)="tabDetalle.set('HISTORIAL')"
            >
              Historial de Cambios y Auditoría ({{ detalle.historialCambios.length }})
            </button>
          </div>

          @if (cargandoDetalle()) {
            <p class="muted">Actualizando detalle...</p>
          } @else if (tabDetalle() === 'DESIGNACIONES') {
            @if (detalle.designaciones.length === 0) {
              <div class="empty-box">
                <p>Esta solicitud aún no tiene designaciones docentes cargadas.</p>
                @if (canManage() && detalle.editable) {
                  <button type="button" class="btn-pri" (click)="abrirModalDesignacion(null)">
                    + Agregar primera designación
                  </button>
                }
              </div>
            } @else {
              <div class="table-wrap">
                <table class="tabla">
                  <thead>
                    <tr>
                      <th>Docente y Tope Período (RN02)</th>
                      <th>Cargo · Dedicación · Estado</th>
                      <th>Distribución 6 Actividades (RF04 / RN06)</th>
                      <th>Asignaturas (Clases vs. Finales · RN09)</th>
                      @if (canManage() && detalle.editable) {
                        <th>Acciones</th>
                      }
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of detalle.designaciones; track item.id) {
                      <tr [class.row-excluded]="item.estadoItem === 'EXCLUIDO'">
                        <td>
                          <div class="doc-name">{{ item.docenteApellido }}, {{ item.docenteNombre }}</div>
                          <div class="doc-meta">
                            DNI {{ item.docenteDni }}
                            @if (item.docenteLegajo) { · Leg. {{ item.docenteLegajo }} }
                          </div>
                          <div class="cap-pill" [class.cap-pill--full]="item.totalHorasDocentePeriodo === 50">
                            Acumulado activo: <strong>{{ item.totalHorasDocentePeriodo }} / 50 hs</strong>
                          </div>
                        </td>

                        <td>
                          <div class="cargo-row">
                            <span class="cargo-title">{{ item.cargoNombre }}</span>
                            <span class="badge" [class]="'badge--item-' + item.estadoItem.toLowerCase()">
                              {{ etiquetaEstadoItem(item.estadoItem) }}
                            </span>
                          </div>
                          <div class="doc-meta">
                            {{ item.dedicacionNombre }} (<strong>{{ item.horasDedicacion }} hs/sem</strong>)
                            · {{ item.tipoDesignacionNombre }}
                          </div>
                          @if (item.nroDesignacionMapuche) {
                            <div class="doc-meta">Mapuche: <code>{{ item.nroDesignacionMapuche }}</code></div>
                          }
                        </td>

                        <td>
                          <div class="dist-summary">
                            <span><strong>{{ item.totalHorasDistribuidas }}</strong> / {{ item.horasDedicacion }} hs</span>
                            @if (item.estadoItem === 'EXCLUIDO') {
                              <span class="badge badge--muted">Excluida</span>
                            } @else if (item.tieneAlertaHorasSinDistribuir) {
                              <span class="badge badge--warn" title="Horas de dedicación aún no distribuidas (RF04.5)">
                                ⚠ Faltan {{ item.horasSinDistribuir }} hs
                              </span>
                            } @else {
                              <span class="badge badge--ok">✓ Completa</span>
                            }
                          </div>
                          <div class="dist-chips">
                            <span>Clases: {{ item.horasClases }}h</span>
                            <span>Inv: {{ item.horasInvestigacion }}h</span>
                            <span>Ext: {{ item.horasExtension }}h</span>
                            <span>Gest: {{ item.horasGestion }}h</span>
                            <span>Tesis: {{ item.horasDireccionTesis }}h</span>
                            <span>Otras: {{ item.horasOtras }}h</span>
                          </div>
                        </td>

                        <td>
                          @if (item.asignaciones.length === 0) {
                            <span class="muted-sm">Sin asignaturas asignadas</span>
                          } @else {
                            <div class="asig-list">
                              @for (a of item.asignaciones; track a.id) {
                                <div class="asig-chip">
                                  <span
                                    class="badge"
                                    [class.badge--clases]="a.modalidadParticipacion === 'DESARROLLO_CLASES'"
                                    [class.badge--finales]="a.modalidadParticipacion === 'EVALUACION_FINALES'"
                                  >
                                    {{ a.modalidadParticipacion === 'DESARROLLO_CLASES' ? 'Clases' : 'Solo Finales' }}
                                  </span>
                                  <span class="asig-text">
                                    <strong>{{ a.actividadNombre }}</strong>
                                    ({{ a.carreraSigla || a.carreraNombre }} · {{ a.planEstudioNombre }}
                                    @if (a.planEstadoVigencia !== 'VIGENTE') { · <em>No vigente</em> })
                                  </span>
                                </div>
                              }
                            </div>
                          }
                        </td>

                        @if (canManage() && detalle.editable) {
                          <td class="actions-cell">
                            @if (item.estadoItem === 'EXCLUIDO') {
                              <button type="button" class="btn-sm btn-reactivar" (click)="reactivarDesignacion(item)">
                                ↺ Reactivar
                              </button>
                            } @else {
                              <button type="button" class="btn-sm" (click)="abrirModalDesignacion(item)">Cargo</button>
                              <button type="button" class="btn-sm" (click)="abrirModalDistribucion(item)">6 Actividades</button>
                              <button type="button" class="btn-sm" (click)="abrirModalAsignaciones(item)">Asignaturas</button>
                              <button type="button" class="btn-sm btn-danger" (click)="abrirModalExcluir(item)">
                                Excluir
                              </button>
                              @if (!item.designacionOrigenId) {
                                <button type="button" class="btn-sm btn-danger" (click)="eliminarDesignacion(item)">
                                  Borrar
                                </button>
                              }
                            }
                          </td>
                        }
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          } @else {
            <!-- Pestaña HISTORIAL DE CAMBIOS (RF08) -->
            @if (detalle.historialCambios.length === 0) {
              <div class="empty-box">
                <p>Aún no hay movimientos registrados en el historial de esta solicitud.</p>
              </div>
            } @else {
              <div class="table-wrap">
                <table class="tabla">
                  <thead>
                    <tr>
                      <th>Fecha y Hora</th>
                      <th>Usuario</th>
                      <th>Evento</th>
                      <th>Valor Anterior → Nuevo</th>
                      <th>Motivo / Observación (RF08)</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (h of detalle.historialCambios; track h.id) {
                      <tr>
                        <td class="nowrap">{{ h.fechaCambio | date: 'dd/MM/yyyy HH:mm' }}</td>
                        <td>{{ h.usuarioNombre || 'Sistema' }}</td>
                        <td><span class="badge badge--muted">{{ etiquetaEvento(h.tipoEvento) }}</span></td>
                        <td>
                          @if (h.valorAnterior) {
                            <div class="diff-old">− {{ h.valorAnterior }}</div>
                          }
                          @if (h.valorNuevo) {
                            <div class="diff-new">+ {{ h.valorNuevo }}</div>
                          }
                        </td>
                        <td>{{ h.motivo || '—' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </section>
      }

      <!-- MODAL 1: Nueva Solicitud (con Clonación RF07) -->
      @if (modalNuevaSolicitud()) {
        <div class="modal-backdrop" (click)="modalNuevaSolicitud.set(false)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Nueva Solicitud de Designación</h3>
            <div class="form-grid">
              <label>
                Departamento Académico *
                <select [(ngModel)]="nuevaSolForm.departamentoAcademicoId" [disabled]="esSoloDirectorDepto()">
                  <option value="">Seleccione un departamento...</option>
                  @for (d of departamentos(); track d.id) {
                    <option [value]="d.id">{{ d.denominacion }} ({{ d.codigo }})</option>
                  }
                </select>
              </label>

              <label>
                Período Académico *
                <select [(ngModel)]="nuevaSolForm.periodoAcademicoId">
                  <option value="">Seleccione un período...</option>
                  @for (p of periodos(); track p.id) {
                    <option [value]="p.id">{{ p.denominacion }} ({{ p.codigo }})</option>
                  }
                </select>
              </label>

              <label>
                Tipo de Solicitud *
                <select [(ngModel)]="nuevaSolForm.tipoSolicitud">
                  <option value="ANUAL">ANUAL (General del Departamento)</option>
                  <option value="EXTRAORDINARIA">EXTRAORDINARIA (Movimiento durante el período · RF09)</option>
                </select>
              </label>

              @if (nuevaSolForm.tipoSolicitud === 'ANUAL') {
                <label class="chk-box">
                  <input type="checkbox" [(ngModel)]="nuevaSolForm.clonarPeriodoAnterior" />
                  <span>
                    <strong>Clonar automáticamente el período anterior (RF07):</strong>
                    copia los cargos activos, distribución en 6 actividades y asignaturas vigentes de la última solicitud aprobada/presentada del Departamento.
                  </span>
                </label>
              }

              <label>
                Observaciones del Departamento
                <textarea rows="3" [(ngModel)]="nuevaSolForm.observacionesDepartamento" placeholder="Opcional"></textarea>
              </label>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="modalNuevaSolicitud.set(false)">Cancelar</button>
              <button type="button" class="btn-pri" [disabled]="guardando()" (click)="crearSolicitud()">
                {{ guardando() ? 'Procesando...' : 'Crear Solicitud' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- MODAL 2: Agregar / Editar Cargo y Dedicación -->
      @if (modalDesignacion()) {
        <div class="modal-backdrop" (click)="modalDesignacion.set(false)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>{{ editandoDesignacionItem() ? 'Editar Cargo y Dedicación' : 'Agregar Cargo / Designación Docente' }}</h3>
            <div class="form-grid">
              <label>
                Docente *
                <select [(ngModel)]="desigForm.docenteId" [disabled]="!!editandoDesignacionItem()">
                  <option value="">Seleccione un docente...</option>
                  @for (doc of docentesDisponibles(); track doc.id) {
                    <option [value]="doc.id">
                      {{ doc.apellido }}, {{ doc.nombre }} — DNI {{ doc.dni }}
                      @if (doc.legajo) { (Leg. {{ doc.legajo }}) }
                    </option>
                  }
                </select>
              </label>

              <label>
                Cargo Docente *
                <select [(ngModel)]="desigForm.cargoDocenteId">
                  <option value="">Seleccione cargo...</option>
                  @for (c of cargos(); track c.id) {
                    <option [value]="c.id">{{ c.nombre }}</option>
                  }
                </select>
              </label>

              <label>
                Dedicación *
                <select [(ngModel)]="desigForm.dedicacionDocenteId">
                  <option value="">Seleccione dedicación...</option>
                  @for (ded of dedicaciones(); track ded.id) {
                    <option [value]="ded.id">{{ ded.nombre }} ({{ ded.horasSemanales }} hs/sem)</option>
                  }
                </select>
              </label>

              <label>
                Tipo de Designación *
                <select [(ngModel)]="desigForm.tipoDesignacionId">
                  <option value="">Seleccione tipo...</option>
                  @for (t of tiposDesignacion(); track t.id) {
                    <option [value]="t.id">{{ t.nombre }}</option>
                  }
                </select>
              </label>

              <label>
                N° Designación SIU-Mapuche
                <input type="text" [(ngModel)]="desigForm.nroDesignacionMapuche" placeholder="Opcional" />
              </label>

              <label>
                Observaciones
                <textarea rows="2" [(ngModel)]="desigForm.observaciones" placeholder="Opcional"></textarea>
              </label>

              @if (requiereMotivoModalCargo()) {
                <label class="motivo-highlight">
                  Motivo de la modificación (obligatorio · RF08) *
                  <input
                    type="text"
                    [(ngModel)]="desigForm.motivoCambio"
                    placeholder="Ej.: Promoción ordinaria / Ajuste por observación SGA"
                  />
                </label>
              }
            </div>

            @if (desigForm.docenteId && desigForm.dedicacionDocenteId) {
              <div class="preview-cap" [class.preview-cap--over]="horasProyectadasDocente() > 50">
                Carga proyectada en el período (RN02):
                <strong>{{ horasProyectadasDocente() }} / 50 hs semanales</strong>
                @if (!docenteYaEnSolicitudActual()) {
                  <span> (referencia local; el servidor valida el acumulado global del período).</span>
                }
                @if (horasProyectadasDocente() > 50) {
                  <span> — Supera el tope institucional de 50 hs.</span>
                }
              </div>
            }

            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="modalDesignacion.set(false)">Cancelar</button>
              <button
                type="button"
                class="btn-pri"
                [disabled]="guardando() || horasProyectadasDocente() > 50"
                (click)="guardarDesignacion()"
              >
                {{ guardando() ? 'Guardando...' : 'Guardar Designación' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- MODAL 3: Distribución Horaria en 6 Actividades -->
      @if (modalDistribucion(); as target) {
        <div class="modal-backdrop" (click)="modalDistribucion.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Distribución Horaria Semanal (RF04 / RN06)</h3>
            <p class="sub-desc">
              {{ target.docenteApellido }}, {{ target.docenteNombre }} · {{ target.cargoNombre }} ({{ target.horasDedicacion }} hs/sem)
            </p>

            <div class="dist-grid">
              <label>
                1. Desarrollo de clases
                <input type="number" min="0" [(ngModel)]="distForm.horasClases" />
              </label>
              <label>
                2. Investigación
                <input type="number" min="0" [(ngModel)]="distForm.horasInvestigacion" />
              </label>
              <label>
                3. Extensión
                <input type="number" min="0" [(ngModel)]="distForm.horasExtension" />
              </label>
              <label>
                4. Gestión
                <input type="number" min="0" [(ngModel)]="distForm.horasGestion" />
              </label>
              <label>
                5. Dirección de tesis / TF
                <input type="number" min="0" [(ngModel)]="distForm.horasDireccionTesis" />
              </label>
              <label>
                6. Otras actividades académicas
                <input type="number" min="0" [(ngModel)]="distForm.horasOtras" />
              </label>
            </div>

            @if (requiereMotivoItem(target)) {
              <div class="form-grid">
                <label class="motivo-highlight">
                  Motivo del cambio horario (obligatorio · RF08) *
                  <input
                    type="text"
                    [(ngModel)]="distForm.motivoCambio"
                    placeholder="Indique el motivo de la redistribución horaria"
                  />
                </label>
              </div>
            }

            <div
              class="preview-cap"
              [class.preview-cap--over]="tieneNegativosDistribucion() || sumaDistribucionModal() > target.horasDedicacion"
              [class.preview-cap--warn]="!tieneNegativosDistribucion() && sumaDistribucionModal() < target.horasDedicacion"
            >
              @if (tieneNegativosDistribucion()) {
                <span>Las horas por actividad no pueden ser negativas.</span>
              } @else {
                Total distribuido: <strong>{{ sumaDistribucionModal() }} / {{ target.horasDedicacion }} hs</strong>
                @if (sumaDistribucionModal() > target.horasDedicacion) {
                  <span> — Excede la dedicación de {{ target.horasDedicacion }} hs (bloqueado por RN06).</span>
                } @else if (sumaDistribucionModal() < target.horasDedicacion) {
                  <span> — Quedan {{ target.horasDedicacion - sumaDistribucionModal() }} hs sin distribuir (alerta RF04.5).</span>
                } @else {
                  <span> — Distribución completa.</span>
                }
              }
            </div>

            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="modalDistribucion.set(null)">Cancelar</button>
              <button
                type="button"
                class="btn-pri"
                [disabled]="guardando() || tieneNegativosDistribucion() || sumaDistribucionModal() > target.horasDedicacion"
                (click)="guardarDistribucion(target)"
              >
                {{ guardando() ? 'Guardando...' : 'Guardar Distribución' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- MODAL 4: Asignación de Asignaturas (RN09) -->
      @if (modalAsignaciones(); as target) {
        <div class="modal-backdrop" (click)="modalAsignaciones.set(null)">
          <div class="modal modal--wide" (click)="$event.stopPropagation()">
            <h3>Asignaturas Asignadas — {{ target.docenteApellido }}, {{ target.docenteNombre }}</h3>
            <p class="sub-desc">
              {{ target.cargoNombre }} · {{ target.dedicacionNombre }} ({{ target.horasDedicacion }} hs/sem).
              Puede asignar asignaturas de planes vigentes o no vigentes, diferenciando Desarrollo de Clases vs. Solo Exámenes Finales (RF05 / RN09).
            </p>

            @if (requiereMotivoItem(target)) {
              <div class="form-grid">
                <label class="motivo-highlight">
                  Motivo de modificación de asignaturas (obligatorio para altas/bajas en este cargo · RF08) *
                  <input
                    type="text"
                    [(ngModel)]="asigForm.motivoCambio"
                    placeholder="Indique el motivo para agregar o quitar asignaturas"
                  />
                </label>
              </div>
            }

            @if (target.asignaciones.length > 0) {
              <table class="tabla tabla--compact">
                <thead>
                  <tr>
                    <th>Carrera / Plan</th>
                    <th>Asignatura</th>
                    <th>Modalidad</th>
                    <th>Hs</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  @for (a of target.asignaciones; track a.id) {
                    <tr>
                      <td>
                        {{ a.carreraSigla || a.carreraNombre }} · {{ a.planEstudioNombre }}
                        @if (a.planEstadoVigencia !== 'VIGENTE') {
                          <span class="badge badge--muted">No vigente</span>
                        }
                      </td>
                      <td>{{ a.actividadNombre }}</td>
                      <td>
                        <span
                          class="badge"
                          [class.badge--clases]="a.modalidadParticipacion === 'DESARROLLO_CLASES'"
                          [class.badge--finales]="a.modalidadParticipacion === 'EVALUACION_FINALES'"
                        >
                          {{ a.modalidadParticipacion === 'DESARROLLO_CLASES' ? 'Desarrollo de clases' : 'Solo exámenes finales' }}
                        </span>
                      </td>
                      <td>{{ a.horasAsignadas ?? '—' }}</td>
                      <td>
                        <button type="button" class="btn-sm btn-danger" (click)="eliminarAsignacion(target, a.id)">
                          Quitar
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }

            <div class="asig-form-box">
              <h4>Agregar asignatura</h4>
              <div class="form-grid">
                <label>
                  Carrera *
                  <select [(ngModel)]="asigForm.carreraId" (ngModelChange)="onCarreraChange($event)">
                    <option value="">Seleccione carrera...</option>
                    @for (c of carreras(); track c.id) {
                      <option [value]="c.id">{{ c.nombre }}</option>
                    }
                  </select>
                </label>

                <label>
                  Plan de Estudio (vigente o no vigente) *
                  <select [(ngModel)]="asigForm.planEstudioId" (ngModelChange)="onPlanChange($event)" [disabled]="!asigForm.carreraId">
                    <option value="">Seleccione plan...</option>
                    @for (p of planesCarrera(); track p.id) {
                      <option [value]="p.id">{{ p.nombrePlan }} ({{ p.estadoVigencia }})</option>
                    }
                  </select>
                </label>

                <label>
                  Asignatura *
                  <select [(ngModel)]="asigForm.actividadCurricularId" (ngModelChange)="onActividadChange($event)" [disabled]="!asigForm.planEstudioId">
                    <option value="">Seleccione asignatura...</option>
                    @for (act of asignaturasPlan(); track act.id) {
                      <option [value]="act.id" [disabled]="act.estadoDictado === 'INACTIVA'">
                        {{ act.nombre }}
                        @if (act.estadoDictado === 'SOLO_EXAMENES_FINALES') { [Solo Finales] }
                        @if (act.estadoDictado === 'INACTIVA') { [INACTIVA - No asignable] }
                      </option>
                    }
                  </select>
                </label>

                <label>
                  Modalidad de Participación (RN09) *
                  <select
                    [(ngModel)]="asigForm.modalidadParticipacion"
                    [disabled]="actividadSeleccionadaEsSoloFinales()"
                  >
                    <option value="DESARROLLO_CLASES" [disabled]="actividadSeleccionadaEsSoloFinales()">
                      Desarrollo de clases (dictado regular)
                    </option>
                    <option value="EVALUACION_FINALES">
                      Evaluación en exámenes finales (mesa)
                    </option>
                  </select>
                </label>

                <label>
                  Horas semanales en la asignatura
                  <input type="number" min="0" [(ngModel)]="asigForm.horasAsignadas" placeholder="Opcional" />
                </label>
              </div>
              <div class="modal-actions">
                <button type="button" class="btn-sec" (click)="modalAsignaciones.set(null)">Cerrar</button>
                <button type="button" class="btn-pri" [disabled]="guardando()" (click)="agregarAsignacion(target)">
                  {{ guardando() ? 'Agregando...' : '+ Agregar Asignatura' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      }

      <!-- MODAL 5: Excluir Designación con Motivo Obligatorio (RF07.6 / RF08) -->
      @if (modalExcluir(); as target) {
        <div class="modal-backdrop" (click)="modalExcluir.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Excluir Designación de la Solicitud (RF07.6)</h3>
            <p class="sub-desc">
              {{ target.docenteApellido }}, {{ target.docenteNombre }} — {{ target.cargoNombre }} ({{ target.horasDedicacion }} hs).
              El renglón quedará marcado como <strong>EXCLUIDO</strong> conservando la trazabilidad histórica y liberando sus horas del tope de 50 hs.
            </p>
            <div class="form-grid">
              <label class="motivo-highlight">
                Motivo de la exclusión (obligatorio · RF08) *
                <textarea
                  rows="3"
                  [(ngModel)]="motivoExclusion"
                  placeholder="Ej.: Licencia sin goce de haberes / Renuncia / Fin de interinato"
                ></textarea>
              </label>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="modalExcluir.set(null)">Cancelar</button>
              <button type="button" class="btn-pri" [disabled]="guardando()" (click)="confirmarExcluir(target)">
                {{ guardando() ? 'Excluyendo...' : 'Confirmar Exclusión' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- MODAL 6: Revisión SGA (Aprobar / Observar) -->
      @if (modalRevisionSga(); as modo) {
        <div class="modal-backdrop" (click)="modalRevisionSga.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>
              {{ modo === 'APROBAR' ? 'Aprobar Solicitud (Secretaría de Gestión Académica)' : 'Observar y Devolver Solicitud al Departamento' }}
            </h3>
            <div class="form-grid">
              <label [class.motivo-highlight]="modo === 'OBSERVAR'">
                {{ modo === 'APROBAR' ? 'Observaciones o nota de conformidad (opcional)' : 'Observaciones para el Departamento (obligatorio) *' }}
                <textarea
                  rows="4"
                  [(ngModel)]="observacionesSgaModal"
                  [placeholder]="modo === 'APROBAR' ? 'Opcional...' : 'Detalle las correcciones requeridas para que el Departamento pueda subsanarlas...'"
                ></textarea>
              </label>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn-sec" (click)="modalRevisionSga.set(null)">Cancelar</button>
              <button type="button" class="btn-pri" [disabled]="guardando()" (click)="confirmarRevisionSga(modo)">
                {{ guardando() ? 'Procesando...' : (modo === 'APROBAR' ? 'Confirmar Aprobación' : 'Devolver como Observada') }}
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .acad-wrap { max-width: 1340px; margin: 0 auto; padding: 20px 24px 48px; }
    .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; margin-bottom: 20px; }
    .alert { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; font-size: 14px; }
    .alert.error { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; }
    .alert.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
    .alert button { background: none; border: none; cursor: pointer; font-weight: 700; color: inherit; }
    .filters-bar { display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-end; margin-bottom: 18px; }
    .filter-group { display: flex; flex-direction: column; gap: 4px; min-width: 240px; }
    .filter-group label { font-size: 12px; font-weight: 600; color: #5a4a53; }
    .filter-group select { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
    .filter-actions { margin-left: auto; }
    .solicitudes-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
    .sol-card { text-align: left; background: #faf8f9; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px; cursor: pointer; display: flex; flex-direction: column; gap: 6px; transition: border-color .15s, box-shadow .15s; }
    .sol-card:hover { border-color: #6b4c5b; }
    .sol-card.selected { border-color: #6b4c5b; background: #fff; box-shadow: 0 0 0 2px rgba(107, 76, 91, 0.18); }
    .sol-card__top { display: flex; justify-content: space-between; gap: 8px; }
    .sol-card__depto { font-size: 15px; color: #1f1a1d; }
    .sol-card__periodo { font-size: 13px; color: #5a4a53; }
    .sol-card__meta { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; font-size: 12px; color: #4b5563; margin-top: 4px; }
    .detail-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; margin-bottom: 14px; }
    .detail-badges { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 6px; }
    .detail-title { margin: 0; font-size: 20px; color: #1f1a1d; }
    .detail-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .sub-desc { margin: 6px 0 0; font-size: 13px; color: #5a4a53; }
    .sga-banner { padding: 10px 14px; border-radius: 8px; background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; font-size: 13px; margin-bottom: 14px; }
    .sga-banner--obs { background: #fef2f2; border-color: #fecaca; color: #991b1b; }
    .inner-tabs { display: flex; gap: 8px; border-bottom: 1px solid #e5e7eb; margin-bottom: 16px; }
    .itab { background: none; border: none; padding: 8px 14px; font-size: 13px; font-weight: 600; color: #5a4a53; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
    .itab.on { color: #6b4c5b; border-bottom-color: #e5b115; }
    .table-wrap { overflow-x: auto; }
    .tabla { width: 100%; border-collapse: collapse; font-size: 13px; }
    .tabla th { text-align: left; padding: 10px 12px; background: #f8f5f7; border-bottom: 1px solid #e5e7eb; color: #4a3741; font-weight: 600; }
    .tabla td { padding: 12px; border-bottom: 1px solid #f1f1f4; vertical-align: top; }
    .tabla--compact td, .tabla--compact th { padding: 8px 10px; }
    .row-excluded { opacity: 0.58; background: #f9fafb; }
    .doc-name { font-weight: 700; color: #1f1a1d; font-size: 14px; }
    .doc-meta { font-size: 12px; color: #5a4a53; margin-top: 2px; }
    .cargo-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    .cargo-title { font-weight: 600; color: #1f1a1d; }
    .cap-pill { display: inline-block; margin-top: 6px; padding: 3px 8px; border-radius: 999px; font-size: 11px; background: #f3f4f6; color: #374151; }
    .cap-pill--full { background: #fef3c7; color: #92400e; }
    .dist-summary { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .dist-chips { display: flex; flex-wrap: wrap; gap: 6px; font-size: 11px; color: #4b5563; }
    .dist-chips span { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
    .asig-list { display: flex; flex-direction: column; gap: 6px; }
    .asig-chip { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; }
    .asig-text { color: #1f1a1d; }
    .actions-cell { display: flex; flex-wrap: wrap; gap: 6px; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
    .badge--tipo { background: #ede9fe; color: #5b21b6; }
    .badge--origen { background: #e0e7ff; color: #3730a3; }
    .badge--estado-borrador { background: #e0f2fe; color: #075985; }
    .badge--estado-presentada_sga { background: #fef3c7; color: #92400e; }
    .badge--estado-aprobada { background: #d1fae5; color: #065f46; }
    .badge--estado-observada { background: #fee2e2; color: #991b1b; }
    .badge--item-nuevo { background: #dcfce7; color: #166534; }
    .badge--item-replicado { background: #e0e7ff; color: #3730a3; }
    .badge--item-modificado { background: #fef3c7; color: #92400e; }
    .badge--item-excluido { background: #e5e7eb; color: #4b5563; }
    .badge--warn { background: #fef3c7; color: #92400e; border: 1px solid #fcd34d; }
    .badge--ok { background: #d1fae5; color: #065f46; }
    .badge--muted { background: #f3f4f6; color: #4b5563; }
    .badge--clases { background: #dbeafe; color: #1e40af; }
    .badge--finales { background: #fef3c7; color: #92400e; }
    .btn-pri { background: #6b4c5b; color: #fff; border: none; border-radius: 8px; padding: 9px 16px; font-weight: 600; font-size: 14px; cursor: pointer; }
    .btn-pri:hover:not(:disabled) { background: #563c49; }
    .btn-pri:disabled, .btn-ok:disabled, .btn-warn-act:disabled { opacity: .55; cursor: not-allowed; }
    .btn-ok { background: #059669; color: #fff; border: none; border-radius: 8px; padding: 9px 14px; font-weight: 600; font-size: 14px; cursor: pointer; }
    .btn-warn-act { background: #d97706; color: #fff; border: none; border-radius: 8px; padding: 9px 14px; font-weight: 600; font-size: 14px; cursor: pointer; }
    .btn-sec { background: #f3f4f6; color: #1f2937; border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 14px; font-weight: 600; font-size: 14px; cursor: pointer; }
    .btn-sm { background: #fff; color: #374151; border: 1px solid #d1d5db; border-radius: 6px; padding: 4px 9px; font-size: 12px; font-weight: 600; cursor: pointer; }
    .btn-sm:hover { background: #f9fafb; border-color: #6b4c5b; }
    .btn-reactivar { color: #065f46; border-color: #a7f3d0; background: #ecfdf5; }
    .btn-danger { color: #b91c1c; border-color: #fecaca; }
    .btn-danger:hover { background: #fef2f2; }
    .empty-box { text-align: center; padding: 28px; color: #5a4a53; }
    .muted { color: #6b7280; font-size: 14px; }
    .muted-sm { color: #9ca3af; font-size: 12px; font-style: italic; }
    .nowrap { white-space: nowrap; }
    .diff-old { color: #991b1b; font-size: 12px; }
    .diff-new { color: #065f46; font-size: 12px; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(17, 24, 39, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .modal { background: #fff; border-radius: 12px; padding: 24px; width: 100%; max-width: 560px; max-height: 90vh; overflow-y: auto; }
    .modal--wide { max-width: 780px; }
    .modal h3 { margin: 0 0 12px; color: #1f1a1d; }
    .form-grid { display: flex; flex-direction: column; gap: 12px; margin: 14px 0; }
    .form-grid label, .dist-grid label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; font-weight: 600; color: #374151; }
    .form-grid input[type="text"], .form-grid input[type="number"], .form-grid select, .form-grid textarea, .dist-grid input { padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; font-weight: 400; }
    .chk-box { flex-direction: row !important; align-items: flex-start; gap: 10px !important; background: #f8f5f7; border: 1px solid #e5e7eb; padding: 10px 12px; border-radius: 8px; font-weight: 400 !important; }
    .chk-box input { margin-top: 3px; }
    .motivo-highlight { background: #fffbeb; border: 1px solid #fde68a; padding: 10px; border-radius: 8px; }
    .dist-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin: 14px 0; }
    .preview-cap { padding: 10px 12px; border-radius: 8px; background: #ecfdf5; color: #065f46; font-size: 13px; margin-top: 8px; }
    .preview-cap--warn { background: #fffbeb; color: #92400e; border: 1px solid #fcd34d; }
    .preview-cap--over { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .asig-form-box { margin-top: 16px; padding-top: 14px; border-top: 1px solid #e5e7eb; }
    .asig-form-box h4 { margin: 0 0 8px; font-size: 15px; color: #1f1a1d; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicaSolicitudesDesignacionComponent implements OnInit {
  private readonly auth = inject(Auth);
  private readonly destroyRef = inject(DestroyRef);
  private readonly solicitudService = inject(SolicitudDesignacionService);
  private readonly parametrosService = inject(DesignacionParametrosService);
  private readonly datosCentralesService = inject(DatosCentralesService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly planEstudioService = inject(PlanEstudioService);

  private readonly session = toSignal(this.auth.currentSession$, { initialValue: this.auth.currentSession });

  readonly departamentos = signal<DepartamentoAcademico[]>([]);
  readonly periodos = signal<PeriodoAcademico[]>([]);
  readonly cargos = signal<CargoDocente[]>([]);
  readonly dedicaciones = signal<DedicacionDocente[]>([]);
  readonly tiposDesignacion = signal<TipoDesignacionDocente[]>([]);
  readonly docentes = signal<CuerpoAcademicoMaestro[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly planesCarrera = signal<PlanEstudioMaestro[]>([]);
  readonly asignaturasPlan = signal<ActividadCurricularPlan[]>([]);

  readonly filtroPeriodoId = signal<string>('');
  readonly filtroDepartamentoId = signal<string>('');
  readonly tabDetalle = signal<TabDetalle>('DESIGNACIONES');

  readonly solicitudes = signal<SolicitudDesignacionResumen[]>([]);
  readonly solicitudSeleccionada = signal<SolicitudDesignacionDetalle | null>(null);
  readonly cargandoLista = signal(false);
  readonly cargandoDetalle = signal(false);
  readonly guardando = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly modalNuevaSolicitud = signal(false);
  readonly modalDesignacion = signal(false);
  readonly editandoDesignacionItem = signal<DesignacionDocenteItem | null>(null);
  readonly modalDistribucion = signal<DesignacionDocenteItem | null>(null);
  readonly modalAsignaciones = signal<DesignacionDocenteItem | null>(null);
  readonly modalExcluir = signal<DesignacionDocenteItem | null>(null);
  readonly modalRevisionSga = signal<'APROBAR' | 'OBSERVAR' | null>(null);

  motivoExclusion = '';
  observacionesSgaModal = '';

  nuevaSolForm: {
    departamentoAcademicoId: string;
    periodoAcademicoId: string;
    tipoSolicitud: TipoSolicitudDesignacion;
    clonarPeriodoAnterior: boolean;
    observacionesDepartamento: string;
  } = {
    departamentoAcademicoId: '',
    periodoAcademicoId: '',
    tipoSolicitud: 'ANUAL',
    clonarPeriodoAnterior: true,
    observacionesDepartamento: '',
  };

  desigForm = {
    docenteId: '',
    cargoDocenteId: '',
    dedicacionDocenteId: '',
    tipoDesignacionId: '',
    nroDesignacionMapuche: '',
    observaciones: '',
    motivoCambio: '',
  };

  distForm = {
    horasClases: 0,
    horasInvestigacion: 0,
    horasExtension: 0,
    horasGestion: 0,
    horasDireccionTesis: 0,
    horasOtras: 0,
    motivoCambio: '',
  };

  asigForm: {
    carreraId: string;
    planEstudioId: string;
    actividadCurricularId: string;
    modalidadParticipacion: ModalidadParticipacionDocente;
    horasAsignadas: number | null;
    observaciones: string;
    motivoCambio: string;
  } = {
    carreraId: '',
    planEstudioId: '',
    actividadCurricularId: '',
    modalidadParticipacion: 'DESARROLLO_CLASES',
    horasAsignadas: null,
    observaciones: '',
    motivoCambio: '',
  };

  readonly canManage = computed(() => {
    const roles = this.session()?.user?.roleCodes ?? [];
    return roles.includes('AEA') || roles.includes('SECRETARIA_ACADEMICA') || roles.includes('DIRECTOR_DEPARTAMENTO');
  });

  readonly canReviewSga = computed(() => {
    const roles = this.session()?.user?.roleCodes ?? [];
    return roles.includes('AEA') || roles.includes('SECRETARIA_ACADEMICA');
  });

  readonly esSoloDirectorDepto = computed(() => {
    const roles = this.session()?.user?.roleCodes ?? [];
    const institucional =
      roles.includes('AEA') ||
      roles.includes('SECRETARIA_ACADEMICA') ||
      roles.includes('CONSULTA_INSTITUCIONAL') ||
      roles.includes('DIRECTOR_ESCUELA') ||
      roles.includes('DIRECTOR_CARRERA');
    return !institucional && roles.includes('DIRECTOR_DEPARTAMENTO');
  });

  readonly docentesDisponibles = computed(() => {
    const detalle = this.solicitudSeleccionada();
    const todos = this.docentes();
    if (!detalle) return todos;
    const delDepto = todos.filter((d) => d.departamentoAcademicoId === detalle.departamentoAcademicoId);
    return this.esSoloDirectorDepto() ? delDepto : todos;
  });

  ngOnInit(): void {
    const userDepto = this.session()?.user?.departamentoAcademicoId ?? '';
    if (this.esSoloDirectorDepto() && userDepto) {
      this.filtroDepartamentoId.set(userDepto);
    }

    this.parametrosService
      .obtenerCatalogos(false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cat) => {
          this.departamentos.set(cat.departamentos);
          this.periodos.set(cat.periodos);
          this.cargos.set(cat.cargos);
          this.dedicaciones.set(cat.dedicaciones);
          this.tiposDesignacion.set(cat.tiposDesignacion);
          const actual = cat.periodos.find((p) => p.esActual);
          if (actual) {
            this.filtroPeriodoId.set(actual.id);
          }
          this.cargarSolicitudes();
        },
        error: () => this.mostrarError('No se pudieron cargar los catálogos docentes.'),
      });

    this.datosCentralesService
      .getCuerpoAcademico(undefined, undefined, undefined, false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.docentes.set(list) });

    this.estructuraService
      .listarCarreras()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.carreras.set(list) });
  }

  cargarSolicitudes(): void {
    this.cargandoLista.set(true);
    this.solicitudService
      .listarSolicitudes(this.filtroPeriodoId() || null, this.filtroDepartamentoId() || null)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.solicitudes.set(list);
          this.cargandoLista.set(false);
          const sel = this.solicitudSeleccionada();
          if (sel && list.some((s) => s.id === sel.id)) {
            this.seleccionarSolicitud(sel.id);
          } else if (list.length > 0) {
            this.seleccionarSolicitud(list[0].id);
          } else {
            this.solicitudSeleccionada.set(null);
          }
        },
        error: (err) => {
          this.cargandoLista.set(false);
          this.mostrarError(err?.error?.error ?? 'Error al cargar las solicitudes de designación.');
        },
      });
  }

  seleccionarSolicitud(solicitudId: string): void {
    this.cargandoDetalle.set(true);
    this.solicitudService
      .obtenerDetalle(solicitudId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (det) => {
          this.solicitudSeleccionada.set(det);
          this.cargandoDetalle.set(false);
          const modAsig = this.modalAsignaciones();
          if (modAsig) {
            const actualizado = det.designaciones.find((d) => d.id === modAsig.id) ?? null;
            this.modalAsignaciones.set(actualizado);
          }
        },
        error: (err) => {
          this.cargandoDetalle.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo cargar el detalle de la solicitud.');
        },
      });
  }

  requiereMotivoItem(item: DesignacionDocenteItem | null): boolean {
    const det = this.solicitudSeleccionada();
    if (!det) return false;
    if (det.estado === 'OBSERVADA') return true;
    return !!item && (!!item.designacionOrigenId || item.estadoItem === 'REPLICADO');
  }

  requiereMotivoModalCargo(): boolean {
    return this.requiereMotivoItem(this.editandoDesignacionItem());
  }

  abrirModalNuevaSolicitud(): void {
    const userDepto = this.session()?.user?.departamentoAcademicoId ?? '';
    this.nuevaSolForm = {
      departamentoAcademicoId: this.esSoloDirectorDepto() ? userDepto : this.filtroDepartamentoId(),
      periodoAcademicoId: this.filtroPeriodoId() || (this.periodos().find((p) => p.esActual)?.id ?? ''),
      tipoSolicitud: 'ANUAL',
      clonarPeriodoAnterior: true,
      observacionesDepartamento: '',
    };
    this.modalNuevaSolicitud.set(true);
  }

  crearSolicitud(): void {
    if (!this.nuevaSolForm.departamentoAcademicoId || !this.nuevaSolForm.periodoAcademicoId) {
      this.mostrarError('Debe seleccionar Departamento Académico y Período Académico.');
      return;
    }
    this.guardando.set(true);
    this.solicitudService
      .crearSolicitud({
        departamentoAcademicoId: this.nuevaSolForm.departamentoAcademicoId,
        periodoAcademicoId: this.nuevaSolForm.periodoAcademicoId,
        tipoSolicitud: this.nuevaSolForm.tipoSolicitud,
        clonarPeriodoAnterior:
          this.nuevaSolForm.tipoSolicitud === 'ANUAL' ? this.nuevaSolForm.clonarPeriodoAnterior : false,
        observacionesDepartamento: this.nuevaSolForm.observacionesDepartamento || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (det) => {
          this.guardando.set(false);
          this.modalNuevaSolicitud.set(false);
          this.solicitudSeleccionada.set(det);
          if (this.filtroPeriodoId() && this.filtroPeriodoId() !== det.periodoAcademicoId) {
            this.filtroPeriodoId.set(det.periodoAcademicoId);
          }
          if (this.filtroDepartamentoId() && this.filtroDepartamentoId() !== det.departamentoAcademicoId) {
            this.filtroDepartamentoId.set(det.departamentoAcademicoId);
          }
          const msgClon =
            det.solicitudOrigenId && det.tipoSolicitud === 'ANUAL'
              ? ` Se replicaron ${det.designaciones.length} designación(es) desde el período anterior.`
              : '';
          this.mostrarExito(`Solicitud creada en estado BORRADOR.${msgClon}`);
          this.cargarSolicitudes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo crear la solicitud.');
        },
      });
  }

  presentarASga(detalle: SolicitudDesignacionDetalle): void {
    if (!confirm('¿Presentar formalmente esta solicitud a Secretaría de Gestión Académica? Pasará a estado PRESENTADA_SGA.')) {
      return;
    }
    this.guardando.set(true);
    this.solicitudService
      .presentarSolicitud(detalle.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (det) => {
          this.guardando.set(false);
          this.solicitudSeleccionada.set(det);
          this.mostrarExito('Solicitud presentada a Secretaría de Gestión Académica.');
          this.cargarSolicitudes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo presentar la solicitud a SGA.');
        },
      });
  }

  abrirModalRevisionSga(modo: 'APROBAR' | 'OBSERVAR'): void {
    this.observacionesSgaModal = '';
    this.modalRevisionSga.set(modo);
  }

  confirmarRevisionSga(modo: 'APROBAR' | 'OBSERVAR'): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (modo === 'OBSERVAR' && !this.observacionesSgaModal.trim()) {
      this.mostrarError('Debe ingresar las observaciones de SGA para devolver la solicitud.');
      return;
    }
    this.guardando.set(true);
    const obs = { observacionesSga: this.observacionesSgaModal.trim() || null };
    const req$ =
      modo === 'APROBAR'
        ? this.solicitudService.aprobarSolicitud(detalle.id, obs)
        : this.solicitudService.observarSolicitud(detalle.id, obs);

    req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (det) => {
        this.guardando.set(false);
        this.modalRevisionSga.set(null);
        this.solicitudSeleccionada.set(det);
        this.mostrarExito(
          modo === 'APROBAR'
            ? 'Solicitud APROBADA por Secretaría de Gestión Académica.'
            : 'Solicitud devuelta como OBSERVADA al Departamento.'
        );
        this.cargarSolicitudes();
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarError(err?.error?.error ?? 'No se pudo registrar la revisión SGA.');
      },
    });
  }

  abrirModalDesignacion(item: DesignacionDocenteItem | null): void {
    if (item) {
      this.editandoDesignacionItem.set(item);
      this.desigForm = {
        docenteId: item.docenteId,
        cargoDocenteId: item.cargoDocenteId,
        dedicacionDocenteId: item.dedicacionDocenteId,
        tipoDesignacionId: item.tipoDesignacionId,
        nroDesignacionMapuche: item.nroDesignacionMapuche ?? '',
        observaciones: item.observaciones ?? '',
        motivoCambio: '',
      };
    } else {
      this.editandoDesignacionItem.set(null);
      this.desigForm = {
        docenteId: '',
        cargoDocenteId: '',
        dedicacionDocenteId: '',
        tipoDesignacionId: '',
        nroDesignacionMapuche: '',
        observaciones: '',
        motivoCambio: '',
      };
    }
    this.modalDesignacion.set(true);
  }

  docenteYaEnSolicitudActual(): boolean {
    const detalle = this.solicitudSeleccionada();
    return !!detalle?.designaciones.some((d) => d.docenteId === this.desigForm.docenteId && d.estadoItem !== 'EXCLUIDO');
  }

  horasProyectadasDocente(): number {
    const detalle = this.solicitudSeleccionada();
    const ded = this.dedicaciones().find((d) => d.id === this.desigForm.dedicacionDocenteId);
    const horasNuevas = ded?.horasSemanales ?? 0;
    if (!detalle || !this.desigForm.docenteId) return horasNuevas;

    const editItem = this.editandoDesignacionItem();
    const existente = detalle.designaciones.find(
      (d) => d.docenteId === this.desigForm.docenteId && d.estadoItem !== 'EXCLUIDO'
    );
    const totalActualDocente = existente?.totalHorasDocentePeriodo ?? 0;
    const horasEditando = editItem && editItem.estadoItem !== 'EXCLUIDO' ? editItem.horasDedicacion : 0;
    return totalActualDocente - horasEditando + horasNuevas;
  }

  guardarDesignacion(): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (!this.desigForm.docenteId || !this.desigForm.cargoDocenteId || !this.desigForm.dedicacionDocenteId || !this.desigForm.tipoDesignacionId) {
      this.mostrarError('Complete Docente, Cargo, Dedicación y Tipo de Designación.');
      return;
    }
    if (this.requiereMotivoModalCargo() && !this.desigForm.motivoCambio.trim()) {
      this.mostrarError('Debe informar el motivo del cambio (RF08).');
      return;
    }

    this.guardando.set(true);
    const payload = {
      docenteId: this.desigForm.docenteId,
      cargoDocenteId: this.desigForm.cargoDocenteId,
      dedicacionDocenteId: this.desigForm.dedicacionDocenteId,
      tipoDesignacionId: this.desigForm.tipoDesignacionId,
      nroDesignacionMapuche: this.desigForm.nroDesignacionMapuche || null,
      observaciones: this.desigForm.observaciones || null,
      motivoCambio: this.desigForm.motivoCambio.trim() || null,
    };

    const editItem = this.editandoDesignacionItem();
    const req$ = editItem
      ? this.solicitudService.actualizarDesignacion(detalle.id, editItem.id, payload)
      : this.solicitudService.agregarDesignacion(detalle.id, payload);

    req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalDesignacion.set(false);
        this.mostrarExito(editItem ? 'Designación actualizada y auditada.' : 'Designación agregada a la solicitud.');
        this.cargarSolicitudes();
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarError(err?.error?.error ?? 'No se pudo guardar la designación.');
      },
    });
  }

  abrirModalExcluir(item: DesignacionDocenteItem): void {
    this.motivoExclusion = '';
    this.modalExcluir.set(item);
  }

  confirmarExcluir(item: DesignacionDocenteItem): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (!this.motivoExclusion.trim()) {
      this.mostrarError('Debe indicar el motivo para excluir la designación (RF08).');
      return;
    }
    this.guardando.set(true);
    this.solicitudService
      .excluirDesignacion(detalle.id, item.id, { motivo: this.motivoExclusion.trim() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.modalExcluir.set(null);
          this.mostrarExito('Designación marcada como EXCLUIDA conservando historial.');
          this.cargarSolicitudes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo excluir la designación.');
        },
      });
  }

  reactivarDesignacion(item: DesignacionDocenteItem): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    this.guardando.set(true);
    this.solicitudService
      .reactivarDesignacion(detalle.id, item.id, { motivo: 'Reactivación en solicitud' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.mostrarExito('Designación reactivada.');
          this.cargarSolicitudes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo reactivar la designación.');
        },
      });
  }

  eliminarDesignacion(item: DesignacionDocenteItem): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (!confirm(`¿Eliminar definitivamente la designación nueva de ${item.docenteApellido}, ${item.docenteNombre}?`)) {
      return;
    }
    this.solicitudService
      .eliminarDesignacion(detalle.id, item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.mostrarExito('Designación eliminada.');
          this.cargarSolicitudes();
        },
        error: (err) => this.mostrarError(err?.error?.error ?? 'No se pudo eliminar la designación.'),
      });
  }

  abrirModalDistribucion(item: DesignacionDocenteItem): void {
    this.distForm = {
      horasClases: item.horasClases ?? 0,
      horasInvestigacion: item.horasInvestigacion ?? 0,
      horasExtension: item.horasExtension ?? 0,
      horasGestion: item.horasGestion ?? 0,
      horasDireccionTesis: item.horasDireccionTesis ?? 0,
      horasOtras: item.horasOtras ?? 0,
      motivoCambio: '',
    };
    this.modalDistribucion.set(item);
  }

  tieneNegativosDistribucion(): boolean {
    return (
      Number(this.distForm.horasClases) < 0 ||
      Number(this.distForm.horasInvestigacion) < 0 ||
      Number(this.distForm.horasExtension) < 0 ||
      Number(this.distForm.horasGestion) < 0 ||
      Number(this.distForm.horasDireccionTesis) < 0 ||
      Number(this.distForm.horasOtras) < 0
    );
  }

  sumaDistribucionModal(): number {
    return (
      (Number(this.distForm.horasClases) || 0) +
      (Number(this.distForm.horasInvestigacion) || 0) +
      (Number(this.distForm.horasExtension) || 0) +
      (Number(this.distForm.horasGestion) || 0) +
      (Number(this.distForm.horasDireccionTesis) || 0) +
      (Number(this.distForm.horasOtras) || 0)
    );
  }

  guardarDistribucion(item: DesignacionDocenteItem): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (this.requiereMotivoItem(item) && !this.distForm.motivoCambio.trim()) {
      this.mostrarError('Debe indicar el motivo del cambio horario (RF08).');
      return;
    }
    this.guardando.set(true);
    this.solicitudService
      .actualizarDistribucion(detalle.id, item.id, {
        horasClases: Number(this.distForm.horasClases) || 0,
        horasInvestigacion: Number(this.distForm.horasInvestigacion) || 0,
        horasExtension: Number(this.distForm.horasExtension) || 0,
        horasGestion: Number(this.distForm.horasGestion) || 0,
        horasDireccionTesis: Number(this.distForm.horasDireccionTesis) || 0,
        horasOtras: Number(this.distForm.horasOtras) || 0,
        motivoCambio: this.distForm.motivoCambio.trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.modalDistribucion.set(null);
          this.mostrarExito('Distribución horaria guardada.');
          this.cargarSolicitudes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo guardar la distribución horaria.');
        },
      });
  }

  abrirModalAsignaciones(item: DesignacionDocenteItem): void {
    this.asigForm = {
      carreraId: '',
      planEstudioId: '',
      actividadCurricularId: '',
      modalidadParticipacion: 'DESARROLLO_CLASES',
      horasAsignadas: null,
      observaciones: '',
      motivoCambio: '',
    };
    this.planesCarrera.set([]);
    this.asignaturasPlan.set([]);
    this.modalAsignaciones.set(item);
  }

  onCarreraChange(carreraId: string): void {
    this.asigForm.planEstudioId = '';
    this.asigForm.actividadCurricularId = '';
    this.planesCarrera.set([]);
    this.asignaturasPlan.set([]);
    if (!carreraId) return;
    this.datosCentralesService
      .getPlanesEstudio(carreraId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (planes) => this.planesCarrera.set(planes) });
  }

  onPlanChange(planId: string): void {
    this.asigForm.actividadCurricularId = '';
    this.asignaturasPlan.set([]);
    if (!planId) return;
    this.planEstudioService
      .listarAsignaturas(planId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (asigs) => this.asignaturasPlan.set(asigs) });
  }

  onActividadChange(actividadId: string): void {
    const act = this.asignaturasPlan().find((a) => a.id === actividadId);
    if (act?.estadoDictado === 'SOLO_EXAMENES_FINALES') {
      this.asigForm.modalidadParticipacion = 'EVALUACION_FINALES';
    } else if (act?.estadoDictado === 'ACTIVA') {
      this.asigForm.modalidadParticipacion = 'DESARROLLO_CLASES';
    }
  }

  actividadSeleccionadaEsSoloFinales(): boolean {
    const act = this.asignaturasPlan().find((a) => a.id === this.asigForm.actividadCurricularId);
    return act?.estadoDictado === 'SOLO_EXAMENES_FINALES';
  }

  agregarAsignacion(item: DesignacionDocenteItem): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (!this.asigForm.carreraId || !this.asigForm.planEstudioId || !this.asigForm.actividadCurricularId) {
      this.mostrarError('Seleccione Carrera, Plan de Estudio y Asignatura.');
      return;
    }
    if (this.requiereMotivoItem(item) && !this.asigForm.motivoCambio.trim()) {
      this.mostrarError('Debe indicar el motivo para agregar asignaturas en este cargo (RF08).');
      return;
    }
    this.guardando.set(true);
    this.solicitudService
      .agregarAsignacion(detalle.id, item.id, {
        carreraId: this.asigForm.carreraId,
        planEstudioId: this.asigForm.planEstudioId,
        actividadCurricularId: this.asigForm.actividadCurricularId,
        modalidadParticipacion: this.asigForm.modalidadParticipacion,
        horasAsignadas: this.asigForm.horasAsignadas,
        observaciones: this.asigForm.observaciones || null,
        motivoCambio: this.asigForm.motivoCambio.trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.asigForm.actividadCurricularId = '';
          this.asigForm.horasAsignadas = null;
          this.mostrarExito('Asignatura asignada correctamente.');
          this.seleccionarSolicitud(detalle.id);
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.error ?? 'No se pudo asignar la asignatura.');
        },
      });
  }

  eliminarAsignacion(item: DesignacionDocenteItem, asignacionId: string): void {
    const detalle = this.solicitudSeleccionada();
    if (!detalle) return;
    if (this.requiereMotivoItem(item) && !this.asigForm.motivoCambio.trim()) {
      this.mostrarError('Indique primero el motivo del cambio arriba antes de quitar la asignatura (RF08).');
      return;
    }
    this.solicitudService
      .eliminarAsignacion(detalle.id, item.id, asignacionId, this.asigForm.motivoCambio.trim() || null)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.mostrarExito('Asignación quitada.');
          this.seleccionarSolicitud(detalle.id);
        },
        error: (err) => this.mostrarError(err?.error?.error ?? 'No se pudo quitar la asignación.'),
      });
  }

  etiquetaEstado(estado: string): string {
    switch (estado) {
      case 'BORRADOR':
        return 'Borrador';
      case 'PRESENTADA_SGA':
        return 'Presentada SGA';
      case 'APROBADA':
        return 'Aprobada';
      case 'OBSERVADA':
        return 'Observada';
      default:
        return estado;
    }
  }

  etiquetaEstadoItem(estado: string): string {
    switch (estado) {
      case 'NUEVO':
        return 'Nuevo';
      case 'REPLICADO':
        return 'Replicado';
      case 'MODIFICADO':
        return 'Modificado';
      case 'EXCLUIDO':
        return 'Excluido';
      default:
        return estado;
    }
  }

  etiquetaEvento(ev: string): string {
    switch (ev) {
      case 'CREACION':
        return 'Creación';
      case 'CLONACION_PERIODO_ANTERIOR':
        return 'Clonación período anterior';
      case 'ALTA_DESIGNACION':
        return 'Alta de cargo';
      case 'MODIFICACION_DESIGNACION':
        return 'Modificación de cargo';
      case 'MODIFICACION_DISTRIBUCION':
        return 'Distribución horaria';
      case 'EXCLUSION_DESIGNACION':
        return 'Exclusión de cargo';
      case 'REACTIVACION_DESIGNACION':
        return 'Reactivación de cargo';
      case 'ALTA_ASIGNACION':
        return 'Alta de asignatura';
      case 'BAJA_ASIGNACION':
        return 'Baja de asignatura';
      case 'PRESENTACION_SGA':
        return 'Presentación a SGA';
      case 'APROBACION_SGA':
        return 'Aprobación SGA';
      case 'OBSERVACION_SGA':
        return 'Observación SGA';
      default:
        return ev;
    }
  }

  private mostrarExito(msg: string): void {
    this.errorMessage.set(null);
    this.successMessage.set(msg);
  }

  private mostrarError(msg: string): void {
    this.successMessage.set(null);
    this.errorMessage.set(msg);
  }
}
