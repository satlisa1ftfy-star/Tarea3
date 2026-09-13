import { buscarRequerimientos } from '../services/requerimientoService'

// (bloques "Conteo de requerimientos" / "Mis Requerimientos" del legado).
export const ESTADO = {
  REGISTRADO: 1,
  POR_ASIGNAR: 2,
  ASIGNADO: 3,
  EN_PROCESO: 4,
  ATENDIDO: 5,
  CERRADO_CONFORME: 6,
  CERRADO_RECHAZADO: 7,
  CERRADO_ANULADO: 8,
  NO_CONFORME: 9,
  CERRADO_NO_AUTORIZADO: 10,
  CERRADO_POR_SISTEMA: 11,
  CERRADO_POR_REVERSION: 14,
}

// Todo lo "Cerrado" se agrupa en una sola columna; No conforme (9) va aparte.
const ESTADOS_CERRADO = [
  ESTADO.CERRADO_CONFORME,
  ESTADO.CERRADO_RECHAZADO,
  ESTADO.CERRADO_ANULADO,
  ESTADO.CERRADO_NO_AUTORIZADO,
  ESTADO.CERRADO_POR_SISTEMA,
  ESTADO.CERRADO_POR_REVERSION,
]

// Columnas fijas de "Mis requerimientos" (registrado por el usuario conectado).
const COLUMNAS_MIS_REQUERIMIENTOS = [
  { title: 'Registrados', tone: 'yellow', estados: [ESTADO.REGISTRADO] },
  { title: 'Por asignar', tone: 'amber', estados: [ESTADO.POR_ASIGNAR] },
  { title: 'Asignados', tone: 'purple', estados: [ESTADO.ASIGNADO] },
  { title: 'En proceso', tone: 'blue', estados: [ESTADO.EN_PROCESO] },
  { title: 'Atendido', tone: 'green', estados: [ESTADO.ATENDIDO] },
  { title: 'Cerrado', tone: 'slate', estados: ESTADOS_CERRADO },
  { title: 'No conforme', tone: 'rose', estados: [ESTADO.NO_CONFORME] },
]

// Datos de referencia (mock) usados como respaldo si el backend no responde.
export const mockMetrics = [
  ['Total', '128', 'document'], ['Por asignar', '16', 'briefcase'], ['Asignados', '28', 'user'], ['En proceso', '34', 'refresh'],
  ['Atendidos', '32', 'check'], ['Cerrado', '24', 'lock'], ['No conforme', '4', 'close'],
]

export const mockColumns = [
  { title: 'Por asignar', tone: 'amber', total: 16, cards: [['REQ-2026-00117', 'Revisar integracion con el SIAF', 'Gerencia de Informatica', '30/08/2026', 'Alta'], ['REQ-2026-00123', 'Soporte para envio masivo de correos', 'Comunicacion', '30/08/2026', 'Media'], ['REQ-2026-00128', 'Actualizacion de instructivo', 'Oficina de Planeamiento', '30/08/2026', 'Baja']] },
  { title: 'Asignados', tone: 'purple', total: 28, cards: [['REQ-2026-00110', 'Validar configuracion de alertas', 'Gerencia de Fiscalizacion', '29/08/2026', 'Alta'], ['REQ-2026-00122', 'Actualizacion de datos de usuarios', 'Gerencia de Informatica', '29/08/2026', 'Baja'], ['REQ-2026-00119', 'Correo no recibido por contribuyente', 'Comunicaciones', '27/08/2026', 'Baja']] },
  { title: 'En proceso', tone: 'blue', total: 34, cards: [['REQ-2026-00125', 'Validacion de reglas de negocio', 'Gerencia de Fiscalizacion', '30/08/2026', 'Alta'], ['REQ-2026-00118', 'Mejora en tiempos de carga', 'Gerencia de Informatica', '26/08/2026', 'Media']] },
  { title: 'Atendido', tone: 'green', total: 32, cards: [['REQ-2026-00112', 'Generacion de reporte mensual', 'Gerencia de Planificacion', '25/08/2026', 'Baja'], ['REQ-2026-00109', 'Permisos para nuevo colaborador', 'Recursos Humanos', '23/08/2026', 'Media'], ['REQ-2026-00107', 'Ajuste en formato de impresion', 'Gerencia de Informatica', '21/08/2026', 'Baja']] },
]

// Datos de referencia (mock) para "Mis pendientes".
export const mockMetricsPendientes = [
  ['Por asignar', '4', 'briefcase'], ['Por atender', '9', 'clock'], ['Por autorizar', '2', 'document'], ['Por dar conformidad', '3', 'check'], ['No conforme', '1', 'close'],
]

