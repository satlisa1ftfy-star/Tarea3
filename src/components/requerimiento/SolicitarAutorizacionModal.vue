<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { normalizarTexto, usePersonasCatalogo } from '../../controllers/usePersonasCatalogo'
import { solicitarAutorizacion } from '../../services/requerimientoService'
import { cargarCargosAutorizados } from '../../models/cargo'
import { cargarSolicitudesAutorizacion } from '../../models/solicitudAutorizacion'
import Icon from '../Icon.vue'

// -------------------------------------------------------------------------
// Solicitar autorización: modal con Motivo (obligatorio) y selección múltiple
// de personas que autorizan. Las personas salen del catálogo en memoria
// (GET /api/personas?vigencia=1, descargado una sola vez) y se buscan en memoria
// con un buscador general y filtros por columna. La solicitud se envía a
// POST /api/requerimiento/solicitar.

const props = defineProps({
  visible: { type: Boolean, default: false },
  perfil: { type: Object, default: null },
  requerimiento: { type: Array, default: null },          // tarjeta seleccionada (selectedRequest)
  estado: { type: String, default: '' },                  // título de la columna (selectedColumn.title)
  codigoRequerimiento: { type: Number, default: null },   // codigoRequerimientoSeleccionado() del Dashboard
  unidadOrganica: { type: Object, default: null },        // ya no se usa: el catálogo trae todas las UO
})
const emit = defineEmits(['cerrar', 'solicitudPreparada'])

const LARGO_MAXIMO_OBSERVACION = 2000 // Nota del sistema antiguo: máx. 2000 caracteres

// personasCatalogo: todas las personas vigentes (compartido, se descarga una sola vez).
const { personasCatalogo, cargandoPersonas, errorPersonas, cargarCatalogoPersonas } = usePersonasCatalogo()

const motivoAutorizacion = ref('')
const autorizadoresSeleccionados = ref([])
const autorizadorTexto = ref('')  // buscador principal (nombre OR cargo OR unidad)
const filtroPersona = ref('')     // filtro de la columna Persona (solo nombre)
const filtroCargo = ref('')       // filtro de la columna Cargo (solo cargo)
const filtroUnidad = ref('')      // filtro de la columna Unidad (solo unidadOrganica)
const mostrarResultados = ref(false) // visibilidad de la grilla temporal de resultados
const enviandoAutorizacion = ref(false)
const errorAutorizacion = ref('')

// -------------------------------------------------------------------------
// Cargos autorizados (GET /api/requerimiento/buscarCargo): lista informativa,
// independiente de la selección de personas. Categoría y subcategoría vienen
// en las posiciones [5] y [6] de la tarjeta del requerimiento (models/dashboard.js).
const cargosAutorizados = ref([])
const cargandoCargos = ref(false)
const errorCargos = ref('')
const faltanDatosCargos = ref(false)

const parametrosCargos = computed(() => ({
  codigoReq: props.codigoRequerimiento,
  codigoCat: props.requerimiento?.[5] ?? null,
  codigoSubCat: props.requerimiento?.[6] ?? null,
}))

let consultaCargosActual = ''
async function cargarCargos() {
  const parametros = parametrosCargos.value
  const clave = `${parametros.codigoReq}|${parametros.codigoCat}|${parametros.codigoSubCat}`
  consultaCargosActual = clave
  errorCargos.value = ''
  faltanDatosCargos.value = false

  if (parametros.codigoReq == null || parametros.codigoCat == null || parametros.codigoSubCat == null) {
    cargosAutorizados.value = []
    faltanDatosCargos.value = true
    return
  }

  cargandoCargos.value = true
  try {
    const cargos = await cargarCargosAutorizados(parametros)
    if (consultaCargosActual === clave) cargosAutorizados.value = cargos
  } catch (error) {
    console.error('No se pudieron cargar los cargos autorizados.', error)
    if (consultaCargosActual === clave) {
      cargosAutorizados.value = []
      errorCargos.value = 'No se pudieron cargar los cargos autorizados.'
    }
  } finally {
    if (consultaCargosActual === clave) cargandoCargos.value = false
  }
}

// Si cambia el requerimiento con el modal abierto, se consultan sus cargos
// (cargarCargosAutorizados reutiliza lo ya consultado con los mismos parámetros).
watch(parametrosCargos, () => {
  if (props.visible) cargarCargos()
})

