export interface CarreraVinculada {
  id: string;
  nombre: string;
  sigla?: string;
}

export interface TipoEspacioFisico {
  id: string;
  codigo: string;
  denominacion: string;
  activo: boolean;
}

export interface EspacioFisico {
  id?: string;
  tipoEspacioId?: string;
  tipoEspacioCodigo?: string;
  tipoEspacioDenominacion?: string;
  denominacion: string;
  descripcionInfraestructura?: string;
  cantidad?: number;
  capacidadPersonasSentadas?: number;
  capacidadEstudiantes: number;
  superficieM2?: number;
  equipamientoDisponible?: string;
  estadoConeau?: string;
  activo?: boolean;
}

export interface DocumentacionRespaldo {
  id?: string;
  nombreArchivo: string;
  urlMinio: string;
  rutaRepositorio?: string;
  tipoAnexo: string;
  descripcion?: string;
  subidoPorUsuarioNombre?: string;
  fechaSubida?: string;
}

export interface Inmueble {
  id: string;
  institucionId?: string;
  numeroOrden?: number;
  nombre: string;
  domicilio: string;
  calle?: string;
  numero?: string;
  provinciaId?: number;
  provinciaNombre?: string;
  departamentoId?: number;
  departamentoNombre?: string;
  localidadId?: number;
  localidadNombre?: string;
  anioConstruccion?: number;
  anioInicioInstitucion?: number;
  propiedadInstitucion: boolean;
  condicionTenencia: string;
  situacion?: string;
  vinculaTodasCarreras?: boolean;
  esMaestro?: boolean;
  procesoId?: string;
  superficieCubiertaM2?: number;
  superficieTotalM2?: number;
  observaciones?: string;
  verificado: boolean;
  fechaVerificacion?: string;
  estadoConeau?: string;
  activo: boolean;
  carreras: CarreraVinculada[];
  espacios: EspacioFisico[];
  documentos: DocumentacionRespaldo[];
  cantidadEspacios: number;
  tieneDocumentacionRespaldo: boolean;
}

export interface RegistrarInmuebleRequest {
  numeroOrden?: number;
  nombre: string;
  domicilio: string;
  calle?: string;
  numero?: string;
  provinciaId?: number;
  departamentoId?: number;
  localidadId?: number;
  anioConstruccion?: number;
  anioInicioInstitucion?: number;
  propiedadInstitucion: boolean;
  condicionTenencia: string;
  situacion?: string;
  vinculaTodasCarreras?: boolean;
  esMaestro?: boolean;
  procesoId?: string;
  superficieCubiertaM2?: number;
  superficieTotalM2?: number;
  observaciones?: string;
  estadoConeau?: string;
  carrerasIds: string[];
  espacios: {
    tipoEspacioId?: string;
    denominacion: string;
    descripcionInfraestructura?: string;
    cantidad?: number;
    capacidadPersonasSentadas?: number;
    capacidadEstudiantes?: number;
    superficieM2?: number;
    equipamientoDisponible?: string;
    estadoConeau?: string;
  }[];
  documentos?: {
    nombreArchivo: string;
    urlMinio: string;
    tipoAnexo: string;
    descripcion?: string;
  }[];
}

export interface CrearTipoEspacioRequest {
  codigo?: string;
  denominacion: string;
}

export interface ExtenderPlazoRequest {
  nuevaFechaLimite: string;
  motivo?: string;
}

export interface VerificarInfraestructuraResponse {
  procesoId: string;
  verificado: boolean;
  estadoActividadPlan: string;
  fechaVerificacion: string;
  mensaje: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// GT-003: Higiene, Seguridad y Medio Ambiente Laboral
// ─────────────────────────────────────────────────────────────────────────────

export interface GuiaHigieneDocumento {
  id: string;
  tipoDocumentoClave: string;
  tipoDocumentoNombre: string;
  organismoEmisor?: string;
  numeroNormativa?: string;
  descripcion?: string;
  nombreArchivo?: string;
  urlMinio?: string;
  rutaRepositorio: string;
  obligatorio: boolean;
  cargado: boolean;
  fechaSubida?: string;
}

export interface GuiaHigieneSeguridad {
  id: string;
  procesoId?: string;
  esMaestro: boolean;
  edicion: string;
  fechaVigencia?: string;
  elaboradoPor?: string;
  revisadoPor?: string;
  aprobadoPor?: string;
  instanciaResponsableUa?: string;
  politicaUnidadesApoyo?: string;
  aplicaUnidadesApoyo: boolean;
  verificado: boolean;
  fechaVerificacion?: string;
  fechaSolicitud?: string;
  fechaLimite?: string;
  estado: string;
  observaciones?: string;
  documentos: GuiaHigieneDocumento[];
  cantidadDocumentosCargados: number;
  totalObligatorios: number;
  requisitosCompletos: boolean;
}

export interface ActualizarGuiaHigieneRequest {
  instanciaResponsableUa?: string;
  politicaUnidadesApoyo?: string;
  aplicaUnidadesApoyo?: boolean;
  observaciones?: string;
  documentos?: DocumentoHigieneItemRequest[];
}

export interface DocumentoHigieneItemRequest {
  id?: string;
  tipoDocumentoClave?: string;
  tipoDocumentoNombre?: string;
  organismoEmisor?: string;
  numeroNormativa?: string;
  descripcion?: string;
  nombreArchivo?: string;
  urlMinio?: string;
  obligatorio?: boolean;
}
