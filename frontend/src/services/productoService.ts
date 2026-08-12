import { api } from './api'
import type { Producto } from '@/types'

export interface ProductoPayload {
  codigoInterno: string
  nombrePrincipal: string
  descripcion?: string
  peso: number
  precioListaProveedor?: number
  descuentoProveedor?: number
  costoNeto?: number
  aliases: string[]
}

export const productoService = {
  listar: (q?: string) =>
    api.get<Producto[]>(q ? `/productos?q=${encodeURIComponent(q)}` : '/productos'),

  obtener: (id: string) =>
    api.get<Producto>(`/productos/${id}`),

  crear: (data: ProductoPayload) =>
    api.post<Producto>('/productos', data),

  actualizar: (id: string, data: ProductoPayload) =>
    api.put<Producto>(`/productos/${id}`, data),

  eliminar: (id: string) =>
    api.delete<void>(`/productos/${id}`),
}
