import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth/auth';
import { NotificacionesCampanaComponent } from './core/notifications/notificaciones-campana/notificaciones-campana.component';

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

  onSignOut(): void {
    this.auth.signOut();
  }
}
