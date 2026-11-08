import { useState, useEffect, type ChangeEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Cliente, MonedaTarifa } from '@/types'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { User, X } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  initial?: Cliente | null
  onSave: (cliente: Omit<Cliente, 'id'> & { id?: string }) => Promise<void>
}

export function ClienteModal({ open, onOpenChange, initial, onSave }: Props) {
  const [nombre,            setNombre]            = useState('')
  const [telefono,          setTelefono]          = useState('')
  const [detalles,          setDetalles]          = useState('')
  const [descuentoPct,      setDescuentoPct]      = useState('')
  const [tarifaKg,          setTarifaKg]          = useState('')
  const [tarifaMoneda,      setTarifaMoneda]      = useState<MonedaTarifa>('USD')
  const [saldoInicial,      setSaldoInicial]      = useState('')
  const [fechaInicioCuenta, setFechaInicioCuenta] = useState('')
  const [error,             setError]             = useState<string | null>(null)
  const [guardando,         setGuardando]         = useState(false)

  useEffect(() => {
    if (open) {
      setNombre(initial?.nombre ?? '')
      setTelefono(initial?.telefono ?? '')
      setDetalles(initial?.detalles ?? '')
      setDescuentoPct(initial?.descuentoPct != null ? String(initial.descuentoPct) : '0')
      setTarifaKg(initial?.tarifaKg != null ? String(initial.tarifaKg) : '')
      setTarifaMoneda(initial?.tarifaMoneda ?? 'USD')
      setSaldoInicial(initial?.saldoInicial != null ? String(initial.saldoInicial) : '0')
      setFechaInicioCuenta(initial?.fechaInicioCuenta ?? '')
      setError(null)
      setGuardando(false)
    }
  }, [open, initial])

  useEffect(() => {
    if (!open) return
    const h = (e: KeyboardEvent) => { if (e.key === 'Escape') onOpenChange(false) }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [open, onOpenChange])

  const handleSave = async () => {
    if (!nombre.trim() || guardando) return
    setGuardando(true)
    setError(null)
    const descVal = parseFloat(descuentoPct)
    const tarifaVal = parseFloat(tarifaKg)
    const saldoVal = parseFloat(saldoInicial)
    try {
      await onSave({
        id: initial?.id,
        nombre: nombre.trim(),
        telefono: telefono.trim() || undefined,
        detalles: detalles.trim() || undefined,
        descuentoPct: !isNaN(descVal) ? descVal : 0,
        tarifaKg: !isNaN(tarifaVal) && tarifaVal > 0 ? tarifaVal : null,
        tarifaMoneda,
        saldoInicial: !isNaN(saldoVal) ? saldoVal : 0,
        fechaInicioCuenta: fechaInicioCuenta ? fechaInicioCuenta : null,
      })
      onOpenChange(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar')
    } finally {
      setGuardando(false)
    }
  }

  if (!open) return null

  const isEdit = !!initial

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]" onClick={() => onOpenChange(false)} />
      <div className="relative z-10 w-full max-w-md bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden max-h-full">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <User className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <h2 className="text-sm font-semibold text-[#022F40]">
              {isEdit ? 'Editar cliente' : 'Nuevo cliente'}
            </h2>
          </div>
          <button
            onClick={() => onOpenChange(false)}
            className="w-7 h-7 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] hover:text-[#022F40] transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="px-5 py-5 flex flex-col gap-4 flex-1 min-h-0 overflow-y-auto">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
              {error}
            </div>
          )}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Nombre *</label>
            <Input
              value={nombre}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setNombre(e.target.value)}
              placeholder="Ej. Juan Pérez o Constructora Norte SA"
              autoFocus
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Teléfono</label>
            <Input
              value={telefono}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setTelefono(e.target.value)}
              placeholder="Opcional"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Descuento sobre la lista (%)</label>
            <Input
              type="number"
              step="0.01"
              min="0"
              max="99.99"
              value={descuentoPct}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setDescuentoPct(e.target.value)}
              placeholder="0"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Tarifa por kg para placas y platinas (sin IVA)</label>
            <div className="flex gap-2">
              <Input
                type="number"
                step="0.0001"
                min="0"
                value={tarifaKg}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setTarifaKg(e.target.value)}
                placeholder="Opcional"
                className="flex-1"
              />
              <select
                value={tarifaMoneda}
                onChange={e => setTarifaMoneda(e.target.value as MonedaTarifa)}
                className="h-9 rounded border border-[#E5E7EB] bg-white px-2.5 text-sm text-[#022F40] outline-none focus:border-[#022F40]"
              >
                <option value="USD">USD</option>
                <option value="NIO">C$</option>
              </select>
            </div>
            <span className="text-sm text-[#80727B]">
              Si lo dejas vacío se usa la de Configuración. En USD se convierte con el tipo de cambio configurado. Al facturar puedes usar otra tarifa sin cambiar esta.
            </span>
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Saldo inicial (C$)</label>
            <Input
              type="number"
              step="0.01"
              value={saldoInicial}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setSaldoInicial(e.target.value)}
              placeholder="0"
            />
            <span className="text-sm text-[#80727B]">
              Lo que debía al empezar a usar el sistema. Si tenía saldo a favor, escríbelo en negativo.
            </span>
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">La cuenta empieza el</label>
            <Input
              type="date"
              value={fechaInicioCuenta}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFechaInicioCuenta(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Notas</label>
            <textarea
              value={detalles}
              onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setDetalles(e.target.value)}
              placeholder="Información adicional sobre el cliente…"
              rows={3}
              className="w-full rounded border border-[#E5E7EB] px-3 py-2 text-sm text-[#022F40] placeholder:text-[#80727B] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 resize-none"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-2 px-5 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] shrink-0">
          <Button variant="outline" onClick={() => onOpenChange(false)} className="rounded border-[#E5E7EB] text-sm">
            Cancelar
          </Button>
          <Button
            onClick={handleSave}
            disabled={guardando || !nombre.trim()}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm"
          >
            {isEdit ? 'Guardar cambios' : 'Crear cliente'}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
