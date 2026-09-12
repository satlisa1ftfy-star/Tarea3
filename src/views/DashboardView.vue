<script setup>
import { computed, onMounted, ref } from 'vue'
import { cargarTablero, mockColumns, mockMetrics } from '../models/dashboard'

const props = defineProps({
  perfil: { type: Object, default: null },
})
defineEmits(['logout'])

const activeMenu = ref('Tablero')
const selectedRequest = ref(null)

const metrics = ref(mockMetrics)
const columns = ref(mockColumns)
const cargando = ref(true)
const conectado = ref(false)
const mensajeError = ref('')

const nombreUsuario = computed(() => {
  const nombreCompleto = props.perfil?.nombreCompleto
  if (!nombreCompleto) return 'usuario'
  // "GUILLEN TAMARA JULY PATRICIA" -> "Guillen"
  const primeraPalabra = nombreCompleto.trim().split(/\s+/)[0]
  return primeraPalabra.charAt(0) + primeraPalabra.slice(1).toLowerCase()
})

const inicialesUsuario = computed(() => {
  const nombreCompleto = props.perfil?.nombreCompleto
  if (!nombreCompleto) return '--'
  const palabras = nombreCompleto.trim().split(/\s+/)
  return (palabras[0]?.[0] || '') + (palabras[1]?.[0] || '')
})

async function refrescar() {
  cargando.value = true
  const tablero = await cargarTablero(props.perfil)
  metrics.value = tablero.metrics
  columns.value = tablero.columns
  conectado.value = tablero.online
  mensajeError.value = tablero.errorMessage || ''
  cargando.value = false
}

onMounted(refrescar)
</script>

<template>
  <main class="dashboard" :class="{ 'dashboard--detail-open': selectedRequest }">
    <header class="dashboard-header"><div class="sat-brand"><strong>SAT</strong><span>Servicio de<br>Administracion<br>Tributaria de Lima</span></div><div class="header-actions"><span class="header-system"><span class="header-icon">☷</span>Gestion de Requerimientos</span><button class="bell">♩<b>1</b></button><span class="avatar">{{ inicialesUsuario }}</span><button class="profile" @click="$emit('logout')">{{ perfil?.nombreCompleto ? 'Cerrar sesión' : 'Mi perfil' }}⌄</button></div></header>
    <aside class="sidebar"><nav><button class="nav-button">◷ <span>Pendientes</span></button><button class="menu-parent">▣ <span>Mis requerimientos</span><b>⌃</b></button><div class="submenu"><button :class="{ selected: activeMenu === 'Tablero' }" @click="activeMenu = 'Tablero'">▦ <span>Tablero</span></button><button :class="{ selected: activeMenu === 'Listado' }" @click="activeMenu = 'Listado'">☷ <span>Listado</span></button></div><button class="nav-button">▥ <span>Reportes</span></button><button class="nav-button">⚒ <span>Incidencias - Mantenimiento</span></button><button class="nav-button">? <span>Ayuda</span></button></nav><button class="hide-menu">≪ <span>Ocultar menú</span></button></aside>
    <section class="dashboard-content"><div class="greeting"><h1>Hola, {{ nombreUsuario }} <span>!</span></h1><p>Gestiona y da seguimiento a los requerimientos de tu área.</p><p v-if="cargando" class="conn-status">Cargando requerimientos…</p><p v-else-if="!conectado" class="conn-status conn-status--offline">⚠ No se pudo conectar con el backend (service-requerimiento)<span v-if="mensajeError">: {{ mensajeError }}</span>. Mostrando datos de referencia. <button @click="refrescar">Reintentar</button></p></div><div class="toolbar"><button class="new-request">＋ Nuevo requerimiento</button><label class="search">⌕<input placeholder="Buscar por código, título o área solicitante..."></label><button>⚑ Alta prioridad</button><button>♙ Asignados a mí</button><button>◷ Pendientes de conformidad</button><button>♢ Requiere autorización</button></div><section class="metrics"><article v-for="metric in metrics" :key="metric[0]"><span>{{ metric[0] }}</span><strong>{{ metric[1] }}</strong><i :class="metric[2]">{{ metric[2] === 'check' ? '✓' : metric[2] === 'close' ? '×' : metric[2] === 'clock' ? '◷' : metric[2] === 'user' ? '♙' : '▧' }}</i></article></section><div class="board-hint"><span>✥ Arrastra las tarjetas para actualizar el estado</span><button>Ordenar por: <b>Más recientes</b>⌄</button><button>☷</button></div><section class="board"><article v-for="column in columns" :key="column.title" class="board-column"><header :class="column.tone"><strong>{{ column.title }}</strong><b>{{ column.total }}</b></header><div class="cards"><article v-for="card in column.cards" :key="card[0]" class="request-card" @click="selectedRequest = card"><div class="card-top"><span>⁝</span><strong>{{ card[0] }}</strong><em :class="card[4].toLowerCase()">{{ card[4] }}</em></div><h3>{{ card[1] }}</h3><p>{{ card[2] }}</p><small>▧ {{ card[3] }} <b>ER</b></small><div class="tags"><span>Autorizado</span><span>Conf. pendiente</span></div></article><div v-if="column.title === 'En proceso'" class="drop-zone">✥<br><b>Suelta aquí para mover<br>a En proceso</b></div></div><footer>+ {{ column.total - 4 }} más</footer></article></section></section>
    <aside v-if="selectedRequest" class="detail-panel"><button class="close-detail" aria-label="Cerrar detalle" @click="selectedRequest = null">×</button><div class="detail-code"><span :class="selectedRequest[4].toLowerCase()">{{ selectedRequest[4] }}</span><b>{{ selectedRequest[0] }}</b></div><h2>{{ selectedRequest[1] }}</h2><dl><div><dt>♙ Área solicitante</dt><dd>{{ selectedRequest[2] }}</dd></div><div><dt>♙ Solicitante</dt><dd>Erika Rojas</dd></div><div><dt>▧ Fecha registrada</dt><dd>{{ selectedRequest[3] }} 10:21</dd></div><div><dt>◷ Estado actual</dt><dd class="orange">Por asignar</dd></div><div><dt>♙ Asignado a</dt><dd>-</dd></div><div><dt>▧ Autorización</dt><dd class="purple-text">Autorización pendiente</dd></div><div><dt>◷ Conformidad</dt><dd class="orange">Conformidad pendiente</dd></div></dl><hr><h3>Acciones rápidas</h3><button class="move">▣ Mover de estado <b>›</b></button><button>♢ Solicitar autorización</button><button>◉ Solicitar conformidad</button><button>◷ Ver historial</button></aside>
  </main>
</template>
