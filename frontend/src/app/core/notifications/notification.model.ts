export interface DatosPayloadAltaMiembro {
  apellido: string;
  nombreCompleto: string;
  documento: string;
  cuitPasaporte: string;
  correoElectronico: string;
  correoGmail: string;
  cargoFuncion: string;
  esDocente: boolean;
}

export interface SolicitudNotificacion {
  id: string;
  tipo: string;
  titulo: string;
  mensaje: string;
  datosPayload: string;
  procesoId: string | null;
  carreraNombre: string | null;
  solicitadoPor: string | null;
  solicitadoPorNombre: string | null;
  destinatarioRol: string;
  estado: 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';
  leida: boolean;
  resueltoPor: string | null;
  resueltoEn: string | null;
  creadoEn: string;
}

export interface ConteoNotificaciones {
  conteo: number;
}
