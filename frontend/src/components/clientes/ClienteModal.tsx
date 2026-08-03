import { useState, useEffect, type ChangeEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Cliente } from '@/types'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { User, X } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  initial?: Cliente | null
  onSave: (cliente: Omit<Cliente, 'id'> & { id?: string }) => void
}

export function ClienteModal({ open, onOpenChange, initial, onSave }: Props) {
  const [nombre,   setNombre]   = useState('')
  const [telefono, setTelefono] = useState('')
  const [detalles, setDetalles] = useState('')

  useEffect(() => {
    if (open) {
      setNombre(initial?.nombre   ?? '')
      setTelefono(initial?.telefono ?? '')
      setDetalles(initial?.detalles ?? '')
    }
  }, [open, initial])

  useEffect(() => {
    if (!open) return
    const h = (e: KeyboardEvent) => { if (e.key === 'Escape') onOpenChange(false) }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [open, onOpenChange])

  const handleSave = () => {
    if (!nombre.trim()) return
    onSave({ id: initial?.id, nombre: nombre.trim(), telefono: telefono.trim() || undefined, detalles: detalles.trim() || undefined })
    onOpenChange(false)
  }

  if (!open) return null

  const isEdit = !!initial

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]" onClick={() => onOpenChange(false)} />
      <div className="relative z-10 w-full max-w-md bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E5E7EB]">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <User className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <h2 className="text-sm font-semibold text-[#022F40]">
              {isEdit ? 'Editar Cliente' : 'Nuevo Cliente'}
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
        <div className="px-5 py-5 flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56]">Nombre *</label>
            <Input
              value={nombre}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setNombre(e.target.value)}
              placeholder="Ej. Juan Pérez o Constructora Norte SA"
              autoFocus
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56]">Teléfono</label>
            <Input
              value={telefono}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setTelefono(e.target.value)}
              placeholder="Opcional"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56]">Notas</label>
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
        <div className="flex justify-end gap-2 px-5 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB]">
          <Button variant="outline" onClick={() => onOpenChange(false)} className="rounded border-[#E5E7EB] text-sm h-9">
            Cancelar
          </Button>
          <Button
            onClick={handleSave}
            disabled={!nombre.trim()}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9"
          >
            {isEdit ? 'Guardar Cambios' : 'Crear Cliente'}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
