import type { Proyecto, Documento } from '@/types'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { documentoService } from '@/services/documentoService'
import { clienteService } from '@/services/clienteService'
import { formatCurrency, formatDate } from '@/lib/format'
import { EstadoDocumentoBadge } from '@/components/documentos/EstadoDocumentoBadge'
import { Button } from '@/components/ui/button'
import { X, Pencil, FolderGit2, Building2, TrendingUp, AlertCircle } from 'lucide-react'

interface Props {
  proyecto: Proyecto | null
  open: boolean
  onOpenChange: (v: boolean) => void
  onEdit: (p: Proyecto) => void
}

export function ProyectoDetailSheet({ proyecto, open, onOpenChange, onEdit }: Props) {
  const [documentos, setDocumentos] = useState<Documento[]>([])
  const [cuentaProyecto, setCuentaProyecto] = useState<{ facturado: number; abonado: number; saldo: number } | null>(null)

  useEffect(() => {
    if (!proyecto?.id || !open) return
    documentoService.listar({ proyectoId: proyecto.id }).then(setDocumentos).catch(console.error)
    clienteService.cuenta(proyecto.clienteId).then(cuenta => {
      const fila = cuenta.proyectos.find(p => p.proyectoId === proyecto.id)
      if (fila) {
        setCuentaProyecto({ facturado: fila.facturado, abonado: fila.abonado, saldo: fila.saldo })
      } else {
        setCuentaProyecto({ facturado: 0, abonado: 0, saldo: 0 })
      }
    }).catch(console.error)
  }, [proyecto?.id, proyecto?.clienteId, open])

  if (!open || !proyecto) return null

  const facturado = cuentaProyecto?.facturado ?? 0
  const cobrado = cuentaProyecto?.abonado ?? 0
  const porCobrar = cuentaProyecto?.saldo ?? 0

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
                <span className={`text-sm font-semibold px-2 py-0.5 rounded border ${estadoColors[proyecto.estado]}`}>
                  {proyecto.estado}
                </span>
              </div>
              <p className="text-sm text-[#705D56] mt-1 flex items-center gap-1">
                <Building2 className="w-3 h-3" /> {proyecto.clienteNombre ?? 'Cliente desconocido'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <Button size="sm" variant="outline" onClick={() => onEdit(proyecto)} className="rounded border-[#E5E7EB] text-[#022F40] h-8 text-sm gap-1 bg-white">
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
            <p className="text-sm text-[#80727B] uppercase tracking-wide">Facturado</p>
            <p className="text-base font-bold text-[#022F40]">{formatCurrency(facturado)}</p>
          </div>
          <div className="w-px bg-[#E5E7EB]" />
          <div>
            <p className="text-sm text-[#80727B] uppercase tracking-wide">Cobrado</p>
            <p className="text-base font-bold text-[#558564]">{formatCurrency(cobrado)}</p>
          </div>
          <div className="w-px bg-[#E5E7EB]" />
          <div>
            <p className="text-sm text-[#80727B] uppercase tracking-wide">Por Cobrar</p>
            <p className={`text-base font-bold ${porCobrar > 0 ? 'text-amber-600' : 'text-[#80727B]'}`}>
              {formatCurrency(porCobrar)}
            </p>
          </div>
        </div>

        {/* Body (Facturas) */}
        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-4">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
            <TrendingUp className="w-3.5 h-3.5" /> Cotizaciones y facturas
          </h3>

          {documentos.length === 0 ? (
            <div className="border border-dashed border-[#E5E7EB] rounded p-6 flex flex-col items-center justify-center text-center">
              <AlertCircle className="w-6 h-6 text-[#80727B] mb-2" />
              <p className="text-sm font-medium text-[#705D56]">Sin documentos</p>
              <p className="text-sm text-[#80727B] mt-1">Este proyecto aún no tiene cotizaciones ni facturas.</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {documentos.map(d => (
                <Link key={d.id} to={`/documentos/${d.id}`} className="border border-[#E5E7EB] rounded p-3 bg-white flex flex-col gap-2 shadow-sm hover:border-[#022F40]/20 transition-colors">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-sm font-semibold text-[#022F40] leading-none">{d.folio}</p>
                      <p className="text-sm text-[#705D56] mt-1">{formatDate(d.fecha)}</p>
                    </div>
                    <EstadoDocumentoBadge documento={d} />
                  </div>

                  <div className="flex items-center justify-between mt-1 pt-2 border-t border-[#F3F4F6]">
                    <div>
                      <p className="text-sm text-[#80727B]">Total</p>
                      <p className="text-sm font-bold text-[#022F40]">{formatCurrency(d.total)}</p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
