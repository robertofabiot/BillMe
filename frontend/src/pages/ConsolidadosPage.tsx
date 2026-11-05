import { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Consolidado } from '@/types'
import { consolidadoService } from '@/services/consolidadoService'
import { formatCurrency, formatDate } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Layers, Plus, Search, Building2, Calendar, Trash2 } from 'lucide-react'

export function ConsolidadosPage() {
  const navigate = useNavigate()
  const [consolidados, setConsolidados] = useState<Consolidado[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    try {
      setLoading(true)
      const data = await consolidadoService.listar()
      setConsolidados(data)
      setError(null)
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : 'Error al cargar consolidados')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim()
    return consolidados.filter(c =>
      !q ||
      c.nombre.toLowerCase().includes(q) ||
      (c.folio?.toLowerCase().includes(q) ?? false) ||
      c.clienteNombre.toLowerCase().includes(q) ||
      (c.proyectoNombre?.toLowerCase().includes(q) ?? false)
    )
  }, [consolidados, search])

  const handleDelete = async (e: React.MouseEvent, c: Consolidado) => {
    e.stopPropagation()
    if (!c.id) return
    const folioTexto = c.folio ?? 'este consolidado'
    if (!window.confirm(`¿Eliminar el consolidado ${folioTexto}?`)) return
    try {
      await consolidadoService.eliminar(c.id)
      await fetchData()
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : 'Error al eliminar consolidado')
    }
  }

  if (loading) {
    return <div className="p-4 md:p-8 text-center text-[#705D56]">Cargando consolidados...</div>
  }
  if (error) {
    return <div className="p-4 md:p-8 text-center text-red-500">{error}</div>
  }

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Layers className="w-5 h-5" /> Consolidados
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">
            Estados de cuenta agrupados por fecha de entrega y cuadrados con facturas.
          </p>
        </div>
        <Button
          onClick={() => navigate('/consolidados/nuevo')}
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
          onChange={e => setSearch(e.target.value)}
          placeholder="Buscar por folio, nombre o cliente…"
          className="pl-8"
        />
      </div>

      {/* ── Grid ── */}
      {filtered.length === 0 ? (
        <div className="bg-white border border-[#E5E7EB] rounded p-12 text-center shadow-sm flex flex-col items-center">
          <Layers className="w-10 h-10 text-[#80727B] mb-3" />
          <p className="text-sm font-semibold text-[#022F40]">Sin Consolidados</p>
          <p className="text-sm text-[#705D56] mt-1 max-w-sm">
            No hay documentos consolidados. Crea uno nuevo seleccionando un rango de fechas de un cliente.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(c => {
            const clienteProyecto = c.clienteNombre + (c.proyectoNombre ? ` / ${c.proyectoNombre}` : '')
            const rango = `${formatDate(c.desde)} – ${formatDate(c.hasta)}`

            return (
              <div
                key={c.id}
                onClick={() => navigate(`/consolidados/${c.id}`)}
                className="bg-white border border-[#E5E7EB] rounded p-5 flex flex-col shadow-sm hover:border-[#022F40]/40 transition-all cursor-pointer group"
              >
                <div className="flex justify-between items-start gap-2 mb-2">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-mono font-bold text-[#80727B]">{c.folio}</p>
                    <h3 className="text-sm font-semibold text-[#022F40] leading-tight truncate mt-0.5">
                      {c.nombre}
                    </h3>
                  </div>
                  <button
                    onClick={e => handleDelete(e, c)}
                    className="p-1 rounded text-[#80727B] hover:text-red-600 hover:bg-red-50 transition-colors"
                    title="Eliminar consolidado"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                <p className="text-sm text-[#705D56] flex items-center gap-1.5 mb-1 truncate">
                  <Building2 className="w-3.5 h-3.5 text-[#80727B] shrink-0" /> {clienteProyecto}
                </p>

                <p className="text-sm text-[#80727B] flex items-center gap-1.5 mb-4">
                  <Calendar className="w-3.5 h-3.5 shrink-0" /> {rango}
                </p>

                <div className="mt-auto pt-3 border-t border-[#E5E7EB] flex items-center justify-between">
                  <div>
                    <p className="text-sm text-[#705D56] uppercase tracking-wider">Total Actualizado</p>
                    <p className="text-base font-bold text-[#022F40] leading-none mt-0.5">
                      {formatCurrency(c.totalActualizado)}
                    </p>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
