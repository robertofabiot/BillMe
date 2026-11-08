import type { Documento, FuentePrecio, PrecioLista, PreciosListaProducto, Producto } from '@/types'
import { useCallback, useEffect, useState } from 'react'
import { documentoService } from '@/services/documentoService'
import { productoService } from '@/services/productoService'
import { precioListaService } from '@/services/precioListaService'
import { formatCurrency, formatDate } from '@/lib/format'
import { Button } from '@/components/ui/button'
import { CalcularListaDialog } from '@/components/precios/CalcularListaDialog'
import { X, Pencil, Trash2, Tag, Weight, Hash, FileText, History, Calculator, Receipt } from 'lucide-react'

const FUENTES: Record<FuentePrecio, string> = {
  LISTA_IMPORTADA: 'Lista del proveedor',
  FACTURA_PROVEEDOR: 'Factura del proveedor',
  PRECIO_CLIENTE: 'Precio a un cliente',
  MANUAL: 'Escrito a mano',
}

function origen(p: PrecioLista) {
  if (p.precioOrigen == null) return ''
  const descuento = p.descuentoPct != null ? ` con ${p.descuentoPct}%` : ''
  return p.fuente === 'PRECIO_CLIENTE'
    ? `${formatCurrency(p.precioOrigen)} con IVA${descuento}`
    : `${formatCurrency(p.precioOrigen)} sin IVA${descuento}`
}

interface PriceEntry {
  clienteId: string
  clienteNombre: string
  ultimoPrecio: number
  ultimaFecha: string
  cantidadCompras: number
}

/** Último precio dado a cada cliente. Las cotizaciones cuentan; las facturas anuladas no. */
function processHistory(productoId: string, documentos: Documento[]): PriceEntry[] {
  const map = new Map<string, PriceEntry>()

  // Vienen de la más reciente a la más antigua
  documentos
    .filter(d => d.estado !== 'ANULADA')
    .forEach(d => {
      const linea = d.lineas.find(l => l.productoId === productoId)
      if (!linea) return
      const entrada = map.get(d.clienteId)
      if (entrada) {
        entrada.cantidadCompras += 1
      } else {
        map.set(d.clienteId, {
          clienteId: d.clienteId,
          clienteNombre: d.clienteNombre,
          ultimoPrecio: linea.precioUnit,
          ultimaFecha: d.fecha,
          cantidadCompras: 1,
        })
      }
    })

  return Array.from(map.values())
}

interface Props {
  producto: Producto | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onEdit: (producto: Producto) => void
  /** Avisa que cambió el precio de lista para refrescar la lista de productos. */
  onPrecioActualizado?: () => void
}