export const mockColumnsPendientes = [
  { title: 'Por asignar', tone: 'amber', total: 4, cards: [] },
  { title: 'Por atender', tone: 'blue', total: 9, cards: [] },
  { title: 'Por autorizar', tone: 'slate', total: 2, cards: [] },
  { title: 'Por dar conformidad', tone: 'purple', total: 3, cards: [] },
  { title: 'No conforme', tone: 'rose', total: 1, cards: [] },
]

/**
 * Búsqueda avanzada de requerimientos 
 * Administrador y Operador
 */
export async function buscarAvanzado({
  codigoUoResponsable = 0,
  numero = 0,
  codigoEstado = 0,
  fechaInicio = 'TODO',
  fechaFin = 'TODO',
} = {}) {
  try {
    const requerimientos = await buscarRequerimientos({
      codigoUoResponsable,
      numero,
      codigoEstado,
      fechaInicio,
      fechaFin,
      vigencia: 1,
    })
    return { cards: requerimientos.map(aTarjeta), online: true }
  } catch (error) {
    console.error('No se pudo ejecutar la búsqueda avanzada.', error)
    return { cards: [], online: false, errorMessage: error?.message || 'Error desconocido' }
  }
}

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
 * Agrupa requerimientos en las columnas fijas de "Mis requerimientos"
 * mediante códigos de estado, consolidando cierres y separando "No conforme".
 */
export function construirTablero(requerimientos) {
  const columns = COLUMNAS_MIS_REQUERIMIENTOS.map((col) => {
    const reqs = requerimientos.filter((r) => col.estados.includes(r.codigoEstado))
    return { title: col.title, tone: col.tone, total: reqs.length, cards: reqs.map(aTarjeta) }
  })

  const metrics = [
    ['Total', String(requerimientos.length), 'document'],
    ...columns.map((c) => [c.title, String(c.total), 'briefcase']),
  ]

  return { metrics, columns }
}

/**
 * Mis requerimientos: filtra únicamente los registrados por el usuario conectado
 */
export async function cargarTablero(perfil = null) {
  const codigoPersonaGr = perfil?.codigoPersonaGr

  try {
    const requerimientos = codigoPersonaGr
      ? await buscarRequerimientos({ codigoPersonaSolicitante: codigoPersonaGr, vigencia: 1 })
      : await buscarRequerimientos({ vigencia: 1 })

    return { ...construirTablero(requerimientos), online: true }
  } catch (error) {
//  Imprime el error técnico completo en consola (CORS, 500, red) para depuración.
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

/**
 * Mis pendientes: items a resolver por el usuario como responsable actual
 * (por asignar/atender, o no conforme reasignado) o solicitante (por conformar)
 */
export async function cargarPendientes(perfil = null) {
  const codigoPersonaGr = perfil?.codigoPersonaGr

  if (!codigoPersonaGr) {
    return {
      metrics: mockMetricsPendientes,
      columns: mockColumnsPendientes,
      online: false,
      errorMessage: 'No hay usuario logueado.',
    }
  }

  try {
    const [comoResponsable, porConformidad] = await Promise.all([
      buscarRequerimientos({ codigoPersonaResponsable: codigoPersonaGr, vigencia: 1 }),
      buscarRequerimientos({ codigoPersonaSolicitante: codigoPersonaGr, codigoEstado: ESTADO.ATENDIDO, vigencia: 1 }),
    ])

    const porAsignar = comoResponsable.filter((r) => r.codigoEstado === ESTADO.POR_ASIGNAR)
    const porAtender = comoResponsable.filter((r) => [ESTADO.ASIGNADO, ESTADO.EN_PROCESO].includes(r.codigoEstado))
    const noConforme = comoResponsable.filter((r) => r.codigoEstado === ESTADO.NO_CONFORME)

    const columns = [
      { title: 'Por asignar', tone: 'amber', total: porAsignar.length, cards: porAsignar.map(aTarjeta) },
      { title: 'Por atender', tone: 'blue', total: porAtender.length, cards: porAtender.map(aTarjeta) },
      
      // "Por autorizar" todavía no se puede completar con datos reales:
      // solo visual, aun sin empoint  
      { title: 'Por autorizar', tone: 'slate', total: 0, cards: [] },
      { title: 'Por dar conformidad', tone: 'purple', total: porConformidad.length, cards: porConformidad.map(aTarjeta) },
      { title: 'No conforme', tone: 'rose', total: noConforme.length, cards: noConforme.map(aTarjeta) },
    ]

    const metrics = columns.map((c) => [c.title, String(c.total), 'briefcase'])

    return { metrics, columns, online: true }
  } catch (error) {
    console.error('No se pudo conectar con service-requerimiento, usando datos de referencia.', error)
    return {
      metrics: mockMetricsPendientes,
      columns: mockColumnsPendientes,
      online: false,
      error,
      errorMessage: error?.message || 'Error desconocido',
    }
  }
}
