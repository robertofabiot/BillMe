import { api } from './api'
import type { ReporteData } from '@/types'

export const reporteService = {
  general: (filtros?: {
    clienteId?: string
    proyectoId?: string
    desde?: string
    hasta?: string
  }) => {
    const params = new URLSearchParams()
    if (filtros) {
      Object.entries(filtros).forEach(([k, v]) => {
        if (v) params.set(k, v)
      })
    }
    const qs = params.toString()
    return api.get<ReporteData>(qs ? `/reportes/general?${qs}` : '/reportes/general')
  },
}
