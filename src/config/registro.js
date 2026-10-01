/**
 * Parámetros del formulario "Registro de Requerimiento".
 */
export const REGISTRO = {
  maxTitulo: 200,
  maxDescripcion: 2000,

  // El SP guarda un único adjunto inicial (vDocumentoAdjunto_Req).
  maxAdjuntos: 1,
  maxBytesAdjunto: 10 * 1024 * 1024,

  // iCodUO de las gerencias donde NO aplican los correos en copia (p. ej. Asuntos Legales).
  // PENDIENTE: completar con el iCodUO real de la Gerencia de Asuntos Legales.
  uoSinCopias: [],

  minCaracteresBusquedaPersona: 3,
  maxResultadosPersonas: 8,
}
