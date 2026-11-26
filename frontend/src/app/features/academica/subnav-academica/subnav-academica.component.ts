import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-subnav-academica',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="academica-header">
      <nav class="breadcrumb-nav" aria-label="Ruta de navegación académica">
        <a routerLink="/dashboard" class="breadcrumb-link" title="Ir al Panel Principal">
          Dashboard
        </a>
        <span class="breadcrumb-separator">/</span>
        <a routerLink="/academica" class="breadcrumb-link" title="Ir a Gestión Académica">
          Académica
        </a>
        <span class="breadcrumb-separator">/</span>
        <a [routerLink]="getSeccionRoute()" class="breadcrumb-link breadcrumb-current">
          {{ seccionActual() }}
        </a>
      </nav>
      <nav class="subnav-tabs-bar" aria-label="Navegación Gestión Académica">
        <a routerLink="/academica/carreras" routerLinkActive="active" class="subnav-tab">
          Carreras
        </a>
        <a routerLink="/academica/estandares" routerLinkActive="active" class="subnav-tab">
          Estándares
        </a>
        <a routerLink="/academica/planes" routerLinkActive="active" class="subnav-tab">
          Planes y Actividades Curriculares
        </a>
        <a routerLink="/academica/departamentos-parametros" routerLinkActive="active" class="subnav-tab">
          Departamentos y Parámetros
        </a>
        <a routerLink="/academica/cuerpo-academico" routerLinkActive="active" class="subnav-tab">
          Cuerpo Académico Troncal
        </a>
      </nav>
    </header>
  `,
  styles: [`
    .academica-header {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 24px;
    }
    .breadcrumb-nav {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      font-size: 14px;
      color: var(--color-text-muted, #5a4a53);
    }
    .breadcrumb-link {
      color: var(--color-text-muted, #5a4a53);
      text-decoration: none;
      font-weight: 500;
      &:hover {
        color: var(--color-primary-dark, #6b4c5b);
        text-decoration: underline;
      }
      &.breadcrumb-current {
        color: var(--color-text, #1f1a1d);
        font-weight: 600;
      }
    }
    .breadcrumb-separator {
      color: #9ca3af;
      font-size: 13px;
      user-select: none;
    }
    .subnav-tabs-bar {
      display: flex;
      gap: 28px;
      align-items: flex-start;
      border-bottom: 1px solid var(--border-color, #e5e7eb);
      width: 100%;
      flex-wrap: wrap;
    }
    .subnav-tab {
      display: flex;
      flex-direction: column;
      padding: 10px 0;
      font-size: 16px;
      color: var(--color-text-muted, #5a4a53);
      text-decoration: none;
      font-weight: 400;
      position: relative;
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
export class SubnavAcademicaComponent {
  readonly seccionActual = input.required<string>();

  getSeccionRoute(): string {
    switch (this.seccionActual()) {
      case 'Carreras':
        return '/academica/carreras';
      case 'Estándares':
        return '/academica/estandares';
      case 'Planes y Actividades Curriculares':
        return '/academica/planes';
      case 'Departamentos y Parámetros':
        return '/academica/departamentos-parametros';
      case 'Cuerpo Académico Troncal':
        return '/academica/cuerpo-academico';
      default:
        return '/academica/planes';
    }
  }
}
