import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos.
 * Ver spGR_Requerimiento_Consultar / RequerimientoController#buscar en el backend.
 *
 * Valores que el backend interpreta como "sin filtro":
 * numero=0, titulo='TODO', fechaInicio='TODO', fechaFin='TODO',
 * codigoPersonaSolicitante=0, codigoUoSolicitante=0,
 * codigoPersonaResponsable=0, codigoUoResponsable=0, codigoEstado=0,
 * vigencia=3 (1=vigente, 2=no vigente, 3=todos)
 */
export function buscarRequerimientos(filtros = {}) {
  return http.get('/api/requerimiento/buscar', filtros)
}
