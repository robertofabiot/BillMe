import { useState, useEffect, useRef, type ChangeEvent, type KeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Producto } from '@/types'
import type { ProductoPayload } from '@/services/productoService'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Package, X, Plus, Tag } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  initial?: Producto | null
  onSave: (producto: ProductoPayload & { id?: string }) => Promise<void>
}

export function ProductoModal({ open, onOpenChange, initial, onSave }: Props) {
  const [codigo,    setCodigo]   = useState('')
  const [nombre,    setNombre]   = useState('')
  const [desc,      setDesc]     = useState('')
  const [peso,      setPeso]     = useState('')
  const [aliases,   setAliases]  = useState<string[]>([])
  const [aliasIn,   setAliasIn]  = useState('')

  const [unidad,    setUnidad]   = useState('UND')
  const [error,     setError]    = useState<string | null>(null)
  const [guardando, setGuardando]= useState(false)

  const aliasRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (open) {
      setCodigo(initial?.codigoInterno   ?? '')
      setNombre(initial?.nombrePrincipal ?? '')
      setDesc(initial?.descripcion       ?? '')
      setPeso(initial?.peso != null ? String(initial.peso) : '')
      setAliases(initial?.aliases        ?? [])
      setAliasIn('')

      setUnidad(initial?.unidad ?? 'UND')
      setError(null)
      setGuardando(false)
    }
  }, [open, initial])

  useEffect(() => {
    if (!open) return
    const h = (e: globalThis.KeyboardEvent) => { if (e.key === 'Escape') onOpenChange(false) }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [open, onOpenChange])

  // ── Alias handlers ──
  const addAlias = () => {
    const val = aliasIn.trim()
    if (!val || aliases.includes(val)) return
    setAliases(prev => [...prev, val])
    setAliasIn('')
    aliasRef.current?.focus()
  }
  const removeAlias = (a: string) => setAliases(prev => prev.filter(x => x !== a))
  const handleAliasKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter')     { e.preventDefault(); addAlias() }
    if (e.key === 'Backspace' && !aliasIn && aliases.length) setAliases(prev => prev.slice(0, -1))
  }

  const handleSave = async () => {
    if (!codigo.trim() || !nombre.trim() || guardando) return
    setGuardando(true)
    setError(null)
    try {
      await onSave({
        id: initial?.id,
        codigoInterno:   codigo.trim(),
        nombrePrincipal: nombre.trim(),
        descripcion:     desc.trim() || undefined,
        peso:            parseFloat(peso) || 0,
        unidad:          unidad.trim() || 'UND',
        aliases,
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

      <div className="relative z-10 w-full max-w-lg bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden max-h-full">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E5E7EB] shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <Package className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <h2 className="text-sm font-semibold text-[#022F40]">
              {isEdit ? 'Editar producto' : 'Nuevo producto'}
            </h2>
          </div>
          <button onClick={() => onOpenChange(false)} className="w-7 h-7 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B] transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 min-h-0 overflow-y-auto px-5 py-5 flex flex-col gap-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
              {error}
            </div>
          )}

          {/* Código + Peso */}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-[#705D56]">Código Interno *</label>
              <Input value={codigo} onChange={(e: ChangeEvent<HTMLInputElement>) => setCodigo(e.target.value.toUpperCase())} placeholder="Ej. TUB-001" className="font-mono" autoFocus />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-[#705D56]">Peso (kg)</label>
              <Input type="number" min={0} step={0.01} value={peso} onChange={(e: ChangeEvent<HTMLInputElement>) => setPeso(e.target.value)} placeholder="0.00" />
            </div>
          </div>

          {/* Nombre */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Nombre Principal *</label>
            <Input value={nombre} onChange={(e: ChangeEvent<HTMLInputElement>) => setNombre(e.target.value)} placeholder='Ej. Tubo Galvanizado 1/2"' />
          </div>

          {/* Unidad */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Unidad</label>
            <Input value={unidad} onChange={(e: ChangeEvent<HTMLInputElement>) => setUnidad(e.target.value.toUpperCase())} placeholder="UND, KG, METRO…" />
            <p className="text-sm text-[#80727B]">El precio de lista se registra en la ficha del producto, con su fecha.</p>
          </div>

          {/* Descripción */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Descripción</label>
            <textarea value={desc} onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setDesc(e.target.value)} placeholder="Opcional — especificaciones técnicas…" rows={2}
              className="w-full rounded border border-[#E5E7EB] px-3 py-2 text-sm text-[#022F40] placeholder:text-[#80727B] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 resize-none" />
          </div>

          {/* Alias */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56] flex items-center gap-1">
              <Tag className="w-3 h-3" /> Alias / Nombres Alternativos
            </label>
            <div
              className="min-h-[38px] w-full rounded border border-[#E5E7EB] px-2 py-1.5 flex flex-wrap gap-1.5 items-center cursor-text focus-within:border-[#022F40] focus-within:ring-2 focus-within:ring-[#022F40]/10 transition-all"
              onClick={() => aliasRef.current?.focus()}
            >
              {aliases.map(a => (
                <span key={a} className="inline-flex items-center gap-1 bg-[#022F40]/8 text-[#022F40] text-sm px-2 py-0.5 rounded border border-[#022F40]/15 font-medium">
                  {a}
                  <button type="button" onClick={(e) => { e.stopPropagation(); removeAlias(a) }} className="text-[#80727B] hover:text-[#022F40]">
                    <X className="w-2.5 h-2.5" />
                  </button>
                </span>
              ))}
              <input ref={aliasRef} value={aliasIn}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setAliasIn(e.target.value)}
                onKeyDown={handleAliasKey}
                placeholder={aliases.length === 0 ? 'Escribe y presiona Enter…' : ''}
                className="flex-1 min-w-[120px] text-sm text-[#022F40] placeholder:text-[#80727B] outline-none bg-transparent py-0.5" />
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end px-5 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] shrink-0 gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} className="rounded border-[#E5E7EB] text-sm">Cancelar</Button>
          <Button onClick={handleSave} disabled={guardando || !codigo.trim() || !nombre.trim()}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-1">
            <Plus className="w-3.5 h-3.5" />
            {isEdit ? 'Guardar cambios' : 'Crear producto'}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
