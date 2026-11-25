import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { Auth } from '../auth/auth';
import { ConvocatoriaService } from '../convocatorias/convocatoria.service';
import { ConvocatoriaDetalle, ConvocatoriaResumen } from '../convocatorias/models/convocatoria.model';
import { EstructuraService } from '../estructura/estructura.service';
import { CarreraResponse } from '../estructura/estructura.types';
import { PlanEstudioFt001, PlanEstudioVigenteDto } from '../planes-estudio/plan-estudio.models';
import { PlanEstudioService } from '../planes-estudio/plan-estudio.service';

export interface AutoevaluacionContextState {
  convocatorias: ConvocatoriaResumen[];
  convocatoriaSeleccionadaId: string | null;
  convocatoriaDetalle: ConvocatoriaDetalle | null;
  carrerasTodas: CarreraResponse[];
  carrerasDisponibles: CarreraResponse[];
  carreraSeleccionadaId: string | null;
  procesoIdActivo: string | null;
  cargando: boolean;
}

export interface StoredAutoevalContext {
  convocatoriaId: string | null;
  carreraId: string | null;
  procesoId: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class AutoevaluacionContextService {
  private readonly router = inject(Router);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly planEstudioService = inject(PlanEstudioService);
  private readonly auth = inject(Auth);

  readonly STORAGE_KEY = 'acreditacion_autoeval_context';

  // Guard flag to prevent duplicate HTTP calls on multiple init invocations
  private inicializado = false;

  // Writable signals
  readonly convocatorias = signal<ConvocatoriaResumen[]>([]);
  readonly convocatoriaSeleccionadaId = signal<string | null>(null);
  readonly convocatoriaDetalle = signal<ConvocatoriaDetalle | null>(null);
  readonly carrerasTodas = signal<CarreraResponse[]>([]);
  readonly carreraSeleccionadaId = signal<string | null>(null);
  readonly cargando = signal<boolean>(false);

  // Multi-Plan Signals
  readonly planesConvocados = signal<PlanEstudioFt001[]>([]);
  readonly planConvocadoActivoId = signal<string | null>(null);

  readonly planConvocadoActivo = computed<PlanEstudioFt001 | null>(() => {
    const list = this.planesConvocados();
    const activeId = this.planConvocadoActivoId();
    if (!activeId) return list.length > 0 ? list[0] : null;
    return list.find((p) => p.id === activeId) || (list.length > 0 ? list[0] : null);
  });

  // Computed signals
  readonly carrerasDisponibles = computed<CarreraResponse[]>(() => {
    const detalle = this.convocatoriaDetalle();
    const todas = this.carrerasTodas();
    let baseCarreras = todas;

    if (detalle && detalle.carrerasPropuestas && detalle.carrerasPropuestas.length > 0) {
      const aprobadasIds = new Set(
        detalle.carrerasPropuestas
          .filter((cp) => !!cp.seleccionadaPorDe)
          .map((cp) => cp.carreraId)
      );
      baseCarreras = todas.filter((c) => aprobadasIds.has(c.id));
    }

    // Multi-tenancy filter by Academic Unit for non-AEA academic roles
    const currentUser = this.auth.currentSession?.user;
    const esAea = this.auth.hasAnyRole('AEA');

    if (!esAea && currentUser?.unidadAcademicaId) {
      const esAcademico = this.auth.hasAnyRole(
        'DIRECTOR_CARRERA',
        'DIRECTOR_ESCUELA',
        'DIRECTOR_DEPARTAMENTO',
        'COMISION_AUTOEVALUACION',
        'DOCENTE'
      );
      if (esAcademico) {
        return baseCarreras.filter(
          (carrera) => carrera.unidadAcademicaId === currentUser.unidadAcademicaId
        );
      }
    }

    return baseCarreras;
  });

  constructor() {
    effect(() => {
      const disp = this.carrerasDisponibles();
      const current = this.carreraSeleccionadaId();
      if (current && !disp.some((c) => c.id === current)) {
        const defaultCarId = disp.length > 0 ? disp[0].id : null;
        this.carreraSeleccionadaId.set(defaultCarId);
      }
    });
  }

  readonly procesoIdActivo = computed<string | null>(() => {
    const detalle = this.convocatoriaDetalle();
    const carreraId = this.carreraSeleccionadaId();
    if (!detalle || !carreraId) return null;

    if (detalle.carrerasPropuestas && detalle.carrerasPropuestas.length > 0) {
      const match = detalle.carrerasPropuestas.find((cp) => cp.carreraId === carreraId);
      return match?.procesoId || null;
    }
    if (detalle.carreraId === carreraId) {
      return detalle.id;
    }
    return null;
  });

  readonly carreraActiva = computed<CarreraResponse | null>(() => {
    const carId = this.carreraSeleccionadaId();
    if (!carId) return null;
    return this.carrerasTodas().find((c) => c.id === carId) || null;
  });

  readonly convocatoriaActiva = computed<ConvocatoriaResumen | null>(() => {
    const convId = this.convocatoriaSeleccionadaId();
    if (!convId) return null;
    return this.convocatorias().find((c) => c.id === convId) || null;
  });

  // Permisos reactivos según roles del usuario para cada actividad de autoevaluación
  readonly puedeEditarInfraestructura = computed<boolean>(() => {
    return this.auth.hasAnyRole(
      'AEA',
      'INFRAESTRUCTURA',
      'BEDELIA',
      'HIGIENE_SEGURIDAD',
      'DIRECTOR_CARRERA',
      'COMISION_AUTOEVALUACION'
    );
  });

  readonly puedeEditarBiblioteca = computed<boolean>(() => {
    return this.auth.hasAnyRole('AEA', 'BIBLIOTECA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION');
  });

  readonly puedeEditarAmbitosYConvenios = computed<boolean>(() => {
    return this.auth.hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION');
  });

  readonly puedeEditarPlanesEstudio = computed<boolean>(() => {
    return this.auth.hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION');
  });

  readonly puedeEditarProgramasAnaliticos = computed<boolean>(() => {
    return this.auth.hasAnyRole('AEA', 'DOCENTE', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION');
  });

  /**
   * Initializes the autoevaluation context with cold-start hydration order:
   * 1. URL query parameters (parsed via router.parseUrl)
   * 2. SessionStorage (`acreditacion_autoeval_context`)
   * 3. Convocatorias (preferring EN_CURSO/EN_PREPARACION) and Carreras (preferring Agrimensura)
   */
  inicializarContexto(): Observable<void> {
    if (this.inicializado) {
      return of(void 0);
    }
    this.inicializado = true;
    this.cargando.set(true);

    return forkJoin({
      convocatorias: this.convocatoriaService.listarConvocatorias(),
      carreras: this.estructuraService.listarCarreras(),
    }).pipe(
      switchMap(({ convocatorias, carreras }) => {
        this.convocatorias.set(convocatorias);
        this.carrerasTodas.set(carreras);

        // 1. Inspect active queryParams via router.parseUrl during cold start
        const tree = this.router.parseUrl(this.router.url);
        const queryParams = tree.queryParams || {};
        let cId: string | null = queryParams['convocatoriaId'] || null;
        let carId: string | null = queryParams['carreraId'] || null;

        // 2. Fallback to sessionStorage
        if (!cId || !carId) {
          try {
            const storedRaw = window.sessionStorage.getItem(this.STORAGE_KEY);
            if (storedRaw) {
              const stored: StoredAutoevalContext = JSON.parse(storedRaw);
              cId = cId || stored.convocatoriaId;
              carId = carId || stored.carreraId;
            }
          } catch (e) {
            console.warn('Error reading stored autoeval context', e);
          }
        }

        // 3. Fallback to API defaults for Convocatoria
        if (!cId && convocatorias.length > 0) {
          const active = convocatorias.find(
            (c) => c.estado === 'EN_CURSO' || c.estado === 'EN_PREPARACION'
          );
          cId = active ? active.id : convocatorias[0].id;
        }

        if (cId) {
          this.convocatoriaSeleccionadaId.set(cId);
          return this.convocatoriaService.obtenerConvocatoria(cId).pipe(
            catchError((err) => {
              console.error('Error fetching convocatoria detalle', err);
              return of(null);
            }),
            tap((detalle) => {
              if (detalle) {
                this.convocatoriaDetalle.set(detalle);
              }
              const disp = this.carrerasDisponibles();
              if (!carId || !disp.some((c) => c.id === carId)) {
                const agrimensura = disp.find((c) =>
                  c.nombre?.toLowerCase().includes('agrimensura')
                );
                carId = agrimensura ? agrimensura.id : disp.length > 0 ? disp[0].id : null;
              }
              if (carId) {
                this.carreraSeleccionadaId.set(carId);
                this.actualizarPlanesConvocados(carId);
              }
              this.sincronizarRouterYStorage();
              this.cargando.set(false);
            }),
            map(() => void 0)
          );
        } else {
          const disp = this.carrerasDisponibles();
          if (!carId && disp.length > 0) {
            const agrimensura = disp.find((c) =>
              c.nombre?.toLowerCase().includes('agrimensura')
            );
            carId = agrimensura ? agrimensura.id : disp[0].id;
          }
          if (carId) {
            this.carreraSeleccionadaId.set(carId);
            this.actualizarPlanesConvocados(carId);
          }
          this.sincronizarRouterYStorage();
          this.cargando.set(false);
          return of(void 0);
        }
      }),
      catchError((err) => {
        console.error('Error inicializando contexto de autoevaluación', err);
        this.cargando.set(false);
        return of(void 0);
      })
    );
  }

  /**
   * Updates selected convocatoria, fetches its details, adjusts carrera selection
   * if not applicable to the newly selected convocatoria, and synchronizes state.
   */
  seleccionarConvocatoria(convocatoriaId: string, syncRouter = true): void {
    if (
      this.convocatoriaSeleccionadaId() === convocatoriaId &&
      this.convocatoriaDetalle()?.id === convocatoriaId
    ) {
      return;
    }

    this.convocatoriaSeleccionadaId.set(convocatoriaId);
    this.convocatoriaService.obtenerConvocatoria(convocatoriaId).subscribe({
      next: (detalle) => {
        this.convocatoriaDetalle.set(detalle);
        const disp = this.carrerasDisponibles();
        const currentCar = this.carreraSeleccionadaId();

        if (!currentCar || !disp.some((c) => c.id === currentCar)) {
          const agrimensura = disp.find((c) =>
            c.nombre?.toLowerCase().includes('agrimensura')
          );
          const defaultCarId = agrimensura ? agrimensura.id : disp.length > 0 ? disp[0].id : null;
          this.carreraSeleccionadaId.set(defaultCarId);
          this.actualizarPlanesConvocados(defaultCarId);
        } else {
          this.actualizarPlanesConvocados(currentCar);
        }

        if (syncRouter) {
          this.sincronizarRouterYStorage();
        }
      },
      error: (err) => console.error('Error fetching convocatoria detalle', err),
    });
  }

  setPlanActivo(planId: string): void {
    if (this.planConvocadoActivoId() === planId) {
      return;
    }
    this.planConvocadoActivoId.set(planId);
  }

  actualizarPlanesConvocados(carreraId?: string | null, procesoId?: string | null): void {
    const carId = carreraId || this.carreraSeleccionadaId();
    const procId = procesoId || this.procesoIdActivo() || undefined;
    if (!carId) {
      this.planesConvocados.set([]);
      this.planConvocadoActivoId.set(null);
      return;
    }
    this.planEstudioService.listarPlanes(carId, procId).subscribe({
      next: (planes) => {
        this.planesConvocados.set(planes || []);
        const currentActive = this.planConvocadoActivoId();
        if (planes && planes.length > 0) {
          if (!currentActive || !planes.some((p) => p.id === currentActive)) {
            this.planConvocadoActivoId.set(planes[0].id || null);
          }
        } else {
          this.planConvocadoActivoId.set(null);
        }
      },
      error: (err) => console.error('Error fetching planes convocados', err),
    });
  }

  /**
   * Updates selected degree career and synchronizes state with router and storage.
   */
  seleccionarCarrera(carreraId: string, syncRouter = true): void {
    if (this.carreraSeleccionadaId() === carreraId) {
      return;
    }
    this.carreraSeleccionadaId.set(carreraId);
    this.actualizarPlanesConvocados(carreraId);
    if (syncRouter) {
      this.sincronizarRouterYStorage();
    }
  }

  /**
   * Selects convocatoria and carrera simultaneously and navigates to the target autoevaluation activity.
   */
  seleccionarConvocatoriaYCarrera(convocatoriaId: string, carreraId: string, rutaDestino = '/autoevaluacion/infraestructura'): void {
    this.convocatoriaSeleccionadaId.set(convocatoriaId);
    this.carreraSeleccionadaId.set(carreraId);
    this.convocatoriaService.obtenerConvocatoria(convocatoriaId).subscribe({
      next: (detalle) => {
        this.convocatoriaDetalle.set(detalle);
        this.actualizarPlanesConvocados(carreraId);
        const procesoId = this.procesoIdActivo();
        const payload: StoredAutoevalContext = {
          convocatoriaId,
          carreraId,
          procesoId,
        };
        try {
          window.sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(payload));
        } catch (e) {
          console.warn('Unable to persist autoeval context', e);
        }
        void this.router.navigate([rutaDestino], {
          queryParams: {
            convocatoriaId,
            carreraId,
            procesoId: procesoId || undefined,
          },
        });
      },
      error: (err) => {
        console.error('Error fetching convocatoria detalle', err);
        void this.router.navigate([rutaDestino], {
          queryParams: {
            convocatoriaId,
            carreraId,
          },
        });
      },
    });
  }

  /**
   * Synchronizes context state to sessionStorage and URL query params.
   * Includes guard against infinite router loops by checking whether incoming values differ.
   */
  sincronizarRouterYStorage(): void {
    const convocatoriaId = this.convocatoriaSeleccionadaId();
    const carreraId = this.carreraSeleccionadaId();
    const procesoId = this.procesoIdActivo();

    const payload: StoredAutoevalContext = {
      convocatoriaId,
      carreraId,
      procesoId,
    };

    try {
      window.sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(payload));
    } catch (e) {
      console.warn('Unable to persist autoeval context in sessionStorage', e);
    }

    // Guard against infinite router loops: compare current query params
    const currentTree = this.router.parseUrl(this.router.url);
    const currentParams = currentTree.queryParams || {};

    const convEqual = (currentParams['convocatoriaId'] || null) === (convocatoriaId || null);
    const carEqual = (currentParams['carreraId'] || null) === (carreraId || null);
    const procEqual = (currentParams['procesoId'] || null) === (procesoId || null);

    if (convEqual && carEqual && procEqual) {
      return;
    }

    this.router.navigate([], {
      queryParams: {
        convocatoriaId: convocatoriaId || undefined,
        carreraId: carreraId || undefined,
        procesoId: procesoId || undefined,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  /**
   * Returns a snapshot of the current active context IDs.
   */
  obtenerSnapshot(): StoredAutoevalContext {
    return {
      convocatoriaId: this.convocatoriaSeleccionadaId(),
      carreraId: this.carreraSeleccionadaId(),
      procesoId: this.procesoIdActivo(),
    };
  }
}
