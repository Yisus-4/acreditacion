import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { ConvocatoriaService } from '../convocatorias/convocatoria.service';
import { ConvocatoriaDetalle, ConvocatoriaResumen } from '../convocatorias/models/convocatoria.model';
import { EstructuraService } from '../estructura/estructura.service';
import { CarreraResponse } from '../estructura/estructura.types';

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

  // Computed signals
  readonly carrerasDisponibles = computed<CarreraResponse[]>(() => {
    const detalle = this.convocatoriaDetalle();
    const todas = this.carrerasTodas();
    if (!detalle || !detalle.carrerasPropuestas || detalle.carrerasPropuestas.length === 0) {
      return todas;
    }
    const propuestasIds = new Set(detalle.carrerasPropuestas.map((cp) => cp.carreraId));
    const filtradas = todas.filter((c) => propuestasIds.has(c.id));
    return filtradas.length > 0 ? filtradas : todas;
  });

  readonly procesoIdActivo = computed<string | null>(() => {
    const detalle = this.convocatoriaDetalle();
    const carreraId = this.carreraSeleccionadaId();
    if (!detalle || !carreraId) return null;

    if (detalle.carrerasPropuestas && detalle.carrerasPropuestas.length > 0) {
      const match = detalle.carrerasPropuestas.find((cp) => cp.carreraId === carreraId);
      if (match && match.procesoId) {
        return match.procesoId;
      }
    }
    if (detalle.carreraId === carreraId) {
      return detalle.id;
    }
    return detalle.id || null;
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
        }

        if (syncRouter) {
          this.sincronizarRouterYStorage();
        }
      },
      error: (err) => console.error('Error fetching convocatoria detalle', err),
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
    if (syncRouter) {
      this.sincronizarRouterYStorage();
    }
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
