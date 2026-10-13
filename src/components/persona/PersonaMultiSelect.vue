<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { normalizarTexto, usePersonasCatalogo } from '../../controllers/usePersonasCatalogo'
import Icon from '../Icon.vue'

// -------------------------------------------------------------------------
// Buscador de personas reutilizable: autocomplete + grilla de resultados +
// grilla fija de seleccionadas. Busca en el catálogo en memoria
// (usePersonasCatalogo, GET /api/personas?vigencia=1 una sola vez), así que
// escribir no hace llamadas HTTP.
//
// Uso:  <PersonaMultiSelect v-model="personasSeleccionadas" />
// modelValue es un arreglo de personas del catálogo
// ({ codigoPersona, nombre, cargo, codigoCargo, codigoUo, unidadOrganica }).

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Buscar persona por nombre...' },
  maxResultados: { type: Number, default: 50 }, // filas a dibujar en la grilla de resultados
})
const emit = defineEmits(['update:modelValue'])

const { personasCatalogo, cargandoPersonas, errorPersonas, cargarCatalogoPersonas } = usePersonasCatalogo()

const textoBusqueda = ref('')
const mostrarResultados = ref(false)
const contenedorBusqueda = ref(null)
const grillaResultados = ref(null)

// Identificador de la persona en todo el proyecto: codigoPersona.
function clavePersona(persona) {
  return persona.codigoPersona
}

function estaSeleccionada(persona) {
  return props.modelValue.some((p) => clavePersona(p) === clavePersona(persona))
}

// resultadosBusqueda: filtro en memoria por nombre, sin mayúsculas ni tildes.
// Es independiente de modelValue (personas seleccionadas).
const resultadosBusqueda = computed(() => {
  const texto = normalizarTexto(textoBusqueda.value.trim())
  if (!texto) return []
  return personasCatalogo.value.filter((p) => normalizarTexto(p.nombre).includes(texto))
})
const resultadosVisibles = computed(() => resultadosBusqueda.value.slice(0, props.maxResultados))

const desplegarResultados = computed(() => mostrarResultados.value && textoBusqueda.value.trim().length > 0)

function seleccionar(persona) {
  if (estaSeleccionada(persona)) return // no se permite seleccionar dos veces a la misma persona
  emit('update:modelValue', [...props.modelValue, persona])
}

function quitar(persona) {
  emit('update:modelValue', props.modelValue.filter((p) => clavePersona(p) !== clavePersona(persona)))
}

// La grilla de resultados vuelve arriba al cambiar el texto.
watch(textoBusqueda, async () => {
  mostrarResultados.value = true
  await nextTick()
  if (grillaResultados.value) grillaResultados.value.scrollTop = 0
})

// El input y la grilla de resultados forman un solo contenedor: la grilla se
// oculta solo cuando se hace clic o se enfoca algo fuera de él (no con @blur,
// que la cerraría antes de procesar el clic en una fila).
function alInteractuarFuera(event) {
  if (contenedorBusqueda.value && !contenedorBusqueda.value.contains(event.target)) {
    mostrarResultados.value = false
  }
}

onMounted(() => {
  cargarCatalogoPersonas() // si ya está en memoria no vuelve a llamar al endpoint
  document.addEventListener('pointerdown', alInteractuarFuera)
  document.addEventListener('focusin', alInteractuarFuera)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', alInteractuarFuera)
  document.removeEventListener('focusin', alInteractuarFuera)
})
</script>

