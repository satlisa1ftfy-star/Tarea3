<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ESTADO, buscarAvanzado, cargarPendientes, cargarTablero } from '../models/dashboard'
import { listarRoles } from '../services/seguridadService'
import { listarPersonas } from '../services/personaService'
import { buscarUnidadOrganica } from '../services/unidadOrganicaService'
import { asignarRequerimiento } from '../services/requerimientoService'
import Icon from '../components/Icon.vue'
import NuevoRequerimientoForm from '../components/dashboard/NuevoRequerimientoForm.vue'
import ClasificarRequerimientoModal from '../components/dashboard/ClasificarRequerimientoModal.vue'

const props = defineProps({
  perfil: { type: Object, default: null },
})
const emit = defineEmits(['logout'])

// vista: 'requerimientos' = Mis requerimientos, Mis pendientes 
const vista = ref('requerimientos')
const activeMenu = ref('Tablero')
const selectedRequest = ref(null)
const selectedColumn = ref(null)

// Trae los datos del backend.
const metrics = ref([])
const columns = ref([])
const cargando = ref(true)
const conectado = ref(false)
const mensajeError = ref('')

// Tarjetas de métricas: al seleccionar una, el tablero solo muestra la
// columna correspondiente (según el Figma). Vuelve a mostrar todo si se
// hace clic en la misma métrica de nuevo.
const metricaSeleccionada = ref(null)
function alternarMetrica(titulo) {
  metricaSeleccionada.value =
    titulo === 'Total' || metricaSeleccionada.value === titulo ? null : titulo
}
const columnasVisibles = computed(() =>
  metricaSeleccionada.value
    ? columns.value.filter((c) => c.title === metricaSeleccionada.value)
    : columns.value
)

// Vista del tablero: 'grid'  o 'list'.
const vistaTarjetas = ref('grid')
function cambiarVistaTarjetas(valor) {
  vistaTarjetas.value = valor
}

// Colapsar/expandir el menú lateral.
const sidebarColapsado = ref(false)
function alternarSidebar() {
  sidebarColapsado.value = !sidebarColapsado.value
}

// Menú de perfil (roles del usuario + cerrar sesión)
const menuAbierto = ref(false)
const roles = ref([])
const perfilActivo = ref(null) // rol seleccionado en el desplegable

// Búsqueda avanzada
function hoyISO() {
  return new Date().toISOString().slice(0, 10) // yyyy-MM-dd, formato que espera el backend
}

const numeroBusqueda = ref('')
// Por defecto busca lo de hoy para evitar el timeout (15s):
// El filtro por unidad en el SP recorre toda la tabla sin índice (Aun por evaluar logica)  ***********PENDIENTE***********
// calculando el responsable fila por fila (~12s con 5k filas) (Desarrollo)
const fechaInicioBusqueda = ref(hoyISO())
const fechaFinBusqueda = ref(hoyISO())
const resultadosBusqueda = ref(null)
const buscandoAvanzado = ref(false)
const errorBusquedaAvanzada = ref('')

// Panel "Búsqueda Avanzada" 
// Los únicos filtros que el backend real soporta hoy son número/título (ya cubierto por el buscador de arriba), 
// unidad orgánica del responsable (se infiere del rol activo),
// estado y fechas — el resto (Solicitante, Responsable, Rol, Prioridad,
// Subestado, Categoría, Subcategoría) se muestra tal como en el Figma pero
// todavía no se envía al backend porque                                                  ***********PENDIENTE***********`
const mostrarPanelBusqueda = ref(false)
const solicitanteFiltro = ref('')
const responsableFiltro = ref('')
const rolFiltro = ref('')
const prioridadFiltro = ref('')
const estadoFiltro = ref('')
const subestadoFiltro = ref('')
const categoriaFiltro = ref('')
const subcategoriaFiltro = ref('')

const OPCIONES_ESTADO_BUSQUEDA = [
  { value: ESTADO.REGISTRADO, label: 'Registrado' },
  { value: ESTADO.POR_ASIGNAR, label: 'Por Asignar' },
  { value: ESTADO.ASIGNADO, label: 'Asignado' },
  { value: ESTADO.EN_PROCESO, label: 'En Proceso' },
  { value: ESTADO.ATENDIDO, label: 'Atendido' },
  { value: ESTADO.CERRADO_CONFORME, label: 'Cerrado Conforme' },
  { value: ESTADO.CERRADO_RECHAZADO, label: 'Cerrado Rechazado' },
  { value: ESTADO.CERRADO_ANULADO, label: 'Cerrado Anulado' },
  { value: ESTADO.NO_CONFORME, label: 'No Conforme' },
]

