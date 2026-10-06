<script setup>
import { computed, ref, watch } from 'vue'
import { normalizarTexto, usePersonasCatalogo } from '../../controllers/usePersonasCatalogo'
import { solicitarAutorizacion } from '../../services/requerimientoService'
import { cargarCargosAutorizados } from '../../models/cargo'
import Icon from '../Icon.vue'

// -------------------------------------------------------------------------
// Solicitar autorización: modal con Motivo (obligatorio) y selección múltiple
// de personas que autorizan. Las personas salen del catálogo en memoria
// (GET /api/personas?vigencia=1, descargado una sola vez) y la solicitud se
// envía a POST /api/requerimiento/solicitar.

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
const autorizadorTexto = ref('')
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

// Cada vez que se abre el modal: limpia el formulario y pide el catálogo.
// Si ya está en memoria, cargarCatalogoPersonas() no vuelve a llamar al endpoint.
function inicializarModal() {
  motivoAutorizacion.value = ''
  autorizadoresSeleccionados.value = []
  autorizadorTexto.value = ''
  errorAutorizacion.value = ''
  cargarCatalogoPersonas()
  cargarCargos()
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

// personasFiltradas: filtro en memoria por nombre, cargo y unidad orgánica, sin
// distinguir mayúsculas ni tildes. Solo decide qué filas se ven en la grilla:
// no toca personasCatalogo ni autorizadoresSeleccionados.
const personasFiltradas = computed(() => {
  const texto = normalizarTexto(autorizadorTexto.value.trim())
  if (!texto) return personasCatalogo.value
  return personasCatalogo.value.filter((p) =>
    normalizarTexto(p.nombre).includes(texto) ||
    normalizarTexto(p.cargo).includes(texto) ||
    normalizarTexto(p.unidadOrganica).includes(texto)
  )
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
      codPerSolAutorizacion: payload.codigoPersonaSolicita != null ? String(payload.codigoPersonaSolicita) : null,
      codPerHelpDesk: payload.codigoPersonaSolicita,
      codPerActualiza: payload.codigoPersonaActualizacion,
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
            :placeholder="cargandoPersonas ? 'Cargando personas...' : 'Nombre, cargo o unidad...'"
            :disabled="cargandoPersonas"
            autocomplete="off"
          >
        </label>

        <p v-if="cargandoPersonas" class="form-hint">Cargando personas...</p>
        <p v-else-if="errorPersonas" class="conn-status conn-status--offline">
          ⚠ No se pudo cargar el catálogo de personas: {{ errorPersonas }}
          <button type="button" @click="cargarCatalogoPersonas">Reintentar</button>
        </p>
        <div v-else-if="personasFiltradas.length" class="autorizadores-grilla">
          <table>
            <thead>
              <tr>
                <th class="col-check"></th>
                <th>Persona</th>
                <th>Cargo</th>
                <th>Unidad Orgánica</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="p in personasFiltradas"
                :key="claveAutorizador(p)"
                :class="{ seleccionada: estaSeleccionadoAutorizador(p) }"
                @click="alternarAutorizador(p)"
              >
                <td class="col-check">
                  <input type="checkbox" :checked="estaSeleccionadoAutorizador(p)" @click.stop @change="alternarAutorizador(p)">
                </td>
                <td>{{ p.nombre }}</td>
                <td>{{ p.cargo || '—' }}</td>
                <td>{{ p.unidadOrganica || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="form-hint">No hay personas que coincidan.</p>

        <p class="autorizadores-contador">
          {{ autorizadoresSeleccionados.length }} {{ autorizadoresSeleccionados.length === 1 ? 'persona seleccionada' : 'personas seleccionadas' }}
        </p>
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
.cargos-grilla{max-height:140px}
.cargos-grilla tbody tr{cursor:default}
.cargos-grilla tbody tr:hover{background:none}
.cargos-grilla .col-codigo{width:70px}
.cargos-grilla .col-aprob{width:60px;text-align:center}
.autorizadores-contador{margin:2px 0 0;color:var(--muted);font-size:11.5px;font-weight:600}
</style>
