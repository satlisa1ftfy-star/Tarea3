import { buscarRequerimientos } from '../services/requerimientoService'

// Catálogo GRTabEstado (bloques "Conteo de requerimientos" / "Mis Requerimientos" del legado).
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

export const ESTADOS_CERRADO = [
  ESTADO.CERRADO_CONFORME,
  ESTADO.CERRADO_RECHAZADO,
  ESTADO.CERRADO_ANULADO,
  ESTADO.CERRADO_NO_AUTORIZADO,
  ESTADO.CERRADO_POR_SISTEMA,
  ESTADO.CERRADO_POR_REVERSION,
]

// Subpestañas del tablero.
export const PESTANAS = [
  { id: 'otros', label: 'Otros requerimientos' },
  { id: 'noClasificados', label: 'No clasificados' },
  { id: 'pendientes', label: 'Mis pendientes' },
  { id: 'mis', label: 'Mis requerimientos' },
]

// Columnas por subpestaña. soloSiHay: se oculta cuando está vacía.
const COLUMNAS = {
  mis: [
    { title: 'Pendientes', tone: 'yellow', icon: 'cpu', estados: [ESTADO.REGISTRADO, ESTADO.POR_ASIGNAR, ESTADO.ASIGNADO, ESTADO.EN_PROCESO, ESTADO.NO_CONFORME] },
    { title: 'Atendidos', tone: 'green', icon: 'check-circle', estados: [ESTADO.ATENDIDO] },
    { title: 'Cerrados', tone: 'slate', icon: 'check', estados: ESTADOS_CERRADO, soloSiHay: true },
  ],
  pendientes: [
    { title: 'Por asignar', tone: 'amber', icon: 'send', estados: [ESTADO.REGISTRADO, ESTADO.POR_ASIGNAR] },
    { title: 'Asignados', tone: 'orange', icon: 'user', estados: [ESTADO.ASIGNADO] },
    { title: 'En proceso', tone: 'blue', icon: 'cpu', estados: [ESTADO.EN_PROCESO] },
    { title: 'No conforme', tone: 'rose', icon: 'x', estados: [ESTADO.NO_CONFORME], soloSiHay: true },
    { title: 'Atendidos', tone: 'green', icon: 'check-circle', estados: [ESTADO.ATENDIDO], soloSiHay: true },
    // Sin "Cerrados": lo cerrado ya no es un pendiente (ver sinCerrados).
  ],
  noClasificados: [
    { title: 'Por clasificar', tone: 'purple', icon: 'file-text', estados: null },
  ],
  otros: [
    { title: 'Encontrados', tone: 'blue', icon: 'search', estados: null },
  ],
}

