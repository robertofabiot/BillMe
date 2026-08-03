export interface Producto {
  id: string
  codigoInterno: string
  nombrePrincipal: string
  descripcion?: string
  peso: number
  aliases: string[]
  precioListaProveedor?: number // Precio de lista original del proveedor
  descuentoProveedor?: number   // Descuento % que nos dio el proveedor
  costoNeto?: number            // Nuestro costo real
}

export interface DetalleFactura {
  key: string
  productoId: string
  nombre: string
  codigoInterno: string
  cantidad: number
  precioUnitario: number
  precioSugerido: number
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
  estado: 'COTIZACION' | 'PENDIENTE' | 'PAGO_PARCIAL' | 'PAGADO'
  costoRealEmpresa?: number
  totalVenta: number
  clienteId: string
  proyectoId?: string
  createdAt: string
  detalles: DetalleFactura[]
  abonos: Abono[]
}

export interface Cliente {
  id: string
  nombre: string
  telefono?: string
  detalles?: string
}

export interface Proyecto {
  id: string
  clienteId: string
  nombre: string
  estado: 'ACTIVO' | 'FINALIZADO'
}
