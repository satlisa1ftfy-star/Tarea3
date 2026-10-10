<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { normalizarTexto, usePersonasCatalogo } from '../../controllers/usePersonasCatalogo'
import Icon from '../Icon.vue'

// -------------------------------------------------------------------------
// Ventana modal reutilizable para buscar y seleccionar personas del catálogo.
// No conoce ningún proceso de negocio: solo administra la selección que le
// entrega el padre por v-model. Cada proceso define su título, su descripción
// y qué hace con las personas elegidas.
//
// Uso:
//   <SeleccionarPersonasModal
//     v-if="mostrarModalBuscarPersonas"
//     v-model="personasSeleccionadas"
//     titulo="Seleccionar responsables"
//     descripcion="Requerimiento: REQ-280299"
//     @confirmar="mostrarModalBuscarPersonas = false"
//     @cerrar="mostrarModalBuscarPersonas = false"
//   />
//
// Catálogo: usePersonasCatalogo (GET /api/personas?vigencia=1 una sola vez y
// compartido en memoria). Buscar no hace llamadas HTTP.
// Identificador de persona: codigoPersona.

const props = defineProps({
  modelValue: { type: Array, default: () => [] },   // personas seleccionadas (objetos del catálogo)
  titulo: { type: String, default: 'Seleccionar personas' },
  descripcion: { type: String, default: '' },        // contexto del proceso que abre la ventana
})
const emit = defineEmits(['update:modelValue', 'confirmar', 'cerrar'])

const { personasCatalogo, cargandoPersonas, errorPersonas, cargarCatalogoPersonas } = usePersonasCatalogo()

const textoBusqueda = ref('')  // buscador principal (nombre OR cargo OR unidad)
const filtroPersona = ref('')  // filtro de la columna Persona (solo nombre)
const filtroCargo = ref('')    // filtro de la columna Cargo (solo cargo)
const filtroUnidad = ref('')   // filtro de la columna Unidad (solo unidadOrganica)
const mostrarResultados = ref(false)

function clavePersona(persona) {
  return persona.codigoPersona
}

function estaSeleccionada(persona) {
  return props.modelValue.some((p) => clavePersona(p) === clavePersona(persona))
}

// La selección real vive en el padre: solo se emiten listas nuevas.
function agregarPersona(persona) {
  if (estaSeleccionada(persona)) return
  emit('update:modelValue', [...props.modelValue, persona])
}

function quitarPersona(persona) {
  emit('update:modelValue', props.modelValue.filter((p) => clavePersona(p) !== clavePersona(persona)))
}

