export const fmt = new Intl.NumberFormat('es-NI', {
  style: 'currency',
  currency: 'NIO',
  minimumFractionDigits: 2,
})

export function formatCurrency(amount: number): string {
  return fmt.format(amount)
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

export function getSaldo(totalVenta: number, abonos: { monto: number }[]): number {
  const abonado = abonos.reduce((sum, a) => sum + a.monto, 0)
  return totalVenta - abonado
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
