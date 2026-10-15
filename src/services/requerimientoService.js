import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos mediante Backend (service-requerimiento).
 * Maneja valores por defecto para ignorar filtros
 */
export function buscarRequerimientos(filtros = {}) {
  return http.get('/api/requerimiento/buscar', filtros)
}

/**
 * Números de requerimiento que el usuario clasificó (GET /api/requerimiento/clasificados), tomados del
 * historial en la base. Devuelve un arreglo de enteros, del más reciente al más antiguo.
 */
export function listarClasificadosPorUsuario(codigoPersonaOrganizacion, { dias = 90, maximo = 30 } = {}) {
  return http.get('/api/requerimiento/clasificados', { codigoPersonaOrganizacion, dias, maximo })
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
 * Registra un requerimiento (POST /api/requerimiento/registrar).
 * payload: { unidadOrganicaId, divisionId, activoId, categoriaId, subCategoriaId, sumilla, descripcionHtml,
 *            codigoPersonaSolicitante, codigoPersonaActualizacion, datosComplementarios, personasCopiaCodigos,
 *            archivosAdjuntos }
 * Responde { requerimientoId, numeroRequerimiento, fechaRegistro }.
 */
export function registrarRequerimiento(payload) {
  return http.post('/api/requerimiento/registrar', payload)
}

/**
 * Sube un archivo a la carpeta temporal; devuelve { archivoTemporalId, nombreOriginal, tamano }.
 * El archivo se confirma recién al registrar el requerimiento.
 */
export function subirAdjuntoTemporal(archivo) {
  const formData = new FormData()
  formData.append('file', archivo)
  return http.postForm('/api/requerimiento/documentos/temporal', formData)
}

/**
 * Detalle del requerimiento para clasificar (incluye categoría, prioridad, datos complementarios y adjuntos).
 */
export function obtenerParaClasificar(codigoRequerimiento) {
  return http.get(`/api/requerimiento/${codigoRequerimiento}/para-clasificar`)
}

/**
 * Clasifica un requerimiento (PUT /api/requerimiento/{id}/clasificacion).
 * payload: { categoriaId, subCategoriaId, prioridadId, observacion, codigoPersonaActualizacion }
 */
export function clasificarRequerimiento(codigoRequerimiento, payload) {
  return http.put(`/api/requerimiento/${codigoRequerimiento}/clasificacion`, payload)
}

/**
 * Detalle del requerimiento:
 * medio de envío, asignado por, ubicación, tipo de evaluación, correos copia, etc.
 */
export function consultarDetalleRequerimiento(codigo) {
  return http.get(`/api/requerimiento/detalle/${encodeURIComponent(codigo)}`)
}

/**
 * Historial de movimientos, del más reciente al más antiguo.
 */
export function consultarHistorialRequerimiento(codigo) {
  return http.get(`/api/requerimiento/historial/${encodeURIComponent(codigo)}`)
}

/**
 * Documentos adjuntos. Solo lista: la descarga
 * queda pendiente del servicio de documentos.
 */
export function consultarAdjuntosRequerimiento(codigo) {
  return http.get(`/api/requerimiento/adjuntos/${encodeURIComponent(codigo)}`)
}

/**
 * Personas que autorizan (bloque del detalle): fechas, código, nombre y estado.
 */
export function consultarPersonasAutorizan(codigo) {
  return http.get(`/api/requerimiento/autorizaciones/${encodeURIComponent(codigo)}`)
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
