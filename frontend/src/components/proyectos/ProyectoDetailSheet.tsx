import type { Proyecto } from '@/types'
import { useEffect, useState } from 'react'
import { facturaService } from '@/services/facturaService'
import { formatCurrency, formatDate } from '@/lib/format'
import { EstadoBadge } from '@/components/facturas/EstadoBadge'
import { Button } from '@/components/ui/button'
import { X, Pencil, FolderGit2, Building2, TrendingUp, AlertCircle } from 'lucide-react'

interface Props {
  proyecto: Proyecto | null
  open: boolean
  onOpenChange: (v: boolean) => void
  onEdit: (p: Proyecto) => void
}

export function ProyectoDetailSheet({ proyecto, open, onOpenChange, onEdit }: Props) {
  const [facturas, setFacturas] = useState<any[]>([])

  useEffect(() => {
    if (!proyecto?.id || !open) return
    facturaService.listar({ proyectoId: proyecto.id }).then(setFacturas).catch(console.error)
  }, [proyecto?.id, open])

  if (!open || !proyecto) return null

  const facturado = facturas.filter(f => f.estado !== 'COTIZACION').reduce((s, f) => s + f.totalVenta, 0)
  const cobrado = facturas.filter(f => f.estado !== 'COTIZACION').reduce((s, f) => s + (f.totalAbonado ?? f.abonos?.reduce((a: any, b: any) => a + b.monto, 0) ?? 0), 0)
  const porCobrar = facturado - cobrado

  const estadoColors = {
    ACTIVO: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    FINALIZADO: 'bg-[#022F40]/10 text-[#022F40] border-[#022F40]/20',
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end">
      <div className="absolute inset-0 bg-black/20" onClick={() => onOpenChange(false)} />

      <div className="relative z-10 w-full max-w-[420px] h-full bg-white border-l border-[#E5E7EB] flex flex-col shadow-sm overflow-hidden">

        {/* Header */}
        <div className="flex items-start justify-between px-6 py-5 border-b border-[#E5E7EB] shrink-0 bg-[#F9FAFB]">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0">
              <FolderGit2 className="w-4.5 h-4.5 text-[#91E5F6]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-semibold text-[#022F40] leading-tight">{proyecto.nombre}</h2>
                <span className={`text-[10px] font-semibold px-2 py-0.5 rounded border ${estadoColors[proyecto.estado]}`}>
                  {proyecto.estado}
                </span>
              </div>
              <p className="text-xs text-[#705D56] mt-1 flex items-center gap-1">
                <Building2 className="w-3 h-3" /> {proyecto.clienteNombre ?? 'Cliente desconocido'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <Button size="sm" variant="outline" onClick={() => onEdit(proyecto)} className="rounded border-[#E5E7EB] text-[#022F40] h-8 text-xs gap-1 bg-white">
              <Pencil className="w-3.5 h-3.5" /> Editar
            </Button>
            <button onClick={() => onOpenChange(false)} className="w-8 h-8 flex items-center justify-center rounded hover:bg-white text-[#80727B] transition-colors ml-1">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Financial Summary */}
        <div className="px-6 py-4 border-b border-[#E5E7EB] shrink-0 flex items-center justify-between">
          <div>
            <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Facturado</p>
            <p className="text-base font-bold text-[#022F40]">{formatCurrency(facturado)}</p>
          </div>
          <div className="h-8 w-px bg-[#E5E7EB]" />
          <div>
            <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Cobrado</p>
            <p className="text-base font-bold text-[#558564]">{formatCurrency(cobrado)}</p>
          </div>
          <div className="h-8 w-px bg-[#E5E7EB]" />
          <div>
            <p className="text-[10px] text-[#80727B] uppercase tracking-wide">Por Cobrar</p>
            <p className={`text-base font-bold ${porCobrar > 0 ? 'text-amber-600' : 'text-[#80727B]'}`}>
              {formatCurrency(porCobrar)}
            </p>
          </div>
        </div>

        {/* Body (Facturas) */}
        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-4">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
            <TrendingUp className="w-3.5 h-3.5" /> Facturas Asociadas
          </h3>

          {facturas.length === 0 ? (
            <div className="border border-dashed border-[#E5E7EB] rounded p-6 flex flex-col items-center justify-center text-center">
              <AlertCircle className="w-6 h-6 text-[#80727B] mb-2" />
              <p className="text-sm font-medium text-[#705D56]">Sin facturas</p>
              <p className="text-xs text-[#80727B] mt-1">Este proyecto aún no tiene facturas asociadas.</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {facturas.map(f => {
                const abonado = f.totalAbonado ?? f.abonos?.reduce((s: any, a: any) => s + a.monto, 0) ?? 0
                const saldo = f.saldoPendiente ?? (f.totalVenta - abonado)
                return (
                  <div key={f.id} className="border border-[#E5E7EB] rounded p-3 bg-white flex flex-col gap-2 shadow-sm hover:border-[#022F40]/20 transition-colors">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="text-sm font-semibold text-[#022F40] leading-none">{f.folioInterno}</p>
                        <p className="text-[10px] text-[#705D56] mt-1">{formatDate(f.createdAt)}</p>
                      </div>
                      <EstadoBadge estado={f.estado} />
                    </div>
                    
                    <div className="flex items-center justify-between mt-1 pt-2 border-t border-[#F3F4F6]">
                      <div>
                        <p className="text-[10px] text-[#80727B]">Total</p>
                        <p className="text-xs font-bold text-[#022F40]">{formatCurrency(f.totalVenta)}</p>
                      </div>
                      {f.estado !== 'COTIZACION' && saldo > 0 && (
                        <div className="text-right">
                          <p className="text-[10px] text-[#80727B]">Saldo pendiente</p>
                          <p className="text-xs font-bold text-amber-600">{formatCurrency(saldo)}</p>
                        </div>
                      )}
                      {f.estado !== 'COTIZACION' && saldo <= 0 && (
                        <div className="text-right">
                          <p className="text-[10px] text-[#80727B]">Estado</p>
                          <p className="text-xs font-bold text-[#558564]">Pagado</p>
                        </div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
