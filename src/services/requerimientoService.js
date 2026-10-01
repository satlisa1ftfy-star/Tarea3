import { http } from './http'

/**
 * Búsqueda avanzada de requerimientos mediante Backend (service-requerimiento).
 * Maneja valores por defecto para ignorar filtros:
 * - 'TODO' (textos/fechas), 0 (IDs/números) y 3 (todos en vigencia).
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
