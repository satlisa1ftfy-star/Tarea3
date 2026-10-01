<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import Icon from '../Icon.vue'
import { PRIORIDADES, listarCategorias, listarSubCategorias } from '../../services/catalogoService'
import { clasificarRequerimiento, obtenerParaClasificar } from '../../services/requerimientoService'

const props = defineProps({
  codigoRequerimiento: { type: Number, required: true },
  perfil: { type: Object, default: null },
})
const emit = defineEmits(['cerrar', 'clasificado'])

const LARGO_MAXIMO_OBSERVACION = 2000

const detalle = ref(null)
const cargando = ref(true)
const error = ref('')
const guardando = ref(false)

const categorias = ref([])
const subCategorias = ref([])
const cargandoSubCategorias = ref(false)
const categoriaId = ref(null)
const subCategoriaId = ref(null)
const prioridadId = ref(32)
const observacion = ref('')

// La descripción viene de usuarios y del sistema legado: se quita lo ejecutable antes de mostrarla.
function sanear(html) {
  const doc = new DOMParser().parseFromString(String(html || ''), 'text/html')
  doc.querySelectorAll('script,style,iframe,object,embed,link,meta,form').forEach((n) => n.remove())
  doc.body.querySelectorAll('*').forEach((el) => {
    for (const attr of [...el.attributes]) {
      const nombre = attr.name.toLowerCase()
      const valor = attr.value.trim().toLowerCase()
      if (nombre.startsWith('on') || nombre === 'style' || valor.startsWith('javascript:')) {
        el.removeAttribute(attr.name)
      }
    }
  })
  return doc.body.innerHTML
}

const descripcionSegura = computed(() => sanear(detalle.value?.descripcionHtml))

const categoriasPorActivo = computed(() => {
  const grupos = new Map()
  for (const c of categorias.value) {
    if (!grupos.has(c.activoNombre)) grupos.set(c.activoNombre, [])
    grupos.get(c.activoNombre).push(c)
  }
  return [...grupos].map(([activo, items]) => ({ activo, items }))
})

const puedeGuardar = computed(
  () => !!categoriaId.value && !!subCategoriaId.value && !!prioridadId.value && !guardando.value
)

async function cargarSubCategorias(idCategoria) {
  subCategorias.value = []
  if (!idCategoria) return
  cargandoSubCategorias.value = true
  try {
    subCategorias.value = await listarSubCategorias(idCategoria)
  } catch (e) {
    error.value = e?.message || 'No se pudieron cargar las subcategorías.'
  } finally {
    cargandoSubCategorias.value = false
  }
}

// Al cambiar de categoría se descarta la subcategoría elegida.
function alCambiarCategoria(valor) {
  categoriaId.value = valor ? Number(valor) : null
  subCategoriaId.value = null
  cargarSubCategorias(categoriaId.value)
}

onMounted(async () => {
  try {
    detalle.value = await obtenerParaClasificar(props.codigoRequerimiento)
    prioridadId.value = detalle.value.prioridadId || 32

    // Las categorías son de la unidad destino; si no tiene, se muestran todas.
    let lista = await listarCategorias(detalle.value.unidadOrganicaDestinoId || 0)
    if (!lista.length) lista = await listarCategorias(0)
    categorias.value = lista

    // Si ya tenía categoría (requerimiento reclasificado) se precarga; 0 = «No Clasificado».
    if (detalle.value.categoriaId > 0 && lista.some((c) => c.id === detalle.value.categoriaId)) {
      categoriaId.value = detalle.value.categoriaId
      await cargarSubCategorias(categoriaId.value)
      if (subCategorias.value.some((s) => s.id === detalle.value.subCategoriaId)) {
        subCategoriaId.value = detalle.value.subCategoriaId
      }
    }
  } catch (e) {
    error.value = e?.message || 'No se pudo cargar el requerimiento.'
  } finally {
    cargando.value = false
  }
})

watch(observacion, (v) => {
  if (v.length > LARGO_MAXIMO_OBSERVACION) observacion.value = v.slice(0, LARGO_MAXIMO_OBSERVACION)
})

async function guardar() {
  if (!puedeGuardar.value) return
  if (!props.perfil?.codigoPersonaOrganizacion) {
    error.value = 'No se pudo identificar al usuario logueado. Vuelva a iniciar sesión.'
    return
  }
  guardando.value = true
  error.value = ''
  try {
    await clasificarRequerimiento(props.codigoRequerimiento, {
      categoriaId: categoriaId.value,
      subCategoriaId: subCategoriaId.value,
      prioridadId: prioridadId.value,
      observacion: observacion.value.trim(),
      codigoPersonaActualizacion: props.perfil.codigoPersonaOrganizacion,
    })
    emit('clasificado')
  } catch (e) {
    error.value = e?.message || 'No se pudo clasificar el requerimiento.'
  } finally {
    guardando.value = false
  }
}

