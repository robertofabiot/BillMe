import { useState, useMemo, type ChangeEvent } from 'react'
import type { Proyecto } from '@/types'
import { PROYECTOS, CLIENTES, FACTURAS } from '@/data/mock'
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
  const [proyectos, setProyectos]   = useState<Proyecto[]>(PROYECTOS)
  const [selected,  setSelected]    = useState<Proyecto | null>(null)
  const [editTarget, setEditTarget] = useState<Proyecto | null>(null)
  const [showModal, setShowModal]   = useState(false)
  const [search,    setSearch]      = useState('')

  /* ── Stats ── */
  const stats = useMemo(() => {
    const activos = proyectos.filter(p => p.estado === 'ACTIVO').length
    const facturado = FACTURAS
      .filter(f => f.estado !== 'COTIZACION' && f.proyectoId)
      .reduce((s, f) => s + f.totalVenta, 0)
    
    return { total: proyectos.length, activos, facturado }
  }, [proyectos])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return proyectos.filter(p => {
      const c = CLIENTES.find(c => c.id === p.clienteId)
      return !q ||
        p.nombre.toLowerCase().includes(q) ||
        (c?.nombre.toLowerCase().includes(q) ?? false)
    }).sort((a, b) => {
      // Ordenar por estado (Activos primero)
      if (a.estado === 'ACTIVO' && b.estado !== 'ACTIVO') return -1
      if (a.estado !== 'ACTIVO' && b.estado === 'ACTIVO') return 1
      return 0
    })
  }, [proyectos, search])

  /* ── Handlers ── */
  const openCreate = () => { setEditTarget(null); setShowModal(true) }
  const openEdit   = (p: Proyecto) => { setEditTarget(p); setShowModal(true); setSelected(null) }

  const handleSave = (data: Omit<Proyecto, 'id'> & { id?: string }) => {
    if (data.id) {
      setProyectos(prev => prev.map(p => p.id === data.id ? { ...p, ...data } as Proyecto : p))
    } else {
      setProyectos(prev => [{ ...data, id: `p-${Date.now()}` } as Proyecto, ...prev])
    }
  }

  /* ── Helpers ── */
  const getFacturado = (proyectoId: string) => 
    FACTURAS
      .filter(f => f.proyectoId === proyectoId && f.estado !== 'COTIZACION')
      .reduce((s, f) => s + f.totalVenta, 0)

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
            const cliente = CLIENTES.find(c => c.id === p.clienteId)
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
                      <Building2 className="w-3 h-3" /> {cliente?.nombre ?? 'Desconocido'}
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