// -------------------------------------------------------------------------
// Selección inicial: personas a las que ya se solicitó autorización
// (GET /api/requerimiento/consultarSolicitud). Cada "codigo" de la respuesta se
// busca como codigoPersona en personasCatalogo y se agrega ese objeto completo
// a autorizadoresSeleccionados.
const consultandoSolicitudes = ref(false)
const errorSolicitudes = ref('')
let consultaSolicitudesActual = 0

async function cargarSeleccionInicial() {
  const consulta = ++consultaSolicitudesActual
  errorSolicitudes.value = ''
  if (props.codigoRequerimiento == null) return

  consultandoSolicitudes.value = true
  try {
    // El catálogo se necesita para convertir codigo -> persona completa.
    // Si ya está en memoria, no vuelve a llamar a /api/personas.
    const [solicitudes] = await Promise.all([
      cargarSolicitudesAutorizacion(props.codigoRequerimiento),
      cargarCatalogoPersonas(),
    ])
    if (consulta !== consultaSolicitudesActual) return // el modal se reabrió con otro requerimiento
    if (errorPersonas.value) return // sin catálogo no se puede relacionar; ya se muestra su error

    const nuevas = []
    for (const solicitud of solicitudes) {
      const persona = personasCatalogo.value.find((p) => claveAutorizador(p) === solicitud.codigo)
      if (!persona) {
        console.warn('No se encontró en personasCatalogo la persona de la autorización:', solicitud.codigo)
        continue
      }
      if (!estaSeleccionadoAutorizador(persona) && !nuevas.some((p) => claveAutorizador(p) === solicitud.codigo)) {
        nuevas.push(persona)
      }
    }
    autorizadoresSeleccionados.value = [...autorizadoresSeleccionados.value, ...nuevas]
  } catch (error) {
    console.error('No se pudieron consultar las autorizaciones del requerimiento.', error)
    if (consulta === consultaSolicitudesActual) {
      errorSolicitudes.value = 'No se pudieron consultar las autorizaciones ya registradas. Puedes seleccionar las personas manualmente.'
    }
  } finally {
    if (consulta === consultaSolicitudesActual) consultandoSolicitudes.value = false
  }
}

// Cada vez que se abre el modal: limpia el formulario, pide el catálogo y
// reconstruye la selección con las autorizaciones ya registradas.
// Si el catálogo ya está en memoria, cargarCatalogoPersonas() no vuelve a llamar al endpoint.
function inicializarModal() {
  motivoAutorizacion.value = ''
  autorizadoresSeleccionados.value = []
  limpiarFiltros() // también oculta los resultados
  errorAutorizacion.value = ''
  cargarCatalogoPersonas()
  cargarCargos()
  cargarSeleccionInicial()
}

watch(() => props.visible, (abierto) => {
  if (abierto) inicializarModal()
}, { immediate: true })

function cerrarModalAutorizacion() {
  emit('cerrar')
}

function claveAutorizador(persona) {
  return persona.codigoPersona ?? persona.codigoPer
}

function estaSeleccionadoAutorizador(persona) {
  return autorizadoresSeleccionados.value.some((p) => claveAutorizador(p) === claveAutorizador(persona))
}

function alternarAutorizador(persona) {
  if (estaSeleccionadoAutorizador(persona)) {
    autorizadoresSeleccionados.value = autorizadoresSeleccionados.value.filter(
      (p) => claveAutorizador(p) !== claveAutorizador(persona)
    )
  } else {
    autorizadoresSeleccionados.value = [...autorizadoresSeleccionados.value, persona]
  }
}

// -------------------------------------------------------------------------
// Dos conceptos separados:
//   autorizadoresSeleccionados -> selección real (grilla "Personas seleccionadas")
//   personasFiltradas          -> resultados temporales de búsqueda (+ Agregar)
// Solo agregarAutorizador / quitarAutorizador modifican la selección.

function agregarAutorizador(persona) {
  if (!estaSeleccionadoAutorizador(persona)) alternarAutorizador(persona)
}

function quitarAutorizador(persona) {
  if (estaSeleccionadoAutorizador(persona)) alternarAutorizador(persona)
}

