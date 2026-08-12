import { useState, useMemo, useEffect } from 'react'
import type { Consolidado, Cliente } from '@/types'
import { consolidadoService, clienteService } from '@/services'
import { formatCurrency, formatDate } from '@/lib/format'
import { NuevoConsolidadoSheet } from '@/components/consolidados/NuevoConsolidadoSheet'
import { ConsolidadoDetailSheet } from '@/components/consolidados/ConsolidadoDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Layers, Plus, Search, Building2, Eye, Trash2 } from 'lucide-react'

export function ConsolidadosPage() {
  const [consolidados, setConsolidados] = useState<Consolidado[]>([])
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [showSheet, setShowSheet] = useState(false)
  const [selectedConsolidado, setSelectedConsolidado] = useState<Consolidado | null>(null)
  const [search, setSearch] = useState('')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    try {
      setLoading(true)
      const [resConsolidados, resClientes] = await Promise.all([
        consolidadoService.listar(),
        clienteService.listar()
      ])
      setConsolidados(resConsolidados)
      setClientes(resClientes)
      setError(null)
    } catch (err) {
      console.error(err)
      setError('Error al cargar datos')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return consolidados.filter(c => {
      return !q ||
        c.nombre.toLowerCase().includes(q) ||
        c.folioInterno.toLowerCase().includes(q) ||
        (c.clienteNombre?.toLowerCase().includes(q) ?? false)
    }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [consolidados, search])

  const handleSave = async (data: any) => {
    try {
      await consolidadoService.crear(data)
      await fetchData()
      setShowSheet(false)
    } catch (err) {
      console.error(err)
      alert('Error al crear consolidado')
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('¿Seguro que deseas eliminar este consolidado?')) return
    try {
      await consolidadoService.eliminar(id)
      await fetchData()
    } catch (err) {
      console.error(err)
      alert('Error al eliminar consolidado')
    }
  }

  const getTotal = (c: Consolidado) => {
    return c.totalGeneral ?? c.grupos.reduce((accG, g) => 
      accG + g.items.reduce((accI, i) => accI + (i.cantidad * i.precioUnitario), 0)
    , 0)
  }

  if (loading) {
    return <div className="p-8 text-center text-[#705D56]">Cargando consolidados...</div>
  }
  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>
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
                  <Building2 className="w-3.5 h-3.5 text-[#80727B]" /> {c.clienteNombre ?? 'Desconocido'}
                </p>
                <p className="text-[11px] text-[#80727B] mb-4">Creado el {formatDate(c.createdAt)}</p>
                
                <div className="mt-auto pt-3 border-t border-[#E5E7EB] flex items-center justify-between">
                  <div>
                    <p className="text-[10px] text-[#705D56] uppercase tracking-wider">Total Acumulado</p>
                    <p className="text-base font-bold text-[#022F40] leading-none mt-0.5">{formatCurrency(total)}</p>
                  </div>
                  <div className="flex gap-1">
                    <Button variant="ghost" size="sm" onClick={() => setSelectedConsolidado(c)} className="h-8 w-8 p-0 rounded text-[#022F40] bg-[#F9FAFB] hover:bg-[#E5E7EB]">
                      <Eye className="w-4 h-4" />
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => handleDelete(c.id)} className="h-8 w-8 p-0 rounded text-red-500 hover:bg-red-50">
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* ── Sheets ── */}
      <NuevoConsolidadoSheet
        open={showSheet}
        onOpenChange={setShowSheet}
        onSave={handleSave}
        clientes={clientes}
      />
      <ConsolidadoDetailSheet
        consolidado={selectedConsolidado}
        open={selectedConsolidado !== null}
        onOpenChange={(v) => { if (!v) setSelectedConsolidado(null) }}
      />
    </div>
  )
}
