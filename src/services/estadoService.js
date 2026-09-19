import { http } from './http'

export function buscarEstado(nombre = 'TODO') {
  return http.get(`/api/requerimiento/buscarEstado/${encodeURIComponent(nombre)}`)
}
