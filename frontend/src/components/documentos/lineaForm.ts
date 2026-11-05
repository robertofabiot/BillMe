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

const r2 = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100

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
      return { texto: `${Number(p.tarifaUsd).toFixed(2)} USD por kg` }
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
