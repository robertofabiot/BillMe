import { api } from './api'
import type { Ganancias } from '@/types'

export const reporteService = {
  ganancias: (filtros?: {
    desde?: string
    hasta?: string
    clienteId?: string
    proyectoId?: string
  }) => {
    const params = new URLSearchParams()
    if (filtros) {
      Object.entries(filtros).forEach(([k, v]) => {
        if (v) params.set(k, v)
      })
    }
    const qs = params.toString()
    return api.get<Ganancias>(qs ? `/reportes/ganancias?${qs}` : '/reportes/ganancias')
  },
}
