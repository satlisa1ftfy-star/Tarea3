import { http, httpOrg } from './http'

/**
 * Catálogos para Registro y Clasificación de requerimientos (service-requerimiento).
 * Unidades, categorías y subcategorías vienen del backend; activos, prioridades y tipos de dato
 * complementario no tienen endpoint y se resuelven aquí (datos de GestionRQ).
 */

// GRTabActivos
const ACTIVOS = [
  { id: 1, nombre: 'Equipamiento Auxiliar' },
  { id: 2, nombre: 'Redes de Comunicaciones' },
  { id: 3, nombre: 'Datos / Información' },
  { id: 4, nombre: 'Hardware (Equipos Informáticos)' },
  { id: 5, nombre: 'Instalaciones' },
  { id: 6, nombre: 'No Clasificado' },
  { id: 8, nombre: 'Servicios' },
  { id: 10, nombre: 'Software (Aplicaciones)' },
]

// GRMaeAtributos (iCodigo_Pri). El SP de registro siempre deja 32.
export const PRIORIDADES = [
  { id: 32, nombre: 'Importante' },
  { id: 33, nombre: 'Muy importante' },
  { id: 34, nombre: 'Crítico' },
]

// GRMaeAtributos (iCodigo_TipoDatCom). vDatCom = "id,valor;id,valor"
export const TIPOS_DATOS_COMPLEMENTARIOS = [
  { id: 82, descripcion: 'RUC', mascara: '^\\d{11}$', formato: '11 dígitos', ejemplo: '20131370645' },
  { id: 83, descripcion: 'DNI/Libreta Electoral', mascara: '^\\d{8}$', formato: '8 dígitos', ejemplo: '09446741' },
  { id: 84, descripcion: 'Carne de Identidad', mascara: null, formato: 'Libre', ejemplo: '' },
  { id: 85, descripcion: 'Carne de Extranjeria', mascara: '^[A-Za-z0-9]{9,12}$', formato: '9-12 caracteres', ejemplo: '001234567' },
  { id: 86, descripcion: 'Pasaporte', mascara: '^[A-Za-z0-9]{6,12}$', formato: '6-12 caracteres', ejemplo: 'AB123456' },
  { id: 87, descripcion: 'Codigo SAT', mascara: null, formato: 'Libre', ejemplo: '' },
  { id: 88, descripcion: 'N° Doc. Sanción', mascara: null, formato: 'Libre', ejemplo: '' },
  { id: 92, descripcion: 'Cantidad', mascara: '^\\d+$', formato: 'Numérico', ejemplo: '10' },
  { id: 93, descripcion: 'Monto', mascara: '^\\d+(\\.\\d{1,2})?$', formato: 'Monto (2 decimales)', ejemplo: '1500.50' },
  { id: 94, descripcion: 'Documento de Deuda', mascara: null, formato: 'Libre', ejemplo: '' },
  { id: 104, descripcion: 'Plazo de Atención', mascara: null, formato: 'Libre', ejemplo: '' },
]

export function nombreActivo(id) {
  return ACTIVOS.find((a) => a.id === id)?.nombre || ''
}

function normalizarUnidades(lista) {
  if (!Array.isArray(lista)) return []
  const vistos = new Set()
  return lista
    .filter((u) => u.codigoUo != null && !vistos.has(u.codigoUo) && vistos.add(u.codigoUo))
    .map((u) => ({ id: u.codigoUo, nombre: (u.nombreUnidadOrganica || `Unidad Orgánica ${u.codigoUo}`).trim() }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
}

/** Unidades orgánicas a las que se puede solicitar (primer nivel del combo). */
export async function listarUnidadesSolicitud() {
  return normalizarUnidades(await httpOrg.get('/api/organizacion/unidadesOrganicas/solicitud'))
}

/** Divisiones / unidades hijas de una unidad orgánica. */
export async function listarDependencias(codigoUo) {
  return normalizarUnidades(await httpOrg.get(`/api/organizacion/unidadesOrganicas/${codigoUo}/dependencias`))
}

/** Categorías de una unidad orgánica (0 = todas), con su activo. */
export async function listarCategorias(codigoUo = 0) {
  const lista = await http.get('/api/requerimiento/categorias-activo', { codigoUo })
  return (Array.isArray(lista) ? lista : []).map((c) => ({
    id: c.codigoCategoria,
    nombre: c.nombreCategoria || '',
    activoId: c.codigoActivo ?? null,
    activoNombre: c.nombreActivo || 'Sin activo',
  }))
}

/** Subcategorías de una categoría. */
export async function listarSubCategorias(codigoCategoria) {
  const lista = await http.get(`/api/requerimiento/buscarSubCategoria/${codigoCategoria}/TODO`)
  return (Array.isArray(lista) ? lista : []).map((s) => ({
    id: s.codigoSubcategoria,
    nombre: s.nombreSubcategoria || '',
  }))
}

/** Personas vigentes cuyo nombre contiene `texto` (para los correos en copia). */
export async function buscarPersonas(texto) {
  const lista = await http.get('/api/personas', { nombre: texto, codigoUo: 0, vigencia: 1 })
  return (Array.isArray(lista) ? lista : []).map((p) => ({
    id: p.codigoPer,
    nombre: p.nombre || '',
    codigoPersonal: (p.codigoPersona || '').trim(), // cCodPer: lo que espera el SP para las copias
    unidadOrganica: p.unidadOrganica || '',
  }))
}
