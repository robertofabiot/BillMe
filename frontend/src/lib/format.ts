export const fmt = new Intl.NumberFormat('es-MX', {
  style: 'currency',
  currency: 'MXN',
  minimumFractionDigits: 2,
})

export function formatCurrency(amount: number): string {
  return fmt.format(amount)
}

export function formatDate(dateStr: string): string {
  return new Date(`${dateStr}T12:00:00`).toLocaleDateString('es-MX', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function getSaldo(totalVenta: number, abonos: { monto: number }[]): number {
  const abonado = abonos.reduce((sum, a) => sum + a.monto, 0)
  return totalVenta - abonado
}
