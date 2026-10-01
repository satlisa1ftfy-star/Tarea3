<script setup>
import { ref, computed, onMounted } from 'vue'
import Icon from '../Icon.vue'
import { useRegistroRequerimiento } from '../../composables/useRegistroRequerimiento'
import { REGISTRO } from '../../config/registro'

const props = defineProps({
  perfil: { type: Object, default: null },
})
const emit = defineEmits(['cerrar', 'registrado'])

const f = useRegistroRequerimiento(props.perfil)
const mostrarDescartar = ref(false)
const inputArchivo = ref(null)

onMounted(() => f.iniciar())

const tituloRestante = computed(() => REGISTRO.maxTitulo - f.titulo.value.length)
const maxDescripcion = REGISTRO.maxDescripcion

function volver() {
  if (f.modificado.value && !f.resultado.value) mostrarDescartar.value = true
  else emit('cerrar')
}

function descartar() {
  mostrarDescartar.value = false
  f.limpiar()
  emit('cerrar')
}

async function registrar() {
  const ok = await f.registrar()
  if (ok) emit('registrado', f.resultado.value)
}

function alSeleccionarArchivo(evento) {
  f.seleccionarArchivo(evento.target.files?.[0] || null)
  evento.target.value = ''
}

function formatearTamano(bytes) {
  if (!bytes) return ''
  return bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(0)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatearFecha(iso) {
  const fecha = iso ? new Date(iso) : new Date()
  return Number.isNaN(fecha.getTime()) ? String(iso) : fecha.toLocaleString('es-PE')
}

function registrarOtro() {
  f.limpiar()
}

function volverAlTablero() {
  f.limpiar()
  emit('cerrar')
}
</script>

<template>
  <div>
    <button class="detail-back" @click="volver"><Icon name="arrow-left" :size="14" /> Volver al tablero</button>
    <p class="section-eyebrow" style="margin-top:10px">OPERACIONES · REGISTRO</p>

    <div class="greeting greeting--form">
      <div>
        <h1>Registro de Requerimiento</h1>
        <p>Completa los datos para registrar un nuevo requerimiento.</p>
      </div>
      <div class="form-toolbar">
        <button class="modal-btn-secundario" :disabled="f.enviando.value" @click="f.limpiar">Limpiar</button>
        <button
          class="modal-btn-primario"
          :disabled="!f.puedeRegistrar.value"
          :title="f.faltantes.value.length ? 'Falta: ' + f.faltantes.value.join(', ') : 'Registrar requerimiento'"
          @click="registrar"
        >
          {{ f.enviando.value ? 'Registrando…' : 'Registrar' }}
        </button>
      </div>
    </div>

    <p v-if="f.faltantes.value.length" class="form-hint aviso-faltantes">
      Campos obligatorios pendientes: {{ f.faltantes.value.join(', ') }}.
    </p>
    <div v-if="f.errorGeneral.value" class="banner-error" role="alert">
      <strong>{{ f.errorGeneral.value }}</strong>
    </div>
    <p v-if="f.errorCatalogo.value" class="conn-status conn-status--offline">
      ⚠ {{ f.errorCatalogo.value }} <button @click="f.cargarGerencias">Reintentar</button>
    </p>

    <div class="nuevo-req-grid">
      <!-- ─────────── Columna principal ─────────── -->
      <div class="nuevo-req-col">
        <div class="detail-card">
          <div class="detail-card-head">
            <h3>Título del Requerimiento *</h3>
            <span class="form-hint">{{ tituloRestante }} restantes</span>
          </div>
          <p class="form-hint">Describe brevemente el requerimiento en una línea.</p>
          <input
            class="form-input-block"
            v-model="f.titulo.value"
            :maxlength="REGISTRO.maxTitulo"
            placeholder="Ej: Solicitud de acceso al portal Intrasat..."
          />
        </div>

        <div class="detail-card">
          <h3>Unidad Orgánica a quien se solicita</h3>
          <p class="form-hint">Selecciona la unidad orgánica que atenderá el requerimiento.</p>
          <div class="modal-grid-2">
            <div class="form-field">
              <label>GERENCIA CENTRAL / GERENCIA *</label>
              <select
                :value="f.gerenciaId.value ?? ''"
                :disabled="f.cargando.value.gerencias"
                @change="f.onGerencia($event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">{{ f.cargando.value.gerencias ? 'Cargando…' : '— Seleccione —' }}</option>
                <option v-for="u in f.gerencias.value" :key="u.id" :value="u.id">{{ u.nombre }}</option>
              </select>
            </div>

            <div class="form-field">
              <label>DIVISIÓN / UNIDAD</label>
              <select
                :value="f.divisionId.value ?? ''"
                :disabled="!f.gerenciaId.value || f.cargando.value.divisiones || !f.divisiones.value.length"
                @change="f.onDivision($event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">
                  {{ !f.gerenciaId.value ? '— Seleccione —' : f.cargando.value.divisiones ? 'Cargando…' : !f.divisiones.value.length ? 'Sin divisiones' : '— Seleccione —' }}
                </option>
                <option v-for="u in f.divisiones.value" :key="u.id" :value="u.id">{{ u.nombre }}</option>
              </select>
            </div>

            <div class="form-field">
              <label>UNIDAD</label>
              <select
                :value="f.unidadId.value ?? ''"
                :disabled="!f.divisionId.value || f.cargando.value.unidades || !f.unidades.value.length"
                @change="f.onUnidad($event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">
                  {{ !f.divisionId.value ? '— Seleccione —' : f.cargando.value.unidades ? 'Cargando…' : !f.unidades.value.length ? 'Sin unidades' : '— Seleccione —' }}
                </option>
                <option v-for="u in f.unidades.value" :key="u.id" :value="u.id">{{ u.nombre }}</option>
              </select>
            </div>

            <div class="form-field">
              <label>CATEGORÍA *</label>
              <select
                :value="f.categoriaId.value ?? ''"
                :disabled="!f.gerenciaId.value || f.cargando.value.categorias"
                @change="f.onCategoria($event.target.value ? Number($event.target.value) : null)"
              >
                <option value="">
                  {{ !f.gerenciaId.value ? '— Seleccione —' : f.cargando.value.categorias ? 'Cargando…' : !f.categorias.value.length ? 'Sin categorías' : '— Seleccione —' }}
                </option>
                <optgroup v-for="g in f.categoriasPorActivo.value" :key="g.activo" :label="g.activo">
                  <option v-for="c in g.items" :key="c.id" :value="c.id">{{ c.nombre }}</option>
                </optgroup>
              </select>
            </div>
          </div>

          <div class="form-field">
            <label>SUBCATEGORÍA *</label>
            <select
              :value="f.subCategoriaId.value ?? ''"
              :disabled="!f.categoriaId.value || f.cargando.value.subCategorias"
              @change="f.subCategoriaId.value = $event.target.value ? Number($event.target.value) : null"
            >
              <option value="">
                {{ !f.categoriaId.value ? '— Seleccione —' : f.cargando.value.subCategorias ? 'Cargando…' : !f.subCategorias.value.length ? 'Sin subcategorías' : '— Seleccione —' }}
              </option>
              <option v-for="s in f.subCategorias.value" :key="s.id" :value="s.id">{{ s.nombre }}</option>
            </select>
          </div>
        </div>

        <div class="detail-card">
          <div class="detail-card-head">
            <h3>Descripción del Requerimiento *</h3>
            <span class="form-hint">{{ f.descripcion.value.length }} / {{ maxDescripcion }}</span>
          </div>
          <textarea
            class="form-input-block"
            v-model="f.descripcion.value"
            :maxlength="maxDescripcion"
            rows="6"
            placeholder="Describe detalladamente el requerimiento..."
          ></textarea>
          <p class="form-hint">Cada Enter genera un salto de línea.</p>
        </div>

        <div class="detail-card">
          <h3>Documentos Adjuntos</h3>
          <div class="nota-box">
            <strong>Nota:</strong>
            <ul>
              <li>Tamaño máximo {{ REGISTRO.maxBytesAdjunto / 1024 / 1024 }} MB por archivo.</li>
              <li>Seleccionar archivo → Adjuntar. Se admite {{ REGISTRO.maxAdjuntos }} archivo por requerimiento.</li>
            </ul>
          </div>
          <div class="adjuntar-row">
            <input ref="inputArchivo" type="file" class="oculto" @change="alSeleccionarArchivo" />
            <button
              class="modal-btn-secundario"
              :disabled="!f.puedeAdjuntar.value || f.subiendo.value"
              @click="inputArchivo.click()"
            >
              <Icon name="paperclip" :size="14" /> Seleccionar archivo
            </button>
            <span class="form-hint">{{ f.archivoPendiente.value ? f.archivoPendiente.value.name : 'Ningún archivo seleccionado' }}</span>
            <button
              class="modal-btn-primario"
              :disabled="!f.archivoPendiente.value || f.subiendo.value || !f.puedeAdjuntar.value"
              @click="f.adjuntar"
            >
              {{ f.subiendo.value ? 'Subiendo…' : 'Adjuntar' }}
            </button>
          </div>
          <p v-if="f.errorAdjunto.value" class="campo-error">{{ f.errorAdjunto.value }}</p>
          <ul v-if="f.adjuntos.value.length" class="lista-simple">
            <li v-for="(a, i) in f.adjuntos.value" :key="a.archivoTemporalId">
              <Icon name="file-text" :size="14" />
              <span class="lista-simple__texto">{{ a.nombreOriginal }}</span>
              <span class="form-hint">{{ formatearTamano(a.tamanioBytes) }}</span>
              <button class="btn-icono" aria-label="Quitar archivo" @click="f.quitarAdjunto(i)"><Icon name="x" :size="14" /></button>
            </li>
          </ul>
        </div>
      </div>

      <!-- ─────────── Columna lateral ─────────── -->
      <div class="nuevo-req-col nuevo-req-col--lateral">
        <div class="detail-card">
          <h3>Prioridad</h3>
          <div class="prioridad-opcion selected">
            <span class="priority-badge prioridad-chip importante">Importante</span>
          </div>
          <p class="form-hint">Prioridad por defecto. El operador la define al clasificar el requerimiento.</p>
        </div>

        <div class="detail-card">
          <h3>Datos Complementarios</h3>
          <div class="form-field">
            <label>TIPO DE DATO</label>
            <select v-model="f.datoTipoId.value" @change="f.errorDato.value = ''">
              <option :value="null">— Seleccione —</option>
              <option v-for="t in f.tiposDatos" :key="t.id" :value="t.id">{{ t.descripcion }}</option>
            </select>
          </div>
          <div class="form-field">
            <label>VALOR <span v-if="f.tipoDatoActivo.value" class="form-hint">({{ f.tipoDatoActivo.value.formato }})</span></label>
            <div class="fila-agregar">
              <input
                v-model="f.datoValor.value"
                maxlength="50"
                :placeholder="f.tipoDatoActivo.value?.ejemplo ? `Ej: ${f.tipoDatoActivo.value.ejemplo}` : 'Ingrese el valor...'"
                @keydown.enter.prevent="f.agregarDato"
              />
              <button class="modal-btn-secundario" type="button" @click="f.agregarDato"><Icon name="plus" :size="14" /> Agregar</button>
            </div>
          </div>
          <p v-if="f.errorDato.value" class="campo-error">{{ f.errorDato.value }}</p>
          <ul v-if="f.datos.value.length" class="lista-simple">
            <li v-for="(d, i) in f.datos.value" :key="d.tipoDatoId + d.valor">
              <span class="lista-simple__texto"><strong>{{ d.tipoDatoDescripcion }}:</strong> {{ d.valor }}</span>
              <button class="btn-icono" aria-label="Quitar dato" @click="f.quitarDato(i)"><Icon name="x" :size="14" /></button>
            </li>
          </ul>
        </div>

        <div class="detail-card">
          <h3>Correos Copia</h3>
          <div class="form-field combo">
            <label>APELLIDO PATERNO</label>
            <input
              v-model="f.copiasQuery.value"
              :disabled="f.copiasDeshabilitadas.value"
              :placeholder="f.copiasDeshabilitadas.value ? 'No aplica para esta gerencia' : 'Buscar colaborador...'"
              autocomplete="off"
            />
            <ul v-if="f.copiasResultados.value.length || f.buscandoCopias.value" class="combo-resultados">
              <li v-if="f.buscandoCopias.value" class="combo-vacio">Buscando…</li>
              <li v-for="p in f.copiasResultados.value" :key="p.id" @click="f.agregarCopia(p)">
                <strong>{{ p.nombre }}</strong>
                <span class="form-hint">Cód: {{ p.codigoPersonal }} · {{ p.unidadOrganica }}</span>
              </li>
            </ul>
            <p
              v-else-if="f.copiasQuery.value.trim().length >= REGISTRO.minCaracteresBusquedaPersona"
              class="form-hint"
            >
              Sin resultados.
            </p>
          </div>
          <ul v-if="f.copias.value.length" class="chips">
            <li v-for="p in f.copias.value" :key="p.id" class="chip">
              {{ p.nombre }}
              <button aria-label="Quitar" @click="f.quitarCopia(p.id)"><Icon name="x" :size="12" /></button>
            </li>
          </ul>
          <p class="conn-status">(*) Nota: No aplica para la Gerencia de Asuntos Legales.</p>
        </div>
      </div>
    </div>

    <!-- Confirmación al salir con datos sin registrar -->
    <div v-if="mostrarDescartar" class="modal-overlay" @click.self="mostrarDescartar = false">
      <div class="modal-box modal-box--chico">
        <h2>¿Descartar el registro?</h2>
        <p class="form-hint" style="margin-bottom:16px">Hay datos sin registrar. Si sales ahora se perderán.</p>
        <div class="modal-actions">
          <button class="modal-btn-secundario" @click="mostrarDescartar = false">Seguir editando</button>
          <button class="modal-btn-primario" @click="descartar">Descartar</button>
        </div>
      </div>
    </div>

    <!-- Registro exitoso -->
    <div v-if="f.resultado.value" class="modal-overlay">
      <div class="modal-box">
        <h2>¡Requerimiento registrado!</h2>
        <dl class="resumen">
          <dt>N° de requerimiento</dt><dd><strong>{{ f.resultado.value.numeroRequerimiento }}</strong></dd>
          <dt>Fecha y hora</dt><dd>{{ formatearFecha(f.resultado.value.fechaRegistro) }}</dd>
          <dt>Adjuntos</dt><dd>{{ f.resultado.value.totalAdjuntos }}</dd>
          <dt>Copias (CC)</dt><dd>{{ f.resultado.value.totalCopias }}</dd>
        </dl>
        <div class="modal-actions">
          <button class="modal-btn-secundario" @click="registrarOtro">Registrar otro</button>
          <button class="modal-btn-primario" @click="volverAlTablero">Volver al tablero</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.oculto { display: none; }
.aviso-faltantes { margin: -6px 0 10px; }
.banner-error { margin: 0 0 12px; padding: 10px 12px; border: 1px solid #f5c2c7; border-radius: 10px; background: #fdf0f1; color: #a12b36; font-size: 12px; }
.campo-error { margin: 4px 0 0; color: #b42318; font-size: 11px; }
.fila-agregar { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.fila-agregar .modal-btn-secundario { display: inline-flex; align-items: center; gap: 4px; padding: 8px 12px; }
.lista-simple { list-style: none; margin: 10px 0 0; padding: 0; display: grid; gap: 6px; }
.lista-simple li { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid var(--border); border-radius: 9px; background: #fff; font-size: 12px; }
.lista-simple__texto { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btn-icono { border: 0; background: none; color: var(--muted); cursor: pointer; padding: 2px; display: inline-flex; }
.btn-icono:hover { color: #b42318; }
.combo { position: relative; }
.combo-resultados { position: absolute; z-index: 20; top: 100%; left: 0; right: 0; margin: 2px 0 0; padding: 4px; list-style: none; max-height: 240px; overflow-y: auto; border: 1px solid var(--border); border-radius: 10px; background: #fff; box-shadow: 0 8px 24px #0b2d5526; }
.combo-resultados li { display: grid; gap: 1px; padding: 8px 10px; border-radius: 8px; font-size: 12px; cursor: pointer; }
.combo-resultados li:hover { background: #eef3ff; }
.combo-vacio { color: var(--muted); cursor: default !important; }
.chips { list-style: none; margin: 8px 0 0; padding: 0; display: flex; flex-wrap: wrap; gap: 6px; }
.chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px 4px 10px; border-radius: 20px; background: #eef3ff; color: var(--blue); font-size: 11px; font-weight: 600; }
.chip button { border: 0; background: none; color: inherit; cursor: pointer; display: inline-flex; padding: 0; }
.prioridad-chip { margin-left: 0; font-size: 11px; padding: 3px 10px; }
.prioridad-chip.importante { color: #b45309; background: #fef3c7; }
.prioridad-opcion { cursor: default; }
.resumen { display: grid; grid-template-columns: auto 1fr; gap: 8px 16px; margin: 0 0 18px; font-size: 13px; }
.resumen dt { color: var(--muted); }
.resumen dd { margin: 0; text-align: right; }
select:disabled, input:disabled { cursor: not-allowed; }
</style>
