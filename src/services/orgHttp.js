import { createHttpClient } from './http'

// Cliente para service-organizacion (unidades orgánicas y personal)
export const orgHttp = createHttpClient(
  import.meta.env.VITE_ORG_API_BASE_URL || 'http://localhost:8082'
)
