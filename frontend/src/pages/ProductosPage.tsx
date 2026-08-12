import { useState, useMemo, useEffect, type ChangeEvent } from 'react'
import type { Producto, Factura } from '@/types'
import { productoService } from '@/services/productoService'
import { facturaService } from '@/services/facturaService'
import { formatCurrency } from '@/lib/format'
import { ProductoModal } from '@/components/productos/ProductoModal'
import { ProductoDetailSheet } from '@/components/productos/ProductoDetailSheet'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Package, Plus, Search, Tag, Hash, BarChart3 } from 'lucide-react'

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

export function ProductosPage() {
  const [productos, setProductos]   = useState<Producto[]>([])
  const [facturas, setFacturas]     = useState<Factura[]>([])
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
        const [productosData, facturasData] = await Promise.all([
          productoService.listar(),
          facturaService.listar()
        ])
        setProductos(productosData)
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
    const totalAliases  = productos.reduce((s, p) => s + p.aliases.length, 0)
    const vendidos      = new Set(
      facturas.flatMap(f => f.detalles.map(d => d.productoId))
    ).size
    const sinAlias = productos.filter(p => p.aliases.length === 0).length
    return { total: productos.length, totalAliases, vendidos, sinAlias }
  }, [productos, facturas])

  /* ── Filter ── */
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return productos.filter(p =>
      !q ||
      p.codigoInterno.toLowerCase().includes(q)  ||
      p.nombrePrincipal.toLowerCase().includes(q) ||
      p.aliases.some(a => a.toLowerCase().includes(q))
    )
  }, [productos, search])

  /* ── Handlers ── */
  const openCreate = () => { setEditTarget(null); setShowModal(true) }
  const openEdit   = (p: Producto) => { setEditTarget(p); setShowModal(true); setSelected(null) }

  const handleSave = async (data: Omit<Producto, 'id'> & { id?: string }) => {
    try {
      if (data.id) {
        await productoService.actualizar(data.id, data)
      } else {
        await productoService.crear(data)
      }
      const newData = await productoService.listar()
      setProductos(newData)
    } catch (e) {
      console.error(e)
    }
  }

  /* ── Veces vendido por producto ── */
  const vecesVendido = (productoId: string) =>
    facturas.filter(f =>
      f.estado !== 'COTIZACION' && f.detalles.some(d => d.productoId === productoId)
    ).length

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
            <Package className="w-5 h-5" /> Productos
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">Catálogo de materiales y precios</p>
        </div>
        <Button
          onClick={openCreate}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-2"
        >
          <Plus className="w-4 h-4" /> Nuevo Producto
        </Button>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard icon={Package}   label="Total Productos"  value={String(stats.total)} />
        <StatCard icon={Tag}       label="Total Alias"      value={String(stats.totalAliases)} />
        <StatCard icon={BarChart3} label="Productos Vendidos" value={String(stats.vendidos)} accent="bg-[#022F40]" />
        <StatCard icon={Hash}      label="Sin Alias"        value={String(stats.sinAlias)}
          accent={stats.sinAlias > 0 ? 'bg-amber-500' : undefined}
        />
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

      {/* ── Table ── */}
      <div className="bg-white border border-[#E5E7EB] rounded overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#E5E7EB] bg-[#F9FAFB]">
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Código</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Nombre Principal</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Alias</th>
              <th className="text-center px-4 py-3 font-medium text-[#705D56]">Peso</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Precio Lista</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Costo Neto</th>
              <th className="text-center px-4 py-3 font-medium text-[#705D56]">Ventas</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-[#80727B] text-sm">
                  No se encontraron productos.
                </td>
              </tr>
            )}
            {filtered.map((p, i) => {
              const ventas = vecesVendido(p.id)
              const isLast = i === filtered.length - 1
              return (
                <tr
                  key={p.id}
                  onClick={() => setSelected(p)}
                  className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] cursor-pointer transition-colors ${isLast ? 'border-b-0' : ''}`}
                >
                  <td className="px-4 py-3">
                    <span className="font-mono text-xs font-semibold text-[#022F40] bg-[#F9FAFB] border border-[#E5E7EB] rounded px-2 py-0.5">
                      {p.codigoInterno}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-[#022F40]">{p.nombrePrincipal}</td>
                  <td className="px-4 py-3">
                    {p.aliases.length === 0 ? (
                      <span className="text-xs text-[#80727B] italic">Sin alias</span>
                    ) : (
                      <div className="flex flex-wrap gap-1">
                        {p.aliases.slice(0, 3).map(a => (
                          <span
                            key={a}
                            className="text-[11px] bg-[#022F40]/8 text-[#022F40] px-1.5 py-0.5 rounded border border-[#022F40]/15"
                          >
                            {a}
                          </span>
                        ))}
                        {p.aliases.length > 3 && (
                          <span className="text-[11px] text-[#80727B]">+{p.aliases.length - 3}</span>
                        )}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center text-[#705D56] text-xs">
                    {p.peso > 0 ? `${p.peso} kg` : '—'}
                  </td>
                  <td className="px-4 py-3 text-right text-xs font-medium text-[#80727B]">
                    {p.precioListaProveedor ? formatCurrency(p.precioListaProveedor) : '—'}
                  </td>
                  <td className="px-4 py-3 text-right text-xs font-semibold text-[#558564]">
                    {p.costoNeto ? formatCurrency(p.costoNeto) : '—'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    {ventas > 0
                      ? <span className="text-xs font-semibold text-[#558564] bg-[#558564]/10 border border-[#558564]/20 px-2 py-0.5 rounded">{ventas} {ventas === 1 ? 'venta' : 'ventas'}</span>
                      : <span className="text-xs text-[#80727B]">—</span>
                    }
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
        onOpenChange={v => { if (!v) setSelected(null) }}
        onEdit={openEdit}
      />
    </div>
  )
}
