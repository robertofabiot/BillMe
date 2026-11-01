import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import type { Producto } from '@/types'
import { precioListaService, type PrecioListaPayload } from '@/services/precioListaService'
import { formatCurrency } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Calculator, X } from 'lucide-react'

type Fuente = PrecioListaPayload['fuente']

const OPCIONES: { fuente: Fuente; titulo: string; precio: string; descuento?: string }[] = [
  {
    fuente: 'FACTURA_PROVEEDOR',
    titulo: 'Desde la factura del proveedor',
    precio: 'Precio que facturó el proveedor, sin IVA (C$)',
    descuento: 'Descuento que dio el proveedor (%)',
  },
  {
    fuente: 'PRECIO_CLIENTE',
    titulo: 'Desde el precio dado a un cliente',
    precio: 'Precio que se le dio al cliente, con IVA (C$)',
    descuento: 'Descuento de ese cliente sobre la lista (%)',
  },
  {
    fuente: 'MANUAL',
    titulo: 'Escribir el precio de lista',
    precio: 'Precio de lista, sin IVA (C$)',
  },
]

const hoy = () => new Date().toISOString().slice(0, 10)

interface Props {
  producto: Producto
  descuentoSugerido: number | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onGuardado: () => void
}

export function CalcularListaDialog({ producto, descuentoSugerido, open, onOpenChange, onGuardado }: Props) {
  const [fuente, setFuente] = useState<Fuente>('FACTURA_PROVEEDOR')
  const [precio, setPrecio] = useState('')
  const [descuento, setDescuento] = useState('')
  const [fecha, setFecha] = useState(hoy())
  const [nota, setNota] = useState('')
  const [resultado, setResultado] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const opcion = OPCIONES.find(o => o.fuente === fuente)!

  useEffect(() => {
    if (!open) return
    setFuente('FACTURA_PROVEEDOR')
    setPrecio('')
    setDescuento(descuentoSugerido != null ? String(descuentoSugerido) : '')
    setFecha(hoy())
    setNota('')
    setError(null)
  }, [open, descuentoSugerido])

  const payload = (): PrecioListaPayload => ({
    fuente,
    precio: Number(precio),
    descuentoPct: opcion.descuento && descuento ? Number(descuento) : undefined,
    fecha,
    nota: nota.trim() || undefined,
  })

  // Vista previa: el cálculo lo hace siempre el backend
  useEffect(() => {
    const listo = Number(precio) > 0
    const timer = setTimeout(() => {
      if (!open || !listo) { setResultado(null); return }
      precioListaService.calcular(producto.id, payload())
        .then(r => { setResultado(r.precioSinIva); setError(null) })
        .catch(e => { setResultado(null); setError(e instanceof Error ? e.message : 'No se pudo calcular') })
    }, 300)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, fuente, precio, descuento])

  const guardar = async () => {
    setGuardando(true)
    try {
      await precioListaService.registrar(producto.id, payload())
      onGuardado()
      onOpenChange(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar')
    } finally {
      setGuardando(false)
    }
  }

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]" onClick={() => onOpenChange(false)} />

      <div className="relative z-10 w-full max-w-md bg-white rounded border border-[#E5E7EB] shadow-sm flex flex-col overflow-hidden max-h-[calc(100vh-4rem)]">
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E5E7EB]">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
              <Calculator className="w-3.5 h-3.5 text-[#91E5F6]" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-[#022F40]">Calcular precio de lista</h2>
              <p className="text-xs text-[#80727B]">{producto.nombrePrincipal}</p>
            </div>
          </div>
          <button onClick={() => onOpenChange(false)} className="w-7 h-7 flex items-center justify-center rounded hover:bg-[#F9FAFB] text-[#80727B]">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="px-5 py-5 flex flex-col gap-4 overflow-y-auto">
          <div className="flex flex-col gap-2">
            {OPCIONES.map(o => (
              <label key={o.fuente} className={`flex items-center gap-2 border rounded px-3 py-2.5 text-sm cursor-pointer ${fuente === o.fuente ? 'border-[#022F40] bg-[#022F40]/5 text-[#022F40] font-medium' : 'border-[#E5E7EB] text-[#705D56]'}`}>
                <input type="radio" name="fuente" checked={fuente === o.fuente} onChange={() => setFuente(o.fuente)} />
                {o.titulo}
              </label>
            ))}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="precio" className="text-xs font-medium text-[#705D56]">{opcion.precio}</label>
            <Input id="precio" type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} autoFocus />
          </div>

          {opcion.descuento && (
            <div className="flex flex-col gap-1.5">
              <label htmlFor="descuento" className="text-xs font-medium text-[#705D56]">{opcion.descuento}</label>
              <Input id="descuento" type="number" min="0" max="99.99" step="0.01" value={descuento} onChange={e => setDescuento(e.target.value)} />
              {fuente === 'FACTURA_PROVEEDOR' && descuentoSugerido != null && (
                <p className="text-[11px] text-[#80727B]">Sugerido: {descuentoSugerido}% (el último usado en esta familia).</p>
              )}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="fecha" className="text-xs font-medium text-[#705D56]">Vigente desde</label>
              <Input id="fecha" type="date" value={fecha} onChange={e => setFecha(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label htmlFor="nota" className="text-xs font-medium text-[#705D56]">Nota (opcional)</label>
              <Input id="nota" value={nota} onChange={e => setNota(e.target.value)} placeholder="Ej. factura 1234" />
            </div>
          </div>

          <div className="border border-[#E5E7EB] bg-[#F9FAFB] rounded px-4 py-3">
            <p className="text-xs text-[#705D56]">Precio de lista (sin IVA)</p>
            <p className="text-2xl font-bold text-[#022F40]">{resultado != null ? formatCurrency(resultado) : '—'}</p>
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
        </div>

        <div className="flex justify-end gap-2 px-5 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB]">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancelar</Button>
          <Button onClick={guardar} disabled={resultado == null || guardando} className="bg-[#022F40] hover:bg-[#022F40]/90 text-white">
            {guardando ? 'Guardando…' : 'Guardar precio de lista'}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
