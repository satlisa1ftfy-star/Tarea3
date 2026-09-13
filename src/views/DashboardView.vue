<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ESTADO, buscarAvanzado, cargarPendientes, cargarTablero, mockColumns, mockMetrics } from '../models/dashboard'
import { listarRoles } from '../services/seguridadService'
import { listarPersonas } from '../services/personaService'
import { buscarUnidadOrganica } from '../services/unidadOrganicaService'
import { asignarRequerimiento } from '../services/requerimientoService'

const props = defineProps({
  perfil: { type: Object, default: null },
})
const emit = defineEmits(['logout'])

// vista: 'requerimientos' = Mis requerimientos (lo que registré), 'pendientes' = Mis pendientes (lo que tengo por resolver)
const vista = ref('requerimientos')
const activeMenu = ref('Tablero')
const selectedRequest = ref(null)
const selectedColumn = ref(null)

const metrics = ref(mockMetrics)
const columns = ref(mockColumns)
const cargando = ref(true)
const conectado = ref(false)
const mensajeError = ref('')

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
// sin fecha, el filtro por unidad en el SP recorre toda la tabla sin índice
// calculando el responsable fila por fila (~12s con 5k filas).
const fechaInicioBusqueda = ref(hoyISO())
const fechaFinBusqueda = ref(hoyISO())
const resultadosBusqueda = ref(null)
const buscandoAvanzado = ref(false)
const errorBusquedaAvanzada = ref('')

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

