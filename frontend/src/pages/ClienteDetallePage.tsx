import { useEffect, useState, useCallback } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import type { Cliente, CuentaCliente, Documento, Proyecto, Abono, MovimientoCuenta } from '@/types'
import { clienteService, type ClientePayload } from '@/services/clienteService'
import { documentoService } from '@/services/documentoService'
import { proyectoService } from '@/services/proyectoService'
import { abonoService } from '@/services/abonoService'
import { formatCurrency, formatDate, textoSaldo, hoyIso } from '@/lib/format'
import { EstadoDocumentoBadge } from '@/components/documentos/EstadoDocumentoBadge'
import { ClienteModal } from '@/components/clientes/ClienteModal'
import { AbonoDialog } from '@/components/clientes/AbonoDialog'
import { Button } from '@/components/ui/button'
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Plus,
  Phone,
  StickyNote,
  TrendingUp,
  FolderKanban,
  ReceiptText,
  Calendar,
} from 'lucide-react'

export function ClienteDetallePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [cuenta, setCuenta] = useState<CuentaCliente | null>(null)
  const [documentos, setDocumentos] = useState<Documento[]>([])
  const [proyectos, setProyectos] = useState<Proyecto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Dialogs
  const [showEditCliente, setShowEditCliente] = useState(false)
  const [showAbonoDialog, setShowAbonoDialog] = useState(false)
  const [abonoTarget, setAbonoTarget] = useState<Abono | null>(null)

  const cargarDatos = useCallback(async () => {
    if (!id) return
    try {
      setError(null)
      const [c, cu, docs, proys] = await Promise.all([
        clienteService.obtener(id),
        clienteService.cuenta(id),
        documentoService.listar({ clienteId: id }),
        proyectoService.listar(id),
      ])
      setCliente(c)
      setCuenta(cu)
      setDocumentos(docs)
      setProyectos(proys)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar datos del cliente')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    cargarDatos()
  }, [cargarDatos])

  const handleSaveCliente = async (data: ClientePayload & { id?: string }) => {
    if (!id) return
    try {
      await clienteService.actualizar(id, data)
      await cargarDatos()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al actualizar el cliente')
    }
  }

  const handleNuevoAbono = () => {
    setAbonoTarget(null)
    setShowAbonoDialog(true)
  }

  const handleEditAbono = (mov: MovimientoCuenta) => {
    if (!cliente || !mov.id) return
    const proy = proyectos.find(p => p.nombre === mov.proyectoNombre)
    setAbonoTarget({
      id: mov.id,
      clienteId: cliente.id,
      proyectoId: proy ? proy.id : null,
      proyectoNombre: mov.proyectoNombre,
      monto: mov.abono ?? 0,
      fechaPago: mov.fecha ?? hoyIso(),
      nota: mov.concepto !== 'Abono' ? mov.concepto : null,
    })
    setShowAbonoDialog(true)
  }

  const handleEliminarCliente = async () => {
    if (!cliente) return
    if (!window.confirm(`¿Eliminar al cliente ${cliente.nombre}?`)) return
    try {
      await clienteService.eliminar(cliente.id)
      navigate('/clientes')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al eliminar el cliente')
    }
  }

  const handleDeleteAbono = async (mov: MovimientoCuenta) => {
    if (!mov.id) return
    const montoFmt = formatCurrency(mov.abono ?? 0)
    const fechaFmt = mov.fecha ? formatDate(mov.fecha) : ''
    const confirmado = window.confirm(`¿Eliminar el abono de ${montoFmt} del ${fechaFmt}?`)
    if (!confirmado) return

    try {
      await abonoService.eliminar(mov.id)
      await cargarDatos()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al eliminar el abono')
    }
  }

  if (loading) {
    return (
      <div className="p-4 md:p-8 flex items-center justify-center h-64">
        <div className="text-sm text-[#705D56]">Cargando cuenta del cliente…</div>
      </div>
    )
  }

  if (error && !cliente) {
    return (
      <div className="p-4 md:p-8 flex flex-col items-center justify-center h-64 gap-3">
        <div className="text-sm text-red-600">{error}</div>
        <Link to="/clientes" className="text-sm text-[#022F40] underline">
          Volver a clientes
        </Link>
      </div>
    )
  }

  if (!cliente || !cuenta) return null

  const movimientosRecientes = [...cuenta.movimientos].reverse()

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6 max-w-5xl mx-auto">
      {/* ── Breadcrumb / Volver ── */}
      <div>
        <Link
          to="/clientes"
          className="inline-flex items-center gap-1.5 text-sm text-[#705D56] hover:text-[#022F40] transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" /> Volver a Clientes
        </Link>
      </div>

      {/* ── Error Banner ── */}
      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
          {error}
        </div>
      )}

      {/* ── 1. Header: Nombre, teléfono, notas y acciones ── */}
      <div className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded bg-[#022F40] flex items-center justify-center shrink-0">
            <span className="text-[#91E5F6] font-bold text-lg">
              {cliente.nombre.charAt(0).toUpperCase()}
            </span>
          </div>
          <div className="flex flex-col gap-1">
            <h1 className="text-xl font-bold text-[#022F40]">{cliente.nombre}</h1>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-[#705D56]">
              {cliente.telefono ? (
                <span className="flex items-center gap-1">
                  <Phone className="w-3.5 h-3.5" /> {cliente.telefono}
                </span>
              ) : (
                <span className="text-[#80727B]">Sin teléfono</span>
              )}
              {cliente.fechaInicioCuenta && (
                <span className="flex items-center gap-1 text-[#80727B]">
                  <Calendar className="w-3.5 h-3.5" /> Cuenta desde {formatDate(cliente.fechaInicioCuenta)}
                </span>
              )}
            </div>
            {cliente.detalles && (
              <p className="text-sm text-[#80727B] flex items-start gap-1 mt-1">
                <StickyNote className="w-3.5 h-3.5 shrink-0 mt-0.5" /> {cliente.detalles}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 self-start md:self-center shrink-0">
          <Button
            variant="outline"
            onClick={handleEliminarCliente}
            className="rounded border-red-200 text-red-600 hover:bg-red-50 text-sm gap-1.5"
          >
            <Trash2 className="w-3.5 h-3.5" /> Eliminar
          </Button>
          <Button
            variant="outline"
            onClick={() => setShowEditCliente(true)}
            className="rounded border-[#E5E7EB] text-[#022F40] text-sm gap-1.5"
          >
            <Pencil className="w-3.5 h-3.5" /> Editar
          </Button>
          <Button
            onClick={handleNuevoAbono}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm gap-1.5"
          >
            <Plus className="w-4 h-4" /> Registrar abono
          </Button>
        </div>
      </div>

      {/* ── 2. Tarjeta con saldo en grande, Facturado y Abonado ── */}
      <div className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wider text-[#80727B]">Estado de cuenta</p>
          <p
            className={`text-3xl font-extrabold mt-1 ${
              cuenta.saldo > 0
                ? 'text-[#022F40]'
                : cuenta.saldo < 0
                  ? 'text-[#558564]'
                  : 'text-[#558564]'
            }`}
          >
            {textoSaldo(cuenta.saldo)}
          </p>
        </div>

        <div className="h-px bg-[#E5E7EB]" />

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <div>
            <p className="text-sm text-[#705D56]">Facturado</p>
            <p className="text-lg font-bold text-[#022F40] mt-0.5">
              {formatCurrency(cuenta.totalFacturado)}
            </p>
          </div>
          <div>
            <p className="text-sm text-[#705D56]">Abonado</p>
            <p className="text-lg font-bold text-[#558564] mt-0.5">
              {formatCurrency(cuenta.totalAbonado)}
            </p>
          </div>
          {cuenta.saldoInicial !== 0 && (
            <div>
              <p className="text-sm text-[#705D56]">Saldo inicial</p>
              <p className="text-lg font-bold text-[#705D56] mt-0.5">
                {formatCurrency(cuenta.saldoInicial)}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ── 3. Por proyecto (solo si tiene proyectos) ── */}
      {cuenta.proyectos && cuenta.proyectos.length > 0 && (
        <section className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col gap-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
            <FolderKanban className="w-4 h-4" /> Por proyecto
          </h2>
          <div className="border border-[#E5E7EB] rounded overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Proyecto</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Facturado</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Abonado</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Saldo</th>
                </tr>
              </thead>
              <tbody>
                {cuenta.proyectos.map((p, idx) => (
                  <tr
                    key={p.proyectoId ?? `sin-proyecto-${idx}`}
                    className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] transition-colors ${
                      idx === cuenta.proyectos.length - 1 ? 'border-b-0' : ''
                    }`}
                  >
                    <td className="px-4 py-2.5 font-medium text-[#022F40]">{p.nombre}</td>
                    <td className="px-4 py-2.5 text-right font-semibold text-[#022F40]">
                      {formatCurrency(p.facturado)}
                    </td>
                    <td className="px-4 py-2.5 text-right font-semibold text-[#558564]">
                      {formatCurrency(p.abonado)}
                    </td>
                    <td className="px-4 py-2.5 text-right font-bold text-[#022F40]">
                      {textoSaldo(p.saldo)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* ── 4. Movimientos (del más reciente al más antiguo) ── */}
      <section className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col gap-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
          <TrendingUp className="w-4 h-4" /> Movimientos ({cuenta.movimientos.length})
        </h2>
        {movimientosRecientes.length === 0 ? (
          <p className="text-sm text-[#80727B] py-3 text-center">Sin movimientos registrados.</p>
        ) : (
          <div className="border border-[#E5E7EB] rounded overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Fecha</th>
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Concepto</th>
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Proyecto</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Cargo</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Abono</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Saldo</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {movimientosRecientes.map((m, idx) => (
                  <tr
                    key={m.id ? `${m.tipo}-${m.id}` : `mov-${idx}`}
                    className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] transition-colors ${
                      idx === movimientosRecientes.length - 1 ? 'border-b-0' : ''
                    }`}
                  >
                    <td className="px-4 py-2.5 text-[#705D56] whitespace-nowrap">
                      {m.fecha ? formatDate(m.fecha) : '—'}
                    </td>
                    <td className="px-4 py-2.5">
                      {m.tipo === 'FACTURA' && m.id ? (
                        <Link
                          to={`/documentos/${m.id}`}
                          className="font-mono font-semibold text-[#022F40] underline underline-offset-2"
                        >
                          {m.concepto}
                        </Link>
                      ) : (
                        <span className="text-[#022F40] font-medium">{m.concepto}</span>
                      )}
                    </td>
                    <td className="px-4 py-2.5 text-[#705D56] whitespace-nowrap">
                      {m.proyectoNombre || '—'}
                    </td>
                    <td className="px-4 py-2.5 text-right font-semibold text-[#022F40] whitespace-nowrap">
                      {m.cargo != null ? formatCurrency(m.cargo) : '—'}
                    </td>
                    <td className="px-4 py-2.5 text-right font-semibold text-[#558564] whitespace-nowrap">
                      {m.abono != null ? formatCurrency(m.abono) : '—'}
                    </td>
                    <td className="px-4 py-2.5 text-right font-bold text-[#022F40] whitespace-nowrap">
                      {textoSaldo(m.saldo)}
                    </td>
                    <td className="px-4 py-2.5 text-right whitespace-nowrap">
                      {m.tipo === 'ABONO' && (
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => handleEditAbono(m)}
                            className="p-1 rounded text-[#80727B] hover:text-[#022F40] hover:bg-[#F9FAFB] transition-colors"
                            title="Editar abono"
                          >
                            <Pencil className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDeleteAbono(m)}
                            className="p-1 rounded text-[#80727B] hover:text-red-600 hover:bg-red-50 transition-colors"
                            title="Eliminar abono"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* ── 5. Cotizaciones y facturas: Folio, Fecha, Total y Estado ── */}
      <section className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col gap-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
          <ReceiptText className="w-4 h-4" /> Cotizaciones y facturas ({documentos.length})
        </h2>
        {documentos.length === 0 ? (
          <p className="text-sm text-[#80727B] py-3 text-center">Sin cotizaciones ni facturas.</p>
        ) : (
          <div className="border border-[#E5E7EB] rounded overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Folio</th>
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Fecha</th>
                  <th className="text-right px-4 py-2.5 font-medium text-[#705D56]">Total</th>
                  <th className="text-left px-4 py-2.5 font-medium text-[#705D56]">Estado</th>
                </tr>
              </thead>
              <tbody>
                {documentos.map((d, idx) => (
                  <tr
                    key={d.id}
                    className={`border-b border-[#E5E7EB] hover:bg-[#F9FAFB] transition-colors ${
                      idx === documentos.length - 1 ? 'border-b-0' : ''
                    }`}
                  >
                    <td className="px-4 py-2.5 font-mono font-semibold text-[#022F40]">
                      <Link to={`/documentos/${d.id}`} className="underline underline-offset-2">
                        {d.folio}
                      </Link>
                    </td>
                    <td className="px-4 py-2.5 text-[#705D56] whitespace-nowrap">
                      {formatDate(d.fecha)}
                    </td>
                    <td className="px-4 py-2.5 text-right font-semibold text-[#022F40] whitespace-nowrap">
                      {formatCurrency(d.total)}
                    </td>
                    <td className="px-4 py-2.5">
                      <EstadoDocumentoBadge documento={d} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* ── Diálogos ── */}
      <ClienteModal
        open={showEditCliente}
        onOpenChange={setShowEditCliente}
        initial={cliente}
        onSave={handleSaveCliente}
      />

      <AbonoDialog
        open={showAbonoDialog}
        onOpenChange={setShowAbonoDialog}
        clienteId={cliente.id}
        proyectos={proyectos}
        initial={abonoTarget}
        onGuardado={cargarDatos}
      />
    </div>
  )
}
