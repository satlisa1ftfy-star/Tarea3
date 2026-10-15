import { ref } from 'vue'
import { listarPersonas } from '../services/personaService'

// Catálogo de personas vigentes (GET /api/personas?vigencia=1) en memoria.
// El estado vive a nivel de módulo, así que se comparte entre todos los
// componentes y dura mientras la aplicación Vue esté abierta: se descarga
// una sola vez y luego todas las búsquedas se hacen en memoria.
const personasCatalogo = ref([])
const cargandoPersonas = ref(false)
const errorPersonas = ref('')
let catalogoCargado = false
let cargaEnCurso = null // evita dos GET simultáneos si se pide la carga dos veces

// Solo se conservan los campos que usa la selección/búsqueda de personas.
function aPersonaCatalogo(persona) {
  return {
    codigoPersona: persona.codigoPersona,
    nombre: persona.nombre,
    cargo: persona.cargo,
    codigoCargo: persona.codigoCargo,
    codigoUo: persona.codigoUo,
    unidadOrganica: persona.unidadOrganica,
  }
}

async function cargarCatalogoPersonas() {
  if (catalogoCargado) return personasCatalogo.value
  if (cargaEnCurso) return cargaEnCurso

  cargandoPersonas.value = true
  errorPersonas.value = ''
  cargaEnCurso = (async () => {
    try {
      const personas = await listarPersonas({ vigencia: 1 })
      personasCatalogo.value = (personas || []).map(aPersonaCatalogo)
      catalogoCargado = true
    } catch (error) {
      // No se marca como cargado: el próximo intento vuelve a pedir el catálogo.
      console.error('No se pudo cargar el catálogo de personas.', error)
      errorPersonas.value = error?.message || 'No se pudo cargar el catálogo de personas.'
    } finally {
      cargandoPersonas.value = false
      cargaEnCurso = null
    }
    return personasCatalogo.value
  })()
  return cargaEnCurso
}

// Quita tildes y pasa a minúsculas: "FISCALIZACIÓN" -> "fiscalizacion".
export function normalizarTexto(texto) {
  return (texto || '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
}

export function usePersonasCatalogo() {
  return { personasCatalogo, cargandoPersonas, errorPersonas, cargarCatalogoPersonas }
}
