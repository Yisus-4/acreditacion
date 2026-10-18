import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Auth } from '../../core/auth/auth';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  readonly auth = inject(Auth);

  get session() {
    return this.auth.currentSession;
  }

  get user() {
    return this.session?.user;
  }

  get isAdmin(): boolean {
    return this.user?.roleCodes?.includes('ADMINISTRATOR') ?? false;
  }

  get canManageInstitutions(): boolean {
    return (
      this.user?.roleCodes?.some(
        (role) => role === 'ADMINISTRATOR' || role === 'AEA'
      ) ?? false
    );
  }

  onSignOut(): void {
    this.auth.signOut();
  }
}