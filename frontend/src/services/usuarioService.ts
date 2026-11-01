import { api } from './api'
import type { Usuario } from '@/types'

export interface UsuarioPayload {
  username: string
  nombre: string
  password: string
}

export const usuarioService = {
  listar: () =>
    api.get<Usuario[]>('/usuarios'),

  crear: (data: UsuarioPayload) =>
    api.post<Usuario>('/usuarios', data),

  cambiarMiPassword: (actual: string, nueva: string) =>
    api.put<void>('/usuarios/yo/password', { actual, nueva }),

  restablecerPassword: (id: string, nueva: string) =>
    api.put<void>(`/usuarios/${id}/password`, { nueva }),
}
