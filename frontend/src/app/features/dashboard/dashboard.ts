import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Auth } from '../../core/auth/auth';
import { ConvocatoriaService } from '../../core/convocatorias/convocatoria.service';
import { ConvocatoriaResumen } from '../../core/convocatorias/models/convocatoria.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements OnInit {
  readonly auth = inject(Auth);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  readonly convocatoriasEnCurso = signal<ConvocatoriaResumen[]>([]);
  readonly cargandoConvocatorias = signal<boolean>(true);

  ngOnInit(): void {
    this.convocatoriaService.listarConvocatorias().subscribe({
      next: (list) => {
        const activas = list.filter((c) => c.estado !== 'FINALIZADA' && c.estado !== 'CANCELADA');
        this.convocatoriasEnCurso.set(activas);
        this.cargandoConvocatorias.set(false);
      },
      error: () => this.cargandoConvocatorias.set(false),
    });
  }

  get session() {
    return this.auth.currentSession;
  }

  get user() {
    return this.session?.user;
  }

  get isAdmin(): boolean {
    return this.user?.roleCodes?.includes('AEA') ?? false;
  }

  get canManageInstitutions(): boolean {
    return this.user?.roleCodes?.includes('AEA') ?? false;
  }

  get canAccessDatosCentrales(): boolean {
    const allowed = [
      'AEA',
      'SECRETARIA_ACADEMICA',
      'DIRECTOR_DEPARTAMENTO',
      'CONSULTA_INSTITUCIONAL',
      'DIRECTOR_ESCUELA',
      'DIRECTOR_CARRERA',
      'INFRAESTRUCTURA',
      'BEDELIA',
    ];
    return this.user?.roleCodes?.some((role) => allowed.includes(role)) ?? false;
  }

  onSignOut(): void {
    this.auth.signOut();
  }
}