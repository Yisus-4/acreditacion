import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { AutoevaluacionContextService } from '../../../core/autoevaluacion/autoevaluacion-context.service';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import { ConvocatoriaDetalle, ConvocatoriaResumen } from '../../../core/convocatorias/models/convocatoria.model';
import { EstructuraService } from '../../../core/estructura/estructura.service';
import { CarreraResponse } from '../../../core/estructura/estructura.types';
import { PlanEstudioService } from '../../../core/planes-estudio/plan-estudio.service';
import { PlanEstudioVigenteDto } from '../../../core/planes-estudio/plan-estudio.models';

interface CarreraConvocadaItem {
  id: string;
  nombre: string;
  sigla?: string;
  procesoId?: string;
  estado?: string;
  rolEdicionDisponible: boolean;
  actividadesPermitidas: string[];
  planes?: PlanEstudioVigenteDto[];
}

@Component({
  selector: 'app-autoevaluacion-convocatorias-hub',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="hub-container">
      <!-- Breadcrumb y Encabezado Principal -->
      <nav class="hub-breadcrumb" aria-label="Navegación secundaria">
        <a routerLink="/dashboard" class="breadcrumb-link">Inicio</a>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-current">Autoevaluación Institucional</span>
      </nav>

      <div class="hub-header">
        <div class="hub-header-text">
          <h1 class="hub-title">Autoevaluación Institucional por Convocatoria</h1>
          <p class="hub-subtitle">
            Seleccione una convocatoria y la carrera convocada correspondiente para acceder a su espacio de trabajo aislado.
            Cada carrera recopila sus propios datos de infraestructura, ámbitos de práctica, planes de estudio y programas analíticos según los roles asignados.
          </p>
        </div>
        <div class="user-role-badge">
          <span class="role-pill">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            {{ userRolesLabel() }}
          </span>
        </div>
      </div>

      <!-- Estado de Carga -->
      @if (cargando()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Cargando convocatorias realizadas y carreras convocadas...</p>
        </div>
      } @else if (convocatorias().length === 0) {
        <div class="empty-state">
          <div class="empty-icon">📁</div>
          <h3>No se registran convocatorias realizadas</h3>
          <p>Actualmente no hay convocatorias de acreditación registradas en el sistema para realizar autoevaluación.</p>
          <a routerLink="/acreditacion/convocatorias" class="btn-primary">Ver Módulo de Convocatorias</a>
        </div>
      } @else {
        <!-- Listado de Convocatorias Realizadas -->
        <div class="convocatorias-list">
          @for (conv of convocatorias(); track conv.id) {
            <article class="convocatoria-card" [class.expanded]="convocatoriaAbiertaId() === conv.id">
              <header class="convocatoria-card-header">
                <div class="convocatoria-meta">
                  <div class="conv-badges">
                    <span class="badge-code">{{ conv.codigo }}</span>
                    <span class="badge-estado" [ngClass]="conv.estado.toLowerCase()">
                      {{ formatearEstado(conv.estado) }}
                    </span>
                  </div>
                  <h2 class="convocatoria-titulo">{{ conv.titulo }}</h2>
                  <p class="convocatoria-submeta">
                    <strong>Resolución CONEAU:</strong> {{ conv.nroResolucionConeau || 'S/N' }} &nbsp;|&nbsp;
                    <strong>Período:</strong> {{ conv.fechaInicio || 'Inicio' }} &rarr; {{ conv.fechaCierre || 'Cierre' }}
                  </p>
                </div>

                <div class="convocatoria-actions">
                  <button
                    type="button"
                    class="btn-toggle-convocatoria"
                    (click)="toggleConvocatoria(conv.id)"
                    [attr.aria-expanded]="convocatoriaAbiertaId() === conv.id"
                  >
                    @if (convocatoriaAbiertaId() === conv.id) {
                      <span>Ocultar Carreras</span>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="18 15 12 9 6 15"></polyline>
                      </svg>
                    } @else {
                      <span>Acceder y Ver Carreras</span>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="6 9 12 15 18 9"></polyline>
                      </svg>
                    }
                  </button>
                </div>
              </header>

              <!-- Panel Desplegable: Carreras Convocadas -->
              @if (convocatoriaAbiertaId() === conv.id) {
                <section class="carreras-section">
                  <div class="carreras-section-header">
                    <h3 class="carreras-title">
                      Carreras Convocadas en {{ conv.codigo }}
                    </h3>
                    <span class="carreras-count">
                      {{ obtenerCarrerasDeConvocatoria(conv.id).length }} carrera(s) vinculada(s)
                    </span>
                  </div>

                  @if (cargandoDetalleConvocatoriaId() === conv.id) {
                    <div class="carreras-loading">
                      <div class="spinner-small"></div>
                      <span>Obteniendo carreras convocadas...</span>
                    </div>
                  } @else if (obtenerCarrerasDeConvocatoria(conv.id).length === 0) {
                    <div class="carreras-empty">
                      <p>Esta convocatoria no posee carreras asignadas aún.</p>
                    </div>
                  } @else {
                    <div class="carreras-grid">
                      @for (carrera of obtenerCarrerasDeConvocatoria(conv.id); track carrera.id) {
                        <div class="carrera-card">
                          <div class="carrera-card-body">
                            <div class="carrera-header">
                              <span class="badge-carrera-sigla">{{ carrera.sigla || 'CARRERA' }}</span>
                              @if (carrera.rolEdicionDisponible) {
                                <span class="badge-permiso editor">Rol con Carga Habilitada</span>
                              } @else {
                                <span class="badge-permiso lector">Solo Consulta</span>
                              }
                            </div>

                            <h4 class="carrera-nombre">{{ carrera.nombre }}</h4>

                            @if (carrera.planes && carrera.planes.length > 0) {
                              <div style="display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 10px;">
                                @for (p of carrera.planes; track p.id) {
                                  <span style="font-size: 11px; font-weight: 600; background: #e0e7ff; color: #3730a3; padding: 2px 7px; border-radius: 12px;">
                                    {{ p.codigoResolucion || ('Plan ' + p.anioAprobacion) }}
                                  </span>
                                }
                              </div>
                            }

                            <div class="actividades-permitidas">
                              <span class="actividades-label">Actividades según rol asignado:</span>
                              <div class="chips-container">
                                @for (act of carrera.actividadesPermitidas; track act) {
                                  <span class="chip-actividad">{{ act }}</span>
                                }
                              </div>
                            </div>
                          </div>

                          <div class="carrera-card-footer">
                            <button
                              type="button"
                              class="btn-acceder-carrera"
                              (click)="ingresarAutoevaluacionCarrera(conv.id, carrera.id)"
                            >
                              <span>Acceder a Autoevaluación</span>
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                                <polyline points="12 5 19 12 12 19"></polyline>
                              </svg>
                            </button>
                          </div>
                        </div>
                      }
                    </div>
                  }
                </section>
              }
            </article>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .hub-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 20px 48px;
      font-family: inherit;
    }

    .hub-breadcrumb {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #6b7280;
      margin-bottom: 16px;
    }

    .breadcrumb-link {
      color: #6b4c5b;
      text-decoration: none;
      font-weight: 500;
      &:hover {
        text-decoration: underline;
      }
    }

    .breadcrumb-separator {
      color: #9ca3af;
    }

    .breadcrumb-current {
      color: #374151;
      font-weight: 600;
    }

    .hub-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 28px;
      padding-bottom: 20px;
      border-bottom: 1px solid #e5e7eb;
    }

    .hub-title {
      font-size: 24px;
      font-weight: 700;
      color: #1f1a1d;
      margin: 0 0 8px 0;
      letter-spacing: -0.01em;
    }

    .hub-subtitle {
      font-size: 14.5px;
      line-height: 1.55;
      color: #4b5563;
      margin: 0;
      max-width: 820px;
    }

    .user-role-badge {
      flex-shrink: 0;
    }

    .role-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #fdfafb;
      border: 1px solid #d4c5cb;
      color: #6b4c5b;
      font-size: 12.5px;
      font-weight: 600;
      padding: 6px 12px;
      border-radius: 20px;
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      gap: 16px;
      color: #6b7280;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #f3e8ee;
      border-top-color: #6b4c5b;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .spinner-small {
      width: 20px;
      height: 20px;
      border: 2px solid #f3e8ee;
      border-top-color: #6b4c5b;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .empty-state {
      background: #ffffff;
      border: 1px dashed #d1d5db;
      border-radius: 12px;
      padding: 48px 24px;
      text-align: center;
      max-width: 540px;
      margin: 40px auto;
    }

    .empty-icon {
      font-size: 40px;
      margin-bottom: 12px;
    }

    .empty-state h3 {
      font-size: 18px;
      color: #1f1a1d;
      margin: 0 0 8px;
    }

    .empty-state p {
      font-size: 14px;
      color: #6b7280;
      margin: 0 0 20px;
    }

    .btn-primary {
      display: inline-block;
      background: #6b4c5b;
      color: #ffffff;
      padding: 10px 20px;
      border-radius: 6px;
      font-weight: 600;
      text-decoration: none;
      font-size: 14px;
      transition: background-color 0.2s;
      &:hover {
        background: #533946;
      }
    }

    .convocatorias-list {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .convocatoria-card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      overflow: hidden;
      transition: border-color 0.2s, box-shadow 0.2s;

      &:hover {
        border-color: #d1d5db;
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.06);
      }

      &.expanded {
        border-color: #826170;
        box-shadow: 0 4px 12px rgba(107, 76, 91, 0.08);
      }
    }

    .convocatoria-card-header {
      padding: 20px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      background: #ffffff;
    }

    .convocatoria-meta {
      flex: 1;
    }

    .conv-badges {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .badge-code {
      font-size: 12px;
      font-weight: 700;
      color: #6b4c5b;
      background: #f7eff3;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .badge-estado {
      font-size: 11px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 4px;
      text-transform: uppercase;

      &.en_curso {
        background: #ecfdf5;
        color: #065f46;
      }
      &.en_preparacion {
        background: #fef3c7;
        color: #92400e;
      }
      &.borrador {
        background: #f3f4f6;
        color: #374151;
      }
      &.finalizada {
        background: #f1f5f9;
        color: #475569;
      }
    }

    .convocatoria-titulo {
      font-size: 18px;
      font-weight: 700;
      color: #111827;
      margin: 0 0 6px 0;
    }

    .convocatoria-submeta {
      font-size: 13px;
      color: #6b7280;
      margin: 0;
    }

    .btn-toggle-convocatoria {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: #826170;
      color: #ffffff;
      border: none;
      border-radius: 6px;
      padding: 10px 18px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s ease, transform 0.1s ease;

      &:hover {
        background: #6b4c5b;
      }

      &:active {
        transform: translateY(1px);
      }
    }

    .carreras-section {
      background: #fbf9fa;
      border-top: 1px solid #eedde5;
      padding: 20px 24px 24px;
    }

    .carreras-section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .carreras-title {
      font-size: 14.5px;
      font-weight: 700;
      color: #4a2c3a;
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .carreras-count {
      font-size: 13px;
      color: #6b7280;
      font-weight: 500;
    }

    .carreras-loading, .carreras-empty {
      padding: 24px;
      text-align: center;
      color: #6b7280;
      font-size: 13.5px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
    }

    .carreras-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }

    .carrera-card {
      background: #ffffff;
      border: 1px solid #d4c5cb;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      box-shadow: 0 1px 3px rgba(107, 76, 91, 0.04);
      transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;

      &:hover {
        border-color: #826170;
        box-shadow: 0 4px 10px rgba(107, 76, 91, 0.1);
        transform: translateY(-2px);
      }
    }

    .carrera-card-body {
      padding: 16px 18px;
    }

    .carrera-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
    }

    .badge-carrera-sigla {
      font-size: 11px;
      font-weight: 700;
      background: #e0e7ff;
      color: #3730a3;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .badge-permiso {
      font-size: 11px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 12px;

      &.editor {
        background: #ecfdf5;
        color: #065f46;
      }
      &.lector {
        background: #f3f4f6;
        color: #4b5563;
      }
    }

    .carrera-nombre {
      font-size: 16px;
      font-weight: 700;
      color: #1f1a1d;
      margin: 0 0 12px 0;
      line-height: 1.35;
    }

    .actividades-permitidas {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .actividades-label {
      font-size: 11.5px;
      font-weight: 600;
      color: #6b7280;
    }

    .chips-container {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }

    .chip-actividad {
      font-size: 11px;
      background: #f3f4f6;
      color: #374151;
      padding: 2px 7px;
      border-radius: 4px;
      border: 1px solid #e5e7eb;
    }

    .carrera-card-footer {
      padding: 12px 18px;
      background: #faf7f8;
      border-top: 1px solid #f0e6eb;
    }

    .btn-acceder-carrera {
      width: 100%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: #6b4c5b;
      color: #ffffff;
      border: none;
      border-radius: 6px;
      padding: 9px 14px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s ease;

      &:hover {
        background: #533946;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AutoevaluacionConvocatoriasHubComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly auth = inject(Auth);
  readonly contextService = inject(AutoevaluacionContextService);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly planEstudioService = inject(PlanEstudioService);

  readonly cargando = signal<boolean>(true);
  readonly convocatorias = signal<ConvocatoriaResumen[]>([]);
  readonly carrerasTodas = signal<CarreraResponse[]>([]);
  readonly planesPorCarreraMap = signal<Map<string, PlanEstudioVigenteDto[]>>(new Map());
  readonly convocatoriaAbiertaId = signal<string | null>(null);
  readonly cargandoDetalleConvocatoriaId = signal<string | null>(null);

  // Mapa en caché de detalles por ID de convocatoria
  readonly detallesConvocatoria = signal<Map<string, ConvocatoriaDetalle>>(new Map());

  ngOnInit(): void {
    this.cargarDatosIniciales();
  }

  cargarDatosIniciales(): void {
    this.cargando.set(true);
    this.convocatoriaService.listarConvocatorias().subscribe({
      next: (convs) => {
        this.convocatorias.set(convs);
        this.estructuraService.listarCarreras().subscribe({
          next: (carrs) => {
            this.carrerasTodas.set(carrs);
            this.cargando.set(false);

            // Si hay una convocatoria activa preferente (o ya guardada en contexto), abrirla
            const idEnContexto = this.contextService.convocatoriaSeleccionadaId();
            if (idEnContexto && convs.some((c) => c.id === idEnContexto)) {
              this.toggleConvocatoria(idEnContexto);
            } else if (convs.length > 0) {
              const activa = convs.find((c) => c.estado === 'EN_CURSO' || c.estado === 'EN_PREPARACION') || convs[0];
              this.toggleConvocatoria(activa.id);
            }
          },
          error: (err) => {
            console.error('Error al listar carreras', err);
            this.cargando.set(false);
          },
        });
      },
      error: (err) => {
        console.error('Error al listar convocatorias', err);
        this.cargando.set(false);
      },
    });
  }

  toggleConvocatoria(convocatoriaId: string): void {
    if (this.convocatoriaAbiertaId() === convocatoriaId) {
      this.convocatoriaAbiertaId.set(null);
      return;
    }

    this.convocatoriaAbiertaId.set(convocatoriaId);

    // Si ya tenemos el detalle en caché, no volver a pedirlo
    if (this.detallesConvocatoria().has(convocatoriaId)) {
      return;
    }

    this.cargandoDetalleConvocatoriaId.set(convocatoriaId);
    this.convocatoriaService.obtenerConvocatoria(convocatoriaId).subscribe({
      next: (detalle) => {
        const mapa = new Map(this.detallesConvocatoria());
        mapa.set(convocatoriaId, detalle);
        this.detallesConvocatoria.set(mapa);
        this.cargandoDetalleConvocatoriaId.set(null);
      },
      error: (err) => {
        console.error('Error al obtener detalle de convocatoria', err);
        this.cargandoDetalleConvocatoriaId.set(null);
      },
    });
  }

  obtenerCarrerasDeConvocatoria(convocatoriaId: string): CarreraConvocadaItem[] {
    const detalle = this.detallesConvocatoria().get(convocatoriaId);
    const todas = this.carrerasTodas();

    let carrerasBase: CarreraResponse[] = [];
    if (detalle?.carrerasPropuestas && detalle.carrerasPropuestas.length > 0) {
      const idsPropuestas = new Set(detalle.carrerasPropuestas.map((cp) => cp.carreraId));
      carrerasBase = todas.filter((c) => idsPropuestas.has(c.id));
    }

    if (carrerasBase.length === 0) {
      if (detalle?.carreraId) {
        carrerasBase = todas.filter((c) => c.id === detalle.carreraId);
      } else {
        carrerasBase = todas;
      }
    }

    const tieneCargaHabilitada =
      this.contextService.puedeEditarInfraestructura() ||
      this.contextService.puedeEditarBiblioteca() ||
      this.contextService.puedeEditarAmbitosYConvenios() ||
      this.contextService.puedeEditarPlanesEstudio() ||
      this.contextService.puedeEditarProgramasAnaliticos();

    return carrerasBase.map((c) => {
      const matchPropuesta = detalle?.carrerasPropuestas?.find((cp) => cp.carreraId === c.id);
      const actividades: string[] = [];

      if (this.contextService.puedeEditarInfraestructura()) actividades.push('Infraestructura (8.1)');
      if (this.contextService.puedeEditarBiblioteca()) actividades.push('Biblioteca (8.8)');
      if (this.contextService.puedeEditarAmbitosYConvenios()) actividades.push('Ámbitos y Convenios (8.2)');
      if (this.contextService.puedeEditarPlanesEstudio()) actividades.push('Planes FT-001 (8.3)');
      if (this.contextService.puedeEditarProgramasAnaliticos()) actividades.push('Programas (8.4)');

      if (actividades.length === 0) {
        actividades.push('Consulta de Autoevaluación');
      }

      if (!this.planesPorCarreraMap().has(c.id)) {
        this.cargarPlanesCarrera(c.id);
      }

      return {
        id: c.id,
        nombre: c.nombre,
        sigla: c.sigla,
        procesoId: matchPropuesta?.procesoId || detalle?.id,
        estado: matchPropuesta?.estadoProceso || 'En Curso',
        rolEdicionDisponible: tieneCargaHabilitada,
        actividadesPermitidas: actividades,
        planes: this.planesPorCarreraMap().get(c.id) || [],
      };
    });
  }

  cargarPlanesCarrera(carreraId: string): void {
    this.planEstudioService.listarPlanesVigentesPorCarrera(carreraId).subscribe({
      next: (planes) => {
        const mapa = new Map(this.planesPorCarreraMap());
        mapa.set(carreraId, planes || []);
        this.planesPorCarreraMap.set(mapa);
      },
      error: () => {},
    });
  }

  ingresarAutoevaluacionCarrera(convocatoriaId: string, carreraId: string): void {
    this.contextService.seleccionarConvocatoriaYCarrera(convocatoriaId, carreraId, '/autoevaluacion/infraestructura');
  }

  userRolesLabel(): string {
    const user = this.auth.currentSession?.user;
    if (!user || !user.roleCodes || user.roleCodes.length === 0) {
      return 'Usuario';
    }
    return user.roleCodes.join(', ');
  }

  formatearEstado(estado?: string): string {
    if (!estado) return 'En Curso';
    switch (estado.toUpperCase()) {
      case 'EN_CURSO':
        return 'En Curso';
      case 'EN_PREPARACION':
        return 'En Preparación';
      case 'BORRADOR':
        return 'Borrador';
      case 'FINALIZADA':
        return 'Finalizada';
      default:
        return estado;
    }
  }
}
