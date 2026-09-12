import { buscarRequerimientos } from '../services/requerimientoService'

// Datos de referencia (mock) usados como respaldo si el backend no responde.
export const mockMetrics = [
  ['Registrados', '128', 'document'], ['Por asignar', '16', 'briefcase'], ['Asignados', '28', 'user'], ['En proceso', '34', 'refresh'],
  ['Atendidos', '32', 'check'], ['Pendientes de conformidad', '10', 'clock'], ['No conformes', '4', 'close'], ['Cerrados', '24', 'lock'],
]

export const mockColumns = [
  { title: 'Por asignar', total: 16, tone: 'yellow', cards: [['REQ-2026-00117', 'Revisar integracion con el SIAF', 'Gerencia de Informatica', '30/08/2026', 'Alta'], ['REQ-2026-00123', 'Soporte para envio masivo de correos', 'Comunicacion', '30/08/2026', 'Media'], ['REQ-2026-00128', 'Actualizacion de instructivo', 'Oficina de Planeamiento', '30/08/2026', 'Baja']] },
  { title: 'Asignado', total: 28, tone: 'purple', cards: [['REQ-2026-00110', 'Validar configuracion de alertas', 'Gerencia de Fiscalizacion', '29/08/2026', 'Alta'], ['REQ-2026-00122', 'Actualizacion de datos de usuarios', 'Gerencia de Informatica', '29/08/2026', 'Baja'], ['REQ-2026-00119', 'Correo no recibido por contribuyente', 'Comunicaciones', '27/08/2026', 'Baja']] },
  { title: 'En proceso', total: 34, tone: 'blue', cards: [['REQ-2026-00125', 'Validacion de reglas de negocio', 'Gerencia de Fiscalizacion', '30/08/2026', 'Alta'], ['REQ-2026-00118', 'Mejora en tiempos de carga', 'Gerencia de Informatica', '26/08/2026', 'Media']] },
  { title: 'Atendido', total: 32, tone: 'green', cards: [['REQ-2026-00112', 'Generacion de reporte mensual', 'Gerencia de Planificacion', '25/08/2026', 'Baja'], ['REQ-2026-00109', 'Permisos para nuevo colaborador', 'Recursos Humanos', '23/08/2026', 'Media'], ['REQ-2026-00107', 'Ajuste en formato de impresion', 'Gerencia de Informatica', '21/08/2026', 'Baja']] },
]

const TONES = ['yellow', 'purple', 'blue', 'green', 'orange', 'red']

function formatearFecha(fechaISO) {
  if (!fechaISO) return '-'
  const fecha = new Date(fechaISO)
  if (Number.isNaN(fecha.getTime())) return String(fechaISO).slice(0, 10)
  return fecha.toLocaleDateString('es-PE')
}

function aTarjeta(req) {
  return [
    `REQ-${req.codigoRequerimiento}`,
    req.titulo || '(sin título)',
    req.unidadOrganicaSolicitante || req.unidadOrganicaRequerimiento || '-',
    formatearFecha(req.fechaRegistro),
    req.prioridad || 'Media',
  ]
}

/**
 * Transforma la lista plana de requerimientos del backend en las
 * columnas (agrupadas por estado) y métricas que consume el tablero.
 */
export function construirTablero(requerimientos) {
  const porEstado = new Map()

  for (const req of requerimientos) {
    const estado = req.estado || 'Sin estado'
    if (!porEstado.has(estado)) porEstado.set(estado, [])
    porEstado.get(estado).push(req)
  }

  const columns = Array.from(porEstado.entries()).map(([title, reqs], i) => ({
    title,
    total: reqs.length,
    tone: TONES[i % TONES.length],
    cards: reqs.map(aTarjeta),
  }))

  const metrics = [
    ['Registrados', String(requerimientos.length), 'document'],
    ...columns.map((c) => [c.title, String(c.total), 'briefcase']),
  ]

  return { metrics, columns }
}

function combinarSinDuplicados(listas) {
  const porCodigo = new Map()
  for (const lista of listas) {
    for (const req of lista) {
      porCodigo.set(req.codigoRequerimiento, req)
    }
  }
  return Array.from(porCodigo.values())
}

/**
 * Carga "mis requerimientos": los que el usuario logueado solicitó y los
 * que tiene asignados como responsable, combinados sin duplicar.
 *
 * `perfil` es el objeto devuelto por el login (service-user-auth), con
 * `codigoPersonaGr` = iCodigo_Per en Gestión de Requerimientos.
 * Si no hay perfil (o no tiene código), se listan todos los requerimientos.
 */
export async function cargarTablero(perfil = null) {
  const codigoPersonaGr = perfil?.codigoPersonaGr

  try {
    let requerimientos

    if (codigoPersonaGr) {
      // vigencia: 1 = solo requerimientos vigentes (no cerrados/anulados).
      // El tablero es "mis pendientes", no un historial completo, y esto
      // evita traer años de requerimientos ya cerrados en cada carga.
      const [comoSolicitante, comoResponsable] = await Promise.all([
        buscarRequerimientos({ codigoPersonaSolicitante: codigoPersonaGr, vigencia: 1 }),
        buscarRequerimientos({ codigoPersonaResponsable: codigoPersonaGr, vigencia: 1 }),
      ])
      requerimientos = combinarSinDuplicados([comoSolicitante, comoResponsable])
    } else {
      requerimientos = await buscarRequerimientos({ vigencia: 1 })
    }

    return { ...construirTablero(requerimientos), online: true }
  } catch (error) {
    // Se deja en consola el detalle real (CORS, 500 del backend, red caída, etc.)
    // porque el banner de la vista solo puede mostrar un resumen.
    console.error('No se pudo conectar con service-requerimiento, usando datos de referencia.', error)
    return {
      metrics: mockMetrics,
      columns: mockColumns,
      online: false,
      error,
      errorMessage: error?.message || 'Error desconocido',
    }
  }
}
