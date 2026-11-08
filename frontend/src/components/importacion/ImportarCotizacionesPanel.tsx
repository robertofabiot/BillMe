import { useState, useEffect, type ChangeEvent } from 'react'
import type { ImportacionCotizaciones, Cliente, DecisionConciliacion } from '@/types'
import { importacionService, clienteService } from '@/services'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { FileSpreadsheet, CheckCircle2 } from 'lucide-react'
import { ConciliacionTabla } from './ConciliacionTabla'

function Cifra({ etiqueta, valor, destacar }: { etiqueta: string; valor: number; destacar?: boolean }) {
  return (
    <div className="border border-[#E5E7EB] rounded px-4 py-3">
      <p className="text-sm text-[#705D56]">{etiqueta}</p>
      <p className={`text-xl font-bold ${destacar ? 'text-[#022F40]' : 'text-[#80727B]'}`}>
        {valor.toLocaleString('es-NI')}
      </p>
    </div>
  )
}

export function ImportarCotizacionesPanel() {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [clienteId, setClienteId] = useState('')
  const [archivo, setArchivo] = useState<File | null>(null)
  const [resumen, setResumen] = useState<ImportacionCotizaciones | null>(null)
  const [decisiones, setDecisiones] = useState<Record<string, string>>({})
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    clienteService
      .listar()
      .then(lista => setClientes(lista.slice().sort((a, b) => a.nombre.localeCompare(b.nombre))))
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudo cargar la lista de clientes'))
  }, [])

  const elegirArchivo = (e: ChangeEvent<HTMLInputElement>) => {
    setArchivo(e.target.files?.[0] ?? null)
    setResumen(null)
    setDecisiones({})
    setError(null)
  }

  const elegirCliente = (e: ChangeEvent<HTMLSelectElement>) => {
    setClienteId(e.target.value)
    setResumen(null)
    setDecisiones({})
    setError(null)
  }

  const cambiarDecision = (descripcion: string, productoId: string) => {
    setDecisiones(prev => ({ ...prev, [descripcion]: productoId }))
  }

  const ejecutar = async (paso: () => Promise<ImportacionCotizaciones>) => {
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

  const revisar = () => {
    if (!archivo || !clienteId) return
    ejecutar(() => importacionService.vistaPreviaCotizaciones(archivo, clienteId))
  }

  const importar = () => {
    if (!archivo || !clienteId) return
    const decisionesList: DecisionConciliacion[] = Object.entries(decisiones).map(
      ([descripcion, productoId]) => ({ descripcion, productoId })
    )
    ejecutar(() =>
      importacionService.aplicarCotizaciones(
        archivo,
        clienteId,
        decisionesList.length > 0 ? decisionesList : undefined
      )
    )
  }

  return (
    <div className="bg-white border border-[#E5E7EB] rounded shadow-sm flex flex-col overflow-hidden">
      <div className="px-6 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center gap-2">
        <FileSpreadsheet className="w-4 h-4 text-[#022F40]" />
        <h2 className="text-sm font-semibold text-[#022F40]">Histórico de precios</h2>
      </div>

      <div className="p-6 flex flex-col gap-5">
        <p className="text-sm text-[#705D56]">
          Sube el Excel con el historial de precios. Primero se muestra qué se va a importar; nada se guarda hasta que presiones "Importar". Puedes subir el mismo archivo otra vez sin que se duplique nada. Estos registros no aparecen en Facturas ni cuentan en saldos o reportes: solo sirven de referencia de precios. Para usar uno como venta, ábrelo desde Precios y conviértelo en factura activa.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="archivo-cotizaciones" className="text-sm font-medium text-[#705D56]">
              Archivo de Excel (.xlsx o .xlsm)
            </label>
            <Input
              id="archivo-cotizaciones"
              type="file"
              accept=".xlsx,.xlsm"
              onChange={elegirArchivo}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cliente-cotizaciones" className="text-sm font-medium text-[#705D56]">
              Cliente de las filas sin columna cliente
            </label>
            <select
              id="cliente-cotizaciones"
              value={clienteId}
              onChange={elegirCliente}
              className="w-full rounded border border-[#E5E7EB] bg-white px-3 text-sm text-[#022F40] outline-none focus:border-[#022F40]"
            >
              <option value="">Elige un cliente…</option>
              {clientes.map(c => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <Button
            onClick={revisar}
            disabled={!archivo || !clienteId || trabajando}
            variant="outline"
          >
            {trabajando && !resumen ? 'Revisando…' : 'Revisar archivo'}
          </Button>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        {resumen && (
          <div className="flex flex-col gap-4">
            {resumen.aplicada && (
              <p className="text-sm text-[#558564] flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-[#558564]" /> Histórico importado.
              </p>
            )}

            <p className="text-sm text-[#80727B]">
              Hoja "{resumen.hoja}": {resumen.filasLeidas.toLocaleString('es-NI')} filas leídas.
            </p>

            {resumen.clientesNuevos.length > 0 && (
              <div className="rounded border border-[#E5E7EB] bg-[#F9FAFB] p-3 text-sm text-[#705D56]">
                <span className="font-medium text-[#022F40]">
                  Clientes que se crearán automáticamente ({resumen.clientesNuevos.length}):{' '}
                </span>
                {resumen.clientesNuevos.join(', ')}
              </div>
            )}

            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <Cifra etiqueta="Filas leídas" valor={resumen.filasLeidas} />
              <Cifra etiqueta="Filas nuevas" valor={resumen.filasNuevas} destacar />
              <Cifra etiqueta="Ya importadas" valor={resumen.filasYaImportadas} />
              <Cifra etiqueta="Registros nuevos" valor={resumen.registrosNuevos} destacar />
            </div>

            {resumen.registrosConLineasNuevas > 0 && (
              <p className="text-sm text-[#705D56]">
                {resumen.registrosConLineasNuevas === 1
                  ? '1 registro existente recibirá líneas nuevas.'
                  : `${resumen.registrosConLineasNuevas} registros existentes recibirán líneas nuevas.`}
              </p>
            )}

            {resumen.filasIncompletas.length > 0 && (
              <div className="rounded border border-amber-200 bg-amber-50 p-3 flex flex-col gap-1 text-sm text-amber-900">
                <p className="font-semibold text-amber-950">
                  Filas incompletas no importadas ({resumen.filasIncompletas.length}):
                </p>
                <ul className="list-disc list-inside space-y-0.5">
                  {resumen.filasIncompletas.map(f => (
                    <li key={f.fila}>
                      Fila {f.fila}: {f.motivo}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {!resumen.aplicada && resumen.porConciliar.length > 0 && (
              <ConciliacionTabla
                items={resumen.porConciliar}
                decisiones={decisiones}
                onChange={cambiarDecision}
              />
            )}

            {!resumen.aplicada && (
              <div>
                <Button
                  onClick={importar}
                  disabled={trabajando || resumen.filasNuevas === 0}
                  className="bg-[#022F40] hover:bg-[#022F40]/90 text-white"
                >
                  {trabajando
                    ? 'Importando…'
                    : resumen.filasNuevas > 0
                      ? 'Importar'
                      : 'No hay nada nuevo que importar'}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
