import { createHttpClient } from './http'

// Cliente para service-user-auth (login)
export const authHttp = createHttpClient(
  import.meta.env.VITE_AUTH_API_BASE_URL || 'http://localhost:8081'
)
