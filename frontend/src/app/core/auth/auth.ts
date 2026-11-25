import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, Observable, tap, throwError } from 'rxjs';

import { Credentials, Session } from './auth.models';

const SESSION_STORAGE_KEY = 'undec_session';
const DEFAULT_JWT_TTL_MS = 60 * 60 * 1000; // 1 hora

/**
 * `Auth` es el servicio singleton de autenticación.
 *
 * Se conecta al backend Spring Boot mediante el endpoint HTTP `/api/auth/login`.
 * En desarrollo, las peticiones a `/api/*` son redirigidas por proxy.conf.json a http://localhost:8080.
 */
@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session$ = new BehaviorSubject<Session | null>(this.loadSessionFromStorage());

  /** Observable de la sesión actual (null cuando no hay sesión). */
  readonly currentSession$: Observable<Session | null> = this.session$.asObservable();

  get currentSession(): Session | null {
    return this.session$.value;
  }

  get isAuthenticated(): boolean {
    const session = this.session$.value;
    if (!session) return false;
    if (this.isSessionExpired(session)) {
      this.signOut();
      return false;
    }
    return true;
  }

  hasRole(roleCode: string): boolean {
    const user = this.currentSession?.user;
    return !!user?.roleCodes?.includes(roleCode);
  }

  hasAnyRole(...roleCodes: string[]): boolean {
    const user = this.currentSession?.user;
    if (!user || !user.roleCodes) return false;
    return roleCodes.some((r) => user.roleCodes.includes(r));
  }

  /**
   * Autentica credenciales contra el backend.
   * Devuelve un `Observable<Session>` que emite la sesión en éxito o un
   * `InvalidCredentialsError` en 401.
   */
  signIn(credentials: Credentials): Observable<Session> {
    return this.http.post<Session>('/api/auth/login', credentials).pipe(
      tap((session) => {
        this.session$.next(session);
        this.saveSessionToStorage(session);
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          return throwError(() => new InvalidCredentialsError());
        }
        return throwError(() => error);
      }),
    );
  }

  /** Cierra la sesión actual, limpia almacenamiento y navega al login. */
  signOut(): void {
    try {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {
      // Ignorar fallo al acceder a localStorage
    }
    this.session$.next(null);
    void this.router.navigate(['/login']);
  }

  private loadSessionFromStorage(): Session | null {
    try {
      const raw = localStorage.getItem(SESSION_STORAGE_KEY);
      if (raw) {
        const session: Session = JSON.parse(raw);
        if (session && session.user && session.token) {
          if (this.isSessionExpired(session)) {
            localStorage.removeItem(SESSION_STORAGE_KEY);
            return null;
          }
          return session;
        }
      }
    } catch {
      // Ignorar fallo al acceder a localStorage o parsear
    }
    return null;
  }

  private isSessionExpired(session: Session): boolean {
    try {
      const parts = session.token.split('.');
      if (parts.length === 3) {
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(base64)) as { exp?: number };
        if (typeof payload.exp === 'number') {
          return Date.now() >= payload.exp * 1000;
        }
      }
    } catch {
      // Fallback a issuedAt + 1h si el token no es decodificable localmente
    }
    const sessionWithTs = session as Session & { issuedAt?: number };
    if (typeof sessionWithTs.issuedAt === 'number') {
      return Date.now() - sessionWithTs.issuedAt >= DEFAULT_JWT_TTL_MS;
    }
    return false;
  }

  private saveSessionToStorage(session: Session): void {
    try {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Ignorar fallo al guardar en localStorage
    }
  }
}

/** Error de dominio para credenciales inválidas (distinto de un error de red). */
export class InvalidCredentialsError extends Error {
  constructor() {
    super('Credenciales inválidas');
    this.name = 'InvalidCredentialsError';
    Object.setPrototypeOf(this, InvalidCredentialsError.prototype);
  }
}
