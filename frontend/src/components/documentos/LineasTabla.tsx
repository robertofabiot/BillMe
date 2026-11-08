import { formatCurrency } from '@/lib/format'
import { calcularLinea, formatearTagPrecio, type LineaForm } from './lineaForm'
import { Input } from '@/components/ui/input'
import { Calculator, Tag, X } from 'lucide-react'

interface Props {
  lineas: LineaForm[]
  verEntrega: boolean
  soloLectura: boolean
  /** La línea recién agregada recibe el cursor. */
  claveNueva: string | null
  onCambiar: (clave: string, cambios: Partial<LineaForm>) => void
  onCambiarPeso: (clave: string, peso: string) => void
  onQuitar: (clave: string) => void
  onAbrirPrecio?: (clave: string) => void
  onAbrirCalculadora?: (clave: string) => void
  onBlurDescripcion?: (clave: string) => void
}

const CELDA = 'h-9 px-2 text-sm'

export function LineasTabla({
  lineas,
  verEntrega,
  soloLectura,
  claveNueva,
  onCambiar,
  onCambiarPeso,
  onQuitar,
  onAbrirPrecio,
  onAbrirCalculadora,
  onBlurDescripcion,
}: Props) {
  if (lineas.length === 0) {
    return (
      <p className="text-sm text-[#80727B] border border-dashed border-[#E5E7EB] rounded px-4 py-8 text-center">
        Todavía no hay líneas. Busca un producto arriba para agregarlo.
      </p>
    )
  }

  return (
    <>
      {/* Vista de tarjetas en móvil (< 768px) */}
      <div className="md:hidden flex flex-col gap-3">
        {lineas.map((l, i) => {
          const { total, pesoTotal } = calcularLinea(l)
          const tag = formatearTagPrecio(l)
          return (
            <div key={l.clave} className="bg-white border border-[#E5E7EB] rounded p-4 flex flex-col gap-3 shadow-xs">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-sm text-[#022F40]">Línea {i + 1}</span>
                {!soloLectura && (
                  <button
                    type="button"
                    aria-label={`Quitar la línea ${i + 1}`}
                    onClick={() => onQuitar(l.clave)}
                    className="w-11 h-11 flex items-center justify-center rounded text-[#80727B] hover:text-red-600 hover:bg-red-50"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-[#705D56] mb-1">Descripción</label>
                <Input
                  aria-label={`Descripción de la línea ${i + 1}`}
                  value={l.descripcion}
                  maxLength={500}
                  placeholder={l.productoGenerico ? 'Escribe la descripción…' : undefined}
                  onChange={e => onCambiar(l.clave, { descripcion: e.target.value })}
                  onBlur={() => onBlurDescripcion?.(l.clave)}
                  className="h-10 text-sm"
                />
                {!l.productoGenerico && <p className="text-sm text-[#80727B] font-mono mt-1 px-1">{l.productoCodigo}</p>}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-[#705D56] mb-1">Cantidad</label>
                  <Input
                    aria-label={`Cantidad de la línea ${i + 1}`}
                    type="number" min="0" step="any"
                    autoFocus={l.clave === claveNueva}
                    value={l.cantidad}
                    onChange={e => onCambiar(l.clave, { cantidad: e.target.value })}
                    className="h-10 text-sm"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#705D56] mb-1">Peso unit. (kg)</label>
                  <div className="flex items-center gap-1">
                    <Input
                      aria-label={`Peso unitario de la línea ${i + 1}`}
                      type="number" min="0" step="any"
                      value={l.pesoUnitKg}
                      onChange={e => onCambiarPeso(l.clave, e.target.value)}
                      className="h-10 text-sm"
                    />
                    {!soloLectura && onAbrirCalculadora && (
                      <button
                        type="button"
                        aria-label={`Calcular peso de la línea ${i + 1}`}
                        onClick={() => onAbrirCalculadora(l.clave)}
                        className="w-10 h-10 flex-shrink-0 flex items-center justify-center rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] text-[#705D56]"
                        title="Calcular peso"
                      >
                        <Calculator className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 items-center text-sm">
                <div>
                  <span className="text-[#705D56]">Peso total:</span>{' '}
                  <span className="font-medium text-[#022F40]">{pesoTotal === null ? '—' : `${pesoTotal.toFixed(2)} kg`}</span>
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#705D56] mb-1">Precio unit. C$</label>
                  <div className="flex items-center gap-1">
                    <Input
                      aria-label={`Precio unitario de la línea ${i + 1}`}
                      type="number" min="0" step="any"
                      value={l.precioUnit}
                      onChange={e => onCambiar(l.clave, { precioUnit: e.target.value, metodoPrecio: 'MANUAL', parametros: null })}
                      className="h-10 text-sm"
                    />
                    {!soloLectura && onAbrirPrecio && (
                      <button
                        type="button"
                        aria-label={`Elegir precio de la línea ${i + 1}`}
                        onClick={() => onAbrirPrecio(l.clave)}
                        className="w-10 h-10 flex-shrink-0 flex items-center justify-center rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] text-[#705D56]"
                        title="Elegir precio"
                      >
                        <Tag className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                  {tag && (
                    <div className="mt-1 flex flex-col text-sm leading-tight text-[#705D56]">
                      <span>{tag.texto}</span>
                      {tag.viejo && (
                        <span className="text-amber-600 font-medium">
                          Tiene {tag.dias} días: revísalo
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>

              {verEntrega && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-[#705D56] mb-1">Fecha de entrega</label>
                    <Input
                      aria-label={`Fecha de entrega de la línea ${i + 1}`}
                      type="date"
                      value={l.fechaEntrega}
                      onChange={e => onCambiar(l.clave, { fechaEntrega: e.target.value })}
                      className="h-10 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#705D56] mb-1">Lugar de entrega</label>
                    <Input
                      aria-label={`Lugar de entrega de la línea ${i + 1}`}
                      value={l.lugarEntrega}
                      maxLength={200}
                      onChange={e => onCambiar(l.clave, { lugarEntrega: e.target.value })}
                      className="h-10 text-sm"
                    />
                  </div>
                </div>
              )}

              <div className="flex items-center justify-between pt-2 border-t border-[#E5E7EB]">
                <span className="text-sm font-medium text-[#705D56]">Total:</span>
                <span className="text-base font-bold text-[#022F40]">{formatCurrency(total)}</span>
              </div>
            </div>
          )
        })}
      </div>

      {/* Vista de tabla en escritorio (>= 768px) */}
      <div className="hidden md:block border border-[#E5E7EB] rounded overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB] text-sm text-[#705D56]">
              <th className="text-left font-medium px-2 py-2 w-20">Cantidad</th>
              <th className="text-left font-medium px-2 py-2 min-w-[260px]">Descripción</th>
              <th className="text-left font-medium px-2 py-2 w-32">Peso unit. (kg)</th>
              <th className="text-right font-medium px-2 py-2 w-24">Peso total (kg)</th>
              <th className="text-left font-medium px-2 py-2 w-36">Precio unit. C$</th>
              <th className="text-right font-medium px-2 py-2 w-28">Total C$</th>
              {verEntrega && <th className="text-left font-medium px-2 py-2 w-36">Fecha de entrega</th>}
              {verEntrega && <th className="text-left font-medium px-2 py-2 w-40">Lugar de entrega</th>}
              {!soloLectura && <th className="w-9" />}
            </tr>
          </thead>
          <tbody>
            {lineas.map((l, i) => {
              const { total, pesoTotal } = calcularLinea(l)
              const tag = formatearTagPrecio(l)
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
                      onBlur={() => onBlurDescripcion?.(l.clave)}
                      className={CELDA}
                    />
                    {!l.productoGenerico && <p className="text-sm text-[#80727B] font-mono mt-0.5 px-1">{l.productoCodigo}</p>}
                  </td>
                  <td className="px-2 py-1.5">
                    <div className="flex items-center gap-1">
                      <Input
                        aria-label={`Peso unitario de la línea ${i + 1}`}
                        type="number" min="0" step="any"
                        value={l.pesoUnitKg}
                        onChange={e => onCambiarPeso(l.clave, e.target.value)}
                        className={CELDA}
                      />
                      {!soloLectura && onAbrirCalculadora && (
                        <button
                          type="button"
                          aria-label={`Calcular peso de la línea ${i + 1}`}
                          onClick={() => onAbrirCalculadora(l.clave)}
                          className="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] text-[#705D56]"
                          title="Calcular peso"
                        >
                          <Calculator className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </td>
                  <td className="px-2 py-2.5 text-right text-[#705D56] whitespace-nowrap">
                    {pesoTotal === null ? '—' : pesoTotal.toFixed(2)}
                  </td>
                  <td className="px-2 py-1.5">
                    <div className="flex items-center gap-1">
                      <Input
                        aria-label={`Precio unitario de la línea ${i + 1}`}
                        type="number" min="0" step="any"
                        value={l.precioUnit}
                        onChange={e => onCambiar(l.clave, { precioUnit: e.target.value, metodoPrecio: 'MANUAL', parametros: null })}
                        className={CELDA}
                      />
                      {!soloLectura && onAbrirPrecio && (
                        <button
                          type="button"
                          aria-label={`Elegir precio de la línea ${i + 1}`}
                          onClick={() => onAbrirPrecio(l.clave)}
                          className="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] text-[#705D56]"
                          title="Elegir precio"
                        >
                          <Tag className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                    {tag && (
                      <div className="mt-1 flex flex-col text-sm leading-tight text-[#705D56]">
                        <span>{tag.texto}</span>
                        {tag.viejo && (
                          <span className="text-amber-600 font-medium">
                            Tiene {tag.dias} días: revísalo
                          </span>
                        )}
                      </div>
                    )}
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
    </>
  )
}
