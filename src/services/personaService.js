import { http } from './http'

// Personas GR  -> service-requerimiento (8080)
export function listarPersonas(filtros = {}) {
  return http.get('/api/requerimiento/buscarNombre', filtros)
}
