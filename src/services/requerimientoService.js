import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos mediante Backend (service-requerimiento).
 * Maneja valores por defecto para ignorar filtros
 */
export function buscarRequerimientos(filtros = {}) {
  return http.get('/api/requerimiento/buscar', filtros)
}

/**
 * Asigna un requerimiento a una persona responsable.
 * payload: { codigoRequerimiento, codigoPersonaAsigna, codigoPersonaResponsable,
 *            responsablePrincipal, informeTecnico, observacion, codigoPersonaActualizacion }
 */
export function asignarRequerimiento(payload) {
  return http.post('/api/requerimiento/asignar', payload)
}

/**
 * Detalle del requerimiento:
 * medio de envío, asignado por, ubicación, tipo de evaluación, correos copia, etc.
 */
export function consultarDetalleRequerimiento(codigo) {
  return http.get(`/api/requerimiento/detalle/${encodeURIComponent(codigo)}`)
}

/**
 * Historial de movimientos, del más reciente al más antiguo.
 */
export function consultarHistorialRequerimiento(codigo) {
  return http.get(`/api/requerimiento/historial/${encodeURIComponent(codigo)}`)
}

/**
 * Documentos adjuntos. Solo lista: la descarga
 * queda pendiente del servicio de documentos.
 */
export function consultarAdjuntosRequerimiento(codigo) {
  return http.get(`/api/requerimiento/adjuntos/${encodeURIComponent(codigo)}`)
}
