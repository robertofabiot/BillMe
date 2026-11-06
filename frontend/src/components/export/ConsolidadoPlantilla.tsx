import { Fragment, forwardRef } from 'react'
import type { Consolidado } from '@/types'
import { formatCurrency, formatFechaCorta, formatUsd } from '@/lib/format'

export interface ConsolidadoPlantillaProps {
  consolidado: Consolidado
  encabezado: string
}

export const ConsolidadoPlantilla = forwardRef<HTMLDivElement, ConsolidadoPlantillaProps>(
  function ConsolidadoPlantilla({ consolidado, encabezado }, ref) {
    const conLugar = consolidado.grupos.some((g) =>
      g.lineas.some((l) => l.lugarEntrega && l.lugarEntrega.trim() !== '')
    )

    const tieneSaldo = consolidado.saldoAnterior !== 0
    const tieneAbonos = consolidado.totalAbonos !== 0

    return (
      <div
        ref={ref}
        className="p-8 font-sans"
        style={{
          width: conLugar ? '960px' : '760px',
          backgroundColor: '#FFFFFF',
          color: '#000000',
        }}
      >
        {/* Fila superior: encabezado a la izquierda, fecha a la derecha */}
        <div className="flex items-start justify-between gap-4 mb-4">
          <div className="font-bold text-lg leading-tight">{encabezado}</div>
          <div className="font-bold text-sm whitespace-nowrap">
            {formatFechaCorta(consolidado.hasta)}
          </div>
        </div>

        {/* Tabla estática */}
        <table className="w-full border-collapse border border-black text-sm mb-4">
          <thead>
            <tr>
              <th className="border border-black px-2 py-1 text-center font-bold w-16">Cantidad</th>
              <th className="border border-black px-2 py-1 text-left font-bold">Descripción</th>
              <th className="border border-black px-2 py-1 text-right font-bold w-24 whitespace-nowrap">
                Peso Unit (KG)
              </th>
              <th className="border border-black px-2 py-1 text-right font-bold w-24 whitespace-nowrap">
                Peso Total (KG)
              </th>
              <th className="border border-black px-2 py-1 text-right font-bold w-28 whitespace-nowrap">
                Precio Unitario C$
              </th>
              <th className="border border-black px-2 py-1 text-right font-bold w-28 whitespace-nowrap">
                Precio Total C$
              </th>
              {conLugar && (
                <th className="border border-black px-2 py-1 text-left font-bold w-32 whitespace-nowrap">
                  Lugar entrega
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {consolidado.grupos.map((grupo) => (
              <Fragment key={grupo.fechaEntrega}>
                {grupo.lineas.map((linea, lIdx) => {
                  const tienePeso = linea.pesoUnitKg != null
                  return (
                    <tr key={`${linea.documentoId}-${lIdx}`}>
                      <td className="border border-black px-2 py-1 text-center">
                        {linea.cantidad}
                      </td>
                      <td className="border border-black px-2 py-1 text-left">
                        {linea.descripcion}
                      </td>
                      <td className="border border-black px-2 py-1 text-right">
                        {tienePeso ? Number(linea.pesoUnitKg).toFixed(2) : ''}
                      </td>
                      <td className="border border-black px-2 py-1 text-right">
                        {tienePeso && linea.pesoTotalKg != null
                          ? Number(linea.pesoTotalKg).toFixed(2)
                          : tienePeso
                          ? (Number(linea.cantidad) * Number(linea.pesoUnitKg)).toFixed(2)
                          : ''}
                      </td>
                      <td className="border border-black px-2 py-1 text-right">
                        {formatCurrency(linea.precioUnit)}
                      </td>
                      <td className="border border-black px-2 py-1 text-right font-semibold">
                        {formatCurrency(linea.total)}
                      </td>
                      {conLugar && (
                        <td className="border border-black px-2 py-1 text-left">
                          {linea.lugarEntrega || ''}
                        </td>
                      )}
                    </tr>
                  )
                })}
                {/* Fila de subtotal por grupo */}
                <tr key={`sub-${grupo.fechaEntrega}`}>
                  <td className="border border-black px-2 py-1" />
                  <td className="border border-black px-2 py-1 text-left font-bold">
                    {grupo.nombre}
                  </td>
                  <td className="border border-black px-2 py-1" />
                  <td className="border border-black px-2 py-1 text-right font-bold">
                    {grupo.subtotalKg != null ? Number(grupo.subtotalKg).toFixed(2) : ''}
                  </td>
                  <td className="border border-black px-2 py-1" />
                  <td className="border border-black px-2 py-1 text-right font-bold">
                    {formatCurrency(grupo.subtotal)}
                  </td>
                  {conLugar && <td className="border border-black px-2 py-1" />}
                </tr>
              </Fragment>
            ))}
            <tr>
              <td
                colSpan={conLugar ? 7 : 6}
                className="border border-black px-2 py-1 text-center font-bold italic"
              >
                *****FIN DE LÍNEA*****
              </td>
            </tr>
          </tbody>
        </table>

        {/* Cuadro de totales a la derecha */}
        <div className="flex justify-end">
          <table className="border-collapse border border-black w-[300px]">
            <tbody>
              <tr className="border-b border-black">
                <td className="border-r border-black px-3 py-1 font-bold text-sm">Total C$</td>
                <td className="px-3 py-1 font-bold text-sm text-right">
                  {formatCurrency(consolidado.total)}
                </td>
              </tr>
              {consolidado.mostrarTotalUsd && (
                <tr className="border-b border-black">
                  <td className="border-r border-black px-3 py-1 font-bold text-sm">Total U$</td>
                  <td className="px-3 py-1 font-bold text-sm text-right">
                    {formatUsd(consolidado.totalUsd ?? 0)}
                  </td>
                </tr>
              )}
              <tr className={tieneSaldo || tieneAbonos ? 'border-b border-black' : ''}>
                <td className="border-r border-black px-3 py-1 font-bold text-sm">Total KG</td>
                <td className="px-3 py-1 font-bold text-sm text-right">
                  {(consolidado.totalKg ?? 0).toFixed(2)}
                </td>
              </tr>
              {tieneSaldo && (
                <tr className="border-b border-black">
                  <td className="border-r border-black px-3 py-1 font-bold text-sm">
                    Saldo anterior
                  </td>
                  <td className="px-3 py-1 font-bold text-sm text-right">
                    {formatCurrency(consolidado.saldoAnterior)}
                  </td>
                </tr>
              )}
              {tieneAbonos && (
                <tr className="border-b border-black">
                  <td className="border-r border-black px-3 py-1 font-bold text-sm">
                    Menos abonos
                  </td>
                  <td className="px-3 py-1 font-bold text-sm text-right">
                    {formatCurrency(consolidado.totalAbonos)}
                  </td>
                </tr>
              )}
              {(tieneSaldo || tieneAbonos) && (
                <>
                  <tr className={consolidado.mostrarTotalUsd ? 'border-b border-black' : ''}>
                    <td className="border-r border-black px-3 py-1 font-bold text-sm">
                      Total actualizado C$
                    </td>
                    <td className="px-3 py-1 font-bold text-sm text-right">
                      {formatCurrency(consolidado.totalActualizado)}
                    </td>
                  </tr>
                  {consolidado.mostrarTotalUsd && (
                    <tr>
                      <td className="border-r border-black px-3 py-1 font-bold text-sm">
                        Total actualizado U$
                      </td>
                      <td className="px-3 py-1 font-bold text-sm text-right">
                        {formatUsd(consolidado.totalActualizadoUsd ?? 0)}
                      </td>
                    </tr>
                  )}
                </>
              )}
            </tbody>
          </table>
        </div>
      </div>
    )
  }
)
