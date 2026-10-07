/**
 * Procesa errores devueltos por la API (ProblemDetail / JSON).
 */
async function extraerMensajeError(response, path) {
  try {
    const data = await response.json()
    if (data?.detail) return data.detail
    if (data?.message) return data.message
  } catch {
    // el cuerpo no era JSON o vino vacío
  }
  return `Error ${response.status} al consultar ${path}`
}

const TIMEOUT_MS = 15000

/**
 * Cliente HTTP configurable para distintas URLs base que administra
 * el token Bearer (`Authorization`) según el estado de autenticación.
 */
export function createHttpClient(baseUrl) {
  let authToken = null

  async function request(path, { method = 'GET', params, body } = {}) {
    const url = new URL(baseUrl + path)

    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) url.searchParams.set(key, value)
      })
    }

    // Con FormData (subida de archivos) el navegador fija el Content-Type con su boundary.
    const esForm = typeof FormData !== 'undefined' && body instanceof FormData
    const headers = {}
    if (body && !esForm) headers['Content-Type'] = 'application/json'
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`

// Define un tiempo límite de respuesta (timeout) para evitar bloqueos indefinidos en la interfaz.
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS)

    let response
    try {
      response = await fetch(url, {
        method,
        headers: Object.keys(headers).length ? headers : undefined,
        body: body ? (esForm ? body : JSON.stringify(body)) : undefined,
        signal: controller.signal,
      })
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new Error(
          `${path} tardó más de ${TIMEOUT_MS / 1000}s en responder (backend caído, ` +
            'lento, o sin conexión a la base de datos). Se canceló la petición.'
        )
      }
      throw error
    } finally {
      clearTimeout(timeoutId)
    }

    if (!response.ok) {
      throw new Error(await extraerMensajeError(response, path))
    }

    if (response.status === 204) return null
    // Algunos endpoints (p. ej. PUT de clasificación) responden 200 sin cuerpo.
    const texto = await response.text()
    return texto ? JSON.parse(texto) : null
  }

  return {
    get: (path, params) => request(path, { method: 'GET', params }),
    post: (path, body) => request(path, { method: 'POST', body }),
    postForm: (path, formData) => request(path, { method: 'POST', body: formData }),
    put: (path, body) => request(path, { method: 'PUT', body }),
    del: (path) => request(path, { method: 'DELETE' }),
    setAuthToken: (token) => { authToken = token },
    clearAuthToken: () => { authToken = null },
  }
}

// Cliente para requerimiento (requerimientos, personas, categorías, etc.)
export const http = createHttpClient(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082')

// Cliente para Organizacion (requerimientos, personas, categorías, etc.)
export const httpOrg = createHttpClient(import.meta.env.VITE_ORG_API_BASE_URL || 'http://localhost:8083')
