export interface UserItem {
  readonly id: string;
  readonly username: string;
  readonly email: string;
  readonly active: boolean;
  readonly systemUser: boolean;
  readonly roleCodes: readonly string[];
  readonly unidadAcademicaId?: string | null;
}

export interface RoleItem {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly description: string;
}

export interface CreateUserPayload {
  readonly username: string;
  readonly email: string;
  readonly password: string;
  readonly roleCodes: readonly string[];
  readonly unidadAcademicaId?: string | null;
}

export interface UpdateUserPayload {
  readonly username: string;
  readonly email: string;
  readonly roleCodes: readonly string[];
  readonly newPassword?: string;
  readonly unidadAcademicaId?: string | null;
}
