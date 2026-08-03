import type { Producto } from '@/types'
import { CLIENTES, FACTURAS } from '@/data/mock'
import { formatCurrency, formatDate } from '@/lib/format'
import { Button } from '@/components/ui/button'
import { X, Pencil, Tag, Weight, Hash, FileText, History, Calculator } from 'lucide-react'

interface PriceEntry {
  clienteId: string
  clienteNombre: string
  ultimoPrecio: number
  ultimaFecha: string
  cantidadCompras: number
}

function getPriceHistory(productoId: string): PriceEntry[] {
  const map = new Map<string, { precio: number; fecha: string; count: number }>()

  FACTURAS
    .filter(f => f.estado !== 'COTIZACION')
    .forEach(f => {
      const d = f.detalles.find(det => det.productoId === productoId)
      if (!d) return
      const ex = map.get(f.clienteId)
      if (!ex) {
        map.set(f.clienteId, { precio: d.precioUnitarioVenta, fecha: f.createdAt, count: 1 })
      } else {
        map.set(f.clienteId, {
          precio: f.createdAt > ex.fecha ? d.precioUnitarioVenta : ex.precio,
          fecha:  f.createdAt > ex.fecha ? f.createdAt : ex.fecha,
          count:  ex.count + 1,
        })
      }
    })

  return Array.from(map.entries())
    .map(([clienteId, data]) => ({
      clienteId,
      clienteNombre: CLIENTES.find(c => c.id === clienteId)?.nombre ?? '—',
      ultimoPrecio:  data.precio,
      ultimaFecha:   data.fecha,
      cantidadCompras: data.count,
    }))
    .sort((a, b) => b.ultimaFecha.localeCompare(a.ultimaFecha))
}

interface Props {
  producto: Producto | null
  open: boolean
  onOpenChange: (v: boolean) => void
  onEdit: (p: Producto) => void
}

export function ProductoDetailSheet({ producto, open, onOpenChange, onEdit }: Props) {
  if (!open || !producto) return null

  const history = getPriceHistory(producto.id)
  const totalVentas = FACTURAS
    .filter(f => f.estado !== 'COTIZACION')
    .reduce((s, f) => {
      const d = f.detalles.find(det => det.productoId === producto.id)
      return d ? s + d.cantidad : s
    }, 0)

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
              <p className="text-xs font-mono text-[#80727B]">{producto.codigoInterno}</p>
              <h2 className="text-base font-semibold text-[#022F40] leading-tight">{producto.nombrePrincipal}</h2>
              {producto.descripcion && (
                <p className="text-xs text-[#705D56] mt-0.5">{producto.descripcion}</p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <Button
              size="sm"
              variant="outline"
              onClick={() => onEdit(producto)}
              className="rounded border-[#E5E7EB] text-[#022F40] h-8 text-xs gap-1"
            >
              <Pencil className="w-3.5 h-3.5" /> Editar
            </Button>
            <button
              onClick={() => onOpenChange(false)}
              className="w-8 h-8 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] transition-colors ml-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Quick info row */}
        <div className="grid grid-cols-4 border-b border-[#E5E7EB] shrink-0">
          {[
            { icon: Weight,  label: 'Peso',          value: `${producto.peso} kg` },
            { icon: Calculator, label: 'Lista Prov.', value: producto.precioListaProveedor ? formatCurrency(producto.precioListaProveedor) : '—' },
            { icon: Calculator, label: 'Costo Neto', value: producto.costoNeto ? formatCurrency(producto.costoNeto) : '—' },
            { icon: History, label: 'Unidades vend.', value: totalVentas > 0 ? `${totalVentas}` : '—' },
          ].map((s, idx) => (
            <div key={idx} className="flex items-center gap-2 px-4 py-3 border-r border-[#E5E7EB] last:border-r-0">
              <s.icon className="w-3.5 h-3.5 text-[#80727B] shrink-0" />
              <div>
                <p className="text-[10px] text-[#705D56]">{s.label}</p>
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
              <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5" /> Alias
              </h3>
              <div className="flex flex-wrap gap-1.5">
                {producto.aliases.map(a => (
                  <span
                    key={a}
                    className="inline-flex items-center gap-1 bg-[#022F40]/8 text-[#022F40] text-xs px-2.5 py-1 rounded border border-[#022F40]/15 font-medium"
                  >
                    {a}
                  </span>
                ))}
              </div>
              <p className="text-[10px] text-[#80727B] mt-2">
                Este producto puede buscarse por cualquiera de estos alias al crear facturas.
              </p>
            </section>
          )}

          {/* Historial de precios por cliente */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5" /> Historial de Precios por Cliente
            </h3>

            {history.length === 0 ? (
              <p className="text-xs text-[#80727B] py-2">
                Este producto no ha sido vendido aún.
              </p>
            ) : (
              <div className="border border-[#E5E7EB] rounded overflow-hidden">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Cliente</th>
                      <th className="text-center px-3 py-2 font-medium text-[#705D56]">Compras</th>
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
    </div>
  )
}
