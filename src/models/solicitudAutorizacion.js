import { consultarSolicitud } from '../services/requerimientoService'

// Campos que devuelve GET /api/requerimiento/consultarSolicitud.
// "codigo" corresponde a codigoPersona del catálogo de personas.
function aSolicitudAutorizacion(solicitud) {
  return {
    codigo: solicitud.codigo,
    nombre: solicitud.nombre,
    estado: solicitud.estado,
  }
}

/**
 * Autorizaciones ya registradas para el requerimiento.
 */
export async function cargarSolicitudesAutorizacion(codigoRequerimiento) {
  const solicitudes = await consultarSolicitud(codigoRequerimiento)
  return (solicitudes || []).map(aSolicitudAutorizacion)
}
