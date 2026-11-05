import { forwardRef } from 'react'
import type { Documento } from '@/types'
import { formatCurrency, formatFechaCorta } from '@/lib/format'

export interface DocumentoPlantillaProps {
  documento: Documento
  encabezado: string
}

export const DocumentoPlantilla = forwardRef<HTMLDivElement, DocumentoPlantillaProps>(
  function DocumentoPlantilla({ documento, encabezado }, ref) {
    const conEntrega = documento.mostrarEntrega

    return (
      <div
        ref={ref}
        className="p-8 font-sans"
        style={{
          width: conEntrega ? '960px' : '700px',
          backgroundColor: '#FFFFFF',
          color: '#000000',
        }}
      >
        {/* Fila superior: encabezado a la izquierda, fecha a la derecha */}
        <div className="flex items-start justify-between gap-4 mb-4">
          <div className="font-bold text-lg leading-tight">
            {encabezado}
          </div>
          <div className="font-bold text-sm whitespace-nowrap">
            {formatFechaCorta(documento.fecha)}
          </div>
        </div>

        {/* Tabla estática */}
        <table className="w-full border-collapse border border-black text-sm mb-4">
          <thead>
            <tr>
              <th className="border border-black px-2 py-1 text-center font-bold w-16">Cantidad</th>
              <th className="border border-black px-2 py-1 text-left font-bold">Descripción</th>
              <th className="border border-black px-2 py-1 text-right font-bold w-24 whitespace-nowrap">Peso Unit (KG)</th>
              <th className="border border-black px-2 py-1 text-right font-bold w-24 whitespace-nowrap">Peso Total (KG)</th>
              <th className="border border-black px-2 py-1 text-right font-bold w-28 whitespace-nowrap">Precio Unitario C$</th>
              <th className="border border-black px-2 py-1 text-right font-bold w-28 whitespace-nowrap">Precio Total C$</th>
              {conEntrega && (
                <>
                  <th className="border border-black px-2 py-1 text-center font-bold w-28 whitespace-nowrap">Fecha entrega</th>
                  <th className="border border-black px-2 py-1 text-left font-bold w-32 whitespace-nowrap">Lugar entrega</th>
                </>
              )}
            </tr>
          </thead>
          <tbody>
            {documento.lineas.map((linea) => {
              const tienePeso = linea.pesoUnitKg != null
              return (
                <tr key={linea.id}>
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
                  {conEntrega && (
                    <>
                      <td className="border border-black px-2 py-1 text-center">
                        {linea.fechaEntrega ? formatFechaCorta(linea.fechaEntrega) : ''}
                      </td>
                      <td className="border border-black px-2 py-1 text-left">
                        {linea.lugarEntrega || ''}
                      </td>
                    </>
                  )}
                </tr>
              )
            })}
            <tr>
              <td
                colSpan={conEntrega ? 8 : 6}
                className="border border-black px-2 py-1 text-center font-bold italic"
              >
                *****FIN DE LÍNEA*****
              </td>
            </tr>
          </tbody>
        </table>

        {/* Cuadro de totales a la derecha */}
        <div className="flex justify-end">
          <table className="border-collapse border border-black w-[280px]">
            <tbody>
              <tr className="border-b border-black">
                <td className="border-r border-black px-3 py-1 font-bold text-sm">
                  Total C$
                </td>
                <td className="px-3 py-1 font-bold text-sm text-right">
                  {formatCurrency(documento.total)}
                </td>
              </tr>
              <tr>
                <td className="border-r border-black px-3 py-1 font-bold text-sm">
                  Total KG
                </td>
                <td className="px-3 py-1 font-bold text-sm text-right">
                  {(documento.totalKg ?? 0).toFixed(2)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    )
  }
)
