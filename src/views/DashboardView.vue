<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ESTADO, buscarAvanzado, cargarPendientes, cargarTablero } from '../models/dashboard'
import { listarRoles } from '../services/seguridadService'
import { listarPersonas } from '../services/personaService'
import { buscarUnidadOrganica } from '../services/unidadOrganicaService'
import {
  asignarRequerimiento, consultarAdjuntosRequerimiento, consultarDetalleRequerimiento, consultarHistorialRequerimiento,
} from '../services/requerimientoService'
import { buscarCategorias, buscarSubcategorias } from '../services/categoriaService'
import Icon from '../components/Icon.vue'
import Paginador from '../components/Paginador.vue'
import SolicitarAutorizacionModal from '../components/requerimiento/SolicitarAutorizacionModal.vue'

const props = defineProps({
  perfil: { type: Object, default: null },
})
const emit = defineEmits(['logout'])

// vista: 'requerimientos' (tablero con subpestañas) | 'mantenimiento' | 'documentacion'
const vista = ref('requerimientos')
// Subpestaña activa del tablero (Figma): otros | noClasificados | pendientes | mis
const pestana = ref('mis')
const selectedRequest = ref(null)
const selectedColumn = ref(null)
const vistaDetalle = ref(false) // true = vista completa "Detalle de requerimiento" (Figma)

// Datos propios del usuario por subpestaña (sin filtros).
const datos = ref({ mis: [], pendientes: [], noClasificados: [] })
const cargando = ref(true)
const conectado = ref(false)
const mensajeError = ref('')

// Tarjetas de métricas: al seleccionar una, el tablero solo muestra esa columna.
const metricaSeleccionada = ref(null)
function alternarMetrica(titulo) {
  metricaSeleccionada.value = metricaSeleccionada.value === titulo ? null : titulo
}

// Vista del tablero: 'grid' o 'list'.
const vistaTarjetas = ref('grid')
function cambiarVistaTarjetas(valor) {
  vistaTarjetas.value = valor
}

// Colapsar/expandir el menú lateral (botón hamburguesa del header).
const sidebarColapsado = ref(false)
function alternarSidebar() {
  sidebarColapsado.value = !sidebarColapsado.value
}
const mantenimientoAbierto = ref(true) // submenú "Mantenimiento" del sidebar

// Menú de perfil (roles del usuario + cerrar sesión)
const menuAbierto = ref(false)
const roles = ref([])
const perfilActivo = ref(null) // rol seleccionado en el desplegable

// -------------------------------------------------------------------------
// Búsqueda (buscador por número + panel "Búsqueda Avanzada")

// Fecha local yyyy-MM-dd (toISOString usaría UTC: después de las 19:00 en Lima daría mañana).
function hoyISO() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

// Solo dígitos: acepta "287273" (o "REQ-287273" si lo pegan así).
function soloNumero(texto) {
  return Number(String(texto || '').replace(/\D/g, '')) || 0
}

// Borrador (lo que está escrito en el panel / buscador). La fecha por defecto es solo hoy.
const numeroBusqueda = ref('')
const solicitanteFiltro = ref('')
const responsableFiltro = ref('')
const rolFiltro = ref('')
const prioridadFiltro = ref('')
const estadoFiltro = ref('')
const subestadoFiltro = ref('')
const categoriaFiltro = ref('')
const subcategoriaFiltro = ref('')
const fechaInicioBusqueda = ref(hoyISO())
const fechaFinBusqueda = ref(hoyISO())

// Filtros aplicados (copia del borrador al presionar "Aplicar filtros" o Enter). null = sin búsqueda.
const filtrosAplicados = ref(null)
const resultadosBusqueda = ref([]) // filas devueltas por /buscar para los filtros aplicados
const buscandoAvanzado = ref(false)
const errorBusquedaAvanzada = ref('')
const mostrarPanelBusqueda = ref(false)

const OPCIONES_ESTADO_BUSQUEDA = [
  { value: ESTADO.REGISTRADO, label: 'Registrados' },
  { value: ESTADO.POR_ASIGNAR, label: 'Por asignar' },
  { value: ESTADO.ASIGNADO, label: 'Asignados' },
  { value: ESTADO.EN_PROCESO, label: 'En proceso' },
  { value: ESTADO.ATENDIDO, label: 'Atendidos' },
  { value: ESTADO.NO_CONFORME, label: 'No conforme' },
  { value: ESTADO.CERRADO_CONFORME, label: 'Cerrado conforme' },
  { value: ESTADO.CERRADO_RECHAZADO, label: 'Cerrado rechazado' },
  { value: ESTADO.CERRADO_ANULADO, label: 'Cerrado anulado' },
  { value: ESTADO.CERRADO_NO_AUTORIZADO, label: 'Cerrado no autorizado' },
  { value: ESTADO.CERRADO_POR_SISTEMA, label: 'Cerrado por el sistema' },
  { value: ESTADO.CERRADO_POR_REVERSION, label: 'Cerrado por reversión' },
]

// Catálogos del panel (se cargan al abrirlo por primera vez).
const categorias = ref([])
const subcategorias = ref([])
async function cargarCategorias() {
  if (categorias.value.length) return
  try {
    categorias.value = await buscarCategorias()
  } catch (error) {
    console.error('No se pudieron cargar las categorías.', error)
  }
}
watch(categoriaFiltro, async (codigo) => {
  subcategoriaFiltro.value = ''
  subcategorias.value = []
  if (!codigo) return
  try {
    subcategorias.value = await buscarSubcategorias(codigo)
  } catch (error) {
    console.error('No se pudieron cargar las subcategorías.', error)
  }
})

// Prioridades: las que vienen en los datos (no hay catálogo en el backend).
const opcionesPrioridad = computed(() => {
  const todas = [...datos.value.mis, ...datos.value.pendientes, ...datos.value.noClasificados, ...resultadosBusqueda.value]
  const valores = [...new Set(todas.map((r) => (r.prioridad || '').trim()).filter(Boolean))]
  return valores.length ? valores.sort() : ['Normal', 'Alta', 'Urgente']
})

function abrirPanelBusqueda() {
  cargarCategorias()
  mostrarPanelBusqueda.value = true
}
function cerrarPanelBusqueda() {
  mostrarPanelBusqueda.value = false
}

function restablecerBorrador() {
  numeroBusqueda.value = ''
  solicitanteFiltro.value = ''
  responsableFiltro.value = ''
  rolFiltro.value = ''
  prioridadFiltro.value = ''
  estadoFiltro.value = ''
  subestadoFiltro.value = ''
  categoriaFiltro.value = ''
  subcategoriaFiltro.value = ''
  fechaInicioBusqueda.value = hoyISO()
  fechaFinBusqueda.value = hoyISO()
}

// "Limpiar todo": vuelve a los valores por defecto (fecha = hoy) y quita la búsqueda activa.
function limpiarFiltrosAvanzados() {
  restablecerBorrador()
  filtrosAplicados.value = null
  resultadosBusqueda.value = []
  errorBusquedaAvanzada.value = ''
}

function capturarBorrador() {
  return {
    numero: soloNumero(numeroBusqueda.value),
    solicitante: solicitanteFiltro.value.trim(),
    responsable: responsableFiltro.value.trim(),
    prioridad: prioridadFiltro.value,
    estado: estadoFiltro.value ? Number(estadoFiltro.value) : 0,
    categoria: categoriaFiltro.value ? Number(categoriaFiltro.value) : 0,
    subcategoria: subcategoriaFiltro.value ? Number(subcategoriaFiltro.value) : 0,
    fechaInicio: fechaInicioBusqueda.value || '',
    fechaFin: fechaFinBusqueda.value || '',
  }
}

// Consulta al backend con los filtros que acepta /buscar (número, estado, fechas).
async function ejecutarBusqueda() {
  const f = filtrosAplicados.value
  if (!f) return
  buscandoAvanzado.value = true
  errorBusquedaAvanzada.value = ''
  const resultado = await buscarAvanzado({
    numero: f.numero,
    codigoEstado: f.estado,
    fechaInicio: f.fechaInicio || 'TODO',
    fechaFin: f.fechaFin || 'TODO',
  })
  resultadosBusqueda.value = resultado.requerimientos
  errorBusquedaAvanzada.value = resultado.online ? '' : (resultado.errorMessage || 'No se pudo buscar.')
  buscandoAvanzado.value = false
  irAPestanaConResultados()
}

async function aplicarFiltrosAvanzados() {
  filtrosAplicados.value = capturarBorrador()
  mostrarPanelBusqueda.value = false
  metricaSeleccionada.value = null
  await ejecutarBusqueda()
}

// Buscador junto a "Nuevo requerimiento": solo número de requerimiento.
async function buscarPorNumero() {
  const numero = soloNumero(numeroBusqueda.value)
  if (!numero) {
    if (filtrosAplicados.value?.numero) quitarFiltroActivo('numero')
    return
  }
  await aplicarFiltrosAvanzados()
}

// Si la subpestaña actual quedó sin resultados, pasa a la primera que sí tenga.
function irAPestanaConResultados() {
  if (conteos.value[pestana.value] > 0) return
  const conResultados = ['mis', 'pendientes', 'noClasificados', 'otros'].find((id) => conteos.value[id] > 0)
  if (conResultados) pestana.value = conResultados
}

// Chips de filtros aplicados (con su "x").
const filtrosActivos = computed(() => {
  const f = filtrosAplicados.value
  if (!f) return []
  const activos = []
  if (f.numero) activos.push({ clave: 'numero', etiqueta: `N°: ${f.numero}` })
  if (f.solicitante) activos.push({ clave: 'solicitante', etiqueta: `Solicitante: ${f.solicitante}` })
  if (f.responsable) activos.push({ clave: 'responsable', etiqueta: `Responsable: ${f.responsable}` })
  if (f.prioridad) activos.push({ clave: 'prioridad', etiqueta: `Prioridad: ${f.prioridad}` })
  if (f.estado) {
    const opcion = OPCIONES_ESTADO_BUSQUEDA.find((o) => o.value === f.estado)
    activos.push({ clave: 'estado', etiqueta: `Estado: ${opcion?.label || f.estado}` })
  }
  if (f.categoria) {
    const cat = categorias.value.find((c) => c.codigoCategoria === f.categoria)
    activos.push({ clave: 'categoria', etiqueta: `Categoría: ${cat?.nombreCategoria || f.categoria}` })
  }
  if (f.subcategoria) {
    const sub = subcategorias.value.find((s) => s.codigoSubcategoria === f.subcategoria)
    activos.push({ clave: 'subcategoria', etiqueta: `Subcategoría: ${sub?.nombreSubcategoria || f.subcategoria}` })
  }
  // Con número la fecha no se envía (Cambio 11): mostrar el chip sería engañoso.
  if (!f.numero && f.fechaInicio) activos.push({ clave: 'fechaInicio', etiqueta: `Desde: ${f.fechaInicio}` })
  if (!f.numero && f.fechaFin) activos.push({ clave: 'fechaFin', etiqueta: `Hasta: ${f.fechaFin}` })
  return activos
})

const CLAVES_BACKEND = ['numero', 'estado', 'fechaInicio', 'fechaFin']

