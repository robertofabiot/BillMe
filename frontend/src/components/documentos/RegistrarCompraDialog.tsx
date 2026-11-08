import { useEffect, useState } from 'react'
import type { LineaDocumento, MetodoPrecio, Proveedor, CompraRequest } from '@/types'
import { proveedorService } from '@/services/proveedorService'
import { compraService } from '@/services/compraService'
import { precioListaService } from '@/services/precioListaService'
import { formatCurrency, hoyIso } from '@/lib/format'
import { recalcularPrecioPorPeso } from './lineaForm'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  documentoId: string
  lineas: LineaDocumento[]
  tipoCambio: number
  ivaPct: number
  onCompraRegistrada: () => void
}

interface LineaCompraItem {
  lineaId: string
  productoId: string
  productoGenerico: boolean
  descripcion: string
  cantidad: number
  compraId?: string
  incluida: boolean
  precioSinIva: string
  descuentoPct: string
  pesoUnitKg: string
  metodoPrecio: MetodoPrecio
  parametros?: string | null
  precioUnit: number
}

const SELECT =
  'h-11 w-full rounded border border-[#E5E7EB] bg-white px-3 text-base text-[#022F40] outline-none focus:border-[#022F40] disabled:opacity-60'

export function RegistrarCompraDialog({
  open,
  onOpenChange,
  documentoId,
  lineas,
  tipoCambio,
  ivaPct,
  onCompraRegistrada,
}: Props) {
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [proveedorId, setProveedorId] = useState('')
  const [fecha, setFecha] = useState(hoyIso())
  const [numeroFactura, setNumeroFactura] = useState('')
  const [nuevoProveedor, setNuevoProveedor] = useState<string | null>(null)
  const [creandoProveedor, setCreandoProveedor] = useState(false)
  const [lineasCompra, setLineasCompra] = useState<LineaCompraItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const hasNonGenericos = lineas.some(l => !l.productoGenerico)

  useEffect(() => {
    if (!open) return
    setError(null)
    setGuardando(false)
    setFecha(hoyIso())
    setNumeroFactura('')
    setNuevoProveedor(null)

    proveedorService.listar().then(setProveedores).catch(console.error)

    const items: LineaCompraItem[] = lineas.map(l => ({
      lineaId: l.id,
      productoId: l.productoId,
      productoGenerico: l.productoGenerico,
      descripcion: l.descripcion,
      cantidad: l.cantidad,
      compraId: l.compraId,
      incluida: !l.compraId,
      precioSinIva: '',
      descuentoPct: '',
      pesoUnitKg: '',
      metodoPrecio: l.metodoPrecio,
      parametros: l.parametros,
      precioUnit: l.precioUnit,
    }))
    setLineasCompra(items)

    const nonGenericos = Array.from(
      new Set(lineas.filter(l => !l.productoGenerico).map(l => l.productoId)),
    )
    if (nonGenericos.length > 0) {
      Promise.all(
        nonGenericos.map(async pid => {
          try {
            const res = await precioListaService.consultar(pid)
            return { pid, descuento: res.descuentoProveedorSugerido }
          } catch {
            return { pid, descuento: null }
          }
        }),
      ).then(descuentos => {
        const map = new Map<string, number>()
        for (const d of descuentos) {
          if (d.descuento != null) map.set(d.pid, d.descuento)
        }
        setLineasCompra(prev =>
          prev.map(item => {
            if (!item.productoGenerico && item.descuentoPct === '' && map.has(item.productoId)) {
              const sugerido = map.get(item.productoId)
              return { ...item, descuentoPct: sugerido != null ? String(sugerido) : '' }
            }
            return item
          }),
        )
      })
    }
  }, [open, lineas])

  const actualizarLinea = (lineaId: string, cambios: Partial<LineaCompraItem>) => {
    setLineasCompra(prev =>
      prev.map(l => (l.lineaId === lineaId ? { ...l, ...cambios } : l)),
    )
  }

  const crearProveedor = async () => {
    const nombre = nuevoProveedor?.trim()
    if (!nombre) return
    setCreandoProveedor(true)
    setError(null)
    try {
      const prov = await proveedorService.crear({ nombre })
      setProveedores(prev =>
        [...prev, prov].sort((a, b) => a.nombre.localeCompare(b.nombre)),
      )
      setProveedorId(prov.id)
      setNuevoProveedor(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo crear el proveedor')
    } finally {
      setCreandoProveedor(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!proveedorId) {
      setError('Elige un proveedor.')
      return
    }
    if (!fecha) {
      setError('Indica la fecha.')
      return
    }

    const seleccionadas = lineasCompra.filter(l => l.incluida)
    if (seleccionadas.length === 0) {
      setError('Selecciona al menos una línea para registrar la compra.')
      return
    }

    for (const l of seleccionadas) {
      const p = Number(l.precioSinIva)
      if (!l.precioSinIva.trim() || isNaN(p) || p <= 0) {
        setError(`Ingresa el precio sin IVA válido para "${l.descripcion}".`)
        return
      }
      if (!l.productoGenerico && l.descuentoPct.trim() !== '') {
        const d = Number(l.descuentoPct)
        if (isNaN(d) || d < 0 || d >= 100) {
          setError(`El descuento de "${l.descripcion}" debe estar entre 0 y 99.99%.`)
          return
        }
      }
      if (l.pesoUnitKg.trim() !== '') {
        const w = Number(l.pesoUnitKg)
        if (isNaN(w) || w <= 0) {
          setError(`El peso de "${l.descripcion}" debe ser mayor a 0.`)
          return
        }
      }
    }

    setGuardando(true)
    try {
      const payload: CompraRequest = {
        proveedorId,
        fecha,
        numeroFactura: numeroFactura.trim() || undefined,
        lineas: seleccionadas.map(l => ({
          lineaId: l.lineaId,
          precioSinIva: Number(l.precioSinIva),
          descuentoPct:
            !l.productoGenerico && l.descuentoPct.trim() !== ''
              ? Number(l.descuentoPct)
              : undefined,
          pesoUnitKg: l.pesoUnitKg.trim() !== '' ? Number(l.pesoUnitKg) : undefined,
        })),
      }

      await compraService.registrar(documentoId, payload)
      onCompraRegistrada()
      onOpenChange(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo registrar la compra')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Registrar compra de factura</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2.5 rounded border border-red-200">{error}</p>}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="compra-proveedor" className="text-sm font-medium text-[#705D56]">
                Proveedor
              </label>
              {nuevoProveedor === null ? (
                <div className="flex gap-2">
                  <select
                    id="compra-proveedor"
                    value={proveedorId}
                    onChange={e => setProveedorId(e.target.value)}
                    className={SELECT}
                  >
                    <option value="">Elige un proveedor…</option>
                    {proveedores.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.nombre}
                      </option>
                    ))}
                  </select>
                  <Button
                    type="button"
                    variant="outline"
                    aria-label="Registrar proveedor nuevo"
                    title="Registrar proveedor nuevo"
                    onClick={() => setNuevoProveedor('')}
                    className="px-2.5"
                  >
                    <Plus className="w-4 h-4" />
                  </Button>
                </div>
              ) : (
                <div className="flex gap-2">
                  <Input
                    autoFocus
                    placeholder="Nombre del proveedor"
                    value={nuevoProveedor}
                    onChange={e => setNuevoProveedor(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        void crearProveedor()
                      }
                    }}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    disabled={!nuevoProveedor.trim() || creandoProveedor}
                    onClick={() => void crearProveedor()}
                    
                  >
                    {creandoProveedor ? 'Creando…' : 'Crear'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setNuevoProveedor(null)}
                    
                  >
                    Cancelar
                  </Button>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="compra-fecha" className="text-sm font-medium text-[#705D56]">
                Fecha
              </label>
              <Input
                id="compra-fecha"
                type="date"
                value={fecha}
                onChange={e => setFecha(e.target.value)}
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="compra-numero" className="text-sm font-medium text-[#705D56]">
                Número de factura (opcional)
              </label>
              <Input
                id="compra-numero"
                type="text"
                placeholder="Ej. F-10492"
                value={numeroFactura}
                onChange={e => setNumeroFactura(e.target.value)}
              />
            </div>
          </div>

          <div className="border border-[#E5E7EB] rounded overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB] text-sm font-semibold text-[#705D56]">
                  <th className="p-2.5 text-center w-12">Incluir</th>
                  <th className="p-2.5">Descripción</th>
                  <th className="p-2.5 text-right w-20">Cantidad</th>
                  <th className="p-2.5 text-right w-36">Precio sin IVA (C$)</th>
                  {hasNonGenericos && (
                    <th className="p-2.5 text-right w-28">Descuento (%)</th>
                  )}
                  <th className="p-2.5 text-right w-36">Peso unit. real (kg)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E5E7EB]">
                {lineasCompra.map(l => (
                  <tr
                    key={l.lineaId}
                    className={`hover:bg-gray-50/50 ${l.incluida ? '' : 'opacity-60 bg-gray-50/30'}`}
                  >
                    <td className="p-2 text-center">
                      <input
                        type="checkbox"
                        checked={l.incluida}
                        onChange={e =>
                          actualizarLinea(l.lineaId, { incluida: e.target.checked })
                        }
                        className="h-4 w-4 rounded border-gray-300 text-[#022F40] focus:ring-[#022F40] cursor-pointer"
                      />
                    </td>
                    <td className="p-2 text-sm text-[#022F40]">
                      <div className="font-medium">{l.descripcion}</div>
                      {l.compraId && (
                        <span className="text-sm text-[#558564] font-normal">
                          (ya tiene compra asignada)
                        </span>
                      )}
                    </td>
                    <td className="p-2 text-right text-sm text-[#022F40] font-mono">
                      {l.cantidad}
                    </td>
                    <td className="p-2 text-right">
                      <Input
                        type="number"
                        step="0.01"
                        min="0.01"
                        placeholder="0.00"
                        value={l.precioSinIva}
                        disabled={!l.incluida}
                        onChange={e =>
                          actualizarLinea(l.lineaId, { precioSinIva: e.target.value })
                        }
                        className="w-28 text-right ml-auto font-mono"
                      />
                    </td>
                    {hasNonGenericos && (
                      <td className="p-2 text-right">
                        {l.productoGenerico ? (
                          <span className="text-[#80727B] text-sm pr-4">—</span>
                        ) : (
                          <Input
                            type="number"
                            step="0.01"
                            min="0"
                            max="99.99"
                            placeholder="0"
                            value={l.descuentoPct}
                            disabled={!l.incluida}
                            onChange={e =>
                              actualizarLinea(l.lineaId, { descuentoPct: e.target.value })
                            }
                            className="w-24 text-right ml-auto font-mono"
                          />
                        )}
                      </td>
                    )}
                    <td className="p-2 text-right">
                      <Input
                        type="number"
                        step="0.01"
                        min="0.01"
                        placeholder="Opcional"
                        value={l.pesoUnitKg}
                        disabled={!l.incluida}
                        onChange={e =>
                          actualizarLinea(l.lineaId, { pesoUnitKg: e.target.value })
                        }
                        className="w-28 text-right ml-auto font-mono"
                      />
                      {(() => {
                        if (!l.incluida || l.metodoPrecio !== 'POR_PESO' || !l.pesoUnitKg.trim()) return null
                        const nuevo = recalcularPrecioPorPeso(l.metodoPrecio, l.parametros, l.pesoUnitKg, tipoCambio, ivaPct)
                        if (!nuevo) return null
                        return (
                          <p className="text-xs text-[#705D56] mt-1 text-right">
                            El precio unitario cambiará de {formatCurrency(l.precioUnit)} a {formatCurrency(Number(nuevo))}.
                          </p>
                        )
                      })()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <DialogFooter className="mt-2 flex items-center justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={guardando}
              className="bg-[#022F40] hover:bg-[#022F40]/90 text-white"
            >
              {guardando ? 'Guardando…' : 'Guardar compra'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
