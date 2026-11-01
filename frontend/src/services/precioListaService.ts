import { api } from './api'
import type { FuentePrecio, PrecioLista, PreciosListaProducto } from '@/types'

export interface PrecioListaPayload {
  fuente: Exclude<FuentePrecio, 'LISTA_IMPORTADA'>
  precio: number
  descuentoPct?: number
  fecha?: string
  nota?: string
}

export const precioListaService = {
  consultar: (productoId: string) =>
    api.get<PreciosListaProducto>(`/productos/${productoId}/precios-lista`),

  /** Calcula el precio de lista sin guardarlo (vista previa). */
  calcular: (productoId: string, data: PrecioListaPayload) =>
    api.post<{ precioSinIva: number }>(`/productos/${productoId}/precios-lista/calcular`, data),

  registrar: (productoId: string, data: PrecioListaPayload) =>
    api.post<PrecioLista>(`/productos/${productoId}/precios-lista`, data),
}
