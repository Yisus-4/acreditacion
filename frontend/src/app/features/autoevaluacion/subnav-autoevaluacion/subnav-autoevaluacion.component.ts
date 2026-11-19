import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { AutoevaluacionContextService } from '../../../core/autoevaluacion/autoevaluacion-context.service';

@Component({
  selector: 'app-subnav-autoevaluacion',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="autoeval-header">
      <div style="display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; border-bottom: 1px solid #e5e7eb; padding-bottom: 0;">
        <nav class="subnav-tabs-bar" style="border-bottom: none; flex: 1;" aria-label="Navegación Dimensiones Autoevaluación">
        <a
          routerLink="/autoevaluacion/infraestructura"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Infraestructura y biblioteca
        </a>
        <a
          routerLink="/autoevaluacion/ambitos-formacion"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Ámbitos de formación
        </a>
        <a
          routerLink="/autoevaluacion/organizacion-ua"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Organización UA
        </a>
        <a
          routerLink="/autoevaluacion/estructura-curricular"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Estructura curricular
        </a>
        <a
          routerLink="/autoevaluacion/estudiantes-graduados"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Estudiantes y graduados
        </a>
        <a
          routerLink="/autoevaluacion/cuerpo-academico"
          routerLinkActive="active"
          queryParamsHandling="preserve"
          class="subnav-tab"
        >
          Cuerpo académico
        </a>
      </nav>

      @if (esAeaOAdmin() && procesoIdCarrera()) {
        <a
          [routerLink]="['/acreditacion/convocatorias', procesoIdCarrera()]"
          [queryParams]="{ tab: 'PLAN_TRABAJO' }"
          class="btn-pt001"
          style="margin-bottom: 6px; white-space: nowrap;"
          title="Ver Plan de Trabajo y Cronograma PT-001"
        >
          <svg
            class="icon-pt"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          <span>Ver Plan de Trabajo (PT-001)</span>
        </a>
      }
      </div>
    </header>
  `,
  styles: [`
    .autoeval-header {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-bottom: 20px;
    }

    .autoeval-context-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      background: #fdfafb;
      border: 1px solid #ebdbe2;
      border-radius: 8px;
      padding: 12px 18px;
      box-shadow: 0 1px 3px rgba(107, 76, 91, 0.05);
    }

    .context-group {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 20px;
      flex: 1;
      min-width: 280px;
    }

    .context-field {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;
      min-width: 220px;
    }

    .context-label {
      font-size: 11.5px;
      font-weight: 700;
      color: #6b4c5b;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .select-wrapper {
      position: relative;
    }

    .context-select {
      width: 100%;
      height: 38px;
      padding: 6px 12px;
      font-size: 13.5px;
      color: #1f1a1d;
      background-color: #ffffff;
      border: 1px solid #d4c5cb;
      border-radius: 6px;
      outline: none;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
      cursor: pointer;

      &:focus {
        border-color: #826170;
        box-shadow: 0 0 0 3px rgba(130, 97, 112, 0.15);
      }

      &:disabled {
        background-color: #f3f4f6;
        cursor: not-allowed;
      }
    }

    .plan-switcher-field {
      min-width: 180px;
    }

    .plan-switcher-pills {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: center;
      height: 38px;
    }

    .plan-pill-btn {
      padding: 5px 12px;
      font-size: 12px;
      font-weight: 600;
      color: #6b4c5b;
      background-color: #f7eff3;
      border: 1px solid #ebdbe2;
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background-color: #ebdbe2;
        color: #4a2c3a;
      }

      &.active {
        background-color: #6b4c5b;
        color: #ffffff;
        border-color: #6b4c5b;
        box-shadow: 0 1px 3px rgba(107, 76, 91, 0.2);
      }
    }

    .context-actions {
      display: flex;
      align-items: center;
      margin-top: auto;
      margin-bottom: auto;
    }

    .btn-pt001 {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 38px;
      padding: 0 14px;
      font-size: 13px;
      font-weight: 600;
      color: #ffffff;
      background-color: #826170;
      border: 1px solid #6b4c5b;
      border-radius: 6px;
      text-decoration: none;
      white-space: nowrap;
      transition: background-color 0.2s ease, transform 0.1s ease;

      &:hover {
        background-color: #6b4c5b;
        color: #ffffff;
      }

      &:active {
        transform: translateY(1px);
      }

      .icon-pt {
        flex-shrink: 0;
      }
    }

    .subnav-tabs-bar {
      display: flex;
      gap: 28px;
      align-items: flex-start;
      border-bottom: 1px solid var(--border-color, #e5e7eb);
      width: 100%;
      margin: 0;
      padding: 0;
      overflow-x: auto;
    }

    .subnav-tab {
      display: flex;
      flex-direction: column;
      padding: 10px 0;
      font-size: 15px;
      color: var(--color-text-muted, #5a4a53);
      text-decoration: none;
      font-weight: 400;
      position: relative;
      white-space: nowrap;
      transition: color 0.15s ease;

      &:hover {
        color: var(--color-text, #1f1a1d);
      }

      &.active {
        font-weight: 700;
        color: var(--color-primary-dark, #6b4c5b);
        border-bottom: 3px solid var(--color-secondary, #e5b115);
        margin-bottom: -1px;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubnavAutoevaluacionComponent implements OnInit {
  readonly seccionActual = input<string>('');
  readonly contextService = inject(AutoevaluacionContextService);
  private readonly auth = inject(Auth);

  readonly esAeaOAdmin = computed(() => {
    return this.auth.hasAnyRole('AEA');
  });

  readonly procesoIdCarrera = computed(() => {
    return this.contextService.procesoIdActivo() || this.contextService.convocatoriaSeleccionadaId();
  });

  ngOnInit(): void {
    this.contextService.inicializarContexto().subscribe();
  }

  onConvocatoriaChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    if (select && select.value) {
      this.contextService.seleccionarConvocatoria(select.value);
    }
  }

  onCarreraChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    if (select && select.value) {
      this.contextService.seleccionarCarrera(select.value);
    }
  }

  seleccionarPlan(planId: string): void {
    this.contextService.setPlanActivo(planId);
  }
}
