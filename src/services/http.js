// El backend (Spring) responde los errores como ProblemDetail/JSON con
// "detail" o "message". Si no se puede leer, cae a un mensaje genérico.
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
 * Crea un cliente HTTP apuntando a un backend distinto. Se usa una fábrica
 * porque este frontend consume dos microservicios independientes:
 * service-requerimiento y service-user-auth (login), cada uno con su propia
 * URL base.
 *
 * Además soporta un token Bearer opcional (setAuthToken/clearAuthToken),
 * para el esquema de JWT de service-user-auth: una vez logueado, todas las
 * peticiones que use ese cliente incluyen "Authorization: Bearer <token>".
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

    const headers = {}
    if (body) headers['Content-Type'] = 'application/json'
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`

    // Sin esto, si el backend nunca responde (SQL Server lento/colgado,
    // pool de conexiones agotado, etc.) la promesa de fetch queda pendiente
    // para siempre y la vista se queda en "Cargando..." sin avisar nada.
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS)

    let response
    try {
      response = await fetch(url, {
        method,
        headers: Object.keys(headers).length ? headers : undefined,
        body: body ? JSON.stringify(body) : undefined,
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
    return response.json()
  }

  return {
    get: (path, params) => request(path, { method: 'GET', params }),
    post: (path, body) => request(path, { method: 'POST', body }),
    put: (path, body) => request(path, { method: 'PUT', body }),
    del: (path) => request(path, { method: 'DELETE' }),
    setAuthToken: (token) => { authToken = token },
    clearAuthToken: () => { authToken = null },
  }
}

// Cliente para service-requerimiento (requerimientos, personas, categorías, etc.)
export const http = createHttpClient(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
