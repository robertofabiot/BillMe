import type { MetodoPrecio } from '@/types'
import { formatCurrency, formatDate } from '@/lib/format'

/** Una línea mientras se edita: los números son texto para poder escribir decimales con calma. */
export interface LineaForm {
  clave: string
  id?: string
  productoId: string
  productoCodigo: string
  productoGenerico: boolean
  productoForma: 'NINGUNA' | 'PLACA' | 'PLATINA' | 'ANGULAR'
  descripcion: string
  cantidad: string
  pesoUnitKg: string
  precioUnit: string
  metodoPrecio: MetodoPrecio
  parametros?: string | null
  fechaEntrega: string
  lugarEntrega: string
}

export const r2 = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100

export function recalcularPrecioPorPeso(
  metodoPrecio: MetodoPrecio, parametros: string | null | undefined,
  pesoTexto: string, tipoCambio: number, ivaPct: number,
): string | null {
  if (metodoPrecio !== 'POR_PESO' || !parametros) return null
  try {
    const p = JSON.parse(parametros)
    const peso = Number(pesoTexto)
    const tarifa = Number(p.tarifa)
    if (!(peso > 0) || !(tarifa > 0)) return null
    const cambio = p.moneda === 'NIO' ? 1 : tipoCambio
    return String(r2(r2(peso) * tarifa * cambio * (1 + ivaPct / 100)))
  } catch {
    return null
  }
}

/** Solo para mostrar mientras se escribe: al guardar, los montos oficiales los calcula el backend. */
export function calcularLinea(l: LineaForm) {
  const cantidad = Number(l.cantidad) || 0
  const pu = r2(Number(l.precioUnit) || 0)
  const peso = l.pesoUnitKg.trim() === '' ? null : r2(Number(l.pesoUnitKg) || 0)
  return { total: r2(cantidad * pu), pesoTotal: peso === null ? null : r2(cantidad * peso) }
}

export function formatearTagPrecio(l: LineaForm): { texto: string; viejo?: boolean; dias?: number } | null {
  if (!l.parametros || l.metodoPrecio === 'MANUAL') return null
  try {
    const p = typeof l.parametros === 'string' ? JSON.parse(l.parametros) : l.parametros
    if (!p) return null
    if (l.metodoPrecio === 'ULTIMO_PRECIO') {
      const origen = p.otroCliente && p.clienteNombre ? `Precio de ${p.clienteNombre}` : 'Último precio'
      const folio = p.folio ? ` · ${p.folio}` : ''
      const fecha = p.fecha ? ` · ${formatDate(p.fecha)}` : ''
      return { texto: `${origen}${folio}${fecha}`, viejo: Boolean(p.viejo), dias: p.dias }
    }
    if (l.metodoPrecio === 'FORMULA_LISTA') {
      return { texto: `Lista ${formatCurrency(p.lista)} − ${p.descuentoPct} %` }
    }
    if (l.metodoPrecio === 'POR_PESO') {
      return { texto: `${Number(p.tarifa).toFixed(2)} ${p.moneda === 'NIO' ? 'C$' : 'USD'} por kg` }
    }
    if (l.metodoPrecio === 'POR_METRO') {
      const puMetro = p.largoReferencia ? p.precioReferencia / p.largoReferencia : 0
      return { texto: `${formatCurrency(puMetro)} por metro` }
    }
  } catch {
    return null
  }
  return null
}
