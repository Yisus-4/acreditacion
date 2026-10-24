/**
 * Modelos mínimos de sesión/usuario para el módulo de autenticación.
 *
 * Estos modelos describen la **forma** de la sesión futura sin acoplarse a JWT
 * ni a endpoints reales (el backend aún no expone /auth/login). El `token` es
 * un placeholder opaco que el mock rellena; cuando exista JWT real, este mismo
 * contrato se conserva y el servicio deja de generar el token localmente.
 */

/** Códigos de rol dinámicos (strings). Sin enum rígido: el backend define los códigos. */
export type RoleCode = string;

export interface User {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  /** Códigos de rol como strings dinámicos; nunca un enum cerrado del frontend. */
  readonly roleCodes: readonly RoleCode[];
  readonly unidadAcademicaId?: string | null;
}

export interface Session {
  readonly user: User;
  /**
   * Placeholder opaco. NO es JWT todavía. El mock lo genera localmente; el
   * backend real deberá devolverlo y el frontend solo lo almacenará.
   */
  readonly token: string;
  readonly issuedAt: number;
}

export interface Credentials {
  readonly email: string;
  readonly password: string;
}

/** Estados visuales del formulario de login. */
export type SignInStatus =
  | 'idle'
  | 'validating'
  | 'loading'
  | 'invalid'
  | 'success';
