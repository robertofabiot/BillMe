import { api } from './api'
import type { Abono } from '@/types'

export interface AbonoPayload {
  clienteId: string
  proyectoId?: string | null
  monto: number
  fechaPago: string
  nota?: string | null
}

export const abonoService = {
  crear: (data: AbonoPayload) =>
    api.post<Abono>('/abonos', data),

  actualizar: (id: string, data: AbonoPayload) =>
    api.put<Abono>(`/abonos/${id}`, data),

  eliminar: (id: string) =>
    api.delete<void>(`/abonos/${id}`),
}