export function ProductoDetailSheet({ producto, open, onOpenChange, onEdit, onPrecioActualizado }: Props) {
  const [history, setHistory] = useState<PriceEntry[]>([])
  const [totalDocumentos, setTotalDocumentos] = useState(0)
  const [totalVentas, setTotalVentas] = useState(0)
  const [precios, setPrecios] = useState<PreciosListaProducto | null>(null)
  const [calculando, setCalculando] = useState(false)

  const cargarPrecios = useCallback(() => {
    if (!producto?.id) return
    precioListaService.consultar(producto.id).then(setPrecios).catch(console.error)
  }, [producto?.id])

  useEffect(() => {
    if (!producto?.id || !open) return
    cargarPrecios()
    // Solo los documentos que incluyen este producto (tamano=200)
    documentoService.listar({ productoId: producto.id, tamano: 200 }).then(res => {
      setTotalDocumentos(res.totalElementos)
      setHistory(processHistory(producto.id, res.contenido))
      setTotalVentas(res.contenido
        .filter(d => d.tipo === 'FACTURA' && d.estado === 'ACTIVA')
        .flatMap(d => d.lineas)
        .filter(l => l.productoId === producto.id)
        .reduce((s, l) => s + l.cantidad, 0))
    }).catch(console.error)
  }, [producto?.id, open, cargarPrecios])

  if (!open || !producto) return null

  const vigente = precios?.vigente ?? null

  const handleEliminar = async () => {
    if (!producto) return
    if (!window.confirm(`¿Eliminar el producto ${producto.nombrePrincipal}?`)) return
    try {
      await productoService.eliminar(producto.id)
      onOpenChange(false)
      window.location.reload()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al eliminar el producto')
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end">
      <div className="absolute inset-0 bg-black/20" onClick={() => onOpenChange(false)} />

      <div className="relative z-10 w-full max-w-[520px] h-full bg-white border-l border-[#E5E7EB] flex flex-col shadow-sm overflow-hidden">

        {/* Header */}
        <div className="flex items-start justify-between px-6 py-5 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0">
              <Hash className="w-4.5 h-4.5 text-[#91E5F6]" />
            </div>
            <div>
              <p className="text-sm font-mono text-[#80727B]">{producto.codigoInterno}</p>
              <h2 className="text-base font-semibold text-[#022F40] leading-tight">{producto.nombrePrincipal}</h2>
              {producto.descripcion && (
                <p className="text-sm text-[#705D56] mt-0.5">{producto.descripcion}</p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <Button
              variant="outline"
              onClick={handleEliminar}
              className="rounded border-red-200 text-red-600 hover:bg-red-50 gap-1"
            >
              <Trash2 className="w-3.5 h-3.5" /> Eliminar
            </Button>
            <Button
              variant="outline"
              onClick={() => onEdit(producto)}
              className="rounded border-[#E5E7EB] text-[#022F40] gap-1"
            >
              <Pencil className="w-3.5 h-3.5" /> Editar
            </Button>
            <button
              onClick={() => onOpenChange(false)}
              className="w-11 h-11 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] transition-colors ml-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Quick info row */}
        <div className="grid grid-cols-4 border-b border-[#E5E7EB] shrink-0">
          {[
            { icon: Weight,  label: 'Peso',          value: producto.peso ? `${producto.peso} kg` : '—' },
            { icon: Calculator, label: 'Lista (sin IVA)', value: vigente ? formatCurrency(vigente.precioSinIva) : '—' },
            { icon: Calculator, label: 'Lista desde', value: vigente ? formatDate(vigente.fecha) : '—' },
            { icon: History, label: 'Unidades vend.', value: totalVentas > 0 ? `${totalVentas}` : '—' },
          ].map((s, idx) => (
            <div key={idx} className="flex items-center gap-2 px-4 py-3 border-r border-[#E5E7EB] last:border-r-0">
              <s.icon className="w-3.5 h-3.5 text-[#80727B] shrink-0" />
              <div>
                <p className="text-sm text-[#705D56]">{s.label}</p>
                <p className="text-sm font-semibold text-[#022F40]">{s.value}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-6">

          {/* Aliases */}
          {producto.aliases.length > 0 && (
            <section>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5" /> Alias
              </h3>
              <div className="flex flex-wrap gap-1.5">
                {producto.aliases.map(a => (
                  <span
                    key={a}
                    className="inline-flex items-center gap-1 bg-[#022F40]/8 text-[#022F40] text-sm px-2.5 py-1 rounded border border-[#022F40]/15 font-medium"
                  >
                    {a}
                  </span>
                ))}
              </div>
              <p className="text-sm text-[#80727B] mt-2">
                Este producto puede buscarse por cualquiera de estos alias al crear facturas.
              </p>
            </section>
          )}

          {/* Precio de lista del proveedor */}
          <section>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
                <Receipt className="w-3.5 h-3.5" /> Precio de lista del proveedor
              </h3>
              <Button variant="outline" onClick={() => setCalculando(true)} className="gap-1">
                <Calculator className="w-3.5 h-3.5" /> Calcular precio de lista
              </Button>
            </div>

            {!precios || precios.historial.length === 0 ? (
              <p className="text-sm text-[#80727B] py-2">
                Sin precio de lista. Calcúlalo desde la factura del proveedor o desde el precio dado a un cliente.
              </p>
            ) : (
              <div className="border border-[#E5E7EB] rounded overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Desde</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Lista (sin IVA)</th>
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Cómo se obtuvo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {precios.historial.map((p, i) => (
                      <tr key={p.id} className={`border-b border-[#E5E7EB] last:border-b-0 ${i === 0 ? 'bg-[#558564]/5' : ''}`}>
                        <td className="px-3 py-2.5 text-[#705D56]">{formatDate(p.fecha)}</td>
                        <td className="px-3 py-2.5 text-right font-semibold text-[#022F40]">{formatCurrency(p.precioSinIva)}</td>
                        <td className="px-3 py-2.5 text-[#705D56]">
                          <p>{FUENTES[p.fuente]}{i === 0 && <span className="ml-1 text-[#558564] font-medium">· vigente</span>}</p>
                          {origen(p) && <p className="text-sm text-[#80727B]">{origen(p)}</p>}
                          {p.nota && <p className="text-sm text-[#80727B]">{p.nota}</p>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Historial de precios por cliente */}
          <section>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5" /> Historial de Precios por Cliente
            </h3>

            {totalDocumentos > 200 && (
              <p className="text-sm text-[#705D56] bg-amber-50 border border-amber-200 px-3 py-1.5 rounded mb-2">
                Se muestran los 200 más recientes.
              </p>
            )}

            {history.length === 0 ? (
              <p className="text-sm text-[#80727B] py-2">
                Este producto no ha sido vendido aún.
              </p>
            ) : (
              <div className="border border-[#E5E7EB] rounded overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Cliente</th>
                      <th className="text-center px-3 py-2 font-medium text-[#705D56]">Veces</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Último Precio</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Última Fecha</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h, i) => (
                      <tr
                        key={h.clienteId}
                        className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] transition-colors ${i === history.length - 1 ? 'border-b-0' : ''}`}
                      >
                        <td className="px-3 py-2.5 font-medium text-[#022F40]">{h.clienteNombre}</td>
                        <td className="px-3 py-2.5 text-center text-[#705D56]">{h.cantidadCompras}</td>
                        <td className="px-3 py-2.5 text-right font-semibold text-[#022F40]">
                          {formatCurrency(h.ultimoPrecio)}
                        </td>
                        <td className="px-3 py-2.5 text-right text-[#705D56]">
                          {formatDate(h.ultimaFecha)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      </div>

      <CalcularListaDialog
        producto={producto}
        descuentoSugerido={precios?.descuentoProveedorSugerido ?? null}
        open={calculando}
        onOpenChange={setCalculando}
        onGuardado={() => { cargarPrecios(); onPrecioActualizado?.() }}
      />
    </div>
  )
}
