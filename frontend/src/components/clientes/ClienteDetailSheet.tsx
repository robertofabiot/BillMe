import type { Cliente, Factura, Proyecto } from '@/types'
import { FACTURAS, PROYECTOS } from '@/data/mock'
import { formatCurrency, formatDate, getSaldo } from '@/lib/format'
import { EstadoBadge } from '@/components/facturas/EstadoBadge'
import {
  Phone, StickyNote, FolderKanban, ReceiptText,
  X, Pencil, TrendingUp, Clock,
} from 'lucide-react'
import { Button } from '@/components/ui/button'

interface Props {
  cliente: Cliente | null
  open: boolean
  onOpenChange: (v: boolean) => void
  onEdit: (cliente: Cliente) => void
}

export function ClienteDetailSheet({ cliente, open, onOpenChange, onEdit }: Props) {
  if (!open || !cliente) return null

  const facturas: Factura[] = FACTURAS.filter(f => f.clienteId === cliente.id)
  const proyectos: Proyecto[] = PROYECTOS.filter(p => p.clienteId === cliente.id)

  const totalFacturado = facturas.reduce((s, f) => s + f.totalVenta, 0)
  const totalCobrado   = facturas.reduce((s, f) => s + f.abonos.reduce((a, b) => a + b.monto, 0), 0)
  const porCobrar      = facturas.reduce((s, f) => {
    if (f.estado === 'PAGADO' || f.estado === 'COTIZACION') return s
    return s + getSaldo(f.totalVenta, f.abonos)
  }, 0)

  return (
    <div className="fixed inset-0 z-40 flex justify-end">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/20"
        onClick={() => onOpenChange(false)}
      />

      {/* Panel */}
      <div className="relative z-10 w-full max-w-[560px] h-full bg-white border-l border-[#E5E7EB] flex flex-col shadow-sm overflow-hidden">

        {/* Header */}
        <div className="flex items-start justify-between px-6 py-5 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0 mt-0.5">
              <span className="text-[#91E5F6] font-bold text-sm">
                {cliente.nombre.charAt(0).toUpperCase()}
              </span>
            </div>
            <div>
              <h2 className="text-base font-semibold text-[#022F40]">{cliente.nombre}</h2>
              {cliente.telefono && (
                <p className="text-xs text-[#705D56] flex items-center gap-1 mt-0.5">
                  <Phone className="w-3 h-3" /> {cliente.telefono}
                </p>
              )}
              {cliente.detalles && (
                <p className="text-xs text-[#80727B] flex items-center gap-1 mt-0.5">
                  <StickyNote className="w-3 h-3" /> {cliente.detalles}
                </p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <Button
              size="sm"
              variant="outline"
              onClick={() => onEdit(cliente)}
              className="rounded border-[#E5E7EB] text-[#022F40] h-8 text-xs gap-1"
            >
              <Pencil className="w-3.5 h-3.5" /> Editar
            </Button>
            <button
              onClick={() => onOpenChange(false)}
              className="w-8 h-8 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] transition-colors ml-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-3 border-b border-[#E5E7EB] shrink-0">
          {[
            { label: 'Total Facturado', value: formatCurrency(totalFacturado), icon: TrendingUp, color: 'text-[#022F40]' },
            { label: 'Total Cobrado',   value: formatCurrency(totalCobrado),   icon: TrendingUp, color: 'text-[#558564]' },
            { label: 'Por Cobrar',      value: formatCurrency(porCobrar),      icon: Clock,      color: porCobrar > 0 ? 'text-amber-600' : 'text-[#80727B]' },
          ].map(s => (
            <div key={s.label} className="flex flex-col items-start px-4 py-3 border-r border-[#E5E7EB] last:border-r-0">
              <p className="text-xs text-[#705D56]">{s.label}</p>
              <p className={`text-sm font-bold mt-0.5 ${s.color}`}>{s.value}</p>
            </div>
          ))}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-6">

          {/* Proyectos */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
              <FolderKanban className="w-3.5 h-3.5" /> Proyectos ({proyectos.length})
            </h3>
            {proyectos.length === 0 ? (
              <p className="text-xs text-[#80727B] py-2">Sin proyectos registrados.</p>
            ) : (
              <div className="flex flex-col gap-1.5">
                {proyectos.map(p => (
                  <div
                    key={p.id}
                    className="flex items-center justify-between px-3 py-2.5 bg-white border border-[#E5E7EB] rounded"
                  >
                    <div className="flex items-center gap-2">
                      <div className={`w-1.5 h-1.5 rounded-full shrink-0 ${p.estado === 'ACTIVO' ? 'bg-[#558564]' : 'bg-[#80727B]'}`} />
                      <span className="text-sm text-[#022F40] font-medium">{p.nombre}</span>
                    </div>
                    <span className={`text-xs px-2 py-0.5 rounded border ${
                      p.estado === 'ACTIVO'
                        ? 'bg-[#558564]/10 text-[#558564] border-[#558564]/20'
                        : 'bg-[#F9FAFB] text-[#80727B] border-[#E5E7EB]'
                    }`}>
                      {p.estado === 'ACTIVO' ? 'Activo' : 'Finalizado'}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Historial de facturas */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
              <ReceiptText className="w-3.5 h-3.5" /> Historial de Compras ({facturas.length})
            </h3>
            {facturas.length === 0 ? (
              <p className="text-xs text-[#80727B] py-2">Sin facturas registradas.</p>
            ) : (
              <div className="border border-[#E5E7EB] rounded overflow-hidden">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Folio</th>
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Fecha</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Total</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Saldo</th>
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {facturas
                      .slice()
                      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
                      .map((f, i) => {
                        const saldo = getSaldo(f.totalVenta, f.abonos)
                        return (
                          <tr
                            key={f.id}
                            className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] transition-colors ${i === facturas.length - 1 ? 'border-b-0' : ''}`}
                          >
                            <td className="px-3 py-2.5 font-mono font-semibold text-[#022F40]">{f.folioInterno}</td>
                            <td className="px-3 py-2.5 text-[#705D56]">{formatDate(f.createdAt)}</td>
                            <td className="px-3 py-2.5 text-right font-semibold text-[#022F40]">{formatCurrency(f.totalVenta)}</td>
                            <td className="px-3 py-2.5 text-right">
                              {f.estado === 'PAGADO'
                                ? <span className="text-[#558564]">Liquidado</span>
                                : f.estado === 'COTIZACION'
                                  ? <span className="text-[#80727B]">—</span>
                                  : <span className="text-[#022F40] font-medium">{formatCurrency(saldo)}</span>
                              }
                            </td>
                            <td className="px-3 py-2.5">
                              <EstadoBadge estado={f.estado} />
                            </td>
                          </tr>
                        )
                      })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
