import { http } from './http'

/**
 * Categorías --> service-requerimiento. TODO / 0 = todas.
 */
export function buscarCategorias(nombre = 'TODO', codigoUo = 0) {
  return http.get('/api/requerimiento/buscarCategoria', { nombre, codigoUo })
}

/**
 * Subcategorías de una categoría. TODO = todas.
 */
export function buscarSubcategorias(codigoCategoria, nombre = 'TODO') {
  return http.get(`/api/requerimiento/buscarSubCategoria/${encodeURIComponent(codigoCategoria)}/${encodeURIComponent(nombre)}`)
}
