import { useState, useMemo } from 'react'
import type { Consolidado } from '@/types'
import { CONSOLIDADOS, CLIENTES } from '@/data/mock'
import { formatCurrency, formatDate } from '@/lib/format'
import { NuevoConsolidadoSheet } from '@/components/consolidados/NuevoConsolidadoSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Layers, Plus, Search, Building2, Eye } from 'lucide-react'

export function ConsolidadosPage() {
  const [consolidados, setConsolidados] = useState<Consolidado[]>(CONSOLIDADOS)
  const [showSheet, setShowSheet] = useState(false)
  const [search, setSearch] = useState('')

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return consolidados.filter(c => {
      const cli = CLIENTES.find(x => x.id === c.clienteId)
      return !q ||
        c.nombre.toLowerCase().includes(q) ||
        c.folioInterno.toLowerCase().includes(q) ||
        (cli?.nombre.toLowerCase().includes(q) ?? false)
    }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [consolidados, search])

  const handleSave = (data: Omit<Consolidado, 'id' | 'createdAt' | 'folioInterno'>) => {
    const nuevo: Consolidado = {
      ...data,
      id: `c-${Date.now()}`,
      folioInterno: `CONS-${String(consolidados.length + 1).padStart(4, '0')}`,
      createdAt: new Date().toISOString().split('T')[0]
    }
    setConsolidados(prev => [nuevo, ...prev])
    setShowSheet(false)
  }

  const getTotal = (c: Consolidado) => {
    return c.grupos.reduce((accG, g) => 
      accG + g.items.reduce((accI, i) => accI + (i.cantidad * i.precioUnitario), 0)
    , 0)
  }

  return (
    <div className="p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Layers className="w-5 h-5" /> Estados de Cuenta Consolidados
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Agrupa múltiples facturas en un solo documento editable.</p>
        </div>
        <Button
          onClick={() => setShowSheet(true)}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo Consolidado
        </Button>
      </div>

      {/* ── Search ── */}
      <div className="relative max-w-xs">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nombre, folio o cliente…"
          className="pl-8"
        />
      </div>

      {/* ── Grid ── */}
      {filtered.length === 0 ? (
        <div className="bg-white border border-[#E5E7EB] rounded p-12 text-center shadow-sm flex flex-col items-center">
          <Layers className="w-10 h-10 text-[#80727B] mb-3" />
          <p className="text-sm font-semibold text-[#022F40]">Sin Consolidados</p>
          <p className="text-xs text-[#705D56] mt-1 max-w-sm">No has creado ningún documento consolidado aún. Crea uno para agrupar facturas y enviarlas a tus clientes.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(c => {
            const cliente = CLIENTES.find(x => x.id === c.clienteId)
            const total = getTotal(c)
            
            return (
              <div key={c.id} className="bg-white border border-[#E5E7EB] rounded p-5 flex flex-col shadow-sm hover:border-[#022F40]/20 transition-all relative group">
                <div className="flex justify-between items-start gap-2 mb-3">
                  <div className="min-w-0">
                    <p className="text-[10px] font-bold text-[#80727B] mb-0.5">{c.folioInterno}</p>
                    <h3 className="text-sm font-semibold text-[#022F40] leading-tight truncate">{c.nombre}</h3>
                  </div>
                </div>
                
                <p className="text-xs text-[#705D56] flex items-center gap-1.5 mb-1 truncate">
                  <Building2 className="w-3.5 h-3.5 text-[#80727B]" /> {cliente?.nombre ?? 'Desconocido'}
                </p>
                <p className="text-[11px] text-[#80727B] mb-4">Creado el {formatDate(c.createdAt)}</p>
                
                <div className="mt-auto pt-3 border-t border-[#E5E7EB] flex items-center justify-between">
                  <div>
                    <p className="text-[10px] text-[#705D56] uppercase tracking-wider">Total Acumulado</p>
                    <p className="text-base font-bold text-[#022F40] leading-none mt-0.5">{formatCurrency(total)}</p>
                  </div>
                  <Button variant="ghost" size="sm" className="h-8 w-8 p-0 rounded text-[#022F40] bg-[#F9FAFB] hover:bg-[#E5E7EB]">
                    <Eye className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* ── Sheet ── */}
      <NuevoConsolidadoSheet
        open={showSheet}
        onOpenChange={setShowSheet}
        onSave={handleSave}
      />
    </div>
  )
}
