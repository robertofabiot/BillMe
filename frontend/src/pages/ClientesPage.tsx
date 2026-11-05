import { useState, useMemo, useEffect, type ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Cliente } from '@/types'
import { clienteService } from '@/services/clienteService'
import { formatCurrency, textoSaldo } from '@/lib/format'
import { ClienteModal } from '@/components/clientes/ClienteModal'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Users, Plus, Search, UserCheck, Clock,
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
        <p className="text-sm text-[#705D56]">{label}</p>
        <p className="text-lg font-bold text-[#022F40] leading-tight">{value}</p>
      </div>
    </div>
  )
}

export function ClientesPage() {
  const navigate = useNavigate()
  const [clientes, setClientes]   = useState<Cliente[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState<string | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [search, setSearch]       = useState('')

  const cargarClientes = async () => {
    try {
      setLoading(true)
      const data = await clienteService.listar()
      setClientes(data)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al cargar datos')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargarClientes()
  }, [])

  /* ── Stats ── */
  const stats = useMemo(() => {
    const conPendientes = clientes.filter(c => (c.saldo ?? 0) > 0).length
    const totalPorCobrar = clientes
      .filter(c => (c.saldo ?? 0) > 0)
      .reduce((s, c) => s + (c.saldo ?? 0), 0)
    return { total: clientes.length, conPendientes, totalPorCobrar }
  }, [clientes])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return clientes.filter(c =>
      !q || c.nombre.toLowerCase().includes(q) || (c.telefono?.includes(q) ?? false)
    )
  }, [clientes, search])

  /* ── Handlers ── */
  const openCreate = () => { setShowModal(true) }

  const handleSave = async (data: Omit<Cliente, 'id'> & { id?: string }) => {
    try {
      if (data.id) {
        await clienteService.actualizar(data.id, data)
      } else {
        await clienteService.crear(data)
      }
      await cargarClientes()
    } catch (e) {
      console.error(e)
    }
  }

  if (loading) return (
    <div className="p-4 md:p-8 flex items-center justify-center h-64">
      <div className="text-sm text-[#705D56]">Cargando...</div>
    </div>
  )
  if (error) return (
    <div className="p-4 md:p-8 flex items-center justify-center h-64">
      <div className="text-sm text-red-600">{error}</div>
    </div>
  )

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Users className="w-5 h-5" /> Clientes
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Directorio y cuentas corrientes</p>
        </div>
        <Button
          onClick={openCreate}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo cliente
        </Button>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard icon={Users}     label="Total clientes"        value={String(stats.total)} />
        <StatCard icon={UserCheck} label="Con saldo pendiente"   value={String(stats.conPendientes)} />
        <StatCard icon={Clock}     label="Total por cobrar"      value={formatCurrency(stats.totalPorCobrar)} accent="bg-[#022F40]" />
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
        <div className="bg-white border border-[#E5E7EB] rounded p-12 text-center shadow-sm flex flex-col items-center justify-center gap-3">
          <Users className="w-10 h-10 mx-auto text-[#80727B]" />
          <p className="text-sm text-[#705D56]">{clientes.length === 0 ? 'Todavía no hay clientes.' : 'No se encontraron clientes.'}</p>
          <Button onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nuevo cliente
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(cliente => {
            const inicial = cliente.nombre.charAt(0).toUpperCase()
            const saldo = cliente.saldo ?? 0

            return (
              <button
                key={cliente.id}
                onClick={() => navigate(`/clientes/${cliente.id}`)}
                className="bg-white border border-[#E5E7EB] rounded p-4 text-left hover:border-[#022F40]/40 hover:shadow-sm transition-all group flex flex-col gap-3"
              >
                {/* Avatar + nombre */}
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0">
                    <span className="text-[#91E5F6] font-bold text-sm">{inicial}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-[#022F40] truncate group-hover:text-[#022F40]">
                      {cliente.nombre}
                    </p>
                    {cliente.telefono
                      ? <p className="text-sm text-[#705D56]">{cliente.telefono}</p>
                      : <p className="text-sm text-[#80727B]">Sin teléfono</p>
                    }
                    {cliente.detalles && (
                      <p className="text-sm text-[#80727B] truncate mt-0.5">{cliente.detalles}</p>
                    )}
                  </div>
                </div>

                {/* Divider */}
                <div className="h-px bg-[#E5E7EB]" />

                {/* Saldo row */}
                <div className="flex items-center justify-between">
                  <span className="text-sm text-[#705D56]">Estado:</span>
                  <span
                    className={`text-sm font-bold ${
                      saldo > 0
                        ? 'text-[#022F40]'
                        : saldo < 0
                          ? 'text-[#558564]'
                          : 'text-[#558564]'
                    }`}
                  >
                    {textoSaldo(saldo)}
                  </span>
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
        onSave={handleSave}
      />
    </div>
  )
}
