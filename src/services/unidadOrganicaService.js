import { httpOrg } from './http'

/**
 * Busca unidades orgánicas por nombre (o parte del nombre).
 */
export function buscarUnidadOrganica(nombre) {
  return httpOrg.get(`/api/organizacion/buscarUnidadOrganica/${encodeURIComponent(nombre)}`)
}
