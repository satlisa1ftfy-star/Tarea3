import { orgHttp } from './orgHttp'

/**
 * Personal de una unidad orgánica -> service-organizacion (8082).
 * tipo: 1 = solo la unidad, 2 = toda la unidad (incluye dependientes).
 */
export function buscarPersonalxUO(codigoUo, tipo = 1) {
  return orgHttp.get(`/api/organizacion/buscarPersonalxUO/${encodeURIComponent(codigoUo)}`, { tipo })
}
