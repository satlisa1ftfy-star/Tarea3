// Registro local de los requerimientos que el usuario clasificó (botón «Clasificar»).
// PROVISIONAL: el backend no expone quién clasificó, así que se recuerda en este navegador, por usuario.
// Cuando exista la marca en la base, solo hay que cambiar el origen en cargarClasificados().

const MAXIMO = 100

function clave(perfil) {
  const usuario = perfil?.codigoPersonaOrganizacion || perfil?.codigoPersonaGr
  return usuario ? `sgr.clasificados.${usuario}` : null
}

function leer(perfil) {
  const k = clave(perfil)
  if (!k) return []
  try {
    const lista = JSON.parse(localStorage.getItem(k) || '[]')
    return Array.isArray(lista) ? lista : []
  } catch {
    return []
  }
}

/** Números de requerimiento clasificados por el usuario, del más reciente al más antiguo. */
export function listarClasificados(perfil) {
  return leer(perfil).map((e) => e.codigo)
}

/** Anota (o sube al inicio) un requerimiento recién clasificado. */
export function registrarClasificado(perfil, codigo) {
  const k = clave(perfil)
  const numero = Number(codigo)
  if (!k || !numero) return
  const lista = leer(perfil).filter((e) => e.codigo !== numero)
  lista.unshift({ codigo: numero, fecha: new Date().toISOString() })
  try {
    localStorage.setItem(k, JSON.stringify(lista.slice(0, MAXIMO)))
  } catch {
    // sin almacenamiento disponible (modo privado, cuota): la sección simplemente no recuerda
  }
}
