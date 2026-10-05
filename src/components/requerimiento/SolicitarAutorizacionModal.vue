<script setup>
import { computed, ref, watch } from 'vue'
import { listarPersonas } from '../../services/personaService'
import Icon from '../Icon.vue'

// -------------------------------------------------------------------------
// Solicitar autorización: modal con Motivo (obligatorio) y selección múltiple
// de personas que autorizan. Las personas se cargan de la UO del rol activo
// (GET /api/personas). Aún no existe endpoint para registrar la solicitud en
// service-requerimiento, así que el envío solo arma el payload.   ***********PENDIENTE***********

const props = defineProps({
  visible: { type: Boolean, default: false },
  perfil: { type: Object, default: null },
  requerimiento: { type: Array, default: null },          // tarjeta seleccionada (selectedRequest)
  estado: { type: String, default: '' },                  // título de la columna (selectedColumn.title)
  codigoRequerimiento: { type: Number, default: null },   // codigoRequerimientoSeleccionado() del Dashboard
  unidadOrganica: { type: Object, default: null },        // unidadOrganicaPropia del Dashboard
})
const emit = defineEmits(['cerrar', 'solicitudPreparada'])

const LARGO_MAXIMO_OBSERVACION = 2000 // Nota del sistema antiguo: máx. 2000 caracteres

