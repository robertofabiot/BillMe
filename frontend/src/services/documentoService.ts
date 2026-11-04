import { api } from './api'
import type { Abono, Documento, EstadoDocumento, MetodoPrecio, RevisionDocumento, TipoDocumento } from '@/types'

export interface LineaPayload {
  id?: string
  productoId: string
  descripcion?: string
  cantidad: number
  pesoUnitKg?: number | null
  precioUnit: number
  metodoPrecio?: MetodoPrecio
  parametros?: string | null
  fechaEntrega?: string | null
  lugarEntrega?: string | null
}

export interface DocumentoPayload {
  /** Solo al crear. */
  tipo?: TipoDocumento
  clienteId: string
  proyectoId?: string | null
  fecha: string
  notas?: string | null
  mostrarEntrega?: boolean
  lineas: LineaPayload[]
}

export interface FiltrosDocumento {
  tipo?: TipoDocumento
  estado?: EstadoDocumento
  clienteId?: string
  proyectoId?: string
  productoId?: string
  desde?: string
  hasta?: string
}

export const documentoService = {
  listar: (filtros: FiltrosDocumento = {}) => {
    const params = new URLSearchParams()
    Object.entries(filtros).forEach(([k, v]) => {
      if (v) params.set(k, v)
    })
    const qs = params.toString()
    return api.get<Documento[]>(qs ? `/documentos?${qs}` : '/documentos')
  },

  obtener: (id: string) =>
    api.get<Documento>(`/documentos/${id}`),

  crear: (data: DocumentoPayload) =>
    api.post<Documento>('/documentos', data),

  actualizar: (id: string, data: DocumentoPayload) =>
    api.put<Documento>(`/documentos/${id}`, data),

  /** Crea la factura de una cotización y devuelve la factura. */
  convertirAFactura: (id: string, fecha: string) =>
    api.post<Documento>(`/documentos/${id}/convertir-a-factura?fecha=${fecha}`, {}),

  /** Rechazar (RECHAZADA) o reabrir (PENDIENTE) una cotización. */
  cambiarEstado: (id: string, estado: EstadoDocumento) =>
    api.post<Documento>(`/documentos/${id}/estado`, { estado }),

  anular: (id: string) =>
    api.post<Documento>(`/documentos/${id}/anular`, {}),

  revisiones: (id: string) =>
    api.get<RevisionDocumento[]>(`/documentos/${id}/revisiones`),

  registrarAbono: (id: string, data: { monto: number; fechaPago: string }) =>
    api.post<Abono>(`/documentos/${id}/abonos`, data),
}
