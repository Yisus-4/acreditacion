import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { Auth, InvalidCredentialsError } from '../../../core/auth/auth';
import { SignInStatus } from '../../../core/auth/auth.models';

/** Longitud mínima de la contraseña para el formulario de login. */
const MIN_PASSWORD_LENGTH = 6;

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  /** Estado visual del formulario. */
  readonly status = signal<SignInStatus>('idle');
  /** Mensaje de error para credenciales inválidas o fallos inesperados. */
  readonly errorMessage = signal('');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
  });

  ngOnInit(): void {
    if (this.auth.isAuthenticated) {
      void this.router.navigate(['/dashboard']);
    }
  }

  get email() {
    return this.form.controls.email;
  }

  get password() {
    return this.form.controls.password;
  }

  /** True cuando un control ya fue tocado o se intentó enviar. */
  private showFieldError(invalid: boolean, touched: boolean): boolean {
    return invalid && (touched || this.status() === 'validating');
  }

  /** Expuesto al template para evitar repetir lógica. */
  emailHasError(): boolean {
    return this.showFieldError(this.email.invalid, this.email.touched);
  }

  passwordHasError(): boolean {
    return this.showFieldError(this.password.invalid, this.password.touched);
  }

  submit(): void {
    if (this.form.invalid) {
      this.status.set('validating');
      this.form.markAllAsTouched();
      return;
    }

    this.status.set('loading');
    this.errorMessage.set('');

    const { email, password } = this.form.getRawValue();
    this.auth.signIn({ email, password }).subscribe({
      next: () => {
        this.status.set('success');
        this.router.navigate(['/dashboard']).then((navigated) => {
          if (!navigated) {
            void this.router.navigateByUrl('/dashboard');
          }
        });
      },
      error: (error: unknown) => {
        if (error instanceof InvalidCredentialsError) {
          this.errorMessage.set('El correo o la contraseña no son correctos.');
        } else {
          this.errorMessage.set('Ocurrió un error inesperado. Intentá nuevamente.');
        }
        this.status.set('invalid');
      },
    });
  }
}
