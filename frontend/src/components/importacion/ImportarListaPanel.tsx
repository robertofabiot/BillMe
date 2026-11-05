import { useState, type ChangeEvent } from 'react'
import type { ImportacionLista } from '@/types'
import { importacionService } from '@/services'
import { formatCurrency, hoyIso } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { FileSpreadsheet, CheckCircle2 } from 'lucide-react'

function Cifra({ etiqueta, valor, destacar }: { etiqueta: string; valor: number; destacar?: boolean }) {
  return (
    <div className="border border-[#E5E7EB] rounded px-4 py-3">
      <p className="text-sm text-[#705D56]">{etiqueta}</p>
      <p className={`text-xl font-bold ${destacar ? 'text-[#022F40]' : 'text-[#80727B]'}`}>{valor.toLocaleString('es-NI')}</p>
    </div>
  )
}

/**
 * Pasos: 1) elegir el archivo y la fecha, 2) revisar qué va a cambiar, 3) importar.
 * Se puede repetir con la misma lista sin duplicar nada.
 */
export function ImportarListaPanel() {
  const [archivo, setArchivo] = useState<File | null>(null)
  const [fecha, setFecha] = useState(hoyIso())
  const [resumen, setResumen] = useState<ImportacionLista | null>(null)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const elegirArchivo = (e: ChangeEvent<HTMLInputElement>) => {
    setArchivo(e.target.files?.[0] ?? null)
    setResumen(null)
    setError(null)
  }

  const ejecutar = async (paso: () => Promise<ImportacionLista>) => {
    setTrabajando(true)
    setError(null)
    try {
      setResumen(await paso())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo procesar el archivo')
    } finally {
      setTrabajando(false)
    }
  }

  const revisar = () => archivo && ejecutar(() => importacionService.vistaPreviaLista(archivo))
  const importar = () => archivo && ejecutar(() => importacionService.aplicarLista(archivo, fecha))

  const hayCambios = resumen && (resumen.productosNuevos > 0 || resumen.preciosQueCambian > 0)

  return (
    <div className="bg-white border border-[#E5E7EB] rounded shadow-sm flex flex-col overflow-hidden">
      <div className="px-6 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center gap-2">
        <FileSpreadsheet className="w-4 h-4 text-[#022F40]" />
        <h2 className="text-sm font-semibold text-[#022F40]">Lista de precios del proveedor</h2>
      </div>

      <div className="p-6 flex flex-col gap-5">
        <p className="text-sm text-[#705D56]">
          Sube el Excel de la lista. Primero se muestra qué va a cambiar; nada se guarda hasta que presiones "Importar".
          Puedes subir la misma lista otra vez sin que se duplique nada.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="archivo" className="text-sm font-medium text-[#705D56]">Archivo de Excel (.xlsx o .xlsm)</label>
            <Input id="archivo" type="file" accept=".xlsx,.xlsm" onChange={elegirArchivo} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="fecha" className="text-sm font-medium text-[#705D56]">Los precios rigen desde</label>
            <Input id="fecha" type="date" value={fecha} onChange={e => setFecha(e.target.value)} />
          </div>
        </div>

        <div>
          <Button onClick={revisar} disabled={!archivo || trabajando} variant="outline">
            {trabajando && !resumen ? 'Revisando…' : 'Revisar archivo'}
          </Button>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        {resumen && (
          <div className="flex flex-col gap-4">
            {resumen.aplicada && (
              <p className="text-sm text-[#558564] flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4" /> Lista importada.
              </p>
            )}
            <p className="text-sm text-[#80727B]">
              Hoja "{resumen.hoja}": {resumen.filasLeidas.toLocaleString('es-NI')} productos leídos
              {resumen.filasIgnoradas > 0 && `, ${resumen.filasIgnoradas} filas ignoradas (sin código o repetidas)`}.
            </p>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <Cifra etiqueta="Productos nuevos" valor={resumen.productosNuevos} destacar />
              <Cifra etiqueta="Precios que cambian" valor={resumen.preciosQueCambian} destacar />
              <Cifra etiqueta="Sin cambios" valor={resumen.sinCambios} />
              <Cifra etiqueta="Sin precio en la lista" valor={resumen.sinPrecio} />
            </div>

            {resumen.muestra.length > 0 && (
              <div className="border border-[#E5E7EB] rounded overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Código</th>
                      <th className="text-left px-3 py-2 font-medium text-[#705D56]">Producto</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Antes</th>
                      <th className="text-right px-3 py-2 font-medium text-[#705D56]">Ahora</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resumen.muestra.map(c => (
                      <tr key={c.codigo} className="border-b border-[#E5E7EB] last:border-b-0">
                        <td className="px-3 py-2 font-mono text-[#705D56]">{c.codigo}</td>
                        <td className="px-3 py-2 text-[#022F40]">{c.descripcion}</td>
                        <td className="px-3 py-2 text-right text-[#80727B]">{c.precioAnterior != null ? formatCurrency(c.precioAnterior) : 'Nuevo'}</td>
                        <td className="px-3 py-2 text-right font-semibold text-[#022F40]">{c.precioNuevo != null ? formatCurrency(c.precioNuevo) : '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {resumen.productosNuevos + resumen.preciosQueCambian > resumen.muestra.length && (
                  <p className="px-3 py-2 text-sm text-[#80727B] border-t border-[#E5E7EB]">Se muestran los primeros {resumen.muestra.length}.</p>
                )}
              </div>
            )}

            {!resumen.aplicada && (
              <div>
                <Button onClick={importar} disabled={trabajando || !hayCambios} className="bg-[#022F40] hover:bg-[#022F40]/90 text-white">
                  {trabajando ? 'Importando…' : hayCambios ? 'Importar' : 'No hay nada nuevo que importar'}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
