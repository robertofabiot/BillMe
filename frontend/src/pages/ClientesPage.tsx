import { useState, useMemo, useEffect, type ChangeEvent } from 'react'
import type { Cliente, Factura } from '@/types'
import { clienteService } from '@/services/clienteService'
import { facturaService } from '@/services/facturaService'
import { formatCurrency } from '@/lib/format'
import { ClienteModal } from '@/components/clientes/ClienteModal'
import { ClienteDetailSheet } from '@/components/clientes/ClienteDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Users, Plus, Search, TrendingUp, Banknote, UserCheck,
} from 'lucide-react'

function StatCard({ icon: Icon, label, value, accent }: {
  icon: React.ElementType; label: string; value: string; accent?: string
}) {
  return (
    <div className="bg-white border border-[#E5E7EB] rounded p-4 flex items-start gap-3 shadow-sm">
      <div className={`w-8 h-8 rounded flex items-center justify-center shrink-0 ${accent ?? 'bg-[#F9FAFB]'}`}>
        <Icon className={`w-4 h-4 ${accent ? 'text-white' : 'text-[#80727B]'}`} />
      </div>
      <div>
        <p className="text-xs text-[#705D56]">{label}</p>
        <p className="text-lg font-bold text-[#022F40] leading-tight">{value}</p>
      </div>
    </div>
  )
}

export function ClientesPage() {
  const [clientes, setClientes]         = useState<Cliente[]>([])
  const [facturas, setFacturas]         = useState<Factura[]>([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState<string | null>(null)
  const [selected, setSelected]         = useState<Cliente | null>(null)
  const [editTarget, setEditTarget]     = useState<Cliente | null>(null)
  const [showModal, setShowModal]       = useState(false)
  const [search, setSearch]             = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const [clientesData, facturasData] = await Promise.all([
          clienteService.listar(),
          facturaService.listar()
        ])
        setClientes(clientesData)
        setFacturas(facturasData)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Error al cargar datos')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  /* ── Stats ── */
  const stats = useMemo(() => {
    const conPendientes = new Set(
      facturas
        .filter(f => f.estado === 'PENDIENTE' || f.estado === 'PAGO_PARCIAL')
        .map(f => f.clienteId)
    ).size
    const totalFacturado = facturas.reduce((s, f) => s + f.totalVenta, 0)
    const totalCobrado   = facturas.reduce((s, f) =>
      s + f.abonos.reduce((a, b) => a + b.monto, 0), 0
    )
    return { total: clientes.length, conPendientes, totalFacturado, totalCobrado }
  }, [clientes, facturas])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return clientes.filter(c =>
      !q || c.nombre.toLowerCase().includes(q) || (c.telefono?.includes(q) ?? false)
    )
  }, [clientes, search])

  /* ── Helpers ── */
  const facturasPorCliente = (clienteId: string) =>
    facturas.filter(f => f.clienteId === clienteId)

  const totalPorCliente = (clienteId: string) =>
    facturasPorCliente(clienteId).reduce((s, f) => s + f.totalVenta, 0)

  /* ── Handlers ── */
  const openCreate = () => { setEditTarget(null); setShowModal(true) }
  const openEdit   = (c: Cliente) => { setEditTarget(c); setShowModal(true); setSelected(null) }

  const handleSave = async (data: Omit<Cliente, 'id'> & { id?: string }) => {
    try {
      if (data.id) {
        await clienteService.actualizar(data.id, data)
      } else {
        await clienteService.crear(data)
      }
      const newData = await clienteService.listar()
      setClientes(newData)
    } catch (e) {
      console.error(e)
    }
  }

  if (loading) return (
    <div className="p-8 flex items-center justify-center h-64">
      <div className="text-sm text-[#705D56]">Cargando...</div>
    </div>
  )
  if (error) return (
    <div className="p-8 flex items-center justify-center h-64">
      <div className="text-sm text-red-600">{error}</div>
    </div>
  )

  return (
    <div className="p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Users className="w-5 h-5" /> Clientes
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Directorio y historial de compras</p>
        </div>
        <Button
          onClick={openCreate}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo Cliente
        </Button>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard icon={Users}     label="Total Clientes"           value={String(stats.total)} />
        <StatCard icon={UserCheck} label="Con Facturas Pendientes"   value={String(stats.conPendientes)} />
        <StatCard icon={TrendingUp} label="Total Facturado"          value={formatCurrency(stats.totalFacturado)} accent="bg-[#022F40]" />
        <StatCard icon={Banknote}  label="Total Cobrado"             value={formatCurrency(stats.totalCobrado)}   accent="bg-[#558564]" />
      </div>

      {/* ── Search ── */}
      <div className="relative max-w-xs">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
        <Input
          value={search}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
          placeholder="Buscar por nombre o teléfono…"
          className="pl-8"
        />
      </div>

      {/* ── Grid de clientes ── */}
      {filtered.length === 0 ? (
        <div className="bg-white border border-[#E5E7EB] rounded p-12 text-center shadow-sm">
          <Users className="w-10 h-10 mx-auto text-[#80727B] mb-3" />
          <p className="text-sm text-[#705D56]">No se encontraron clientes.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(cliente => {
            const facturas   = facturasPorCliente(cliente.id)
            const total      = totalPorCliente(cliente.id)
            const pendientes = facturas.filter(f => f.estado === 'PENDIENTE' || f.estado === 'PAGO_PARCIAL').length
            const inicial    = cliente.nombre.charAt(0).toUpperCase()

            return (
              <button
                key={cliente.id}
                onClick={() => setSelected(cliente)}
                className="bg-white border border-[#E5E7EB] rounded p-4 text-left hover:border-[#022F40]/20 hover:shadow-sm transition-all group flex flex-col gap-3"
              >
                {/* Avatar + nombre */}
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0">
                    <span className="text-[#91E5F6] font-bold text-sm">{inicial}</span>
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-[#022F40] truncate group-hover:text-[#022F40]">
                      {cliente.nombre}
                    </p>
                    {cliente.telefono
                      ? <p className="text-xs text-[#705D56]">{cliente.telefono}</p>
                      : <p className="text-xs text-[#80727B]">Sin teléfono</p>
                    }
                    {cliente.detalles && (
                      <p className="text-xs text-[#80727B] truncate mt-0.5">{cliente.detalles}</p>
                    )}
                  </div>
                </div>

                {/* Divider */}
                <div className="h-px bg-[#E5E7EB]" />

                {/* Stats row */}
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Facturas</p>
                    <p className="text-sm font-bold text-[#022F40]">{facturas.length}</p>
                  </div>
                  <div>
                    <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Pendientes</p>
                    <p className={`text-sm font-bold ${pendientes > 0 ? 'text-amber-600' : 'text-[#80727B]'}`}>
                      {pendientes}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Facturado</p>
                    <p className="text-xs font-bold text-[#022F40]">
                      {total > 0 ? formatCurrency(total) : '—'}
                    </p>
                  </div>
                </div>
              </button>
            )
          })}
        </div>
      )}

      {/* ── Modales ── */}
      <ClienteModal
        open={showModal}
        onOpenChange={setShowModal}
        initial={editTarget}
        onSave={handleSave}
      />
      <ClienteDetailSheet
        cliente={selected}
        open={selected !== null}
        onOpenChange={v => { if (!v) setSelected(null) }}
        onEdit={openEdit}
      />
    </div>
  )
}
