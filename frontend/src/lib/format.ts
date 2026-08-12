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
