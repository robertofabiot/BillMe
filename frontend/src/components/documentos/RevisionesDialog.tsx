import { useEffect, useState } from 'react'
import type { Documento, MetodoPrecio, RevisionDocumento } from '@/types'
import { documentoService } from '@/services/documentoService'
import { formatCurrency, formatDate, formatUsd } from '@/lib/format'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { EstadoDocumentoBadge } from './EstadoDocumentoBadge'

interface Props {
  documentoActual: Documento
  open: boolean
  onOpenChange: (open: boolean) => void
}

function fechaHora(iso: string) {
  return new Date(iso).toLocaleString('es-NI', { dateStyle: 'medium', timeStyle: 'short' })
}

function formatoMetodo(m?: MetodoPrecio | null): string {
  switch (m) {
    case 'ULTIMO_PRECIO': return 'Último precio'
    case 'FORMULA_LISTA': return 'Fórmula de lista'
    case 'POR_PESO': return 'Por peso'
    case 'POR_METRO': return 'Por metro'
    case 'MANUAL': return 'Manual'
    default: return m || 'Manual'
  }
}

export function RevisionesDialog({ documentoActual, open, onOpenChange }: Props) {
  const [revisiones, setRevisiones] = useState<RevisionDocumento[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setRevisiones(null)
    setError(null)
    documentoService.revisiones(documentoActual.id)
      .then(setRevisiones)
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar los cambios'))
  }, [open, documentoActual.id])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-5xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Historial de cambios</DialogTitle>
        </DialogHeader>

        {error && <p className="text-sm text-red-600">{error}</p>}
        {!error && revisiones === null && <p className="text-sm text-[#705D56]">Cargando…</p>}
        {revisiones?.length === 0 && (
          <p className="text-sm text-[#705D56]">Este documento no se ha editado desde que se creó.</p>
        )}

        <div className="flex flex-col gap-6">
          {revisiones?.map((r, i) => {
            const doc = r.documento
            const siguiente = i === 0 ? documentoActual : revisiones[i - 1].documento

            // Diferencias en campos de cabecera
            const cambioFolio = doc.folio !== siguiente.folio
            const cambioEstado = doc.estado !== siguiente.estado
            const cambioCliente = doc.clienteNombre !== siguiente.clienteNombre
            const cambioProyecto = (doc.proyectoNombre ?? null) !== (siguiente.proyectoNombre ?? null)
            const cambioFecha = doc.fecha !== siguiente.fecha
            const cambioNotas = (doc.notas ?? '') !== (siguiente.notas ?? '')
            const cambioMostrarEntrega = Boolean(doc.mostrarEntrega) !== Boolean(siguiente.mostrarEntrega)
            const cambioMostrarTotalUsd = Boolean(doc.mostrarTotalUsd) !== Boolean(siguiente.mostrarTotalUsd)
            const cambioTipoCambio = doc.tipoCambio !== siguiente.tipoCambio
            const cambioTotalC = doc.total !== siguiente.total
            const cambioTotalUsd = doc.totalUsd !== siguiente.totalUsd
            const cambioTotalKg = doc.totalKg !== siguiente.totalKg

            // Líneas agregadas en el cambio que llevó a siguiente
            const idsEnDoc = new Set(doc.lineas.map(l => l.id))
            const lineasAgregadas = siguiente.lineas.filter(sl => !idsEnDoc.has(sl.id))

            return (
              <section key={r.numero} className="border border-[#E5E7EB] rounded overflow-hidden">
                <header className="px-4 py-2.5 bg-[#F9FAFB] border-b border-[#E5E7EB]">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-semibold text-base text-[#022F40]">Versión {r.numero}</span>
                    <span className="text-xs text-[#705D56]">
                      Así estaba antes de: <strong className="text-[#022F40]">{r.motivo}</strong> · {fechaHora(r.editadoEn)}{r.editadoPor ? ` por ${r.editadoPor}` : ''}
                    </span>
                  </div>
                </header>

                {/* Caja de campos del documento */}
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2 p-3 bg-white text-xs border-b border-[#E5E7EB]">
                  <div className={`p-1.5 rounded ${cambioFolio ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Folio</span>
                    <span className="text-[#022F40] font-semibold">{doc.folio}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioEstado ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Estado</span>
                    <EstadoDocumentoBadge documento={doc} />
                  </div>
                  <div className={`p-1.5 rounded ${cambioCliente ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Cliente</span>
                    <span className="text-[#022F40] font-semibold">{doc.clienteNombre}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioProyecto ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Proyecto</span>
                    <span className="text-[#022F40] font-semibold">{doc.proyectoNombre || '—'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioFecha ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Fecha</span>
                    <span className="text-[#022F40] font-semibold">{formatDate(doc.fecha)}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioNotas ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Notas</span>
                    <span className="text-[#022F40] font-semibold">{doc.notas || '—'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioMostrarEntrega ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Mostrar entrega al exportar</span>
                    <span className="text-[#022F40] font-semibold">{doc.mostrarEntrega ? 'Sí' : 'No'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioMostrarTotalUsd ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Mostrar total en dólares</span>
                    <span className="text-[#022F40] font-semibold">{doc.mostrarTotalUsd ? 'Sí' : 'No'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioTipoCambio ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Tipo de cambio</span>
                    <span className="text-[#022F40] font-semibold">{doc.tipoCambio != null ? Number(doc.tipoCambio).toFixed(4) : '—'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioTotalC ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Total C$</span>
                    <span className="text-[#022F40] font-semibold">{formatCurrency(doc.total)}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioTotalUsd ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Total U$</span>
                    <span className="text-[#022F40] font-semibold">{doc.totalUsd != null ? formatUsd(doc.totalUsd) : '—'}</span>
                  </div>
                  <div className={`p-1.5 rounded ${cambioTotalKg ? 'bg-amber-100' : ''}`}>
                    <span className="text-[#705D56] font-medium block">Total KG</span>
                    <span className="text-[#022F40] font-semibold">{(doc.totalKg ?? 0).toFixed(2)}</span>
                  </div>
                </div>

                {/* Tabla de líneas */}
                <div className="overflow-x-auto">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="text-[#705D56] bg-[#F9FAFB] border-b border-[#E5E7EB]">
                        <th className="text-left font-medium px-2.5 py-1.5">Cantidad</th>
                        <th className="text-left font-medium px-2.5 py-1.5">Descripción</th>
                        <th className="text-right font-medium px-2.5 py-1.5">Peso unit.</th>
                        <th className="text-right font-medium px-2.5 py-1.5">Peso total</th>
                        <th className="text-right font-medium px-2.5 py-1.5">Precio unit.</th>
                        <th className="text-right font-medium px-2.5 py-1.5">Total</th>
                        <th className="text-left font-medium px-2.5 py-1.5">Método</th>
                        <th className="text-left font-medium px-2.5 py-1.5">Fecha de entrega</th>
                        <th className="text-left font-medium px-2.5 py-1.5">Lugar de entrega</th>
                        <th className="text-right font-medium px-2.5 py-1.5">Costo unit. c/IVA</th>
                      </tr>
                    </thead>
                    <tbody>
                      {doc.lineas.map(l => {
                        const sigLinea = siguiente.lineas.find(sl => sl.id === l.id)
                        const eliminada = !sigLinea
                        const cambioCant = sigLinea && sigLinea.cantidad !== l.cantidad
                        const cambioPrecio = sigLinea && sigLinea.precioUnit !== l.precioUnit

                        return (
                          <tr
                            key={l.id}
                            className={`border-b border-[#E5E7EB] last:border-b-0 ${eliminada ? 'bg-red-50 text-red-900' : ''}`}
                          >
                            <td className={`px-2.5 py-1.5 font-mono ${cambioCant ? 'bg-amber-100 font-semibold' : ''}`}>
                              {l.cantidad}
                            </td>
                            <td className="px-2.5 py-1.5">
                              {l.descripcion}
                              {eliminada && (
                                <span className="ml-2 inline-block rounded bg-red-100 text-red-700 px-1.5 py-0.5 text-[10px] font-semibold">
                                  Se quitó en este cambio
                                </span>
                              )}
                            </td>
                            <td className="px-2.5 py-1.5 text-right text-[#705D56]">
                              {l.pesoUnitKg != null ? Number(l.pesoUnitKg).toFixed(2) : '—'}
                            </td>
                            <td className="px-2.5 py-1.5 text-right text-[#705D56]">
                              {l.pesoTotalKg != null ? Number(l.pesoTotalKg).toFixed(2) : '—'}
                            </td>
                            <td className={`px-2.5 py-1.5 text-right font-mono ${cambioPrecio ? 'bg-amber-100 font-semibold' : ''}`}>
                              {formatCurrency(l.precioUnit)}
                            </td>
                            <td className="px-2.5 py-1.5 text-right font-semibold text-[#022F40]">
                              {formatCurrency(l.total)}
                            </td>
                            <td className="px-2.5 py-1.5 text-[#705D56]">
                              {formatoMetodo(l.metodoPrecio)}
                            </td>
                            <td className="px-2.5 py-1.5 text-[#705D56]">
                              {l.fechaEntrega ? formatDate(l.fechaEntrega) : '—'}
                            </td>
                            <td className="px-2.5 py-1.5 text-[#705D56]">
                              {l.lugarEntrega || '—'}
                            </td>
                            <td className="px-2.5 py-1.5 text-right text-[#705D56]">
                              {l.costoUnitConIva != null ? formatCurrency(l.costoUnitConIva) : '—'}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {lineasAgregadas.length > 0 && (
                  <div className="px-3 py-2 text-xs bg-green-50 border-t border-[#E5E7EB] text-green-900">
                    <span className="font-semibold">Se agregó en este cambio: </span>
                    {lineasAgregadas.map(sl => sl.descripcion).join(', ')}
                  </div>
                )}
              </section>
            )
          })}
        </div>
      </DialogContent>
    </Dialog>
  )
}
