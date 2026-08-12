import { api } from './api'
import type { Consolidado } from '@/types'

export interface ConsolidadoItemPayload {
  productoNombre: string
  cantidad: number
  precioUnitario: number
  orden: number
  facturaId?: string | null
}

export interface ConsolidadoGroupPayload {
  nombre: string
  orden: number
  items: ConsolidadoItemPayload[]
}

export interface ConsolidadoPayload {
  clienteId: string
  nombre: string
  grupos: ConsolidadoGroupPayload[]
}

export const consolidadoService = {
  listar: (clienteId?: string) =>
    api.get<Consolidado[]>(clienteId ? `/consolidados?clienteId=${clienteId}` : '/consolidados'),

  obtener: (id: string) =>
    api.get<Consolidado>(`/consolidados/${id}`),

  crear: (data: ConsolidadoPayload) =>
    api.post<Consolidado>('/consolidados', data),

  eliminar: (id: string) =>
    api.delete(`/consolidados/${id}`),
}
