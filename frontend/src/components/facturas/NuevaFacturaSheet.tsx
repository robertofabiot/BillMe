import { useState, useMemo, useRef, useEffect, type ChangeEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Factura, Producto, EstadoFactura, Cliente, Proyecto } from '@/types'
import { productoService } from '@/services/productoService'
import { formatCurrency } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { Search, X, Plus, History, ReceiptText } from 'lucide-react'

interface LineItem {
  key: string
  productoId: string
  nombre: string
  codigoInterno: string
  cantidad: number
  precioUnitario: number
  precioSugerido: number
  precioListaProveedor?: number
  costoUnitario?: number
}

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  onSave: (payload: any) => void
  clientes: Cliente[]
  proyectos: Proyecto[]
}

export function NuevaFacturaSheet({ open, onOpenChange, onSave, clientes, proyectos }: Props) {
  const [clienteId, setClienteId]     = useState('')
  const [proyectoId, setProyectoId]   = useState('')
  const [items, setItems]             = useState<LineItem[]>([])
  const [productSearch, setSearch]    = useState('')
  const [showDropdown, setDropdown]   = useState(false)
  const searchRef                     = useRef<HTMLDivElement>(null)

  // Close on Escape
  useEffect(() => {
    if (!open) return
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onOpenChange(false) }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onOpenChange])

  const clienteProyectos = useMemo(
    () => proyectos.filter(p => p.clienteId === clienteId && p.estado === 'ACTIVO'),
    [clienteId, proyectos],
  )

  const [filteredProducts, setFilteredProducts] = useState<Producto[]>([])

  useEffect(() => {
    if (!productSearch.trim()) {
      setFilteredProducts([])
      return
    }
    const fetchProducts = async () => {
      try {
        const results = await productoService.listar(productSearch)
        setFilteredProducts(results.slice(0, 8))
      } catch (err) {
        console.error(err)
      }
    }
    const timer = setTimeout(fetchProducts, 300)
    return () => clearTimeout(timer)
  }, [productSearch])

  const total = items.reduce((sum, i) => sum + i.cantidad * i.precioUnitario, 0)

  const addProduct = (producto: Producto) => {
    const precioSugerido = 0
    setItems(prev => [...prev, {
      key: `${producto.id}-${Date.now()}`,
      productoId: producto.id,
      nombre: producto.nombrePrincipal,
      codigoInterno: producto.codigoInterno,
      cantidad: 1,
      precioUnitario: precioSugerido || producto.precioListaProveedor || 0,
      precioSugerido,
      precioListaProveedor: producto.precioListaProveedor,
      costoUnitario: producto.costoNeto,
    }])
    setSearch('')
    setDropdown(false)
  }

  const updateItem = (key: string, field: 'cantidad' | 'precioUnitario', value: number) =>
    setItems(prev => prev.map(i => i.key === key ? { ...i, [field]: value } : i))

  const removeItem = (key: string) =>
    setItems(prev => prev.filter(i => i.key !== key))

  const handleClienteChange = (id: string) => {
    setClienteId(id)
    setProyectoId('')
  }

  const handleSave = (estado: EstadoFactura) => {
    if (!clienteId || items.length === 0) return
    onSave({
      clienteId,
      proyectoId: proyectoId || undefined,
      estado,
      detalles: items.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad,
        precioUnitarioVenta: item.precioUnitario,
      })),
    })
    setClienteId(''); setProyectoId(''); setItems([]); setSearch('')
    onOpenChange(false)
  }

  const canSave = clienteId && items.length > 0 &&
    items.every(i => i.cantidad > 0 && i.precioUnitario > 0)

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-[2px]"
        onClick={() => onOpenChange(false)}
      />

      {/* Modal */}
      <div className="relative z-10 w-[900px] max-w-[calc(100vw-2rem)] max-h-[calc(100vh-4rem)] bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden">

        {/* ── Header ── */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <ReceiptText className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-[#022F40]">Nueva Factura</h2>
              <p className="text-xs text-[#80727B]">Folio: <span className="font-mono font-medium">Automático</span></p>
            </div>
          </div>
          <button
            onClick={() => onOpenChange(false)}
            className="w-7 h-7 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] hover:text-[#022F40] transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* ── Body ── */}
        <div className="flex flex-1 overflow-hidden">

          {/* Left column — Cliente + Productos */}
          <div className="flex-1 flex flex-col overflow-y-auto border-r border-[#E5E7EB] px-6 py-5 gap-6">

            {/* Cliente + Proyecto */}
            <section className="flex flex-col gap-3">
              <p className="text-xs font-semibold uppercase tracking-wider text-[#80727B]">Cliente</p>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs text-[#705D56]">Cliente *</label>
                  <Select value={clienteId} onValueChange={handleClienteChange}>
                    <SelectTrigger className="rounded border-[#E5E7EB] text-sm h-9">
                      <SelectValue placeholder="Seleccionar cliente…" />
                    </SelectTrigger>
                    <SelectContent>
                      {clientes.map(c => (
                        <SelectItem key={c.id} value={c.id}>{c.nombre}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs text-[#705D56]">Proyecto (opcional)</label>
                  <Select
                    value={proyectoId}
                    onValueChange={setProyectoId}
                    disabled={clienteProyectos.length === 0}
                  >
                    <SelectTrigger className="rounded border-[#E5E7EB] text-sm h-9">
                      <SelectValue placeholder={
                        !clienteId ? 'Elige un cliente primero' :
                        clienteProyectos.length ? 'Asignar proyecto…' : 'Sin proyectos activos'
                      } />
                    </SelectTrigger>
                    <SelectContent>
                      {clienteProyectos.map(p => (
                        <SelectItem key={p.id} value={p.id}>{p.nombre}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </section>

            <div className="h-px bg-[#E5E7EB]" />

            {/* Búsqueda de productos */}
            <section className="flex flex-col gap-3 flex-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-[#80727B]">Agregar Productos</p>
              <div className="relative" ref={searchRef}>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
                  <Input
                    value={productSearch}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => {
                      setSearch(e.target.value); setDropdown(true)
                    }}
                    onFocus={() => setDropdown(true)}
                    onBlur={() => setTimeout(() => setDropdown(false), 150)}
                    placeholder="Buscar por nombre, código o alias…"
                    className="pl-8"
                  />
                </div>

                {showDropdown && filteredProducts.length > 0 && (
                  <div className="absolute z-50 top-full mt-1 w-full bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden">
                    {filteredProducts.map(p => (
                      <button
                        key={p.id}
                        onMouseDown={() => addProduct(p)}
                        className="w-full flex items-center justify-between px-3 py-2.5 hover:bg-[#F9FAFB] transition-colors text-left group"
                      >
                        <div>
                          <p className="text-sm text-[#022F40] font-medium">{p.nombrePrincipal}</p>
                          <p className="text-xs text-[#80727B]">
                            {p.codigoInterno}
                            {p.aliases.length > 0 && <span className="ml-1">· {p.aliases.slice(0, 2).join(', ')}</span>}
                          </p>
                        </div>
                        <Plus className="w-4 h-4 text-[#80727B] group-hover:text-[#022F40] shrink-0" />
                      </button>
                    ))}
                  </div>
                )}

                {showDropdown && productSearch.trim() && filteredProducts.length === 0 && (
                  <div className="absolute z-50 top-full mt-1 w-full bg-white border border-[#E5E7EB] rounded shadow-sm px-3 py-3 text-xs text-[#705D56]">
                    No se encontraron productos para <strong>"{productSearch}"</strong>
                  </div>
                )}
              </div>
            </section>
          </div>

          {/* Right column — Items table */}
          <div className="w-[420px] shrink-0 flex flex-col overflow-hidden">
            <div className="px-5 py-3 border-b border-[#E5E7EB] bg-[#F9FAFB]">
              <p className="text-xs font-semibold uppercase tracking-wider text-[#80727B]">
                Partidas <span className="text-[#022F40]">({items.length})</span>
              </p>
            </div>

            <div className="flex-1 overflow-y-auto">
              {items.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full gap-2 text-center px-8 py-12">
                  <Plus className="w-8 h-8 text-[#E5E7EB]" />
                  <p className="text-sm text-[#80727B]">Agrega productos desde el buscador</p>
                </div>
              ) : (
                <table className="w-full text-xs">
                  <thead className="sticky top-0 bg-white border-b border-[#E5E7EB]">
                    <tr>
                      <th className="text-left px-4 py-2 font-medium text-[#705D56]">Producto</th>
                      <th className="text-center px-2 py-2 font-medium text-[#705D56] w-16">Cant.</th>
                      <th className="text-center px-2 py-2 font-medium text-[#705D56] w-24">Precio</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56] w-24">Subtotal</th>
                      <th className="w-7" />
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item, idx) => (
                      <tr
                        key={item.key}
                        className={`border-b border-[#E5E7EB] ${idx === items.length - 1 ? 'border-b-0' : ''}`}
                      >
                        <td className="px-4 py-2.5">
                          <p className="text-[#022F40] font-medium leading-tight">{item.nombre}</p>
                          <p className="text-[#80727B] font-mono">{item.codigoInterno}</p>
                          {item.precioSugerido > 0 && item.precioUnitario === item.precioSugerido ? (
                            <span className="inline-flex items-center gap-0.5 text-[#558564] mt-0.5">
                              <History className="w-2.5 h-2.5" />
                              <span className="text-[10px]">Precio historial</span>
                            </span>
                          ) : item.precioListaProveedor ? (
                            <span className="inline-flex items-center gap-0.5 text-[#80727B] mt-0.5">
                              <span className="text-[10px]">Lista Prov: {formatCurrency(item.precioListaProveedor)}</span>
                            </span>
                          ) : null}
                        </td>
                        <td className="px-2 py-2.5">
                          <Input
                            type="number"
                            min={0.01}
                            step={0.01}
                            value={item.cantidad}
                            onChange={(e: ChangeEvent<HTMLInputElement>) =>
                              updateItem(item.key, 'cantidad', parseFloat(e.target.value) || 0)
                            }
                            className="h-7 text-center px-1 text-xs"
                          />
                        </td>
                        <td className="px-2 py-2.5">
                          <Input
                            type="number"
                            min={0}
                            step={0.01}
                            value={item.precioUnitario}
                            onChange={(e: ChangeEvent<HTMLInputElement>) =>
                              updateItem(item.key, 'precioUnitario', parseFloat(e.target.value) || 0)
                            }
                            className="h-7 text-center px-1 text-xs"
                          />
                        </td>
                        <td className="px-3 py-2.5 text-right font-semibold text-[#022F40]">
                          {formatCurrency(item.cantidad * item.precioUnitario)}
                        </td>
                        <td className="pr-2 py-2.5">
                          <button
                            onClick={() => removeItem(item.key)}
                            className="w-5 h-5 flex items-center justify-center rounded hover:bg-red-50 text-[#80727B] hover:text-red-500 transition-colors"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>

        {/* ── Footer ── */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] shrink-0">
          <div>
            <p className="text-xs text-[#705D56]">Total estimado</p>
            <p className="text-2xl font-bold text-[#022F40]">{formatCurrency(total)}</p>
            {clienteId && items.some(i => i.precioSugerido > 0 && i.precioUnitario === i.precioSugerido) && (
              <p className="text-xs text-[#558564] flex items-center gap-1 mt-0.5">
                <History className="w-3 h-3" /> Precios del historial del cliente aplicados
              </p>
            )}
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => handleSave('COTIZACION')}
              disabled={!canSave}
              className="rounded border-[#E5E7EB] text-[#022F40] h-9 text-sm"
            >
              Guardar Cotización
            </Button>
            <Button
              onClick={() => handleSave('PENDIENTE')}
              disabled={!canSave}
              className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white h-9 text-sm"
            >
              Confirmar Venta
            </Button>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  )
}