// Lista de referencia usada solo si no hay UO en el perfil o falla /api/personas.
const PERSONAS_AUTORIZADORAS_MOCK = [
  { codigoPersona: 'MOCK-1', nombre: 'Gordon David', cargo: 'Gerente (mock)' },
  { codigoPersona: 'MOCK-2', nombre: 'Cruz Blades', cargo: 'Jefe de División (mock)' },
  { codigoPersona: 'MOCK-3', nombre: 'Daniel Blades', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-4', nombre: 'Galvez Dionisio', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-5', nombre: 'Gustavo Dionisio', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-6', nombre: 'Gian Dionisio', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-7', nombre: 'Luis Dionisio', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-8', nombre: 'Jose Dionisio', cargo: 'Jefe de Unidad (mock)' },
  { codigoPersona: 'MOCK-9', nombre: 'Danye Dionisio', cargo: 'Jefe de Unidad (mock)' },
]

const motivoAutorizacion = ref('')
const personasAutorizacion = ref([])
const autorizadoresSeleccionados = ref([])
const autorizadorTexto = ref('')
const cargandoAutorizadores = ref(false)
const usandoMockAutorizadores = ref(false)
const enviandoAutorizacion = ref(false)
const errorAutorizacion = ref('')

// Cada vez que se abre el modal: limpia el formulario y carga las personas.
async function inicializarModal() {
  motivoAutorizacion.value = ''
  autorizadoresSeleccionados.value = []
  autorizadorTexto.value = ''
  errorAutorizacion.value = ''

  const uo = props.unidadOrganica
  usandoMockAutorizadores.value = false
  if (!uo) {
    personasAutorizacion.value = PERSONAS_AUTORIZADORAS_MOCK
    usandoMockAutorizadores.value = true
    return
  }
  cargandoAutorizadores.value = true
  try {
    personasAutorizacion.value = await listarPersonas({ codigoUo: uo.codigoUo, vigencia: 1 })
  } catch (error) {
    console.error('No se pudieron cargar las personas para autorizar.', error)
    personasAutorizacion.value = PERSONAS_AUTORIZADORAS_MOCK
    usandoMockAutorizadores.value = true
  } finally {
    cargandoAutorizadores.value = false
  }
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

// Filtro local sobre la lista cargada (nombre, cargo y unidad orgánica, campos
// que devuelve GET /api/personas). Solo decide qué filas se ven en la grilla:
// no toca personasAutorizacion ni autorizadoresSeleccionados.
const autorizadoresFiltrados = computed(() => {
  const texto = autorizadorTexto.value.trim().toLowerCase()
  if (!texto) return personasAutorizacion.value
  return personasAutorizacion.value.filter((p) =>
    [p.nombre, p.cargo, p.unidadOrganica].some((campo) => (campo || '').toLowerCase().includes(texto))
  )
})

// Las columnas Cargo y Unidad Orgánica se muestran solo si algún registro las trae
// (el MOCK no tiene unidad orgánica).
const mostrarColumnaCargo = computed(() => personasAutorizacion.value.some((p) => p.cargo))
const mostrarColumnaUnidad = computed(() => personasAutorizacion.value.some((p) => p.unidadOrganica))

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
    // TODO: reemplazar por la llamada al endpoint cuando exista en service-requerimiento.
    console.info('Solicitud de autorización (pendiente de endpoint):', payload)
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

      <div class="form-field">
        <label>Personas que autorizan *</label>

        <!-- Seleccionados: sale de autorizadoresSeleccionados, no de la grilla filtrada -->
        <div class="autorizadores-seleccionados">
          <span class="autorizadores-subtitulo">Seleccionados:</span>
          <div v-if="autorizadoresSeleccionados.length" class="autorizadores-chips">
            <span v-for="p in autorizadoresSeleccionados" :key="claveAutorizador(p)" class="filtro-chip">
              {{ p.nombre }}
              <button type="button" @click="alternarAutorizador(p)" aria-label="Quitar persona"><Icon name="x" :size="10" /></button>
            </span>
          </div>
          <p v-else class="form-hint">Aún no has seleccionado personas.</p>
        </div>

        <span class="autorizadores-subtitulo">Buscar persona</span>
        <label class="autorizadores-buscador">
          <Icon name="search" :size="15" />
          <input
            v-model="autorizadorTexto"
            :placeholder="cargandoAutorizadores ? 'Cargando...' : 'Nombre, cargo o unidad...'"
            :disabled="cargandoAutorizadores"
            autocomplete="off"
          >
        </label>

        <div v-if="!cargandoAutorizadores && autorizadoresFiltrados.length" class="autorizadores-grilla">
          <table>
            <thead>
              <tr>
                <th class="col-check"></th>
                <th>Persona</th>
                <th v-if="mostrarColumnaCargo">Cargo</th>
                <th v-if="mostrarColumnaUnidad">Unidad Orgánica</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="p in autorizadoresFiltrados"
                :key="claveAutorizador(p)"
                :class="{ seleccionada: estaSeleccionadoAutorizador(p) }"
                @click="alternarAutorizador(p)"
              >
                <td class="col-check">
                  <input type="checkbox" :checked="estaSeleccionadoAutorizador(p)" @click.stop @change="alternarAutorizador(p)">
                </td>
                <td>{{ p.nombre }}</td>
                <td v-if="mostrarColumnaCargo">{{ p.cargo || '—' }}</td>
                <td v-if="mostrarColumnaUnidad">{{ p.unidadOrganica || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else-if="!cargandoAutorizadores" class="form-hint">No hay personas que coincidan.</p>

        <p class="autorizadores-contador">
          {{ autorizadoresSeleccionados.length }} {{ autorizadoresSeleccionados.length === 1 ? 'persona seleccionada' : 'personas seleccionadas' }}
        </p>
        <p v-if="usandoMockAutorizadores" class="form-hint">Mostrando lista de prueba: no se pudo obtener el personal de tu unidad orgánica.</p>
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
.autorizadores-seleccionados{display:grid;gap:6px;margin-bottom:6px}
.autorizadores-subtitulo{color:var(--muted);font-size:11px;font-weight:600}
.autorizadores-buscador{display:flex;align-items:center;gap:8px;padding:0 12px;border:1px solid var(--border);border-radius:10px;color:var(--muted);background:#fff}
.form-field .autorizadores-buscador input{flex:1;padding:10px 0;border:0;outline:0;background:none}
.autorizadores-grilla{max-height:220px;margin-top:6px;overflow-y:auto;border:1px solid var(--border);border-radius:10px;background:#fff}
.autorizadores-grilla table{width:100%;border-collapse:collapse;font-size:12px}
.autorizadores-grilla th{position:sticky;top:0;z-index:1;padding:9px 10px;color:var(--muted);background:#f4f7fc;font-size:11px;font-weight:600;text-align:left;border-bottom:1px solid var(--border)}
.autorizadores-grilla td{padding:8px 10px;color:var(--text);border-bottom:1px solid var(--border)}
.autorizadores-grilla tbody tr{cursor:pointer}
.autorizadores-grilla tbody tr:last-child td{border-bottom:0}
.autorizadores-grilla tbody tr:hover{background:#eef3ff}
.autorizadores-grilla tbody tr.seleccionada{background:#eaf0ff}
.autorizadores-grilla tbody tr.seleccionada td:nth-child(2){color:var(--blue);font-weight:600}
.autorizadores-grilla .col-check{width:36px;text-align:center}
.form-field .autorizadores-grilla input[type="checkbox"]{width:auto;margin:0;padding:0}
.autorizadores-contador{margin:2px 0 0;color:var(--muted);font-size:11.5px;font-weight:600}
</style>
