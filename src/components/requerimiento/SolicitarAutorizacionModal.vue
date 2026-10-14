<script setup>
import { computed, ref, watch } from 'vue'
import { usePersonasCatalogo } from '../../controllers/usePersonasCatalogo'
import { solicitarAutorizacion } from '../../services/requerimientoService'
import { cargarCargosAutorizados } from '../../models/cargo'
import { cargarSolicitudesAutorizacion } from '../../models/solicitudAutorizacion'
import Icon from '../Icon.vue'
import SeleccionarPersonasModal from '../persona/SeleccionarPersonasModal.vue'

// -------------------------------------------------------------------------
// Modal de autorizaciones del requerimiento. Según "modo" (lo elige el menú
// "Autorizaciones" del detalle):
//   'solicitar' -> formulario de solicitud (POST /api/requerimiento/solicitar).
//                  La búsqueda de personas se abre en un segundo modal
//                  (SeleccionarPersonasModal) superpuesto a este formulario.
//   'consultar' -> solicitudes registradas (GET /api/requerimiento/consultarSolicitud).
//   'anular'    -> motivo y confirmación de anulación. El backend no expone
//                  todavía una operación para anular (ver ANULACION_DISPONIBLE).

const props = defineProps({
  visible: { type: Boolean, default: false },
  modo: { type: String, default: 'solicitar' },           // 'solicitar' | 'consultar' | 'anular'
  perfil: { type: Object, default: null },
  requerimiento: { type: Object, default: null },         // tarjeta seleccionada (selectedRequest, objeto de aTarjeta)
  estado: { type: String, default: '' },                  // título de la columna (selectedColumn.title)
  codigoRequerimiento: { type: Number, default: null },   // codigoRequerimientoSeleccionado() del Dashboard
  unidadOrganica: { type: Object, default: null },        // ya no se usa: el catálogo trae todas las UO
})
const emit = defineEmits(['cerrar', 'solicitudPreparada'])

const LARGO_MAXIMO_OBSERVACION = 2000 // Nota del sistema antiguo: máx. 2000 caracteres (vObservacion_ReqMov)

// El SP dbo.spGR_Requerimiento_RegistrarPersonaAutorizacion ya anula con siTipBus = 3
// (recibe vCodPerAutorizacion y vObservacion_ReqMov), pero service-requerimiento no
// expone ningún endpoint que lo invoque: /solicitar envía siempre 1 y /autorizar 2.
// Cuando exista el endpoint, agregar su función en requerimientoService.js, llamarla
// en confirmarAnulacion() y cambiar esta constante a true.
const ANULACION_DISPONIBLE = false

const TITULOS = {
  solicitar: 'Solicitar autorización',
  anular: 'Anular autorizaciones',
}
const titulo = computed(() => TITULOS[props.modo] || TITULOS.solicitar)

// personasCatalogo: todas las personas vigentes (compartido, se descarga una sola vez).
// Aquí se usa para convertir los códigos de consultarSolicitud en personas completas;
// la búsqueda la hace SeleccionarPersonasModal sobre el mismo catálogo.
const { personasCatalogo, errorPersonas, cargarCatalogoPersonas } = usePersonasCatalogo()

const motivoAutorizacion = ref('')
const autorizadoresSeleccionados = ref([]) // selección real; v-model de SeleccionarPersonasModal
const mostrarModalBuscarPersonas = ref(false)
const enviandoAutorizacion = ref(false)
const errorAutorizacion = ref('')

// -------------------------------------------------------------------------
// Cargos autorizados (GET /api/requerimiento/buscarCargo): lista informativa,
// independiente de la selección de personas. Categoría y subcategoría vienen
// en card.raw (fila de /buscar, ver aTarjeta en models/dashboard.js).
const cargosAutorizados = ref([])
const cargandoCargos = ref(false)
const errorCargos = ref('')
const faltanDatosCargos = ref(false)

