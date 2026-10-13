import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos mediante Backend (service-requerimiento).
 * Maneja valores por defecto para ignorar filtros
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

/**
 * Cargos autorizados para un requerimiento según su categoría/subcategoría.
 * GET /api/requerimiento/buscarCargo?TipBus=&codigoReq=&codigoCat=&Codigo_SubCat=
 */
export function buscarCargosAutorizados({ tipBus, codigoReq, codigoCat, codigoSubCat }) {
  return http.get('/api/requerimiento/buscarCargo', {
    TipBus: tipBus,
    codigoReq,
    codigoCat,
    Codigo_SubCat: codigoSubCat,
  })
}

/**
 * Personas a las que ya se solicitó autorización para el requerimiento.
 * GET /api/requerimiento/consultarSolicitud?codigoRequerimiento=
 * Respuesta: [{ codigo, nombre, estado }]
 */
export function consultarSolicitud(codigoRequerimiento) {
  return http.get('/api/requerimiento/consultarSolicitud', { codigoRequerimiento })
}

/**
 * Solicita la autorización de un requerimiento a una o más personas.
 * payload: { codRequerimiento, codPerAutoriza ('1001,1002'), codPerSolAutorizacion,
 *            codPerHelpDesk, codPerActualiza, observacion }
 */
export function solicitarAutorizacion(payload) {
  return http.post('/api/requerimiento/solicitar', payload)
}