function quitarFiltroActivo(clave) {
  const f = filtrosAplicados.value
  if (!f) return
  const vacio = { numero: 0, solicitante: '', responsable: '', prioridad: '', estado: 0, categoria: 0, subcategoria: 0, fechaInicio: '', fechaFin: '' }
  filtrosAplicados.value = { ...f, [clave]: vacio[clave] }
  if (clave === 'categoria') filtrosAplicados.value.subcategoria = 0
  // Mantiene el panel sincronizado con lo aplicado.
  const borrador = {
    numero: numeroBusqueda, solicitante: solicitanteFiltro, responsable: responsableFiltro, prioridad: prioridadFiltro,
    estado: estadoFiltro, categoria: categoriaFiltro, subcategoria: subcategoriaFiltro,
    fechaInicio: fechaInicioBusqueda, fechaFin: fechaFinBusqueda,
  }
  borrador[clave].value = ''
  // Sin filtros, o si al quitar el N° solo quedan las fechas por defecto: se limpia la búsqueda.
  const soloFechas = filtrosActivos.value.every((c) => c.clave === 'fechaInicio' || c.clave === 'fechaFin')
  if (!filtrosActivos.value.length || (clave === 'numero' && soloFechas)) {
    limpiarFiltrosAvanzados()
    return
  }
  if (CLAVES_BACKEND.includes(clave)) ejecutarBusqueda()
}

// Accesos rápidos del toolbar (sobre la subpestaña visible).
const soloAltaPrioridad = ref(false)
const soloConfPendiente = ref(false)
function cumpleAccesosRapidos(r) {
  if (soloAltaPrioridad.value && !/alta|urgente/i.test(r.prioridad || '')) return false
  if (soloConfPendiente.value && r.codigoEstado !== ESTADO.ATENDIDO) return false
  return true
}

// -------------------------------------------------------------------------
// Reparto por subpestaña

const codigoUoActiva = computed(() => perfilActivo.value?.codigoUnidadOrganica || 0)

// Todos los iCodigo_Per con los que el usuario registró (el SP ya los expande por DNI).
const codigosSolicitante = computed(() => {
  const set = new Set(datos.value.mis.map((r) => r.codigoPersonaSolicitante))
  if (props.perfil?.codigoPersonaGr) set.add(props.perfil.codigoPersonaGr)
  return set
})

// Con filtros: cada pestaña es un subconjunto de su propio total (ver repartirResultados).
const requerimientosPorPestana = computed(() => {
  const f = filtrosAplicados.value
  const base = f
    ? repartirResultados(datos.value, resultadosBusqueda.value, f)
    : { ...datos.value, otros: [] }
  const salida = {}
  Object.keys(base).forEach((id) => { salida[id] = base[id].filter(cumpleAccesosRapidos) })
  return salida
})

// Número en el círculo de cada subpestaña (con búsqueda: lo encontrado en cada una).
const conteos = computed(() => {
  const r = requerimientosPorPestana.value
  return { otros: r.otros.length, noClasificados: r.noClasificados.length, pendientes: r.pendientes.length, mis: r.mis.length }
})

// Total de la búsqueda = requerimientos distintos entre las 4 pestañas (uno puede estar en dos).
const totalResultados = computed(() => {
  if (!filtrosAplicados.value) return 0
  const r = requerimientosPorPestana.value
  return new Set([...r.mis, ...r.pendientes, ...r.noClasificados, ...r.otros].map((x) => x.codigoRequerimiento)).size
})

const tablero = computed(() => construirTablero(pestana.value, requerimientosPorPestana.value[pestana.value]))
const columnasVisibles = computed(() =>
  metricaSeleccionada.value
    ? tablero.value.columns.filter((c) => c.title === metricaSeleccionada.value)
    : tablero.value.columns
)

// Paginación (Figma): tablero 10 por columna, lista (tabla) 15.
const TAM_PAGINA_TABLERO = 10
const TAM_PAGINA_LISTA = 15
const paginasColumna = ref({})
const paginaLista = ref(1)
function paginaColumna(column) {
  const total = Math.max(1, Math.ceil(column.total / TAM_PAGINA_TABLERO))
  return Math.min(paginasColumna.value[column.title] || 1, total)
}
function cambiarPaginaColumna(column, n) {
  paginasColumna.value = { ...paginasColumna.value, [column.title]: n }
}
function tarjetasPagina(column) {
  const p = paginaColumna(column)
  return column.cards.slice((p - 1) * TAM_PAGINA_TABLERO, p * TAM_PAGINA_TABLERO)
}
const filasVisibles = computed(() =>
  metricaSeleccionada.value ? tablero.value.filas.filter((f) => f.grupo === metricaSeleccionada.value) : tablero.value.filas
)
const paginaListaActual = computed(() =>
  Math.min(paginaLista.value, Math.max(1, Math.ceil(filasVisibles.value.length / TAM_PAGINA_LISTA)))
)
const filasPagina = computed(() =>
  filasVisibles.value.slice((paginaListaActual.value - 1) * TAM_PAGINA_LISTA, paginaListaActual.value * TAM_PAGINA_LISTA)
)
// Vuelve a la página 1 cuando cambian los datos, la pestaña, el filtro de métrica o la vista.
// Se compara por los números de la pestaña actual: la actualización automática de
// "No clasificados" no debe regresar a la página 1 si lo visible no cambió.
const firmaTablero = computed(() =>
  `${pestana.value}|${(requerimientosPorPestana.value[pestana.value] || []).map((r) => r.codigoRequerimiento).join(',')}`
)
watch([firmaTablero, metricaSeleccionada, vistaTarjetas], () => {
  paginasColumna.value = {}
  paginaLista.value = 1
})

function cambiarPestana(id) {
  pestana.value = id
  metricaSeleccionada.value = null
  cerrarDetalle()
}

// -------------------------------------------------------------------------
// Perfil

const nombreUsuario = computed(() => {
  const nombreCompleto = props.perfil?.nombreCompleto
  if (!nombreCompleto) return 'usuario'

  const palabras = nombreCompleto.trim().split(/\s+/)

  // palabras[0] = Guillen, palabras[2] = July
  const primerNombre = palabras[2] || palabras[0]
  const primerApellido = palabras[0]

  const formatoCapital = (texto) =>
    texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase()

  return `${formatoCapital(primerNombre)} ${formatoCapital(primerApellido)}`
})

const inicialesUsuario = computed(() => {
  const nombreCompleto = props.perfil?.nombreCompleto
  if (!nombreCompleto) return '--'
  const palabras = nombreCompleto.trim().split(/\s+/)
  return ((palabras[2]?.[0] || palabras[0]?.[0] || '') + (palabras[0]?.[0] || '')).toUpperCase()
})

// Fecha de hoy para el header (dd/mm/aaaa, igual al Figma).
const fechaHoy = computed(() => new Date().toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }))

const unidadOrganicaPerfil = computed(() => {
  const rol = perfilActivo.value
  if (!rol) return ''
  return (
    rol.unidadOrganica ||
    rol.nombreUnidadOrganica ||
    rol.descripcionUnidadOrganica ||
    rol.unidadOrganica?.descripcion ||
    rol.vDesLuo ||
    ''
  )
})

// Precarga la UO del usuario ({codigoUo, nombreUnidadOrganica}) en "Asignar requerimiento" (permite cambio).
const unidadOrganicaPropia = computed(() => {
  const rol = perfilActivo.value
  if (!rol) return null
  const codigoUo = rol.codigoUnidadOrganica ?? rol.codigoUo ?? rol.unidadOrganica?.codigo ?? null
  const nombreUnidadOrganica = unidadOrganicaPerfil.value
  if (!codigoUo || !nombreUnidadOrganica) return null
  return { codigoUo, nombreUnidadOrganica }
})

// Trae los roles activos para el desplegable del avatar.
async function cargarRolesUsuario() {
  const usuario = props.perfil?.usuarioWindows
  if (!usuario) return
  try {
    roles.value = await listarRoles(usuario)
    // Rol activo por defecto: primero de la lista del backend. Cambiable en "Mis roles".
    if (!perfilActivo.value && roles.value.length) {
      perfilActivo.value = roles.value[0]
      registrarIngresoRol(perfilActivo.value)
    }
  } catch (error) {
    console.error('No se pudieron cargar los roles del usuario.', error)
    roles.value = []
  }
}

// Cambia el rol activo desde el desplegable de perfil.
function seleccionarRol(rol) {
  const cambioDeRol = perfilActivo.value?.codigoPersonaRol !== rol.codigoPersonaRol
  perfilActivo.value = rol
  if (cambioDeRol) registrarIngresoRol(rol)
  menuAbierto.value = false
  mantenimientoActivo.value = opcionesMantenimientoActivas.value[0]?.id
  if (rol.nombreRol === 'Supervisor') {
    if (vista.value !== 'mantenimiento' && vista.value !== 'documentacion') {
      vista.value = 'mantenimiento'
    }
  } else if (vista.value === 'documentacion') {
    vista.value = 'requerimientos'
  }
}

// Registra el ingreso con el rol activo (como el sistema legado). No bloquea la pantalla si falla.
async function registrarIngresoRol(rol) {
  if (!rol?.codigoPersonaRol) return
  try {
    await registrarSesion(rol.codigoPersonaRol)
  } catch (error) {
    console.error('No se pudo registrar el inicio de sesión del rol.', error)
  }
}

function alternarMenuPerfil() {
  menuAbierto.value = !menuAbierto.value
}

function cerrarSesion() {
  menuAbierto.value = false
  emit('logout')
}

function irARequerimientos() {
  vista.value = 'requerimientos'
  mostrarNuevoRequerimiento.value = false
  cerrarDetalle()
}

function seleccionarRequerimiento(card, column) {
  selectedRequest.value = card
  selectedColumn.value = column
  menuAutorizacionesAbierto.value = false
  mensajeAutorizacion.value = ''
}

// Clic en la tarjeta o en la fila: abre la vista completa de detalle (ya no el panel lateral).
function abrirDetalle(card, column, seccion = null) {
  seleccionarRequerimiento(card, column || { title: card.grupo })
  observacionDetalle.value = ''
  vistaDetalle.value = true
  cargarDetalleCompleto(card.codigo)
  window.scrollTo({ top: 0 })
  if (seccion) setTimeout(() => document.getElementById(seccion)?.scrollIntoView({ behavior: 'smooth' }), 50)
}

function cerrarDetalle() {
  menuAutorizacionesAbierto.value = false
  selectedRequest.value = null
  selectedColumn.value = null
  vistaDetalle.value = false
  consultaDetalleId++
  detalleExtra.value = null
  historialDetalle.value = []
  adjuntosDetalle.value = []
}

const observacionDetalle = ref('')

