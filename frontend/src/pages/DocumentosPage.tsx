import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { Documento, EstadoDocumento, TipoDocumento } from '@/types'
import { documentoService } from '@/services/documentoService'
import { formatCurrency, formatDate } from '@/lib/format'
import { EstadoDocumentoBadge } from '@/components/documentos/EstadoDocumentoBadge'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { FileText, Plus, ReceiptText, Search } from 'lucide-react'

const TEXTOS = {
  COTIZACION: { titulo: 'Cotizaciones', subtitulo: 'Precios que se le dan a un cliente antes de la venta', nuevo: 'Nueva cotización', vacio: 'Todavía no hay cotizaciones.', icono: FileText },
  FACTURA: { titulo: 'Facturas', subtitulo: 'Ventas confirmadas', nuevo: 'Nueva factura', vacio: 'Todavía no hay facturas.', icono: ReceiptText },
}

const ESTADOS: Record<TipoDocumento, { valor: EstadoDocumento; texto: string }[]> = {
  COTIZACION: [
    { valor: 'PENDIENTE', texto: 'Pendientes' },
    { valor: 'ACEPTADA', texto: 'Aceptadas' },
    { valor: 'RECHAZADA', texto: 'Rechazadas' },
  ],
  FACTURA: [
    { valor: 'ACTIVA', texto: 'Activas' },
    { valor: 'ANULADA', texto: 'Anuladas' },
  ],
}

/** Lista de cotizaciones o de facturas. Al hacer clic en una fila se abre el documento. */
export function DocumentosPage({ tipo }: { tipo: TipoDocumento }) {
  const textos = TEXTOS[tipo]
  const navigate = useNavigate()
  const [documentos, setDocumentos] = useState<Documento[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busqueda, setBusqueda] = useState('')
  const [debouncedBusqueda, setDebouncedBusqueda] = useState('')
  const [estado, setEstado] = useState<EstadoDocumento | ''>('')
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(1)
  const [, setTotalElementos] = useState(0)

  // Espera de 300 ms en la búsqueda y vuelve a la primera página
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedBusqueda(busqueda)
      setPagina(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [busqueda])

  // Al cambiar de tipo, resetea todo a página 0
  useEffect(() => {
    setBusqueda('')
    setDebouncedBusqueda('')
    setEstado('')
    setPagina(0)
  }, [tipo])

  // Carga con paginación y búsqueda en servidor
  useEffect(() => {
    setCargando(true)
    setError(null)
    documentoService.listar({
      tipo,
      estado: estado || undefined,
      q: debouncedBusqueda.trim() || undefined,
      pagina,
      tamano: 50,
    })
      .then(res => {
        setDocumentos(res.contenido)
        setTotalPaginas(res.totalPaginas)
        setTotalElementos(res.totalElementos)
      })
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar los datos'))
      .finally(() => setCargando(false))
  }, [tipo, estado, debouncedBusqueda, pagina])

  const handleEstadoChange = (nuevoEstado: EstadoDocumento | '') => {
    setEstado(nuevoEstado)
    setPagina(0)
  }

  const Icono = textos.icono

  return (
    <div className="p-4 md:p-8 flex flex-col gap-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Icono className="w-5 h-5" /> {textos.titulo}
          </h1>
          <p className="text-sm text-[#705D56] mt-0.5">{textos.subtitulo}</p>
        </div>
        <Button
          onClick={() => navigate(tipo === 'COTIZACION' ? '/documentos/nuevo?tipo=COTIZACION' : '/documentos/nuevo')}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white gap-2 shrink-0"
        >
          <Plus className="w-4 h-4" /> {textos.nuevo}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative w-full max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
          <Input
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
            placeholder="Buscar por folio, cliente o proyecto…"
            className="pl-8"
          />
        </div>
        <select
          aria-label="Filtrar por estado"
          value={estado}
          onChange={e => handleEstadoChange(e.target.value as EstadoDocumento | '')}
          className="rounded border border-[#E5E7EB] bg-white px-3 h-11 text-sm text-[#022F40] outline-none focus:border-[#022F40]"
        >
          <option value="">Todos los estados</option>
          {ESTADOS[tipo].map(e => <option key={e.valor} value={e.valor}>{e.texto}</option>)}
        </select>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Folio</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Fecha</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Cliente</th>
              <th className="text-right px-4 py-3 font-medium text-[#705D56]">Total</th>
              <th className="text-left px-4 py-3 font-medium text-[#705D56]">Estado</th>
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-[#80727B]">Cargando…</td></tr>
            )}
            {!cargando && documentos.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-[#80727B]">
                  {!busqueda && !estado ? (
                    <div className="flex flex-col items-center justify-center gap-3">
                      <p className="text-sm text-[#705D56]">{textos.vacio}</p>
                      <Button onClick={() => navigate(tipo === 'COTIZACION' ? '/documentos/nuevo?tipo=COTIZACION' : '/documentos/nuevo')}>
                        <Plus className="w-4 h-4" /> {textos.nuevo}
                      </Button>
                    </div>
                  ) : (
                    <p className="text-sm text-[#80727B]">No se encontraron documentos.</p>
                  )}
                </td>
              </tr>
            )}
            {!cargando && documentos.map(d => (
              <tr
                key={d.id}
                onClick={() => navigate(`/documentos/${d.id}`)}
                className="border-b border-[#E5E7EB] last:border-b-0 hover:bg-[#F9FAFB] cursor-pointer transition-colors"
              >
                <td className="px-4 py-3 font-mono text-sm font-semibold text-[#022F40]">
                  <Link to={`/documentos/${d.id}`} onClick={e => e.stopPropagation()}>{d.folio}</Link>
                </td>
                <td className="px-4 py-3 text-[#705D56] whitespace-nowrap">{formatDate(d.fecha)}</td>
                <td className="px-4 py-3 text-[#022F40]">
                  {d.clienteNombre}
                  {d.proyectoNombre && <span className="text-[#80727B]"> / {d.proyectoNombre}</span>}
                </td>
                <td className="px-4 py-3 text-right font-semibold text-[#022F40] whitespace-nowrap">{formatCurrency(d.total)}</td>
                <td className="px-4 py-3"><EstadoDocumentoBadge documento={d} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ── Paginación ── */}
      <div className="flex items-center justify-between px-4 py-3 bg-white border border-[#E5E7EB] rounded shadow-xs">
        <Button
          variant="outline"
          disabled={pagina <= 0 || cargando}
          onClick={() => setPagina(p => Math.max(0, p - 1))}
          className="text-sm"
        >
          Anterior
        </Button>
        <span className="text-sm text-[#705D56]">
          Página {totalPaginas === 0 ? 0 : pagina + 1} de {Math.max(1, totalPaginas)}
        </span>
        <Button
          variant="outline"
          disabled={pagina >= totalPaginas - 1 || cargando}
          onClick={() => setPagina(p => p + 1)}
          className="text-sm"
        >
          Siguiente
        </Button>
      </div>
    </div>
  )
}