function formatearFecha(fechaISO) {
  if (!fechaISO) return '-'
  const fecha = new Date(fechaISO)
  if (Number.isNaN(fecha.getTime())) return String(fechaISO).slice(0, 10)
  return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

// dd/mm/aaaa hh:mm:ss (detalle del requerimiento).
export function formatearFechaHora(fechaISO) {
  if (!fechaISO) return '-'
  const fecha = new Date(fechaISO)
  if (Number.isNaN(fecha.getTime())) return String(fechaISO)
  const hora = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
  return `${formatearFecha(fechaISO)} ${hora}`
}

// La descripción del legado viene en HTML (copiado de Word/Outlook): se muestra como texto limpio,
// como en el sistema antiguo. Se usa DOMParser (no v-html) para no ejecutar nada del contenido.
export function htmlATexto(html) {
  const fuente = String(html || '')
  if (!fuente.trim()) return ''
  if (typeof DOMParser === 'undefined' || !/[<&]/.test(fuente)) return fuente.trim()
  const doc = new DOMParser().parseFromString(fuente, 'text/html')
  doc.querySelectorAll('br').forEach((br) => br.replaceWith('\n'))
  doc.querySelectorAll('li').forEach((li) => li.prepend('• '))
  doc.querySelectorAll('p, div, li, tr, h1, h2, h3, h4, h5, h6').forEach((el) => el.append('\n'))
  return (doc.body.textContent || '')
    .replace(/\u00a0/g, ' ')          // &nbsp;
    .replace(/[ \t]+/g, ' ')          // espacios repetidos de Word
    .split('\n').map((l) => l.trim()).join('\n')
    .replace(/\n{3,}/g, '\n\n')       // como máximo una línea en blanco
    .trim()
}

// Mis pendientes no incluye requerimientos cerrados.
export function sinCerrados(requerimientos = []) {
  return requerimientos.filter((r) => !ESTADOS_CERRADO.includes(r.codigoEstado))
}

function iniciales(nombre) {
  const p = String(nombre || '').trim().split(/\s+/).filter(Boolean)
  if (!p.length) return ''
  return ((p[0][0] || '') + (p[1]?.[0] || '')).toUpperCase()
}

// Tarjeta del tablero a partir de una fila de /api/requerimiento/buscar.
export function aTarjeta(req) {
  const texto = (v) => String(v || '').trim()
  return {
    codigo: req.codigoRequerimiento,
    numero: String(req.codigoRequerimiento), 
    area: texto(req.unidadOrganicaSolicitante) || texto(req.unidadOrganicaRequerimiento) || '-',
    categoria: texto(req.categoria),
    subcategoria: texto(req.subcategoria),
    descripcion: htmlATexto(req.descripcion),
    fecha: formatearFecha(req.fechaRegistro),
    fechaHora: formatearFechaHora(req.fechaRegistro),
    prioridad: texto(req.prioridad) || 'Normal',
    codigoEstado: req.codigoEstado,
    estado: texto(req.estado),
    solicitante: texto(req.solicitante),
    cargoSolicitante: texto(req.cargoSolicitante),
    inicialesSolicitante: iniciales(req.solicitante),
    responsable: texto(req.responsable),
    cargoResponsable: texto(req.cargoResponsable),
    unidadOrganicaResponsable: texto(req.unidadOrganicaResponsable),
    inicialesResponsable: iniciales(req.responsable),
    raw: req,
  }
}

// Métricas + columnas de una subpestaña a partir de su lista de requerimientos.
export function construirTablero(pestana, requerimientos = []) {
  const columns = (COLUMNAS[pestana] || [])
    .map((col) => {
      const reqs = col.estados ? requerimientos.filter((r) => col.estados.includes(r.codigoEstado)) : requerimientos
      return { title: col.title, tone: col.tone, icon: col.icon, soloSiHay: !!col.soloSiHay, total: reqs.length, cards: reqs.map(aTarjeta) }
    })
    .filter((c) => !c.soloSiHay || c.total > 0)

  const metrics = columns.map((c) => ({ title: c.title, total: c.total, tone: c.tone, icon: c.icon }))
  // Vista lista (tabla del Figma): todas las filas con su grupo (columna).
  const filas = columns.flatMap((c) => c.cards.map((card) => ({ ...card, grupo: c.title, tonoGrupo: c.tone })))
  return { metrics, columns, filas }
}

/**
 * No clasificados: Registrados vigentes cuya ubicación actual es la unidad del rol activo
 * (última transferencia o UO de registro). Aunque aún no tengan movimientos ni responsable.
 * Sin UO conocida no se consulta: sin acotar recorre toda la tabla (timeout 15s).
 */
export function cargarNoClasificados(codigoUoActiva = 0) {
  if (!codigoUoActiva) return Promise.resolve([])
  return buscarRequerimientos({ codigoEstado: ESTADO.REGISTRADO, codigoUoRequerimiento: codigoUoActiva, vigencia: 1 })
    // Solo los de MI unidad (destino/ubicación actual): no se clasifica ni asigna lo de otras unidades.
    // Respaldo por si el backend aún no aplica el filtro (devolvería todas las unidades).
    .then((lista) => (lista || []).filter((r) => Number(r.codigoUoRequerimiento) === Number(codigoUoActiva)))
}

/**
 * Carga las subpestañas propias del usuario (sin filtro de fecha):
 * - mis: registrados con su DNI (el SP expande todos sus iCodigo_Per históricos).
 * - pendientes: donde es responsable vigente (le asignaron), sin cerrados.
 * - noClasificados: Registrados (sin clasificar ni asignar) derivados a su unidad.
 */
export async function cargarPestanas(perfil = null, codigoUoActiva = 0) {
  const codigoPersonaGr = perfil?.codigoPersonaGr
  if (!codigoPersonaGr) {
    return { mis: [], pendientes: [], noClasificados: [], online: false, errorMessage: 'No hay usuario logueado.' }
  }

  const consultas = [
    buscarRequerimientos({ codigoPersonaSolicitante: codigoPersonaGr, vigencia: 1 }),
    buscarRequerimientos({ codigoPersonaResponsable: codigoPersonaGr, vigencia: 1 }),
    cargarNoClasificados(codigoUoActiva),
  ]

  const resultados = await Promise.allSettled(consultas)
  const valor = (r) => (r.status === 'fulfilled' ? r.value || [] : [])
  const errores = resultados.filter((r) => r.status === 'rejected')
  errores.forEach((e) => console.error('No se pudo cargar una subpestaña.', e.reason))

  return {
    mis: valor(resultados[0]),
    pendientes: sinCerrados(valor(resultados[1])),
    noClasificados: valor(resultados[2]),
    online: errores.length === 0,
    errorMessage: errores[0]?.reason?.message || '',
  }
}

/**
 * Búsqueda contra el backend (solo los filtros que acepta /buscar).
 * Con número no se acota por fecha: un número puede ser de cualquier fecha.
 */
export async function buscarAvanzado({ numero = 0, codigoEstado = 0, fechaInicio = 'TODO', fechaFin = 'TODO' } = {}) {
  const filtros = { numero, codigoEstado, vigencia: 3 }
  if (!numero) {
    filtros.fechaInicio = fechaInicio || 'TODO'
    filtros.fechaFin = fechaFin || 'TODO'
  }
  try {
    const requerimientos = await buscarRequerimientos(filtros)
    return { requerimientos: requerimientos || [], online: true }
  } catch (error) {
    console.error('No se pudo ejecutar la búsqueda avanzada.', error)
    return { requerimientos: [], online: false, errorMessage: error?.message || 'Error desconocido' }
  }
}

/**
 * Con búsqueda,
 * Con N° de requerimiento no se acota por fecha (un número puede ser de cualquier fecha).
 */
export function cumpleFiltros(r, f) {
  const contiene = (valor, texto) => String(valor || '').toLowerCase().includes(texto.toLowerCase())
  if (f.numero && r.codigoRequerimiento !== f.numero) return false
  if (f.estado && r.codigoEstado !== f.estado) return false
  if (!f.numero) {
    const fecha = String(r.fechaRegistro || '').slice(0, 10) // yyyy-MM-dd
    if (f.fechaInicio && fecha < f.fechaInicio) return false
    if (f.fechaFin && fecha > f.fechaFin) return false
  }
  if (f.solicitante && !contiene(r.solicitante, f.solicitante)) return false
  if (f.responsable && !contiene(r.responsable, f.responsable)) return false
  if (f.prioridad && String(r.prioridad || '').trim().toLowerCase() !== f.prioridad.toLowerCase()) return false
  if (f.categoria && r.codigoCategoria !== f.categoria) return false
  if (f.subcategoria && r.codigoSubcategoria !== f.subcategoria) return false
  return true
}

/**
 * Con búsqueda, cada pestaña muestra SOLO lo suyo que cumple los filtros
 * (subconjunto de su total sin filtros: si Mis pendientes tiene 22 y 10 caen
 * en el rango, muestra 10). Otros = lo encontrado que no está en ninguna de las tres.
 */
export function repartirResultados(datos, resultadosBusqueda, filtros) {
  const mis = datos.mis.filter((r) => cumpleFiltros(r, filtros))
  const pendientes = datos.pendientes.filter((r) => cumpleFiltros(r, filtros))
  const noClasificados = datos.noClasificados.filter((r) => cumpleFiltros(r, filtros))
  const propios = new Set([...datos.mis, ...datos.pendientes, ...datos.noClasificados].map((r) => r.codigoRequerimiento))
  const otros = resultadosBusqueda.filter((r) => !propios.has(r.codigoRequerimiento) && cumpleFiltros(r, filtros))
  return { mis, pendientes, noClasificados, otros }
}
