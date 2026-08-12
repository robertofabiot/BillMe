import { api } from './api'
import type { Proyecto } from '@/types'

export interface ProyectoPayload {
  clienteId: string
  nombre: string
  estado: string
}

export const proyectoService = {
  listar: (clienteId?: string, activos?: boolean) => {
    const params = new URLSearchParams()
    if (clienteId) params.set('clienteId', clienteId)
    if (activos) params.set('activos', 'true')
    const qs = params.toString()
    return api.get<Proyecto[]>(qs ? `/proyectos?${qs}` : '/proyectos')
  },

  obtener: (id: string) =>
    api.get<Proyecto>(`/proyectos/${id}`),

  crear: (data: ProyectoPayload) =>
    api.post<Proyecto>('/proyectos', data),

  actualizar: (id: string, data: ProyectoPayload) =>
    api.put<Proyecto>(`/proyectos/${id}`, data),
}
