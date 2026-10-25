import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
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
