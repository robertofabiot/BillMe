import { useState, useMemo, type ChangeEvent } from 'react'
import type { Factura, Abono, EstadoFactura } from '@/types'
import { CLIENTES, PROYECTOS, FACTURAS } from '@/data/mock'
import { formatCurrency, formatDate, getSaldo } from '@/lib/format'
import { EstadoBadge } from '@/components/facturas/EstadoBadge'
import { NuevaFacturaSheet } from '@/components/facturas/NuevaFacturaSheet'
import { FacturaDetailSheet } from '@/components/facturas/FacturaDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  ReceiptText, Plus, Search, TrendingUp,
  Clock, Banknote, FileText,
} from 'lucide-react'

type FiltroEstado = EstadoFactura | 'TODOS'

function StatCard({
  icon: Icon, label, value, sub, accent,
}: {
  icon: React.ElementType
  label: string
  value: string
  sub?: string
  accent?: string
}) {
  return (
    <div className="bg-white border border-[#E5E7EB] rounded p-4 flex items-start gap-3 shadow-sm">
      <div className={`w-8 h-8 rounded flex items-center justify-center shrink-0 ${accent ?? 'bg-[#F9FAFB]'}`}>
        <Icon className={`w-4 h-4 ${accent ? 'text-white' : 'text-[#80727B]'}`} />
      </div>
      <div>
        <p className="text-xs text-[#705D56]">{label}</p>
        <p className="text-lg font-bold text-[#022F40] leading-tight">{value}</p>
        {sub && <p className="text-xs text-[#80727B] mt-0.5">{sub}</p>}
      </div>
    </div>
  )
}