<template>
  <div class="persona-multiselect">
    <!-- Contenedor de búsqueda: input + grilla de resultados (temporal) -->
    <div ref="contenedorBusqueda" class="pms-busqueda">
      <label class="pms-buscador">
        <Icon name="search" :size="15" />
        <input
          v-model="textoBusqueda"
          :placeholder="cargandoPersonas ? 'Cargando personas...' : placeholder"
          :disabled="cargandoPersonas"
          autocomplete="off"
          @focus="mostrarResultados = true"
        >
      </label>

      <div v-if="desplegarResultados" class="pms-resultados">
        <p class="pms-titulo">Resultados de búsqueda</p>
        <p v-if="cargandoPersonas" class="form-hint">Buscando...</p>
        <p v-else-if="errorPersonas" class="conn-status conn-status--offline">
          ⚠ Error al consultar personas: {{ errorPersonas }}
          <button type="button" @click="cargarCatalogoPersonas">Reintentar</button>
        </p>
        <p v-else-if="!resultadosBusqueda.length" class="form-hint">No se encontraron personas.</p>
        <template v-else>
          <div ref="grillaResultados" class="pms-grilla">
            <table>
              <thead>
                <tr><th class="col-codigo">Código</th><th>Persona</th><th>U.O.</th><th>Cargo</th></tr>
              </thead>
              <tbody>
                <!-- Una persona puede figurar en dos U.O. con el mismo codigoPersona: la key combina ambos -->
                <tr
                  v-for="p in resultadosVisibles"
                  :key="`${clavePersona(p)}-${p.codigoUo}`"
                  :class="{ 'ya-seleccionada': estaSeleccionada(p) }"
                  :title="estaSeleccionada(p) ? 'Ya está seleccionada' : 'Seleccionar'"
                  @click="seleccionar(p)"
                >
                  <td class="col-codigo">{{ p.codigoPersona }}</td>
                  <td>{{ p.nombre }}<small v-if="estaSeleccionada(p)"> ✓</small></td>
                  <td>{{ p.unidadOrganica || '—' }}</td>
                  <td>{{ p.cargo || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="resultadosBusqueda.length > resultadosVisibles.length" class="form-hint">
            Mostrando {{ resultadosVisibles.length }} de {{ resultadosBusqueda.length }}. Escribe más para afinar la búsqueda.
          </p>
        </template>
      </div>
    </div>

    <!-- Grilla fija de personas seleccionadas -->
    <p class="pms-titulo">Personas seleccionadas ({{ modelValue.length }})</p>
    <div v-if="modelValue.length" class="pms-grilla pms-grilla--seleccionadas">
      <table>
        <thead>
          <tr><th class="col-codigo">Código</th><th>Persona</th><th>U.O.</th><th>Cargo</th><th class="col-quitar"></th></tr>
        </thead>
        <tbody>
          <tr v-for="p in modelValue" :key="clavePersona(p)">
            <td class="col-codigo">{{ p.codigoPersona }}</td>
            <td>{{ p.nombre }}</td>
            <td>{{ p.unidadOrganica || '—' }}</td>
            <td>{{ p.cargo || '—' }}</td>
            <td class="col-quitar">
              <button type="button" class="pms-quitar" aria-label="Quitar persona" @click="quitar(p)"><Icon name="x" :size="12" /></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="form-hint">Aún no has seleccionado personas.</p>
  </div>
</template>

<style scoped>
.persona-multiselect{display:grid;gap:6px}
.pms-titulo{margin:4px 0 0;color:var(--muted);font-size:11px;font-weight:600}
.pms-buscador{display:flex;align-items:center;gap:8px;padding:0 12px;border:1px solid var(--border);border-radius:10px;color:var(--muted);background:#fff}
.pms-buscador input{flex:1;padding:10px 0;border:0;outline:0;background:none;color:var(--text);font-size:13px;font-family:inherit}
.pms-resultados{margin-top:6px;padding:8px;border:1px solid var(--border);border-radius:10px;background:#f8faff;box-shadow:0 6px 18px #45628b1f}
.pms-resultados .pms-titulo{margin:0 0 6px}
.pms-grilla{max-height:220px;overflow-y:auto;border:1px solid var(--border);border-radius:10px;background:#fff}
.pms-grilla table{width:100%;border-collapse:collapse;font-size:12px}
.pms-grilla th{position:sticky;top:0;z-index:1;padding:9px 10px;color:var(--muted);background:#f4f7fc;font-size:11px;font-weight:600;text-align:left;border-bottom:1px solid var(--border)}
.pms-grilla td{padding:8px 10px;color:var(--text);border-bottom:1px solid var(--border)}
.pms-grilla tbody tr:last-child td{border-bottom:0}
.pms-resultados tbody tr{cursor:pointer}
.pms-resultados tbody tr:hover{background:#eef3ff}
.pms-resultados tbody tr.ya-seleccionada{color:var(--muted);background:#eaf0ff;cursor:default}
.pms-resultados tbody tr.ya-seleccionada td:nth-child(2){color:var(--blue);font-weight:600}
.pms-grilla--seleccionadas{max-height:180px}
.col-codigo{width:60px}
.col-quitar{width:36px;text-align:center}
.pms-quitar{display:inline-grid;place-items:center;padding:4px;border:0;border-radius:6px;color:var(--muted);background:none;cursor:pointer}
.pms-quitar:hover{color:#c0392b;background:#fdecea}
</style>
