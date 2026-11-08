/**
 * API Client central para BillMe.
 * Todas las llamadas HTTP al backend pasan por aquí.
 */

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
const URL_SALUD = new URL('/actuator/health', new URL(API_BASE, window.location.origin)).href

/**
 * Consulta la salud del backend. En el plan gratis de Render el servidor se duerme
 * sin uso: esta llamada lo despierta y termina cuando ya está listo.
 */
export async function esperarServidor(): Promise<boolean> {
  try {
    const res = await fetch(URL_SALUD, { cache: 'no-store' })
    return res.ok
  } catch {
    return false
  }
}

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
    const mensaje = body.errores && body.errores.length > 0
      ? body.errores.map(e => e.mensaje).join('. ')
      : body.mensaje
    if (mensaje) return new ApiError(res.status, mensaje, body.errores ?? [])
  } catch {
    // respuesta que no es JSON (por ejemplo, un proxy caído)
  }
  return new ApiError(res.status, text || res.statusText || `Error ${res.status}`)
}

export async function enviar(path: string, options?: RequestInit): Promise<Response> {
  const token = localStorage.getItem('token') || localStorage.getItem('billme_token')
  // Con FormData (subir archivos) el navegador pone el Content-Type con su separador
  const esArchivo = options?.body instanceof FormData
  const headers: Record<string, string> = {
    ...(esArchivo ? {} : { 'Content-Type': 'application/json' }),
    ...(options?.headers as Record<string, string> | undefined),
  }

  if (token && !headers['Authorization'] && !headers['authorization']) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const cleanPath = path.startsWith('/api') ? path.slice(4) : path
  const url = path.startsWith('http') ? path : `${API_BASE}${cleanPath}`

  const res = await fetch(url, {
    ...options,
    headers,
  })

  if (res.status === 401 && !path.startsWith('/auth/')) {
    // La sesión venció o el usuario ya no es válido: volver al login
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
    localStorage.removeItem('billme_token')
    localStorage.removeItem('billme_user')
    if (typeof window !== 'undefined') {
      window.location.assign('/login')
    }
  }

  if (!res.ok) {
    throw await toApiError(res)
  }

  return res
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await enviar(path, options)

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
  subir: <T>(path: string, form: FormData) =>
    request<T>(path, { method: 'POST', body: form }),
  descargar: async (path: string): Promise<Blob> => {
    const res = await enviar(path)
    return res.blob()
  },
}
