import type { Cliente, Proyecto, Producto, Factura } from '@/types'

export const CLIENTES: Cliente[] = [
  { id: 'c1', nombre: 'Juan Pérez', telefono: '555-1234' },
  { id: 'c2', nombre: 'Obra Norte SA', telefono: '555-5678', detalles: 'Construcción comercial' },
  { id: 'c3', nombre: 'María López', telefono: '555-9012' },
  { id: 'c4', nombre: 'Constructora Ríos', telefono: '555-3456', detalles: 'Proyectos residenciales' },
  { id: 'c5', nombre: 'Roberto Hernández', telefono: '555-7890' },
]

export const PROYECTOS: Proyecto[] = [
  { id: 'p1', nombre: 'Construcción Residencial Norte', estado: 'ACTIVO',    clienteId: 'c2' },
  { id: 'p2', nombre: 'Remodelación Oficinas',          estado: 'ACTIVO',    clienteId: 'c2' },
  { id: 'p3', nombre: 'Casa Habitación Lomas',          estado: 'ACTIVO',    clienteId: 'c4' },
  { id: 'p4', nombre: 'Bodega Industrial',              estado: 'FINALIZADO', clienteId: 'c4' },
]

export const PRODUCTOS: Producto[] = [
  { id: 'pr1', codigoInterno: 'TUB-001', nombrePrincipal: 'Tubo Galvanizado 1/2"',       peso: 2.5,  aliases: ['Tubo 1/2', 'Tubería 1/2'],        precioListaProveedor: 120,  descuentoProveedor: 10, costoNeto: 108 },
  { id: 'pr2', codigoInterno: 'TUB-002', nombrePrincipal: 'Tubo Galvanizado 3/4"',       peso: 3.2,  aliases: ['Tubo 3/4', 'Tubería 3/4'],        precioListaProveedor: 150,  descuentoProveedor: 10, costoNeto: 135 },
  { id: 'pr3', codigoInterno: 'VAR-001', nombrePrincipal: 'Varilla Corrugada 3/8"',      peso: 5.6,  aliases: ['Varilla 3/8', 'Hierro 3/8'],       precioListaProveedor: 310,  descuentoProveedor: 20, costoNeto: 248 },
  { id: 'pr4', codigoInterno: 'VAR-002', nombrePrincipal: 'Varilla Corrugada 1/2"',      peso: 9.8,  aliases: ['Varilla 1/2', 'Hierro 1/2'],       precioListaProveedor: 215,  descuentoProveedor: 20, costoNeto: 172 },
  { id: 'pr5', codigoInterno: 'LAM-001', nombrePrincipal: 'Lámina Galvanizada Cal. 26',  peso: 8.2,  aliases: ['Lámina 26', 'Galvanizada 26'],      precioListaProveedor: 253.33, descuentoProveedor: 10, costoNeto: 228 },
  { id: 'pr6', codigoInterno: 'CEM-001', nombrePrincipal: 'Cemento Gris 50kg',           peso: 50.0, aliases: ['Cemento', 'Bulto cemento'],         precioListaProveedor: 164.44, descuentoProveedor: 10, costoNeto: 148 },
  { id: 'pr7', codigoInterno: 'CAB-001', nombrePrincipal: 'Cable THHN 12 AWG',           peso: 0.3,  aliases: ['Cable 12', 'THHN 12'],             precioListaProveedor: 80,   descuentoProveedor: 15, costoNeto: 68 },
  { id: 'pr8', codigoInterno: 'TUB-003', nombrePrincipal: 'Tubo Conduit 3/4"',           peso: 1.8,  aliases: ['Conduit 3/4'],                     precioListaProveedor: 72.94,  descuentoProveedor: 15, costoNeto: 62 },
]

export const FACTURAS: Factura[] = [
  {
    id: 'f1', folioInterno: 'FAC-0001', estado: 'PAGADO',
    totalVenta: 12500, clienteId: 'c1', costoRealEmpresa: 9800, createdAt: '2026-07-28',
    detalles: [
      { id: 'd1', productoId: 'pr1', productoNombre: 'Tubo Galvanizado 1/2"',  productoCodigoInterno: 'TUB-001', cantidad: 50,  precioUnitarioVenta: 145,    costoUnitario: 108   },
      { id: 'd2', productoId: 'pr3', productoNombre: 'Varilla Corrugada 3/8"', productoCodigoInterno: 'VAR-001', cantidad: 20,  precioUnitarioVenta: 322.50, costoUnitario: 248   },
    ],
    abonos: [
      { id: 'a1', monto: 7000, fechaPago: '2026-07-29' },
      { id: 'a2', monto: 5500, fechaPago: '2026-08-01' },
    ],
  },
  {
    id: 'f2', folioInterno: 'FAC-0002', estado: 'PAGO_PARCIAL',
    totalVenta: 45200, clienteId: 'c2', proyectoId: 'p1', createdAt: '2026-07-30',
    detalles: [
      { id: 'd3', productoId: 'pr4', productoNombre: 'Varilla Corrugada 1/2"',     productoCodigoInterno: 'VAR-002', cantidad: 100, precioUnitarioVenta: 218, costoUnitario: 172 },
      { id: 'd4', productoId: 'pr5', productoNombre: 'Lámina Galvanizada Cal. 26', productoCodigoInterno: 'LAM-001', cantidad: 80,  precioUnitarioVenta: 290, costoUnitario: 228 },
      { id: 'd5', productoId: 'pr6', productoNombre: 'Cemento Gris 50kg',          productoCodigoInterno: 'CEM-001', cantidad: 20,  precioUnitarioVenta: 185, costoUnitario: 148 },
    ],
    abonos: [
      { id: 'a3', monto: 20000, fechaPago: '2026-07-31' },
    ],
  },
  {
    id: 'f3', folioInterno: 'FAC-0003', estado: 'PENDIENTE',
    totalVenta: 8900, clienteId: 'c3', createdAt: '2026-08-01',
    detalles: [
      { id: 'd6', productoId: 'pr7', productoNombre: 'Cable THHN 12 AWG', productoCodigoInterno: 'CAB-001', cantidad: 100, precioUnitarioVenta: 89, costoUnitario: 68 },
    ],
    abonos: [],
  },
  {
    id: 'f4', folioInterno: 'FAC-0004', estado: 'COTIZACION',
    totalVenta: 3200, clienteId: 'c4', proyectoId: 'p3', createdAt: '2026-08-02',
    detalles: [
      { id: 'd7', productoId: 'pr8', productoNombre: 'Tubo Conduit 3/4"', productoCodigoInterno: 'TUB-003', cantidad: 40, precioUnitarioVenta: 80, costoUnitario: 62 },
    ],
    abonos: [],
  },
]

export const USUARIOS = [
  { id: 'u1', username: 'admin', rol: 'ADMIN', nombre: 'Administrador' },
  { id: 'u2', username: 'ventas1', rol: 'VENDEDOR', nombre: 'Vendedor Principal' },
]

export const CONSOLIDADOS: any[] = []