// Búsqueda en memoria sobre personasCatalogo (sin llamadas HTTP), ignorando
// mayúsculas y tildes con normalizarTexto:
//   (principal en nombre OR cargo OR unidad) AND persona AND cargo AND unidad
// Sin texto en el buscador principal no hay resultados (no se muestra el catálogo).
// Excluye a las personas ya seleccionadas.
const personasFiltradas = computed(() => {
  const general = normalizarTexto(autorizadorTexto.value.trim())
  if (!general) return []
  const persona = normalizarTexto(filtroPersona.value.trim())
  const cargo = normalizarTexto(filtroCargo.value.trim())
  const unidad = normalizarTexto(filtroUnidad.value.trim())

  return personasCatalogo.value.filter((p) => {
    if (estaSeleccionadoAutorizador(p)) return false
    const nombreP = normalizarTexto(p.nombre)
    const cargoP = normalizarTexto(p.cargo)
    const unidadP = normalizarTexto(p.unidadOrganica)
    if (!(nombreP.includes(general) || cargoP.includes(general) || unidadP.includes(general))) return false
    if (persona && !nombreP.includes(persona)) return false
    if (cargo && !cargoP.includes(cargo)) return false
    if (unidad && !unidadP.includes(unidad)) return false
    return true
  })
})

// Filtros por columna activos (indicador dentro de la zona de resultados).
const filtrosActivos = computed(() => [
  { etiqueta: 'Persona', valor: filtroPersona.value.trim() },
  { etiqueta: 'Cargo', valor: filtroCargo.value.trim() },
  { etiqueta: 'Unidad', valor: filtroUnidad.value.trim() },
].filter((f) => f.valor))

const hayBusqueda = computed(() => autorizadorTexto.value.trim().length > 0)

// Limpia la búsqueda y oculta los resultados; no modifica autorizadoresSeleccionados.
function limpiarFiltros() {
  autorizadorTexto.value = ''
  filtroPersona.value = ''
  filtroCargo.value = ''
  filtroUnidad.value = ''
  mostrarResultados.value = false
}

// -------------------------------------------------------------------------
// Zona de búsqueda (input principal + grilla de resultados + botones Agregar):
// los resultados se ocultan solo cuando se hace clic o se enfoca algo fuera de
// esa zona. No se usa @blur, que los ocultaría antes de procesar "+ Agregar".
const zonaBusquedaRef = ref(null)
const desplegarResultados = computed(() => mostrarResultados.value && hayBusqueda.value)

function alInteractuarFuera(event) {
  if (zonaBusquedaRef.value && !zonaBusquedaRef.value.contains(event.target)) {
    mostrarResultados.value = false
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', alInteractuarFuera)
  document.addEventListener('focusin', alInteractuarFuera)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', alInteractuarFuera)
  document.removeEventListener('focusin', alInteractuarFuera)
})

// Cambiar el buscador principal inicia una nueva búsqueda general: limpia los
// filtros por columna (incluso al dejarlo vacío). Solo reacciona a cambios reales
// de contenido (no al foco/clic) y no genera ciclos: este watch solo escribe los
// filtros, y nada que observe los filtros escribe autorizadorTexto.
// No toca autorizadoresSeleccionados.
watch(autorizadorTexto, () => {
  filtroPersona.value = ''
  filtroCargo.value = ''
  filtroUnidad.value = ''
})

// Al cambiar la búsqueda: se muestran los resultados y su grilla vuelve arriba.
const personasGrillaRef = ref(null)
watch([autorizadorTexto, filtroPersona, filtroCargo, filtroUnidad], async () => {
  if (hayBusqueda.value) mostrarResultados.value = true
  await nextTick()
  if (personasGrillaRef.value) personasGrillaRef.value.scrollTop = 0
})

const puedeEnviarAutorizacion = computed(() =>
  motivoAutorizacion.value.trim().length > 0 && autorizadoresSeleccionados.value.length > 0
)

