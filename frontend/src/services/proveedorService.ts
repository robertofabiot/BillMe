import { api } from './api'
import type { Proveedor, ProveedorRequest } from '@/types'

export const proveedorService = {
  listar: () => api.get<Proveedor[]>('/proveedores'),
  crear: (data: ProveedorRequest) => api.post<Proveedor>('/proveedores', data),
}
