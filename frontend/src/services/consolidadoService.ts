import { api } from './api'
import type { Consolidado } from '@/types'

export interface ConsolidadoRangoPayload {
  clienteId: string
  proyectoId?: string | null
  desde: string
  hasta: string
}

export interface NombreGrupoPayload {
  fechaEntrega: string
  nombre: string
}

export interface ConsolidadoGuardarPayload {
  clienteId: string
  proyectoId?: string | null
  nombre: string
  desde: string
  hasta: string
  grupos?: NombreGrupoPayload[]
}

export const consolidadoService = {
  listar: (clienteId?: string) =>
    api.get<Consolidado[]>(clienteId ? `/consolidados?clienteId=${clienteId}` : '/consolidados'),

  obtener: (id: string) =>
    api.get<Consolidado>(`/consolidados/${id}`),

  vistaPrevia: (data: ConsolidadoRangoPayload) =>
    api.post<Consolidado>('/consolidados/vista-previa', data),

  crear: (data: ConsolidadoGuardarPayload) =>
    api.post<Consolidado>('/consolidados', data),

  actualizar: (id: string, data: ConsolidadoGuardarPayload) =>
    api.put<Consolidado>(`/consolidados/${id}`, data),

  eliminar: (id: string) =>
    api.delete<void>(`/consolidados/${id}`),

  descargarExcel: async (id: string, encabezado?: string): Promise<Blob> => {
    const params = new URLSearchParams()
    if (encabezado != null && encabezado.trim() !== '') {
      params.set('encabezado', encabezado.trim())
    }
    const qs = params.toString()
    return api.descargar(`/consolidados/${id}/excel${qs ? `?${qs}` : ''}`)
  },
}
