import { useEffect, useState } from 'react'
import type { RevisionDocumento } from '@/types'
import { documentoService } from '@/services/documentoService'
import { formatCurrency, formatDate } from '@/lib/format'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

interface Props {
  documentoId: string
  open: boolean
  onOpenChange: (v: boolean) => void
}

function fechaHora(iso: string) {
  return new Date(iso).toLocaleString('es-NI', { dateStyle: 'medium', timeStyle: 'short' })
}

/** Muestra cómo era el documento antes de cada edición, de la más reciente a la primera. */
export function RevisionesDialog({ documentoId, open, onOpenChange }: Props) {
  const [revisiones, setRevisiones] = useState<RevisionDocumento[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setRevisiones(null)
    setError(null)
    documentoService.revisiones(documentoId)
      .then(setRevisiones)
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar los cambios'))
  }, [open, documentoId])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Versiones anteriores</DialogTitle>
        </DialogHeader>

        {error && <p className="text-sm text-red-600">{error}</p>}
        {!error && revisiones === null && <p className="text-sm text-[#705D56]">Cargando…</p>}
        {revisiones?.length === 0 && (
          <p className="text-sm text-[#705D56]">Este documento no se ha editado desde que se creó.</p>
        )}

        <div className="flex flex-col gap-5">
          {revisiones?.map(r => (
            <section key={r.numero} className="border border-[#E5E7EB] rounded">
              <header className="flex flex-wrap justify-between gap-2 px-3 py-2 bg-[#F9FAFB] border-b border-[#E5E7EB] text-sm">
                <span className="font-semibold text-[#022F40]">Versión {r.numero}</span>
                <span className="text-sm text-[#705D56]">
                  Cambiada el {fechaHora(r.editadoEn)}{r.editadoPor ? ` por ${r.editadoPor}` : ''}
                </span>
              </header>
              <p className="px-3 pt-2 text-sm text-[#705D56]">
                {r.documento.clienteNombre}
                {r.documento.proyectoNombre ? ` · ${r.documento.proyectoNombre}` : ''}
                {' · '}{formatDate(r.documento.fecha)}
              </p>
              <table className="w-full text-sm mt-1">
                <thead>
                  <tr className="text-[#705D56] border-b border-[#E5E7EB]">
                    <th className="text-left font-medium px-3 py-1.5">Cantidad</th>
                    <th className="text-left font-medium px-3 py-1.5">Descripción</th>
                    <th className="text-right font-medium px-3 py-1.5">Precio unit.</th>
                    <th className="text-right font-medium px-3 py-1.5">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {r.documento.lineas.map(l => (
                    <tr key={l.id} className="border-b border-[#E5E7EB] last:border-b-0">
                      <td className="px-3 py-1.5">{l.cantidad}</td>
                      <td className="px-3 py-1.5">{l.descripcion}</td>
                      <td className="px-3 py-1.5 text-right">{formatCurrency(l.precioUnit)}</td>
                      <td className="px-3 py-1.5 text-right">{formatCurrency(l.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="px-3 py-2 text-sm text-right font-semibold text-[#022F40] border-t border-[#E5E7EB]">
                Total {formatCurrency(r.documento.total)}
              </p>
            </section>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  )
}
