import { orgHttp } from './orgHttp'

/**
 * Busca unidades orgánicas por nombre (o parte del nombre).
 */
export function buscarUnidadOrganica(nombre) {
  return orgHttp.get(`/api/organizacion/buscarUnidadOrganica/${encodeURIComponent(nombre)}`)
}
