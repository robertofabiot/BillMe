import { useState, type ChangeEvent } from 'react'
import type { Factura, Abono, EstadoFactura } from '@/types'
import { CLIENTES, PROYECTOS } from '@/data/mock'
import { formatCurrency, formatDate, getSaldo } from '@/lib/format'
import { EstadoBadge } from './EstadoBadge'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import {
  User, FolderKanban, CalendarDays, Plus,
  CheckCircle2, ReceiptText, ArrowRight,
} from 'lucide-react'

interface Props {
  factura: Factura | null
  open: boolean
  onOpenChange: (v: boolean) => void
  onAddAbono: (facturaId: string, abono: Abono) => void
  onUpdateEstado: (facturaId: string, estado: EstadoFactura) => void
}

export function FacturaDetailSheet({ factura, open, onOpenChange, onAddAbono, onUpdateEstado }: Props) {
  const [showAbonoForm, setShowAbonoForm] = useState(false)
  const [abonoMonto, setAbonoMonto] = useState('')
  const [abonoFecha, setAbonoFecha] = useState(() => new Date().toISOString().split('T')[0])

  if (!factura) return null

  const cliente = CLIENTES.find(c => c.id === factura.clienteId)
  const proyecto = factura.proyectoId ? PROYECTOS.find(p => p.id === factura.proyectoId) : null
  const totalAbonado = factura.abonos.reduce((s, a) => s + a.monto, 0)
  const saldo = getSaldo(factura.totalVenta, factura.abonos)

  const costoTotal = factura.detalles.reduce((s, d) => s + (d.costoUnitario || 0) * d.cantidad, 0)
  const gananciaNeta = factura.totalVenta - costoTotal
  const margen = factura.totalVenta > 0 ? (gananciaNeta / factura.totalVenta) * 100 : 0

  const handleAddAbono = () => {
    const monto = parseFloat(abonoMonto)
    if (!monto || monto <= 0) return
    const abono: Abono = {
      id: `a-${Date.now()}`,
      monto,
      fechaPago: abonoFecha,
    }
    onAddAbono(factura.id, abono)
    setAbonoMonto('')
    setShowAbonoForm(false)
  }

  const handleConfirmar = () => onUpdateEstado(factura.id, 'PENDIENTE')
  const handleMarcarPagado = () => onUpdateEstado(factura.id, 'PAGADO')

  return (
    <Sheet open={open} onOpenChange={(v) => { onOpenChange(v); setShowAbonoForm(false) }}>
      <SheetContent side="right" className="w-full sm:w-[620px] p-0 flex flex-col overflow-hidden">

        {/* Header */}
        <SheetHeader className="px-6 py-5 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <SheetTitle className="text-base font-semibold font-mono text-[#022F40]">
                  {factura.folioInterno}
                </SheetTitle>
                <EstadoBadge estado={factura.estado} />
              </div>
              <p className="text-xs text-[#705D56] flex items-center gap-1">
                <CalendarDays className="w-3 h-3" />
                {formatDate(factura.createdAt)}
              </p>
            </div>

            {/* Actions */}
            <div className="flex gap-2 shrink-0">
              {factura.estado === 'COTIZACION' && (
                <Button
                  onClick={handleConfirmar}
                  size="sm"
                  className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-xs h-8 gap-1"
                >
                  <ArrowRight className="w-3.5 h-3.5" />
                  Confirmar Venta
                </Button>
              )}
              {(factura.estado === 'PENDIENTE' || factura.estado === 'PAGO_PARCIAL') && (
                <>
                  <Button
                    onClick={() => setShowAbonoForm(v => !v)}
                    size="sm"
                    variant="outline"
                    className="rounded border-[#E5E7EB] text-[#022F40] text-xs h-8 gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Abono
                  </Button>
                  {saldo <= 0 && (
                    <Button
                      onClick={handleMarcarPagado}
                      size="sm"
                      className="rounded bg-[#558564] hover:bg-[#558564]/90 text-white text-xs h-8 gap-1"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Marcar Pagado
                    </Button>
                  )}
                </>
              )}
            </div>
          </div>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-6">

          {/* Cliente / Proyecto */}
          <section className="flex gap-4">
            <div className="flex-1 bg-[#F9FAFB] border border-[#E5E7EB] rounded px-4 py-3 flex items-start gap-2">
              <User className="w-4 h-4 text-[#80727B] mt-0.5 shrink-0" />
              <div>
                <p className="text-xs text-[#705D56]">Cliente</p>
                <p className="text-sm font-semibold text-[#022F40]">{cliente?.nombre ?? '—'}</p>
                {cliente?.telefono && <p className="text-xs text-[#80727B]">{cliente.telefono}</p>}
              </div>
            </div>
            {proyecto && (
              <div className="flex-1 bg-[#F9FAFB] border border-[#E5E7EB] rounded px-4 py-3 flex items-start gap-2">
                <FolderKanban className="w-4 h-4 text-[#80727B] mt-0.5 shrink-0" />
                <div>
                  <p className="text-xs text-[#705D56]">Proyecto</p>
                  <p className="text-sm font-semibold text-[#022F40]">{proyecto.nombre}</p>
                  <p className="text-xs text-[#80727B]">{proyecto.estado === 'ACTIVO' ? 'Activo' : 'Finalizado'}</p>
                </div>
              </div>
            )}
          </section>

          {/* Line items */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2 flex items-center gap-1.5">
              <ReceiptText className="w-3.5 h-3.5" /> Productos
            </h3>
            <div className="border border-[#E5E7EB] rounded overflow-hidden">
              <table className="w-full text-xs">
                <thead>
                  <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                    <th className="text-left px-3 py-2 font-medium text-[#705D56]">Producto</th>
                    <th className="text-center px-2 py-2 font-medium text-[#705D56] w-16">Cant.</th>
                    <th className="text-right px-3 py-2 font-medium text-[#705D56] w-24">Precio</th>
                    <th className="text-right px-3 py-2 font-medium text-[#705D56] w-28">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {factura.detalles.map((d, i) => (
                    <tr key={d.id} className={`border-b border-[#E5E7EB] ${i === factura.detalles.length - 1 ? 'border-b-0' : ''}`}>
                      <td className="px-3 py-2.5">
                        <p className="text-[#022F40] font-medium">{d.productoNombre}</p>
                        <p className="text-[#80727B] font-mono">{d.productoCodigoInterno}</p>
                      </td>
                      <td className="px-2 py-2.5 text-center text-[#022F40]">{d.cantidad}</td>
                      <td className="px-3 py-2.5 text-right text-[#705D56]">{formatCurrency(d.precioUnitarioVenta)}</td>
                      <td className="px-3 py-2.5 text-right font-semibold text-[#022F40]">
                        {formatCurrency(d.cantidad * d.precioUnitarioVenta)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {/* Totals */}
          <section className="bg-[#F9FAFB] border border-[#E5E7EB] rounded px-4 py-3 flex flex-col gap-2">
            <div className="flex justify-between text-sm">
              <span className="text-[#705D56]">Total venta</span>
              <span className="font-semibold text-[#022F40]">{formatCurrency(factura.totalVenta)}</span>
            </div>
            {costoTotal > 0 && (
              <>
                <div className="flex justify-between text-sm">
                  <span className="text-[#705D56]">Costo mercancía</span>
                  <span className="text-[#705D56]">− {formatCurrency(costoTotal)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-[#558564] font-medium">Ganancia Neta</span>
                  <div className="text-right">
                    <span className="font-bold text-[#558564]">{formatCurrency(gananciaNeta)}</span>
                    <span className="text-[10px] text-[#558564] ml-2 bg-[#558564]/10 px-1.5 py-0.5 rounded">
                      {margen.toFixed(1)}% margen
                    </span>
                  </div>
                </div>
                <Separator className="bg-[#E5E7EB] my-1" />
              </>
            )}
            {totalAbonado > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-[#558564]">Abonado</span>
                <span className="font-semibold text-[#558564]">− {formatCurrency(totalAbonado)}</span>
              </div>
            )}
            {factura.estado !== 'COTIZACION' && (
              <>
                <Separator className="bg-[#E5E7EB]" />
                <div className="flex justify-between">
                  <span className="text-sm font-semibold text-[#022F40]">Saldo pendiente</span>
                  <span className={`text-base font-bold ${saldo <= 0 ? 'text-[#558564]' : 'text-[#022F40]'}`}>
                    {saldo <= 0 ? 'Liquidado ✓' : formatCurrency(saldo)}
                  </span>
                </div>
              </>
            )}
          </section>

          {/* Agregar abono form */}
          {showAbonoForm && (
            <section className="border border-[#91E5F6]/50 bg-[#91E5F6]/5 rounded px-4 py-4 flex flex-col gap-3">
              <h4 className="text-xs font-semibold text-[#022F40]">Registrar Abono</h4>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label className="text-xs text-[#705D56]">Monto</Label>
                  <Input
                    type="number"
                    min={0.01}
                    step={0.01}
                    placeholder="0.00"
                    value={abonoMonto}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setAbonoMonto(e.target.value)}
                    className="h-8 rounded border-[#E5E7EB] text-sm"
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label className="text-xs text-[#705D56]">Fecha de pago</Label>
                  <Input
                    type="date"
                    value={abonoFecha}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setAbonoFecha(e.target.value)}
                    className="h-8 rounded border-[#E5E7EB] text-sm"
                  />
                </div>
              </div>
              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowAbonoForm(false)}
                  className="rounded border-[#E5E7EB] text-xs h-8"
                >
                  Cancelar
                </Button>
                <Button
                  size="sm"
                  onClick={handleAddAbono}
                  disabled={!abonoMonto || parseFloat(abonoMonto) <= 0}
                  className="rounded bg-[#022F40] text-white text-xs h-8"
                >
                  Guardar Abono
                </Button>
              </div>
            </section>
          )}

          {/* Historial de abonos */}
          {factura.abonos.length > 0 && (
            <section>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-[#80727B] mb-2">
                Historial de Pagos
              </h3>
              <div className="flex flex-col gap-1">
                {factura.abonos.map(a => (
                  <div
                    key={a.id}
                    className="flex items-center justify-between px-3 py-2.5 bg-white border border-[#E5E7EB] rounded"
                  >
                    <div className="flex items-center gap-2">
                      <div className="w-1.5 h-1.5 rounded-full bg-[#558564]" />
                      <span className="text-xs text-[#705D56]">{formatDate(a.fechaPago)}</span>
                    </div>
                    <span className="text-sm font-semibold text-[#558564]">
                      {formatCurrency(a.monto)}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
