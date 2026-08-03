import { useState, useEffect, type ChangeEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Proyecto } from '@/types'
import { CLIENTES } from '@/data/mock'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { FolderGit2, X, Plus, Building2 } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  initial?: Proyecto | null
  onSave: (proyecto: Omit<Proyecto, 'id'> & { id?: string }) => void
  defaultClienteId?: string
}

export function ProyectoModal({ open, onOpenChange, initial, onSave, defaultClienteId }: Props) {
  const [nombre,       setNombre]       = useState('')
  const [clienteId,    setClienteId]    = useState('')
  const [estado,       setEstado]       = useState<Proyecto['estado']>('ACTIVO')

  useEffect(() => {
    if (open) {
      setNombre(initial?.nombre ?? '')
      setClienteId(initial?.clienteId ?? defaultClienteId ?? '')
      setEstado(initial?.estado ?? 'ACTIVO')
    }
  }, [open, initial, defaultClienteId])

  useEffect(() => {
    if (!open) return
    const h = (e: globalThis.KeyboardEvent) => { if (e.key === 'Escape') onOpenChange(false) }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [open, onOpenChange])

  const handleSave = () => {
    if (!nombre.trim() || !clienteId) return
    onSave({
      id: initial?.id,
      clienteId,
      nombre: nombre.trim(),
      estado,
    })
    onOpenChange(false)
  }

  if (!open) return null
  const isEdit = !!initial

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]" onClick={() => onOpenChange(false)} />

      <div className="relative z-10 w-full max-w-sm bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <FolderGit2 className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <h2 className="text-sm font-semibold text-[#022F40]">
              {isEdit ? 'Editar Proyecto' : 'Nuevo Proyecto'}
            </h2>
          </div>
          <button onClick={() => onOpenChange(false)} className="w-7 h-7 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="px-5 py-5 flex flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56]">Nombre del Proyecto *</label>
            <Input value={nombre} onChange={(e: ChangeEvent<HTMLInputElement>) => setNombre(e.target.value)} placeholder="Ej. Remodelación Casa Club" autoFocus />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56] flex items-center gap-1.5">
              <Building2 className="w-3 h-3 text-[#80727B]" /> Cliente *
            </label>
            <select
              value={clienteId}
              onChange={e => setClienteId(e.target.value)}
              className="h-9 w-full rounded border border-[#E5E7EB] bg-white px-3 py-1 text-sm text-[#022F40] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 transition-all"
            >
              <option value="" disabled>Selecciona un cliente</option>
              {CLIENTES.map(c => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>
          </div>

          {isEdit && (
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#705D56]">Estado</label>
              <select
                value={estado}
                onChange={e => setEstado(e.target.value as Proyecto['estado'])}
                className="h-9 w-full rounded border border-[#E5E7EB] bg-white px-3 py-1 text-sm text-[#022F40] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 transition-all"
              >
                <option value="ACTIVO">Activo</option>
                <option value="FINALIZADO">Finalizado</option>
              </select>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end px-5 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] shrink-0 gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} className="rounded border-[#E5E7EB] text-sm h-9">Cancelar</Button>
          <Button onClick={handleSave} disabled={!nombre.trim() || !clienteId}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-1">
            <Plus className="w-3.5 h-3.5" />
            {isEdit ? 'Guardar Cambios' : 'Crear Proyecto'}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