async function enviarSolicitudAutorizacion() {
  errorAutorizacion.value = ''
  if (!puedeEnviarAutorizacion.value) return
  enviandoAutorizacion.value = true
  try {
    const payload = {
      codigoRequerimiento: props.codigoRequerimiento,
      codigoPersonaSolicita: props.perfil?.codigoPersonaGr,
      codigoPersonaActualizacion: props.perfil?.codigoPersonaOrganizacion,
      codigosPersonaAutoriza: autorizadoresSeleccionados.value.map(claveAutorizador),
      motivo: motivoAutorizacion.value.trim(),
    }
    // Cuerpo de SolicitarAutorizaRequerimientoRequest (service-requerimiento).
    // codPerHelpDesk y codPerActualiza también se envían porque el backend los
    // usa al armar el XML del SP y Map.of no admite valores nulos.
    await solicitarAutorizacion({
      codRequerimiento: payload.codigoRequerimiento,
      codPerAutoriza: payload.codigosPersonaAutoriza.join(','),
      codPerSolAutorizacion: payload.codigoPersonaSolicita,
      codPerHelpDesk: payload.codigoPersonaSolicita,
      codPerActualiza: payload.codigoPersonaActualizacion|| '1001' ,
      observacion: payload.motivo,
    })
    emit('solicitudPreparada', payload)
  } catch (error) {
    console.error('No se pudo solicitar la autorización.', error)
    errorAutorizacion.value = error?.message || 'No se pudo solicitar la autorización.'
  } finally {
    enviandoAutorizacion.value = false
  }
}
</script>

