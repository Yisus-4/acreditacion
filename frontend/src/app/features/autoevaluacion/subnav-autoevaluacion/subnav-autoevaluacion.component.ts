import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-subnav-autoevaluacion',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="autoeval-header">
      <nav class="subnav-tabs-bar" aria-label="Navegación Dimensiones Autoevaluación">
        <a routerLink="/autoevaluacion/infraestructura" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Infraestructura y biblioteca
        </a>
        <a routerLink="/autoevaluacion/ambitos-formacion" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Ámbitos de formación
        </a>
        <a routerLink="/autoevaluacion/organizacion-ua" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Organización UA
        </a>
        <a routerLink="/autoevaluacion/estructura-curricular" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Estructura curricular
        </a>
        <a routerLink="/autoevaluacion/estudiantes-graduados" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Estudiantes y graduados
        </a>
        <a routerLink="/autoevaluacion/cuerpo-academico" routerLinkActive="active" queryParamsHandling="preserve" class="subnav-tab">
          Cuerpo académico
        </a>
      </nav>
    </header>
  `,
  styles: [`
    .autoeval-header {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 16px;
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
export class SubnavAutoevaluacionComponent {
  readonly seccionActual = input<string>('');
}