// Resultados temporales: búsqueda en memoria, ignorando mayúsculas y tildes:
//   (principal en nombre OR cargo OR unidad) AND persona AND cargo AND unidad
// Sin texto en el buscador principal no hay resultados. Excluye a las seleccionadas.
const resultadosBusqueda = computed(() => {
  const general = normalizarTexto(textoBusqueda.value.trim())
  if (!general) return []
  const persona = normalizarTexto(filtroPersona.value.trim())
  const cargo = normalizarTexto(filtroCargo.value.trim())
  const unidad = normalizarTexto(filtroUnidad.value.trim())

  return personasCatalogo.value.filter((p) => {
    if (estaSeleccionada(p)) return false
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

const filtrosActivos = computed(() => [
  { etiqueta: 'Persona', valor: filtroPersona.value.trim() },
  { etiqueta: 'Cargo', valor: filtroCargo.value.trim() },
  { etiqueta: 'Unidad', valor: filtroUnidad.value.trim() },
].filter((f) => f.valor))

const hayBusqueda = computed(() => textoBusqueda.value.trim().length > 0)

// Limpia la búsqueda y oculta los resultados; no modifica la selección.
function limpiarBusqueda() {
  textoBusqueda.value = ''
  filtroPersona.value = ''
  filtroCargo.value = ''
  filtroUnidad.value = ''
  mostrarResultados.value = false
}

// Zona de búsqueda (input + resultados + botones Agregar): los resultados se
// ocultan solo al hacer clic o enfocar algo fuera de ella (no con @blur, que
// los ocultaría antes de procesar "+ Agregar").
const zonaBusquedaRef = ref(null)
const desplegarResultados = computed(() => mostrarResultados.value && hayBusqueda.value)

function alInteractuarFuera(event) {
  if (zonaBusquedaRef.value && !zonaBusquedaRef.value.contains(event.target)) {
    mostrarResultados.value = false
  }
}

const buscadorRef = ref(null)
onMounted(async () => {
  cargarCatalogoPersonas() // si ya está en memoria no vuelve a llamar al endpoint
  document.addEventListener('pointerdown', alInteractuarFuera)
  document.addEventListener('focusin', alInteractuarFuera)
  await nextTick()
  buscadorRef.value?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', alInteractuarFuera)
  document.removeEventListener('focusin', alInteractuarFuera)
})

// Cambiar el buscador principal inicia una nueva búsqueda: limpia los filtros por columna.
watch(textoBusqueda, () => {
  filtroPersona.value = ''
  filtroCargo.value = ''
  filtroUnidad.value = ''
})

// Al cambiar la búsqueda: se muestran los resultados y su grilla vuelve arriba.
const grillaResultadosRef = ref(null)
watch([textoBusqueda, filtroPersona, filtroCargo, filtroUnidad], async () => {
  if (hayBusqueda.value) mostrarResultados.value = true
  await nextTick()
  if (grillaResultadosRef.value) grillaResultadosRef.value.scrollTop = 0
})
</script>

<template>
  <div class="modal-overlay modal-overlay--superior" @click.self="emit('cerrar')">
    <div class="modal-box seleccionar-personas" role="dialog" aria-modal="true" :aria-label="titulo">
      <button class="close-detail modal-close" aria-label="Cerrar" @click="emit('cerrar')"><Icon name="x" :size="18" /></button>
      <h2>{{ titulo }}</h2>
      <p v-if="descripcion" class="sp-descripcion">{{ descripcion }}</p>

      <!-- Personas seleccionadas: estado real (v-model del padre) -->
      <section class="sp-zona sp-zona--seleccionadas">
        <header class="sp-zona-titulo">
          <span><Icon name="user-plus" :size="14" /> Personas seleccionadas</span>
          <b>{{ modelValue.length }} {{ modelValue.length === 1 ? 'persona seleccionada' : 'personas seleccionadas' }}</b>
        </header>
        <div v-if="modelValue.length" class="sp-grilla sp-grilla--seleccionadas">
          <table>
            <thead>
              <tr><th>Persona</th><th>Cargo</th><th>Unidad</th><th class="col-accion">Acción</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in modelValue" :key="clavePersona(p)">
                <td>{{ p.nombre }}</td>
                <td>{{ p.cargo || '—' }}</td>
                <td>{{ p.unidadOrganica || '—' }}</td>
                <td class="col-accion">
                  <button type="button" class="btn-quitar" @click="quitarPersona(p)"><Icon name="x" :size="12" /> Quitar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="form-hint">Aún no has seleccionado personas. Búscalas abajo y pulsa "+ Agregar".</p>
      </section>

      <!-- Buscar personas: zona temporal (input + resultados + botones Agregar) -->
      <section ref="zonaBusquedaRef" class="sp-zona sp-zona--busqueda">
        <header class="sp-zona-titulo">
          <span><Icon name="search" :size="14" /> Buscar personas</span>
          <button type="button" class="sp-limpiar" :disabled="!hayBusqueda && !filtrosActivos.length" @click="limpiarBusqueda">
            <Icon name="x" :size="12" /> Limpiar búsqueda
          </button>
        </header>
        <label class="sp-buscador">
          <Icon name="search" :size="15" />
          <input
            ref="buscadorRef"
            v-model="textoBusqueda"
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
        <div v-else-if="desplegarResultados" class="sp-resultados">
          <div class="sp-resumen">
            Resultados de búsqueda · Resultados encontrados: <strong>{{ resultadosBusqueda.length }}</strong>
          </div>
          <div v-if="filtrosActivos.length" class="sp-filtros-activos">
            <span>Filtros activos:</span>
            <span v-for="f in filtrosActivos" :key="f.etiqueta" class="filtro-chip">{{ f.etiqueta }}: {{ f.valor }}</span>
          </div>

          <!-- La cabecera con los filtros siempre se dibuja (aunque no haya resultados) para no perder el foco -->
          <div ref="grillaResultadosRef" class="sp-grilla sp-grilla--resultados">
            <table>
              <thead>
                <tr><th>Persona</th><th>Cargo</th><th>Unidad</th><th class="col-accion">Acción</th></tr>
                <tr class="sp-fila-filtros">
                  <th><label class="sp-filtro"><Icon name="search" :size="12" /><input v-model="filtroPersona" placeholder="Buscar persona..." autocomplete="off"></label></th>
                  <th><label class="sp-filtro"><Icon name="search" :size="12" /><input v-model="filtroCargo" placeholder="Buscar cargo..." autocomplete="off"></label></th>
                  <th><label class="sp-filtro"><Icon name="search" :size="12" /><input v-model="filtroUnidad" placeholder="Buscar unidad..." autocomplete="off"></label></th>
                  <th class="col-accion"></th>
                </tr>
              </thead>
              <tbody>
                <!-- Una persona puede figurar en dos U.O. con el mismo codigoPersona: la key combina ambos -->
                <tr v-for="p in resultadosBusqueda" :key="`${clavePersona(p)}-${p.codigoUo}`">
                  <td>{{ p.nombre }}</td>
                  <td>{{ p.cargo || '—' }}</td>
                  <td>{{ p.unidadOrganica || '—' }}</td>
                  <td class="col-accion">
                    <button type="button" class="btn-agregar" @click="agregarPersona(p)"><Icon name="plus" :size="12" /> Agregar</button>
                  </td>
                </tr>
                <tr v-if="!resultadosBusqueda.length" class="sp-fila-vacia">
                  <td colspan="4">No se encontraron personas para agregar.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <p v-else-if="!hayBusqueda" class="form-hint">Escribe para buscar personas y agrégalas con "+ Agregar".</p>
      </section>

      <div class="modal-actions">
        <button type="button" class="modal-btn-primario" @click="emit('confirmar')">
          <Icon name="check" :size="13" /> Listo ({{ modelValue.length }})
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Se superpone a otro modal abierto (mismo .modal-overlay del proyecto, un nivel más arriba). */
.modal-overlay--superior{z-index:60;background:#0b2d5580}
.seleccionar-personas{max-width:820px;max-height:92vh}
.sp-descripcion{margin:-12px 0 14px;color:var(--muted);font-size:12.5px}
.sp-zona{padding:10px;border-radius:12px}
.sp-zona--seleccionadas{border:1.5px solid var(--blue);background:#fff}
.sp-zona--busqueda{margin-top:12px;border:1px dashed var(--muted-2,#8393b8);background:#f6f8fc}
.sp-zona-titulo{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:6px;color:var(--text);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.04em}
.sp-zona-titulo span{display:inline-flex;align-items:center;gap:6px}
.sp-zona-titulo b{color:var(--blue);font-size:11.5px;font-weight:600;text-transform:none;letter-spacing:0}
.sp-grilla{overflow:auto;border:1px solid var(--border);border-radius:10px;background:#fff}
.sp-grilla table{width:100%;border-collapse:collapse;font-size:12px}
.sp-grilla th{padding:9px 10px;color:var(--muted);background:#f4f7fc;font-size:11px;font-weight:600;text-align:left;border-bottom:1px solid var(--border)}
.sp-grilla td{padding:8px 10px;color:var(--text);border-bottom:1px solid var(--border)}
.sp-grilla tbody tr:last-child td{border-bottom:0}
.sp-grilla thead{position:sticky;top:0;z-index:1}
.sp-grilla--seleccionadas{max-height:200px}
.sp-grilla--resultados{max-height:280px}
.sp-grilla--resultados table{min-width:560px}
.sp-grilla--resultados tbody tr:hover{background:#f6f8fc}
.sp-fila-filtros th{padding:0 8px 8px}
.sp-fila-vacia td{padding:14px 10px;color:var(--muted);text-align:center}
.sp-buscador{display:flex;align-items:center;gap:8px;padding:0 12px;border:1px solid var(--border);border-radius:10px;color:var(--muted);background:#fff}
.sp-buscador input{flex:1;padding:11px 0;border:0;outline:0;background:none;color:var(--text);font-size:13px;font-family:inherit}
.sp-filtro{display:flex;align-items:center;gap:5px;padding:0 7px;border:1px solid var(--border);border-radius:7px;color:var(--muted);background:#fff}
.sp-filtro input{width:100%;min-width:0;padding:5px 0;border:0;outline:0;background:none;color:var(--text);font-size:11.5px;font-weight:400;font-family:inherit}
.sp-resultados{margin-top:8px}
.sp-resumen{margin-bottom:6px;color:var(--muted);font-size:12px}
.sp-resumen strong{color:var(--text)}
.sp-filtros-activos{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin-bottom:6px;color:var(--muted);font-size:11.5px;font-weight:600}
.sp-limpiar{display:inline-flex;align-items:center;gap:5px;padding:4px 10px;border:1px solid var(--border);border-radius:20px;color:var(--blue);background:#fff;font-size:11px;font-weight:600;text-transform:none;letter-spacing:0;cursor:pointer}
.sp-limpiar:disabled{color:var(--muted);cursor:default;opacity:.6}
.col-accion{width:96px;text-align:center!important}
.btn-agregar,.btn-quitar{display:inline-flex;align-items:center;gap:4px;padding:4px 9px;border-radius:7px;font-size:11px;font-weight:600;white-space:nowrap;cursor:pointer}
.btn-agregar{border:1px solid var(--blue);color:var(--blue);background:#fff}
.btn-agregar:hover{color:#fff;background:var(--blue)}
.btn-quitar{border:1px solid #f1c4bf;color:#c0392b;background:#fff}
.btn-quitar:hover{background:#fdecea}
.modal-actions .modal-btn-primario{display:inline-flex;align-items:center;gap:6px}
</style>
