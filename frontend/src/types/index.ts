// ─── Tipos alineados con los DTOs del backend ───────────────────────

export type EstadoProyecto = 'ACTIVO' | 'FINALIZADO'

export interface Producto {
  id: string
  codigoInterno: string
  nombrePrincipal: string
  descripcion?: string
  unidad: string
  familia?: string
  peso: number
  generico: boolean
  formaCalculo: 'NINGUNA' | 'PLACA' | 'PLATINA' | 'ANGULAR'
  /** Precio de lista del proveedor sin IVA más reciente. */
  precioListaVigente?: number
  fechaPrecioLista?: string
  aliases: string[]
  createdAt?: string
}

export interface Abono {
  id: string
  monto: number
  fechaPago: string
}

export interface Cliente {
  id: string
  nombre: string
  telefono?: string
  detalles?: string
  descuentoPct?: number
  tarifaKgUsd?: number | null
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

export type UnidadLongitud = 'PULGADA' | 'PIE' | 'METRO' | 'CM' | 'MM'

export interface Medida {
  texto: string
  unidad: UnidadLongitud
}

export interface CalculoPesoRequest {
  forma: 'PLACA' | 'PLATINA' | 'ANGULAR'
  ancho: Medida
  largo: Medida
  espesor: Medida
  ala2?: Medida | null
}

export interface CalculoPesoResponse {
  pesoUnitKg: number
  descripcion: string
}

export interface PrecioCalculadoResponse {
  precioUnit: number
}

export interface PrecioSugerido {
  precioUnit?: number | null
  origen?: 'ESTE_CLIENTE' | 'OTRO_CLIENTE' | null
  clienteNombre?: string | null
  folio?: string | null
  fecha?: string | null
  diasAntiguedad?: number | null
  viejo?: boolean
  precioLista?: number | null
  descuentoPct?: number | null
  precioFormula?: number | null
  tarifaKgUsd?: number | null
  origenTarifa?: 'ULTIMA' | 'CLIENTE' | 'CONFIGURACION' | null
}

export interface PrecioFormula {
  precioLista: number
  descuentoPct: number
  precioUnit: number
}

export interface PrecioHistorialFila {
  documentoId: string
  folio: string
  tipo: TipoDocumento
  fecha: string
  clienteNombre: string
  descripcion: string
  cantidad: number
  precioUnit: number
  viejo: boolean
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
}

export interface Usuario {
  id: string
  username: string
  nombre: string
  activo: boolean
}

export interface ConfiguracionSistema {
  ivaPct: number
  tipoCambio: number
  tarifaKgUsd: number
  densidadKgIn3: number
  diasPrecioViejo: number
}

export type FuentePrecio = 'LISTA_IMPORTADA' | 'FACTURA_PROVEEDOR' | 'PRECIO_CLIENTE' | 'MANUAL'

/** Precio de lista del proveedor (sin IVA) vigente desde una fecha. */
export interface PrecioLista {
  id: string
  precioSinIva: number
  fecha: string
  fuente: FuentePrecio
  descuentoPct?: number | null
  precioOrigen?: number | null
  nota?: string | null
}

export interface PreciosListaProducto {
  vigente: PrecioLista | null
  historial: PrecioLista[]
  descuentoProveedorSugerido: number | null
}

/** Resumen de importar la lista de precios del proveedor (vista previa o aplicada). */
export interface ImportacionLista {
  hoja: string
  filasLeidas: number
  filasIgnoradas: number
  productosNuevos: number
  preciosQueCambian: number
  sinCambios: number
  sinPrecio: number
  muestra: { codigo: string; descripcion: string; precioAnterior: number | null; precioNuevo: number | null }[]
  aplicada: boolean
}

// ─── Cotizaciones y facturas ────────────────────────────────────────────

export type TipoDocumento = 'COTIZACION' | 'FACTURA'
/** Cotización: PENDIENTE, ACEPTADA o RECHAZADA. Factura: ACTIVA o ANULADA. */
export type EstadoDocumento = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'ACTIVA' | 'ANULADA'
export type MetodoPrecio = 'ULTIMO_PRECIO' | 'FORMULA_LISTA' | 'POR_PESO' | 'POR_METRO' | 'MANUAL'

export interface LineaDocumento {
  id: string
  orden: number
  productoId: string
  productoCodigo: string
  productoGenerico: boolean
  productoForma?: 'NINGUNA' | 'PLACA' | 'PLATINA' | 'ANGULAR'
  descripcion: string
  cantidad: number
  pesoUnitKg?: number | null
  pesoTotalKg?: number | null
  /** Precio unitario con IVA. */
  precioUnit: number
  total: number
  metodoPrecio: MetodoPrecio
  parametros?: string | null
  fechaEntrega?: string | null
  lugarEntrega?: string | null
  costoUnitConIva?: number | null
}

export interface Documento {
  id: string
  tipo: TipoDocumento
  folio: string
  estado: EstadoDocumento
  fecha: string
  clienteId: string
  clienteNombre: string
  proyectoId?: string | null
  proyectoNombre?: string | null
  notas?: string | null
  mostrarEntrega: boolean
  total: number
  totalKg: number
  cotizacionOrigenId?: string | null
  cotizacionOrigenFolio?: string | null
  facturaGeneradaId?: string | null
  facturaGeneradaFolio?: string | null
  lineas: LineaDocumento[]
  abonos: Abono[]
  totalAbonado: number
  /** Solo en facturas. */
  saldoPendiente?: number | null
  createdAt: string
  updatedAt: string
}

/** Cómo estaba un documento antes de una edición. */
export interface RevisionDocumento {
  numero: number
  editadoEn: string
  editadoPor?: string | null
  documento: Documento
}
