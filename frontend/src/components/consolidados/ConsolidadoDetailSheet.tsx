import type { Consolidado } from '@/types'
import { formatCurrency, formatDate } from '@/lib/format'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { User, CalendarDays, Layers } from 'lucide-react'

interface Props {
  consolidado: Consolidado | null
  open: boolean
  onOpenChange: (v: boolean) => void
}

export function ConsolidadoDetailSheet({ consolidado, open, onOpenChange }: Props) {
  if (!consolidado) return null

  // Ensure total fallback logic if missing from backend
  const totalGeneral = consolidado.totalGeneral ?? consolidado.grupos.reduce((accG, g) => 
    accG + g.items.reduce((accI, i) => accI + (i.cantidad * i.precioUnitario), 0)
  , 0)

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:w-[620px] p-0 flex flex-col overflow-hidden">
        {/* Header */}
        <SheetHeader className="px-6 py-5 border-b border-[#E5E7EB] shrink-0 bg-[#F9FAFB]">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <div className="w-8 h-8 rounded bg-[#022F40] flex items-center justify-center shrink-0">
                  <Layers className="w-4 h-4 text-[#91E5F6]" />
                </div>
                <div>
                  <p className="text-[10px] font-bold text-[#80727B] uppercase">{consolidado.folioInterno}</p>
                  <SheetTitle className="text-base font-semibold text-[#022F40] leading-tight">
                    {consolidado.nombre}
                  </SheetTitle>
                </div>
              </div>
            </div>
          </div>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-6 bg-[#F9FAFB]/50">
          {/* Metadata */}
          <section className="flex gap-4">
            <div className="flex-1 bg-white border border-[#E5E7EB] rounded px-4 py-3 flex items-start gap-2 shadow-sm">
              <User className="w-4 h-4 text-[#80727B] mt-0.5 shrink-0" />
              <div>
                <p className="text-xs text-[#705D56]">Cliente</p>
                <p className="text-sm font-semibold text-[#022F40]">{consolidado.clienteNombre ?? '—'}</p>
              </div>
            </div>
            <div className="flex-1 bg-white border border-[#E5E7EB] rounded px-4 py-3 flex items-start gap-2 shadow-sm">
              <CalendarDays className="w-4 h-4 text-[#80727B] mt-0.5 shrink-0" />
              <div>
                <p className="text-xs text-[#705D56]">Fecha de Creación</p>
                <p className="text-sm font-semibold text-[#022F40]">{formatDate(consolidado.createdAt)}</p>
              </div>
            </div>
          </section>

          {/* Groups */}
          <section className="flex flex-col gap-4">
            {consolidado.grupos.map((g) => {
              const subtotal = g.subtotal ?? g.items.reduce((acc, i) => acc + (i.cantidad * i.precioUnitario), 0)
              
              return (
                <div key={g.id} className="border border-[#E5E7EB] rounded overflow-hidden shadow-sm bg-white">
                  <div className="bg-[#F9FAFB] border-b border-[#E5E7EB] px-4 py-2 flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-[#022F40]">{g.nombre}</h3>
                  </div>
                  <div className="bg-white">
                    {g.items.length === 0 ? (
                      <p className="text-xs text-[#705D56] p-3 text-center italic">Sin elementos en este grupo</p>
                    ) : (
                      <table className="w-full text-xs">
                        <thead>
                          <tr className="border-b border-[#E5E7EB] text-[#705D56] bg-white">
                            <th className="text-left px-4 py-2 font-medium">Concepto</th>
                            <th className="text-center px-2 py-2 font-medium w-16">Cant.</th>
                            <th className="text-right px-4 py-2 font-medium w-24">Precio</th>
                            <th className="text-right px-4 py-2 font-medium w-28">Subtotal</th>
                          </tr>
                        </thead>
                        <tbody>
                          {g.items.map((item, i) => (
                            <tr key={item.id} className={i === g.items.length - 1 ? '' : 'border-b border-[#E5E7EB]/50'}>
                              <td className="px-4 py-2 text-[#022F40] font-medium">{item.productoNombre}</td>
                              <td className="px-2 py-2 text-center text-[#705D56]">{item.cantidad}</td>
                              <td className="px-4 py-2 text-right text-[#705D56]">{formatCurrency(item.precioUnitario)}</td>
                              <td className="px-4 py-2 text-right font-semibold text-[#022F40]">
                                {formatCurrency(item.cantidad * item.precioUnitario)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                  <div className="bg-[#022F40]/5 border-t border-[#E5E7EB] px-4 py-2 flex justify-between items-center">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#022F40]">Subtotal {g.nombre}</span>
                    <span className="text-sm font-bold text-[#022F40]">{formatCurrency(subtotal)}</span>
                  </div>
                </div>
              )
            })}
          </section>
        </div>

        {/* Footer / Total */}
        <div className="px-6 py-4 border-t border-[#E5E7EB] bg-white shrink-0 flex items-center justify-between">
          <p className="text-xs font-bold uppercase tracking-wider text-[#80727B]">Total General</p>
          <p className="text-xl font-bold text-[#022F40]">{formatCurrency(totalGeneral)}</p>
        </div>
      </SheetContent>
    </Sheet>
  )
}