function abrirPanelBusqueda() {
  mostrarPanelBusqueda.value = true
}
function cerrarPanelBusqueda() {
  mostrarPanelBusqueda.value = false
}
function limpiarFiltrosAvanzados() {
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
async function aplicarFiltrosAvanzados() {
  await ejecutarBusquedaAvanzada()
  mostrarPanelBusqueda.value = false
}

// Filtros activos (badge/chips): Solo incluye Estado y Fechas (únicos campos enviados al backend).
const filtrosActivos = computed(() => {
  const activos = []
  if (estadoFiltro.value) {
    const opcion = OPCIONES_ESTADO_BUSQUEDA.find((o) => String(o.value) === String(estadoFiltro.value))
    activos.push({ clave: 'estado', etiqueta: `Estado: ${opcion?.label || estadoFiltro.value}` })
  }
  // La fecha solo se envía al backend cuando no hay número de requerimiento
  // (Cambio 11); si hay número, mostrar el chip sería engañoso.
  const fechaCambiada = fechaInicioBusqueda.value !== hoyISO() || fechaFinBusqueda.value !== hoyISO()
  if (fechaCambiada && !(Number(numeroBusqueda.value) || 0)) {
    activos.push({ clave: 'fecha', etiqueta: `Fecha: ${fechaInicioBusqueda.value} a ${fechaFinBusqueda.value}` })
  }
  return activos
})

function quitarFiltroActivo(clave) {
  if (clave === 'estado') estadoFiltro.value = ''
  if (clave === 'fecha') {
    fechaInicioBusqueda.value = hoyISO()
    fechaFinBusqueda.value = hoyISO()
  }
  if (resultadosBusqueda.value !== null) ejecutarBusquedaAvanzada()
}

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
  return (palabras[0]?.[0] || '') + (palabras[1]?.[0] || '')
})

// Fecha de hoy para la barra de encabezado (formato dd/mm/aaaa, igual al Figma).
const fechaHoy = computed(() => new Date().toLocaleDateString('es-PE'))


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

// Etiqueta de la sección activa para el "eyebrow" sobre el título.
const seccionActivaLabel = computed(() => {
  if (vista.value === 'pendientes') return 'Pendientes'
  if (vista.value === 'requerimientos') return 'Mis requerimientos'
  if (vista.value === 'mantenimiento') return 'Mantenimientos'
  if (vista.value === 'documentacion') return 'Documentación'
  return ''
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
    }
  } catch (error) {
    console.error('No se pudieron cargar los roles del usuario.', error)
    roles.value = []
  }
}

