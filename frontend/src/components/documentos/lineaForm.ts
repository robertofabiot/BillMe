import type { MetodoPrecio } from '@/types'

/** Una línea mientras se edita: los números son texto para poder escribir decimales con calma. */
export interface LineaForm {
  clave: string
  id?: string
  productoId: string
  productoCodigo: string
  productoGenerico: boolean
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
