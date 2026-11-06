export const fmt = new Intl.NumberFormat('es-NI', {
  style: 'currency',
  currency: 'NIO',
  minimumFractionDigits: 2,
})

export function formatCurrency(amount: number): string {
  return fmt.format(amount)
}

export function formatUsd(monto: number): string {
  return 'U$' + monto.toLocaleString('es-NI', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return 'N/A'
  // Si ya es un timestamp ISO, no agregamos T12
  const d = dateStr.includes('T') ? new Date(dateStr) : new Date(`${dateStr}T12:00:00`);
  return d.toLocaleDateString('es-MX', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/**
 * Fecha de hoy en formato AAAA-MM-DD según el reloj local.
 * No usar toISOString(): da la fecha UTC, que en Nicaragua ya es "mañana" desde las 18:00.
 */
export function hoyIso(): string {
  const ahora = new Date()
  const mes = String(ahora.getMonth() + 1).padStart(2, '0')
  const dia = String(ahora.getDate()).padStart(2, '0')
  return `${ahora.getFullYear()}-${mes}-${dia}`
}

/**
 * Convierte una fecha ISO (o AAAA-MM-DD) al formato dd/mm/aaaa.
 * Si la fecha viene vacía retorna una cadena vacía.
 */
export function formatFechaCorta(fecha: string): string {
  if (!fecha) return ''
  const parte = fecha.includes('T') ? fecha.split('T')[0] : fecha
  const [yyyy, mm, dd] = parte.split('-')
  if (yyyy && mm && dd) {
    return `${dd}/${mm}/${yyyy}`
  }
  return fecha
}

/**
 * Muestra el estado del saldo de la cuenta:
 * - Debe C$X cuando es positivo.
 * - Saldo a favor C$X cuando es negativo.
 * - Al día cuando es cero o nulo.
 */
export function textoSaldo(saldo?: number | null): string {
  if (saldo == null || saldo === 0) return 'Al día'
  const formatted = Math.abs(saldo).toLocaleString('es-NI', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
  if (saldo > 0) return `Debe C$${formatted}`
  return `Saldo a favor C$${formatted}`
}

