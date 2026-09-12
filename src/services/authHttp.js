import { createHttpClient } from './http'

// Cliente para service-user-auth (login), un microservicio aparte
// con su propio puerto (8081 por defecto).
export const authHttp = createHttpClient(
  import.meta.env.VITE_AUTH_API_BASE_URL || 'http://localhost:8081'
)