export function FacturasPage() {
  const [facturas, setFacturas] = useState<Factura[]>(FACTURAS)
  const [selected, setSelected] = useState<Factura | null>(null)
  const [showNueva, setShowNueva] = useState(false)
  const [search, setSearch] = useState('')
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('TODOS')

  // ── Stats ──────────────────────────────────────────────
  const stats = useMemo(() => {
    const porCobrar = facturas.reduce((sum, f) => {
      if (f.estado === 'PAGADO' || f.estado === 'COTIZACION') return sum
      return sum + getSaldo(f.totalVenta, f.abonos)
    }, 0)
    const cobrado = facturas.reduce((sum, f) =>
      sum + f.abonos.reduce((s, a) => s + a.monto, 0), 0
    )
    const cotizaciones = facturas.filter(f => f.estado === 'COTIZACION').length
    const pendientes = facturas.filter(f => f.estado === 'PENDIENTE' || f.estado === 'PAGO_PARCIAL').length
    return { porCobrar, cobrado, cotizaciones, pendientes }
  }, [facturas])

  // ── Next folio ─────────────────────────────────────────
  const nextFolio = useMemo(() => {
    const nums = facturas
      .map(f => parseInt(f.folioInterno.replace('FAC-', ''), 10))
      .filter(n => !isNaN(n))
    const max = nums.length > 0 ? Math.max(...nums) : 0
    return `FAC-${String(max + 1).padStart(4, '0')}`
  }, [facturas])

  // ── Filtered rows ──────────────────────────────────────
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return facturas.filter(f => {
      const cliente = CLIENTES.find(c => c.id === f.clienteId)
      const matchSearch = !q ||
        f.folioInterno.toLowerCase().includes(q) ||
        (cliente?.nombre.toLowerCase().includes(q) ?? false)
      const matchEstado = filtroEstado === 'TODOS' || f.estado === filtroEstado
      return matchSearch && matchEstado
    })
  }, [facturas, search, filtroEstado])

  // ── Handlers ───────────────────────────────────────────
  const handleSaveFactura = (nueva: Factura) => {
    setFacturas(prev => [nueva, ...prev])
  }

  const handleAddAbono = (facturaId: string, abono: Abono) => {
    setFacturas(prev =>
      prev.map(f => {
        if (f.id !== facturaId) return f
        const abonos = [...f.abonos, abono]
        const totalAbonado = abonos.reduce((s, a) => s + a.monto, 0)
        const estado: EstadoFactura =
          totalAbonado >= f.totalVenta ? 'PAGADO' :
          totalAbonado > 0 ? 'PAGO_PARCIAL' : f.estado
        return { ...f, abonos, estado }
      })
    )
    // Update selected too
    setSelected(prev => {
      if (!prev || prev.id !== facturaId) return prev
      const abonos = [...prev.abonos, abono]
      const totalAbonado = abonos.reduce((s, a) => s + a.monto, 0)
      const estado: EstadoFactura =
        totalAbonado >= prev.totalVenta ? 'PAGADO' :
        totalAbonado > 0 ? 'PAGO_PARCIAL' : prev.estado
      return { ...prev, abonos, estado }
    })
  }

  const handleUpdateEstado = (facturaId: string, estado: EstadoFactura) => {
    setFacturas(prev => prev.map(f => f.id === facturaId ? { ...f, estado } : f))
    setSelected(prev => prev?.id === facturaId ? { ...prev, estado } : prev)
  }

  return (
    <div className="p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <ReceiptText className="w-5 h-5" />
            Facturas
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Gestión de cotizaciones, ventas y pagos</p>
        </div>
        <Button
          onClick={() => setShowNueva(true)}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-2"
        >
          <Plus className="w-4 h-4" />
          Nueva Factura
        </Button>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard icon={FileText}   label="Cotizaciones"  value={String(stats.cotizaciones)} sub="pendientes de confirmar" />
        <StatCard icon={Clock}      label="Facturas Abiertas" value={String(stats.pendientes)} sub="pendiente o pago parcial" />
        <StatCard icon={Banknote}   label="Por Cobrar"    value={formatCurrency(stats.porCobrar)} accent="bg-[#022F40]" />
        <StatCard icon={TrendingUp} label="Total Cobrado" value={formatCurrency(stats.cobrado)}  accent="bg-[#558564]" />
      </div>

      {/* ── Filters ── */}
      <div className="flex gap-3">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
          <Input
            value={search}
            onChange={(e: ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
            placeholder="Buscar por folio o cliente…"
            className="pl-8 rounded border-[#E5E7EB] text-sm h-9"
          />
        </div>
        <Select value={filtroEstado} onValueChange={(v) => setFiltroEstado(v as FiltroEstado)}>
          <SelectTrigger className="w-44 rounded border-[#E5E7EB] text-sm h-9">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="TODOS">Todos los estados</SelectItem>
            <SelectItem value="COTIZACION">Cotización</SelectItem>
            <SelectItem value="PENDIENTE">Pendiente</SelectItem>
            <SelectItem value="PAGO_PARCIAL">Pago Parcial</SelectItem>
            <SelectItem value="PAGADO">Pagado</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* ── Table ── */}
      <div className="bg-white border border-[#E5E7EB] rounded overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#E5E7EB] bg-[#F9FAFB]">
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Folio</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Cliente</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Proyecto</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Fecha</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Total</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Saldo</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Estado</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-[#80727B] text-sm">
                  No se encontraron facturas
                </td>
              </tr>
            )}
            {filtered.map((f, i) => {
              const cliente = CLIENTES.find(c => c.id === f.clienteId)
              const proyecto = f.proyectoId ? PROYECTOS.find(p => p.id === f.proyectoId) : null
              const saldo = getSaldo(f.totalVenta, f.abonos)
              const isLast = i === filtered.length - 1
              return (
                <tr
                  key={f.id}
                  onClick={() => setSelected(f)}
                  className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] cursor-pointer transition-colors ${isLast ? 'border-b-0' : ''}`}
                >
                  <td className="px-4 py-3 font-mono font-semibold text-[#022F40]">{f.folioInterno}</td>
                  <td className="px-4 py-3 text-[#022F40]">{cliente?.nombre ?? '—'}</td>
                  <td className="px-4 py-3 text-[#705D56]">
                    {proyecto
                      ? <span className="text-xs bg-[#F9FAFB] border border-[#E5E7EB] rounded px-2 py-0.5">{proyecto.nombre}</span>
                      : <span className="text-[#80727B]">—</span>
                    }
                  </td>
                  <td className="px-4 py-3 text-[#705D56]">{formatDate(f.createdAt)}</td>
                  <td className="px-4 py-3 text-right font-semibold text-[#022F40]">{formatCurrency(f.totalVenta)}</td>
                  <td className="px-4 py-3 text-right">
                    {f.estado === 'PAGADO'
                      ? <span className="text-[#558564] font-medium text-xs">Liquidado</span>
                      : f.estado === 'COTIZACION'
                        ? <span className="text-[#80727B] text-xs">—</span>
                        : <span className={`font-semibold ${saldo > 0 ? 'text-[#022F40]' : 'text-[#558564]'}`}>
                            {formatCurrency(saldo)}
                          </span>
                    }
                  </td>
                  <td className="px-4 py-3">
                    <EstadoBadge estado={f.estado} />
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* ── Sheets ── */}
      <NuevaFacturaSheet
        open={showNueva}
        onOpenChange={setShowNueva}
        onSave={handleSaveFactura}
        nextFolio={nextFolio}
      />
      <FacturaDetailSheet
        factura={selected}
        open={selected !== null}
        onOpenChange={(v) => { if (!v) setSelected(null) }}
        onAddAbono={handleAddAbono}
        onUpdateEstado={handleUpdateEstado}
      />
    </div>
  )
}
