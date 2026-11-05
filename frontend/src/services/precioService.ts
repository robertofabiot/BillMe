import { api } from './api'
import type { PrecioSugerido, PrecioFormula, PrecioHistorialFila } from '@/types'

export const precioService = {
  sugerido: (clienteId: string, productoId: string, descripcion?: string) => {
    const params = new URLSearchParams({ clienteId, productoId })
    if (descripcion) params.set('descripcion', descripcion)
    return api.get<PrecioSugerido>(`/precios/sugerido?${params.toString()}`)
  },

  formula: (productoId: string, descuentoPct?: number) => {
    const params = new URLSearchParams({ productoId })
    if (descuentoPct != null) params.set('descuentoPct', String(descuentoPct))
    return api.get<PrecioFormula>(`/precios/formula?${params.toString()}`)
  },

  historial: (filtros?: { q?: string; clienteId?: string }) => {
    const params = new URLSearchParams()
    if (filtros?.q) params.set('q', filtros.q)
    if (filtros?.clienteId) params.set('clienteId', filtros.clienteId)
    const qs = params.toString()
    return api.get<PrecioHistorialFila[]>(qs ? `/precios/historial?${qs}` : '/precios/historial')
  },
}
