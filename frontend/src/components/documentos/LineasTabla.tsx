import { formatCurrency } from '@/lib/format'
import { calcularLinea, type LineaForm } from './lineaForm'
import { Input } from '@/components/ui/input'
import { X } from 'lucide-react'

interface Props {
  lineas: LineaForm[]
  verEntrega: boolean
  soloLectura: boolean
  /** La línea recién agregada recibe el cursor. */
  claveNueva: string | null
  onCambiar: (clave: string, cambios: Partial<LineaForm>) => void
  onQuitar: (clave: string) => void
}

const CELDA = 'h-8 px-2 text-sm'

export function LineasTabla({ lineas, verEntrega, soloLectura, claveNueva, onCambiar, onQuitar }: Props) {
  if (lineas.length === 0) {
    return (
      <p className="text-sm text-[#80727B] border border-dashed border-[#E5E7EB] rounded px-4 py-8 text-center">
        Todavía no hay líneas. Busca un producto arriba para agregarlo.
      </p>
    )
  }

  return (
    <div className="border border-[#E5E7EB] rounded overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB] text-xs text-[#705D56]">
            <th className="text-left font-medium px-2 py-2 w-20">Cantidad</th>
            <th className="text-left font-medium px-2 py-2 min-w-[260px]">Descripción</th>
            <th className="text-left font-medium px-2 py-2 w-24">Peso unit. (kg)</th>
            <th className="text-right font-medium px-2 py-2 w-24">Peso total (kg)</th>
            <th className="text-left font-medium px-2 py-2 w-28">Precio unit. C$</th>
            <th className="text-right font-medium px-2 py-2 w-28">Total C$</th>
            {verEntrega && <th className="text-left font-medium px-2 py-2 w-36">Fecha de entrega</th>}
            {verEntrega && <th className="text-left font-medium px-2 py-2 w-40">Lugar de entrega</th>}
            {!soloLectura && <th className="w-9" />}
          </tr>
        </thead>
        <tbody>
          {lineas.map((l, i) => {
            const { total, pesoTotal } = calcularLinea(l)
            return (
              <tr key={l.clave} className="border-b border-[#E5E7EB] last:border-b-0 align-top">
                <td className="px-2 py-1.5">
                  <Input
                    aria-label={`Cantidad de la línea ${i + 1}`}
                    type="number" min="0" step="any"
                    autoFocus={l.clave === claveNueva}
                    value={l.cantidad}
                    onChange={e => onCambiar(l.clave, { cantidad: e.target.value })}
                    className={CELDA}
                  />
                </td>
                <td className="px-2 py-1.5">
                  <Input
                    aria-label={`Descripción de la línea ${i + 1}`}
                    value={l.descripcion}
                    maxLength={500}
                    placeholder={l.productoGenerico ? 'Escribe la descripción…' : undefined}
                    onChange={e => onCambiar(l.clave, { descripcion: e.target.value })}
                    className={CELDA}
                  />
                  {!l.productoGenerico && <p className="text-[11px] text-[#80727B] font-mono mt-0.5 px-1">{l.productoCodigo}</p>}
                </td>
                <td className="px-2 py-1.5">
                  <Input
                    aria-label={`Peso unitario de la línea ${i + 1}`}
                    type="number" min="0" step="any"
                    value={l.pesoUnitKg}
                    onChange={e => onCambiar(l.clave, { pesoUnitKg: e.target.value })}
                    className={CELDA}
                  />
                </td>
                <td className="px-2 py-2.5 text-right text-[#705D56] whitespace-nowrap">
                  {pesoTotal === null ? '—' : pesoTotal.toFixed(2)}
                </td>
                <td className="px-2 py-1.5">
                  <Input
                    aria-label={`Precio unitario de la línea ${i + 1}`}
                    type="number" min="0" step="any"
                    value={l.precioUnit}
                    onChange={e => onCambiar(l.clave, { precioUnit: e.target.value, metodoPrecio: 'MANUAL' })}
                    className={CELDA}
                  />
                </td>
                <td className="px-2 py-2.5 text-right font-semibold text-[#022F40] whitespace-nowrap">
                  {formatCurrency(total)}
                </td>
                {verEntrega && (
                  <td className="px-2 py-1.5">
                    <Input
                      aria-label={`Fecha de entrega de la línea ${i + 1}`}
                      type="date"
                      value={l.fechaEntrega}
                      onChange={e => onCambiar(l.clave, { fechaEntrega: e.target.value })}
                      className={CELDA}
                    />
                  </td>
                )}
                {verEntrega && (
                  <td className="px-2 py-1.5">
                    <Input
                      aria-label={`Lugar de entrega de la línea ${i + 1}`}
                      value={l.lugarEntrega}
                      maxLength={200}
                      onChange={e => onCambiar(l.clave, { lugarEntrega: e.target.value })}
                      className={CELDA}
                    />
                  </td>
                )}
                {!soloLectura && (
                  <td className="px-1 py-1.5">
                    <button
                      type="button"
                      aria-label={`Quitar la línea ${i + 1}`}
                      onClick={() => onQuitar(l.clave)}
                      className="w-8 h-8 flex items-center justify-center rounded text-[#80727B] hover:text-red-600 hover:bg-red-50"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
