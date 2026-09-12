import { ref } from 'vue'
import { login as loginApi } from '../services/seguridadService'
import { authHttp } from '../services/authHttp'

export function useAuthController() {
  const username = ref('')
  const password = ref('')
  const showPassword = ref(false)
  const submitted = ref(false)
  const isAuthenticated = ref(false)
  const loading = ref(false)
  const errorMessage = ref('')
  // Datos devueltos por el backend al iniciar sesión (spGR_Seguridad_ConsultarUsuario).
  const perfil = ref(null)

  async function login() {
    submitted.value = true
    errorMessage.value = ''

    if (!username.value || !password.value) return

    loading.value = true
    try {
      // El usuario es el usuario de dominio (Windows), p. ej. "JGUILLEN".
      // La contraseña no se valida contra el backend: este sistema confía
      // en el inicio de sesión de Windows/dominio y solo verifica que el
      // usuario esté registrado y tenga roles activos en Gestión de Requerimientos.
      perfil.value = await loginApi(username.value.trim())
      isAuthenticated.value = true
    } catch (error) {
      errorMessage.value = error.message || 'No se pudo iniciar sesión.'
      isAuthenticated.value = false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    authHttp.clearAuthToken()
    isAuthenticated.value = false
    perfil.value = null
    submitted.value = false
    errorMessage.value = ''
    username.value = ''
    password.value = ''
  }

  return {
    username,
    password,
    showPassword,
    submitted,
    isAuthenticated,
    loading,
    errorMessage,
    perfil,
    login,
    logout,
  }
}
