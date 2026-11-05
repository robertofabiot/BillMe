import { useEffect, useMemo, useState } from 'react'
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
  const [estado, setEstado] = useState<EstadoDocumento | ''>('')

  useEffect(() => {
    setCargando(true)
    setError(null)
    setBusqueda('')
    setEstado('')
    documentoService.listar({ tipo })
      .then(setDocumentos)
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar los datos'))
      .finally(() => setCargando(false))
  }, [tipo])

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return documentos.filter(d =>
      (!estado || d.estado === estado) &&
      (!q || d.folio.toLowerCase().includes(q)
        || d.clienteNombre.toLowerCase().includes(q)
        || (d.proyectoNombre?.toLowerCase().includes(q) ?? false)))
  }, [documentos, busqueda, estado])

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
          onClick={() => navigate(`/documentos/nuevo?tipo=${tipo}`)}
          className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white h-9 gap-2 shrink-0"
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
          onChange={e => setEstado(e.target.value as EstadoDocumento | '')}
          className="rounded border border-[#E5E7EB] bg-white px-3 text-sm text-[#022F40] outline-none focus:border-[#022F40]"
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
            {!cargando && filtrados.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-[#80727B]">
                  {documentos.length === 0 ? (
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
            {!cargando && filtrados.map(d => (
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
    </div>
  )
}