function formatearFecha(iso) {
  const fecha = iso ? new Date(iso) : null
  return fecha && !Number.isNaN(fecha.getTime()) ? fecha.toLocaleString('es-PE') : '-'
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('cerrar')">
    <div class="modal-box modal-box--ancho">
      <button class="close-detail modal-close" aria-label="Cerrar" @click="emit('cerrar')"><Icon name="x" :size="18" /></button>
      <h2>Clasificar requerimiento</h2>

      <p v-if="cargando" class="conn-status">Cargando requerimiento…</p>

      <template v-else-if="detalle">
        <dl class="clasif-datos">
          <div><dt>N° Requerimiento</dt><dd>{{ detalle.numeroRequerimiento }}</dd></div>
          <div><dt>Fecha</dt><dd>{{ formatearFecha(detalle.fechaRequerimiento) }}</dd></div>
          <div><dt>Estado</dt><dd>{{ detalle.estadoActual }}</dd></div>
          <div><dt>Unidad destino</dt><dd>{{ detalle.unidadOrganicaDestinoNombre || '-' }}</dd></div>
          <div class="ancho"><dt>Solicitante</dt><dd>{{ detalle.solicitanteNombreCompleto || '-' }} · {{ detalle.unidadOrganicaSolicitante || '-' }}<template v-if="detalle.cargoSolicitante"> · {{ detalle.cargoSolicitante }}</template></dd></div>
          <div class="ancho"><dt>Sumilla</dt><dd>{{ detalle.sumilla || '-' }}</dd></div>
        </dl>

        <div class="form-field">
          <label>Descripción</label>
          <div class="clasif-descripcion" v-html="descripcionSegura"></div>
        </div>

        <p v-if="detalle.documentosAdjuntos?.length" class="form-hint">
          Adjuntos: {{ detalle.documentosAdjuntos.map((a) => a.nombreArchivo).join(', ') }}
        </p>
        <p v-if="detalle.datosComplementarios?.length" class="form-hint">
          Datos complementarios: {{ detalle.datosComplementarios.map((d) => `${d.tipoDatoDescripcion}: ${d.valor}`).join(' · ') }}
        </p>

        <div class="modal-grid-2">
          <div class="form-field">
            <label>Categoría *</label>
            <select :value="categoriaId ?? ''" @change="alCambiarCategoria($event.target.value)">
              <option value="">— Seleccione —</option>
              <optgroup v-for="g in categoriasPorActivo" :key="g.activo" :label="g.activo">
                <option v-for="c in g.items" :key="c.id" :value="c.id">{{ c.nombre }}</option>
              </optgroup>
            </select>
          </div>
          <div class="form-field">
            <label>Subcategoría *</label>
            <select v-model="subCategoriaId" :disabled="!categoriaId || cargandoSubCategorias">
              <option :value="null">
                {{ !categoriaId ? '— Seleccione —' : cargandoSubCategorias ? 'Cargando…' : !subCategorias.length ? 'Sin subcategorías' : '— Seleccione —' }}
              </option>
              <option v-for="s in subCategorias" :key="s.id" :value="s.id">{{ s.nombre }}</option>
            </select>
          </div>
        </div>

        <div class="form-field">
          <label>Prioridad *</label>
          <select v-model="prioridadId">
            <option v-for="p in PRIORIDADES" :key="p.id" :value="p.id">{{ p.nombre }}</option>
          </select>
        </div>

        <div class="form-field">
          <label>Observaciones</label>
          <textarea v-model="observacion" :maxlength="LARGO_MAXIMO_OBSERVACION" rows="3" placeholder="Notas sobre la clasificación (opcional)..."></textarea>
          <p class="form-hint">{{ observacion.length }}/{{ LARGO_MAXIMO_OBSERVACION }} caracteres</p>
        </div>
      </template>

      <p v-if="error" class="conn-status conn-status--offline">⚠ {{ error }}</p>

      <div class="modal-actions">
        <button class="modal-btn-secundario" :disabled="guardando" @click="emit('cerrar')">Retornar</button>
        <button class="modal-btn-primario" :disabled="!detalle || !puedeGuardar" @click="guardar">
          {{ guardando ? 'Clasificando…' : 'Clasificar' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.clasif-datos { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; margin: 0 0 14px; font-size: 12px; }
.clasif-datos div { min-width: 0; }
.clasif-datos .ancho { grid-column: 1 / -1; }
.clasif-datos dt { color: var(--muted); font-size: 11px; font-weight: 600; }
.clasif-datos dd { margin: 2px 0 0; word-break: break-word; }
.clasif-descripcion { max-height: 160px; overflow: auto; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; background: #f8faff; font-size: 12px; }
.clasif-descripcion :deep(table) { max-width: 100%; }
/* evita el desborde horizontal cuando un select tiene opciones largas */
.modal-box :deep(.modal-grid-2 > *) { min-width: 0; }
.modal-box :deep(select) { max-width: 100%; }
</style>
