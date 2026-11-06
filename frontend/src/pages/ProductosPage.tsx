import { useState, useEffect, type ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Producto } from '@/types'
import { productoService, type ProductoPayload } from '@/services/productoService'
import { formatCurrency } from '@/lib/format'
import { ProductoModal } from '@/components/productos/ProductoModal'
import { ProductoDetailSheet } from '@/components/productos/ProductoDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Package, Plus, Search, Upload } from 'lucide-react'

export function ProductosPage() {
  const navigate = useNavigate()
  const [productos, setProductos]   = useState<Producto[]>([])
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState<string | null>(null)
  const [selected,  setSelected]    = useState<Producto | null>(null)
  const [editTarget, setEditTarget] = useState<Producto | null>(null)
  const [showModal, setShowModal]   = useState(false)
  const [search,    setSearch]      = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        setProductos(await productoService.listar())
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Error al cargar datos')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  /* ── Búsqueda en el servidor (tolerante a variantes de escritura) ── */
  const [resultados, setResultados] = useState<Producto[] | null>(null)

  useEffect(() => {
    const q = search.trim()
    const timer = setTimeout(() => {
      if (!q) { setResultados(null); return }
      productoService.listar(q).then(setResultados).catch(() => setResultados([]))
    }, 300)
    return () => clearTimeout(timer)
  }, [search])

  const filtered = resultados ?? productos

  /* ── Handlers ── */
  const openCreate = () => { setEditTarget(null); setShowModal(true) }
  const openEdit   = (p: Producto) => { setEditTarget(p); setShowModal(true); setSelected(null) }

  const handleSave = async (data: ProductoPayload & { id?: string }) => {
    if (data.id) {
      await productoService.actualizar(data.id, data)
    } else {
      await productoService.crear(data)
    }
    const newData = await productoService.listar()
    setProductos(newData)
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
            <Package className="w-5 h-5" /> Productos
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Los materiales que vendes y sus precios de lista</p>
        </div>
        <Button
          onClick={openCreate}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo producto
        </Button>
      </div>

      {/* ── Search ── */}
      <div className="relative max-w-xs">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
        <Input
          value={search}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
          placeholder="Buscar por código, nombre o alias…"
          className="pl-8"
        />
      </div>
      <p className="text-sm text-[#80727B] -mt-3">
        {resultados === null
          ? 'Se muestran los productos usados más recientemente. Busca para encontrar cualquier otro.'
          : `${resultados.length} resultado${resultados.length === 1 ? '' : 's'} (máximo 20, los más parecidos primero).`}
      </p>

      {/* ── Table ── */}
      <div className="bg-white border border-[#E5E7EB] rounded overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#E5E7EB] bg-[#F9FAFB]">
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Código</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Nombre principal</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Alias</th>
              <th className="text-center px-4 py-3 font-medium text-[#705D56]">Peso</th>
              <th className="text-center px-4 py-3 font-medium text-[#705D56]">Unidad</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Lista (sin IVA)</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[#80727B] text-sm">
                  {productos.length === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-3">
                      <p>Todavía no hay productos.</p>
                      <Button onClick={() => navigate('/importar')}>
                        <Upload className="w-4 h-4" /> Importar lista
                      </Button>
                    </div>
                  ) : (
                    'No se encontraron productos.'
                  )}
                </td>
              </tr>
            )}
            {filtered.map((p, i) => {
              const isLast = i === filtered.length - 1
              return (
                <tr
                  key={p.id}
                  onClick={() => setSelected(p)}
                  className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] cursor-pointer transition-colors ${isLast ? 'border-b-0' : ''}`}
                >
                  <td className="px-4 py-3">
                    <span className="font-mono text-sm font-semibold text-[#022F40] bg-[#F9FAFB] border border-[#E5E7EB] rounded px-2 py-0.5">
                      {p.codigoInterno}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-[#022F40]">{p.nombrePrincipal}</td>
                  <td className="px-4 py-3">
                    {p.aliases.length === 0 ? (
                      <span className="text-sm text-[#80727B] italic">Sin alias</span>
                    ) : (
                      <div className="flex flex-wrap gap-1">
                        {p.aliases.slice(0, 3).map(a => (
                          <span
                            key={a}
                            className="text-sm bg-[#022F40]/8 text-[#022F40] px-1.5 py-0.5 rounded border border-[#022F40]/15"
                          >
                            {a}
                          </span>
                        ))}
                        {p.aliases.length > 3 && (
                          <span className="text-sm text-[#80727B]">+{p.aliases.length - 3}</span>
                        )}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center text-[#705D56] text-sm">
                    {p.peso > 0 ? `${p.peso} kg` : '—'}
                  </td>
                  <td className="px-4 py-3 text-center text-[#705D56] text-sm">{p.unidad}</td>
                  <td className="px-4 py-3 text-right text-sm font-medium text-[#022F40]">
                    {p.precioListaVigente != null ? formatCurrency(p.precioListaVigente) : '—'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* ── Modals ── */}
      <ProductoModal
        open={showModal}
        onOpenChange={setShowModal}
        initial={editTarget}
        onSave={handleSave}
      />
      <ProductoDetailSheet
        producto={selected}
        open={selected !== null}
        onOpenChange={(v: boolean) => { if (!v) setSelected(null) }}
        onEdit={openEdit}
        onPrecioActualizado={() => productoService.listar().then(setProductos).catch(console.error)}
      />
    </div>
  )
}