<template>
  <!-- Modal Solicitar autorización: Motivo obligatorio + selección múltiple de autorizadores -->
  <div v-if="visible" class="modal-overlay" @click.self="cerrarModalAutorizacion">
    <div class="modal-box modal-box--ancho">
      <button class="close-detail modal-close" aria-label="Cerrar" @click="cerrarModalAutorizacion"><Icon name="x" :size="18" /></button>
      <h2>Solicitar autorización</h2>

      <div class="modal-grid-2">
        <div class="form-field">
          <label>Requerimiento Nº</label>
          <input :value="requerimiento?.[0]" disabled>
        </div>
        <div class="form-field">
          <label>Estado</label>
          <input :value="estado" disabled>
        </div>
      </div>

      <!-- Cargos autorizados: solo informativo (sin buscador ni selección) -->
      <div class="form-field">
        <label>Cargos autorizados</label>
        <p v-if="cargandoCargos" class="form-hint">Cargando cargos autorizados...</p>
        <p v-else-if="errorCargos" class="conn-status conn-status--offline">⚠ {{ errorCargos }}</p>
        <p v-else-if="faltanDatosCargos" class="form-hint">No se pueden consultar los cargos: el requerimiento no tiene categoría o subcategoría.</p>
        <p v-else-if="!cargosAutorizados.length" class="form-hint">No existen cargos autorizados para este requerimiento.</p>
        <div v-else class="autorizadores-grilla cargos-grilla">
          <table>
            <thead>
              <tr>
                <th class="col-codigo">Código</th>
                <th>Cargo</th>
                <th class="col-aprob">Aprob</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cargo in cargosAutorizados" :key="cargo.codigoCargo">
                <td class="col-codigo">{{ cargo.codigoCargo }}</td>
                <td>{{ cargo.nombreCargo }}</td>
                <td class="col-aprob">{{ cargo.tipAprob || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="form-field">
        <label>Personas que autorizan *</label>

        <p v-if="consultandoSolicitudes" class="form-hint">Consultando autorizaciones...</p>
        <p v-if="errorSolicitudes" class="conn-status conn-status--offline">⚠ {{ errorSolicitudes }}</p>
        <!-- PERSONAS SELECCIONADAS: única grilla que representa la selección real -->
        <section class="zona-seleccionados">
          <header class="zona-titulo">
            <span><Icon name="user-plus" :size="14" /> Personas seleccionadas</span>
            <b>{{ autorizadoresSeleccionados.length }} {{ autorizadoresSeleccionados.length === 1 ? 'persona seleccionada' : 'personas seleccionadas' }}</b>
          </header>
          <div v-if="autorizadoresSeleccionados.length" class="autorizadores-grilla seleccionados-grilla">
            <table>
              <thead>
                <tr><th>Persona</th><th>Cargo</th><th>Unidad</th><th class="col-accion">Acción</th></tr>
              </thead>
              <tbody>
                <tr v-for="p in autorizadoresSeleccionados" :key="claveAutorizador(p)">
                  <td>{{ p.nombre }}</td>
                  <td>{{ p.cargo || '—' }}</td>
                  <td>{{ p.unidadOrganica || '—' }}</td>
                  <td class="col-accion">
                    <button type="button" class="btn-quitar" @click="quitarAutorizador(p)"><Icon name="x" :size="12" /> Quitar</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else-if="!consultandoSolicitudes" class="form-hint">Aún no has seleccionado personas. Búscalas abajo y pulsa "+ Agregar".</p>
        </section>

        <!-- BUSCAR PERSONAS: zona temporal (input + resultados + botones Agregar) -->
        <section ref="zonaBusquedaRef" class="zona-busqueda">
          <header class="zona-titulo">
            <span><Icon name="search" :size="14" /> Buscar personas</span>
            <button type="button" class="limpiar-filtros" :disabled="!hayBusqueda && !filtrosActivos.length" @click="limpiarFiltros">
              <Icon name="x" :size="12" /> Limpiar búsqueda
            </button>
          </header>
          <label class="autorizadores-buscador">
            <Icon name="search" :size="15" />
            <input
              v-model="autorizadorTexto"
              :placeholder="cargandoPersonas ? 'Cargando personas...' : 'Buscar por persona, cargo o unidad...'"
              :disabled="cargandoPersonas"
              autocomplete="off"
              @focus="mostrarResultados = true"
              @click="mostrarResultados = true"
            >
          </label>

          <p v-if="cargandoPersonas" class="form-hint">Cargando personas...</p>
          <p v-else-if="errorPersonas" class="conn-status conn-status--offline">
            ⚠ No se pudo cargar el catálogo de personas: {{ errorPersonas }}
            <button type="button" @click="cargarCatalogoPersonas">Reintentar</button>
          </p>
          <div v-else-if="desplegarResultados" class="zona-resultados">
            <div class="autorizadores-resumen">
              <span>Resultados de búsqueda · Resultados encontrados: <strong>{{ personasFiltradas.length }}</strong></span>
            </div>
            <div v-if="filtrosActivos.length" class="autorizadores-filtros-activos">
              <span class="autorizadores-subtitulo">Filtros activos:</span>
              <span v-for="f in filtrosActivos" :key="f.etiqueta" class="filtro-chip">{{ f.etiqueta }}: {{ f.valor }}</span>
            </div>

            <!-- La cabecera con los filtros siempre se dibuja (aunque no haya resultados) para no perder el foco -->
            <div ref="personasGrillaRef" class="autorizadores-grilla personas-grilla">
              <table>
                <thead>
                  <tr>
                    <th>Persona</th>
                    <th>Cargo</th>
                    <th>Unidad</th>
                    <th class="col-accion">Acción</th>
                  </tr>
                  <tr class="fila-filtros">
                    <th><label class="filtro-columna"><Icon name="search" :size="12" /><input v-model="filtroPersona" placeholder="Buscar persona..." autocomplete="off"></label></th>
                    <th><label class="filtro-columna"><Icon name="search" :size="12" /><input v-model="filtroCargo" placeholder="Buscar cargo..." autocomplete="off"></label></th>
                    <th><label class="filtro-columna"><Icon name="search" :size="12" /><input v-model="filtroUnidad" placeholder="Buscar unidad..." autocomplete="off"></label></th>
                    <th class="col-accion"></th>
                  </tr>
                </thead>
                <tbody>
                  <!-- Una persona puede figurar en dos U.O. con el mismo codigoPersona: la key combina ambos -->
                  <tr v-for="p in personasFiltradas" :key="`${claveAutorizador(p)}-${p.codigoUo}`">
                    <td>{{ p.nombre }}</td>
                    <td>{{ p.cargo || '—' }}</td>
                    <td>{{ p.unidadOrganica || '—' }}</td>
                    <td class="col-accion">
                      <button type="button" class="btn-agregar" @click="agregarAutorizador(p)"><Icon name="plus" :size="12" /> Agregar</button>
                    </td>
                  </tr>
                  <tr v-if="!personasFiltradas.length" class="fila-vacia">
                    <td colspan="4">No se encontraron personas para agregar.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <p v-else-if="!hayBusqueda" class="form-hint">Escribe para buscar personas y agrégalas con "+ Agregar".</p>
        </section>
      </div>

      <div class="form-field">
        <label>Motivo *</label>
        <textarea
          v-model="motivoAutorizacion"
          :maxlength="LARGO_MAXIMO_OBSERVACION"
          rows="4"
          placeholder="Indica el motivo de la solicitud de autorización..."
        ></textarea>
        <p class="form-hint">{{ motivoAutorizacion.length }}/{{ LARGO_MAXIMO_OBSERVACION }} caracteres</p>
      </div>

      <p v-if="errorAutorizacion" class="conn-status conn-status--offline">⚠ {{ errorAutorizacion }}</p>

      <div class="modal-actions">
        <button class="modal-btn-secundario" :disabled="enviandoAutorizacion" @click="cerrarModalAutorizacion">Cancelar</button>
        <button
          class="modal-btn-primario"
          :disabled="!puedeEnviarAutorizacion || enviandoAutorizacion"
          @click="enviarSolicitudAutorizacion"
        >{{ enviandoAutorizacion ? 'Enviando...' : 'Solicitar' }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.autorizadores-grilla{max-height:220px;margin-top:6px;overflow-y:auto;border:1px solid var(--border);border-radius:10px;background:#fff}
.autorizadores-grilla table{width:100%;border-collapse:collapse;font-size:12px}
.autorizadores-grilla th{position:sticky;top:0;z-index:1;padding:9px 10px;color:var(--muted);background:#f4f7fc;font-size:11px;font-weight:600;text-align:left;border-bottom:1px solid var(--border)}
.autorizadores-grilla td{padding:8px 10px;color:var(--text);border-bottom:1px solid var(--border)}
.autorizadores-grilla tbody tr:last-child td{border-bottom:0}
.autorizadores-subtitulo{color:var(--muted);font-size:11px;font-weight:600}
.zona-titulo{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:6px;color:var(--text);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.04em}
.zona-titulo span{display:inline-flex;align-items:center;gap:6px}
.zona-titulo b{color:var(--blue);font-size:11.5px;font-weight:600;text-transform:none;letter-spacing:0}
/* Selección real: bloque fijo, borde sólido azul */
.zona-seleccionados{margin-top:4px;padding:10px;border:1.5px solid var(--blue);border-radius:12px;background:#fff}
.seleccionados-grilla{max-height:180px;margin-top:0}
/* Búsqueda: bloque temporal, fondo gris y borde punteado */
.zona-busqueda{margin-top:12px;padding:10px;border:1px dashed var(--muted-2,#8393b8);border-radius:12px;background:#f6f8fc}
.zona-resultados{margin-top:8px}
.autorizadores-buscador{display:flex;align-items:center;gap:8px;padding:0 12px;border:1px solid var(--border);border-radius:10px;color:var(--muted);background:#fff}
.form-field .autorizadores-buscador input{flex:1;padding:11px 0;border:0;outline:0;background:none;font-size:13px}
.autorizadores-resumen{display:flex;align-items:center;justify-content:space-between;gap:8px;color:var(--muted);font-size:12px}
.autorizadores-resumen strong{color:var(--text)}
.limpiar-filtros{display:inline-flex;align-items:center;gap:5px;padding:4px 10px;border:1px solid var(--border);border-radius:20px;color:var(--blue);background:#fff;font-size:11px;font-weight:600;text-transform:none;letter-spacing:0;cursor:pointer}
.limpiar-filtros:disabled{color:var(--muted);cursor:default;opacity:.6}
.autorizadores-filtros-activos{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin-top:6px;font-size:11.5px}
.personas-grilla{max-height:240px;overflow:auto}
.personas-grilla table{min-width:520px}
.personas-grilla thead{position:sticky;top:0;z-index:1}
.personas-grilla th{position:static}
.personas-grilla .fila-filtros th{padding:0 8px 8px}
.filtro-columna{display:flex;align-items:center;gap:5px;padding:0 7px;border:1px solid var(--border);border-radius:7px;color:var(--muted);background:#fff}
.form-field .filtro-columna input{width:100%;min-width:0;padding:5px 0;border:0;outline:0;background:none;font-size:11.5px;font-weight:400}
.personas-grilla tbody tr:hover{background:#f6f8fc}
.personas-grilla .fila-vacia td{padding:14px 10px;color:var(--muted);text-align:center}
.col-accion{width:96px;text-align:center!important}
.btn-agregar,.btn-quitar{display:inline-flex;align-items:center;gap:4px;padding:4px 9px;border-radius:7px;font-size:11px;font-weight:600;white-space:nowrap;cursor:pointer}
.btn-agregar{border:1px solid var(--blue);color:var(--blue);background:#fff}
.btn-agregar:hover{color:#fff;background:var(--blue)}
.btn-quitar{border:1px solid #f1c4bf;color:#c0392b;background:#fff}
.btn-quitar:hover{background:#fdecea}
.cargos-grilla{max-height:140px}
.cargos-grilla .col-codigo{width:70px}
.cargos-grilla .col-aprob{width:60px;text-align:center}
</style>
