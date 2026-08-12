import { api } from './api'
import type { Cliente } from '@/types'

export interface ClientePayload {
  nombre: string
  telefono?: string
  detalles?: string
}

export const clienteService = {
  listar: (q?: string) =>
    api.get<Cliente[]>(q ? `/clientes?q=${encodeURIComponent(q)}` : '/clientes'),

  obtener: (id: string) =>
    api.get<Cliente>(`/clientes/${id}`),

  crear: (data: ClientePayload) =>
    api.post<Cliente>('/clientes', data),

  actualizar: (id: string, data: ClientePayload) =>
    api.put<Cliente>(`/clientes/${id}`, data),

  eliminar: (id: string) =>
    api.delete<void>(`/clientes/${id}`),
}
