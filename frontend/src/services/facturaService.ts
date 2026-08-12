import { api } from './api'
import type { Factura, Abono } from '@/types'

export interface DetallePayload {
  productoId: string
  cantidad: number
  precioUnitarioVenta: number
}

export interface FacturaPayload {
  clienteId: string
  proyectoId?: string | null
  estado?: string
  detalles: DetallePayload[]
}

export interface AbonoPayload {
  monto: number
  fechaPago: string
}

export const facturaService = {
  listar: (filtros?: {
    clienteId?: string
    proyectoId?: string
    estado?: string
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
    return api.get<Factura[]>(qs ? `/facturas?${qs}` : '/facturas')
  },

  obtener: (id: string) =>
    api.get<Factura>(`/facturas/${id}`),

  crear: (data: FacturaPayload) =>
    api.post<Factura>('/facturas', data),

  actualizar: (id: string, data: FacturaPayload) =>
    api.put<Factura>(`/facturas/${id}`, data),

  confirmar: (id: string) =>
    api.patch<Factura>(`/facturas/${id}/confirmar`),

  listarAbonos: (facturaId: string) =>
    api.get<Abono[]>(`/facturas/${facturaId}/abonos`),

  registrarAbono: (facturaId: string, data: AbonoPayload) =>
    api.post<Abono>(`/facturas/${facturaId}/abonos`, data),
}
