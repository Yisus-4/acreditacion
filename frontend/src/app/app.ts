import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth/auth';
import { NotificacionesCampanaComponent } from './core/notifications/notificaciones-campana/notificaciones-campana.component';

const ROL_LABELS: Record<string, string> = {
  AEA: 'Área de Evaluación y Acreditación (Admin)',
  CAREER_DIRECTOR: 'Director de Carrera',
  SCHOOL_DIRECTOR: 'Director de Escuela',
  TEACHER: 'Docente',
  STUDENT: 'Estudiante',
};

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NotificacionesCampanaComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly auth = inject(Auth);
  readonly session = toSignal(this.auth.currentSession$, { initialValue: this.auth.currentSession });
  readonly user = computed(() => this.session()?.user ?? null);

  readonly menuUsuarioAbierto = signal<boolean>(false);

  readonly nombreUsuario = computed(() => {
    const u = this.user();
    return u?.displayName || u?.email?.split('@')[0] || 'admin';
  });

  readonly rolPrincipal = computed(() => {
    const roles = this.user()?.roleCodes ?? [];
    if (roles.length === 0) return 'Usuario';
    return ROL_LABELS[roles[0]] || roles[0];
  });

  toggleMenuUsuario(): void {
    this.menuUsuarioAbierto.update((v) => !v);
  }

  cerrarMenuUsuario(): void {
    this.menuUsuarioAbierto.set(false);
  }

  onSignOut(): void {
    this.cerrarMenuUsuario();
    this.auth.signOut();
  }
}
