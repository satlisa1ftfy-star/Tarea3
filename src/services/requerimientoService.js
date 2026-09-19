import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos mediante Backend (service-requerimiento).
 * Maneja valores por defecto para ignorar filtros:
 * - 'TODO' (textos/fechas), 0 (IDs/números) y 3 (todos en vigencia).
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
