import { authHttp } from './authHttp'
import { http } from './http'

/**
 * Login Windows mediante POST /api/auth/login ({ usuarioWindows }).
 * - Lanza Error si no existe (401) o no tiene roles activos (403).
 */
export async function login(usuarioWindows) {
  const respuesta = await authHttp.post('/api/auth/login', { usuarioWindows })

  // service-requerimiento ahora tambien exige este token
  authHttp.setAuthToken(respuesta.token)
  http.setAuthToken(respuesta.token)

  return {
    ...respuesta.perfil,
    token: respuesta.token,
    expiresAt: respuesta.expiresAt,
  }
}

/**
 * Lista los roles activos del usuario (y sus datos de persona).
 */
export function listarRoles(usuarioWindows, codigoRol = 0) {
  return authHttp.get(`/api/auth/roles/${encodeURIComponent(usuarioWindows)}`, { codigoRol })
}
