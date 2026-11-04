import { api } from './api'
import type { Factura, Abono, EstadoFactura } from '@/types'
import { hoyIso } from '@/lib/format'

/**
 * Capa temporal: traduce la API de documentos al formato que todavía usan las pantallas
 * de facturas. Se elimina al pasar esas pantallas a documentoService.
 */
interface DocumentoApi {
  id: string
  tipo: 'COTIZACION' | 'FACTURA'
  folio: string
  estado: string
  fecha: string
  clienteId: string
  clienteNombre?: string
  proyectoId?: string
  proyectoNombre?: string
  total: number
  totalAbonado: number
  saldoPendiente?: number
  abonos: Abono[]
  lineas: {
    id: string
    productoId: string
    productoCodigo: string
    descripcion: string
    cantidad: number
    precioUnit: number
    costoUnitConIva?: number
    pesoUnitKg?: number
  }[]
}

const estadoDe = (d: DocumentoApi): EstadoFactura => {
  if (d.tipo === 'COTIZACION') return 'COTIZACION'
  if (d.total > 0 && d.totalAbonado >= d.total) return 'PAGADO'
  return d.totalAbonado > 0 ? 'PAGO_PARCIAL' : 'PENDIENTE'
}

const aFactura = (d: DocumentoApi): Factura => ({
  id: d.id,
  folioInterno: d.folio,
  estado: estadoDe(d),
  totalVenta: d.total,
  clienteId: d.clienteId,
  clienteNombre: d.clienteNombre,
  proyectoId: d.proyectoId,
  proyectoNombre: d.proyectoNombre,
  createdAt: d.fecha,
  abonos: d.abonos,
  totalAbonado: d.totalAbonado,
  saldoPendiente: d.saldoPendiente,
  detalles: d.lineas.map(l => ({
    id: l.id,
    productoId: l.productoId,
    productoNombre: l.descripcion,
    productoCodigoInterno: l.productoCodigo,
    cantidad: l.cantidad,
    precioUnitarioVenta: l.precioUnit,
    costoUnitario: l.costoUnitConIva,
    pesoUnitario: l.pesoUnitKg,
  })),
})

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
  listar: async (filtros?: {
    clienteId?: string
    proyectoId?: string
    estado?: string
    desde?: string
    hasta?: string
    productoId?: string
  }) => {
    const params = new URLSearchParams()
    Object.entries(filtros ?? {}).forEach(([k, v]) => {
      if (v && k !== 'estado') params.set(k, v)
    })
    const qs = params.toString()
    const documentos = await api.get<DocumentoApi[]>(qs ? `/documentos?${qs}` : '/documentos')
    return documentos.filter(d => d.estado !== 'ANULADA' && d.estado !== 'ACEPTADA').map(aFactura)
  },

  obtener: async (id: string) =>
    aFactura(await api.get<DocumentoApi>(`/documentos/${id}`)),

  crear: async (data: FacturaPayload) =>
    aFactura(await api.post<DocumentoApi>('/documentos', {
      tipo: data.estado === 'COTIZACION' ? 'COTIZACION' : 'FACTURA',
      clienteId: data.clienteId,
      proyectoId: data.proyectoId,
      fecha: hoyIso(),
      lineas: data.detalles.map(d => ({
        productoId: d.productoId,
        cantidad: d.cantidad,
        precioUnit: d.precioUnitarioVenta,
      })),
    })),

  /** El estado de pago ahora sale de los abonos: ya no se cambia a mano. */
  actualizar: async (_id: string, _data: Partial<FacturaPayload>): Promise<Factura> => {
    throw new Error('El estado de pago se calcula con los abonos registrados')
  },

  confirmar: async (id: string) =>
    aFactura(await api.post<DocumentoApi>(`/documentos/${id}/convertir-a-factura`, {})),

  listarAbonos: (facturaId: string) =>
    api.get<Abono[]>(`/documentos/${facturaId}/abonos`),

  registrarAbono: (facturaId: string, data: AbonoPayload) =>
    api.post<Abono>(`/documentos/${facturaId}/abonos`, data),
}