const parametrosCargos = computed(() => ({
  codigoReq: props.codigoRequerimiento,
  codigoCat: props.requerimiento?.raw?.codigoCategoria ?? null,
  codigoSubCat: props.requerimiento?.raw?.codigoSubcategoria ?? null,
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
  if (props.visible && props.modo === 'solicitar') cargarCargos()
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

// -------------------------------------------------------------------------
// Consultar / Anular: muestran tal cual lo que devuelve consultarSolicitud
// ({ codigo, nombre, estado }), sin necesitar el catálogo de personas.
// La descripción original de la solicitud NO viene en esa respuesta: el SP
// spGR_Requerimiento_ConsultarPersonaAutorizacion (siTipBus = 1) solo devuelve
// código, nombres, estado y fechas; el motivo se guarda en
// GRMovRequerimiento.vObservacion_ReqMov, que ninguna consulta expone.
const solicitudesRegistradas = ref([])
const cargandoRegistradas = ref(false)
const errorRegistradas = ref('')
let consultaRegistradasActual = 0

async function cargarSolicitudesRegistradas() {
  const consulta = ++consultaRegistradasActual
  solicitudesRegistradas.value = []
  errorRegistradas.value = ''
  if (props.codigoRequerimiento == null) {
    errorRegistradas.value = 'No se pudo identificar el requerimiento seleccionado.'
    return
  }
  cargandoRegistradas.value = true
  try {
    const solicitudes = await cargarSolicitudesAutorizacion(props.codigoRequerimiento)
    if (consulta === consultaRegistradasActual) solicitudesRegistradas.value = solicitudes
  } catch (error) {
    console.error('No se pudo consultar la solicitud de autorización.', error)
    if (consulta === consultaRegistradasActual) {
      errorRegistradas.value = 'No se pudo consultar la solicitud de autorización. Inténtalo nuevamente.'
    }
  } finally {
    if (consulta === consultaRegistradasActual) cargandoRegistradas.value = false
  }
}

// Anulación: motivo propio (distinto de la descripción original de la solicitud)
// y confirmación explícita; no se ejecuta al elegir la opción del menú.
const motivoAnulacion = ref('')
const confirmacionAnulacion = ref(false)
const procesandoAnulacion = ref(false)
const errorAnulacion = ref('')

const puedeAnular = computed(() =>
  ANULACION_DISPONIBLE &&
  solicitudesRegistradas.value.length > 0 &&
  motivoAnulacion.value.trim().length > 0 &&
  confirmacionAnulacion.value &&
  !procesandoAnulacion.value
)

function confirmarAnulacion() {
  errorAnulacion.value = ''
  if (!ANULACION_DISPONIBLE) {
    errorAnulacion.value = 'La anulación no está disponible: el backend no expone esta operación. No se envió ningún cambio.'
    return
  }
  // Pendiente del endpoint de anulación (ver ANULACION_DISPONIBLE).
}

// -------------------------------------------------------------------------
// Apertura del modal: cada modo carga solo lo que necesita.
// Si el catálogo ya está en memoria, cargarCatalogoPersonas() no vuelve a llamar al endpoint.
function inicializarModal() {
  mostrarModalBuscarPersonas.value = false
  errorAutorizacion.value = ''
  motivoAnulacion.value = ''
  confirmacionAnulacion.value = false
  errorAnulacion.value = ''
  if (props.modo === 'solicitar') {
    motivoAutorizacion.value = ''
    autorizadoresSeleccionados.value = []
    cargarCatalogoPersonas()
    cargarCargos()
    cargarSeleccionInicial()
  } else {
    cargarSolicitudesRegistradas()
  }
}

watch(() => props.visible, (abierto) => {
  if (abierto) inicializarModal()
  else mostrarModalBuscarPersonas.value = false
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

// Segundo modal de búsqueda: se abre encima del formulario, que sigue abierto
// detrás. Cerrarlo no reinicia nada (selección, motivo y cargos se conservan).
function abrirBuscarPersonas() {
  mostrarModalBuscarPersonas.value = true
}

function cerrarBuscarPersonas() {
  mostrarModalBuscarPersonas.value = false
}

const puedeEnviarAutorizacion = computed(() =>
  motivoAutorizacion.value.trim().length > 0 && autorizadoresSeleccionados.value.length > 0
)

async function enviarSolicitudAutorizacion() {
  errorAutorizacion.value = ''
  if (!puedeEnviarAutorizacion.value) return
  // Auditoría (cCodPerActualizacion): cCodPer real del usuario logueado, sin valor fijo.
  if (!props.perfil?.codigoPersonaOrganizacion || !props.perfil?.codigoPersonaGr) {
    errorAutorizacion.value = 'No se pudo identificar al usuario logueado. Vuelve a iniciar sesión.'
    return
  }
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
  <div v-if="visible" class="modal-overlay" @click.self="cerrarModalAutorizacion">
    <div class="modal-box modal-box--ancho">
      <button class="close-detail modal-close" aria-label="Cerrar" @click="cerrarModalAutorizacion"><Icon name="x" :size="18" /></button>
      <h2>{{ titulo }}</h2>

      <div class="modal-grid-2">
        <div class="form-field">
          <label>Requerimiento Nº</label>
          <input :value="requerimiento?.numero" disabled>
        </div>
        <div class="form-field">
          <label>Estado del requerimiento</label>
          <input :value="estado" disabled>
        </div>
      </div>

      <!-- ===================== MODO SOLICITAR ===================== -->
      <template v-if="modo === 'solicitar'">
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

        <!-- Personas autorizadoras seleccionadas (se gestionan en el segundo modal) -->
        <div class="form-field">
          <label>Personas que autorizan *</label>
          <section class="zona-seleccionados">
            <header class="zona-titulo">
              <span><Icon name="user-plus" :size="14" /> Personas seleccionadas</span>
              <b>{{ autorizadoresSeleccionados.length }} {{ autorizadoresSeleccionados.length === 1 ? 'persona seleccionada' : 'personas seleccionadas' }}</b>
            </header>
            <p v-if="consultandoSolicitudes" class="form-hint">Consultando autorizaciones...</p>
            <p v-if="errorSolicitudes" class="conn-status conn-status--offline">⚠ {{ errorSolicitudes }}</p>
            <div v-if="autorizadoresSeleccionados.length" class="autorizadores-grilla seleccionados-grilla">
              <table>
                <thead>
                  <tr><th>Persona</th><th>Cargo</th><th>Unidad</th></tr>
                </thead>
                <tbody>
                  <tr v-for="p in autorizadoresSeleccionados" :key="claveAutorizador(p)">
                    <td>{{ p.nombre }}</td>
                    <td>{{ p.cargo || '—' }}</td>
                    <td>{{ p.unidadOrganica || '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else-if="!consultandoSolicitudes" class="form-hint">Aún no has seleccionado personas.</p>
            <button type="button" class="btn-gestionar" @click="abrirBuscarPersonas">
              <Icon name="search" :size="14" /> Buscar o gestionar personas
            </button>
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
      </template>

      <!-- ===================== MODO CONSULTAR / ANULAR ===================== -->
      <template v-else>
        <!-- Descripción original de la solicitud (solo lectura) -->
        <div v-if="modo === 'consultar'" class="form-field">
          <label>Descripción de la solicitud</label>
          <div class="descripcion-solicitud descripcion-solicitud--no-disponible">
            La descripción no está disponible: el servicio de consulta de solicitudes no la devuelve.
          </div>
        </div>

        <div class="form-field">
          <label>Personas a las que se solicitó autorización</label>
          <p v-if="cargandoRegistradas" class="form-hint">Consultando solicitud...</p>
          <p v-else-if="errorRegistradas" class="conn-status conn-status--offline">
            ⚠ {{ errorRegistradas }}
            <button type="button" @click="cargarSolicitudesRegistradas">Reintentar</button>
          </p>
          <p v-else-if="!solicitudesRegistradas.length" class="form-hint aviso-info">
            No hay solicitudes de autorización registradas para este requerimiento.
          </p>
          <div v-else class="autorizadores-grilla registradas-grilla">
            <table>
              <thead>
                <tr><th class="col-codigo">Código</th><th>Nombre</th><th class="col-estado">Estado</th></tr>
              </thead>
              <tbody>
                <tr v-for="s in solicitudesRegistradas" :key="s.codigo">
                  <td class="col-codigo">{{ s.codigo }}</td>
                  <td>{{ s.nombre }}</td>
                  <td class="col-estado"><span class="estado-pill">{{ s.estado || '—' }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Anular: motivo de anulación (editable) + confirmación explícita -->
        <template v-if="modo === 'anular' && !cargandoRegistradas && !errorRegistradas && solicitudesRegistradas.length">
          <div class="form-field">
            <label>Motivo de anulación *</label>
            <textarea
              v-model="motivoAnulacion"
              :maxlength="LARGO_MAXIMO_OBSERVACION"
              rows="3"
              placeholder="Indica el motivo por el que se solicita anular esta autorización..."
            ></textarea>
            <p class="form-hint">{{ motivoAnulacion.length }}/{{ LARGO_MAXIMO_OBSERVACION }} caracteres</p>
          </div>

          <div class="aviso-anulacion">
            <strong>¿Anular la solicitud de autorización del requerimiento {{ requerimiento?.numero }}?</strong>
            <p>
              Afectará a {{ solicitudesRegistradas.length }}
              {{ solicitudesRegistradas.length === 1 ? 'persona' : 'personas' }} listadas arriba.
            </p>
            <p v-if="!ANULACION_DISPONIBLE" class="aviso-backend">
              La anulación todavía no está disponible: service-requerimiento no expone una operación
              para anular solicitudes de autorización. No se enviará ningún cambio.
            </p>
            <label class="form-checkbox">
              <input type="checkbox" v-model="confirmacionAnulacion" :disabled="!ANULACION_DISPONIBLE">
              Confirmo que deseo anular esta solicitud.
            </label>
          </div>
          <p v-if="errorAnulacion" class="conn-status conn-status--offline">⚠ {{ errorAnulacion }}</p>
        </template>

        <div class="modal-actions">
          <button class="modal-btn-secundario" :disabled="procesandoAnulacion" @click="cerrarModalAutorizacion">{{ modo === 'anular' ? 'Cancelar' : 'Cerrar' }}</button>
          <button
            v-if="modo === 'anular' && solicitudesRegistradas.length"
            class="modal-btn-peligro"
            :disabled="!puedeAnular"
            :title="ANULACION_DISPONIBLE ? '' : 'No disponible: falta el endpoint de anulación en el backend'"
            @click="confirmarAnulacion"
          >{{ procesandoAnulacion ? 'Anulando...' : 'Anular autorizaciones' }}</button>
        </div>
      </template>
    </div>
  </div>

  <!-- Segundo modal: búsqueda y selección de personas, encima del formulario de solicitud -->
  <SeleccionarPersonasModal
    v-if="visible && modo === 'solicitar' && mostrarModalBuscarPersonas"
    v-model="autorizadoresSeleccionados"
    titulo="Seleccionar personas autorizadoras"
    :descripcion="`Requerimiento: ${requerimiento?.numero || ''}`"
    @confirmar="cerrarBuscarPersonas"
    @cerrar="cerrarBuscarPersonas"
  />
</template>

<style scoped>
.autorizadores-grilla{max-height:220px;margin-top:6px;overflow-y:auto;border:1px solid var(--border);border-radius:10px;background:#fff}
.autorizadores-grilla table{width:100%;border-collapse:collapse;font-size:12px}
.autorizadores-grilla th{position:sticky;top:0;z-index:1;padding:9px 10px;color:var(--muted);background:#f4f7fc;font-size:11px;font-weight:600;text-align:left;border-bottom:1px solid var(--border)}
.autorizadores-grilla td{padding:8px 10px;color:var(--text);border-bottom:1px solid var(--border)}
.autorizadores-grilla tbody tr:last-child td{border-bottom:0}
.zona-titulo{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:6px;color:var(--text);font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.04em}
.zona-titulo span{display:inline-flex;align-items:center;gap:6px}
.zona-titulo b{color:var(--blue);font-size:11.5px;font-weight:600;text-transform:none;letter-spacing:0}
/* Selección real: bloque fijo, borde sólido azul */
.zona-seleccionados{margin-top:4px;padding:10px;border:1.5px solid var(--blue);border-radius:12px;background:#fff}
.seleccionados-grilla{max-height:180px;margin-top:0}
.btn-gestionar{display:inline-flex;align-items:center;gap:6px;margin-top:10px;padding:8px 14px;border:1px solid var(--blue);border-radius:9px;color:#fff;background:var(--blue);font-size:12px;font-weight:700;cursor:pointer}
.btn-gestionar:hover{background:var(--blue-dark,#1d3f9e)}
.cargos-grilla{max-height:140px}
.cargos-grilla .col-codigo{width:70px}
.cargos-grilla .col-aprob{width:60px;text-align:center}
/* Consultar / Anular */
.descripcion-solicitud{max-height:140px;overflow-y:auto;padding:10px 12px;border:1px solid var(--border);border-radius:10px;background:#f4f7fc;color:var(--text);font-size:12.5px;white-space:pre-wrap;word-break:break-word}
.descripcion-solicitud--no-disponible{color:var(--muted);font-style:italic}
.registradas-grilla{max-height:260px}
.registradas-grilla .col-codigo{width:70px}
.registradas-grilla .col-estado{width:130px}
.estado-pill{display:inline-block;padding:3px 9px;border-radius:20px;color:var(--blue);background:#eaf0ff;font-size:11px;font-weight:600}
.aviso-info{padding:10px 12px;border-radius:10px;background:#f4f7fc}
.aviso-anulacion{margin:4px 0 12px;padding:12px 14px;border:1px solid #f1c4bf;border-radius:12px;background:#fff7f6;color:var(--text);font-size:12.5px}
.aviso-anulacion p{margin:6px 0}
.aviso-backend{color:#a8431e;font-weight:600}
.modal-btn-peligro{padding:10px 18px;border:1px solid #c0392b;border-radius:10px;color:#fff;background:#c0392b;font-size:12px;font-weight:700;cursor:pointer}
.modal-btn-peligro:disabled{opacity:.5;cursor:not-allowed}
</style>
