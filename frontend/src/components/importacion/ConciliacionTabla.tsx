import type { PorConciliar } from '@/types'

interface Props {
  items: PorConciliar[]
  decisiones: Record<string, string>
  onChange: (descripcion: string, productoId: string) => void
}

export function ConciliacionTabla({ items, decisiones, onChange }: Props) {
  if (items.length === 0) return null

  return (
    <div className="flex flex-col gap-3">
      <div>
        <h3 className="text-sm font-semibold text-[#022F40]">
          Conciliación de productos ({items.length} pendientes)
        </h3>
        <p className="text-sm text-[#705D56] mt-0.5">
          Si no eliges nada, la línea se guarda con su descripción y su precio.
        </p>
      </div>

      <div className="border border-[#E5E7EB] rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
              <th className="text-left px-3 py-2 font-medium text-[#705D56]">Descripción</th>
              <th className="text-center px-3 py-2 font-medium text-[#705D56] w-20">Filas</th>
              <th className="text-left px-3 py-2 font-medium text-[#705D56]">Asignar a producto</th>
            </tr>
          </thead>
          <tbody>
            {items.map(item => {
              const valorActual = decisiones[item.descripcion] ?? item.propuestaProductoId
              return (
                <tr key={item.descripcion} className="border-b border-[#E5E7EB] last:border-b-0">
                  <td className="px-3 py-2 font-medium text-[#022F40]">{item.descripcion}</td>
                  <td className="px-3 py-2 text-center text-[#80727B]">{item.filas}</td>
                  <td className="px-3 py-2">
                    <select
                      value={valorActual}
                      onChange={e => onChange(item.descripcion, e.target.value)}
                      className="w-full max-w-md rounded border border-[#E5E7EB] bg-white px-2 text-sm text-[#022F40] outline-none focus:border-[#022F40]"
                    >
                      <option value={item.propuestaProductoId}>
                        {item.propuestaNombre} (propuesta genérica)
                      </option>
                      {item.candidatos.map(c => (
                        <option key={c.id} value={c.id}>
                          {c.codigo} — {c.nombre}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
