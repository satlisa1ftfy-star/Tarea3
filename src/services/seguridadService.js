import { authHttp } from './authHttp'

/**
 * Inicia sesión con el usuario de dominio (Windows).
 * Backend: service-user-auth -> POST /api/auth/login  { usuarioWindows }
 *  -> spGR_Seguridad_ConsultarUsuario (siTipBus = 1), que a su vez llama a
 *     organizacion..spEO_Personal_BuscarxUsuWin.
 *
 * Sigue el esquema de JWT del profesor: si el usuario existe y tiene roles
 * activos, el backend responde { token, expiresAt, perfil }. Guardamos el
 * token en authHttp para que las siguientes peticiones a service-user-auth
 * (por ahora, /api/auth/roles) vayan autenticadas con "Bearer <token>".
 *
 * Lanza un Error con el mensaje del backend si el usuario no existe (401)
 * o no tiene roles activos (403).
 */
export async function login(usuarioWindows) {
  const respuesta = await authHttp.post('/api/auth/login', { usuarioWindows })

  authHttp.setAuthToken(respuesta.token)

  return {
    ...respuesta.perfil,
    token: respuesta.token,
    expiresAt: respuesta.expiresAt,
  }
}

/**
 * Lista los roles activos del usuario (y sus datos de persona).
 * Backend: service-user-auth -> GET /api/auth/roles/{usuarioWindows}?codigoRol=0
 *  -> spGR_Seguridad_ConsultarUsuario (siTipBus = 2)
 */
export function listarRoles(usuarioWindows, codigoRol = 0) {
  return authHttp.get(`/api/auth/roles/${encodeURIComponent(usuarioWindows)}`, { codigoRol })
}