// Trae los roles activos para el desplegable del avatar.
async function cargarRolesUsuario() {
  const usuario = props.perfil?.usuarioWindows
  if (!usuario) return
  try {
    roles.value = await listarRoles(usuario)
    // Por defecto se activa el primer rol de la lista que devuelve el
    // backend (sin preferencia fija) — el usuario puede cambiarlo desde
    // el desplegable de "Mis roles".
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
}

function irAPendientes() {
  vista.value = 'pendientes'
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
// Asignar requerimiento: modal + confirmación para el endpoint
// POST /api/requerimiento/asignar, visible solo en "Pendientes" y solo en estados no cerrados/atendidos 
// (whitelist de columnas; no cubre aún "Búsqueda avanzada", que no trae el estado real).

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
// El cargo se muestra aparte (solo lectura), no dentro del buscador.
const personaTexto = ref('')

const responsablePrincipal = ref(false)
// 'motivo': Descripción (bInformeTecnico_ReqMov=0).
// 'informeTecnico': Informe Técnico (bInformeTecnico_ReqMov=1).
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
}

function cerrarModalAsignar() {
  mostrarModalAsignar.value = false
}

// Búsqueda de Unidad Orgánica con debounce (350ms) a partir de 3 caracteres.
watch(unidadOrganicaTexto, (texto) => {
  clearTimeout(temporizadorBusquedaUo)
  if (unidadOrganicaSeleccionada.value && texto === unidadOrganicaSeleccionada.value.nombreUnidadOrganica) {
    return // no relanzar la búsqueda apenas se selecciona una opción
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
}

function irADocumentacion() {
  vista.value = 'documentacion'
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
  } else if (modoBusqueda.value === 'manual' && numero === 0) {
// Filtra por 'hoy' si no hay número; si ingresa número específico, se ignora la fecha.
    filtro.fechaInicio = fechaInicioBusqueda.value || 'TODO'
    filtro.fechaFin = fechaFinBusqueda.value || 'TODO'
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
  cargando.value = false
}

// Resetea la búsqueda avanzada al salir de 'Pendientes' para no arrastrar filtros.
watch(vista, (nuevaVista) => {
  if (nuevaVista !== 'pendientes') {
    resultadosBusqueda.value = null
    numeroBusqueda.value = ''
  }
  refrescar()
})

onMounted(() => {
  refrescar()
  cargarRolesUsuario()
})
</script>

<template>
  <main class="dashboard" :class="{ 'dashboard--detail-open': selectedRequest }">
    <header class="dashboard-header"><div class="sat-brand"><strong>SAT</strong><span>Servicio de<br>Administracion<br>Tributaria de Lima</span></div><div class="header-actions"><span class="header-system"><span class="header-icon">☷</span>Gestion de Requerimientos</span><button class="bell">♩<b>1</b></button><div class="profile-wrap"><button class="profile-trigger" @click="alternarMenuPerfil"><span class="avatar">{{ inicialesUsuario }}</span><b>⌄</b></button><div v-if="menuAbierto" class="role-menu"><div class="role-menu-header"><strong>{{ nombreUsuario }}</strong><small>{{ perfil?.usuarioWindows }}</small></div><div class="role-menu-section"><span>Mis roles</span><ul><li v-for="r in roles" :key="r.codigoPersonaRol" class="rol-opcion" :class="{ activo: perfilActivo?.codigoPersonaRol === r.codigoPersonaRol }" @click="seleccionarRol(r)">{{ r.nombreRol }}</li><li v-if="!roles.length" class="role-menu-empty">Sin roles activos</li></ul></div><button class="role-menu-logout" @click="cerrarSesion">Cerrar sesión</button></div></div></div></header>
    <aside class="sidebar"><nav><template v-if="menuSupervisor"><button class="nav-button" :class="{ selected: vista === 'mantenimiento' }" @click="irAMantenimientos()">🛠 <span>Mantenimientos</span></button><button class="nav-button" :class="{ selected: vista === 'documentacion' }" @click="irADocumentacion">📄 <span>Documentación</span></button><button class="nav-button">? <span>Ayuda</span></button></template><template v-else><button class="nav-button" :class="{ selected: vista === 'pendientes' }" @click="irAPendientes">◷ <span>Pendientes</span></button><button class="menu-parent" :class="{ selected: vista === 'requerimientos' }">▣ <span>Mis requerimientos</span><b>⌃</b></button><div class="submenu"><button :class="{ selected: vista === 'requerimientos' && activeMenu === 'Tablero' }" @click="irARequerimientos('Tablero')">▦ <span>Tablero</span></button><button :class="{ selected: vista === 'requerimientos' && activeMenu === 'Listado' }" @click="irARequerimientos('Listado')">☷ <span>Listado</span></button></div><button class="nav-button">▥ <span>Reportes</span></button><button class="nav-button" :class="{ selected: vista === 'mantenimiento' }" @click="irAMantenimientos()">⚒ <span>Incidencias - Mantenimiento</span></button><button class="nav-button">? <span>Ayuda</span></button></template></nav><button class="hide-menu">≪ <span>Ocultar menú</span></button></aside>
    <section class="dashboard-content"><template v-if="vista === 'requerimientos' || vista === 'pendientes'"><div class="greeting"><h1>Hola, {{ nombreUsuario }} <span>!</span></h1><p v-if="vista === 'pendientes'">Esto es lo que tienes pendiente de resolver (asignado, por dar conformidad o no conforme).</p><p v-else>Gestiona y da seguimiento a los requerimientos que registraste.</p><p v-if="cargando" class="conn-status">Cargando requerimientos…</p><p v-else-if="!conectado" class="conn-status conn-status--offline">⚠ No se pudo conectar con el backend (service-requerimiento)<span v-if="mensajeError">: {{ mensajeError }}</span>. Mostrando datos de referencia. <button @click="refrescar">Reintentar</button></p></div><div class="toolbar"><button class="new-request">＋ Nuevo requerimiento</button><label class="search">⌕<input v-model="numeroBusqueda" placeholder="Buscar por número de requerimiento..." @keyup.enter="vista === 'pendientes' && ejecutarBusquedaAvanzada()"></label><template v-if="vista === 'pendientes'"><template v-if="modoBusqueda === 'manual'"><label class="fecha-filtro">Desde<input type="date" v-model="fechaInicioBusqueda"></label><label class="fecha-filtro">Hasta<input type="date" v-model="fechaFinBusqueda"></label></template><button class="new-request" @click="ejecutarBusquedaAvanzada" :disabled="buscandoAvanzado">⌕ Buscar</button><span v-if="modoBusqueda === 'automatico'" class="modo-busqueda-info">Mostrando automáticamente lo no clasificado de tu unidad (rol Operador)</span><span v-else-if="modoBusqueda === 'manual'" class="modo-busqueda-info">Sin número: por defecto muestra lo registrado hoy en tu gerencia (cambia el rango de fechas o vacíalo para ver otro período). Si buscas un número puntual, la fecha se ignora.</span></template><template v-else><button>⚑ Alta prioridad</button><button>◷ Pendientes de conformidad</button><button>♢ Requiere autorización</button></template></div><template v-if="vista === 'requerimientos' || resultadosBusqueda === null"><section class="metrics"><article v-for="metric in metrics" :key="metric[0]"><span>{{ metric[0] }}</span><strong>{{ metric[1] }}</strong><i :class="metric[2]">{{ metric[2] === 'check' ? '✓' : metric[2] === 'close' ? '×' : metric[2] === 'clock' ? '◷' : metric[2] === 'user' ? '♙' : '▧' }}</i></article></section><div class="board-hint"><span>✥ Arrastra las tarjetas para actualizar el estado</span><button>Ordenar por: <b>Más recientes</b>⌄</button><button>☷</button></div><section class="board"><article v-for="column in columns" :key="column.title" class="board-column"><header :class="column.tone"><strong>{{ column.title }}</strong><b>{{ column.total }}</b></header><div class="cards"><article v-for="card in column.cards" :key="card[0]" class="request-card" @click="seleccionarRequerimiento(card, column)"><div class="card-top"><span>⁝</span><strong>{{ card[0] }}</strong><em :class="card[4].toLowerCase()">{{ card[4] }}</em></div><h3>{{ card[1] }}</h3><p>{{ card[2] }}</p><small>▧ {{ card[3] }} <b>ER</b></small><div class="tags"><span>Autorizado</span><span>Conf. pendiente</span></div></article><p v-if="!column.cards.length" class="empty-column">Sin requerimientos</p></div></article></section></template><template v-else-if="vista === 'pendientes'"><div class="toolbar volver-toolbar"><button class="volver-pill" @click="cerrarBusquedaAvanzada">↩ Volver a {{ vista === 'pendientes' ? 'Mis pendientes' : 'Mis requerimientos' }}</button></div><section v-if="vista === 'pendientes' && resultadosBusqueda !== null" class="busqueda-resultados"><header><strong>Resultados de búsqueda avanzada</strong><button class="cerrar-busqueda" @click="cerrarBusquedaAvanzada">Cerrar ×</button></header><p v-if="buscandoAvanzado" class="conn-status">Buscando…</p><p v-else-if="errorBusquedaAvanzada" class="conn-status conn-status--offline">⚠ {{ errorBusquedaAvanzada }}</p><p v-else-if="!resultadosBusqueda.length" class="empty-column">Sin resultados.</p><div v-else class="cards cards-grid"><article v-for="card in resultadosBusqueda" :key="card[0]" class="request-card" @click="seleccionarRequerimiento(card, { title: 'Búsqueda avanzada' })"><div class="card-top"><span>⁝</span><strong>{{ card[0] }}</strong><em :class="card[4].toLowerCase()">{{ card[4] }}</em></div><h3>{{ card[1] }}</h3><p>{{ card[2] }}</p><small>▧ {{ card[3] }}</small></article></div></section></template></template><template v-else-if="vista === 'mantenimiento'"><div class="greeting"><h1>Mantenimientos <span>!</span></h1><p>Catálogos maestros del sistema (Personas, Categorías, Problemas, Soluciones y sus relaciones).</p></div><div class="toolbar mantenimiento-toolbar"><button v-for="opcion in opcionesMantenimientoActivas" :key="opcion.id" :class="{ selected: mantenimientoActivo === opcion.id }" @click="irAMantenimientos(opcion.id)">{{ opcion.label }}</button></div><section class="mantenimiento-panel"><template v-for="opcion in opcionesMantenimientoActivas" :key="opcion.id"><div v-if="mantenimientoActivo === opcion.id"><h2>{{ opcion.label }}</h2><div class="toolbar"><button class="new-request">＋ Nuevo</button><label class="search">⌕<input placeholder="Buscar..."></label><button>🖶 Imprimir</button></div><p v-if="opcion.backend" class="empty-column">Listado de "{{ opcion.label }}" — pendiente de conectar a los endpoints ya existentes en service-requerimiento.</p><p v-else class="empty-column">"{{ opcion.label }}" tiene SP en el proyecto pero aún no tiene endpoint en service-requerimiento — falta ese trabajo de backend antes de poder listarlo aquí.</p></div></template></section></template><template v-else-if="vista === 'documentacion'"><div class="greeting"><h1>Documentación <span>!</span></h1><p>Procedimientos y manuales del Sistema de Gestión de Requerimientos.</p></div><section class="mantenimiento-panel"><p class="empty-column">Aquí irán los documentos de referencia (Procedimiento del SGR, Manual de usuario). Pendiente de definir si se listan archivos subidos o enlaces.</p></section></template>
    </section><aside v-if="selectedRequest" class="detail-panel"><button class="close-detail" aria-label="Cerrar detalle" @click="cerrarDetalle">×</button><div class="detail-code"><span>{{ selectedColumn?.title }}</span><b>{{ selectedRequest[0] }}</b></div><h2>{{ selectedRequest[1] }}</h2><dl><div><dt>♙ Área solicitante</dt><dd>{{ selectedRequest[2] }}</dd></div><div><dt>▧ Fecha registrada</dt><dd>{{ selectedRequest[3] }}</dd></div><div><dt>◷ Estado actual</dt><dd class="orange">{{ selectedColumn?.title }}</dd></div></dl><hr><h3>Acciones rápidas</h3><button class="move">▣ Mover de estado <b>›</b></button><button v-if="puedeAsignar" class="asignar" @click="abrirModalAsignar">⇄ Asignar requerimiento</button><button>♢ Solicitar autorización</button><template v-if="puedeDarConformidad"><button class="conformidad-si">✓ Dar conformidad</button><button class="conformidad-no">✕ Dar no conformidad</button></template><button>◷ Ver historial</button></aside>

<!-- Modal Asignar Requerimiento: simplifica la pantalla legacy omitiendo Descripción y Apellido Paterno -->
    <div v-if="mostrarModalAsignar" class="modal-overlay" @click.self="cerrarModalAsignar">
      <div class="modal-box modal-box--ancho">
        <button class="close-detail modal-close" aria-label="Cerrar" @click="cerrarModalAsignar">×</button>
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
            <!-- UO seleccionada: contenedor multilínea con clic para reabrir búsqueda (evita recortes de texto). -->
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
