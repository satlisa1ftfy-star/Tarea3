import { http } from './http'

export function listarPersonas(filtros = {}) {
  return http.get('/api/personas', filtros)
}
