import { api } from './api'
import type { Cliente, CuentaCliente, MonedaTarifa } from '@/types'

export interface ClientePayload {
  nombre: string
  telefono?: string
  detalles?: string
  descuentoPct?: number
  tarifaKg?: number | null
  tarifaMoneda?: MonedaTarifa
  saldoInicial?: number
  fechaInicioCuenta?: string | null
}

export const clienteService = {
  listar: (q?: string) =>
    api.get<Cliente[]>(q ? `/clientes?q=${encodeURIComponent(q)}` : '/clientes'),

  obtener: (id: string) =>
    api.get<Cliente>(`/clientes/${id}`),

  cuenta: (id: string) =>
    api.get<CuentaCliente>(`/clientes/${id}/cuenta`),

  crear: (data: ClientePayload) =>
    api.post<Cliente>('/clientes', data),

  actualizar: (id: string, data: ClientePayload) =>
    api.put<Cliente>(`/clientes/${id}`, data),

  eliminar: (id: string) =>
    api.delete<void>(`/clientes/${id}`),
}
