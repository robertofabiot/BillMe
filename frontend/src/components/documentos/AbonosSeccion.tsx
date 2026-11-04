import { useState, type FormEvent } from 'react'
import type { Documento } from '@/types'
import { documentoService } from '@/services/documentoService'
import { formatCurrency, formatDate, hoyIso } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Banknote } from 'lucide-react'

interface Props {
  factura: Documento
  onRegistrado: () => void
}

export function AbonosSeccion({ factura, onRegistrado }: Props) {
  const [monto, setMonto] = useState('')
  const [fecha, setFecha] = useState(hoyIso())
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const registrar = async (e: FormEvent) => {
    e.preventDefault()
    setGuardando(true)
    setError(null)
    try {
      await documentoService.registrarAbono(factura.id, { monto: Number(monto), fechaPago: fecha })
      setMonto('')
      onRegistrado()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo registrar el abono')
    } finally {
      setGuardando(false)
    }
  }

  const saldo = factura.saldoPendiente ?? 0
  const puedeAbonar = factura.estado === 'ACTIVA' && saldo > 0

  return (
    <section className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 flex flex-col gap-3">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-sm font-semibold text-[#022F40] flex items-center gap-2">
          <Banknote className="w-4 h-4" /> Abonos
        </h2>
        <p className="text-sm text-[#705D56]">
          Saldo: <span className="font-bold text-[#022F40]">{formatCurrency(saldo)}</span>
        </p>
      </div>

      {factura.abonos.length > 0 && (
        <ul className="text-sm divide-y divide-[#E5E7EB] border border-[#E5E7EB] rounded">
          {factura.abonos.map(a => (
            <li key={a.id} className="flex justify-between px-3 py-2">
              <span className="text-[#705D56]">{formatDate(a.fechaPago)}</span>
              <span className="font-medium text-[#022F40]">{formatCurrency(a.monto)}</span>
            </li>
          ))}
        </ul>
      )}

      {puedeAbonar && (
        <form onSubmit={registrar} className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="abono-monto" className="text-xs font-medium text-[#705D56]">Monto (C$)</label>
            <Input id="abono-monto" type="number" min="0" step="any" value={monto} onChange={e => setMonto(e.target.value)} className="w-40" />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="abono-fecha" className="text-xs font-medium text-[#705D56]">Fecha</label>
            <Input id="abono-fecha" type="date" value={fecha} onChange={e => setFecha(e.target.value)} className="w-44" />
          </div>
          <Button type="submit" variant="outline" disabled={guardando || !(Number(monto) > 0) || !fecha}>
            {guardando ? 'Registrando…' : 'Registrar abono'}
          </Button>
        </form>
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </section>
  )
}
