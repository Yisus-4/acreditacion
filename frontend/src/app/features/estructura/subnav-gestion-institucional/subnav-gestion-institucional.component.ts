import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-subnav-gestion-institucional',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="gestion-inst-header">
      <p class="breadcrumb-text">Gestión institucional / {{ seccionActual() }}</p>
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
        <a routerLink="/admin/carreras" routerLinkActive="active" class="subnav-tab">
          Carreras
        </a>
        <a routerLink="/admin/convenios" routerLinkActive="active" class="subnav-tab">
          Convenios (RG-007)
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
    .breadcrumb-text {
      margin: 0;
      font-size: 14px;
      color: var(--color-text-muted, #5a4a53);
      font-weight: 400;
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
}