// Cambia el rol activo desde el desplegable de perfil.
function seleccionarRol(rol) {
  perfilActivo.value = rol
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

function alternarMenuPerfil() {
  menuAbierto.value = !menuAbierto.value
}

function cerrarSesion() {
  menuAbierto.value = false
  emit('logout')
}

function irARequerimientos(menu) {
  vista.value = 'requerimientos'
  activeMenu.value = menu
  mostrarNuevoRequerimiento.value = false
}

function irAPendientes() {
  vista.value = 'pendientes'
  mostrarNuevoRequerimiento.value = false
}

function seleccionarRequerimiento(card, column) {
  selectedRequest.value = card
  selectedColumn.value = column
}

function cerrarDetalle() {
  selectedRequest.value = null
  selectedColumn.value = null
}

// La conformidad se evalúa sobre requerimientos Atendidos
// se solicita automáticamente por correo al llegar a Atendido.
const puedeDarConformidad = computed(() => {
  const titulo = selectedColumn.value?.title
  return titulo === 'Atendido' || titulo === 'Por dar conformidad'
})

// -------------------------------------------------------------------------
// Registro de Requerimiento: la lógica vive en NuevoRequerimientoForm
// (composable useRegistroRequerimiento). Aquí solo se abre/cierra y se refresca el tablero.
const mostrarNuevoRequerimiento = ref(false)

function abrirNuevoRequerimiento() {
  resultadosBusqueda.value = null
  mostrarNuevoRequerimiento.value = true
}

function cerrarNuevoRequerimiento() {
  mostrarNuevoRequerimiento.value = false
  recargarListaOperador()
}

async function alRegistrarRequerimiento() {
  await refrescar() // el nuevo requerimiento aparece en el tablero al volver
}

// El Operador en «Mis pendientes» ve su lista automática (sin clasificar); abrir el formulario la
// descarta, así que se vuelve a cargar al salir de él.
function recargarListaOperador() {
  if (vista.value === 'pendientes' && modoBusqueda.value === 'automatico' && resultadosBusqueda.value === null) {
    ejecutarBusquedaAvanzada()
  }
}

// -------------------------------------------------------------------------
// Clasificar requerimiento (Operador): modal sobre PUT /api/requerimiento/{id}/clasificacion.
// Solo para requerimientos en estado Registrado (card[5] = codigoEstado).
const mostrarModalClasificar = ref(false)
const puedeClasificar = computed(() =>
  vista.value === 'pendientes' &&
  perfilActivo.value?.nombreRol === 'Operador' &&
  selectedRequest.value?.[5] === ESTADO.REGISTRADO
)

async function alClasificarRequerimiento() {
  mostrarModalClasificar.value = false
  cerrarDetalle()
  // Clasificar no cambia el estado: se vuelve a consultar para reflejar la nueva prioridad/categoría.
  if (resultadosBusqueda.value !== null) await ejecutarBusquedaAvanzada()
  else await refrescar()
}

// -------------------------------------------------------------------------
// Asignar requerimiento: modal + confirmación para el endpoint
// POST /api/requerimiento/asignar, visible solo en "Pendientes"

const COLUMNAS_PENDIENTES_ASIGNABLES = ['Por asignar', 'Por atender', 'Por autorizar', 'No conforme']
const puedeAsignar = computed(() =>
  vista.value === 'pendientes' && COLUMNAS_PENDIENTES_ASIGNABLES.includes(selectedColumn.value?.title)
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
  const numero = selectedRequest.value?.[0]
  if (!numero) return null
  const soloDigitos = String(numero).replace(/\D/g, '')
  return soloDigitos ? Number(soloDigitos) : null
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

// Toolbar de Mantenimientos: Supervisor ve el catálogo completo;
// Administrador ve solo un subconjunto (Problema/Solución). Backend activo en 4 opciones.
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
  { id: 'problemas', label: 'Problemas', backend: false },
  { id: 'soluciones', label: 'Soluciones', backend: false },
  { id: 'problemaSolucion', label: 'Relación Problema - Solución', backend: false },
]
const opcionesMantenimientoActivas = computed(() =>
  perfilActivo.value?.nombreRol === 'Administrador'
    ? OPCIONES_MANTENIMIENTO_ADMINISTRADOR
    : OPCIONES_MANTENIMIENTO_SUPERVISOR
)
const mantenimientoActivo = ref(OPCIONES_MANTENIMIENTO_SUPERVISOR[0].id)

function irAMantenimientos(id) {
  vista.value = 'mantenimiento'
  mantenimientoActivo.value = id || opcionesMantenimientoActivas.value[0]?.id
  mostrarNuevoRequerimiento.value = false
}

function irADocumentacion() {
  vista.value = 'documentacion'
  mostrarNuevoRequerimiento.value = false
}

async function ejecutarBusquedaAvanzada() {
  buscandoAvanzado.value = true
  const numero = Number(numeroBusqueda.value) || 0
  const filtro = {
    codigoUoResponsable: unidadOrganicaBusqueda.value,
    numero,
  }
  if (modoBusqueda.value === 'automatico') {
    filtro.codigoEstado = ESTADO.REGISTRADO                       // no clasificados todavía
    // Un requerimiento recién registrado aún no tiene responsable: al buscar por número no se filtra por UO.
    if (numero > 0) filtro.codigoUoResponsable = 0
  } else {
    // Filtro manual (panel "Búsqueda Avanzada"): usa el estado elegido si hay uno.
    if (estadoFiltro.value) {
      filtro.codigoEstado = Number(estadoFiltro.value)
    }
    if (numero === 0) {
      // Filtra por 'hoy' si no hay número; si ingresa número específico, se ignora la fecha.
      filtro.fechaInicio = fechaInicioBusqueda.value || 'TODO'
      filtro.fechaFin = fechaFinBusqueda.value || 'TODO'
    }
  }
  const resultado = await buscarAvanzado(filtro)
  resultadosBusqueda.value = resultado.cards
  errorBusquedaAvanzada.value = resultado.online ? '' : (resultado.errorMessage || 'No se pudo buscar.')
  buscandoAvanzado.value = false
}

function cerrarBusquedaAvanzada() {
  resultadosBusqueda.value = null
  numeroBusqueda.value = ''
  fechaInicioBusqueda.value = hoyISO()
  fechaFinBusqueda.value = hoyISO()
}

// El Operador no usa la lupa: en cuanto se conocen sus roles y está en
// 'Mis pendientes', se carga solo.
watch([roles, vista], () => {
  if (vista.value === 'pendientes' && modoBusqueda.value === 'automatico' && resultadosBusqueda.value === null) {
    ejecutarBusquedaAvanzada()
  }
})

async function refrescar() {
  cargando.value = true
  const tablero = vista.value === 'pendientes'
    ? await cargarPendientes(props.perfil)
    : await cargarTablero(props.perfil)
  metrics.value = tablero.metrics
  columns.value = tablero.columns
  conectado.value = tablero.online
  mensajeError.value = tablero.errorMessage || ''
  metricaSeleccionada.value = null // los datos cambiaron; no arrastrar el filtro anterior
  cargando.value = false
}

// Limpia la búsqueda avanzada al cambiar de vista para evitar arrastrar filtros entre secciones.
watch(vista, () => {
  resultadosBusqueda.value = null
  numeroBusqueda.value = ''
  metricaSeleccionada.value = null
  refrescar()
})

onMounted(() => {
  refrescar()
  cargarRolesUsuario()
})
</script>

<template>
  <main class="dashboard" :class="{ 'dashboard--detail-open': selectedRequest, 'sidebar-collapsed': sidebarColapsado }">

    <!-- Header: marca SAT + fecha + notificaciones + perfil con rol activo -->
    <header class="dashboard-header">
      <div class="sat-brand"><strong>SAT</strong><span>Servicio de<br>Administracion<br>Tributaria de Lima</span></div>
      <h2 class="header-title">Gestión de Requerimientos</h2>
      <div class="header-actions">
        <span class="header-date"><Icon name="calendar" :size="15" />{{ fechaHoy }}</span>
        <button class="bell" aria-label="Notificaciones"><Icon name="bell" :size="17" /><b>1</b></button>
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
            <div class="role-menu-header"><strong>{{ nombreUsuario }}</strong><small>{{ perfil?.usuarioWindows }}</small></div>
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

    <!-- Barra de contexto: Unidad Orgánica del rol activo + acceso directo a cerrar sesión -->
    <div class="context-bar">
      <Icon name="building" :size="14" />
      <span v-if="unidadOrganicaPerfil">Unidad Orgánica: <strong>{{ unidadOrganicaPerfil }}</strong></span>
      <span v-else class="context-bar-muted">Unidad Orgánica: <strong>No disponible</strong></span>
      <span class="context-sep"></span>
      <button class="logout-link" @click="cerrarSesion"><Icon name="log-out" :size="13" /> Cerrar Sesión</button>
    </div>

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: sidebarColapsado }">
      <nav>
        <template v-if="menuSupervisor">
          <p class="nav-section-label">NAVEGACIÓN PRINCIPAL</p>
          <button class="nav-button" :class="{ selected: vista === 'mantenimiento' }" @click="irAMantenimientos()" title="Mantenimientos"><Icon name="wrench" :size="17" /> <span>Mantenimientos</span></button>
          <button class="nav-button" :class="{ selected: vista === 'documentacion' }" @click="irADocumentacion" title="Documentación"><Icon name="file-text" :size="17" /> <span>Documentación</span></button>
          <button class="nav-button" title="Ayuda"><Icon name="help-circle" :size="17" /> <span>Ayuda</span></button>
        </template>
        <template v-else>
          <p class="nav-section-label">NAVEGACIÓN PRINCIPAL</p>
          <button class="nav-button" :class="{ selected: vista === 'pendientes' }" @click="irAPendientes" title="Mis pendientes">
            <Icon name="clock" :size="17" /> <span>Mis pendientes</span>
            <b v-if="vista === 'pendientes'" class="nav-badge">{{ columns.reduce((a, c) => a + c.total, 0) }}</b>
          </button>
          <button class="nav-button" :class="{ selected: vista === 'requerimientos' }" @click="irARequerimientos('Tablero')" title="Mis requerimientos">
            <Icon name="folder" :size="17" /> <span>Mis requerimientos</span>
            <b v-if="vista === 'requerimientos'" class="nav-badge">{{ columns.reduce((a, c) => a + c.total, 0) }}</b>
          </button>
          <p class="nav-section-label">HERRAMIENTAS</p>
          <button class="nav-button" title="Reportes"><Icon name="bar-chart" :size="17" /> <span>Reportes</span></button>
          <button class="nav-button" :class="{ selected: vista === 'mantenimiento' }" @click="irAMantenimientos()" title="Incidencias - Mantenimiento"><Icon name="wrench" :size="17" /> <span>Incidencias - Mantenimiento</span></button>
          <button class="nav-button" title="Ayuda"><Icon name="help-circle" :size="17" /> <span>Ayuda</span></button>
        </template>
      </nav>
      <button class="hide-menu" @click="alternarSidebar">
        <Icon :name="sidebarColapsado ? 'chevrons-right' : 'chevrons-left'" :size="15" />
        <span v-if="!sidebarColapsado">Ocultar menú</span>
      </button>
    </aside>

    <!-- Contenido -->
    <section class="dashboard-content">
      <template v-if="vista === 'requerimientos' || vista === 'pendientes'">

        <!-- Registro de Requerimiento (lógica en NuevoRequerimientoForm + useRegistroRequerimiento) -->
        <template v-if="mostrarNuevoRequerimiento">
          <NuevoRequerimientoForm :perfil="perfil" @cerrar="cerrarNuevoRequerimiento" @registrado="alRegistrarRequerimiento" />
        </template>

        <!-- Mis requerimientos / Mis pendientes -->
        <template v-else>
          <p class="section-eyebrow">{{ (perfilActivo?.nombreRol || 'Usuario').toUpperCase() }} · {{ seccionActivaLabel.toUpperCase() }}</p>
          <div class="greeting">
            <h1>Hola, {{ nombreUsuario }} <span>!</span></h1>
            <p v-if="vista === 'pendientes'">Requerimientos que requieren tu acción{{ perfilActivo ? ` como ${perfilActivo.nombreRol.toLowerCase()}` : '' }}.</p>
            <p v-else>Gestiona y da seguimiento a los requerimientos que registraste.</p>
            <p v-if="cargando" class="conn-status">Cargando requerimientos…</p>
            <p v-else-if="!conectado" class="conn-status conn-status--offline">⚠ No se pudo conectar con el backend (service-requerimiento)<span v-if="mensajeError">: {{ mensajeError }}</span>.<span v-if="vista === 'pendientes'"> Mostrando datos de referencia.</span> <button @click="refrescar">Reintentar</button></p>
          </div>

          <div class="toolbar">
            <button class="new-request" @click="abrirNuevoRequerimiento"><Icon name="plus" :size="15" /> Nuevo requerimiento</button>
            <label class="search"><Icon name="search" :size="16" /><input v-model="numeroBusqueda" placeholder="Buscar por número o título..." @keyup.enter="ejecutarBusquedaAvanzada"></label>
            <button><Icon name="star" :size="14" /> Alta prioridad</button>
            <button><Icon name="clock" :size="14" /> Pendientes de conformidad</button>
            <button><Icon name="shield" :size="14" /> Requiere autorización</button>
            <button v-if="vista === 'pendientes'" @click="abrirPanelBusqueda">
              <Icon name="filter" :size="14" /> Búsqueda Avanzada
              <b v-if="filtrosActivos.length" class="filtro-badge">{{ filtrosActivos.length }}</b>
            </button>
            <div class="view-toggle">
              <button :class="{ selected: vistaTarjetas === 'grid' }" @click="cambiarVistaTarjetas('grid')" aria-label="Vista en tablero"><Icon name="grid" :size="15" /></button>
              <button :class="{ selected: vistaTarjetas === 'list' }" @click="cambiarVistaTarjetas('list')" aria-label="Vista en lista"><Icon name="list" :size="15" /></button>
            </div>
          </div>

          <template v-if="resultadosBusqueda === null">
            <section class="metrics">
              <article
                v-for="metric in metrics"
                :key="metric[0]"
                :class="{ selected: metricaSeleccionada === metric[0] }"
                @click="alternarMetrica(metric[0])"
              >
                <i :class="metric[2]"><Icon :name="metric[2] === 'check' ? 'check' : metric[2] === 'close' ? 'x' : metric[2] === 'clock' ? 'clock' : metric[2] === 'user' ? 'user-plus' : metric[2] === 'document' ? 'file-text' : 'folder'" :size="18" /></i>
                <div><span>{{ metric[0] }}</span><strong>{{ metric[1] }}</strong></div>
              </article>
            </section>
            <p v-if="metricaSeleccionada" class="metrics-filter-hint">Mostrando solo "{{ metricaSeleccionada }}". <button class="volver-pill" @click="alternarMetrica(metricaSeleccionada)"><Icon name="x" :size="12" /> Ver todo</button></p>

            <section class="board" :class="{ 'board--list': vistaTarjetas === 'list' }">
              <article v-for="column in columnasVisibles" :key="column.title" class="board-column">
                <header :class="column.tone"><span class="dot"></span><strong>{{ column.title }}</strong><b>{{ column.total }}</b></header>
                <div class="cards">
                  <article v-for="card in column.cards" :key="card[0]" class="request-card" @click="seleccionarRequerimiento(card, column)">
                    <div class="card-top">
                      <strong>{{ card[0] }}</strong>
                      <span class="priority-badge" :class="card[4].toLowerCase()">
                        <Icon v-if="['alta', 'importante'].includes(card[4].toLowerCase())" name="star" :size="10" />
                        <Icon v-else-if="card[4].toLowerCase() === 'urgente'" name="zap" :size="10" />
                        {{ card[4] }}
                      </span>
                    </div>
                    <h3>{{ card[1] }}</h3>
                    <p class="card-area">{{ card[2] }}</p>
                    <div class="tags"><span>Autorizado</span><span>Conf. pendiente</span></div>
                    <div class="card-footer">
                      <span class="card-date"><Icon name="calendar" :size="12" />{{ card[3] }}</span>
                      <b>ER</b>
                    </div>
                  </article>
                  <p v-if="!column.cards.length" class="empty-column">Sin requerimientos</p>
                </div>
              </article>
            </section>
          </template>

          <template v-else>
            <div class="toolbar volver-toolbar">
              <button class="volver-pill" @click="cerrarBusquedaAvanzada"><Icon name="corner-down-left" :size="14" /> Volver al tablero</button>
            </div>
            <div v-if="filtrosActivos.length" class="filtros-chips-row">
              <span v-if="!buscandoAvanzado && !errorBusquedaAvanzada" class="filtros-chips-count">{{ resultadosBusqueda.length }} resultado{{ resultadosBusqueda.length === 1 ? '' : 's' }}</span>
              <span v-for="f in filtrosActivos" :key="f.clave" class="filtro-chip">
                {{ f.etiqueta }}
                <button type="button" @click="quitarFiltroActivo(f.clave)" aria-label="Quitar filtro"><Icon name="x" :size="10" /></button>
              </span>
            </div>
            <section class="busqueda-resultados">
              <header><strong>Resultados de búsqueda avanzada</strong><button class="cerrar-busqueda" @click="cerrarBusquedaAvanzada"><Icon name="x" :size="14" /> Cerrar</button></header>
              <p v-if="buscandoAvanzado" class="conn-status">Buscando…</p>
              <p v-else-if="errorBusquedaAvanzada" class="conn-status conn-status--offline">⚠ {{ errorBusquedaAvanzada }}</p>
              <p v-else-if="!resultadosBusqueda.length" class="empty-column">Sin resultados.</p>
              <div v-else class="cards cards-grid">
                <article v-for="card in resultadosBusqueda" :key="card[0]" class="request-card" @click="seleccionarRequerimiento(card, { title: 'Búsqueda avanzada' })">
                  <div class="card-top">
                    <strong>{{ card[0] }}</strong>
                    <span class="priority-badge" :class="card[4].toLowerCase()">
                      <Icon v-if="['alta', 'importante'].includes(card[4].toLowerCase())" name="star" :size="10" />
                      <Icon v-else-if="card[4].toLowerCase() === 'urgente'" name="zap" :size="10" />
                      {{ card[4] }}
                    </span>
                  </div>
                  <h3>{{ card[1] }}</h3>
                  <p class="card-area">{{ card[2] }}</p>
                  <div class="card-footer"><span class="card-date"><Icon name="calendar" :size="12" />{{ card[3] }}</span></div>
                </article>
              </div>
            </section>
          </template>
        </template>
      </template>

      <template v-else-if="vista === 'mantenimiento'">
        <p class="section-eyebrow">{{ (perfilActivo?.nombreRol || 'Usuario').toUpperCase() }} · MANTENIMIENTOS</p>
        <div class="greeting"><h1>Mantenimientos <span>!</span></h1><p>Catálogos maestros del sistema (Personas, Categorías, Problemas, Soluciones y sus relaciones).</p></div>
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
        <div class="greeting"><h1>Documentación <span>!</span></h1><p>Procedimientos y manuales del Sistema de Gestión de Requerimientos.</p></div>
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
        <button v-if="puedeClasificar" class="asignar" @click="mostrarModalClasificar = true"><Icon name="check" :size="14" /> Clasificar</button>
        <button v-if="puedeAsignar" class="asignar" @click="abrirModalAsignar"><Icon name="user-plus" :size="14" /> Asignar</button>
        <button><Icon name="shield" :size="14" /> Solicitar autorización</button>
        <template v-if="puedeDarConformidad">
          <button class="conformidad-si"><Icon name="check" :size="14" /> Dar conformidad</button>
          <button class="conformidad-no"><Icon name="x" :size="14" /> Dar no conformidad</button>
        </template>
        <button><Icon name="history" :size="14" /> Ver historial</button>
      </div>

      <div class="detail-body">
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

    <!-- Modal Clasificar Requerimiento (Operador) -->
    <ClasificarRequerimientoModal
      v-if="mostrarModalClasificar && codigoRequerimientoSeleccionado()"
      :codigo-requerimiento="codigoRequerimientoSeleccionado()"
      :perfil="perfil"
      @cerrar="mostrarModalClasificar = false"
      @clasificado="alClasificarRequerimiento"
    />

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
          <div class="form-field"><label>ROL</label><select v-model="rolFiltro"><option value="">— Todos —</option><option value="Administrador">Administrador</option><option value="Operador">Operador</option><option value="Supervisor">Supervisor</option><option value="Responsable">Responsable</option></select></div>
          <div class="form-field"><label>PRIORIDAD</label><select v-model="prioridadFiltro"><option value="">— Todos —</option><option value="Alta">Alta</option><option value="Media">Media</option><option value="Baja">Baja</option></select></div>
          <div class="form-field"><label>ESTADO</label><select v-model="estadoFiltro"><option value="">— Todos —</option><option v-for="op in OPCIONES_ESTADO_BUSQUEDA" :key="op.value" :value="op.value">{{ op.label }}</option></select></div>
          <div class="form-field"><label>SUBESTADO</label><select v-model="subestadoFiltro"><option value="">— Todos —</option></select></div>
          <div class="form-field"><label>CATEGORÍA</label><select v-model="categoriaFiltro"><option value="">— Todos —</option></select></div>
          <div class="form-field"><label>SUBCATEGORÍA</label><select v-model="subcategoriaFiltro"><option value="">— Todos —</option></select></div>
          <div class="modal-grid-2">
            <div class="form-field"><label>FECHA DESDE</label><input type="date" v-model="fechaInicioBusqueda"></div>
            <div class="form-field"><label>FECHA HASTA</label><input type="date" v-model="fechaFinBusqueda"></div>
          </div>
          <p class="form-hint">Pendiente de culminar la implementación</p>
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
            <input :value="selectedRequest?.[0]" disabled>
          </div>
          <div class="form-field">
            <label>Estado</label>
            <input :value="selectedColumn?.title" disabled>
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
