/**
 * API Client central para BillMe.
 * Todas las llamadas HTTP al backend pasan por aquí.
 */

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export interface CampoError {
  campo: string
  mensaje: string
}

export class ApiError extends Error {
  status: number
  errores: CampoError[]

  constructor(status: number, message: string, errores: CampoError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errores = errores
  }
}

/** El backend responde los errores como { mensaje, errores }; si no, se usa el texto crudo. */
async function toApiError(res: Response): Promise<ApiError> {
  const text = await res.text().catch(() => '')
  try {
    const body = JSON.parse(text) as { mensaje?: string; errores?: CampoError[] }
    if (body.mensaje) return new ApiError(res.status, body.mensaje, body.errores ?? [])
  } catch {
    // respuesta que no es JSON (por ejemplo, un proxy caído)
  }
  return new ApiError(res.status, text || res.statusText || `Error ${res.status}`)
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem('billme_token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options?.headers as Record<string, string> | undefined),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (res.status === 401 && token && !path.startsWith('/auth/')) {
    // La sesión venció o el usuario ya no es válido: volver al login
    localStorage.removeItem('billme_token')
    localStorage.removeItem('billme_user')
    window.location.assign('/login')
  }

  if (!res.ok) {
    throw await toApiError(res)
  }

  // 204 No Content
  if (res.status === 204) return undefined as T

  return res.json()
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) =>
    request<T>(path, { method: 'DELETE' }),
}
