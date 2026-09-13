import { http } from './http'

/**
 * Busca unidades orgánicas por nombre (o parte del nombre).
 */
export function buscarUnidadOrganica(nombre) {
  return http.get(`/api/requerimiento/buscarUnidadOrganica/${encodeURIComponent(nombre)}`)
}
