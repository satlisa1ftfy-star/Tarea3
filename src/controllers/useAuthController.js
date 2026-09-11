import { ref } from 'vue'

export function useAuthController() {
  const username = ref('')
  const password = ref('')
  const showPassword = ref(false)
  const submitted = ref(false)
  const isAuthenticated = ref(false)

  function login() {
    submitted.value = true
    if (username.value && password.value) isAuthenticated.value = true
  }

  return { username, password, showPassword, submitted, isAuthenticated, login }
}
