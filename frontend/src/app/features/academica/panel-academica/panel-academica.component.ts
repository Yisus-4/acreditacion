import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-panel-academica',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './panel-academica.component.html',
  styleUrl: '../../estructura/panel-administracion/panel-administracion.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelAcademicaComponent {}
