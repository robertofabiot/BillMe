import { useState, useEffect, type ChangeEvent } from 'react'
import type { Abono, Proyecto } from '@/types'
import { abonoService } from '@/services/abonoService'
import { hoyIso } from '@/lib/format'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  clienteId: string
  proyectos: Proyecto[]
  initial?: Abono | null
  onGuardado: () => void
}

export function AbonoDialog({
  open,
  onOpenChange,
  clienteId,
  proyectos,
  initial,
  onGuardado,
}: Props) {
  const [monto, setMonto] = useState('')
  const [fechaPago, setFechaPago] = useState(hoyIso())
  const [proyectoId, setProyectoId] = useState('')
  const [nota, setNota] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    if (open) {
      setError(null)
      setGuardando(false)
      if (initial) {
        setMonto(String(initial.monto))
        setFechaPago(initial.fechaPago)
        setProyectoId(initial.proyectoId ?? '')
        setNota(initial.nota ?? '')
      } else {
        setMonto('')
        setFechaPago(hoyIso())
        setProyectoId('')
        setNota('')
      }
    }
  }, [open, initial])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const montoVal = parseFloat(monto)
    if (isNaN(montoVal) || montoVal <= 0) {
      setError('El monto debe ser mayor a 0')
      return
    }

    if (!fechaPago) {
      setError('La fecha es obligatoria')
      return
    }

    setGuardando(true)
    try {
      const payload = {
        clienteId,
        proyectoId: proyectoId.trim() ? proyectoId.trim() : null,
        monto: montoVal,
        fechaPago,
        nota: nota.trim() ? nota.trim() : null,
      }

      if (initial?.id) {
        await abonoService.actualizar(initial.id, payload)
      } else {
        await abonoService.crear(payload)
      }

      onGuardado()
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar el abono')
    } finally {
      setGuardando(false)
    }
  }

  const isEdit = !!initial?.id

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-sm font-semibold text-[#022F40]">
            {isEdit ? 'Editar abono' : 'Registrar abono'}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4 py-2">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
              {error}
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Monto (C$) *</label>
            <Input
              type="number"
              step="0.01"
              min="0.01"
              value={monto}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setMonto(e.target.value)}
              placeholder="0.00"
              autoFocus
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Fecha *</label>
            <Input
              type="date"
              value={fechaPago}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFechaPago(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Proyecto</label>
            <select
              value={proyectoId}
              onChange={(e: ChangeEvent<HTMLSelectElement>) => setProyectoId(e.target.value)}
              className="w-full rounded border border-[#E5E7EB] bg-white px-3 py-2 text-sm text-[#022F40] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10"
            >
              <option value="">Sin proyecto</option>
              {proyectos.map(p => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Nota</label>
            <Input
              value={nota}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setNota(e.target.value)}
              placeholder="Opcional (banco, cheque, recibo…)"
              maxLength={500}
            />
          </div>

          <DialogFooter className="mt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="rounded border-[#E5E7EB] text-sm"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={guardando}
              className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm"
            >
              {guardando ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Registrar abono'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
