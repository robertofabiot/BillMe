import { useState, useMemo, useEffect, type ChangeEvent } from 'react'
import type { Proyecto, Cliente, Factura } from '@/types'
import { proyectoService } from '@/services/proyectoService'
import { clienteService } from '@/services/clienteService'
import { facturaService } from '@/services/facturaService'
import { formatCurrency } from '@/lib/format'
import { ProyectoModal } from '@/components/proyectos/ProyectoModal'
import { ProyectoDetailSheet } from '@/components/proyectos/ProyectoDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { FolderGit2, Plus, Search, Building2, CheckCircle2, TrendingUp } from 'lucide-react'

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

export function ProyectosPage() {
  const [proyectos, setProyectos]   = useState<Proyecto[]>([])
  const [clientes, setClientes]     = useState<Cliente[]>([])
  const [facturas, setFacturas]     = useState<Factura[]>([])
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState<string | null>(null)
  const [selected,  setSelected]    = useState<Proyecto | null>(null)
  const [editTarget, setEditTarget] = useState<Proyecto | null>(null)
  const [showModal, setShowModal]   = useState(false)
  const [search,    setSearch]      = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const [proyectosData, clientesData, facturasData] = await Promise.all([
          proyectoService.listar(),
          clienteService.listar(),
          facturaService.listar()
        ])
        setProyectos(proyectosData)
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
    const activos = proyectos.filter(p => p.estado === 'ACTIVO').length
    const facturado = facturas
      .filter(f => f.estado !== 'COTIZACION' && f.proyectoId)
      .reduce((s, f) => s + f.totalVenta, 0)
    
    return { total: proyectos.length, activos, facturado }
  }, [proyectos, facturas])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return proyectos.filter(p => {
      const cNombre = p.clienteNombre || clientes.find(c => c.id === p.clienteId)?.nombre
      return !q ||
        p.nombre.toLowerCase().includes(q) ||
        (cNombre?.toLowerCase().includes(q) ?? false)
    }).sort((a, b) => {
      // Ordenar por estado (Activos primero)
      if (a.estado === 'ACTIVO' && b.estado !== 'ACTIVO') return -1
      if (a.estado !== 'ACTIVO' && b.estado === 'ACTIVO') return 1
      return 0
    })
  }, [proyectos, clientes, search])

  /* ── Handlers ── */
  const openCreate = () => { setEditTarget(null); setShowModal(true) }
  const openEdit   = (p: Proyecto) => { setEditTarget(p); setShowModal(true); setSelected(null) }

  const handleSave = async (data: Omit<Proyecto, 'id'> & { id?: string }) => {
    try {
      if (data.id) {
        await proyectoService.actualizar(data.id, data)
      } else {
        await proyectoService.crear(data)
      }
      const newData = await proyectoService.listar()
      setProyectos(newData)
    } catch (e) {
      console.error(e)
    }
  }

  /* ── Helpers ── */
  const getFacturado = (proyectoId: string) => 
    facturas
      .filter(f => f.proyectoId === proyectoId && f.estado !== 'COTIZACION')
      .reduce((s, f) => s + f.totalVenta, 0)

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
            <FolderGit2 className="w-5 h-5" /> Proyectos / Obras
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Control de facturación por obra</p>
        </div>
        <Button
          onClick={openCreate}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo Proyecto
        </Button>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-3 gap-4 max-w-4xl">
        <StatCard icon={FolderGit2} label="Total Proyectos" value={String(stats.total)} />
        <StatCard icon={CheckCircle2} label="Proyectos Activos" value={String(stats.activos)} accent="bg-[#558564]" />
        <StatCard icon={TrendingUp} label="Facturado en Obras" value={formatCurrency(stats.facturado)} accent="bg-[#022F40]" />
      </div>

      {/* ── Search ── */}
      <div className="relative max-w-xs">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
        <Input
          value={search}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
          placeholder="Buscar por nombre o cliente…"
          className="pl-8"
        />
      </div>

      {/* ── Grid de Proyectos ── */}
      {filtered.length === 0 ? (
        <div className="bg-white border border-[#E5E7EB] rounded p-12 text-center shadow-sm">
          <FolderGit2 className="w-10 h-10 mx-auto text-[#80727B] mb-3" />
          <p className="text-sm text-[#705D56]">No se encontraron proyectos.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(p => {
            const clienteNombre = p.clienteNombre || clientes.find(c => c.id === p.clienteId)?.nombre
            const facturado = getFacturado(p.id)
            
            const estadoColors = {
              ACTIVO: 'bg-emerald-100 text-emerald-800 border-emerald-200',
              FINALIZADO: 'bg-[#F9FAFB] text-[#80727B] border-[#E5E7EB]',
            }

            return (
              <button
                key={p.id}
                onClick={() => setSelected(p)}
                className="bg-white border border-[#E5E7EB] rounded p-5 text-left hover:border-[#022F40]/20 hover:shadow-sm transition-all group flex flex-col gap-4 relative overflow-hidden"
              >
                {/* Header */}
                <div className="flex justify-between items-start gap-2">
                  <div className="min-w-0">
                    <h3 className="text-sm font-semibold text-[#022F40] truncate group-hover:text-[#022F40]">{p.nombre}</h3>
                    <p className="text-xs text-[#705D56] truncate mt-0.5 flex items-center gap-1">
                      <Building2 className="w-3 h-3" /> {clienteNombre ?? 'Desconocido'}
                    </p>
                  </div>
                  <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded border ${estadoColors[p.estado]}`}>
                    {p.estado}
                  </span>
                </div>

                {/* Footer (Facturado) */}
                <div className="mt-auto pt-3 border-t border-[#E5E7EB]">
                  <p className="text-[10px] text-[#705D56]">Total Facturado</p>
                  <p className="text-lg font-bold text-[#022F40] leading-tight mt-0.5">{formatCurrency(facturado)}</p>
                </div>
              </button>
            )
          })}
        </div>
      )}

      {/* ── Modales ── */}
      <ProyectoModal
        open={showModal}
        onOpenChange={setShowModal}
        initial={editTarget}
        onSave={handleSave}
        clientes={clientes}
      />
      <ProyectoDetailSheet
        proyecto={selected}
        open={selected !== null}
        onOpenChange={v => { if (!v) setSelected(null) }}
        onEdit={openEdit}
      />
    </div>
  )
}