// Datos completos del detalle (legado): medio de envío, correos copia, historial, etc.
const detalleExtra = ref(null)
const historialDetalle = ref([])
const adjuntosDetalle = ref([])
const cargandoDetalle = ref(false)
const errorDetalle = ref('')
let consultaDetalleId = 0
async function cargarDetalleCompleto(codigo) {
  const id = ++consultaDetalleId // descarta respuestas de un detalle anterior
  detalleExtra.value = null
  historialDetalle.value = []
  adjuntosDetalle.value = []
  errorDetalle.value = ''
  cargandoDetalle.value = true
  const [detalle, historial, adjuntos] = await Promise.allSettled([
    consultarDetalleRequerimiento(codigo),
    consultarHistorialRequerimiento(codigo),
    consultarAdjuntosRequerimiento(codigo),
  ])
  if (id !== consultaDetalleId) return
  if (detalle.status === 'fulfilled') detalleExtra.value = detalle.value
  if (historial.status === 'fulfilled') historialDetalle.value = historial.value || []
  if (adjuntos.status === 'fulfilled') adjuntosDetalle.value = adjuntos.value || []
  const fallo = [detalle, historial, adjuntos].find((r) => r.status === 'rejected')
  if (fallo) {
    console.error('No se pudo cargar el detalle completo.', fallo.reason)
    errorDetalle.value = fallo.reason?.message || 'No se pudo cargar el detalle completo.'
  }
  cargandoDetalle.value = false
}

// Valor del detalle del legado; si no vino, el de la tarjeta (de /buscar).
function dato(campo, respaldo = null) {
  const valor = detalleExtra.value?.[campo]
  return valor !== undefined && valor !== null && valor !== '' ? valor : respaldo
}
const descripcionDetalle = computed(() => {
  const html = detalleExtra.value?.descripcion
  return (html ? htmlATexto(html) : selectedRequest.value?.descripcion) || 'Sin descripción.'
})
const responsableDetalle = computed(() => {
  const nombre = dato('responsable', selectedRequest.value?.responsable)
  if (!nombre) return 'Sin asignar'
  return detalleExtra.value?.responsablePrincipal ? `${nombre} (PRINCIPAL)` : nombre
})
const inicialesSolicitanteDetalle = computed(() => {
  const p = String(dato('solicitante', selectedRequest.value?.solicitante) || '').trim().split(/\s+/).filter(Boolean)
  return p.length ? ((p[0][0] || '') + (p[1]?.[0] || '')).toUpperCase() : '--'
})
function imprimirDetalle() {
  window.print()
}

// Conformidad: solo el solicitante, sobre un requerimiento Atendido
// (se solicita automáticamente por correo al llegar a Atendido).
const puedeDarConformidad = computed(() => {
  const r = selectedRequest.value
  return !!r && r.codigoEstado === ESTADO.ATENDIDO && codigosSolicitante.value.has(r.raw?.codigoPersonaSolicitante)
})

// Botón inferior de la tarjeta (Figma): "Dar conformidad" en Mis requerimientos, "Asignar" en Por asignar.
function accionTarjeta(card, grupo) {
  if (pestana.value === 'mis' && card.codigoEstado === ESTADO.ATENDIDO) return 'conformidad'
  if (pestana.value === 'pendientes' && grupo === 'Por asignar') return 'asignar'
  return null
}
// Columna "Acción" de la tabla: además, "Adjuntar archivo" en mis requerimientos pendientes (Figma).
function accionFila(fila) {
  return accionTarjeta(fila, fila.grupo) || (pestana.value === 'mis' && fila.grupo === 'Pendientes' ? 'adjuntar' : null)
}
function ejecutarAccionTarjeta(card, grupo) {
  const accion = grupo === undefined ? accionFila(card) : accionTarjeta(card, grupo)
  if (accion === 'asignar') {
    seleccionarRequerimiento(card, { title: grupo || card.grupo })
    abrirModalAsignar()
  } else if (accion === 'adjuntar') {
    abrirDetalle(card, null, 'detalle-adjuntos')
  } else {
    abrirDetalle(card, { title: grupo || card.grupo })
  }
}

function tonoEstado(codigo) {
  return {
    [ESTADO.REGISTRADO]: 'gris', [ESTADO.POR_ASIGNAR]: 'ambar', [ESTADO.ASIGNADO]: 'verde',
    [ESTADO.EN_PROCESO]: 'azul', [ESTADO.ATENDIDO]: 'azul', [ESTADO.NO_CONFORME]: 'rojo',
  }[codigo] || 'gris'
}

// Etiquetas de la tarjeta según el estado real del requerimiento.
function etiquetasTarjeta(card) {
  if (pestana.value === 'noClasificados') return [{ texto: 'Por clasificar', tono: 'gris' }]
  const tono = tonoEstado(card.codigoEstado)
  const etiquetas = card.estado ? [{ texto: card.estado, tono }] : []
  if (card.codigoEstado === ESTADO.ATENDIDO && pestana.value !== 'mis') etiquetas.push({ texto: 'Conf. pendiente', tono: 'ambar' })
  return etiquetas
}

function iconoPrioridad(prioridad) {
  const p = (prioridad || '').toLowerCase()
  if (p.includes('urgente')) return 'zap'
  if (p.includes('alta') || p.includes('importante')) return 'star'
  return null
}
function clasePrioridad(prioridad) {
  return (prioridad || 'normal').toLowerCase().split(/\s+/)[0]
}

// -------------------------------------------------------------------------
// Registro de Requerimiento -- todavía no tiene un endpoint
// igual que otras acciones de prototipo ya existentes
// (Mover de estado, Solicitar autorización).
const mostrarNuevoRequerimiento = ref(false)
const nuevoGerencia = ref('')
const nuevoDivision = ref('')
const nuevoUnidad = ref('')
const nuevoCategoria = ref('')
const nuevoSubcategoria = ref('')
const nuevoDescripcion = ref('')
const nuevoPrioridad = ref('normal') // 'normal' | 'alta' | 'urgente'
const nuevoTipoDato = ref('')
const nuevoValorDato = ref('')
const nuevoApellidoPaterno = ref('')
const LARGO_MAXIMO_DESCRIPCION = 2000

function limpiarFormularioNuevo() {
  nuevoGerencia.value = ''
  nuevoDivision.value = ''
  nuevoUnidad.value = ''
  nuevoCategoria.value = ''
  nuevoSubcategoria.value = ''
  nuevoDescripcion.value = ''
  nuevoPrioridad.value = 'normal'
  nuevoTipoDato.value = ''
  nuevoValorDato.value = ''
  nuevoApellidoPaterno.value = ''
}

function abrirNuevoRequerimiento() {
  limpiarFormularioNuevo()
  cerrarDetalle()
  mostrarNuevoRequerimiento.value = true
}

function cerrarNuevoRequerimiento() {
  mostrarNuevoRequerimiento.value = false
}

// -------------------------------------------------------------------------
// Asignar requerimiento: modal + confirmación para el endpoint
// POST /api/requerimiento/asignar, visible solo en "Pendientes"

// Solo en "Mis pendientes" y mientras no esté Atendido/Cerrado (lista blanca por código de estado).
const ESTADOS_ASIGNABLES = [ESTADO.REGISTRADO, ESTADO.POR_ASIGNAR, ESTADO.ASIGNADO, ESTADO.EN_PROCESO, ESTADO.NO_CONFORME]
const puedeAsignar = computed(() =>
  vista.value === 'requerimientos' && pestana.value === 'pendientes' &&
  ESTADOS_ASIGNABLES.includes(selectedRequest.value?.codigoEstado)
)

const mostrarModalAsignar = ref(false)
const mostrarConfirmacionAsignar = ref(false)
const asignando = ref(false)
const errorAsignacion = ref('')

// Unidad Orgánica: solo búsqueda
// por nombre /{nomUO}), se usa un input con autocompletado en vivo.
const unidadOrganicaTexto = ref('')
const unidadesOrganicas = ref([])
const unidadOrganicaSeleccionada = ref(null)
const buscandoUnidadOrganica = ref(false)
let temporizadorBusquedaUo = null

// Personal: se carga recién cuando se elige la Unidad Orgánica
const personas = ref([])
const personaSeleccionada = ref(null)
const cargandoPersonas = ref(false)

// Buscador de personas con filtro en memoria sobre la UO seleccionada.
// El cargo se muestra aparte (lectura), no dentro del buscador.
const personaTexto = ref('')

const responsablePrincipal = ref(false)
const tipoObservacion = ref('motivo')

// Campo editable en el modal (no hardcodeado).
const observacionAsignar = ref('')
const LARGO_MAXIMO_OBSERVACION = 2000 // Nota del sistema antiguo: máx. 2000 caracteres

function codigoRequerimientoSeleccionado() {
  return selectedRequest.value?.codigo ?? null
}

function abrirModalAsignar() {
  unidadOrganicaTexto.value = ''
  unidadesOrganicas.value = []
  unidadOrganicaSeleccionada.value = null
  personas.value = []
  personaSeleccionada.value = null
  personaTexto.value = ''
  responsablePrincipal.value = false
  tipoObservacion.value = 'motivo'
  observacionAsignar.value = ''
  errorAsignacion.value = ''
  mostrarModalAsignar.value = true
  // Precarga la Unidad Orgánica del usuario logueado por defecto; queda el
  // link "cambiar" en caso de derivar a otra oficina.
  if (unidadOrganicaPropia.value) {
    seleccionarUnidadOrganica(unidadOrganicaPropia.value)
  }
}

function cerrarModalAsignar() {
  mostrarModalAsignar.value = false
}

// Búsqueda de Unidad Orgánica con debounce (350ms) a partir de 3 caracteres.
watch(unidadOrganicaTexto, (texto) => {
  clearTimeout(temporizadorBusquedaUo)
  if (unidadOrganicaSeleccionada.value && texto === unidadOrganicaSeleccionada.value.nombreUnidadOrganica) {
    return                          // no relanzar la búsqueda apenas se selecciona una opción
  }
  unidadOrganicaSeleccionada.value = null
  personaSeleccionada.value = null
  personas.value = []
  if (!texto || texto.trim().length < 3) {
    unidadesOrganicas.value = []
    return
  }
  temporizadorBusquedaUo = setTimeout(async () => {
    buscandoUnidadOrganica.value = true
    try {
      unidadesOrganicas.value = await buscarUnidadOrganica(texto.trim())
    } catch (error) {
      console.error('No se pudo buscar la unidad orgánica.', error)
      unidadesOrganicas.value = []
    } finally {
      buscandoUnidadOrganica.value = false
    }
  }, 350)
})

// Descarta la persona elegida si el usuario edita el texto posteriormente.
watch(personaTexto, (texto) => {
  if (personaSeleccionada.value && texto === personaSeleccionada.value.nombre) {
    return
  }
  personaSeleccionada.value = null
})

// Filtro local en memoria sobre la lista de personas previamente cargada.
const personasFiltradas = computed(() => {
  const texto = personaTexto.value.trim().toLowerCase()
  if (!texto) return personas.value
  return personas.value.filter((p) => (p.nombre || '').toLowerCase().includes(texto))
})

// Despliega la lista al editar y la oculta al seleccionar una persona.
const mostrarListaPersonas = computed(() =>
  !!personaTexto.value && (!personaSeleccionada.value || personaTexto.value !== personaSeleccionada.value.nombre)
)

function seleccionarPersona(persona) {
  personaSeleccionada.value = persona
  personaTexto.value = persona.nombre
}

// "✎ cambiar": limpia y reapertura la búsqueda de Personal para la misma UO.
function cambiarPersona() {
  personaSeleccionada.value = null
  personaTexto.value = ''
}

