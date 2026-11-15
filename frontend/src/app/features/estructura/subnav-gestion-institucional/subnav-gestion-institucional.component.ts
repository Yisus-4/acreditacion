import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-subnav-gestion-institucional',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="gestion-inst-header">
      <nav class="breadcrumb-nav" aria-label="Ruta de navegación">
        <a
          routerLink="/admin/estructura"
          class="breadcrumb-link"
          title="Ir al Panel de Gestión Institucional y Académica"
        >
          Gestión institucional
        </a>
        <span class="breadcrumb-separator">/</span>
        <a
          [routerLink]="getSeccionRoute()"
          class="breadcrumb-link breadcrumb-current"
          title="Sección actual"
        >
          {{ seccionActual() }}
        </a>
      </nav>
      <nav class="subnav-tabs-bar" aria-label="Navegación Gestión Institucional">
        <a routerLink="/admin/instituciones" routerLinkActive="active" class="subnav-tab">
          Institución
        </a>
        <a routerLink="/admin/unidades-academicas-coneau" routerLinkActive="active" class="subnav-tab">
          Unidades académicas
        </a>
        <a routerLink="/admin/sedes" routerLinkActive="active" class="subnav-tab">
          Sedes y localizaciones
        </a>
        <a routerLink="/admin/unidades-academicas" routerLinkActive="active" class="subnav-tab">
          Escuelas
        </a>
        <a routerLink="/admin/infraestructura" routerLinkActive="active" class="subnav-tab">
          Infraestructura
        </a>
        <a routerLink="/admin/ambitos-practica" routerLinkActive="active" class="subnav-tab">
          Ámbitos de práctica
        </a>
      </nav>
    </header>
  `,
  styles: [`
    .gestion-inst-header {
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
      font-weight: 400;
    }
    .breadcrumb-link {
      color: var(--color-text-muted, #5a4a53);
      text-decoration: none;
      font-weight: 500;
      cursor: pointer;
      transition: color 0.15s ease;

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
      margin: 0;
      padding: 0;
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
export class SubnavGestionInstitucionalComponent {
  readonly seccionActual = input.required<string>();

  getSeccionRoute(): string {
    switch (this.seccionActual()) {
      case 'Institución':
        return '/admin/instituciones';
      case 'Unidades académicas':
        return '/admin/unidades-academicas-coneau';
      case 'Sedes y localizaciones':
        return '/admin/sedes';
      case 'Escuelas':
        return '/admin/unidades-academicas';
      case 'Infraestructura':
        return '/admin/infraestructura';
      case 'Ámbitos de práctica':
        return '/admin/ambitos-practica';
      default:
        return '/admin/instituciones';
    }
  }
}
