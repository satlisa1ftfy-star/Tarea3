/**
 * Lógica del formulario "Registro de Requerimiento" (sin nada visual).
 *
 * Cascada de unidades (Gerencia → División → Unidad), categorías (con retroceso al nivel superior),
 * datos complementarios con máscara, copias CC (cCodPer), adjunto, validación, armado del payload y envío.
 * `perfil` es el usuario logueado (codigoPersonaGr = solicitante, codigoPersonaOrganizacion = cCodPer de auditoría).
 */
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import {
  TIPOS_DATOS_COMPLEMENTARIOS,
  buscarPersonas,
  listarCategorias,
  listarDependencias,
  listarSubCategorias,
  listarUnidadesSolicitud,
} from '../services/catalogoService'
import { registrarRequerimiento, subirAdjuntoTemporal } from '../services/requerimientoService'
import { REGISTRO } from '../config/registro'

function textoAHtml(texto) {
  const escapado = String(texto || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return escapado
    .split(/\r?\n/)
    .map((linea) => `<p>${linea || '&nbsp;'}</p>`)
    .join('')
}

export function useRegistroRequerimiento(perfil) {
  // ─── Datos del formulario ────────────────────────────────────────────────
  const titulo = ref('')
  const descripcion = ref('')

  const gerenciaId = ref(null)
  const divisionId = ref(null)
  const unidadId = ref(null)
  const categoriaId = ref(null)
  const subCategoriaId = ref(null)
  const activoId = ref(null)

  const datos = ref([]) // { tipoDatoId, tipoDatoDescripcion, valor }
  const datoTipoId = ref(null)
  const datoValor = ref('')
  const errorDato = ref('')

  const copias = ref([]) // personas
  const copiasQuery = ref('')
  const copiasResultados = ref([])
  const buscandoCopias = ref(false)

  const adjuntos = ref([]) // { archivoTemporalId, nombreOriginal, tamanioBytes }
  const archivoPendiente = ref(null)
  const subiendo = ref(false)
  const errorAdjunto = ref('')

  // ─── Catálogos ───────────────────────────────────────────────────────────
  const gerencias = ref([])
  const divisiones = ref([])
  const unidades = ref([])
  const categorias = ref([])
  const subCategorias = ref([])
  const tiposDatos = TIPOS_DATOS_COMPLEMENTARIOS

  const cargando = ref({ gerencias: false, divisiones: false, unidades: false, categorias: false, subCategorias: false })
  const errorCatalogo = ref('')

  // ─── Estado del envío ────────────────────────────────────────────────────
  const enviando = ref(false)
  const errorGeneral = ref('')
  const resultado = ref(null)

  // ─── Cascada de unidades ─────────────────────────────────────────────────
  let secCategorias = 0
  let secSubCategorias = 0

  async function cargarGerencias() {
    cargando.value.gerencias = true
    errorCatalogo.value = ''
    try {
      gerencias.value = await listarUnidadesSolicitud()
      if (!gerencias.value.length) errorCatalogo.value = 'El servicio no devolvió unidades orgánicas.'
    } catch (err) {
      errorCatalogo.value = err.message || 'No se pudieron cargar las unidades orgánicas.'
    } finally {
      cargando.value.gerencias = false
    }
  }

  async function cargarHijos(padreId, destino, bandera) {
    destino.value = []
    if (!padreId) return
    cargando.value[bandera] = true
    try {
      destino.value = await listarDependencias(padreId)
    } catch (err) {
      errorCatalogo.value = err.message || 'No se pudieron cargar las dependencias.'
    } finally {
      cargando.value[bandera] = false
    }
  }

  async function onGerencia(id) {
    gerenciaId.value = id || null
    divisionId.value = null
    unidadId.value = null
    unidades.value = []
    await Promise.all([cargarHijos(gerenciaId.value, divisiones, 'divisiones'), recargarCategorias()])
  }

  async function onDivision(id) {
    divisionId.value = id || null
    unidadId.value = null
    await Promise.all([cargarHijos(divisionId.value, unidades, 'unidades'), recargarCategorias()])
  }

  async function onUnidad(id) {
    unidadId.value = id || null
    await recargarCategorias()
  }

  /**
   * Las categorías pertenecen a una unidad concreta. Se busca por el nivel más profundo elegido
   * y, si no hay categorías, se sube un nivel.
   */
  async function recargarCategorias() {
    const mia = ++secCategorias
    categoriaId.value = null
    subCategoriaId.value = null
    activoId.value = null
    categorias.value = []
    subCategorias.value = []

    const cadena = [unidadId.value, divisionId.value, gerenciaId.value].filter(Boolean)
    if (!cadena.length) return

    cargando.value.categorias = true
    try {
      for (const uo of cadena) {
        const lista = await listarCategorias(uo)
        if (mia !== secCategorias) return
        if (lista.length) {
          categorias.value = lista
          return
        }
      }
    } catch (err) {
      if (mia === secCategorias) errorCatalogo.value = err.message || 'No se pudieron cargar las categorías.'
    } finally {
      if (mia === secCategorias) cargando.value.categorias = false
    }
  }

  async function onCategoria(id) {
    const mia = ++secSubCategorias
    categoriaId.value = id || null
    subCategoriaId.value = null
    subCategorias.value = []
    // El activo se deduce de la categoría: el SP lo necesita para la asignación automática.
    activoId.value = categorias.value.find((c) => c.id === categoriaId.value)?.activoId ?? null
    if (!categoriaId.value) return

    cargando.value.subCategorias = true
    try {
      const lista = await listarSubCategorias(categoriaId.value)
      if (mia === secSubCategorias) subCategorias.value = lista
    } catch (err) {
      if (mia === secSubCategorias) errorCatalogo.value = err.message || 'No se pudieron cargar las subcategorías.'
    } finally {
      if (mia === secSubCategorias) cargando.value.subCategorias = false
    }
  }

  const categoriasPorActivo = computed(() => {
    const grupos = new Map()
    for (const c of categorias.value) {
      if (!grupos.has(c.activoNombre)) grupos.set(c.activoNombre, [])
      grupos.get(c.activoNombre).push(c)
    }
    return [...grupos].map(([activo, items]) => ({ activo, items }))
  })

  // ─── Datos complementarios ───────────────────────────────────────────────
  const tipoDatoActivo = computed(() => tiposDatos.find((t) => t.id === datoTipoId.value) || null)

  function agregarDato() {
    errorDato.value = ''
    const tipo = tipoDatoActivo.value
    const valor = datoValor.value.trim()
    if (!tipo) {
      errorDato.value = 'Seleccione el tipo de dato.'
      return
    }
    if (!valor) {
      errorDato.value = 'Ingrese el valor.'
      return
    }
    if (tipo.mascara && !new RegExp(tipo.mascara).test(valor)) {
      errorDato.value = `Formato inválido para ${tipo.descripcion}: ${tipo.formato}.`
      return
    }
    if (datos.value.some((d) => d.tipoDatoId === tipo.id && d.valor === valor)) {
      errorDato.value = 'Ese dato ya fue agregado.'
      return
    }
    datos.value.push({ tipoDatoId: tipo.id, tipoDatoDescripcion: tipo.descripcion, valor })
    datoValor.value = ''
  }

  function quitarDato(indice) {
    datos.value.splice(indice, 1)
  }

  // ─── Copias CC ───────────────────────────────────────────────────────────
  const copiasDeshabilitadas = computed(() => REGISTRO.uoSinCopias.includes(gerenciaId.value))

  let temporizadorCopias = null
  watch(copiasQuery, (texto) => {
    clearTimeout(temporizadorCopias)
    const q = (texto || '').trim()
    if (q.length < REGISTRO.minCaracteresBusquedaPersona) {
      copiasResultados.value = []
      return
    }
    temporizadorCopias = setTimeout(async () => {
      buscandoCopias.value = true
      try {
        const lista = await buscarPersonas(q)
        const elegidas = new Set(copias.value.map((p) => p.id))
        copiasResultados.value = lista
          .filter((p) => p.codigoPersonal && !elegidas.has(p.id))
          .slice(0, REGISTRO.maxResultadosPersonas)
      } catch (err) {
        copiasResultados.value = []
        errorCatalogo.value = err.message || 'No se pudo buscar colaboradores.'
      } finally {
        buscandoCopias.value = false
      }
    }, 350)
  })

  function agregarCopia(persona) {
    if (!copias.value.some((p) => p.id === persona.id)) copias.value.push(persona)
    copiasQuery.value = ''
    copiasResultados.value = []
  }

  function quitarCopia(id) {
    copias.value = copias.value.filter((p) => p.id !== id)
  }

  watch(copiasDeshabilitadas, (deshabilitadas) => {
    if (deshabilitadas) {
      copias.value = []
      copiasQuery.value = ''
      copiasResultados.value = []
    }
  })

  // ─── Adjuntos (dos pasos: seleccionar → adjuntar) ────────────────────────
  const puedeAdjuntar = computed(() => adjuntos.value.length < REGISTRO.maxAdjuntos)

  function seleccionarArchivo(archivo) {
    errorAdjunto.value = ''
    if (!archivo) {
      archivoPendiente.value = null
      return
    }
    if (archivo.size > REGISTRO.maxBytesAdjunto) {
      errorAdjunto.value = `"${archivo.name}" supera el máximo de ${REGISTRO.maxBytesAdjunto / 1024 / 1024} MB.`
      archivoPendiente.value = null
      return
    }
    archivoPendiente.value = archivo
  }

  async function adjuntar() {
    if (!archivoPendiente.value || !puedeAdjuntar.value) return
    subiendo.value = true
    errorAdjunto.value = ''
    try {
      const resp = await subirAdjuntoTemporal(archivoPendiente.value)
      adjuntos.value.push({
        archivoTemporalId: resp.archivoTemporalId,
        nombreOriginal: resp.nombreOriginal || archivoPendiente.value.name,
        tamanioBytes: resp.tamano || archivoPendiente.value.size,
      })
      archivoPendiente.value = null
    } catch (err) {
      errorAdjunto.value = err.message || 'No se pudo subir el archivo.'
    } finally {
      subiendo.value = false
    }
  }

  function quitarAdjunto(indice) {
    adjuntos.value.splice(indice, 1)
  }

  // ─── Validación ──────────────────────────────────────────────────────────
  const faltantes = computed(() => {
    const f = []
    if (!titulo.value.trim()) f.push('Título')
    if (!gerenciaId.value) f.push('Gerencia')
    if (!categoriaId.value) f.push('Categoría')
    // Sin subcategoría el SP deja iCodigo_CSC = 0 y el requerimiento no aparece en las búsquedas.
    if (!subCategoriaId.value) f.push('Subcategoría')
    if (!descripcion.value.trim()) f.push('Descripción')
    return f
  })

  const puedeRegistrar = computed(() => faltantes.value.length === 0 && !enviando.value && !subiendo.value)

  const modificado = computed(
    () =>
      !!(
        titulo.value ||
        descripcion.value ||
        gerenciaId.value ||
        datos.value.length ||
        copias.value.length ||
        adjuntos.value.length ||
        archivoPendiente.value
      )
  )

  // ─── Envío ───────────────────────────────────────────────────────────────
  function armarPayload() {
    return {
      // iCodigo_Uo = gerencia; iCodDivRes = nivel más profundo elegido (el SP decide cuál usar)
      unidadOrganicaId: gerenciaId.value,
      divisionId: unidadId.value ?? divisionId.value,
      activoId: activoId.value,
      categoriaId: categoriaId.value,
      subCategoriaId: subCategoriaId.value,
      // El título hace de sumilla (vSumilla_Req)
      sumilla: titulo.value.trim(),
      descripcionHtml: textoAHtml(descripcion.value.trim()),
      codigoPersonaSolicitante: perfil?.codigoPersonaGr,
      codigoPersonaActualizacion: perfil?.codigoPersonaOrganizacion,
      datosComplementarios: datos.value.map((d) => ({ tipoDatoId: d.tipoDatoId, valor: d.valor })),
      // El SP espera cCodPer (código de personal), no el código GR
      personasCopiaCodigos: copias.value.map((p) => p.codigoPersonal).filter(Boolean),
      archivosAdjuntos: adjuntos.value.map((a) => ({
        archivoTemporalId: a.archivoTemporalId,
        nombreOriginal: a.nombreOriginal,
        descripcion: '',
      })),
    }
  }

  async function registrar() {
    errorGeneral.value = ''
    if (faltantes.value.length) {
      errorGeneral.value = 'Complete los campos obligatorios antes de registrar.'
      return false
    }
    if (!perfil?.codigoPersonaGr || !perfil?.codigoPersonaOrganizacion) {
      errorGeneral.value = 'No se pudo identificar al usuario logueado. Vuelva a iniciar sesión.'
      return false
    }

    enviando.value = true
    try {
      const creado = await registrarRequerimiento(armarPayload())
      resultado.value = { ...creado, totalAdjuntos: adjuntos.value.length, totalCopias: copias.value.length }
      return true
    } catch (err) {
      errorGeneral.value = err.message || 'Error al procesar el registro en el servidor.'
      return false
    } finally {
      enviando.value = false
    }
  }

  function limpiar() {
    titulo.value = ''
    descripcion.value = ''
    gerenciaId.value = null
    divisionId.value = null
    unidadId.value = null
    categoriaId.value = null
    subCategoriaId.value = null
    activoId.value = null
    divisiones.value = []
    unidades.value = []
    categorias.value = []
    subCategorias.value = []
    secCategorias++
    secSubCategorias++
    datos.value = []
    datoTipoId.value = null
    datoValor.value = ''
    errorDato.value = ''
    copias.value = []
    copiasQuery.value = ''
    copiasResultados.value = []
    adjuntos.value = []
    archivoPendiente.value = null
    errorAdjunto.value = ''
    errorGeneral.value = ''
    resultado.value = null
  }

  onBeforeUnmount(() => clearTimeout(temporizadorCopias))

  return {
    // formulario
    titulo, descripcion,
    gerenciaId, divisionId, unidadId, categoriaId, subCategoriaId,
    datos, datoTipoId, datoValor, errorDato, tipoDatoActivo,
    copias, copiasQuery, copiasResultados, buscandoCopias, copiasDeshabilitadas,
    adjuntos, archivoPendiente, subiendo, errorAdjunto, puedeAdjuntar,
    // catálogos
    gerencias, divisiones, unidades, categorias, categoriasPorActivo, subCategorias, tiposDatos, cargando, errorCatalogo,
    // estado
    enviando, errorGeneral, resultado, faltantes, puedeRegistrar, modificado,
    // acciones
    iniciar: cargarGerencias, cargarGerencias, onGerencia, onDivision, onUnidad, onCategoria,
    agregarDato, quitarDato, agregarCopia, quitarCopia,
    seleccionarArchivo, adjuntar, quitarAdjunto,
    registrar, limpiar,
  }
}