// "✎ cambiar": limpia y reabre la búsqueda de UO, reseteando el Personal cargado.
function cambiarUnidadOrganica() {
  unidadOrganicaSeleccionada.value = null
  unidadOrganicaTexto.value = ''
  personas.value = []
  personaSeleccionada.value = null
  personaTexto.value = ''
}

async function seleccionarUnidadOrganica(uo) {
  unidadOrganicaSeleccionada.value = uo
  unidadOrganicaTexto.value = uo.nombreUnidadOrganica
  unidadesOrganicas.value = []
  personaSeleccionada.value = null
  personaTexto.value = ''
  cargandoPersonas.value = true
  try {
    personas.value = await listarPersonas({ codigoUo: uo.codigoUo, vigencia: 1 })
  } catch (error) {
    console.error('No se pudieron cargar las personas de la unidad orgánica.', error)
    personas.value = []
  } finally {
    cargandoPersonas.value = false
  }
}

const puedeConfirmarAsignacion = computed(() =>
  !!personaSeleccionada.value && observacionAsignar.value.trim().length > 0
)

function pedirConfirmacionAsignar() {
  errorAsignacion.value = ''
  if (!puedeConfirmarAsignacion.value) return
  mostrarConfirmacionAsignar.value = true
}

function cancelarConfirmacionAsignar() {
  mostrarConfirmacionAsignar.value = false
}

// Ejecuta la asignación recién al aceptar el modal de confirmación
// ("¿Está seguro de asignar el requerimiento?")
async function confirmarAsignacion() {
  asignando.value = true
  errorAsignacion.value = ''
  try {
    await asignarRequerimiento({
      codigoRequerimiento: codigoRequerimientoSeleccionado(),
      // Persona que asigna: datos extraídos del usuario logueado en el perfil.
      codigoPersonaAsigna: props.perfil?.codigoPersonaGr,                   // iCodigo_Per
      codigoPersonaActualizacion: props.perfil?.codigoPersonaOrganizacion, // cCodPer (auditoría)
      // Persona destino = la elegida en el combo "Personal".
      codigoPersonaResponsable: personaSeleccionada.value?.codigoPersona, // cCodPer destino
      responsablePrincipal: responsablePrincipal.value,
      informeTecnico: tipoObservacion.value === 'informeTecnico',
      observacion: observacionAsignar.value.trim(),
    })
    mostrarConfirmacionAsignar.value = false
    mostrarModalAsignar.value = false
    cerrarDetalle()
    await refrescar() // refleja el nuevo estado/columna sin recargar la página
  } catch (error) {
    console.error('No se pudo asignar el requerimiento.', error)
    errorAsignacion.value = error?.message || 'No se pudo asignar el requerimiento.'
    mostrarConfirmacionAsignar.value = false
  } finally {
    asignando.value = false
  }
}

// -------------------------------------------------------------------------
// Autorizaciones: la lógica del modal vive en
// components/requerimiento/SolicitarAutorizacionModal.vue. Aquí solo se
// controla el menú "Autorizaciones", el modo con que se abre el modal
// ('solicitar' | 'consultar' | 'anular') y el mensaje del panel de detalle.
const mostrarModalAutorizacion = ref(false)
const modoAutorizacion = ref('solicitar')
const mensajeAutorizacion = ref('')

function abrirModalAutorizacion(modo = 'solicitar') {
  mensajeAutorizacion.value = ''
  modoAutorizacion.value = modo
  mostrarModalAutorizacion.value = true
}

// Menú desplegable: se cierra al elegir una opción, al hacer clic fuera y con Escape.
const menuAutorizacionesAbierto = ref(false)
const menuAutorizacionesRef = ref(null)
const botonAutorizacionesRef = ref(null)

async function alternarMenuAutorizaciones() {
  menuAutorizacionesAbierto.value = !menuAutorizacionesAbierto.value
  if (menuAutorizacionesAbierto.value) {
    await nextTick()
    menuAutorizacionesRef.value?.querySelector('[role="menuitem"]')?.focus()
  }
}

function cerrarMenuAutorizaciones(devolverFoco = false) {
  menuAutorizacionesAbierto.value = false
  if (devolverFoco) botonAutorizacionesRef.value?.focus()
}

function elegirAccionAutorizacion(modo) {
  cerrarMenuAutorizaciones()
  abrirModalAutorizacion(modo)
}

// Teclado: flechas para moverse entre opciones, Escape para cerrar.
function alTeclaMenuAutorizaciones(event) {
  if (!menuAutorizacionesAbierto.value) return
  const opciones = [...(menuAutorizacionesRef.value?.querySelectorAll('[role="menuitem"]') || [])]
  const actual = opciones.indexOf(document.activeElement)
  if (event.key === 'Escape') {
    event.preventDefault()
    cerrarMenuAutorizaciones(true)
  } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    const paso = event.key === 'ArrowDown' ? 1 : -1
    opciones[(actual + paso + opciones.length) % opciones.length]?.focus()
  }
}

function alClicFueraMenuAutorizaciones(event) {
  if (!menuAutorizacionesAbierto.value) return
  const contenedor = menuAutorizacionesRef.value // botón + lista de opciones
  if (contenedor && !contenedor.contains(event.target)) cerrarMenuAutorizaciones()
}
onMounted(() => document.addEventListener('pointerdown', alClicFueraMenuAutorizaciones))
onBeforeUnmount(() => document.removeEventListener('pointerdown', alClicFueraMenuAutorizaciones))

function cerrarModalAutorizacion() {
  mostrarModalAutorizacion.value = false
}

function alPrepararSolicitudAutorizacion(payload) {
  mostrarModalAutorizacion.value = false
  mensajeAutorizacion.value = `Solicitud de autorización preparada para ${payload.codigosPersonaAutoriza.length} persona(s).`
}

// Carga según rol activo: Administrador usa búsqueda manual; Operador autocarga
const modoBusqueda = computed(() => {
  const rol = perfilActivo.value?.nombreRol
  if (rol === 'Administrador') return 'manual'
  if (rol === 'Operador') return 'automatico'
  return null
})
// Usa la UO del rol activo como aproximación (el SP solo filtra por UO solicitante/responsable).
const unidadOrganicaBusqueda = computed(() => perfilActivo.value?.codigoUnidadOrganica || 0)

// Supervisor:
const menuSupervisor = computed(() => perfilActivo.value?.nombreRol === 'Supervisor')

// Mantenimientos: Supervisor ve el catálogo completo; Administrador (y demás roles)
// el submenú del Figma: Personas, Problemas y soluciones, Categorías.
const OPCIONES_MANTENIMIENTO_SUPERVISOR = [
  { id: 'personas', label: 'Personas', backend: true },
  { id: 'categorias', label: 'Categorías', backend: true },
  { id: 'subcategorias', label: 'Sub Categorías', backend: true },
  { id: 'unidadOrganica', label: 'Unidad Orgánica', backend: true },
  { id: 'problemas', label: 'Problemas', backend: false },
  { id: 'soluciones', label: 'Soluciones', backend: false },
  { id: 'personaCategoria', label: 'Relación Persona - Categoría/SubCategoría', backend: false },
  { id: 'personaUnidadOrganica', label: 'Relación Persona - Unidad Orgánica', backend: false },
  { id: 'problemaSolucion', label: 'Relación Problema - Solución', backend: false },
  { id: 'categoriaSubcategoria', label: 'Relación Categoría - SubCategoría', backend: false },
]
const OPCIONES_MANTENIMIENTO_ADMINISTRADOR = [
  { id: 'personas', label: 'Personas', backend: true },
  { id: 'problemasSoluciones', label: 'Problemas y soluciones', backend: false },
  { id: 'categorias', label: 'Categorías', backend: true },
]
const opcionesMantenimientoActivas = computed(() =>
  menuSupervisor.value ? OPCIONES_MANTENIMIENTO_SUPERVISOR : OPCIONES_MANTENIMIENTO_ADMINISTRADOR
)
const mantenimientoActivo = ref(OPCIONES_MANTENIMIENTO_ADMINISTRADOR[0].id)

function irAMantenimientos(id) {
  vista.value = 'mantenimiento'
  mantenimientoActivo.value = id || opcionesMantenimientoActivas.value[0]?.id
  mostrarNuevoRequerimiento.value = false
  cerrarDetalle()
}

function irADocumentacion() {
  vista.value = 'documentacion'
  mostrarNuevoRequerimiento.value = false
  cerrarDetalle()
}

// Recarga las subpestañas (y la búsqueda activa, si hay).
async function refrescar() {
  cargando.value = true
  const resultado = await cargarPestanas(props.perfil, codigoUoActiva.value)
  datos.value = { mis: resultado.mis, pendientes: resultado.pendientes, noClasificados: resultado.noClasificados }
  conectado.value = resultado.online
  mensajeError.value = resultado.errorMessage || ''
  metricaSeleccionada.value = null
  cargando.value = false
  if (filtrosAplicados.value) await ejecutarBusqueda()
}

// "No clasificados" depende de la UO del rol activo: recarga si cambia de rol.
let inicializado = false
watch(codigoUoActiva, () => { if (inicializado) refrescar() }, { flush: 'sync' })

// No clasificados en automático: cada 15 s y al volver a la pestaña del navegador.
// Solo esa consulta y sin "Cargando…", para no interrumpir lo que el usuario está viendo.
const INTERVALO_NO_CLASIFICADOS_MS = 15000
let temporizadorNoClasificados = null
let consultandoNoClasificados = false

async function actualizarNoClasificados() {
  if (!inicializado || cargando.value || consultandoNoClasificados || document.hidden) return
  consultandoNoClasificados = true
  const uo = codigoUoActiva.value
  try {
    const lista = (await cargarNoClasificados(uo)) || []
    const firma = (l) => l.map((r) => `${r.codigoRequerimiento}:${r.codigoEstado}`).join(',')
    // Solo si hubo cambios; si cambió de rol mientras consultaba se descarta (refrescar trae la UO nueva).
    if (uo === codigoUoActiva.value && firma(lista) !== firma(datos.value.noClasificados)) {
      datos.value = { ...datos.value, noClasificados: lista }
    }
  } catch (e) {
    console.error('No se pudo actualizar No clasificados.', e)
  } finally {
    consultandoNoClasificados = false
  }
}

function alCambiarVisibilidad() {
  if (!document.hidden) actualizarNoClasificados()
}

// Primero el rol activo (da la UO) y luego las subpestañas: evita cargar dos veces.
onMounted(async () => {
  await cargarRolesUsuario()
  inicializado = true
  refrescar()
  temporizadorNoClasificados = setInterval(actualizarNoClasificados, INTERVALO_NO_CLASIFICADOS_MS)
  document.addEventListener('visibilitychange', alCambiarVisibilidad)
})

onBeforeUnmount(() => {
  clearInterval(temporizadorNoClasificados)
  document.removeEventListener('visibilitychange', alCambiarVisibilidad)
})
</script>

