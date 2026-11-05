import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { reporteService } from '@/services/reporteService'
import { clienteService } from '@/services/clienteService'
import { proyectoService } from '@/services/proyectoService'
import type { Cliente, Ganancias, Proyecto } from '@/types'
import { formatCurrency, formatFechaCorta } from '@/lib/format'
import { Input } from '@/components/ui/input'
import { AlertTriangle, BarChart3 } from 'lucide-react'

const SELECT =
  'h-9 w-full rounded border border-[#E5E7EB] bg-white px-3 text-sm text-[#022F40] outline-none focus:border-[#022F40] disabled:opacity-60'

export function ReportesPage() {
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [clienteId, setClienteId] = useState('')
  const [proyectoId, setProyectoId] = useState('')

  const [clientes, setClientes] = useState<Cliente[]>([])
  const [proyectos, setProyectos] = useState<Proyecto[]>([])
  const [ganancias, setGanancias] = useState<Ganancias | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    clienteService.listar().then(setClientes).catch(console.error)
  }, [])

  useEffect(() => {
    if (clienteId) {
      proyectoService
        .listar(clienteId)
        .then(res => {
          setProyectos(res)
          setProyectoId(curr => (res.some(p => p.id === curr) ? curr : ''))
        })
        .catch(console.error)
    } else {
      proyectoService.listar().then(setProyectos).catch(console.error)
    }
  }, [clienteId])

  useEffect(() => {
    setCargando(true)
    setError(null)
    const filtros: {
      desde?: string
      hasta?: string
      clienteId?: string
      proyectoId?: string
    } = {}
    if (desde) filtros.desde = desde
    if (hasta) filtros.hasta = hasta
    if (clienteId) filtros.clienteId = clienteId
    if (proyectoId) filtros.proyectoId = proyectoId

    reporteService
      .ganancias(filtros)
      .then(setGanancias)
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudo cargar el reporte'))
      .finally(() => setCargando(false))
  }, [desde, hasta, clienteId, proyectoId])

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6 w-full max-w-7xl mx-auto">
      {/* ── Encabezado ── */}
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
          <BarChart3 className="w-5 h-5" /> Reportes
        </h1>
      </div>

      {/* ── Filtros ── */}
      <section className="bg-white border border-[#E5E7EB] rounded shadow-sm p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="filtro-desde" className="text-xs font-medium text-[#705D56]">
            Desde
          </label>
          <Input
            id="filtro-desde"
            type="date"
            value={desde}
            onChange={e => setDesde(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="filtro-hasta" className="text-xs font-medium text-[#705D56]">
            Hasta
          </label>
          <Input
            id="filtro-hasta"
            type="date"
            value={hasta}
            onChange={e => setHasta(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="filtro-cliente" className="text-xs font-medium text-[#705D56]">
            Cliente
          </label>
          <select
            id="filtro-cliente"
            value={clienteId}
            onChange={e => setClienteId(e.target.value)}
            className={SELECT}
          >
            <option value="">Todos los clientes</option>
            {clientes.map(c => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="filtro-proyecto" className="text-xs font-medium text-[#705D56]">
            Proyecto
          </label>
          <select
            id="filtro-proyecto"
            value={proyectoId}
            onChange={e => setProyectoId(e.target.value)}
            className={SELECT}
          >
            <option value="">Todos los proyectos</option>
            {proyectos.map(p => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
        </div>
      </section>

      {/* ── Error ── */}
      {error && (
        <p className="text-sm text-red-600 bg-red-50 p-3 rounded border border-red-200">
          {error}
        </p>
      )}

      {/* ── Tarjetas de métricas ── */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 flex flex-col">
          <span className="text-xs font-semibold uppercase tracking-wider text-[#705D56] mb-1">
            Facturado
          </span>
          <span className="text-2xl font-bold text-[#022F40]">
            {ganancias ? formatCurrency(ganancias.facturado) : '—'}
          </span>
        </div>

        <div className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 flex flex-col">
          <span className="text-xs font-semibold uppercase tracking-wider text-[#705D56] mb-1">
            Costo
          </span>
          <span className="text-2xl font-bold text-[#022F40]">
            {ganancias ? formatCurrency(ganancias.costo) : '—'}
          </span>
        </div>

        <div className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 flex flex-col">
          <span className="text-xs font-semibold uppercase tracking-wider text-[#705D56] mb-1">
            Ganancia
          </span>
          <span className="text-2xl font-bold text-[#558564]">
            {ganancias ? formatCurrency(ganancias.ganancia) : '—'}
          </span>
        </div>
      </div>

      {/* ── Aviso en ámbar de líneas sin costo ── */}
      {ganancias && ganancias.lineasSinCosto > 0 && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 rounded p-4 flex items-center gap-3 text-sm">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
          <span>
            {ganancias.lineasSinCosto} líneas sin costo (
            {formatCurrency(ganancias.ventasSinCosto)} vendidos) no entran en la ganancia.
          </span>
        </div>
      )}

      {/* ── Tabla de facturas ── */}
      <div className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB] text-xs font-semibold text-[#705D56]">
                <th className="p-3">Folio</th>
                <th className="p-3">Fecha</th>
                <th className="p-3">Cliente</th>
                <th className="p-3 text-right">Total</th>
                <th className="p-3 text-right">Costo</th>
                <th className="p-3 text-right">Ganancia</th>
                <th className="p-3 text-right">Sin costo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E5E7EB] text-sm">
              {cargando ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-sm text-[#705D56]">
                    Cargando reporte…
                  </td>
                </tr>
              ) : !ganancias || ganancias.facturas.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-sm text-[#705D56]">
                    No hay facturas en el período seleccionado.
                  </td>
                </tr>
              ) : (
                ganancias.facturas.map(f => (
                  <tr key={f.id} className="hover:bg-gray-50/50">
                    <td className="p-3">
                      <Link
                        to={`/documentos/${f.id}`}
                        className="font-semibold text-[#022F40] hover:underline"
                      >
                        {f.folio}
                      </Link>
                    </td>
                    <td className="p-3 text-[#705D56]">{formatFechaCorta(f.fecha)}</td>
                    <td className="p-3 text-[#022F40] font-medium">{f.clienteNombre}</td>
                    <td className="p-3 text-right text-[#022F40] font-mono">
                      {formatCurrency(f.total)}
                    </td>
                    <td className="p-3 text-right text-[#705D56] font-mono">
                      {formatCurrency(f.costo)}
                    </td>
                    <td className="p-3 text-right font-semibold text-[#558564] font-mono">
                      {formatCurrency(f.ganancia)}
                    </td>
                    <td className="p-3 text-right font-mono">
                      {f.lineasSinCosto > 0 ? (
                        <span className="text-amber-700 font-semibold">{f.lineasSinCosto}</span>
                      ) : (
                        <span className="text-[#705D56]">0</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
