// ─── Tipos alineados con los DTOs del backend ───────────────────────

export type EstadoFactura = 'COTIZACION' | 'PENDIENTE' | 'PAGO_PARCIAL' | 'PAGADO'
export type EstadoProyecto = 'ACTIVO' | 'FINALIZADO'

export interface Producto {
  id: string
  codigoInterno: string
  nombrePrincipal: string
  descripcion?: string
  peso: number
  aliases: string[]
  precioListaProveedor?: number
  descuentoProveedor?: number
  costoNeto?: number
  createdAt?: string
}

export interface DetalleFactura {
  id: string
  productoId: string
  productoNombre: string
  productoCodigoInterno: string
  cantidad: number
  precioUnitarioVenta: number
  costoUnitario?: number
}

export interface Abono {
  id: string
  monto: number
  fechaPago: string
}

export interface Factura {
  id: string
  folioInterno: string
  estado: EstadoFactura
  costoRealEmpresa?: number
  totalVenta: number
  clienteId: string
  clienteNombre?: string
  proyectoId?: string
  proyectoNombre?: string
  createdAt: string
  detalles: DetalleFactura[]
  abonos: Abono[]
  totalAbonado?: number
  saldoPendiente?: number
}

export interface Cliente {
  id: string
  nombre: string
  telefono?: string
  detalles?: string
  createdAt?: string
}

export interface Proyecto {
  id: string
  clienteId: string
  clienteNombre?: string
  nombre: string
  estado: EstadoProyecto
  createdAt?: string
}

export interface ConsolidadoItem {
  id: string
  facturaId?: string
  productoNombre: string
  cantidad: number
  precioUnitario: number
  orden?: number
}

export interface ConsolidadoGroup {
  id: string
  nombre: string
  items: ConsolidadoItem[]
  orden?: number
  subtotal?: number
}

export interface Consolidado {
  id: string
  folioInterno: string
  clienteId: string
  clienteNombre?: string
  nombre: string
  createdAt: string
  grupos: ConsolidadoGroup[]
  totalGeneral?: number
}

export interface ReporteData {
  ingresosTotales: number
  totalCobrado: number
  porCobrar: number
  costoTotal: number
  gananciaNeta: number
  totalFacturas: number
}

export interface PrecioHistorial {
  folioInterno: string
  fecha: string
  precioUnitario: number
  cantidad: number
}

// ─── Tipos de Autenticación ─────────────────────────────────────────────

export interface AuthRequest {
  username: string
  password?: string // Opcional porque solo se usa para enviar
}

export interface AuthResponse {
  token: string
  username: string
  nombre: string
  rol: 'ADMIN' | 'VENDEDOR'
}