<template>
  <main class="dashboard" :class="{ 'sidebar-collapsed': sidebarColapsado }">

    <!-- Header (Figma): menú + marca SAT | fecha, título, notificaciones, perfil con rol activo -->
    <header class="dashboard-header">
      <button class="hamburger" aria-label="Mostrar u ocultar menú" @click="alternarSidebar"><Icon name="menu" :size="20" /></button>
      <div class="sat-brand"><strong>SAT</strong><span>Servicio de<br>Administración<br>Tributaria de Lima</span></div>
      <div class="header-actions">
        <span class="header-date"><Icon name="calendar" :size="15" />{{ fechaHoy }}</span>
        <span class="header-title">Gestión de Requerimientos</span>
        <button class="bell" aria-label="Notificaciones"><Icon name="bell" :size="18" /><b>1</b></button>
        <div class="profile-wrap">
          <button class="profile-trigger" @click="alternarMenuPerfil">
            <span class="avatar">{{ inicialesUsuario }}</span>
            <span class="profile-id">
              <strong>{{ nombreUsuario }}</strong>
              <span v-if="perfilActivo" class="role-badge">Perfil: {{ perfilActivo.nombreRol }}</span>
            </span>
            <Icon name="chevron-down" :size="15" />
          </button>
          <div v-if="menuAbierto" class="role-menu">
            <div class="role-menu-header">
              <strong>{{ nombreUsuario }}</strong><small>{{ perfil?.usuarioWindows }}</small>
              <small v-if="unidadOrganicaPerfil" class="role-menu-uo"><Icon name="building" :size="12" /> {{ unidadOrganicaPerfil }}</small>
            </div>
            <div class="role-menu-section">
              <span>Mis roles</span>
              <ul>
                <li v-for="r in roles" :key="r.codigoPersonaRol" class="rol-opcion" :class="{ activo: perfilActivo?.codigoPersonaRol === r.codigoPersonaRol }" @click="seleccionarRol(r)">{{ r.nombreRol }}</li>
                <li v-if="!roles.length" class="role-menu-empty">Sin roles activos</li>
              </ul>
            </div>
            <button class="role-menu-logout" @click="cerrarSesion">Cerrar sesión</button>
          </div>
        </div>
      </div>
    </header>

    <!-- Sidebar (Figma) -->
    <aside class="sidebar" :class="{ collapsed: sidebarColapsado }">
      <nav>
        <template v-if="menuSupervisor">
          <p class="nav-section-label">NAVEGACIÓN PRINCIPAL</p>
          <button class="nav-button" :class="{ selected: vista === 'mantenimiento' }" @click="irAMantenimientos()" title="Mantenimientos"><Icon name="wrench" :size="18" /> <span>Mantenimientos</span></button>
          <button class="nav-button" :class="{ selected: vista === 'documentacion' }" @click="irADocumentacion" title="Documentación"><Icon name="file-text" :size="18" /> <span>Documentación</span></button>
          <button class="nav-button" title="Ayuda"><Icon name="help-circle" :size="18" /> <span>Ayuda</span></button>
        </template>
        <template v-else>
          <p class="nav-section-label">NAVEGACIÓN PRINCIPAL</p>
          <button class="nav-button" :class="{ selected: vista === 'requerimientos' }" @click="irARequerimientos" title="Requerimientos">
            <Icon name="folder" :size="18" /> <span>Requerimientos</span>
          </button>
          <button class="nav-button menu-parent" :class="{ open: mantenimientoAbierto, activo: vista === 'mantenimiento' }" @click="mantenimientoAbierto = !mantenimientoAbierto" title="Mantenimiento">
            <Icon name="wrench" :size="18" /> <span>Mantenimiento</span> <Icon class="chev" name="chevron-down" :size="14" />
          </button>
          <div v-if="mantenimientoAbierto" class="submenu">
            <button v-for="opcion in opcionesMantenimientoActivas" :key="opcion.id" :class="{ selected: vista === 'mantenimiento' && mantenimientoActivo === opcion.id }" @click="irAMantenimientos(opcion.id)">{{ opcion.label }}</button>
          </div>
          <hr class="nav-divider">
          <p class="nav-section-label">HERRAMIENTAS</p>
          <button class="nav-button" title="Reportes"><Icon name="bar-chart" :size="18" /> <span>Reportes</span></button>
          <button class="nav-button" title="Incidencias"><Icon name="wrench" :size="18" /> <span>Incidencias</span></button>
          <button class="nav-button" title="Ayuda"><Icon name="help-circle" :size="18" /> <span>Ayuda</span></button>
        </template>
      </nav>
      <button class="sidebar-logout" @click="cerrarSesion" title="Cerrar sesión">
        <Icon name="log-out" :size="15" /> <span>Cerrar sesión</span>
      </button>
    </aside>

    <!-- Contenido -->
    <section class="dashboard-content">
      <template v-if="vista === 'requerimientos'">

        <!-- Registro de Requerimiento: solo arma la imagen del Figma, -->
        <!-- "Registrar" queda deshabilitado porque el backend no tiene ese endpoint todavía. -->
        <template v-if="mostrarNuevoRequerimiento">
          <button class="detail-back" @click="cerrarNuevoRequerimiento"><Icon name="arrow-left" :size="14" /> Volver al tablero</button>
          <p class="section-eyebrow" style="margin-top:10px">OPERACIONES · REGISTRO</p>
          <div class="greeting greeting--form">
            <div>
              <h1>Registro de Requerimiento</h1>
              <p>Completa los datos para registrar un nuevo requerimiento.</p>
            </div>
            <div class="form-toolbar">
              <button class="modal-btn-secundario" @click="limpiarFormularioNuevo">Limpiar</button>
              <button class="modal-btn-primario" disabled title="Aún no hay un endpoint de registro en service-requerimiento">Registrar</button>
            </div>
          </div>

          <div class="nuevo-req-grid">
            <div class="nuevo-req-col">

              <div class="detail-card">
                <h3>Unidad Orgánica a quien se solicita</h3>
                <p class="form-hint">Selecciona la unidad orgánica que atenderá el requerimiento.</p>
                <div class="modal-grid-2">
                  <div class="form-field"><label>GERENCIA CENTRAL / GERENCIA *</label><select v-model="nuevoGerencia"><option value="">— Seleccione —</option></select></div>
                  <div class="form-field"><label>DIVISIÓN / UNIDAD</label><select v-model="nuevoDivision"><option value="">— Seleccione —</option></select></div>
                  <div class="form-field"><label>UNIDAD</label><select v-model="nuevoUnidad"><option value="">— Seleccione —</option></select></div>
                  <div class="form-field"><label>CATEGORÍA *</label><select v-model="nuevoCategoria"><option value="">— Seleccione —</option></select></div>
                </div>
                <div class="form-field"><label>SUBCATEGORÍA</label><select v-model="nuevoSubcategoria"><option value="">— Seleccione —</option></select></div>
              </div>

              <div class="detail-card">
                <div class="detail-card-head"><h3>Descripción del Requerimiento</h3><span class="form-hint">{{ nuevoDescripcion.length }} / {{ LARGO_MAXIMO_DESCRIPCION }}</span></div>
                <textarea class="form-input-block" v-model="nuevoDescripcion" :maxlength="LARGO_MAXIMO_DESCRIPCION" rows="5" placeholder="Describe detalladamente el requerimiento..."></textarea>
                <p class="form-hint">Salto de línea: MAYÚS + Enter</p>
              </div>

              <div class="detail-card">
                <h3>Documentos Adjuntos</h3>
                <div class="nota-box">
                  <strong>Nota:</strong>
                  <ul><li>Tamaño máximo 10 MB por archivo.</li><li>Seleccionar archivo → Adjuntar. Repetir para más archivos.</li></ul>
                </div>
                <div class="adjuntar-row">
                  <button class="modal-btn-secundario"><Icon name="paperclip" :size="14" /> Seleccionar archivo</button>
                  <span class="form-hint">Ningún archivo seleccionado</span>
                  <button class="modal-btn-primario" disabled>Adjuntar</button>
                </div>
              </div>
            </div>

            <div class="nuevo-req-col nuevo-req-col--lateral">
              <div class="detail-card">
                <h3>Prioridad</h3>
                <label class="prioridad-opcion" :class="{ selected: nuevoPrioridad === 'normal' }">
                  <input type="radio" value="normal" v-model="nuevoPrioridad"> Normal
                </label>
                <label class="prioridad-opcion" :class="{ selected: nuevoPrioridad === 'alta' }">
                  <input type="radio" value="alta" v-model="nuevoPrioridad">
                  <span class="priority-badge alta"><Icon name="star" :size="10" /> Alta</span>
                </label>
                <label class="prioridad-opcion" :class="{ selected: nuevoPrioridad === 'urgente' }">
                  <input type="radio" value="urgente" v-model="nuevoPrioridad">
                  <span class="priority-badge urgente"><Icon name="zap" :size="10" /> Urgente</span>
                </label>
              </div>

              <div class="detail-card">
                <h3>Datos Complementarios</h3>
                <div class="form-field"><label>TIPO DE DATO</label><select v-model="nuevoTipoDato"><option value="">— Seleccione —</option></select></div>
                <div class="form-field"><label>VALOR</label><input v-model="nuevoValorDato" placeholder="Ingrese el valor..."></div>
              </div>

              <div class="detail-card">
                <h3>Correos Copia</h3>
                <div class="form-field"><label>APELLIDO PATERNO</label><input v-model="nuevoApellidoPaterno" placeholder="Buscar colaborador..."></div>
                <p class="conn-status">(*) Nota: No aplica para la Gerencia de Asuntos Legales.</p>
              </div>
            </div>
          </div>
        </template>

        <!-- Detalle de requerimiento (vista completa) -->
        <template v-else-if="vistaDetalle && selectedRequest">
          <div class="detalle-head">
            <div>
              <button class="detail-back" @click="cerrarDetalle"><Icon name="arrow-left" :size="14" /> Volver al tablero</button>
              <p class="section-eyebrow">DETALLE DE REQUERIMIENTO</p>
              <div class="detalle-titulo">
                <h1>{{ selectedRequest.numero }}</h1>
                <span class="estado-badge" :class="tonoEstado(selectedRequest.codigoEstado)"><i></i>{{ selectedRequest.estado || selectedColumn?.title }}</span>
                <span class="priority-badge" :class="clasePrioridad(selectedRequest.prioridad)">
                  <Icon v-if="iconoPrioridad(selectedRequest.prioridad)" :name="iconoPrioridad(selectedRequest.prioridad)" :size="10" />
                  {{ selectedRequest.prioridad }}
                </span>
              </div>
            </div>
            <div class="detalle-acciones">
              <button @click="imprimirDetalle"><Icon name="printer" :size="15" /> Imprimir</button>
              <button disabled title="Pendiente: falta endpoint en service-requerimiento"><Icon name="check" :size="15" /> Iniciar</button>
              <button :disabled="!puedeAsignar" :title="puedeAsignar ? '' : 'Solo desde Mis pendientes y si no está Atendido/Cerrado'" @click="abrirModalAsignar"><Icon name="user" :size="15" /> Asignar</button>
              <button disabled title="Pendiente: falta endpoint en service-requerimiento"><Icon name="file-text" :size="15" /> Clasificar</button>
              <button disabled title="Pendiente: falta endpoint en service-requerimiento"><Icon name="clock" :size="15" /> Evaluar impacto</button>
              <button class="detalle-accion-borde" disabled title="Pendiente: falta endpoint en service-requerimiento">⚠ Problema/Solución</button>
              <template v-if="puedeDarConformidad">
                <button class="conformidad-si"><Icon name="check" :size="15" /> Dar conformidad</button>
                <button class="conformidad-no"><Icon name="x" :size="15" /> Dar no conformidad</button>
              </template>
            </div>
          </div>

          <p v-if="errorDetalle" class="conn-status conn-status--offline detalle-aviso">⚠ No se pudo cargar parte del detalle (medio de envío, correos, historial, adjuntos): {{ errorDetalle }} <button @click="cargarDetalleCompleto(selectedRequest.codigo)">Reintentar</button></p>

          <div class="detalle-grid">
            <div class="detalle-col">
              <section class="detalle-card">
                <h3>Datos del Requerimiento</h3>
                <dl class="detalle-datos">
                  <div><dt>N° Requerimiento</dt><dd>{{ selectedRequest.numero }}</dd></div>
                  <div><dt>Fecha</dt><dd>{{ selectedRequest.fechaHora }}</dd></div>
                  <div><dt>Estado actual</dt><dd>{{ selectedRequest.estado || '—' }}</dd></div>
                  <div><dt>Prioridad</dt><dd>{{ selectedRequest.prioridad.toUpperCase() }}</dd></div>
                  <div><dt>Categoría</dt><dd>{{ selectedRequest.categoria || '—' }}</dd></div>
                  <div><dt>Subcategoría</dt><dd>{{ selectedRequest.subcategoria || '—' }}</dd></div>
                  <div><dt>Clasificación (GIN-PR001)</dt><dd>{{ dato('clasificacion', '—') }}</dd></div>
                  <div><dt>Medio envío</dt><dd>{{ dato('medioEnvio', '—') }}</dd></div>
                </dl>
                <p class="detalle-label">Descripción</p>
                <div class="detalle-descripcion">{{ descripcionDetalle }}</div>
              </section>

              <section class="detalle-card detalle-card--tabla">
                <h3>Correos Copia</h3>
                <table class="detalle-tabla">
                  <thead><tr><th>Usuario</th><th>Apellidos y Nombres</th><th>Cargo</th></tr></thead>
                  <tbody>
                    <tr v-for="(c, i) in (detalleExtra?.correosCopia || [])" :key="`${c.usuario}-${i}`">
                      <td class="td-usuario">{{ c.usuario || '—' }}</td><td><strong>{{ c.nombre || '—' }}</strong></td><td>{{ c.cargo || '—' }}</td>
                    </tr>
                    <tr v-if="!(detalleExtra?.correosCopia || []).length">
                      <td colspan="3" class="td-vacio">{{ cargandoDetalle ? 'Cargando…' : 'Sin correos en copia.' }}</td>
                    </tr>
                  </tbody>
                </table>
              </section>

              <section class="detalle-card detalle-card--tabla">
                <h3>Historial del Requerimiento</h3>
                <table class="detalle-tabla">
                  <thead><tr><th>Mov.</th><th>Fecha</th><th>Estado</th><th>Origen</th><th>Destino</th><th>Observación</th></tr></thead>
                  <tbody>
                    <tr v-for="h in historialDetalle" :key="h.codigoMovimiento || h.numero">
                      <td>{{ h.numero }}</td>
                      <td class="td-fecha-hora">{{ formatearFechaHora(h.fecha) }}</td>
                      <td><span class="estado-pill azul">{{ h.estado || '—' }}</span></td>
                      <td class="td-persona">{{ h.personaOrigen || '—' }}<small v-if="h.unidadOrigen">{{ h.unidadOrigen }}</small></td>
                      <td class="td-persona">{{ h.personaDestino || '—' }}<small v-if="h.unidadDestino">{{ h.unidadDestino }}</small></td>
                      <td class="td-observacion">{{ h.observacion || '—' }}</td>
                    </tr>
                    <tr v-if="!historialDetalle.length">
                      <td colspan="6" class="td-vacio">{{ cargandoDetalle ? 'Cargando…' : 'Sin movimientos registrados.' }}</td>
                    </tr>
                  </tbody>
                </table>
              </section>

              <section id="detalle-adjuntos" class="detalle-card">
                <h3>Documentos Adjuntos</h3>
                <div class="nota-box">
                  <strong>Nota:</strong>
                  <ul><li>Tamaño máximo 10 MB por archivo.</li><li>Seleccionar archivo → Adjuntar. Repetir para más archivos.</li></ul>
                </div>
                <div class="adjuntar-row">
                  <button class="modal-btn-secundario"><Icon name="paperclip" :size="14" /> Seleccionar archivo</button>
                  <span class="form-hint">Ningún archivo seleccionado</span>
                  <button class="modal-btn-primario" disabled title="Pendiente: falta endpoint">Adjuntar</button>
                </div>
                <!-- Archivos ya adjuntos (como el sistema actual). Abrir el archivo queda pendiente del servicio de documentos. -->
                <div class="adjuntos-lista">
                  <table class="detalle-tabla">
                    <thead><tr><th>Arc.</th><th>Fecha carga</th><th>Tipo adjunto</th><th>Descripción</th><th>Usuario</th></tr></thead>
                    <tbody>
                      <tr v-for="(d, i) in adjuntosDetalle" :key="`${d.codigoDocumento}-${d.numero ?? i}`">
                        <td>
                          <button class="adjunto-abrir" disabled :title="`${d.nombreOriginal || d.nombreArchivo || 'Archivo'} — la descarga estará disponible cuando exista el servicio de documentos`">
                            <Icon name="folder" :size="16" />
                          </button>
                        </td>
                        <td class="td-fecha-hora">{{ d.fechaCarga || '—' }}</td>
                        <td>{{ d.tipoAdjunto || '—' }}</td>
                        <td>{{ d.descripcion || '—' }}<small v-if="d.nombreOriginal" class="adjunto-nombre">{{ d.nombreOriginal }}</small></td>
                        <td>{{ d.usuario || '—' }}</td>
                      </tr>
                      <tr v-if="!adjuntosDetalle.length">
                        <td colspan="5" class="td-vacio">{{ cargandoDetalle ? 'Cargando…' : 'Sin archivos adjuntos.' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>

            <div class="detalle-col">
              <section class="detalle-card">
                <h3>Datos del Solicitante</h3>
                <div class="detalle-persona">
                  <span class="avatar">{{ inicialesSolicitanteDetalle }}</span>
                  <div><strong>{{ dato('solicitante', selectedRequest.solicitante || '—') }}</strong><small>{{ dato('cargoSolicitante', selectedRequest.cargoSolicitante) }}</small></div>
                </div>
                <p class="detalle-label">Unidad orgánica</p>
                <p class="detalle-valor">{{ dato('unidadOrganicaSolicitante', selectedRequest.area) }}</p>
              </section>

              <section class="detalle-card">
                <h3>Datos del Responsable</h3>
                <p class="detalle-label">Asignado por</p>
                <p class="detalle-valor">{{ dato('asignadoPor', '—') }}</p>
                <p class="detalle-label">Responsable</p>
                <p class="detalle-valor">{{ responsableDetalle }}</p>
                <p class="detalle-label">Cargo</p>
                <p class="detalle-valor">{{ dato('cargoResponsable', selectedRequest.cargoResponsable || '—') }}</p>
                <label class="detalle-check"><input type="checkbox" disabled :checked="!!detalleExtra?.indicadorAvance"> Indicador de Avance</label>
              </section>

              <section class="detalle-card">
                <h3>Datos Adicionales</h3>
                <p class="detalle-label">Ubicación actual</p>
                <p class="detalle-valor">{{ dato('ubicacionActual', selectedRequest.unidadOrganicaResponsable || '—') }}</p>
                <p class="detalle-label">Tipo de evaluación</p>
                <p class="detalle-valor">{{ dato('tipoEvaluacion', '—') }}</p>
                <p class="detalle-label">Tipo doc. ref.</p>
                <p class="detalle-valor">{{ dato('tipoDocumentoReferencia', '—') }}</p>
                <p class="detalle-label">Doc. referencia</p>
                <p class="detalle-valor">{{ dato('documentoReferencia', '—') }}</p>
                <p class="detalle-label">Fecha doc. ref.</p>
                <p class="detalle-valor">{{ dato('fechaDocumentoReferencia', '—') }}</p>
                <label class="detalle-check"><input type="checkbox" disabled :checked="!!detalleExtra?.documentoFisico"> Doc. Físico</label>
              </section>

              <section class="detalle-card">
                <h3>Observación</h3>
                <textarea v-model="observacionDetalle" class="form-input-block" rows="3" placeholder="Ingrese una observación..."></textarea>
              </section>
            </div>
          </div>
        </template>

        <!-- Tablero con subpestañas -->
        <template v-else>
          <div class="content-top">
            <p class="section-eyebrow">BIENVENIDA</p>
            <div class="greeting">
              <h1>Hola, {{ nombreUsuario }}</h1>
              <p>Gestiona y da seguimiento a todos los requerimientos del sistema.</p>
              <p v-if="cargando" class="conn-status">Cargando requerimientos…</p>
              <p v-else-if="!conectado" class="conn-status conn-status--offline">⚠ No se pudo conectar con el backend (service-requerimiento)<span v-if="mensajeError">: {{ mensajeError }}</span>. <button @click="refrescar">Reintentar</button></p>
            </div>

            <div class="toolbar">
              <button class="new-request" @click="abrirNuevoRequerimiento"><Icon name="plus" :size="15" /> Nuevo requerimiento</button>
              <label class="search" title="Número de requerimiento (Enter para buscar)">
                <Icon name="search" :size="16" />
                <input v-model="numeroBusqueda" inputmode="numeric" placeholder="Buscar N° de requerimiento..." @keyup.enter="buscarPorNumero">
              </label>
              <button :class="{ activo: soloAltaPrioridad }" @click="soloAltaPrioridad = !soloAltaPrioridad"><Icon name="star" :size="14" /> Alta prioridad</button>
              <button :class="{ activo: soloConfPendiente }" @click="soloConfPendiente = !soloConfPendiente"><Icon name="clock" :size="14" /> Conf. pendiente</button>
              <button disabled title="El backend aún no expone si el requerimiento requiere autorización"><Icon name="diamond" :size="14" /> Requiere autorización</button>
              <button :class="{ activo: filtrosActivos.length }" @click="abrirPanelBusqueda">
                <Icon name="filter" :size="14" /> Búsqueda Avanzada
                <b v-if="filtrosActivos.length" class="filtro-badge">{{ filtrosActivos.length }}</b>
              </button>
              <div class="view-toggle">
                <button :class="{ selected: vistaTarjetas === 'grid' }" @click="cambiarVistaTarjetas('grid')" aria-label="Vista en tablero"><Icon name="grid" :size="15" /></button>
                <button :class="{ selected: vistaTarjetas === 'list' }" @click="cambiarVistaTarjetas('list')" aria-label="Vista en lista"><Icon name="list" :size="15" /></button>
              </div>
            </div>
          </div>

          <!-- Resultado de la búsqueda: total + chips de filtros aplicados -->
          <div v-if="filtrosAplicados" class="filtros-chips-row">
            <span v-if="buscandoAvanzado" class="filtros-chips-count">Buscando…</span>
            <span v-else-if="errorBusquedaAvanzada" class="filtros-chips-count conn-status--offline">⚠ {{ errorBusquedaAvanzada }}</span>
            <span v-else class="filtros-chips-count">{{ totalResultados }} resultado{{ totalResultados === 1 ? '' : 's' }}</span>
            <span v-for="f in filtrosActivos" :key="f.clave" class="filtro-chip">
              {{ f.etiqueta }}
              <button type="button" @click="quitarFiltroActivo(f.clave)" aria-label="Quitar filtro"><Icon name="x" :size="10" /></button>
            </span>
            <button type="button" class="limpiar-chips" @click="limpiarFiltrosAvanzados">Limpiar</button>
          </div>

          <!-- Subpestañas -->
          <nav class="subtabs">
            <button
              v-for="t in PESTANAS"
              :key="t.id"
              :class="{ activo: pestana === t.id }"
              @click="cambiarPestana(t.id)"
            >
              {{ t.label }}
              <b v-if="t.id !== 'otros' || filtrosAplicados" class="subtab-count">{{ conteos[t.id] }}</b>
            </button>
          </nav>

          <div class="tab-body">
            <!-- Otros requerimientos: solo muestra lo que encuentra la búsqueda -->
            <div v-if="pestana === 'otros' && !filtrosAplicados" class="empty-state">
              <Icon name="search" :size="40" />
              <strong>Busca un requerimiento</strong>
              <p>Ingresa el N° de requerimiento o usa la Búsqueda Avanzada para encontrar cualquier registro del sistema.</p>
            </div>

            <template v-else>
              <section class="metrics">
                <article
                  v-for="metric in tablero.metrics"
                  :key="metric.title"
                  :class="[metric.tone, { selected: metricaSeleccionada === metric.title }]"
                  @click="alternarMetrica(metric.title)"
                >
                  <i><Icon :name="metric.icon" :size="20" /></i>
                  <div><span>{{ metric.title }}</span><strong>{{ metric.total }}</strong></div>
                </article>
              </section>
              <p v-if="metricaSeleccionada" class="metrics-filter-hint">Mostrando solo "{{ metricaSeleccionada }}". <button class="volver-pill" @click="alternarMetrica(metricaSeleccionada)"><Icon name="x" :size="12" /> Ver todo</button></p>

              <!-- Vista tablero: columnas con 10 tarjetas por página -->
              <section v-if="vistaTarjetas === 'grid'" class="board">
                <article v-for="column in columnasVisibles" :key="column.title" class="board-column">
                  <header :class="column.tone"><span class="dot"></span><strong>{{ column.title }}</strong><b>{{ column.total }}</b></header>
                  <div class="cards">
                    <article
                      v-for="card in tarjetasPagina(column)"
                      :key="card.codigo"
                      class="request-card"
                      @click="abrirDetalle(card, column)"
                    >
                      <div class="card-body">
                        <div class="card-top">
                          <span class="card-numero">{{ card.numero }}</span>
                          <span class="priority-badge" :class="clasePrioridad(card.prioridad)">
                            <Icon v-if="iconoPrioridad(card.prioridad)" :name="iconoPrioridad(card.prioridad)" :size="10" />
                            {{ card.prioridad }}
                          </span>
                        </div>
                        <p class="card-area">{{ card.area }}</p>
                        <p v-if="card.categoria" class="card-categoria">{{ card.categoria }}<template v-if="card.subcategoria"> · {{ card.subcategoria }}</template></p>
                        <div class="tags"><span v-for="e in etiquetasTarjeta(card)" :key="e.texto" :class="e.tono">{{ e.texto }}</span></div>
                        <div class="card-footer">
                          <span class="card-date"><Icon name="calendar" :size="12" />{{ card.fecha }}</span>
                          <b v-if="card.inicialesResponsable" :title="card.responsable">{{ card.inicialesResponsable }}</b>
                        </div>
                      </div>
                      <button
                        v-if="accionTarjeta(card, column.title)"
                        class="card-action"
                        :class="accionTarjeta(card, column.title)"
                        @click.stop="ejecutarAccionTarjeta(card, column.title)"
                      >
                        <template v-if="accionTarjeta(card, column.title) === 'conformidad'"><Icon name="check" :size="14" /> Dar conformidad</template>
                        <template v-else><Icon name="user" :size="14" /> Asignar</template>
                      </button>
                    </article>
                    <p v-if="!column.cards.length" class="empty-column">Sin requerimientos</p>
                    <Paginador
                      v-if="column.total > TAM_PAGINA_TABLERO"
                      :pagina="paginaColumna(column)"
                      :total="column.total"
                      :tamano="TAM_PAGINA_TABLERO"
                      @cambiar="(n) => cambiarPaginaColumna(column, n)"
                    />
                  </div>
                </article>
              </section>

              <!-- Vista lista: tabla del Figma con 15 filas por página -->
              <section v-else class="tabla-card">
                <div class="tabla-scroll">
                  <table class="req-tabla">
                    <thead>
                      <tr><th>N° Req.</th><th>Categoría</th><th>Unidad</th><th>Grupo</th><th>Estado</th><th>Prioridad</th><th>Fecha</th><th>Acción</th></tr>
                    </thead>
                    <tbody>
                      <tr v-for="fila in filasPagina" :key="fila.codigo" @click="abrirDetalle(fila)">
                        <td class="td-numero">{{ fila.numero }}</td>
                        <td class="td-categoria"><strong>{{ fila.categoria || 'Sin clasificar' }}</strong><small v-if="fila.subcategoria">{{ fila.subcategoria }}</small></td>
                        <td class="td-unidad" :title="fila.area">{{ fila.area }}</td>
                        <td><span class="grupo-pill" :class="fila.tonoGrupo">{{ fila.grupo }}</span></td>
                        <td><span class="estado-pill" :class="pestana === 'noClasificados' ? 'gris' : tonoEstado(fila.codigoEstado)">{{ pestana === 'noClasificados' ? 'Por clasificar' : (fila.estado || '-') }}</span></td>
                        <td>
                          <span class="priority-badge" :class="clasePrioridad(fila.prioridad)">
                            <Icon v-if="iconoPrioridad(fila.prioridad)" :name="iconoPrioridad(fila.prioridad)" :size="10" />
                            {{ fila.prioridad }}
                          </span>
                        </td>
                        <td class="td-fecha">{{ fila.fecha }}</td>
                        <td>
                          <button v-if="accionFila(fila)" class="tabla-accion" :class="accionFila(fila)" @click.stop="ejecutarAccionTarjeta(fila)">
                            <template v-if="accionFila(fila) === 'conformidad'"><Icon name="check" :size="13" /> Dar conformidad</template>
                            <template v-else-if="accionFila(fila) === 'asignar'"><Icon name="user" :size="13" /> Asignar</template>
                            <template v-else><Icon name="file-text" :size="13" /> Adjuntar archivo</template>
                          </button>
                        </td>
                      </tr>
                      <tr v-if="!filasVisibles.length"><td colspan="8" class="td-vacio">Sin requerimientos</td></tr>
                    </tbody>
                  </table>
                </div>
                <Paginador
                  v-if="filasVisibles.length"
                  :pagina="paginaListaActual"
                  :total="filasVisibles.length"
                  :tamano="TAM_PAGINA_LISTA"
                  @cambiar="(n) => (paginaLista = n)"
                />
              </section>
            </template>
          </div>
        </template>
      </template>

      <template v-else-if="vista === 'mantenimiento'">
        <p class="section-eyebrow">{{ (perfilActivo?.nombreRol || 'Usuario').toUpperCase() }} · MANTENIMIENTOS</p>
        <div class="greeting"><h1>Mantenimientos</h1><p>Catálogos maestros del sistema (Personas, Categorías, Problemas, Soluciones y sus relaciones).</p></div>
        <div class="toolbar mantenimiento-toolbar">
          <button v-for="opcion in opcionesMantenimientoActivas" :key="opcion.id" :class="{ selected: mantenimientoActivo === opcion.id }" @click="irAMantenimientos(opcion.id)">{{ opcion.label }}</button>
        </div>
        <section class="mantenimiento-panel">
          <template v-for="opcion in opcionesMantenimientoActivas" :key="opcion.id">
            <div v-if="mantenimientoActivo === opcion.id">
              <h2>{{ opcion.label }}</h2>
              <div class="toolbar">
                <button class="new-request"><Icon name="plus" :size="15" /> Nuevo</button>
                <label class="search"><Icon name="search" :size="16" /><input placeholder="Buscar..."></label>
                <button><Icon name="printer" :size="14" /> Imprimir</button>
              </div>
              <p v-if="opcion.backend" class="empty-column">Listado de "{{ opcion.label }}" — pendiente de conectar a los endpoints ya existentes en service-requerimiento.</p>
              <p v-else class="empty-column">"{{ opcion.label }}" tiene SP en el proyecto pero aún no tiene endpoint en service-requerimiento — falta en el backend antes de poder listarlo aquí.</p>
            </div>
          </template>
        </section>
      </template>

      <template v-else-if="vista === 'documentacion'">
        <p class="section-eyebrow">{{ (perfilActivo?.nombreRol || 'Usuario').toUpperCase() }} · DOCUMENTACIÓN</p>
        <div class="greeting"><h1>Documentación</h1><p>Procedimientos y manuales del Sistema de Gestión de Requerimientos.</p></div>
        <section class="mantenimiento-panel">
          <p class="empty-column">Aquí irán los documentos de referencia (Procedimiento del SGR, Manual de usuario). </p>
        </section>
      </template>
    </section>

    <!-- Panel de detalle -->
    <aside v-if="selectedRequest" class="detail-panel">
      <div class="detail-panel-head">
        <div>
          <button class="detail-back" @click="cerrarDetalle"><Icon name="arrow-left" :size="14" /> Volver al tablero</button>
          <div class="detail-title-row">
            <b>{{ selectedRequest[0] }}</b>
            <span class="status-pill" :style="{ color: 'var(--blue)', background: '#eaf0ff' }">{{ selectedColumn?.title }}</span>
            <span class="priority-badge" :class="selectedRequest[4].toLowerCase()">{{ selectedRequest[4] }}</span>
          </div>
          <h2 class="detail-subtitle">{{ selectedRequest[1] }}</h2>
        </div>
        <button class="close-detail" aria-label="Cerrar detalle" @click="cerrarDetalle"><Icon name="x" :size="18" /></button>
      </div>

      <div class="detail-actions">
        <button class="move"><Icon name="move" :size="14" /> Mover de estado</button>
        <button v-if="puedeAsignar" class="asignar" @click="abrirModalAsignar"><Icon name="user-plus" :size="14" /> Asignar</button>
        <!-- Botón dividido de autorizaciones: la zona principal abre directamente
             "Solicitar autorización"; la flecha despliega Solicitar / Consultar / Anular
             (todas abren el mismo modal en distinto modo). -->
        <div ref="menuAutorizacionesRef" class="menu-autorizaciones" @keydown="alTeclaMenuAutorizaciones">
          <div class="menu-autorizaciones-grupo">
            <button type="button" class="menu-autorizaciones-principal" @click="elegirAccionAutorizacion('solicitar')">
              <Icon name="shield" :size="14" /> Solicitar autorización
            </button>
            <button
              ref="botonAutorizacionesRef"
              type="button"
              class="menu-autorizaciones-toggle"
              aria-label="Más acciones de autorización"
              aria-haspopup="menu"
              :aria-expanded="menuAutorizacionesAbierto"
              @click="alternarMenuAutorizaciones"
            >
              <Icon :name="menuAutorizacionesAbierto ? 'chevron-up' : 'chevron-down'" :size="13" />
            </button>
          </div>
          <div v-if="menuAutorizacionesAbierto" class="menu-autorizaciones-lista" role="menu" aria-label="Autorizaciones">
            <button type="button" role="menuitem" @click="elegirAccionAutorizacion('solicitar')">
              <Icon name="shield" :size="14" /> Solicitar autorización
            </button>
            <div class="menu-autorizaciones-separador" role="separator"></div>
            <button type="button" role="menuitem" class="menu-autorizaciones-anular" @click="elegirAccionAutorizacion('anular')">
              <Icon name="x" :size="14" /> Anular autorizaciones…
            </button>
          </div>
        </div>
        <template v-if="puedeDarConformidad">
          <button class="conformidad-si"><Icon name="check" :size="14" /> Dar conformidad</button>
          <button class="conformidad-no"><Icon name="x" :size="14" /> Dar no conformidad</button>
        </template>
        <button><Icon name="history" :size="14" /> Ver historial</button>
      </div>

      <div class="detail-body">
        <p v-if="mensajeAutorizacion" class="conn-status">✓ {{ mensajeAutorizacion }}</p>
        <div class="detail-card">
          <h3>Datos del Requerimiento</h3>
          <dl class="detail-grid">
            <div><dt>N° Requerimiento</dt><dd>{{ selectedRequest[0] }}</dd></div>
            <div><dt>Fecha</dt><dd>{{ selectedRequest[3] }}</dd></div>
            <div><dt>Estado actual</dt><dd class="orange">{{ selectedColumn?.title }}</dd></div>
            <div><dt>Prioridad</dt><dd>{{ selectedRequest[4] }}</dd></div>
            <div style="grid-column:1/-1"><dt>Área solicitante</dt><dd>{{ selectedRequest[2] }}</dd></div>
          </dl>
        </div>
      </div>
    </aside>

    <!-- Panel "Búsqueda Avanzada" (Cambio 19) -->
    <div v-if="mostrarPanelBusqueda" class="modal-overlay" @click.self="cerrarPanelBusqueda">
      <aside class="filtros-panel">
        <div class="filtros-panel-head">
          <div><p class="section-eyebrow" style="margin:0">FILTROS</p><h2>Búsqueda Avanzada</h2></div>
          <button class="close-detail" aria-label="Cerrar" @click="cerrarPanelBusqueda"><Icon name="x" :size="18" /></button>
        </div>
        <div class="filtros-panel-body">
          <div class="form-field"><label>SOLICITANTE</label><input v-model="solicitanteFiltro" placeholder="Buscar usuario..."></div>
          <div class="form-field"><label>RESPONSABLE</label><input v-model="responsableFiltro" placeholder="Buscar usuario..."></div>
          <div class="form-field"><label>N° DE REQUERIMIENTO</label><input v-model="numeroBusqueda" inputmode="numeric" placeholder="Ej: 287273"></div>
          <div class="form-field"><label>ROL</label><select v-model="rolFiltro" disabled title="Pendiente: el backend no devuelve el rol"><option value="">— Todos —</option></select></div>
          <div class="form-field"><label>PRIORIDAD</label><select v-model="prioridadFiltro"><option value="">— Todos —</option><option v-for="p in opcionesPrioridad" :key="p" :value="p">{{ p }}</option></select></div>
          <div class="form-field"><label>ESTADO</label><select v-model="estadoFiltro"><option value="">— Todos —</option><option v-for="op in OPCIONES_ESTADO_BUSQUEDA" :key="op.value" :value="op.value">{{ op.label }}</option></select></div>
          <div class="form-field"><label>SUBESTADO</label><select v-model="subestadoFiltro" disabled title="Pendiente: el backend no devuelve el subestado"><option value="">— Todos —</option></select></div>
          <div class="form-field"><label>CATEGORÍA</label><select v-model="categoriaFiltro"><option value="">— Todos —</option><option v-for="c in categorias" :key="c.codigoCategoria" :value="c.codigoCategoria">{{ c.nombreCategoria }}</option></select></div>
          <div class="form-field"><label>SUBCATEGORÍA</label><select v-model="subcategoriaFiltro" :disabled="!categoriaFiltro"><option value="">— Todos —</option><option v-for="s in subcategorias" :key="s.codigoSubcategoria" :value="s.codigoSubcategoria">{{ s.nombreSubcategoria }}</option></select></div>
          <div class="modal-grid-2">
            <div class="form-field"><label>FECHA DESDE</label><input type="date" v-model="fechaInicioBusqueda"></div>
            <div class="form-field"><label>FECHA HASTA</label><input type="date" v-model="fechaFinBusqueda"></div>
          </div>
          <p class="form-hint">Por defecto busca lo registrado hoy. Si ingresas un N° de requerimiento, se busca en cualquier fecha. Rol y Subestado quedan pendientes (el backend no devuelve esos datos).</p>
        </div>
        <div class="filtros-panel-actions">
          <button class="modal-btn-secundario" @click="limpiarFiltrosAvanzados">Limpiar todo</button>
          <button class="modal-btn-primario" :disabled="buscandoAvanzado" @click="aplicarFiltrosAvanzados">Aplicar filtros</button>
        </div>
      </aside>
    </div>

    <!-- Modal Asignar Requerimiento: simplifica la pantalla legacy omitiendo Descripción y Apellido Paterno -->
    <div v-if="mostrarModalAsignar" class="modal-overlay" @click.self="cerrarModalAsignar">
      <div class="modal-box modal-box--ancho">
        <button class="close-detail modal-close" aria-label="Cerrar" @click="cerrarModalAsignar"><Icon name="x" :size="18" /></button>
        <h2>Asignar requerimiento</h2>

        <div class="modal-grid-2">
          <div class="form-field">
            <label>Requerimiento Nº</label>
            <input :value="selectedRequest?.numero" disabled>
          </div>
          <div class="form-field">
            <label>Estado</label>
            <input :value="selectedRequest?.estado || selectedColumn?.title" disabled>
          </div>
        </div>

        <div class="form-field">
          <label>Unidad Orgánica</label>
          <div class="autocomplete">
             <div
              v-if="unidadOrganicaSeleccionada"
              class="campo-confirmado"
              @click="cambiarUnidadOrganica"
            >
              <span>{{ unidadOrganicaTexto }}</span>
              <small>✎ cambiar</small>
            </div>
            <input
              v-else
              v-model="unidadOrganicaTexto"
              placeholder="Escribe al menos 3 letras para buscar..."
              autocomplete="off"
            >
            <ul v-if="unidadesOrganicas.length" class="autocomplete-list">
              <li
                v-for="uo in unidadesOrganicas"
                :key="uo.codigoUo"
                @click="seleccionarUnidadOrganica(uo)"
              >{{ uo.nombreUnidadOrganica }}</li>
            </ul>
            <p v-if="buscandoUnidadOrganica" class="form-hint">Buscando...</p>
          </div>
        </div>

        <div class="form-field">
          <label>Personal</label>
          <div class="autocomplete">
            <div
              v-if="personaSeleccionada"
              class="campo-confirmado"
              @click="cambiarPersona"
            >
              <span>{{ personaTexto }}</span>
              <small>✎ cambiar</small>
            </div>
            <input
              v-else
              v-model="personaTexto"
              :placeholder="!unidadOrganicaSeleccionada ? 'Elige primero una Unidad Orgánica' : (cargandoPersonas ? 'Cargando...' : 'Escribe el nombre de la persona...')"
              autocomplete="off"
              :disabled="!unidadOrganicaSeleccionada || cargandoPersonas"
            >
            <ul v-if="mostrarListaPersonas && personasFiltradas.length" class="autocomplete-list">
              <li
                v-for="persona in personasFiltradas"
                :key="persona.codigoPer"
                @click="seleccionarPersona(persona)"
              >{{ persona.nombre }}</li>
            </ul>
          </div>
          <p v-if="unidadOrganicaSeleccionada && !cargandoPersonas && !personas.length" class="form-hint">
            Esta unidad orgánica no tiene personas vigentes registradas.
          </p>
        </div>

        <div v-if="personaSeleccionada" class="form-field form-field-cargo">
          <label>Cargo</label>
          <div class="campo-confirmado campo-confirmado--solo-lectura">
            <span>{{ personaSeleccionada.cargo || 'Sin cargo' }}</span>
          </div>
        </div>

        <label class="form-checkbox">
          <input type="checkbox" v-model="responsablePrincipal">
          Responsable Principal
        </label>

        <div class="form-field">
          <label>Tipo de observación</label>
          <div class="form-radios">
            <label><input type="radio" value="motivo" v-model="tipoObservacion"> Descripción / Motivo</label>
            <label><input type="radio" value="informeTecnico" v-model="tipoObservacion"> Informe Técnico</label>
          </div>
        </div>

        <div class="form-field">
          <label>{{ tipoObservacion === 'informeTecnico' ? 'Informe Técnico' : 'Motivo' }}</label>
          <textarea
            v-model="observacionAsignar"
            :maxlength="LARGO_MAXIMO_OBSERVACION"
            rows="4"
            placeholder="Escribe aquí..."
          ></textarea>
          <p class="form-hint">{{ observacionAsignar.length }}/{{ LARGO_MAXIMO_OBSERVACION }} caracteres</p>
        </div>

        <p v-if="errorAsignacion" class="conn-status conn-status--offline">⚠ {{ errorAsignacion }}</p>

        <div class="modal-actions">
          <button class="modal-btn-secundario" @click="cerrarModalAsignar">Retornar</button>
          <button
            class="modal-btn-primario"
            :disabled="!puedeConfirmarAsignacion"
            @click="pedirConfirmacionAsignar"
          >Asignar</button>
        </div>
      </div>
    </div>

    <!-- Modal Solicitar autorización -->
    <SolicitarAutorizacionModal
      :visible="mostrarModalAutorizacion"
      :modo="modoAutorizacion"
      :perfil="perfil"
      :requerimiento="selectedRequest"
      :estado="selectedColumn?.title"
      :codigo-requerimiento="codigoRequerimientoSeleccionado()"
      :unidad-organica="unidadOrganicaPropia"
      @cerrar="cerrarModalAutorizacion"
      @solicitud-preparada="alPrepararSolicitudAutorizacion"
    />

    <!-- Modal de confirmación. -->
    <div v-if="mostrarConfirmacionAsignar" class="modal-overlay">
      <div class="modal-box modal-box--chico">
        <h2>Confirmar asignación</h2>
        <p>¿Está seguro de asignar el requerimiento?</p>
        <div class="modal-actions">
          <button class="modal-btn-secundario" :disabled="asignando" @click="cancelarConfirmacionAsignar">Cancelar</button>
          <button class="modal-btn-primario" :disabled="asignando" @click="confirmarAsignacion">
            {{ asignando ? 'Asignando...' : 'Aceptar' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>
