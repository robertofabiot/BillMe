import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Search, Tags } from 'lucide-react'
import type { Cliente, PrecioHistorialFila } from '@/types'
import { clienteService } from '@/services/clienteService'
import { precioService } from '@/services/precioService'
import { formatCurrency, formatDate } from '@/lib/format'
import { Input } from '@/components/ui/input'

export function PreciosPage() {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [filas, setFilas] = useState<PrecioHistorialFila[]>([])
  const [cargando, setCargando] = useState(true)
  const [texto, setTexto] = useState('')
  const [clienteId, setClienteId] = useState('')

  // Cargar lista de clientes al inicio
  useEffect(() => {
    clienteService.listar()
      .then(lista => {
        setClientes(lista.slice().sort((a, b) => a.nombre.localeCompare(b.nombre)))
      })
      .catch(err => console.error('Error al cargar clientes:', err))
  }, [])

  // Buscar con espera de 300 ms
  useEffect(() => {
    let cancelado = false
    const timer = setTimeout(async () => {
      setCargando(true)
      try {
        const data = await precioService.historial({
          q: texto.trim() || undefined,
          clienteId: clienteId || undefined,
        })
        if (!cancelado) {
          setFilas(data)
        }
      } catch (err) {
        console.error('Error al buscar historial de precios:', err)
        if (!cancelado) {
          setFilas([])
        }
      } finally {
        if (!cancelado) {
          setCargando(false)
        }
      }
    }, 300)

    return () => {
      cancelado = true
      clearTimeout(timer)
    }
  }, [texto, clienteId])

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#022F40] flex items-center gap-2">
            <Tags className="w-6 h-6 text-[#91E5F6]" />
            Precios
          </h1>
          <p className="text-sm text-[#705D56] mt-1">
            Historial de precios cobrados en cotizaciones y facturas
          </p>
        </div>
      </div>

      {/* Filtros */}
      <div className="bg-white border border-[#E5E7EB] rounded p-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#80727B]" />
          <Input
            value={texto}
            onChange={e => setTexto(e.target.value)}
            placeholder="Buscar por descripción del producto..."
            className="pl-9 bg-[#F9FAFB] border-[#E5E7EB]"
          />
        </div>
        <div className="w-full sm:w-64">
          <select
            value={clienteId}
            onChange={e => setClienteId(e.target.value)}
            className="w-full h-11 rounded-md border border-[#E5E7EB] bg-[#F9FAFB] px-3 text-base text-[#022F40] shadow-sm focus:outline-none focus:ring-1 focus:ring-[#022F40]"
          >
            <option value="">Todos los clientes</option>
            {clientes.map(c => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Tabla */}
      <div className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Fecha</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Folio</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Cliente</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Descripción</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Cantidad</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Precio unit</th>
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[#80727B]">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && filas.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[#80727B]">
                  No se encontró ningún precio.
                </td>
              </tr>
            )}
            {!cargando &&
              filas.map((fila, idx) => (
                <tr
                  key={`${fila.documentoId}-${idx}`}
                  className="border-b border-[#E5E7EB] last:border-b-0 hover:bg-[#F9FAFB] transition-colors"
                >
                  <td className="px-4 py-3 text-[#705D56] whitespace-nowrap">
                    {formatDate(fila.fecha)}
                  </td>
                  <td className="px-4 py-3 font-mono text-sm font-semibold whitespace-nowrap">
                    <Link
                      to={`/documentos/${fila.documentoId}`}
                      className="text-[#022F40] hover:underline"
                    >
                      {fila.folio}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-[#022F40] whitespace-nowrap">
                    {fila.clienteNombre}
                  </td>
                  <td className="px-4 py-3 text-[#022F40] max-w-md">
                    {fila.descripcion}
                  </td>
                  <td className="px-4 py-3 text-right text-[#022F40] whitespace-nowrap">
                    {fila.cantidad}
                  </td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-1.5 font-semibold text-[#022F40]">
                      <span>{formatCurrency(fila.precioUnit)}</span>
                      {fila.viejo && (
                        <span className="text-amber-700 bg-amber-50 border border-amber-200 text-sm px-1.5 py-0.5 rounded font-normal">
                          Viejo
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
